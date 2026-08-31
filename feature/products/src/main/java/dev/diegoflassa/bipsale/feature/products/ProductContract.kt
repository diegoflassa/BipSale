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
        val label: LabelData,
        /** Units on hand. Also how many labels a print run produces for this product. */
        val quantity: Int
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
            get() = products.filter { it.code in selectedProductCodes }.flatMap { it.printRun() }

        val allLabels: List<LabelData>
            get() = products.flatMap { it.printRun() }

        /**
         * One label per unit on hand, so a shelf of eight gets eight tags in one run. A product
         * with no stock still gets one — otherwise "print all" silently produces an empty job.
         */
        private fun ProductUiModel.printRun(): List<LabelData> =
            List(quantity.coerceAtLeast(1)) { label }
    }

    /** Edit-screen form state. Lives here so a test can drive the screen from one value. */
    @Immutable
    data class Editor(
        val code: String = "",
        val name: String = "",
        val priceInput: String = "",
        val quantityInput: String = "",
        val imagePath: String? = null,
        val imageFileName: String? = null,
        val isSaving: Boolean = false,
        val isLabelPreviewVisible: Boolean = false,
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
        data class QuantityChanged(val value: String) : Intent
        data class ImagePicked(val uri: String) : Intent
        data object SaveProduct : Intent
        data class DeleteProduct(val code: String) : Intent
        data class ToggleProductSelection(val code: String) : Intent
        data object ClearSelection : Intent
        data object PrintAllQrCodes : Intent
        data object PrintSelectedQrCodes : Intent
        data object PrintEditorLabel : Intent
        data object ShowLabelPreview : Intent
        data object HideLabelPreview : Intent

        /** Asks for a destination; nothing is written until one comes back. */
        data object DownloadTemplateRequested : Intent
        data class TemplateDestinationChosen(val destinationUri: String) : Intent
        data object ImportRequested : Intent
        data class ImportSourceChosen(val sourceUri: String) : Intent
    }

    sealed interface Effect {
        data object NavigationBack : Effect
        data class ShowSnackbar(val message: UiText) : Effect
        data class PrintLabels(val labels: List<LabelData>, val requestedColumns: Int) : Effect
        data class PickTemplateDestination(val suggestedFileName: String) : Effect
        data object PickImportSource : Effect
    }
}
