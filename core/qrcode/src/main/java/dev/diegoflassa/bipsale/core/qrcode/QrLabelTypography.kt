package dev.diegoflassa.bipsale.core.qrcode

/**
 * Type sizes for a printed label, in PostScript points (1/72").
 *
 * Points rather than pixels, so a size the operator picks survives any paper size or printer DPI.
 * Line height and the price shrink floor are derived rather than configured: both only ever move
 * with the size they belong to, and two more numbers to keep in step are two more to get wrong.
 */
data class QrLabelTypography(
    val nameTextSizePt: Float = DEFAULT_NAME_TEXT_SIZE_PT,
    val priceTextSizePt: Float = DEFAULT_PRICE_TEXT_SIZE_PT
) {
    val nameLineHeightPt: Float = nameTextSizePt * LINE_HEIGHT_RATIO

    /** A price too wide for its cell shrinks to here before the renderer gives up on fitting it. */
    val minPriceTextSizePt: Float = priceTextSizePt * MIN_PRICE_RATIO

    companion object {
        const val DEFAULT_NAME_TEXT_SIZE_PT = 10f
        const val DEFAULT_PRICE_TEXT_SIZE_PT = 13f

        private const val LINE_HEIGHT_RATIO = 1.2f
        private const val MIN_PRICE_RATIO = 0.7f

        val DEFAULT = QrLabelTypography()
    }
}
