package dev.diegoflassa.bipsale.core.domain.export

import dev.diegoflassa.bipsale.core.domain.model.Sale

/**
 * Writes sales out as a spreadsheet the operator can open elsewhere.
 *
 * The destination crosses this boundary as a `String` URI so the domain stays free of Android
 * types; the caller gets it from the system document picker, which is what puts Drive, Downloads
 * and any other provider on the device in reach.
 */
interface SalesExportRepository {

    suspend fun exportSales(destinationUri: String, sales: List<Sale>)

    fun suggestedFileName(): String
}
