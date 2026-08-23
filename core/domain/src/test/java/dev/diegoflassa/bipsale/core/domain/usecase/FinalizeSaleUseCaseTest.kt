package dev.diegoflassa.bipsale.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.model.saleTotals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinalizeSaleUseCaseTest {

    private val repository = FakeSaleRepository()
    private val useCase = FinalizeSaleUseCase(repository)

    private fun item(
        unitPrice: Double,
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

    private suspend fun finalize(
        items: List<SaleItem>,
        discountPercentage: Double = 0.0
    ) = useCase(
        customerName = "Ana Paula Nogueira",
        customerCpf = "123.456.789-00",
        items = items,
        discountPercentage = discountPercentage,
        paymentMethod = PaymentMethod.PIX
    )

    @Test
    fun `persists a sale and reports what was charged`() = runTest {
        val result = finalize(listOf(item(12.50, quantity = 2), item(64.90)), discountPercentage = 10.0)

        assertThat(result.isSuccess).isTrue()
        val sale = result.getOrThrow()
        assertThat(sale.totalAmount).isEqualTo(89.90)
        assertThat(sale.finalAmount).isEqualTo(80.91)
        assertThat(repository.inserted).containsExactly(sale)
    }

    @Test
    fun `charges the same total the cart derived`() = runTest {
        // The cart and the receipt read from one implementation; this pins that they agree.
        val items = listOf(item(19.99, quantity = 3), item(4.49, discount = ItemDiscount.Percentage(25.0)))
        val expected = saleTotals(items, 7.0)

        val sale = finalize(items, discountPercentage = 7.0).getOrThrow()

        assertThat(sale.totalAmount).isEqualTo(expected.grossAmount)
        assertThat(sale.finalAmount).isEqualTo(expected.finalAmount)
    }

    @Test
    fun `stamps every item with the new sale id`() = runTest {
        val sale = finalize(listOf(item(10.0), item(20.0))).getOrThrow()

        assertThat(sale.items.map { it.saleId }.distinct()).containsExactly(sale.id)
    }

    @Test
    fun `keeps each line's own identity so duplicates stay distinguishable`() = runTest {
        val sale = finalize(listOf(item(10.0), item(10.0))).getOrThrow()

        assertThat(sale.items.map { it.id }.toSet()).hasSize(2)
    }

    @Test
    fun `carries per-line discounts through to the persisted sale`() = runTest {
        val sale = finalize(listOf(item(100.0, discount = ItemDiscount.Amount(10.0)))).getOrThrow()

        assertThat(sale.items.single().discount).isEqualTo(ItemDiscount.Amount(10.0))
        assertThat(sale.finalAmount).isEqualTo(90.0)
    }

    @Test
    fun `refuses an empty cart instead of recording a zero sale`() = runTest {
        val result = finalize(emptyList())

        assertThat(result.isFailure).isTrue()
        assertThat(repository.inserted).isEmpty()
    }

    @Test
    fun `refuses a discount above one hundred percent`() = runTest {
        val result = finalize(listOf(item(10.0)), discountPercentage = 120.0)

        assertThat(result.isFailure).isTrue()
        assertThat(repository.inserted).isEmpty()
    }

    @Test
    fun `reports a repository failure instead of claiming the sale landed`() = runTest {
        val failing = FinalizeSaleUseCase(FakeSaleRepository(failWith = IllegalStateException("disk full")))

        val result = failing(
            customerName = "Ana",
            customerCpf = "",
            items = listOf(item(10.0)),
            discountPercentage = 0.0,
            paymentMethod = PaymentMethod.CASH
        )

        assertThat(result.isFailure).isTrue()
    }
}
