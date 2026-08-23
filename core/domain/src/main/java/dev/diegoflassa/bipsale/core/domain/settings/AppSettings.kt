package dev.diegoflassa.bipsale.core.domain.settings

import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount

/**
 * Everything the operator configures once and the app then applies on every sale.
 *
 * The PIX discount reuses [ItemDiscount] rather than declaring a second discount type: a discount
 * is a percentage or a fixed amount wherever it appears, and one type means one set of rules to
 * validate and one flattening at the storage seam.
 */
data class AppSettings(
    /** The seller's PIX key — CPF, phone, e-mail or a random key. Empty until configured. */
    val pixKey: String = "",
    /** The seller's name as it should appear in the PIX payload. */
    val pixMerchantName: String = "",
    /** The seller's city as it should appear in the PIX payload. */
    val pixMerchantCity: String = "",
    /** Applied automatically when the operator picks PIX at checkout. */
    val pixDiscount: ItemDiscount = ItemDiscount.None,
    /**
     * Whether a sale opens on the customer form. A shop that never records who bought what should
     * not have to tap past an empty form on every sale, so turning this off starts the cart
     * straight away and the sale is recorded as anonymous.
     */
    val askCustomerInfo: Boolean = true
) {
    val isPixConfigured: Boolean get() = pixKey.isNotBlank()

    /**
     * Which PIX fields are still empty, so a screen can name them instead of saying "not
     * configured" and leaving the operator to guess which one.
     *
     * Only [PixField.KEY] blocks a payload; the other two have fallbacks the spec accepts, but a
     * customer's bank shows them, so an unset one is worth reporting rather than hiding.
     */
    fun missingPixFields(): List<PixField> = buildList {
        if (pixKey.isBlank()) add(PixField.KEY)
        if (pixMerchantName.isBlank()) add(PixField.MERCHANT_NAME)
        if (pixMerchantCity.isBlank()) add(PixField.MERCHANT_CITY)
    }

    companion object {
        val EMPTY = AppSettings()
    }
}

/** A configurable PIX field, for naming the ones an operator has not filled in yet. */
enum class PixField { KEY, MERCHANT_NAME, MERCHANT_CITY }
