package dev.diegoflassa.bipsale.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SaleTotalsTest {

    private fun item(unitPrice: Double, quantity: Int = 1, discount: ItemDiscount = ItemDiscount.None) =
        SaleItem(
            saleId = "",
            productCode = "CT-A-RoS",
            productName = "Coturno cano alto rosa",
            unitPrice = unitPrice,
            quantity = quantity,
            discount = discount
        )

    @Test
    fun `an empty cart charges nothing`() {
        assertThat(saleTotals(emptyList(), 0.0)).isEqualTo(SaleTotals.EMPTY)
    }

    @Test
    fun `sums the lines by unit price times quantity`() {
        val totals = saleTotals(listOf(item(12.50, quantity = 2), item(64.90)), 0.0)

        assertThat(totals.grossAmount).isEqualTo(89.90)
        assertThat(totals.finalAmount).isEqualTo(89.90)
    }

    @Test
    fun `applies the sale percentage to the gross`() {
        val totals = saleTotals(listOf(item(89.90)), 10.0)

        assertThat(totals.saleDiscountAmount).isEqualTo(8.99)
        assertThat(totals.finalAmount).isEqualTo(80.91)
    }

    @Test
    fun `does not leak binary rounding error into the amount charged`() {
        // 12.50 * 2 + 64.90 taken 10% off is 80.91000000000001 in raw Double arithmetic, and that
        // is the number that would have been persisted as the amount charged.
        val totals = saleTotals(listOf(item(12.50, quantity = 2), item(64.90)), 10.0)

        assertThat(totals.finalAmount).isEqualTo(80.91)
    }

    @Test
    fun `a full sale discount charges nothing but still reports the gross`() {
        val totals = saleTotals(listOf(item(89.90)), 100.0)

        assertThat(totals.grossAmount).isEqualTo(89.90)
        assertThat(totals.finalAmount).isEqualTo(0.0)
    }

    @Test
    fun `charges the sale percentage on what the line discounts left`() {
        // 100.00 less a 10.00 line discount is 90.00; 10% of that is 9.00, never 10.00.
        val totals = saleTotals(listOf(item(100.0, discount = ItemDiscount.Amount(10.0))), 10.0)

        assertThat(totals.itemDiscountAmount).isEqualTo(10.0)
        assertThat(totals.saleDiscountAmount).isEqualTo(9.0)
        assertThat(totals.finalAmount).isEqualTo(81.0)
    }

    @Test
    fun `reports gross before line discounts so the subtotal stays readable`() {
        val totals = saleTotals(
            listOf(
                item(100.0, discount = ItemDiscount.Percentage(50.0)),
                item(50.0)
            ),
            0.0
        )

        assertThat(totals.grossAmount).isEqualTo(150.0)
        assertThat(totals.itemDiscountAmount).isEqualTo(50.0)
        assertThat(totals.finalAmount).isEqualTo(100.0)
    }

    @Test
    fun `rejects a sale discount above one hundred percent`() {
        val thrown = runCatching { saleTotals(listOf(item(10.0)), 101.0) }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a negative sale discount`() {
        val thrown = runCatching { saleTotals(listOf(item(10.0)), -1.0) }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a non-finite sale discount`() {
        val thrown = runCatching { saleTotals(listOf(item(10.0)), Double.NaN) }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }
}
