package dev.diegoflassa.bipsale.core.utils

import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

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
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(SHEET_NAME)

        val headerRow = sheet.createRow(0)
        SalesColumn.entries.forEach { column ->
            headerRow.createCell(column.ordinal).setCellValue(column.header)
        }

        var rowNum = 1
        val lines = sales.flatMap { sale -> sale.items.map { item -> sale to item } }
        lines.forEach { (sale, item) ->
            writeRow(sheet.createRow(rowNum++), sale, item)
        }

        workbook.write(outputStream)
        workbook.close()
    }

    private fun writeRow(row: Row, sale: Sale, item: SaleItem) {
        SalesColumn.entries.forEach { column ->
            val cell = row.createCell(column.ordinal)
            when (column) {
                SalesColumn.CUSTOMER -> cell.setCellValue(sale.customerName)
                SalesColumn.CPF -> cell.setCellValue(sale.customerCpf)
                SalesColumn.PRODUCT_CODE -> cell.setCellValue(item.productCode)
                SalesColumn.PRODUCT_NAME -> cell.setCellValue(item.productName)
                SalesColumn.UNIT_PRICE -> cell.setCellValue(item.unitPrice)
                SalesColumn.SALE_TOTAL -> cell.setCellValue(sale.finalAmount)
            }
        }
    }

    private enum class SalesColumn(val header: String) {
        CUSTOMER("Cliente"),
        CPF("CPF"),
        PRODUCT_CODE("Código Produto"),
        PRODUCT_NAME("Produto"),
        UNIT_PRICE("Valor Unit."),
        SALE_TOTAL("Total Venda")
    }

    private companion object {
        const val SHEET_NAME = "Vendas"
    }
}
