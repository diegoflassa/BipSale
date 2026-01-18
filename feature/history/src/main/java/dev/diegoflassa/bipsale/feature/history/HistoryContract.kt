package dev.diegoflassa.bipsale.feature.history

import dev.diegoflassa.bipsale.core.domain.model.Sale

class HistoryContract {
    data class State(
        val sales: List<Sale> = emptyList(),
        val selectedSaleIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val isLoading: Boolean = false
    )

    sealed interface Intent {
        data class SearchSales(val query: String) : Intent
        data class LoadSalesByDate(val start: Long, val end: Long) : Intent
        data object RefreshSales : Intent
        data class ToggleSaleSelection(val id: String) : Intent
        data object ClearSelection : Intent
    }

    sealed interface Effect {
        data class ShowError(val message: String) : Effect
    }
}
