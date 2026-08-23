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
                cell = RectF(
                    bounds[QrLabelSheetLayout.LEFT],
                    bounds[QrLabelSheetLayout.TOP],
                    bounds[QrLabelSheetLayout.RIGHT],
                    bounds[QrLabelSheetLayout.BOTTOM]
                ),
                label = labels[index],
                paints = paints
            )
        }
    }

    /**
     * One label rendered at [widthPx] using the same geometry the printer gets, so what the edit
     * screen shows is what comes out of the tray.
     */
    fun renderLabelPreview(
        label: LabelData,
        widthPx: Int,
        layout: QrLabelSheetLayout = QrLabelSheetLayout.a4()
    ): Bitmap {
        val scale = widthPx / layout.cellWidthPt
        val heightPx = (layout.cellHeightPt * scale).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)

        drawLabel(
            canvas = canvas,
            cell = RectF(0f, 0f, layout.cellWidthPt, layout.cellHeightPt),
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
        val innerWidth = cell.width() - 2 * CELL_PADDING_PT

        // The name takes as many lines as it needs, bounded only by leaving the QR enough room to
        // still scan — an unscannable code makes the whole label useless, however readable its name.
        val maxNameLines = maxNameLines(cell)
        val nameLines = wrapText(label.productName, paints.name, innerWidth).take(maxNameLines)
        val nameBlockHeight = nameLines.size * NAME_LINE_HEIGHT_PT

        var baseline = cell.top + CELL_PADDING_PT + NAME_TEXT_SIZE_PT
        for (line in nameLines) {
            canvas.drawText(line, centerX, baseline, paints.name)
            baseline += NAME_LINE_HEIGHT_PT
        }

        // The price is one line, always. Shrink it to fit rather than wrap or clip it, and stop at
        // a size that still reads once the label is cut out.
        paints.price.textSize = PRICE_TEXT_SIZE_PT
        while (
            paints.price.measureText(label.priceFormatted) > innerWidth &&
            paints.price.textSize > MIN_PRICE_TEXT_SIZE_PT
        ) {
            paints.price.textSize -= PRICE_SHRINK_STEP_PT
        }

        // Whatever vertical slack is left over is split evenly above and below the code, so the
        // gap under the name always matches the gap over the price. Anchoring the QR to the name
        // instead pushes all the slack to one side the moment the code is width-limited.
        val nameBottom = cell.top + CELL_PADDING_PT + nameBlockHeight
        val priceTop = cell.bottom - CELL_PADDING_PT - paints.price.textSize
        val spaceForQr = priceTop - nameBottom
        val qrSide = minOf(spaceForQr - 2 * GAP_PT, innerWidth).coerceAtLeast(0f)
        val qrTop = nameBottom + (spaceForQr - qrSide) / 2f

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

    /**
     * How many name lines fit above a QR that is still worth printing. Derived from the cell rather
     * than fixed, so a bigger paper size genuinely gives the name more room.
     */
    private fun maxNameLines(cell: RectF): Int {
        val usableHeight = cell.height() - 2 * CELL_PADDING_PT
        val reserved = PRICE_TEXT_SIZE_PT + 2 * GAP_PT + MIN_QR_SIDE_PT
        val forName = usableHeight - reserved
        return maxOf(1, (forName / NAME_LINE_HEIGHT_PT).toInt())
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
        /** Halved from the original 3 pt: the code is what the eye goes to, so it sits close. */
        const val GAP_PT = 1.5f
        /**
         * Type sizes are in points, so they survive any paper size or printer DPI. These are set to
         * read across a counter rather than at arm's length, which is where a shelf label is
         * actually read from; the price carries the number someone is charged, so it gets more.
         */
        const val NAME_TEXT_SIZE_PT = 13f
        const val NAME_LINE_HEIGHT_PT = 15f
        const val PRICE_TEXT_SIZE_PT = 17f
        const val MIN_PRICE_TEXT_SIZE_PT = 12f
        const val PRICE_SHRINK_STEP_PT = 0.5f

        /**
         * ~22 mm once printed. Below this a code stops reading reliably off a cut-out label under
         * shop lighting, so the name gives way rather than the QR.
         */
        const val MIN_QR_SIDE_PT = 62f
        const val CUT_LINE_WIDTH_PT = 0.5f
        const val DASH_ON_PT = 4f
        const val DASH_OFF_PT = 4f

        /** ~300 DPI once scaled into a ~30 mm cell. */
        const val QR_RENDER_PX = 360
    }
}
