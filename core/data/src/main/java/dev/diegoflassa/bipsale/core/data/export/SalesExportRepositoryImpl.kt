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
            // The destination is the first thing to check when an operator cannot find the file
            // they just saved; it names a provider and a document, never a person.
            Timber.d(
                "[BipSale][Export] Writing to %s sales=%d", destinationUri, sales.size
            )
            val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
                ?: error("Could not open $destinationUri for writing")
            val summary = output.use { excelExporter.exportSalesToExcel(it, sales) }
            // The totals are what an operator disputes a report against, and none of them
            // identify anybody (CORE_RULES section 8.3).
            Timber.i(
                "[BipSale][Export] Wrote sales=%d lines=%d units=%d gross=%.2f discounts=%.2f net=%.2f",
                summary.saleCount,
                sales.sumOf { it.items.size },
                summary.unitCount,
                summary.money.grossAmount,
                summary.money.totalDiscountAmount,
                summary.money.netAmount
            )
            Timber.d(
                "[BipSale][Export] Take by method %s",
                summary.byPaymentMethod.joinToString { total ->
                    "${total.method.serializedName}=${total.saleCount}/${total.netAmount}"
                }
            )
        }

    override fun suggestedFileName(): String {
        val stamp = SimpleDateFormat(FILE_STAMP_PATTERN, Locale.US).format(Date())
        return "bipsale-vendas-$stamp.xlsx"
    }

    private companion object {
        const val FILE_STAMP_PATTERN = "yyyyMMdd-HHmmss"
    }
}
