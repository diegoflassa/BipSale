package dev.diegoflassa.bipsale.core.domain.model

import dev.diegoflassa.bipsale.core.domain.util.roundToCents

/** The span the report covers, taken from the sales in it rather than from the operator's filter. */
data class SalesPeriod(val firstSaleDate: Long, val lastSaleDate: Long)

/** What one payment method took in, so a drawer can be reconciled against the card and PIX slips. */
data class PaymentMethodTotal(
    val method: PaymentMethod,
    val saleCount: Int,
    val netAmount: Double
)

data class SalesSummaryMoney(
    /** The lines before any discount. */
    val grossAmount: Double,
    /** What the per-line discounts took off. */
    val itemDiscountAmount: Double,
    /** What the sale-level percentages took off, charged on what the line discounts left. */
    val saleDiscountAmount: Double,
    /** What was actually collected. */
    val netAmount: Double,
    val averageTicket: Double
) {
    /** The sum of the report's "Total Item" column — gross once the line discounts are off it. */
    val amountAfterItemDiscounts: Double
        get() = (grossAmount - itemDiscountAmount).roundToCents()

    val totalDiscountAmount: Double
        get() = (itemDiscountAmount + saleDiscountAmount).roundToCents()

    companion object {
        val EMPTY = SalesSummaryMoney(0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/** Every figure the exported report totals. */
data class SalesSummary(
    val saleCount: Int,
    /** Units sold, not lines — two of one product on one line count as two. */
    val unitCount: Int,
    val period: SalesPeriod?,
    val money: SalesSummaryMoney,
    val byPaymentMethod: List<PaymentMethodTotal>
) {
    companion object {
        val EMPTY = SalesSummary(0, 0, null, SalesSummaryMoney.EMPTY, emptyList())
    }
}

/**
 * Totals a set of already-persisted sales. It reads the amounts each sale recorded rather than
 * re-running [saleTotals] over the lines, so the report can only ever add up to what was charged —
 * including for a sale restored from an archive written under older rules.
 */
fun salesSummary(sales: List<Sale>): SalesSummary {
    if (sales.isEmpty()) return SalesSummary.EMPTY

    val gross = sales.sumOf { it.totalAmount }.roundToCents()
    val itemDiscounts = sales.sumOf { sale -> sale.items.sumOf { it.discountAmount } }.roundToCents()
    val net = sales.sumOf { it.finalAmount }.roundToCents()

    return SalesSummary(
        saleCount = sales.size,
        unitCount = sales.sumOf { sale -> sale.items.sumOf { it.quantity } },
        period = SalesPeriod(sales.minOf { it.date }, sales.maxOf { it.date }),
        money = SalesSummaryMoney(
            grossAmount = gross,
            itemDiscountAmount = itemDiscounts,
            // Whatever the gross lost that the line discounts did not take is the sale-level cut.
            saleDiscountAmount = (gross - itemDiscounts - net).roundToCents(),
            netAmount = net,
            averageTicket = (net / sales.size).roundToCents()
        ),
        byPaymentMethod = sales.groupBy { it.paymentMethod }
            .map { (method, group) ->
                PaymentMethodTotal(
                    method = method,
                    saleCount = group.size,
                    netAmount = group.sumOf { it.finalAmount }.roundToCents()
                )
            }
            .sortedBy { it.method.ordinal }
    )
}
