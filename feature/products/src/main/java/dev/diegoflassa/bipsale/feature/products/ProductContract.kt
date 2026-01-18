package dev.diegoflassa.bipsale.feature.products

import dev.diegoflassa.bipsale.core.domain.model.Product

class ProductContract {
    data class State(
        val products: List<Product> = emptyList(),
        val selectedProductCodes: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed interface Intent {
        data object LoadProducts : Intent
        data class SaveProduct(val code: String, val name: String, val price: Double) : Intent
        data class DeleteProduct(val product: Product) : Intent
        data class ToggleProductSelection(val code: String) : Intent
        data object ClearSelection : Intent
    }

    sealed interface Effect {
        data object NavigationBack : Effect
        data class ShowSnackbar(val message: String) : Effect
    }
}
