package dev.diegoflassa.bipsale.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.usecase.DeleteProductUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductsUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.SaveProductImageUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.SaveProductUseCase
import dev.diegoflassa.bipsale.core.domain.util.parsePriceInput
import dev.diegoflassa.bipsale.core.qrcode.LabelData
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
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProducts: GetProductsUseCase,
    private val getProduct: GetProductUseCase,
    private val saveProduct: SaveProductUseCase,
    private val deleteProduct: DeleteProductUseCase,
    private val saveProductImage: SaveProductImageUseCase,
    private val productImageStore: ProductImageStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductContract.State())
    val uiState: StateFlow<ProductContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<ProductContract.Effect>()
    val effect: Flow<ProductContract.Effect> = _effect.receiveAsFlow()

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    /** Last emission, kept so a delete can pass the real entity back to the repository. */
    private var domainProducts: List<Product> = emptyList()

    init {
        onIntent(ProductContract.Intent.LoadProducts)
    }

    fun onIntent(intent: ProductContract.Intent) {
        when (intent) {
            is ProductContract.Intent.LoadProducts -> loadProducts()
            is ProductContract.Intent.LoadProduct -> loadProduct(intent.code)
            is ProductContract.Intent.CodeChanged -> updateEditor { it.copy(code = intent.value) }
            is ProductContract.Intent.NameChanged -> updateEditor { it.copy(name = intent.value) }
            is ProductContract.Intent.PriceChanged ->
                updateEditor { it.copy(priceInput = intent.value) }
            is ProductContract.Intent.ImagePicked -> importImage(intent.uri)
            is ProductContract.Intent.SaveProduct -> persistEditor()
            is ProductContract.Intent.DeleteProduct -> removeProduct(intent.code)
            is ProductContract.Intent.ToggleProductSelection -> toggleSelection(intent.code)
            is ProductContract.Intent.ClearSelection -> clearSelection()
            is ProductContract.Intent.PrintAllQrCodes -> print(_uiState.value.allLabels)
            is ProductContract.Intent.PrintSelectedQrCodes -> print(_uiState.value.selectedLabels)
            is ProductContract.Intent.PrintEditorLabel ->
                print(listOfNotNull(_uiState.value.editor.label))
            is ProductContract.Intent.ShowLabelPreview ->
                updateEditor { it.copy(isLabelPreviewVisible = true) }
            is ProductContract.Intent.HideLabelPreview ->
                updateEditor { it.copy(isLabelPreviewVisible = false) }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            getProducts()
                .catch { throwable ->
                    Timber.e(throwable, "[BipSale][Product] Loading product list failed")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.toUiText())
                    }
                }
                .collect { products ->
                    Timber.d("[BipSale][Product] Product list emitted count=%d", products.size)
                    domainProducts = products
                    _uiState.update { state ->
                        state.copy(
                            products = products.map { it.toUiModel() },
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun loadProduct(code: String) {
        viewModelScope.launch {
            Timber.d("[BipSale][Product] Loading product code=%s", code)
            getProduct(code)
                .onSuccess { product ->
                    if (product == null) {
                        Timber.w("[BipSale][Product] No product found for code=%s", code)
                        return@onSuccess
                    }
                    updateEditor {
                        it.copy(
                            code = product.code,
                            name = product.name,
                            priceInput = product.price.toString(),
                            imageFileName = product.imageFileName,
                            imagePath = product.imageFileName
                                ?.let(productImageStore::resolvePath)
                        )
                    }
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[BipSale][Product] Loading product failed code=%s", code)
                    emitSnackbar(throwable.toUiText())
                }
        }
    }

    private fun importImage(uri: String) {
        val editor = _uiState.value.editor
        viewModelScope.launch {
            Timber.d("[BipSale][Product][IMAGE] Importing image for code=%s", editor.code)
            saveProductImage(uri, editor.code, editor.imageFileName)
                .onSuccess { fileName ->
                    updateEditor {
                        it.copy(
                            imageFileName = fileName,
                            imagePath = productImageStore.resolvePath(fileName)
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "[BipSale][Product][IMAGE] Import failed")
                    emitSnackbar(UiText.StringResource(R.string.products_image_save_failed))
                }
        }
    }

    private fun persistEditor() {
        val editor = _uiState.value.editor
        val price = parsePriceInput(editor.priceInput)
        if (price == null) {
            Timber.w("[BipSale][Product] Rejected unparseable price for code=%s", editor.code)
            emitSnackbar(UiText.StringResource(R.string.products_price_invalid))
            return
        }

        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            Timber.d("[BipSale][Product] Saving product code=%s", editor.code)
            saveProduct(editor.code, editor.name, price, editor.imageFileName)
                .onSuccess {
                    Timber.d("[BipSale][Product] Product saved code=%s", editor.code)
                    _effect.send(ProductContract.Effect.NavigationBack)
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "[BipSale][Product] Save failed code=%s", editor.code)
                    updateEditor { it.copy(isSaving = false) }
                    emitSnackbar(throwable.toUiText())
                }
        }
    }

    private fun removeProduct(code: String) {
        val product = domainProducts.firstOrNull { it.code == code }
        if (product == null) {
            Timber.w("[BipSale][Product] Delete requested for unknown code=%s", code)
            return
        }
        viewModelScope.launch {
            Timber.d("[BipSale][Product] Deleting product code=%s", code)
            deleteProduct(product)
                .onSuccess { emitSnackbar(UiText.StringResource(R.string.product_deleted)) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Timber.e(throwable, "[BipSale][Product] Delete failed code=%s", code)
                    emitSnackbar(throwable.toUiText())
                }
        }
    }

    private fun toggleSelection(code: String) {
        _uiState.update { state ->
            val selection = if (code in state.selectedProductCodes) {
                state.selectedProductCodes - code
            } else {
                state.selectedProductCodes + code
            }
            state.copy(selectedProductCodes = selection)
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedProductCodes = emptySet()) }
    }

    private fun print(labels: List<LabelData>) {
        if (labels.isEmpty()) {
            Timber.w("[BipSale][Product][QR_EXPORT] Print requested with nothing to print")
            emitSnackbar(UiText.StringResource(R.string.products_no_products_to_print))
            return
        }
        Timber.d("[BipSale][Product][QR_EXPORT] Requesting print of %d labels", labels.size)
        viewModelScope.launch { _effect.send(ProductContract.Effect.PrintLabels(labels)) }
    }

    private fun updateEditor(transform: (ProductContract.Editor) -> ProductContract.Editor) {
        _uiState.update { state ->
            val editor = transform(state.editor)
            state.copy(editor = editor.copy(label = editor.toLabel()))
        }
    }

    private fun emitSnackbar(message: UiText) {
        viewModelScope.launch { _effect.send(ProductContract.Effect.ShowSnackbar(message)) }
    }

    private fun ProductContract.Editor.toLabel(): LabelData? {
        if (code.isBlank()) return null
        val price = parsePriceInput(priceInput) ?: return null
        return LabelData(
            qrData = SaveProductUseCase.buildQrPayload(code, price),
            productName = name.ifBlank { code },
            priceFormatted = currencyFormat.format(price)
        )
    }

    private fun Product.toUiModel() = ProductContract.ProductUiModel(
        code = code,
        name = name,
        priceFormatted = currencyFormat.format(price),
        imagePath = imageFileName?.let(productImageStore::resolvePath),
        label = LabelData(
            qrData = qrCode ?: SaveProductUseCase.buildQrPayload(code, price),
            productName = name,
            priceFormatted = currencyFormat.format(price)
        )
    )

    private fun Throwable.toUiText(): UiText =
        message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.products_generic_error)
}
