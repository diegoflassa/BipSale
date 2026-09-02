package dev.diegoflassa.bipsale.core.qrcode

import dev.diegoflassa.bipsale.core.domain.settings.AppSettings

/**
 * The label type sizes the operator configured, in the form the renderer takes.
 *
 * Settings hold whole points because that is what a picker offers; the renderer works in the same
 * fractional points as the rest of the page geometry, so the widening happens once, here.
 */
fun AppSettings.toQrLabelTypography(): QrLabelTypography = QrLabelTypography(
    nameTextSizePt = qrLabelNameTextSizePt.toFloat(),
    priceTextSizePt = qrLabelPriceTextSizePt.toFloat()
)
