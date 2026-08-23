package dev.diegoflassa.bipsale.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SalesSummaryTest {

    private fun item(
        price: Double,
        quantity: Int = 1,
        discount: ItemDiscount = ItemDiscount.None
    ) = SaleItem(
        saleId = "sale-1",
        productCode = "CF-200",
        productName = "Cafe Premium 200ml",
        unitPrice = price,
        quantity = quantity,
        discount = discount
    )

    private fun sale(
        id: String = "sale-1",
        items: List<SaleItem>,
        discountPercentage: Double = 0.0,
        method: PaymentMethod = PaymentMethod.PIX,
        date: Long = 1_700_000_000_000
    ): Sale {
        val totals = saleTotals(items, discountPercentage)
        return Sale(
            id = id,
            customerName = "Ana Paula Nogueira",
            customerCpf = "123.456.789-00",
            totalAmount = totals.grossAmount,
            discountPercentage = discountPercentage,
            finalAmount = totals.finalAmount,
            paymentMethod = method,
            date = date,
            items = items
        )
    }

    @Test
    fun `no sales totals to nothing rather than dividing by zero`() {
        assertThat(salesSummary(emptyList())).isEqualTo(SalesSummary.EMPTY)
    }

    @Test
    fun `the gross, the discounts and the net add up to what was charged`() {
        val summary = salesSummary(
            listOf(
                sale(
                    items = listOf(
                        item(10.0, quantity = 2, discount = ItemDiscount.Percentage(25.0)),
                        item(30.0)
                    ),
                    discountPercentage = 10.0
                )
            )
        )

        // Gross 50, line discount 5, so 45 before the sale-level 10% takes 4.50.
        assertThat(summary.money.grossAmount).isEqualTo(50.0)
        assertThat(summary.money.itemDiscountAmount).isEqualTo(5.0)
        assertThat(summary.money.amountAfterItemDiscounts).isEqualTo(45.0)
        assertThat(summary.money.saleDiscountAmount).isEqualTo(4.50)
        assertThat(summary.money.totalDiscountAmount).isEqualTo(9.50)
        assertThat(summary.money.netAmount).isEqualTo(40.50)
    }

    @Test
    fun `units are counted, not lines`() {
        val summary = salesSummary(
            listOf(sale(items = listOf(item(10.0, quantity = 3), item(5.0, quantity = 2))))
        )

        assertThat(summary.unitCount).isEqualTo(5)
        assertThat(summary.saleCount).isEqualTo(1)
    }

    @Test
    fun `the period spans the earliest and latest sale, whatever order they arrive in`() {
        val summary = salesSummary(
            listOf(
                sale(id = "s1", items = listOf(item(10.0)), date = 3_000),
                sale(id = "s2", items = listOf(item(10.0)), date = 1_000),
                sale(id = "s3", items = listOf(item(10.0)), date = 2_000)
            )
        )

        assertThat(summary.period?.firstSaleDate).isEqualTo(1_000)
        assertThat(summary.period?.lastSaleDate).isEqualTo(3_000)
    }

    @Test
    fun `the average ticket is the net over the sale count, rounded to cents`() {
        val summary = salesSummary(
            listOf(
                sale(id = "s1", items = listOf(item(10.0))),
                sale(id = "s2", items = listOf(item(10.0))),
                sale(id = "s3", items = listOf(item(10.01)))
            )
        )

        // 30.01 / 3 = 10.003333…
        assertThat(summary.money.averageTicket).isEqualTo(10.0)
    }

    @Test
    fun `each payment method carries its own count and total`() {
        val summary = salesSummary(
            listOf(
                sale(id = "s1", items = listOf(item(10.0)), method = PaymentMethod.PIX),
                sale(id = "s2", items = listOf(item(25.0)), method = PaymentMethod.PIX),
                sale(id = "s3", items = listOf(item(40.0)), method = PaymentMethod.CASH)
            )
        )

        val pix = summary.byPaymentMethod.single { it.method == PaymentMethod.PIX }
        val cash = summary.byPaymentMethod.single { it.method == PaymentMethod.CASH }
        assertThat(pix.saleCount).isEqualTo(2)
        assertThat(pix.netAmount).isEqualTo(35.0)
        assertThat(cash.saleCount).isEqualTo(1)
        assertThat(cash.netAmount).isEqualTo(40.0)
    }

    @Test
    fun `the payment breakdown adds back up to the net`() {
        val sales = listOf(
            sale(id = "s1", items = listOf(item(10.0)), method = PaymentMethod.PIX),
            sale(id = "s2", items = listOf(item(25.0)), method = PaymentMethod.CREDIT_CARD),
            sale(id = "s3", items = listOf(item(40.0)), method = PaymentMethod.CASH)
        )

        val summary = salesSummary(sales)

        assertThat(summary.byPaymentMethod.sumOf { it.netAmount })
            .isEqualTo(summary.money.netAmount)
        assertThat(summary.byPaymentMethod.sumOf { it.saleCount }).isEqualTo(summary.saleCount)
    }

    @Test
    fun `a method nobody paid with is left out rather than reported as zero`() {
        val summary = salesSummary(
            listOf(sale(items = listOf(item(10.0)), method = PaymentMethod.PIX))
        )

        assertThat(summary.byPaymentMethod.map { it.method })
            .containsExactly(PaymentMethod.PIX)
    }

    @Test
    fun `binary addition never leaks into a total`() {
        // 0.1 + 0.2 sums to 0.30000000000000004 before rounding.
        val summary = salesSummary(
            listOf(
                sale(id = "s1", items = listOf(item(0.1))),
                sale(id = "s2", items = listOf(item(0.2)))
            )
        )

        assertThat(summary.money.netAmount).isEqualTo(0.30)
        assertThat(summary.money.grossAmount).isEqualTo(0.30)
    }
}
