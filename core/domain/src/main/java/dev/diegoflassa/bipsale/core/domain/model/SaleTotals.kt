package dev.diegoflassa.bipsale.core.domain.model

import dev.diegoflassa.bipsale.core.domain.util.roundToCents

/** Every number the cart shows and the sale persists, derived once so the two cannot disagree. */
data class SaleTotals(
    /** The lines before any discount — the "subtotal" line. */
    val grossAmount: Double,
    /** What the per-line discounts took off. */
    val itemDiscountAmount: Double,
    /** What the sale-level percentage took off, charged on what the line discounts left. */
    val saleDiscountAmount: Double,
    /** What the customer pays. */
    val finalAmount: Double
) {
    companion object {
        val EMPTY = SaleTotals(0.0, 0.0, 0.0, 0.0)
    }
}

const val MAX_SALE_DISCOUNT_PERCENTAGE: Double = 100.0

/**
 * The single implementation of the money maths. The cart and [dev.diegoflassa.bipsale.core.domain.usecase.FinalizeSaleUseCase]
 * both call it, because a second copy is how a screen ends up showing a total the receipt disagrees with.
 */
fun saleTotals(items: List<SaleItem>, discountPercentage: Double): SaleTotals {
    require(discountPercentage.isFinite() && discountPercentage in 0.0..MAX_SALE_DISCOUNT_PERCENTAGE) {
        "Sale discount must be between 0 and $MAX_SALE_DISCOUNT_PERCENTAGE, was $discountPercentage."
    }
    if (items.isEmpty()) return SaleTotals.EMPTY

    val gross = items.sumOf { it.grossAmount }.roundToCents()
    val itemDiscounts = items.sumOf { it.discountAmount }.roundToCents()
    val afterItemDiscounts = (gross - itemDiscounts).roundToCents()
    val saleDiscount =
        (afterItemDiscounts * discountPercentage / MAX_SALE_DISCOUNT_PERCENTAGE).roundToCents()

    return SaleTotals(
        grossAmount = gross,
        itemDiscountAmount = itemDiscounts,
        saleDiscountAmount = saleDiscount,
        finalAmount = (afterItemDiscounts - saleDiscount).roundToCents()
    )
}
