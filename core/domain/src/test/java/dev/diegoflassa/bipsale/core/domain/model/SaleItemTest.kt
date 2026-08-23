package dev.diegoflassa.bipsale.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SaleItemTest {

    private fun item(
        unitPrice: Double = 100.0,
        quantity: Int = 1,
        discount: ItemDiscount = ItemDiscount.None
    ) = SaleItem(
        saleId = "",
        productCode = "CT-A-RoS",
        productName = "Coturno cano alto rosa",
        unitPrice = unitPrice,
        quantity = quantity,
        discount = discount
    )

    @Test
    fun `an undiscounted line charges unit price times quantity`() {
        val line = item(unitPrice = 12.50, quantity = 3)

        assertThat(line.grossAmount).isEqualTo(37.50)
        assertThat(line.discountAmount).isEqualTo(0.0)
        assertThat(line.netAmount).isEqualTo(37.50)
    }

    @Test
    fun `a percentage discount comes off the whole line, not off one unit`() {
        val line = item(unitPrice = 50.0, quantity = 2, discount = ItemDiscount.Percentage(10.0))

        assertThat(line.discountAmount).isEqualTo(10.0)
        assertThat(line.netAmount).isEqualTo(90.0)
    }

    @Test
    fun `a fixed discount comes off the line total`() {
        val line = item(unitPrice = 50.0, quantity = 2, discount = ItemDiscount.Amount(15.0))

        assertThat(line.discountAmount).isEqualTo(15.0)
        assertThat(line.netAmount).isEqualTo(85.0)
    }

    @Test
    fun `a fixed discount larger than the line never pays the customer to buy it`() {
        val line = item(unitPrice = 10.0, discount = ItemDiscount.Amount(999.0))

        assertThat(line.discountAmount).isEqualTo(10.0)
        assertThat(line.netAmount).isEqualTo(0.0)
    }

    @Test
    fun `rounds a percentage that does not divide evenly`() {
        val line = item(unitPrice = 10.0, quantity = 3, discount = ItemDiscount.Percentage(33.0))

        assertThat(line.discountAmount).isEqualTo(9.90)
        assertThat(line.netAmount).isEqualTo(20.10)
    }

    @Test
    fun `two lines of the same product are separately addressable`() {
        val first = item()
        val second = item()

        assertThat(first.id).isNotEqualTo(second.id)
    }

    @Test
    fun `rejects a percentage above one hundred`() {
        val thrown = runCatching { ItemDiscount.Percentage(101.0) }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a negative percentage`() {
        assertThat(runCatching { ItemDiscount.Percentage(-1.0) }.isFailure).isTrue()
    }

    @Test
    fun `rejects a negative fixed discount`() {
        assertThat(runCatching { ItemDiscount.Amount(-0.01) }.isFailure).isTrue()
    }

    @Test
    fun `rejects a non-finite discount`() {
        assertThat(runCatching { ItemDiscount.Percentage(Double.NaN) }.isFailure).isTrue()
        assertThat(runCatching { ItemDiscount.Amount(Double.POSITIVE_INFINITY) }.isFailure).isTrue()
    }
}
