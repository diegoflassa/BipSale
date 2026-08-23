package dev.diegoflassa.bipsale.core.utils

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExcelExporterTest {

    private val exporter = ExcelExporter()

    private fun item(code: String, name: String, price: Double, quantity: Int = 1) = SaleItem(
        saleId = "sale-1",
        productCode = code,
        productName = name,
        unitPrice = price,
        quantity = quantity
    )

    private fun sale(
        id: String = "sale-1",
        customer: String = "Ana Paula Nogueira",
        cpf: String = "123.456.789-00",
        items: List<SaleItem>,
        finalAmount: Double = 100.0
    ) = Sale(
        id = id,
        customerName = customer,
        customerCpf = cpf,
        totalAmount = finalAmount,
        discountPercentage = 0.0,
        finalAmount = finalAmount,
        paymentMethod = PaymentMethod.PIX,
        date = 1_700_000_000_000,
        items = items
    )

    private fun export(sales: List<Sale>): List<Row> {
        val out = ByteArrayOutputStream()
        exporter.exportSalesToExcel(out, sales)
        val sheet = XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).getSheetAt(0)
        return (0..sheet.lastRowNum).map { sheet.getRow(it) }
    }

    @Test
    fun `writes a header row naming every column`() {
        val header = export(emptyList()).single()

        assertThat(header.getCell(0).stringCellValue).isEqualTo("Cliente")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("CPF")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("Código Produto")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("Produto")
        assertThat(header.getCell(4).stringCellValue).isEqualTo("Valor Unit.")
        assertThat(header.getCell(5).stringCellValue).isEqualTo("Total Venda")
    }

    @Test
    fun `writes one row per sale item`() {
        val rows = export(
            listOf(
                sale(
                    items = listOf(
                        item("CF-200", "Cafe Premium 200ml", 12.50),
                        item("CH-064", "Chocolate meio amargo 90g", 64.90)
                    )
                )
            )
        )

        assertThat(rows).hasSize(3)
    }

    @Test
    fun `each row carries the customer and the item it belongs to`() {
        val rows = export(
            listOf(sale(items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50)), finalAmount = 12.50))
        )

        val row = rows[1]
        assertThat(row.getCell(0).stringCellValue).isEqualTo("Ana Paula Nogueira")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("123.456.789-00")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("CF-200")
        assertThat(row.getCell(3).stringCellValue).isEqualTo("Cafe Premium 200ml")
        assertThat(row.getCell(4).numericCellValue).isEqualTo(12.50)
        assertThat(row.getCell(5).numericCellValue).isEqualTo(12.50)
    }

    @Test
    fun `values stay under the header they belong to`() {
        // The header list and the value writer read the same enum; this pins that they cannot
        // drift, which would silently file every CPF under the customer column.
        val rows = export(
            listOf(sale(items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50))))
        )

        val header = rows[0]
        val row = rows[1]
        assertThat(header.getCell(2).stringCellValue).isEqualTo("Código Produto")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("CF-200")
    }

    @Test
    fun `several sales append after one another`() {
        val rows = export(
            listOf(
                sale(id = "sale-1", items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50))),
                sale(id = "sale-2", customer = "Bruno Carvalho", items = listOf(item("PR-100", "Prancheta oficio", 100.0)))
            )
        )

        assertThat(rows).hasSize(3)
        assertThat(rows[2].getCell(0).stringCellValue).isEqualTo("Bruno Carvalho")
    }

    @Test
    fun `a sale with no items contributes no rows`() {
        val rows = export(listOf(sale(items = emptyList())))

        assertThat(rows).hasSize(1)
    }

    @Test
    fun `an anonymous sale exports with empty customer fields rather than failing`() {
        val rows = export(
            listOf(
                sale(
                    customer = "",
                    cpf = "",
                    items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50))
                )
            )
        )

        assertThat(rows[1].getCell(0).stringCellValue).isEmpty()
        assertThat(rows[1].getCell(1).stringCellValue).isEmpty()
    }
}
