package dev.diegoflassa.bipsale.core.domain.model

import dev.diegoflassa.bipsale.core.domain.util.roundToCents
import java.util.UUID

data class SaleItem(
    /** Stable line identity — two scans of the same product are two lines that discount apart. */
    val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productCode: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val discount: ItemDiscount = ItemDiscount.None
) {
    val grossAmount: Double
        get() = (unitPrice * quantity).roundToCents()

    val discountAmount: Double
        get() = when (discount) {
            is ItemDiscount.None -> 0.0
            is ItemDiscount.Percentage ->
                (grossAmount * discount.percent / ItemDiscount.MAX_PERCENT).roundToCents()
            // A fixed discount larger than the line would otherwise pay the customer to buy it.
            is ItemDiscount.Amount -> discount.amount.coerceAtMost(grossAmount).roundToCents()
        }

    val netAmount: Double
        get() = (grossAmount - discountAmount).roundToCents()
}
