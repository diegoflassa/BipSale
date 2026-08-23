package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.export.SalesExportRepository
import dev.diegoflassa.bipsale.core.domain.model.Sale
import javax.inject.Inject

/** Raised instead of writing a header-only sheet, which reads as a successful export of nothing. */
class NoSalesToExport : IllegalStateException("No sales to export")

class ExportSalesUseCase @Inject constructor(
    private val salesExportRepository: SalesExportRepository
) {
    /** Returns how many sales reached the file. */
    suspend operator fun invoke(destinationUri: String, sales: List<Sale>): Result<Int> {
        if (sales.isEmpty()) return Result.failure(NoSalesToExport())
        return runCatching {
            salesExportRepository.exportSales(destinationUri, sales)
            sales.size
        }
    }

    fun suggestedFileName(): String = salesExportRepository.suggestedFileName()
}
