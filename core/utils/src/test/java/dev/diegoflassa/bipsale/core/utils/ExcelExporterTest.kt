package dev.diegoflassa.bipsale.core.utils

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val COL_DATE = 0
private const val COL_SALE_ID = 1
private const val COL_CUSTOMER = 2
private const val COL_CPF = 3
private const val COL_PRODUCT_CODE = 4
private const val COL_PRODUCT_NAME = 5
private const val COL_QUANTITY = 6
private const val COL_UNIT_PRICE = 7
private const val COL_ITEM_DISCOUNT = 8
private const val COL_ITEM_TOTAL = 9
private const val COL_SALE_DISCOUNT_PERCENT = 10
private const val COL_SALE_TOTAL = 11
private const val SALE_DATE = 1_700_000_000_000

class ExcelExporterTest {

    private val exporter = ExcelExporter()

    private fun item(
        code: String,
        name: String,
        price: Double,
        quantity: Int = 1,
        discount: ItemDiscount = ItemDiscount.None
    ) = SaleItem(
        saleId = "sale-1",
        productCode = code,
        productName = name,
        unitPrice = price,
        quantity = quantity,
        discount = discount
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
        date = SALE_DATE,
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

        assertThat(header.getCell(COL_DATE).stringCellValue).isEqualTo("Data")
        assertThat(header.getCell(COL_SALE_ID).stringCellValue).isEqualTo("ID Venda")
        assertThat(header.getCell(COL_CUSTOMER).stringCellValue).isEqualTo("Cliente")
        assertThat(header.getCell(COL_CPF).stringCellValue).isEqualTo("CPF")
        assertThat(header.getCell(COL_PRODUCT_CODE).stringCellValue).isEqualTo("Código Produto")
        assertThat(header.getCell(COL_PRODUCT_NAME).stringCellValue).isEqualTo("Produto")
        assertThat(header.getCell(COL_QUANTITY).stringCellValue).isEqualTo("Qtd")
        assertThat(header.getCell(COL_UNIT_PRICE).stringCellValue).isEqualTo("Valor Unit.")
        assertThat(header.getCell(COL_ITEM_DISCOUNT).stringCellValue).isEqualTo("Desconto Item")
        assertThat(header.getCell(COL_ITEM_TOTAL).stringCellValue).isEqualTo("Total Item")
        assertThat(header.getCell(COL_SALE_DISCOUNT_PERCENT).stringCellValue)
            .isEqualTo("Desconto Venda (%)")
        assertThat(header.getCell(COL_SALE_TOTAL).stringCellValue).isEqualTo("Total Venda")
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
            listOf(
                sale(
                    items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50)),
                    finalAmount = 12.50
                )
            )
        )

        val row = rows[1]
        assertThat(row.getCell(COL_SALE_ID).stringCellValue).isEqualTo("sale-1")
        assertThat(row.getCell(COL_CUSTOMER).stringCellValue).isEqualTo("Ana Paula Nogueira")
        assertThat(row.getCell(COL_CPF).stringCellValue).isEqualTo("123.456.789-00")
        assertThat(row.getCell(COL_PRODUCT_CODE).stringCellValue).isEqualTo("CF-200")
        assertThat(row.getCell(COL_PRODUCT_NAME).stringCellValue).isEqualTo("Cafe Premium 200ml")
        assertThat(row.getCell(COL_UNIT_PRICE).numericCellValue).isEqualTo(12.50)
        assertThat(row.getCell(COL_SALE_TOTAL).numericCellValue).isEqualTo(12.50)
    }

    @Test
    fun `the sale date lands in the date column as a real date`() {
        val rows = export(
            listOf(
                sale(items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50)))
            )
        )

        val cell = rows[1].getCell(COL_DATE)
        assertThat(DateUtil.isCellDateFormatted(cell)).isTrue()
        assertThat(cell.dateCellValue.time).isEqualTo(SALE_DATE)
    }

    @Test
    fun `the quantity sold reaches the sheet`() {
        val rows = export(
            listOf(sale(items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50, quantity = 3))))
        )

        assertThat(rows[1].getCell(COL_QUANTITY).numericCellValue).isEqualTo(3.0)
    }

    @Test
    fun `a line discount is reported next to what the line actually charged`() {
        val rows = export(
            listOf(
                sale(
                    items = listOf(
                        item(
                            "CF-200",
                            "Cafe Premium 200ml",
                            10.0,
                            quantity = 2,
                            discount = ItemDiscount.Percentage(25.0)
                        )
                    )
                )
            )
        )

        assertThat(rows[1].getCell(COL_ITEM_DISCOUNT).numericCellValue).isEqualTo(5.0)
        assertThat(rows[1].getCell(COL_ITEM_TOTAL).numericCellValue).isEqualTo(15.0)
    }

    @Test
    fun `the sale total is written once so summing the column cannot double count`() {
        // Repeating the sale total on every line was counting a two-item sale twice.
        val rows = export(
            listOf(
                sale(
                    items = listOf(
                        item("CF-200", "Cafe Premium 200ml", 12.50),
                        item("CH-064", "Chocolate meio amargo 90g", 64.90)
                    ),
                    finalAmount = 69.66
                ).copy(discountPercentage = 10.0)
            )
        )

        assertThat(rows[1].getCell(COL_SALE_TOTAL).numericCellValue).isEqualTo(69.66)
        assertThat(rows[1].getCell(COL_SALE_DISCOUNT_PERCENT).numericCellValue).isEqualTo(10.0)
        assertThat(rows[2].getCell(COL_SALE_TOTAL)).isNull()
        assertThat(rows[2].getCell(COL_SALE_DISCOUNT_PERCENT)).isNull()
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
        assertThat(header.getCell(COL_PRODUCT_CODE).stringCellValue).isEqualTo("Código Produto")
        assertThat(row.getCell(COL_PRODUCT_CODE).stringCellValue).isEqualTo("CF-200")
    }

    @Test
    fun `several sales append after one another`() {
        val rows = export(
            listOf(
                sale(id = "sale-1", items = listOf(item("CF-200", "Cafe Premium 200ml", 12.50))),
                sale(
                    id = "sale-2",
                    customer = "Bruno Carvalho",
                    items = listOf(item("PR-100", "Prancheta oficio", 100.0))
                )
            )
        )

        assertThat(rows).hasSize(3)
        assertThat(rows[2].getCell(COL_CUSTOMER).stringCellValue).isEqualTo("Bruno Carvalho")
        assertThat(rows[2].getCell(COL_SALE_ID).stringCellValue).isEqualTo("sale-2")
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

        assertThat(rows[1].getCell(COL_CUSTOMER).stringCellValue).isEmpty()
        assertThat(rows[1].getCell(COL_CPF).stringCellValue).isEmpty()
    }
}
