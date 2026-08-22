package dev.diegoflassa.bipsale.core.qrcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import javax.inject.Inject

/**
 * Draws sheets of cut-out QR labels straight onto a page canvas.
 *
 * Rendering per page rather than composing one tall bitmap is what keeps this off the heap: a
 * full-page ARGB_8888 bitmap at print resolution is ~33 MB, so a multi-page batch built that way
 * runs out of memory before it reaches the printer.
 */
class QrLabelSheetRenderer @Inject constructor(
    private val qrGenerator: QrGenerator
) {

    fun drawPage(
        canvas: Canvas,
        layout: QrLabelSheetLayout,
        labels: List<LabelData>,
        pageIndex: Int
    ) {
        val paints = LabelPaints()

        val first = pageIndex * layout.labelsPerPage
        val last = minOf(first + layout.labelsPerPage, labels.size)

        for (index in first until last) {
            val bounds = layout.cellBounds(index - first)
            drawLabel(
                canvas = canvas,
                cell = RectF(bounds[0], bounds[1], bounds[2], bounds[3]),
                label = labels[index],
                paints = paints
            )
        }
    }

    /**
     * One label rendered at [widthPx] using the same geometry the printer gets, so what the edit
     * screen shows is what comes out of the tray.
     */
    fun renderLabelPreview(label: LabelData, widthPx: Int): Bitmap {
        val scale = widthPx / QrLabelSheetLayout.TARGET_CELL_WIDTH_PT
        val heightPx = (QrLabelSheetLayout.TARGET_CELL_HEIGHT_PT * scale).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)

        drawLabel(
            canvas = canvas,
            cell = RectF(
                0f,
                0f,
                QrLabelSheetLayout.TARGET_CELL_WIDTH_PT,
                QrLabelSheetLayout.TARGET_CELL_HEIGHT_PT
            ),
            label = label,
            paints = LabelPaints()
        )
        return bitmap
    }

    private fun drawLabel(
        canvas: Canvas,
        cell: RectF,
        label: LabelData,
        paints: LabelPaints
    ) {
        canvas.drawRect(cell, paints.cut)

        val centerX = cell.centerX()
        val nameLines = wrapText(label.productName, paints.name, cell.width() - 2 * CELL_PADDING_PT)
            .take(MAX_NAME_LINES)
        val nameHeight = nameLines.size * NAME_LINE_HEIGHT_PT

        var textY = cell.top + CELL_PADDING_PT + NAME_TEXT_SIZE_PT
        for (line in nameLines) {
            canvas.drawText(line, centerX, textY, paints.name)
            textY += NAME_LINE_HEIGHT_PT
        }

        val qrTop = cell.top + CELL_PADDING_PT + nameHeight + GAP_PT
        val qrBottom = cell.bottom - CELL_PADDING_PT - PRICE_TEXT_SIZE_PT - GAP_PT
        val qrSide = minOf(
            qrBottom - qrTop,
            cell.width() - 2 * CELL_PADDING_PT
        )
        if (qrSide > 0f) {
            val qrBitmap = qrGenerator.generateQrCode(label.qrData, QR_RENDER_PX, QR_RENDER_PX)
            if (qrBitmap != null) {
                val destination = RectF(
                    centerX - qrSide / 2f,
                    qrTop,
                    centerX + qrSide / 2f,
                    qrTop + qrSide
                )
                canvas.drawBitmap(qrBitmap, null as Rect?, destination, paints.qr)
                qrBitmap.recycle()
            }
        }

        canvas.drawText(
            label.priceFormatted,
            centerX,
            cell.bottom - CELL_PADDING_PT,
            paints.price
        )
    }

    /** Built once per sheet — a Paint per label would churn allocations across a full page. */
    private data class LabelPaints(
        val cut: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = CUT_LINE_WIDTH_PT
            pathEffect = DashPathEffect(floatArrayOf(DASH_ON_PT, DASH_OFF_PT), 0f)
        },
        val name: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = NAME_TEXT_SIZE_PT
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        },
        val price: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PRICE_TEXT_SIZE_PT
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        },
        // QR modules must stay hard-edged; smoothing them costs scan reliability at label size.
        val qr: Paint = Paint().apply { isFilterBitmap = false }
    )

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth <= 0f) return listOf(text)
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder()
                }
                // A single word wider than the cell still has to land somewhere.
                if (paint.measureText(word) > maxWidth) {
                    lines.add(truncate(word, paint, maxWidth))
                } else {
                    current = StringBuilder(word)
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())

        return lines.ifEmpty { listOf(text) }
    }

    private fun truncate(word: String, paint: Paint, maxWidth: Float): String {
        var end = word.length
        while (end > 1 && paint.measureText(word.take(end) + "…") > maxWidth) {
            end--
        }
        return word.take(end) + "…"
    }

    private companion object {
        const val CELL_PADDING_PT = 6f
        const val GAP_PT = 3f
        const val NAME_TEXT_SIZE_PT = 6.5f
        const val NAME_LINE_HEIGHT_PT = 8f
        const val PRICE_TEXT_SIZE_PT = 9f
        const val MAX_NAME_LINES = 2
        const val CUT_LINE_WIDTH_PT = 0.5f
        const val DASH_ON_PT = 4f
        const val DASH_OFF_PT = 4f

        /** ~300 DPI once scaled into a ~30 mm cell. */
        const val QR_RENDER_PX = 360
    }
}
