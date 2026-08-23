package dev.diegoflassa.bipsale.core.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Needs real `android.graphics`, so it is instrumented rather than a JVM test.
 *
 * The point of every case here is that the bitmap **decodes back**. A QR that renders and cannot be
 * scanned looks perfectly fine in a screenshot and is worthless at a counter.
 */
@RunWith(AndroidJUnit4::class)
class QrGeneratorTest {

    private val generator = QrGenerator()

    private fun Bitmap.decodeQr(): String? {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        return runCatching {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        }.getOrNull()
    }

    @Test
    fun generatesABitmapOfTheRequestedSize() {
        val bitmap = generator.generateQrCode("bipsale://product?code=CT-A-RoS&price=130.0")

        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(512)
        assertThat(bitmap.height).isEqualTo(512)
    }

    @Test
    fun honoursAnExplicitSize() {
        val bitmap = generator.generateQrCode("CT-A-RoS", width = 360, height = 360)

        assertThat(bitmap!!.width).isEqualTo(360)
        assertThat(bitmap.height).isEqualTo(360)
    }

    @Test
    fun theRenderedCodeScansBackToItsPayload() {
        val payload = "bipsale://product?code=CT-A-RoS&price=130.0"

        val decoded = generator.generateQrCode(payload)!!.decodeQr()

        assertThat(decoded).isEqualTo(payload)
    }

    @Test
    fun aCodeRenderedAtPrintResolutionStillScans() {
        // 360 px is what the label sheet renders into a ~30 mm cell.
        val payload = "bipsale://product?code=CF-200&price=12.5"

        val decoded = generator.generateQrCode(payload, width = 360, height = 360)!!.decodeQr()

        assertThat(decoded).isEqualTo(payload)
    }

    @Test
    fun aLongPayloadStillScans() {
        val payload = "bipsale://product?code=PADARIA-CONFEITARIA-GOURMET-CENTRO-001&price=1234.56"

        val decoded = generator.generateQrCode(payload)!!.decodeQr()

        assertThat(decoded).isEqualTo(payload)
    }

    @Test
    fun rendersInBlackOnWhiteSoAScannerHasContrast() {
        val bitmap = generator.generateQrCode("CT-A-RoS")!!
        val colors = buildSet {
            for (x in 0 until bitmap.width step 16) {
                for (y in 0 until bitmap.height step 16) {
                    add(bitmap.getPixel(x, y))
                }
            }
        }

        assertThat(colors).containsExactly(Color.BLACK, Color.WHITE)
    }

    @Test
    fun theQuietZoneIsWhiteSoTheCodeIsFindable() {
        val bitmap = generator.generateQrCode("CT-A-RoS")!!

        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.WHITE)
        assertThat(bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)).isEqualTo(Color.WHITE)
    }

    @Test
    fun returnsNullRatherThanThrowingOnAnUnencodablePayload() {
        // ZXing cannot encode an empty payload; the caller treats null as "no label".
        assertThat(generator.generateQrCode("")).isNull()
    }

    @Test
    fun theSamePayloadRendersIdentically() {
        val first = generator.generateQrCode("CT-A-RoS")!!
        val second = generator.generateQrCode("CT-A-RoS")!!

        assertThat(first.sameAs(second)).isTrue()
    }
}
