package dev.diegoflassa.bipsale.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.data.repository.ProductRepository
import dev.diegoflassa.bipsale.feature.qrcode.QrGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val qrGenerator: QrGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductContract.State())
    val uiState: StateFlow<ProductContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<ProductContract.Effect>()
    val effect: Flow<ProductContract.Effect> = _effect.receiveAsFlow()

    init {
        onIntent(ProductContract.Intent.LoadProducts)
    }

    fun onIntent(intent: ProductContract.Intent) {
        when (intent) {
            is ProductContract.Intent.LoadProducts -> loadProducts()
            is ProductContract.Intent.SaveProduct -> saveProduct(intent.code, intent.name, intent.price)
            is ProductContract.Intent.DeleteProduct -> deleteProduct(intent.product)
            is ProductContract.Intent.ToggleProductSelection -> toggleSelection(intent.code)
            is ProductContract.Intent.ClearSelection -> clearSelection()
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collect { list ->
                _uiState.update { it.copy(products = list) }
            }
        }
    }

    private fun saveProduct(code: String, name: String, price: Double) {
        viewModelScope.launch {
            val qrData = "bipsale://product?code=$code&price=$price"
            val product = ProductEntity(
                productCode = code,
                productName = name,
                price = price,
                qrCodeData = qrData,
                lastUpdated = System.currentTimeMillis()
            )
            productRepository.insertProduct(product)
            _effect.send(ProductContract.Effect.NavigationBack)
        }
    }

    private fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
            _effect.send(ProductContract.Effect.ShowSnackbar("Produto removido"))
        }
    }

    private fun toggleSelection(code: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedProductCodes.contains(code)) {
                state.selectedProductCodes - code
            } else {
                state.selectedProductCodes + code
            }
            state.copy(selectedProductCodes = newSelection)
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedProductCodes = emptySet()) }
    }

    suspend fun getProductByCode(code: String): ProductEntity? {
        return productRepository.getProductByCode(code)
    }
}
