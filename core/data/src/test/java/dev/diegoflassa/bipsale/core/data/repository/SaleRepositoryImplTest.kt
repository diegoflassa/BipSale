package dev.diegoflassa.bipsale.core.data.repository

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaleRepositoryImplTest {

    private val boom = IllegalStateException("database is closed")

    private val saleEntity = SaleEntity(
        id = "sale-1",
        customerName = "Ana Paula Nogueira",
        customerCpf = "123.456.789-00",
        totalAmount = 89.90,
        discountPercentage = 10.0,
        finalAmount = 80.91,
        paymentMethod = "PIX",
        date = 1_700_000_000_000
    )

    private val itemEntity = SaleItemEntity(
        id = "item-1",
        saleId = "sale-1",
        productCode = "CF-200",
        productName = "Cafe Premium 200ml",
        unitPrice = 12.50,
        quantity = 2
    )

    private val stored = SaleWithItems(saleEntity, listOf(itemEntity))

    private val sale = Sale(
        id = "sale-2",
        customerName = "Bruno Carvalho",
        customerCpf = "987.654.321-00",
        totalAmount = 100.0,
        discountPercentage = 0.0,
        finalAmount = 90.0,
        paymentMethod = PaymentMethod.CASH,
        date = 1_700_000_100_000,
        items = listOf(
            SaleItem(
                id = "item-2",
                saleId = "sale-2",
                productCode = "PR-100",
                productName = "Prancheta oficio",
                unitPrice = 100.0,
                quantity = 1,
                discount = ItemDiscount.Amount(10.0)
            )
        )
    )

    private fun repository(dao: FakeSaleDao) = SaleRepositoryImpl(dao)

    @Test
    fun `maps stored sales onto domain sales with their items`() = runTest {
        val sales = repository(FakeSaleDao(listOf(stored))).getAllSales().first()

        val loaded = sales.single()
        assertThat(loaded.id).isEqualTo("sale-1")
        assertThat(loaded.paymentMethod).isEqualTo(PaymentMethod.PIX)
        assertThat(loaded.finalAmount).isEqualTo(80.91)
        assertThat(loaded.items.single().productCode).isEqualTo("CF-200")
    }

    @Test
    fun `reads a sale back by its id`() = runTest {
        val loaded = repository(FakeSaleDao(listOf(stored))).getSaleById("sale-1")

        assertThat(loaded?.customerName).isEqualTo("Ana Paula Nogueira")
    }

    @Test
    fun `returns null for a sale id that does not exist`() = runTest {
        assertThat(repository(FakeSaleDao(listOf(stored))).getSaleById("missing")).isNull()
    }

    @Test
    fun `swallows a read failure so the history screen stays usable`() = runTest {
        val loaded = repository(FakeSaleDao(failWith = boom)).getSaleById("sale-1")

        assertThat(loaded).isNull()
    }

    @Test
    fun `persists a sale together with its items`() = runTest {
        val dao = FakeSaleDao()

        repository(dao).insertFullSale(sale)

        assertThat(dao.sales.single().id).isEqualTo("sale-2")
        assertThat(dao.items.single().saleId).isEqualTo("sale-2")
    }

    @Test
    fun `carries a per-line discount into storage`() = runTest {
        val dao = FakeSaleDao()

        repository(dao).insertFullSale(sale)

        val row = dao.items.single()
        assertThat(row.discountType).isEqualTo(SaleItemEntity.DISCOUNT_TYPE_AMOUNT)
        assertThat(row.discountValue).isEqualTo(10.0)
    }

    @Test
    fun `stock moves in the same write as the sale that moved it`() = runTest {
        // A committed sale whose stock decrement was rolled back is inventory nobody can
        // reconcile, which is why the decrement rides the sale's own transaction.
        val dao = FakeSaleDao()

        repository(dao).insertFullSale(sale)

        val line = sale.items.single()
        assertThat(dao.stockTaken).containsEntry(line.productCode, line.quantity)
    }

    @Test
    fun `rethrows a write failure instead of reporting a sale that was never recorded`() = runTest {
        // The one failure the app must never swallow: the UI navigates away on success, and a
        // sale that silently did not land is money nobody can reconcile.
        val thrown = runCatching {
            repository(FakeSaleDao(failWith = boom)).insertFullSale(sale)
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(boom)
    }

    @Test
    fun `searches sales by customer name`() = runTest {
        val found = repository(FakeSaleDao(listOf(stored))).searchSales("Ana").first()

        assertThat(found.single().id).isEqualTo("sale-1")
    }

    @Test
    fun `searches sales by cpf`() = runTest {
        val found = repository(FakeSaleDao(listOf(stored))).searchSales("987").first()

        assertThat(found).isEmpty()
    }

    @Test
    fun `filters sales by date range`() = runTest {
        val repository = repository(FakeSaleDao(listOf(stored)))

        assertThat(repository.getSalesByDateRange(0, 1_700_000_000_000).first()).hasSize(1)
        assertThat(repository.getSalesByDateRange(0, 1_699_999_999_999).first()).isEmpty()
    }
}
