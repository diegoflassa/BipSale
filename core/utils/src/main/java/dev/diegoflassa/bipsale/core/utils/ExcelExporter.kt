package dev.diegoflassa.bipsale.core.utils

import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDTH_NARROW_CHARS = 8
private const val WIDTH_NORMAL_CHARS = 18
private const val WIDTH_WIDE_CHARS = 34

@Singleton
class ExcelExporter @Inject constructor() {

    /**
     * One row per sale item. The header and the values are written from the same enum so a column
     * added in one place cannot silently shift the other out of alignment.
     */
    fun exportSalesToExcel(
        outputStream: OutputStream,
        sales: List<Sale>
    ) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet(SHEET_NAME)
            val dateStyle = workbook.createCellStyle().apply {
                dataFormat = workbook.creationHelper.createDataFormat().getFormat(DATE_FORMAT)
            }

            writeHeader(sheet)

            // The sale-level columns are left off every line but a sale's first. Repeating them
            // would make a SUM over the column count a multi-item sale once per item.
            val firstLineColumns = SalesColumn.entries
            val laterLineColumns = firstLineColumns - SALE_LEVEL_COLUMNS

            var rowNum = 1
            sales.forEach { sale ->
                sale.items.forEachIndexed { index, item ->
                    val columns = if (index == 0) firstLineColumns else laterLineColumns
                    writeRow(sheet.createRow(rowNum++), sale, item, columns, dateStyle)
                }
            }

            workbook.write(outputStream)
        }
    }

    private fun writeHeader(sheet: Sheet) {
        val headerRow = sheet.createRow(0)
        SalesColumn.entries.forEach { column ->
            headerRow.createCell(column.ordinal).setCellValue(column.header)
            // Widths are declared rather than auto-sized: POI's autoSizeColumn measures text
            // through java.awt, which does not exist on Android and throws at runtime.
            sheet.setColumnWidth(column.ordinal, column.widthInChars * CHAR_WIDTH_UNITS)
        }
    }

    private fun writeRow(
        row: Row,
        sale: Sale,
        item: SaleItem,
        columns: List<SalesColumn>,
        dateStyle: CellStyle
    ) {
        columns.forEach { column ->
            val cell = row.createCell(column.ordinal)
            when (column) {
                SalesColumn.DATE -> {
                    cell.setCellValue(Date(sale.date))
                    cell.cellStyle = dateStyle
                }

                SalesColumn.SALE_ID -> cell.setCellValue(sale.id)
                SalesColumn.CUSTOMER -> cell.setCellValue(sale.customerName)
                SalesColumn.CPF -> cell.setCellValue(sale.customerCpf)
                SalesColumn.PRODUCT_CODE -> cell.setCellValue(item.productCode)
                SalesColumn.PRODUCT_NAME -> cell.setCellValue(item.productName)
                SalesColumn.QUANTITY -> cell.setCellValue(item.quantity.toDouble())
                SalesColumn.UNIT_PRICE -> cell.setCellValue(item.unitPrice)
                SalesColumn.ITEM_DISCOUNT -> cell.setCellValue(item.discountAmount)
                SalesColumn.ITEM_TOTAL -> cell.setCellValue(item.netAmount)
                SalesColumn.SALE_DISCOUNT_PERCENT -> cell.setCellValue(sale.discountPercentage)
                SalesColumn.SALE_TOTAL -> cell.setCellValue(sale.finalAmount)
            }
        }
    }

    private enum class SalesColumn(val header: String, val widthInChars: Int) {
        DATE("Data", WIDTH_NORMAL_CHARS),
        SALE_ID("ID Venda", WIDTH_WIDE_CHARS),
        CUSTOMER("Cliente", WIDTH_WIDE_CHARS),
        CPF("CPF", WIDTH_NORMAL_CHARS),
        PRODUCT_CODE("Código Produto", WIDTH_NORMAL_CHARS),
        PRODUCT_NAME("Produto", WIDTH_WIDE_CHARS),
        QUANTITY("Qtd", WIDTH_NARROW_CHARS),
        UNIT_PRICE("Valor Unit.", WIDTH_NORMAL_CHARS),
        ITEM_DISCOUNT("Desconto Item", WIDTH_NORMAL_CHARS),
        ITEM_TOTAL("Total Item", WIDTH_NORMAL_CHARS),
        SALE_DISCOUNT_PERCENT("Desconto Venda (%)", WIDTH_NORMAL_CHARS),
        SALE_TOTAL("Total Venda", WIDTH_NORMAL_CHARS)
    }

    private companion object {
        const val SHEET_NAME = "Vendas"
        const val DATE_FORMAT = "dd/mm/yyyy hh:mm"
        const val CHAR_WIDTH_UNITS = 256

        val SALE_LEVEL_COLUMNS =
            setOf(SalesColumn.SALE_DISCOUNT_PERCENT, SalesColumn.SALE_TOTAL)
    }
}
