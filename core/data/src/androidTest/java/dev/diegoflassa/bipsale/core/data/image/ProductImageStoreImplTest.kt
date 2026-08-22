package dev.diegoflassa.bipsale.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ProductImageStoreImplTest {

    private lateinit var context: Context
    private lateinit var store: ProductImageStoreImpl

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = ProductImageStoreImpl(context)
    }

    private fun sourceImage(width: Int = 2400, height: Int = 1600): Uri {
        val file = File(context.cacheDir, "source_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun storesAPickedImage() = runBlocking {
        // Regression: bounds decoding runs with inJustDecodeBounds, so decodeStream returns null on
        // success. Null-checking that result instead of the stream rejected every image picked.
        val fileName = store.save(sourceImage().toString(), "CT-A-RoS")

        val stored = File(store.resolvePath(fileName))
        assertThat(stored.exists()).isTrue()
        assertThat(stored.length()).isGreaterThan(0L)
    }

    @Test
    fun downscalesAnOversizedPhoto() = runBlocking {
        val fileName = store.save(sourceImage(4000, 3000).toString(), "CT-A-RoS")

        val decoded = android.graphics.BitmapFactory.decodeFile(store.resolvePath(fileName))
        assertThat(decoded).isNotNull()
        assertThat(maxOf(decoded.width, decoded.height)).isAtMost(1024)
        decoded.recycle()
    }

    @Test
    fun sanitizesACodeThatIsIllegalInAFileName() = runBlocking {
        val fileName = store.save(sourceImage().toString(), "CT/A\\RoS:1")

        assertThat(fileName).doesNotContain("/")
        assertThat(fileName).doesNotContain("\\")
        assertThat(fileName).doesNotContain(":")
        assertThat(File(store.resolvePath(fileName)).exists()).isTrue()
    }

    @Test
    fun givesEveryPickItsOwnFileName() = runBlocking {
        val first = store.save(sourceImage().toString(), "CT-A-RoS")
        Thread.sleep(2)
        val second = store.save(sourceImage().toString(), "CT-A-RoS")

        // A shared name would leave the image loader serving the bitmap it cached for the old file.
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun deletesAStoredImage() = runBlocking {
        val fileName = store.save(sourceImage().toString(), "CT-A-RoS")
        store.delete(fileName)

        assertThat(File(store.resolvePath(fileName)).exists()).isFalse()
    }

    @Test
    fun reportsAnUnreadableSourceInsteadOfStoringAnEmptyFile() = runBlocking {
        val missing = Uri.fromFile(File(context.cacheDir, "does_not_exist.jpg"))

        val result = runCatching { store.save(missing.toString(), "CT-A-RoS") }

        assertThat(result.isFailure).isTrue()
    }
}
