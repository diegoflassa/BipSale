package dev.diegoflassa.bipsale.core.domain.settings

import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount

/**
 * Everything the operator configures once and the app then applies on every sale.
 *
 * PIX key, merchant name and city are no longer user-configurable — they live as build constants
 * in [dev.diegoflassa.bipsale.core.domain.pix.PixDefaults]. The discount reuses [ItemDiscount]
 * rather than declaring a second discount type: a discount is a percentage or a fixed amount
 * wherever it appears, and one type means one set of rules to validate and one flattening at the
 * storage seam.
 */
data class AppSettings(
    /** Applied automatically when the operator picks PIX at checkout. */
    val pixDiscount: ItemDiscount = ItemDiscount.None,
    /**
     * Whether a sale opens on the customer form. A shop that never records who bought what should
     * not have to tap past an empty form on every sale, so turning this off starts the cart
     * straight away and the sale is recorded as anonymous.
     */
    val askCustomerInfo: Boolean = true,
    /** Number of columns in the QR label print grid. Minimum 1, maximum 6. */
    val qrLabelColumns: Int = DEFAULT_QR_LABEL_COLUMNS
) {
    companion object {
        const val DEFAULT_QR_LABEL_COLUMNS = 4
        const val MIN_QR_LABEL_COLUMNS = 1
        const val MAX_QR_LABEL_COLUMNS = 6
        val EMPTY = AppSettings()
    }
}
