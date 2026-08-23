package dev.diegoflassa.bipsale.core.qrcode

/**
 * Grid arithmetic for a sheet of cut-out QR labels, in PostScript points (1/72").
 *
 * Kept free of Android types so the paper-fitting maths — the part that decides whether a printed
 * QR is still scannable — is unit-testable.
 */
data class QrLabelSheetLayout(
    val columns: Int,
    val rows: Int,
    val cellWidthPt: Float,
    val cellHeightPt: Float,
    val marginPt: Float
) {
    val labelsPerPage: Int = columns * rows

    fun pageCount(labelCount: Int): Int =
        if (labelCount <= 0) 0 else ceilDiv(labelCount, labelsPerPage)

    /** Bounds of [slot] (0-based, row-major) as `left, top, right, bottom` in points. */
    fun cellBounds(slot: Int): FloatArray {
        require(slot in 0 until labelsPerPage) { "Slot $slot out of range for $labelsPerPage cells." }
        val column = slot % columns
        val row = slot / columns
        val left = marginPt + column * cellWidthPt
        val top = marginPt + row * cellHeightPt
        return floatArrayOf(left, top, left + cellWidthPt, top + cellHeightPt)
    }

    companion object {
        /** Index into the [cellBounds] array; named so a caller cannot transpose two edges. */
        const val LEFT = 0
        const val TOP = 1
        const val RIGHT = 2
        const val BOTTOM = 3

        /** A4 is the house default; the print dialog may still hand us any other media size. */
        const val A4_WIDTH_PT = 595f
        const val A4_HEIGHT_PT = 842f

        /** ~30 mm of QR — small enough to tile densely, large enough for a phone to read. */
        const val TARGET_CELL_WIDTH_PT = 133f
        const val TARGET_CELL_HEIGHT_PT = 142f
        const val DEFAULT_MARGIN_PT = 28f

        /** The grid the print dialog defaults to, and what the on-screen preview mirrors. */
        fun a4(): QrLabelSheetLayout = forPage(A4_WIDTH_PT, A4_HEIGHT_PT)

        /** Points to millimetres, for telling the operator the physical size of a label. */
        fun pointsToMillimetres(points: Float): Float = points / POINTS_PER_INCH * MM_PER_INCH

        /**
         * Fits as many whole cells as the page allows, then grows the cells to consume the
         * leftover strip so the sheet stays edge-to-edge on any paper size.
         */
        fun forPage(
            pageWidthPt: Float,
            pageHeightPt: Float,
            marginPt: Float = DEFAULT_MARGIN_PT
        ): QrLabelSheetLayout {
            val usableWidth = (pageWidthPt - 2 * marginPt).coerceAtLeast(1f)
            val usableHeight = (pageHeightPt - 2 * marginPt).coerceAtLeast(1f)

            val columns = (usableWidth / TARGET_CELL_WIDTH_PT).toInt().coerceAtLeast(1)
            val rows = (usableHeight / TARGET_CELL_HEIGHT_PT).toInt().coerceAtLeast(1)

            return QrLabelSheetLayout(
                columns = columns,
                rows = rows,
                cellWidthPt = usableWidth / columns,
                cellHeightPt = usableHeight / rows,
                marginPt = marginPt
            )
        }

        private const val POINTS_PER_INCH = 72f
        private const val MM_PER_INCH = 25.4f

        private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
    }
}
