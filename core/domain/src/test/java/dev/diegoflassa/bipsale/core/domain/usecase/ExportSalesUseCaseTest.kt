package dev.diegoflassa.bipsale.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.export.SalesExportRepository
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class ExportSalesUseCaseTest {

    private class FakeSalesExportRepository(
        private val failWith: Throwable? = null
    ) : SalesExportRepository {
        var destination: String? = null
        var exported: List<Sale> = emptyList()
        var calls = 0

        override suspend fun exportSales(destinationUri: String, sales: List<Sale>) {
            calls++
            destination = destinationUri
            exported = sales
            failWith?.let { throw it }
        }

        override fun suggestedFileName(): String = "vendas.xlsx"
    }

    private fun sale(id: String) = Sale(
        id = id,
        customerName = "Ana Paula Nogueira",
        customerCpf = "123.456.789-00",
        totalAmount = 100.0,
        discountPercentage = 0.0,
        finalAmount = 100.0,
        paymentMethod = PaymentMethod.PIX,
        date = 1_700_000_000_000,
        items = listOf(
            SaleItem(
                id = "item-$id",
                saleId = id,
                productCode = "PR-100",
                productName = "Prancheta oficio",
                unitPrice = 100.0,
                quantity = 1
            )
        )
    )

    @Test
    fun `writes the sales and reports how many reached the file`() = runTest {
        val repository = FakeSalesExportRepository()

        val result = ExportSalesUseCase(repository)("content://docs/vendas.xlsx", listOf(sale("s1"), sale("s2")))

        assertThat(result.getOrThrow()).isEqualTo(2)
        assertThat(repository.destination).isEqualTo("content://docs/vendas.xlsx")
        assertThat(repository.exported).hasSize(2)
    }

    @Test
    fun `refuses an empty export instead of writing a header-only file`() = runTest {
        val repository = FakeSalesExportRepository()

        val result = ExportSalesUseCase(repository)("content://docs/vendas.xlsx", emptyList())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSalesToExport::class.java)
        assertThat(repository.calls).isEqualTo(0)
    }

    @Test
    fun `a write failure is reported rather than looking like a success`() = runTest {
        val repository = FakeSalesExportRepository(failWith = IOException("no space left on device"))

        val result = ExportSalesUseCase(repository)("content://docs/vendas.xlsx", listOf(sale("s1")))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
    }

    @Test
    fun `the suggested file name comes from the repository`() {
        assertThat(ExportSalesUseCase(FakeSalesExportRepository()).suggestedFileName())
            .isEqualTo("vendas.xlsx")
    }
}
