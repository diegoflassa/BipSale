package dev.diegoflassa.bipsale.feature.products

import androidx.compose.runtime.Immutable
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.ui.util.UiText

class ProductContract {

    /**
     * A product as the list renders it. Price arrives formatted and the image already resolved to
     * a path, so the screen never touches currency rules or the filesystem.
     */
    @Immutable
    data class ProductUiModel(
        val code: String,
        val name: String,
        val priceFormatted: String,
        val imagePath: String?,
        val label: LabelData
    )

    @Immutable
    data class State(
        val products: List<ProductUiModel> = emptyList(),
        val selectedProductCodes: Set<String> = emptySet(),
        val isLoading: Boolean = true,
        val errorMessage: UiText? = null,
        val editor: Editor = Editor()
    ) {
        val selectedLabels: List<LabelData>
            get() = products.filter { it.code in selectedProductCodes }.map { it.label }

        val allLabels: List<LabelData>
            get() = products.map { it.label }
    }

    /** Edit-screen form state. Lives here so a test can drive the screen from one value. */
    @Immutable
    data class Editor(
        val code: String = "",
        val name: String = "",
        val priceInput: String = "",
        val imagePath: String? = null,
        val imageFileName: String? = null,
        val isSaving: Boolean = false,
        val label: LabelData? = null
    ) {
        val canSave: Boolean
            get() = !isSaving && code.isNotBlank() && name.isNotBlank() && label != null
    }

    sealed interface Intent {
        data object LoadProducts : Intent
        data class LoadProduct(val code: String) : Intent
        data class CodeChanged(val value: String) : Intent
        data class NameChanged(val value: String) : Intent
        data class PriceChanged(val value: String) : Intent
        data class ImagePicked(val uri: String) : Intent
        data object SaveProduct : Intent
        data class DeleteProduct(val code: String) : Intent
        data class ToggleProductSelection(val code: String) : Intent
        data object ClearSelection : Intent
        data object PrintAllQrCodes : Intent
        data object PrintSelectedQrCodes : Intent
        data object PrintEditorLabel : Intent
    }

    sealed interface Effect {
        data object NavigationBack : Effect
        data class ShowSnackbar(val message: UiText) : Effect
        data class PrintLabels(val labels: List<LabelData>) : Effect
    }
}
