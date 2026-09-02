package dev.diegoflassa.bipsale.core.qrcode

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import org.junit.Test

class QrLabelTypographyTest {

    @Test
    fun `line height leaves room above and below the glyphs`() {
        // Set equal to the text size, consecutive name lines touch and the block reads as one smear.
        val typography = QrLabelTypography(nameTextSizePt = 10f, priceTextSizePt = 13f)

        assertThat(typography.nameLineHeightPt).isGreaterThan(typography.nameTextSizePt)
    }

    @Test
    fun `the price shrink floor stays below the size it starts from`() {
        // The floor is what the shrink loop stops at. At or above the starting size the loop can
        // never run, and a price too wide for the cell gets clipped instead of fitted.
        val typography = QrLabelTypography(nameTextSizePt = 10f, priceTextSizePt = 13f)

        assertThat(typography.minPriceTextSizePt).isLessThan(typography.priceTextSizePt)
        assertThat(typography.minPriceTextSizePt).isGreaterThan(0f)
    }

    @Test
    fun `derived sizes follow the configured size rather than staying fixed`() {
        val small = QrLabelTypography(nameTextSizePt = 8f, priceTextSizePt = 10f)
        val large = QrLabelTypography(nameTextSizePt = 16f, priceTextSizePt = 20f)

        assertThat(large.nameLineHeightPt).isGreaterThan(small.nameLineHeightPt)
        assertThat(large.minPriceTextSizePt).isGreaterThan(small.minPriceTextSizePt)
    }

    @Test
    fun `the default matches what settings hand out when nothing was configured`() {
        // Two defaults that drift apart mean a fresh install prints one size and the settings
        // screen shows another.
        val fromSettings = AppSettings.EMPTY.toQrLabelTypography()

        assertThat(fromSettings).isEqualTo(QrLabelTypography.DEFAULT)
    }

    @Test
    fun `settings points widen into the renderer's fractional points`() {
        val settings = AppSettings(qrLabelNameTextSizePt = 14, qrLabelPriceTextSizePt = 18)

        val typography = settings.toQrLabelTypography()

        assertThat(typography.nameTextSizePt).isEqualTo(14f)
        assertThat(typography.priceTextSizePt).isEqualTo(18f)
    }

    @Test
    fun `the configured range brackets the default sizes`() {
        // A default outside its own range would be coerced away the first time it is read back.
        val range = AppSettings.MIN_QR_LABEL_TEXT_SIZE_PT..AppSettings.MAX_QR_LABEL_TEXT_SIZE_PT

        assertThat(range).contains(AppSettings.DEFAULT_QR_LABEL_NAME_TEXT_SIZE_PT)
        assertThat(range).contains(AppSettings.DEFAULT_QR_LABEL_PRICE_TEXT_SIZE_PT)
    }
}
