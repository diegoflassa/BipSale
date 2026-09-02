package dev.diegoflassa.bipsale.feature.products.print

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetLayout
import dev.diegoflassa.bipsale.core.qrcode.QrLabelSheetRenderer
import dev.diegoflassa.bipsale.core.qrcode.QrLabelTypography
import dev.diegoflassa.bipsale.feature.products.R
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException

/**
 * Lays QR labels out as a real multi-page document.
 *
 * Going through [PrintDocumentAdapter] rather than a single printed bitmap is what buys the
 * page-by-page preview, the system "Save as PDF" destination, and a grid that re-fits itself to
 * whatever media size the operator picks in the dialog — A4 is only the default we ask for.
 */
class QrLabelPrintAdapter(
    private val context: Context,
    private val documentName: String,
    private val labels: List<LabelData>,
    private val renderer: QrLabelSheetRenderer,
    private val requestedColumns: Int? = null,
    private val typography: QrLabelTypography = QrLabelTypography.DEFAULT
) : PrintDocumentAdapter() {

    private var pdfDocument: PdfDocument? = null
    private var layout: QrLabelSheetLayout = QrLabelSheetLayout.forPage(
        QrLabelSheetLayout.A4_WIDTH_PT,
        QrLabelSheetLayout.A4_HEIGHT_PT,
        requestedColumns = requestedColumns
    )
    private var pageCount: Int = 0
    private var pageWidthPt: Int = QrLabelSheetLayout.A4_WIDTH_PT.toInt()
    private var pageHeightPt: Int = QrLabelSheetLayout.A4_HEIGHT_PT.toInt()

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val mediaSize = (newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4).asPortrait()
        // MediaSize is in mils (1/1000"); the canvas works in points (1/72").
        pageWidthPt = milsToPoints(mediaSize.widthMils)
        pageHeightPt = milsToPoints(mediaSize.heightMils)

        layout = QrLabelSheetLayout.forPage(
            pageWidthPt.toFloat(), pageHeightPt.toFloat(),
            requestedColumns = requestedColumns
        )
        pageCount = layout.pageCount(labels.size)

        if (pageCount == 0) {
            callback.onLayoutFailed(context.getString(R.string.products_no_products_to_print))
            return
        }

        Timber.d(
            "[BipSale][Product][QR_EXPORT] Layout media=%s %dx%dpt grid=%dx%d perPage=%d pages=%d",
            mediaSize.id, pageWidthPt, pageHeightPt,
            layout.columns, layout.rows, layout.labelsPerPage, pageCount
        )

        val info = PrintDocumentInfo.Builder(documentName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()

        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val document = PdfDocument()
        pdfDocument = document

        try {
            for (pageIndex in 0 until pageCount) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                if (!pages.containsPage(pageIndex)) continue

                val pageInfo = PdfDocument.PageInfo
                    .Builder(pageWidthPt, pageHeightPt, pageIndex)
                    .create()
                val page = document.startPage(pageInfo)
                renderer.drawPage(page.canvas, layout, labels, pageIndex, typography)
                document.finishPage(page)
            }

            FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
            Timber.d(
                "[BipSale][Product][QR_EXPORT] Wrote %d labels across %d page(s)",
                labels.size, pageCount
            )
            callback.onWriteFinished(arrayOf(PageRange(0, pageCount - 1)))
        } catch (e: IOException) {
            Timber.e(e, "[BipSale][Product][QR_EXPORT] Failed writing label document")
            callback.onWriteFailed(e.message)
        } finally {
            document.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
    }

    private fun Array<out PageRange>.containsPage(page: Int): Boolean =
        isEmpty() || any { page >= it.start && page <= it.end }

    private fun milsToPoints(mils: Int): Int = (mils * POINTS_PER_INCH / MILS_PER_INCH).toInt()

    private companion object {
        const val POINTS_PER_INCH = 72f
        const val MILS_PER_INCH = 1000f
    }
}
