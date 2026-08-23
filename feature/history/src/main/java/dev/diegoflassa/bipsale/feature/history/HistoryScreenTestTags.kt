package dev.diegoflassa.bipsale.feature.history

object HistoryScreenTestTags {
    const val ROOT = "history_screen"
    const val SEARCH_FIELD = "history_search_field"
    const val LOADING = "history_loading"
    const val LIST = "history_list"
    const val EMPTY = "history_empty"
    const val CLEAR_SELECTION_BUTTON = "history_clear_selection_button"
    const val EXPORT_SELECTED_BUTTON = "history_export_selected_button"

    fun saleRow(id: String) = "history_row_$id"
}
