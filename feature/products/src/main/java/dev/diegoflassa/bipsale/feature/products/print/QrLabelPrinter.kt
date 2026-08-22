package dev.diegoflassa.bipsale.feature.products.print

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.getSystemService
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import timber.log.Timber

/**
 * Hands a label sheet to the system print dialog, which is also where "Save as PDF" lives.
 */
class QrLabelPrinter(
    private val renderer: QrLabelSheetRenderer
) {
    fun print(context: Context, documentName: String, labels: List<LabelData>): Boolean {
        if (labels.isEmpty()) {
            Timber.w("[BipSale][Product][QR_EXPORT] Print requested with no labels")
            return false
        }

        val printManager = context.getSystemService<PrintManager>()
        if (printManager == null) {
            Timber.e("[BipSale][Product][QR_EXPORT] PrintManager unavailable on this device")
            return false
        }

        Timber.d("[BipSale][Product][QR_EXPORT] Print dialog opened for %d labels", labels.size)
        printManager.print(
            documentName,
            QrLabelPrintAdapter(context, documentName, labels, renderer),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("default", "default", DPI, DPI))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
        return true
    }

    private companion object {
        const val DPI = 300
    }
}
