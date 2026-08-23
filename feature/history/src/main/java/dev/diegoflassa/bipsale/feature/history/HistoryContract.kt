package dev.diegoflassa.bipsale.feature.history

import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.settings.PixField
import dev.diegoflassa.bipsale.core.ui.util.UiText

class HistoryContract {
    data class State(
        val sales: List<Sale> = emptyList(),
        val selectedSaleIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val isLoading: Boolean = false,
        val isExporting: Boolean = false,
        /** The sale whose PIX code is on screen, so a customer can pay one that was already rung up. */
        val pixSaleId: String? = null,
        val pixPayload: String? = null,
        val missingPixFields: List<PixField> = emptyList()
    ) {
        val isShowingPix: Boolean get() = pixSaleId != null
    }

    /** Which sales an export covers. Stateless cases, so an enum. */
    enum class ExportScope { ALL, SELECTED }

    sealed interface Intent {
        data class SearchSales(val query: String) : Intent
        data class LoadSalesByDate(val start: Long, val end: Long) : Intent
        data object RefreshSales : Intent
        data class ToggleSaleSelection(val id: String) : Intent
        data object ClearSelection : Intent

        /** Asks for a destination; nothing is written until one comes back. */
        data class ExportRequested(val scope: ExportScope) : Intent
        data class ExportDestinationChosen(val destinationUri: String) : Intent
        data object ExportCancelled : Intent

        /** Rebuilds the PIX code for a sale that is already recorded. */
        data class ShowSalePix(val saleId: String) : Intent
        data object HideSalePix : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: UiText) : Effect
        data class PickExportDestination(val suggestedFileName: String) : Effect
    }
}
