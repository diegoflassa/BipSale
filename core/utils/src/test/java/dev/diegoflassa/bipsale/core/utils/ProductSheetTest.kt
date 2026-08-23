package dev.diegoflassa.bipsale.core.utils

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRejection
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ProductSheetTest {

    private val sheet = ProductSheet()

    private fun template(): ByteArray {
        val out = ByteArrayOutputStream()
        sheet.writeTemplate(out)
        return out.toByteArray()
    }

    /** Builds a filled sheet the way an operator would, starting from the real template. */
    private fun filled(rows: List<List<Any?>>): ByteArray {
        val workbook = XSSFWorkbook(ByteArrayInputStream(template()))
        val target: Sheet = workbook.getSheetAt(0)
        // Drop the example row the template ships with, then write the operator's rows.
        target.getRow(1)?.let(target::removeRow)
        rows.forEachIndexed { index, values ->
            val row = target.createRow(index + 1)
            values.forEachIndexed { column, value ->
                when (value) {
                    null -> Unit
                    is String -> row.createCell(column).setCellValue(value)
                    is Double -> row.createCell(column).setCellValue(value)
                    is Int -> row.createCell(column).setCellValue(value.toDouble())
                    else -> error("unsupported cell value")
                }
            }
        }
        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray) = sheet.read(ByteArrayInputStream(bytes))

    @Test
    fun `the template names every column the importer reads`() {
        val header = XSSFWorkbook(ByteArrayInputStream(template())).getSheetAt(0).getRow(0)

        assertThat(header.getCell(0).stringCellValue).isEqualTo("Código")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("Produto")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("Preço")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("Quantidade")
    }

    @Test
    fun `the template ships an example row the operator can overwrite`() {
        val report = read(template())

        assertThat(report.accepted).hasSize(1)
        assertThat(report.accepted.single().code).isEqualTo("CF-200")
        assertThat(report.accepted.single().quantity).isEqualTo(10)
    }

    @Test
    fun `a filled sheet round-trips every row`() {
        val report = read(
            filled(
                listOf(
                    listOf("CF-200", "Cafe Premium 200ml", 12.50, 8),
                    listOf("PR-100", "Prancheta oficio", 100.0, 3)
                )
            )
        )

        assertThat(report.accepted.map { it.code }).containsExactly("CF-200", "PR-100")
        assertThat(report.accepted.first().price).isEqualTo(12.50)
        assertThat(report.accepted.last().quantity).isEqualTo(3)
        assertThat(report.rejected).isEmpty()
    }

    @Test
    fun `a numeric barcode is read as digits rather than in scientific notation`() {
        // 7891000100103 typed into a spreadsheet arrives as a NUMERIC cell, and toString on it
        // yields 7.891000100103E12 — which matches no product anybody ever scans.
        val report = read(filled(listOf(listOf(7891000100103.0, "Cafe", 12.50, 1))))

        assertThat(report.accepted.single().code).isEqualTo("7891000100103")
    }

    @Test
    fun `a price typed with a comma is still read`() {
        val report = read(filled(listOf(listOf("CF-200", "Cafe", "12,50", 1))))

        assertThat(report.accepted.single().price).isEqualTo(12.50)
    }

    @Test
    fun `a blank row is skipped rather than rejected`() {
        val report = read(
            filled(
                listOf(
                    listOf("CF-200", "Cafe", 12.50, 1),
                    listOf(null, null, null, null),
                    listOf("PR-100", "Prancheta", 100.0, 1)
                )
            )
        )

        assertThat(report.accepted).hasSize(2)
        assertThat(report.rejected).isEmpty()
    }

    @Test
    fun `a bad row is reported and the good rows still import`() {
        val report = read(
            filled(
                listOf(
                    listOf("CF-200", "Cafe", 12.50, 1),
                    listOf("", "Sem codigo", 10.0, 1),
                    listOf("PR-100", "Prancheta", 100.0, 1)
                )
            )
        )

        assertThat(report.accepted.map { it.code }).containsExactly("CF-200", "PR-100")
        assertThat(report.rejected.single().reason)
            .isEqualTo(ProductImportRejection.Reason.MISSING_CODE)
    }

    @Test
    fun `a rejection names the row number the operator sees in the spreadsheet`() {
        val report = read(
            filled(
                listOf(
                    listOf("CF-200", "Cafe", 12.50, 1),
                    listOf("PR-100", "", 100.0, 1)
                )
            )
        )

        // Header is row 1, first product row 2, so the bad one is row 3.
        assertThat(report.rejected.single().row).isEqualTo(3)
        assertThat(report.rejected.single().reason)
            .isEqualTo(ProductImportRejection.Reason.MISSING_NAME)
    }

    @Test
    fun `a zero or missing price is refused rather than registered as free`() {
        val report = read(
            filled(
                listOf(
                    listOf("CF-200", "Cafe", 0.0, 1),
                    listOf("PR-100", "Prancheta", null, 1)
                )
            )
        )

        assertThat(report.accepted).isEmpty()
        assertThat(report.rejected.map { it.reason })
            .containsExactly(
                ProductImportRejection.Reason.INVALID_PRICE,
                ProductImportRejection.Reason.INVALID_PRICE
            )
    }

    @Test
    fun `a missing quantity imports as no stock rather than failing the row`() {
        val report = read(filled(listOf(listOf("CF-200", "Cafe", 12.50, null))))

        assertThat(report.accepted.single().quantity).isEqualTo(0)
    }

    @Test
    fun `a sheet with only a header produces nothing at all`() {
        val workbook = XSSFWorkbook(ByteArrayInputStream(template()))
        val target = workbook.getSheetAt(0)
        target.getRow(1)?.let(target::removeRow)
        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val report = read(out.toByteArray())

        assertThat(report.isEmpty).isTrue()
    }
}
