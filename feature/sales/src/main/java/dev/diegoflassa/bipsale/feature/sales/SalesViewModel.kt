package dev.diegoflassa.bipsale.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.repository.ProductRepository
import dev.diegoflassa.bipsale.core.data.repository.SaleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository
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
            is SalesContract.Intent.FinalizeSale -> finalizeSale()
        }
    }

    private fun updateCustomerInfo(name: String, cpf: String, anonymous: Boolean) {
        _uiState.update { it.copy(customerName = name, customerCpf = cpf, isAnonymous = anonymous) }
    }

    private fun addProductByQr(qrData: String) {
        try {
            val uri = android.net.Uri.parse(qrData)
            val code = uri.getQueryParameter("code") ?: return
            val price = uri.getQueryParameter("price")?.toDoubleOrNull() ?: 0.0
            
            viewModelScope.launch {
                val product = productRepository.getProductByCode(code)
                val productName = product?.productName ?: "Produto Desconhecido"
                addItem(code, productName, price)
            }
        } catch (e: Exception) {
            viewModelScope.launch { _effect.send(SalesContract.Effect.ShowError("QR Code inválido")) }
        }
    }

    private fun addItem(code: String, name: String, price: Double) {
        val newItem = SaleItemEntity(
            saleId = "",
            productCode = code,
            productName = name,
            unitPrice = price,
            quantity = 1
        )
        _uiState.update { state ->
            val updatedItems = state.items + newItem
            state.copy(items = updatedItems).recalculate()
        }
    }

    private fun removeItem(item: SaleItemEntity) {
        _uiState.update { state ->
            val updatedItems = state.items - item
            state.copy(items = updatedItems).recalculate()
        }
    }

    private fun updateDiscount(percentage: Double) {
        _uiState.update { it.copy(discountPercentage = percentage).recalculate() }
    }

    private fun SalesContract.State.recalculate(): SalesContract.State {
        val total = items.sumOf { it.unitPrice * it.quantity }
        val discount = total * (discountPercentage / 100.0)
        return copy(totalAmount = total, finalAmount = total - discount)
    }

    private fun finalizeSale() {
        val state = _uiState.value
        val saleId = UUID.randomUUID().toString()
        val sale = SaleEntity(
            id = saleId,
            customerName = if (state.isAnonymous) "Anônimo" else state.customerName,
            customerCpf = if (state.isAnonymous) "-" else state.customerCpf,
            totalAmount = state.totalAmount,
            discountPercentage = state.discountPercentage,
            finalAmount = state.finalAmount,
            paymentMethod = "PIX",
            date = System.currentTimeMillis()
        )
        val itemsWithSaleId = state.items.map { it.copy(saleId = saleId) }

        viewModelScope.launch {
            saleRepository.insertFullSale(sale, itemsWithSaleId)
            _uiState.update { it.copy(isSaleFinished = true) }
            _effect.send(SalesContract.Effect.NavigateBack)
        }
    }
}
