package dev.diegoflassa.bipsale.core.data.product

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.bipsale.core.domain.product.ProductImportReport
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRepository
import dev.diegoflassa.bipsale.core.utils.ProductSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productSheet: ProductSheet
) : ProductImportRepository {

    override suspend fun writeTemplate(destinationUri: String) = withContext(Dispatchers.IO) {
        Timber.d("[BipSale][Import] Writing the product template to %s", destinationUri)
        val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
            ?: error("Could not open $destinationUri for writing")
        output.use { productSheet.writeTemplate(it) }
        Timber.i("[BipSale][Import] Product template written")
    }

    override suspend fun read(sourceUri: String): ProductImportReport =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            val mimeType = context.contentResolver.getType(uri)
            Timber.d("[BipSale][Import] Reading products from %s type=%s", sourceUri, mimeType)
            val report = openSheet(uri, mimeType).use { productSheet.read(it) }
            Timber.i(
                "[BipSale][Import] Sheet read accepted=%d rejected=%d",
                report.accepted.size, report.rejected.size
            )
            report.rejected.forEach {
                Timber.w(
                    "[BipSale][Import] Row %d rejected code=%s reason=%s",
                    it.row, it.code, it.reason
                )
            }
            report
        }

    /**
     * A file picked out of Google Drive can be one of two very different things: an uploaded
     * `.xlsx`, which opens as a plain stream, or a *native* Google Sheet, which has no bytes of its
     * own and only exists as something Drive can export. `openInputStream` fails outright on the
     * second, which is why the export path is tried whenever the document declares a Google type.
     */
    private fun openSheet(uri: Uri, mimeType: String?): InputStream {
        if (mimeType != null && mimeType.startsWith(GOOGLE_DOCS_MIME_PREFIX)) {
            Timber.d("[BipSale][Import] Exporting a native Google document as a spreadsheet")
            val descriptor = context.contentResolver
                .openTypedAssetFileDescriptor(uri, SPREADSHEET_MIME_TYPE, null)
                ?: error("Drive would not export $uri as a spreadsheet")
            return descriptor.createInputStream()
        }
        return context.contentResolver.openInputStream(uri)
            ?: error("Could not open $uri for reading")
    }

    override fun suggestedTemplateName(): String {
        val stamp = SimpleDateFormat(FILE_STAMP_PATTERN, Locale.US).format(Date())
        return "bipsale-produtos-modelo-$stamp.xlsx"
    }

    private companion object {
        const val FILE_STAMP_PATTERN = "yyyyMMdd-HHmmss"
        const val GOOGLE_DOCS_MIME_PREFIX = "application/vnd.google-apps"
        const val SPREADSHEET_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
