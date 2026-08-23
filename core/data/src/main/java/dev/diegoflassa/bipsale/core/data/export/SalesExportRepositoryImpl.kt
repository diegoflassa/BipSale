package dev.diegoflassa.bipsale.core.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.bipsale.core.domain.export.SalesExportRepository
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.utils.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val excelExporter: ExcelExporter
) : SalesExportRepository {

    override suspend fun exportSales(destinationUri: String, sales: List<Sale>) =
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
                ?: error("Could not open $destinationUri for writing")
            output.use { excelExporter.exportSalesToExcel(it, sales) }
            val lines = sales.sumOf { it.items.size }
            Timber.i("[BipSale][Export] Wrote sales=%d lines=%d", sales.size, lines)
        }

    override fun suggestedFileName(): String {
        val stamp = SimpleDateFormat(FILE_STAMP_PATTERN, Locale.US).format(Date())
        return "bipsale-vendas-$stamp.xlsx"
    }

    private companion object {
        const val FILE_STAMP_PATTERN = "yyyyMMdd-HHmmss"
    }
}
