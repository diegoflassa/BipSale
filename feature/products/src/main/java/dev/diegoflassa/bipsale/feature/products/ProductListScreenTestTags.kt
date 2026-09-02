package dev.diegoflassa.bipsale.feature.products

object ProductListScreenTestTags {
    const val OVERFLOW_BUTTON = "product_list_overflow_button"
    const val DOWNLOAD_TEMPLATE_ITEM = "product_list_download_template"
    const val IMPORT_ITEM = "product_list_import"
    const val ROOT = "product_list_screen"
    const val LIST = "product_list_items"
    const val EMPTY = "product_list_empty"
    const val LOADING = "product_list_loading"
    const val ERROR = "product_list_error"
    const val ADD_BUTTON = "product_list_add_button"
    const val PRINT_ALL_BUTTON = "product_list_print_all_button"
    const val PRINT_SELECTED_BUTTON = "product_list_print_selected_button"
    const val CLEAR_SELECTION_BUTTON = "product_list_clear_selection_button"
    const val IMPORT_CONFIRM_DIALOG = "product_list_import_confirm_dialog"
    const val IMPORT_CONFIRM_ACCEPT = "product_list_import_confirm_accept"
    const val IMPORT_CONFIRM_CANCEL = "product_list_import_confirm_cancel"

    fun productRow(code: String) = "product_list_row_$code"
    fun productCheckbox(code: String) = "product_list_checkbox_$code"
    fun deleteButton(code: String) = "product_list_delete_$code"
}
