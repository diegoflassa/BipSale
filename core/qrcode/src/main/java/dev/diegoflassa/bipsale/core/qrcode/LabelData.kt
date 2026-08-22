package dev.diegoflassa.bipsale.core.qrcode

/** One printable QR label: name above the code, price below it. */
data class LabelData(
    val qrData: String,
    val productName: String,
    val priceFormatted: String
)
