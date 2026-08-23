package dev.diegoflassa.bipsale.core.utils

import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethodTotal
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.model.SalesSummary
import dev.diegoflassa.bipsale.core.domain.model.salesSummary
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
     * A "Vendas" sheet with one row per sale item and a totals row under it, and a "Resumo" sheet
     * carrying the same totals broken down by payment method.
     *
     * The header and the values are written from the same enum so a column added in one place
     * cannot silently shift the other out of alignment.
     *
     * Returns the totals it wrote, so the caller can log what left the app without summing twice.
     */
    fun exportSalesToExcel(
        outputStream: OutputStream,
        sales: List<Sale>
    ): SalesSummary {
        XSSFWorkbook().use { workbook ->
            val dateStyle = workbook.createCellStyle().apply {
                dataFormat = workbook.creationHelper.createDataFormat().getFormat(DATE_FORMAT)
            }
            val boldStyle = workbook.createCellStyle().apply {
                setFont(workbook.createFont().apply { bold = true })
            }
            val summary = salesSummary(sales)

            val sheet = workbook.createSheet(SALES_SHEET_NAME)
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

            if (sales.isNotEmpty()) {
                // A blank separator keeps the totals out of a sort or filter over the data range.
                writeTotalsRow(sheet, rowNum + 1, summary, boldStyle)
            }

            writeSummarySheet(workbook, summary, boldStyle, dateStyle)

            workbook.write(outputStream)
            return summary
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

    /**
     * Only the columns whose sum means something. A unit price and a discount percentage are
     * per-line rates, and adding them up produces a number that looks like money and is not.
     */
    private fun writeTotalsRow(
        sheet: Sheet,
        rowIndex: Int,
        summary: SalesSummary,
        boldStyle: CellStyle
    ) {
        val row = sheet.createRow(rowIndex)
        fun cell(column: SalesColumn) =
            row.createCell(column.ordinal).apply { cellStyle = boldStyle }

        cell(SalesColumn.DATE).setCellValue(TOTALS_LABEL)
        cell(SalesColumn.QUANTITY).setCellValue(summary.unitCount.toDouble())
        cell(SalesColumn.ITEM_DISCOUNT).setCellValue(summary.money.itemDiscountAmount)
        cell(SalesColumn.ITEM_TOTAL).setCellValue(summary.money.amountAfterItemDiscounts)
        cell(SalesColumn.SALE_TOTAL).setCellValue(summary.money.netAmount)
    }

    private fun writeSummarySheet(
        workbook: XSSFWorkbook,
        summary: SalesSummary,
        boldStyle: CellStyle,
        dateStyle: CellStyle
    ) {
        val sheet = workbook.createSheet(SUMMARY_SHEET_NAME)
        sheet.setColumnWidth(LABEL_COLUMN, WIDTH_WIDE_CHARS * CHAR_WIDTH_UNITS)
        sheet.setColumnWidth(VALUE_COLUMN, WIDTH_NORMAL_CHARS * CHAR_WIDTH_UNITS)
        sheet.setColumnWidth(SECOND_VALUE_COLUMN, WIDTH_NORMAL_CHARS * CHAR_WIDTH_UNITS)

        var rowIndex = 0
        label(sheet, rowIndex++, "Resumo de Vendas", boldStyle)
        rowIndex++

        summary.period?.let { period ->
            dateRow(sheet, rowIndex++, "Primeira venda", period.firstSaleDate, dateStyle)
            dateRow(sheet, rowIndex++, "Última venda", period.lastSaleDate, dateStyle)
        }
        numberRow(sheet, rowIndex++, "Vendas", summary.saleCount.toDouble())
        numberRow(sheet, rowIndex++, "Itens vendidos", summary.unitCount.toDouble())
        numberRow(sheet, rowIndex++, "Ticket médio", summary.money.averageTicket)
        rowIndex++

        numberRow(sheet, rowIndex++, "Subtotal bruto", summary.money.grossAmount)
        numberRow(sheet, rowIndex++, "Descontos por item", summary.money.itemDiscountAmount)
        numberRow(sheet, rowIndex++, "Descontos por venda", summary.money.saleDiscountAmount)
        numberRow(sheet, rowIndex++, "Total de descontos", summary.money.totalDiscountAmount)
        numberRow(sheet, rowIndex++, "Receita líquida", summary.money.netAmount, boldStyle)
        rowIndex++

        writePaymentBreakdown(sheet, rowIndex, summary.byPaymentMethod, boldStyle)
    }

    private fun writePaymentBreakdown(
        sheet: Sheet,
        firstRowIndex: Int,
        totals: List<PaymentMethodTotal>,
        boldStyle: CellStyle
    ) {
        val header = sheet.createRow(firstRowIndex)
        listOf("Forma de pagamento", "Vendas", "Total").forEachIndexed { index, title ->
            header.createCell(index).apply {
                setCellValue(title)
                cellStyle = boldStyle
            }
        }
        totals.forEachIndexed { index, total ->
            val row = sheet.createRow(firstRowIndex + 1 + index)
            row.createCell(LABEL_COLUMN).setCellValue(total.method.reportLabel())
            row.createCell(VALUE_COLUMN).setCellValue(total.saleCount.toDouble())
            row.createCell(SECOND_VALUE_COLUMN).setCellValue(total.netAmount)
        }
    }

    private fun label(sheet: Sheet, rowIndex: Int, text: String, style: CellStyle) {
        sheet.createRow(rowIndex).createCell(LABEL_COLUMN).apply {
            setCellValue(text)
            cellStyle = style
        }
    }

    private fun numberRow(
        sheet: Sheet,
        rowIndex: Int,
        text: String,
        value: Double,
        style: CellStyle? = null
    ) {
        val row = sheet.createRow(rowIndex)
        row.createCell(LABEL_COLUMN).apply {
            setCellValue(text)
            style?.let { cellStyle = it }
        }
        row.createCell(VALUE_COLUMN).apply {
            setCellValue(value)
            style?.let { cellStyle = it }
        }
    }

    private fun dateRow(
        sheet: Sheet,
        rowIndex: Int,
        text: String,
        value: Long,
        dateStyle: CellStyle
    ) {
        val row = sheet.createRow(rowIndex)
        row.createCell(LABEL_COLUMN).setCellValue(text)
        row.createCell(VALUE_COLUMN).apply {
            setCellValue(Date(value))
            cellStyle = dateStyle
        }
    }

    private fun PaymentMethod.reportLabel(): String = when (this) {
        PaymentMethod.PIX -> "PIX"
        PaymentMethod.CASH -> "Dinheiro"
        PaymentMethod.CREDIT_CARD -> "Cartão de Crédito"
        PaymentMethod.DEBIT_CARD -> "Cartão de Débito"
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
        const val SALES_SHEET_NAME = "Vendas"
        const val SUMMARY_SHEET_NAME = "Resumo"
        const val DATE_FORMAT = "dd/mm/yyyy hh:mm"
        const val CHAR_WIDTH_UNITS = 256
        const val TOTALS_LABEL = "TOTAIS"
        const val LABEL_COLUMN = 0
        const val VALUE_COLUMN = 1
        const val SECOND_VALUE_COLUMN = 2

        val SALE_LEVEL_COLUMNS =
            setOf(SalesColumn.SALE_DISCOUNT_PERCENT, SalesColumn.SALE_TOTAL)
    }
}
