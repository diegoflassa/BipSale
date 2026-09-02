package dev.diegoflassa.bipsale.feature.products.print

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.getSystemService
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import dev.diegoflassa.bipsale.core.qrcode.QrLabelTypography
import timber.log.Timber

/**
 * Hands a label sheet to the system print dialog, which is also where "Save as PDF" lives.
 */
class QrLabelPrinter(
    private val renderer: QrLabelSheetRenderer
) {
    fun print(
        context: Context,
        documentName: String,
        labels: List<LabelData>,
        requestedColumns: Int? = null,
        typography: QrLabelTypography = QrLabelTypography.DEFAULT
    ): Boolean {
        if (labels.isEmpty()) {
            Timber.w("[BipSale][Product][QR_EXPORT] Print requested with no labels")
            return false
        }

        val printManager = context.getSystemService<PrintManager>()
        if (printManager == null) {
            Timber.e("[BipSale][Product][QR_EXPORT] PrintManager unavailable on this device")
            return false
        }

        // asPortrait matters: ISO_A4 on its own carries whatever orientation the print service
        // last used, and a landscape A4 hands the adapter a 297x210 page whose grid comes out
        // rotated. The dialog can still be changed by hand — the layout re-fits whatever it gets.
        val requested = PrintAttributes.MediaSize.ISO_A4.asPortrait()
        Timber.d(
            "[BipSale][Product][QR_EXPORT] Print dialog opened for %d labels, requesting %s " +
                "(%dx%d mils) columns=%s namePt=%.1f pricePt=%.1f",
            labels.size, requested.id, requested.widthMils, requested.heightMils,
            requestedColumns?.toString() ?: "auto",
            typography.nameTextSizePt, typography.priceTextSizePt
        )
        printManager.print(
            documentName,
            QrLabelPrintAdapter(
                context, documentName, labels, renderer, requestedColumns, typography
            ),
            PrintAttributes.Builder()
                .setMediaSize(requested)
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
