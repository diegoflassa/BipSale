package dev.diegoflassa.bipsale.core.data.mapper

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import org.junit.Test

class SaleMapperTest {

    private fun itemEntity(
        discountType: String = SaleItemEntity.DISCOUNT_TYPE_NONE,
        discountValue: Double = 0.0
    ) = SaleItemEntity(
        id = "item-1",
        saleId = "sale-1",
        productCode = "CF-200",
        productName = "Cafe Premium 200ml",
        unitPrice = 12.50,
        quantity = 2,
        discountType = discountType,
        discountValue = discountValue
    )

    private fun saleEntity(paymentMethod: String = "PIX") = SaleEntity(
        id = "sale-1",
        customerName = null,
        customerCpf = null,
        totalAmount = 25.0,
        discountPercentage = 0.0,
        finalAmount = 25.0,
        paymentMethod = paymentMethod,
        date = 1_700_000_000_000
    )

    @Test
    fun `a null customer reads back as an anonymous sale, not as a crash`() {
        val sale = SaleWithItems(saleEntity(), listOf(itemEntity())).toDomain()

        assertThat(sale.customerName).isEmpty()
        assertThat(sale.customerCpf).isEmpty()
    }

    @Test
    fun `an unrecognised payment method falls back rather than failing the read`() {
        val sale = SaleWithItems(saleEntity(paymentMethod = "BITCOIN"), emptyList()).toDomain()

        assertThat(sale.paymentMethod).isEqualTo(PaymentMethod.PIX)
    }

    @Test
    fun `line identity survives the round trip`() {
        val item = SaleItem(
            id = "item-9",
            saleId = "sale-1",
            productCode = "PR-100",
            productName = "Prancheta oficio",
            unitPrice = 100.0,
            quantity = 1
        )

        assertThat(item.toEntity().toDomain()).isEqualTo(item)
    }

    @Test
    fun `a percentage discount survives the round trip`() {
        val item = SaleItem(
            id = "item-9",
            saleId = "sale-1",
            productCode = "PR-100",
            productName = "Prancheta oficio",
            unitPrice = 100.0,
            quantity = 1,
            discount = ItemDiscount.Percentage(15.0)
        )

        val row = item.toEntity()
        assertThat(row.discountType).isEqualTo(SaleItemEntity.DISCOUNT_TYPE_PERCENTAGE)
        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.Percentage(15.0))
    }

    @Test
    fun `a fixed discount survives the round trip`() {
        val item = SaleItem(
            id = "item-9",
            saleId = "sale-1",
            productCode = "PR-100",
            productName = "Prancheta oficio",
            unitPrice = 100.0,
            quantity = 1,
            discount = ItemDiscount.Amount(12.34)
        )

        val row = item.toEntity()
        assertThat(row.discountType).isEqualTo(SaleItemEntity.DISCOUNT_TYPE_AMOUNT)
        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.Amount(12.34))
    }

    @Test
    fun `a row written before per-line discounts existed reads back undiscounted`() {
        assertThat(itemEntity().toDomain().discount).isEqualTo(ItemDiscount.None)
    }

    @Test
    fun `an unknown discount type reads back undiscounted rather than throwing`() {
        val row = itemEntity(discountType = "BUY_ONE_GET_ONE", discountValue = 5.0)

        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.None)
    }

    @Test
    fun `an out-of-range stored percentage is coerced instead of taking the screen down`() {
        // ItemDiscount rejects anything past 100 at construction, so a corrupt or hand-edited row
        // has to be clamped at the seam or every read of that sale would throw.
        val row = itemEntity(discountType = SaleItemEntity.DISCOUNT_TYPE_PERCENTAGE, discountValue = 250.0)

        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.Percentage(100.0))
    }

    @Test
    fun `a negative stored amount is coerced to zero`() {
        val row = itemEntity(discountType = SaleItemEntity.DISCOUNT_TYPE_AMOUNT, discountValue = -5.0)

        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.Amount(0.0))
    }

    @Test
    fun `a non-finite stored discount reads back undiscounted`() {
        val row = itemEntity(
            discountType = SaleItemEntity.DISCOUNT_TYPE_PERCENTAGE,
            discountValue = Double.NaN
        )

        assertThat(row.toDomain().discount).isEqualTo(ItemDiscount.None)
    }
}
