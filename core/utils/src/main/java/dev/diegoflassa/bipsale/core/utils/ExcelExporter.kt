package dev.diegoflassa.bipsale.core.utils

import android.content.Context
import android.os.Environment
import dev.diegoflassa.bipsale.core.domain.model.Sale
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExporter @Inject constructor() {

    fun exportSalesToExcel(
        outputStream: OutputStream,
        sales: List<Sale>
    ) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Vendas")

        // Header
        val headerRow = sheet.createRow(0)
        val columns = listOf("Cliente", "CPF", "Código Produto", "Produto", "Valor Unit.", "Total Venda")
        columns.forEachIndexed { index, col ->
            headerRow.createCell(index).setCellValue(col)
        }

        var rowNum = 1
        sales.forEach { sale ->
            sale.items.forEach { item ->
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(sale.customerName)
                row.createCell(1).setCellValue(sale.customerCpf)
                row.createCell(2).setCellValue(item.productCode)
                row.createCell(3).setCellValue(item.productName)
                row.createCell(4).setCellValue(item.unitPrice)
                row.createCell(5).setCellValue(sale.finalAmount)
            }
        }

        workbook.write(outputStream)
        workbook.close()
    }
}
