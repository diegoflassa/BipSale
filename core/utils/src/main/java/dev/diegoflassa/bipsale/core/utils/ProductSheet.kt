package dev.diegoflassa.bipsale.core.utils

import dev.diegoflassa.bipsale.core.domain.product.ImportedProduct
import dev.diegoflassa.bipsale.core.domain.product.ProductImportReport
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRejection
import dev.diegoflassa.bipsale.core.domain.util.parseDecimalInput
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDTH_NORMAL_CHARS = 18
private const val WIDTH_WIDE_CHARS = 34

/**
 * The spreadsheet an operator fills in to register a catalogue in bulk, and the reader that takes
 * it back.
 *
 * Product photos are deliberately not part of it: a spreadsheet cannot carry them usefully, so the
 * import brings in the rows and the operator attaches images per product afterwards.
 */
@Singleton
class ProductSheet @Inject constructor() {

    /** Writes an empty template with the header row and one filled example the operator overwrites. */
    fun writeTemplate(outputStream: OutputStream) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet(SHEET_NAME)
            val boldStyle = workbook.createCellStyle().apply {
                setFont(workbook.createFont().apply { bold = true })
            }

            val header = sheet.createRow(0)
            ProductColumn.entries.forEach { column ->
                header.createCell(column.ordinal).apply {
                    setCellValue(column.header)
                    cellStyle = boldStyle
                }
                sheet.setColumnWidth(column.ordinal, column.widthInChars * CHAR_WIDTH_UNITS)
            }

            val example = sheet.createRow(1)
            example.createCell(ProductColumn.CODE.ordinal).setCellValue(EXAMPLE_CODE)
            example.createCell(ProductColumn.NAME.ordinal).setCellValue(EXAMPLE_NAME)
            example.createCell(ProductColumn.PRICE.ordinal).setCellValue(EXAMPLE_PRICE)
            example.createCell(ProductColumn.QUANTITY.ordinal).setCellValue(EXAMPLE_QUANTITY)

            workbook.write(outputStream)
        }
    }

    /**
     * Reads a filled template. A bad row is reported rather than thrown: one typo in a fifty-row
     * catalogue must not cost the operator the other forty-nine.
     */
    fun read(inputStream: InputStream): ProductImportReport {
        val accepted = mutableListOf<ImportedProduct>()
        val rejected = mutableListOf<ProductImportRejection>()

        WorkbookFactory.create(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            // Row 0 is the header the template wrote; data starts under it. A blank row is a gap
            // the operator left, not an error, so it never reaches the parser.
            val filledRows = (1..sheet.lastRowNum).mapNotNull { index ->
                sheet.getRow(index)?.takeUnless { it.isBlank() }?.let { index to it }
            }
            filledRows.forEach { (index, row) ->
                when (val parsed = row.toProduct(index)) {
                    is ParsedRow.Accepted -> accepted += parsed.product
                    is ParsedRow.Rejected -> rejected += parsed.rejection
                }
            }
        }
        return ProductImportReport(accepted, rejected)
    }

    private fun Row.toProduct(rowIndex: Int): ParsedRow {
        val code = text(ProductColumn.CODE)
        val name = text(ProductColumn.NAME)
        val priceText = text(ProductColumn.PRICE)
        val price = number(ProductColumn.PRICE) ?: parseDecimalInput(priceText)
        val quantity = number(ProductColumn.QUANTITY)?.toInt() ?: 0

        // Row numbers are reported one-based, matching what the operator sees in the spreadsheet.
        val humanRow = rowIndex + 1
        return when {
            code.isBlank() -> reject(humanRow, code, ProductImportRejection.Reason.MISSING_CODE)
            name.isBlank() -> reject(humanRow, code, ProductImportRejection.Reason.MISSING_NAME)
            price == null || !price.isFinite() || price <= 0.0 ->
                reject(humanRow, code, ProductImportRejection.Reason.INVALID_PRICE)

            quantity < 0 -> reject(humanRow, code, ProductImportRejection.Reason.INVALID_QUANTITY)
            else -> ParsedRow.Accepted(ImportedProduct(code, name, price, quantity))
        }
    }

    private fun reject(row: Int, code: String, reason: ProductImportRejection.Reason) =
        ParsedRow.Rejected(ProductImportRejection(row, code, reason))

    private fun Row.isBlank(): Boolean =
        ProductColumn.entries.all { text(it).isBlank() }

    private fun Row.text(column: ProductColumn): String =
        getCell(column.ordinal)?.asText().orEmpty().trim()

    private fun Row.number(column: ProductColumn): Double? =
        getCell(column.ordinal)?.takeIf { it.cellType == CellType.NUMERIC }?.numericCellValue

    private fun Cell.asText(): String = when (cellType) {
        CellType.STRING -> stringCellValue
        // A code typed as 7891000100103 comes back numeric, and toString would make it 7.891E12.
        CellType.NUMERIC -> numericCellValue.toPlainText()
        CellType.BOOLEAN -> booleanCellValue.toString()
        else -> ""
    }

    private fun Double.toPlainText(): String =
        if (this == toLong().toDouble()) toLong().toString() else toString()

    private sealed interface ParsedRow {
        data class Accepted(val product: ImportedProduct) : ParsedRow
        data class Rejected(val rejection: ProductImportRejection) : ParsedRow
    }

    private enum class ProductColumn(val header: String, val widthInChars: Int) {
        CODE("Código", WIDTH_NORMAL_CHARS),
        NAME("Produto", WIDTH_WIDE_CHARS),
        PRICE("Preço", WIDTH_NORMAL_CHARS),
        QUANTITY("Quantidade", WIDTH_NORMAL_CHARS)
    }

    private companion object {
        const val SHEET_NAME = "Produtos"
        const val CHAR_WIDTH_UNITS = 256
        const val EXAMPLE_CODE = "CF-200"
        const val EXAMPLE_NAME = "Café Premium 200ml"
        const val EXAMPLE_PRICE = 12.50
        const val EXAMPLE_QUANTITY = 10.0
    }
}
