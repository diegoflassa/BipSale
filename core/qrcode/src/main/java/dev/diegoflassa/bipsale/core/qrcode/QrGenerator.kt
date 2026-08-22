package dev.diegoflassa.bipsale.core.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import timber.log.Timber
import javax.inject.Inject

class QrGenerator @Inject constructor() {

    fun generateQrCode(text: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val matrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
                setPixels(matrix.toPixels(width, height), 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            Timber.e(e, "[BipSale][Product][QR_EXPORT] QR encode failed for payload length %d", text.length)
            null
        }
    }

    /**
     * One [Bitmap.setPixels] beats the per-pixel `setPixel` loop this replaces: that cost ~260k
     * JNI calls for a single 512² code, visible as a stutter while typing a price.
     */
    private fun BitMatrix.toPixels(width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        return pixels
    }
}
