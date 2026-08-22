package dev.diegoflassa.bipsale.core.qrcode

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QrLabelSheetLayoutTest {

    private fun a4() = QrLabelSheetLayout.forPage(
        QrLabelSheetLayout.A4_WIDTH_PT,
        QrLabelSheetLayout.A4_HEIGHT_PT
    )

    @Test
    fun `fits a dense grid on A4`() {
        val layout = a4()

        assertThat(layout.columns).isEqualTo(4)
        assertThat(layout.rows).isEqualTo(5)
        assertThat(layout.labelsPerPage).isEqualTo(20)
    }

    @Test
    fun `keeps A4 cells large enough to stay scannable`() {
        val layout = a4()

        // ~47 x 55 mm per label: dense, but the QR inside still reads on a phone camera.
        val widthMm = layout.cellWidthPt / POINTS_PER_MM
        val heightMm = layout.cellHeightPt / POINTS_PER_MM
        assertThat(widthMm).isGreaterThan(40f)
        assertThat(heightMm).isGreaterThan(40f)
    }

    @Test
    fun `cells fill the page inside the margins`() {
        val layout = a4()
        val usedWidth = layout.columns * layout.cellWidthPt + 2 * layout.marginPt
        val usedHeight = layout.rows * layout.cellHeightPt + 2 * layout.marginPt

        assertThat(usedWidth).isWithin(TOLERANCE).of(QrLabelSheetLayout.A4_WIDTH_PT)
        assertThat(usedHeight).isWithin(TOLERANCE).of(QrLabelSheetLayout.A4_HEIGHT_PT)
    }

    @Test
    fun `adapts the grid to a non-A4 media size`() {
        val letter = QrLabelSheetLayout.forPage(612f, 792f)

        assertThat(letter.columns).isAtLeast(1)
        assertThat(letter.rows).isAtLeast(1)
        assertThat(letter.columns * letter.cellWidthPt + 2 * letter.marginPt)
            .isWithin(TOLERANCE).of(612f)
    }

    @Test
    fun `never degenerates to a zero-cell grid on tiny paper`() {
        // A page smaller than one target cell used to yield zero rows, and the page loop that
        // consumed it never advanced — the app hung instead of printing.
        val tiny = QrLabelSheetLayout.forPage(80f, 80f)

        assertThat(tiny.columns).isEqualTo(1)
        assertThat(tiny.rows).isEqualTo(1)
        assertThat(tiny.labelsPerPage).isEqualTo(1)
    }

    @Test
    fun `paginates without dropping or duplicating labels`() {
        val layout = a4()

        assertThat(layout.pageCount(0)).isEqualTo(0)
        assertThat(layout.pageCount(1)).isEqualTo(1)
        assertThat(layout.pageCount(20)).isEqualTo(1)
        assertThat(layout.pageCount(21)).isEqualTo(2)
        assertThat(layout.pageCount(40)).isEqualTo(2)
        assertThat(layout.pageCount(41)).isEqualTo(3)
    }

    @Test
    fun `lays cells out row-major without overlapping`() {
        val layout = a4()
        val first = layout.cellBounds(0)
        val second = layout.cellBounds(1)
        val nextRow = layout.cellBounds(layout.columns)

        assertThat(first[1]).isEqualTo(second[1])
        assertThat(second[0]).isWithin(TOLERANCE).of(first[2])
        assertThat(nextRow[1]).isWithin(TOLERANCE).of(first[3])
        assertThat(nextRow[0]).isWithin(TOLERANCE).of(first[0])
    }

    @Test
    fun `first cell starts at the margin`() {
        val layout = a4()
        val first = layout.cellBounds(0)

        assertThat(first[0]).isWithin(TOLERANCE).of(layout.marginPt)
        assertThat(first[1]).isWithin(TOLERANCE).of(layout.marginPt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a slot beyond the page`() {
        val layout = a4()
        layout.cellBounds(layout.labelsPerPage)
    }

    private companion object {
        const val TOLERANCE = 0.01f
        const val POINTS_PER_MM = 72f / 25.4f
    }
}
