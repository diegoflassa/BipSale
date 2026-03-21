package dev.diegoflassa.bipsale.feature.products

import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.ui.util.UiText

class ProductContract {
    data class State(
        val products: List<Product> = emptyList(),
        val editProduct: Product? = null,
        val selectedProductCodes: Set<String> = emptySet(),
        val isLoading: Boolean = false
    )

    sealed interface Intent {
        data object LoadProducts : Intent
        data class LoadProduct(val code: String) : Intent
        data class SaveProduct(val code: String, val name: String, val price: Double) : Intent
        data class DeleteProduct(val product: Product) : Intent
        data class ToggleProductSelection(val code: String) : Intent
        data object ClearSelection : Intent
    }

    sealed interface Effect {
        data object NavigationBack : Effect
        data class ShowSnackbar(val message: UiText) : Effect
    }
}
