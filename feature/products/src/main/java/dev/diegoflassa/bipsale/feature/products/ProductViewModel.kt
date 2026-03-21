package dev.diegoflassa.bipsale.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import dev.diegoflassa.bipsale.core.domain.usecase.SaveProductUseCase
import dev.diegoflassa.bipsale.core.ui.util.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val saveProductUseCase: SaveProductUseCase
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
            is ProductContract.Intent.LoadProduct -> loadProduct(intent.code)
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

    private fun loadProduct(code: String) {
        viewModelScope.launch {
            val product = productRepository.getProductByCode(code)
            _uiState.update { it.copy(editProduct = product) }
        }
    }

    private fun saveProduct(code: String, name: String, price: Double) {
        viewModelScope.launch {
            saveProductUseCase(code, name, price)
                .onSuccess { _effect.send(ProductContract.Effect.NavigationBack) }
                .onFailure { _effect.send(ProductContract.Effect.ShowSnackbar(UiText.DynamicString(it.message ?: ""))) }
        }
    }

    private fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
            _effect.send(ProductContract.Effect.ShowSnackbar(UiText.StringResource(R.string.product_deleted)))
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
}
