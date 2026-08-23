package dev.diegoflassa.bipsale.feature.sales

object SalesScreenTestTags {
    const val ROOT = "sales_screen"
    const val EMPTY_MESSAGE = "sales_empty_message"
    const val ITEM_LIST = "sales_item_list"
    const val SCAN_BUTTON = "sales_scan_button"
    const val ADD_PRODUCT_BUTTON = "sales_add_product_button"
    const val CATALOG_DIALOG = "sales_catalog_dialog"
    const val MANUAL_CODE_DIALOG = "sales_manual_code_dialog"
    const val MANUAL_CODE_FIELD = "sales_manual_code_field"
    const val DISCOUNT_BUTTON = "sales_discount_button"
    const val DISCOUNT_DIALOG = "sales_discount_dialog"
    const val DISCOUNT_FIELD = "sales_discount_field"
    const val ITEM_DISCOUNT_DIALOG = "sales_item_discount_dialog"
    const val ITEM_DISCOUNT_FIELD = "sales_item_discount_field"
    const val PAYMENT_METHOD_DROPDOWN = "sales_payment_method_dropdown"
    const val SUBTOTAL_VALUE = "sales_subtotal_value"
    const val TOTAL_VALUE = "sales_total_value"
    const val PIX_QR_CARD = "sales_pix_qr_card"
    const val PIX_MISSING_CARD = "sales_pix_missing_card"
    const val COMPLETED_PIX_DIALOG = "sales_completed_pix_dialog"
    const val PRODUCT_DETAIL_SHEET = "sales_product_detail_sheet"
    const val PRODUCT_DETAIL_DISCOUNT = "sales_product_detail_discount"
    const val FINALIZE_BUTTON = "sales_finalize_button"

    fun catalogRow(code: String) = "sales_catalog_row_$code"
}
