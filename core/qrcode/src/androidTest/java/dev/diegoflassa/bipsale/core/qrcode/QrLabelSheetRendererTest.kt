package dev.diegoflassa.bipsale.core.qrcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Test
import org.junit.runner.RunWith

/** Needs real `android.graphics` (Canvas, Paint, Typeface), so it is instrumented. */
@RunWith(AndroidJUnit4::class)
class QrLabelSheetRendererTest {

    private val renderer = QrLabelSheetRenderer(QrGenerator())

    private fun label(
        code: String = "CT-A-RoS",
        name: String = "Coturno cano alto rosa",
        price: String = "R$ 130,00"
    ) = LabelData(
        qrData = "bipsale://product?code=$code&price=130.0",
        productName = name,
        priceFormatted = price
    )

    private fun Bitmap.decodeQr(): String? {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        return runCatching {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        }.getOrNull()
    }

    private fun Bitmap.nonWhitePixelCount(): Int {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.count { it != Color.WHITE }
    }

    private fun renderPage(labels: List<LabelData>, pageIndex: Int = 0): Bitmap {
        val layout = QrLabelSheetLayout.a4()
        val bitmap = Bitmap.createBitmap(
            QrLabelSheetLayout.A4_WIDTH_PT.toInt(),
            QrLabelSheetLayout.A4_HEIGHT_PT.toInt(),
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            renderer.drawPage(this, layout, labels, pageIndex)
        }
        return bitmap
    }

    @Test
    fun aRenderedLabelPreviewKeepsTheCellAspectRatio() {
        val layout = QrLabelSheetLayout.a4()

        val bitmap = renderer.renderLabelPreview(label(), widthPx = 400)

        val expectedHeight = (layout.cellHeightPt * (400 / layout.cellWidthPt)).toInt()
        assertThat(bitmap.width).isEqualTo(400)
        assertThat(bitmap.height).isEqualTo(expectedHeight)
    }

    @Test
    fun theQrInsideARenderedLabelStillScans() {
        // The whole point of the label: what comes out of the tray has to read back.
        val bitmap = renderer.renderLabelPreview(label(), widthPx = 600)

        assertThat(bitmap.decodeQr()).isEqualTo("bipsale://product?code=CT-A-RoS&price=130.0")
    }

    @Test
    fun aLabelWithAVeryLongNameStillScans() {
        val long = label(name = "Padaria e Confeitaria Gourmet do Centro Pao de Queijo Congelado 1kg")

        val bitmap = renderer.renderLabelPreview(long, widthPx = 600)

        assertThat(bitmap.decodeQr()).isEqualTo("bipsale://product?code=CT-A-RoS&price=130.0")
    }

    @Test
    fun aLabelDrawsSomethingOnAnOtherwiseBlankPage() {
        val blank = renderPage(emptyList())
        val oneLabel = renderPage(listOf(label()))

        assertThat(blank.nonWhitePixelCount()).isEqualTo(0)
        assertThat(oneLabel.nonWhitePixelCount()).isGreaterThan(0)
    }

    @Test
    fun aFullPageDrawsMoreThanAPartialOne() {
        val layout = QrLabelSheetLayout.a4()
        val one = renderPage(listOf(label()))
        val full = renderPage(List(layout.labelsPerPage) { label(code = "CODE-$it") })

        assertThat(full.nonWhitePixelCount()).isGreaterThan(one.nonWhitePixelCount())
    }

    @Test
    fun theSecondPageDrawsTheOverflowRatherThanRepeatingThePageOne() {
        val layout = QrLabelSheetLayout.a4()
        val labels = List(layout.labelsPerPage + 1) { label(code = "CODE-$it") }

        val second = renderPage(labels, pageIndex = 1)

        // Exactly one label spilled onto page two, so it must be drawn but far from full.
        val full = renderPage(labels.take(layout.labelsPerPage))
        assertThat(second.nonWhitePixelCount()).isGreaterThan(0)
        assertThat(second.nonWhitePixelCount()).isLessThan(full.nonWhitePixelCount())
    }

    @Test
    fun aPageIndexPastTheEndDrawsNothingInsteadOfCrashing() {
        val page = renderPage(listOf(label()), pageIndex = 5)

        assertThat(page.nonWhitePixelCount()).isEqualTo(0)
    }

    @Test
    fun twoDifferentProductsRenderDifferentLabels() {
        val first = renderer.renderLabelPreview(label(code = "CT-A-RoS"), widthPx = 400)
        val second = renderer.renderLabelPreview(label(code = "CF-200"), widthPx = 400)

        assertThat(first.sameAs(second)).isFalse()
    }

    @Test
    fun theSameLabelRendersIdentically() {
        val first = renderer.renderLabelPreview(label(), widthPx = 400)
        val second = renderer.renderLabelPreview(label(), widthPx = 400)

        assertThat(first.sameAs(second)).isTrue()
    }

    @Test
    fun aLongPriceIsShrunkToFitInsideTheCellPadding() {
        // The renderer shrinks the price rather than wrapping or clipping it. What that has to
        // buy is ink staying inside the padding: a price that bled into the cut margin would be
        // sliced through when the sheet is cut up. The dashed cut border itself sits on the very
        // edge by design, so the check starts just inside it.
        val layout = QrLabelSheetLayout.a4()
        val widthPx = 400
        val scale = widthPx / layout.cellWidthPt
        val paddingPx = (QrLabelSheetRenderer.CELL_PADDING_PT * scale).toInt()
        val priceBandHeightPx =
            (QrLabelTypography.DEFAULT.priceTextSizePt * scale).toInt()

        val wide = renderer.renderLabelPreview(label(price = "R$ 1.234.567,89"), widthPx = widthPx)

        val bandTop = wide.height - paddingPx - priceBandHeightPx
        val bandBottom = wide.height - paddingPx
        val gutter = buildList {
            for (y in bandTop until bandBottom) {
                for (x in CUT_BORDER_PX until paddingPx - 1) {
                    add(wide.getPixel(x, y))
                    add(wide.getPixel(wide.width - 1 - x, y))
                }
            }
        }

        assertThat(gutter).isNotEmpty()
        assertThat(gutter.all { it == Color.WHITE }).isTrue()
    }

    @Test
    fun biggerConfiguredTypeActuallyPutsMoreInkOnTheLabel() {
        // The setting is only real if it reaches the paints. Passing it and ignoring it renders
        // identically, and the operator changes the number to no effect.
        val small = renderer.renderLabelPreview(
            label(), widthPx = 400,
            typography = QrLabelTypography(nameTextSizePt = 7f, priceTextSizePt = 8f)
        )
        val large = renderer.renderLabelPreview(
            label(), widthPx = 400,
            typography = QrLabelTypography(nameTextSizePt = 16f, priceTextSizePt = 18f)
        )

        assertThat(large.sameAs(small)).isFalse()
    }

    @Test
    fun theQrStillScansAtTheLargestConfigurableType() {
        // Type is allowed to crowd the code, but never to the point of an unreadable label.
        val bitmap = renderer.renderLabelPreview(
            label(), widthPx = 600,
            typography = QrLabelTypography(nameTextSizePt = 24f, priceTextSizePt = 24f)
        )

        assertThat(bitmap.decodeQr()).isEqualTo("bipsale://product?code=CT-A-RoS&price=130.0")
    }

    private companion object {
        /** The dashed cut rectangle is stroked on the cell edge; skip past it before sampling. */
        const val CUT_BORDER_PX = 3
    }
}
