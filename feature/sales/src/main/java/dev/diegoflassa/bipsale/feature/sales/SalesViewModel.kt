package dev.diegoflassa.bipsale.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.usecase.AddProductByQrUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.FinalizeSaleUseCase
import dev.diegoflassa.bipsale.core.ui.util.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val finalizeSaleUseCase: FinalizeSaleUseCase,
    private val addProductByQrUseCase: AddProductByQrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesContract.State())
    val uiState: StateFlow<SalesContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<SalesContract.Effect>()
    val effect: Flow<SalesContract.Effect> = _effect.receiveAsFlow()

    fun onIntent(intent: SalesContract.Intent) {
        when (intent) {
            is SalesContract.Intent.UpdateCustomerInfo -> updateCustomerInfo(intent.name, intent.cpf, intent.anonymous)
            is SalesContract.Intent.AddProductByQr -> addProductByQr(intent.qrData)
            is SalesContract.Intent.AddManualItem -> addItem(intent.code, intent.name, intent.price)
            is SalesContract.Intent.RemoveItem -> removeItem(intent.item)
            is SalesContract.Intent.UpdateDiscount -> updateDiscount(intent.percentage)
            is SalesContract.Intent.SelectPaymentMethod -> selectPaymentMethod(intent.method)
            is SalesContract.Intent.FinalizeSale -> finalizeSale()
        }
    }

    private fun updateCustomerInfo(name: String, cpf: String, anonymous: Boolean) {
        _uiState.update { it.copy(customerName = name, customerCpf = cpf, isAnonymous = anonymous) }
    }

    private fun addProductByQr(qrData: String) {
        viewModelScope.launch {
            addProductByQrUseCase(qrData)
                .onSuccess { item ->
                    _uiState.update { state -> state.copy(items = state.items + item).recalculate() }
                }
                .onFailure {
                    _effect.send(SalesContract.Effect.ShowError(UiText.StringResource(R.string.invalid_qr_code)))
                }
        }
    }

    private fun addItem(code: String, name: String, price: Double) {
        val newItem = SaleItem(saleId = "", productCode = code, productName = name, unitPrice = price, quantity = 1)
        _uiState.update { state -> state.copy(items = state.items + newItem).recalculate() }
    }

    private fun removeItem(item: SaleItem) {
        _uiState.update { state -> state.copy(items = state.items - item).recalculate() }
    }

    private fun updateDiscount(percentage: Double) {
        _uiState.update { it.copy(discountPercentage = percentage).recalculate() }
    }

    private fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    private fun SalesContract.State.recalculate(): SalesContract.State {
        val total = items.sumOf { it.unitPrice * it.quantity }
        return copy(totalAmount = total, finalAmount = total - total * (discountPercentage / 100.0))
    }

    private fun finalizeSale() {
        val state = _uiState.value
        viewModelScope.launch {
            finalizeSaleUseCase(
                customerName = if (state.isAnonymous) "" else state.customerName,
                customerCpf = if (state.isAnonymous) "" else state.customerCpf,
                items = state.items,
                discountPercentage = state.discountPercentage,
                paymentMethod = state.paymentMethod
            ).onSuccess {
                _uiState.update { it.copy(isSaleFinished = true) }
                _effect.send(SalesContract.Effect.NavigateBack)
            }.onFailure {
                _effect.send(SalesContract.Effect.ShowError(UiText.StringResource(R.string.error_finalizing_sale)))
            }
        }
    }
}
