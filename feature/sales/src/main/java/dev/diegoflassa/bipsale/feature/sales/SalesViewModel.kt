package dev.diegoflassa.bipsale.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.MAX_SALE_DISCOUNT_PERCENTAGE
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.model.saleTotals
import dev.diegoflassa.bipsale.core.domain.usecase.AddProductByCodeUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.AddProductByQrUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.FinalizeSaleUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductsUseCase
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.pix.PixDefaults
import dev.diegoflassa.bipsale.core.domain.pix.PixPayload
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import dev.diegoflassa.bipsale.core.ui.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val finalizeSaleUseCase: FinalizeSaleUseCase,
    private val addProductByQrUseCase: AddProductByQrUseCase,
    private val addProductByCodeUseCase: AddProductByCodeUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val settingsRepository: SettingsRepository,
    private val productImageStore: ProductImageStore
) : ViewModel() {

    /** Read once per sale; the checkout path must not wait on a settings read per keystroke. */
    private var settings: AppSettings = AppSettings.EMPTY

    private val _uiState = MutableStateFlow(SalesContract.State())
    val uiState: StateFlow<SalesContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<SalesContract.Effect>()
    val effect: Flow<SalesContract.Effect> = _effect.receiveAsFlow()

    init {
        loadCatalog()
        loadSettings()
    }

    fun onIntent(intent: SalesContract.Intent) {
        when (intent) {
            is SalesContract.Intent.UpdateCustomerInfo ->
                updateCustomerInfo(intent.name, intent.cpf, intent.anonymous)

            is SalesContract.Intent.AddProductByQr -> addProductByQr(intent.qrData)
            is SalesContract.Intent.AddProductByCode -> addProductByCode(intent.code)
            is SalesContract.Intent.RemoveItem -> removeItem(intent.itemId)
            is SalesContract.Intent.UpdateItemDiscount ->
                updateItemDiscount(intent.itemId, intent.discount)

            is SalesContract.Intent.UpdateDiscount -> updateDiscount(intent.percentage)
            is SalesContract.Intent.SelectPaymentMethod -> selectPaymentMethod(intent.method)
            is SalesContract.Intent.ShowProductDetail ->
                _uiState.update { it.copy(detailItemId = intent.itemId) }

            is SalesContract.Intent.HideProductDetail ->
                _uiState.update { it.copy(detailItemId = null) }

            is SalesContract.Intent.TogglePixAmount -> togglePixAmount(intent.carriesAmount)
            is SalesContract.Intent.PixPaymentAcknowledged -> acknowledgePixPayment()
            is SalesContract.Intent.FinalizeSale -> finalizeSale()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settings = runCatching { settingsRepository.current() }
                .onFailure { Timber.e(it, "[BipSale][Sale] Loading settings failed") }
                .getOrDefault(AppSettings.EMPTY)
            Timber.d(
                "[BipSale][Sale] Settings loaded discount=%s",
                settings.pixDiscount::class.simpleName
            )
            // A method picked before the settings landed still gets its discount and its QR.
            _uiState.value.paymentMethod?.let(::selectPaymentMethod)
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            getProductsUseCase()
                .catch { throwable ->
                    Timber.e(throwable, "[BipSale][Sale] Loading the product catalogue failed")
                    emitError(UiText.StringResource(R.string.sales_catalog_unavailable))
                }
                .collect { products ->
                    Timber.d("[BipSale][Sale] Catalogue emitted count=%d", products.size)
                    _uiState.update { state ->
                        state.copy(catalog = products.map { it.toCatalogProduct() })
                    }
                }
        }
    }

    private fun updateCustomerInfo(name: String, cpf: String, anonymous: Boolean) {
        _uiState.update { it.copy(customerName = name, customerCpf = cpf, isAnonymous = anonymous) }
    }

    private fun addProductByQr(qrData: String) {
        viewModelScope.launch {
            addProductByQrUseCase(qrData)
                .onSuccess { addToCart(it, source = "QR") }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "[BipSale][Sale][CHECKOUT] Scan rejected")
                    emitError(UiText.StringResource(R.string.invalid_qr_code))
                }
        }
    }

    private fun addProductByCode(code: String) {
        viewModelScope.launch {
            addProductByCodeUseCase(code)
                .onSuccess { addToCart(it, source = "CODE") }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "[BipSale][Sale][CHECKOUT] Code rejected")
                    emitError(UiText.StringResource(R.string.sales_unknown_product_code))
                }
        }
    }

    private fun addToCart(item: SaleItem, source: String) {
        _uiState.update { state -> state.copy(items = state.items + item).recalculate() }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Line added via=%s code=%s unit=%.2f lines=%d total=%.2f",
            source, item.productCode, item.unitPrice, _uiState.value.items.size,
            _uiState.value.finalAmount
        )
    }

    private fun removeItem(itemId: String) {
        val removed = _uiState.value.items.firstOrNull { it.id == itemId }
        if (removed == null) {
            Timber.w("[BipSale][Sale][CHECKOUT] Remove requested for a line not in the cart")
            return
        }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.id == itemId }).recalculate()
        }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Line removed code=%s lines=%d total=%.2f",
            removed.productCode, _uiState.value.items.size, _uiState.value.finalAmount
        )
    }

    private fun updateItemDiscount(itemId: String, discount: ItemDiscount) {
        if (_uiState.value.items.none { it.id == itemId }) {
            Timber.w("[BipSale][Sale][CHECKOUT] Discount requested for a line not in the cart")
            return
        }
        _uiState.update { state ->
            val items = state.items.map { item ->
                if (item.id == itemId) item.copy(discount = discount) else item
            }
            state.copy(items = items).recalculate()
        }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Line discount applied kind=%s lineDiscounts=%.2f total=%.2f",
            discount.logKind(), _uiState.value.itemDiscountAmount, _uiState.value.finalAmount
        )
    }

    private fun updateDiscount(percentage: Double) {
        if (!percentage.isFinite() || percentage !in 0.0..MAX_SALE_DISCOUNT_PERCENTAGE) {
            Timber.w("[BipSale][Sale][CHECKOUT] Sale discount rejected as out of range")
            emitError(UiText.StringResource(R.string.sales_discount_out_of_range))
            return
        }
        _uiState.update { it.copy(discountPercentage = percentage, pixDiscountAutoApplied = false).recalculate() }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Sale discount applied percent=%.2f total=%.2f",
            percentage, _uiState.value.finalAmount
        )
    }

    /**
     * Picking PIX does two things beyond recording the method: it applies whatever default discount
     * the operator configured, and it builds the payload the customer scans. Both are derived here
     * rather than in the screen so the total shown and the total encoded cannot disagree.
     */
    private fun selectPaymentMethod(method: PaymentMethod) {
        val isPix = method == PaymentMethod.PIX
        _uiState.update { state ->
            val discounted = if (isPix) {
                state.copy(paymentMethod = method).applyPixDiscount()
            } else {
                state.copy(paymentMethod = method, pixPayload = null).stripAutoPixDiscount()
            }
            discounted.withPixPayload(isPix)
        }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Payment method set to %s total=%.2f pixReady=%b",
            method.serializedName,
            _uiState.value.finalAmount,
            _uiState.value.pixPayload != null
        )
    }

    /** The configured default only fills an empty sale discount; a typed one is never overwritten. */
    private fun SalesContract.State.applyPixDiscount(): SalesContract.State {
        val percent = (settings.pixDiscount as? ItemDiscount.Percentage)?.percent
        if (percent == null || discountPercentage > 0.0) return this
        Timber.d("[BipSale][Sale][CHECKOUT] Applying configured PIX discount percent=%.2f", percent)
        return copy(discountPercentage = percent, pixDiscountAutoApplied = true).recalculate()
    }

    /** Removes the discount only if it was auto-applied by PIX; a typed one is left alone. */
    private fun SalesContract.State.stripAutoPixDiscount(): SalesContract.State {
        if (!pixDiscountAutoApplied) return this
        Timber.d("[BipSale][Sale][CHECKOUT] Stripping auto-applied PIX discount")
        return copy(discountPercentage = 0.0, pixDiscountAutoApplied = false).recalculate()
    }

    private fun SalesContract.State.withPixPayload(isPix: Boolean): SalesContract.State {
        if (!isPix) return this
        val amount = if (pixCarriesAmount) finalAmount else null
        val payload = runCatching {
            PixPayload.build(
                pixKey = PixDefaults.KEY,
                merchantName = PixDefaults.MERCHANT_NAME,
                merchantCity = PixDefaults.MERCHANT_CITY,
                amount = amount
            )
        }.onFailure {
            Timber.e(it, "[BipSale][Sale][CHECKOUT] Building the PIX payload failed")
        }.getOrNull()
        return copy(pixPayload = payload)
    }

    private fun togglePixAmount(carriesAmount: Boolean) {
        _uiState.update { state ->
            state.copy(pixCarriesAmount = carriesAmount)
                .withPixPayload(state.paymentMethod == PaymentMethod.PIX)
        }
        Timber.d(
            "[BipSale][Sale][CHECKOUT] PIX amount mode toggled carriesAmount=%b",
            carriesAmount
        )
    }

    /**
     * A PIX payload encodes the amount, so the payload is rebuilt here rather than at the call
     * sites: every cart change already goes through this, and a QR left showing the pre-change
     * total is a customer underpaying by exactly the difference.
     */
    private fun SalesContract.State.recalculate(): SalesContract.State {
        val totals = saleTotals(items, discountPercentage)
        return copy(
            totalAmount = totals.grossAmount,
            itemDiscountAmount = totals.itemDiscountAmount,
            finalAmount = totals.finalAmount
        ).withPixPayload(paymentMethod == PaymentMethod.PIX)
    }

    private fun finalizeSale() {
        val state = _uiState.value
        val method = state.paymentMethod
        if (method == null) {
            // canFinalize already gates the button; this covers an intent arriving any other way.
            Timber.w("[BipSale][Sale][CHECKOUT] Finalize refused with no payment method chosen")
            emitError(UiText.StringResource(R.string.sales_payment_method_required))
            return
        }
        // The customer fields never reach the log; counts and totals are what reconciles a sale
        // that failed at a terminal, and none of them identify anybody (CORE_RULES section 8.3).
        Timber.i(
            "[BipSale][Sale][CHECKOUT] Finalize requested lines=%d gross=%.2f final=%.2f method=%s anonymous=%b",
            state.items.size, state.totalAmount, state.finalAmount,
            method.serializedName, state.isAnonymous
        )
        _uiState.update { it.copy(isFinalizing = true) }
        viewModelScope.launch {
            finalizeSaleUseCase(
                customerName = if (state.isAnonymous) "" else state.customerName,
                customerCpf = if (state.isAnonymous) "" else state.customerCpf,
                items = state.items,
                discountPercentage = state.discountPercentage,
                paymentMethod = method
            ).onSuccess { sale ->
                Timber.i(
                    "[BipSale][Sale][CHECKOUT] Sale confirmed id=%s lines=%d final=%.2f",
                    sale.id, sale.items.size, sale.finalAmount
                )
                // A PIX sale is not over when it is written — the customer still has to scan.
                // The screen holds until the operator says the payment came through.
                val awaitingPix = method == PaymentMethod.PIX
                _uiState.update {
                    it.copy(
                        isFinalizing = false,
                        isSaleFinished = true,
                        isAwaitingPixPayment = awaitingPix
                    )
                }
                if (awaitingPix) {
                    Timber.i(
                        "[BipSale][Sale][CHECKOUT] Holding for PIX payment pixReady=%b",
                        _uiState.value.pixPayload != null
                    )
                } else {
                    _effect.send(SalesContract.Effect.NavigateBack)
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Timber.e(
                    throwable,
                    "[BipSale][Sale][CHECKOUT] Finalize failed lines=%d final=%.2f",
                    state.items.size, state.finalAmount
                )
                _uiState.update { it.copy(isFinalizing = false) }
                emitError(UiText.StringResource(R.string.error_finalizing_sale))
            }
        }
    }

    private fun acknowledgePixPayment() {
        Timber.d("[BipSale][Sale][CHECKOUT] PIX payment acknowledged")
        _uiState.update { it.copy(isAwaitingPixPayment = false) }
        viewModelScope.launch { _effect.send(SalesContract.Effect.NavigateBack) }
    }

    private fun emitError(message: UiText) {
        viewModelScope.launch { _effect.send(SalesContract.Effect.ShowError(message)) }
    }

    private fun Product.toCatalogProduct() = SalesContract.CatalogProduct(
        code = code,
        name = name,
        price = price,
        imagePath = imageFileName?.let(productImageStore::resolvePath),
        quantity = quantity
    )

    /** Which kind of discount was applied; the value itself is already in the logged totals. */
    private fun ItemDiscount.logKind(): String = when (this) {
        is ItemDiscount.None -> "NONE"
        is ItemDiscount.Percentage -> "PERCENTAGE"
        is ItemDiscount.Amount -> "AMOUNT"
    }
}
