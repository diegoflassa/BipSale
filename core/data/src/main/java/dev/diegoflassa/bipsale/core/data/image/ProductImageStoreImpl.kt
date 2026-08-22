package dev.diegoflassa.bipsale.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProductImageStore {

    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }

    override suspend fun save(sourceUri: String, productCode: String): String =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            // A fresh name per pick: reusing one would leave the image loader serving the bitmap
            // it already cached under that path.
            val fileName = "${sanitize(productCode)}_${System.currentTimeMillis()}.jpg"
            val destination = File(imagesDir, fileName)

            val bitmap = decodeDownscaled(uri)
                ?: error("Could not decode image at $sourceUri")
            try {
                FileOutputStream(destination).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                }
            } finally {
                bitmap.recycle()
            }

            Timber.d(
                "[BipSale][Product][IMAGE] Stored image name=%s bytes=%d",
                fileName,
                destination.length()
            )
            fileName
        }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) {
            val deleted = File(imagesDir, fileName).delete()
            Timber.d("[BipSale][Product][IMAGE] Delete name=%s deleted=%b", fileName, deleted)
        }
    }

    override fun resolvePath(fileName: String): String = File(imagesDir, fileName).absolutePath

    override suspend fun listFileNames(): List<String> = withContext(Dispatchers.IO) {
        imagesDir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
    }

    override suspend fun readBytes(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(imagesDir, fileName)
        if (!file.exists()) {
            Timber.w("[BipSale][Product][IMAGE] Missing file while reading name=%s", fileName)
            return@withContext null
        }
        file.readBytes()
    }

    override suspend fun writeBytes(fileName: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            // Names come out of an archive, so a crafted entry could otherwise escape the folder.
            val safeName = File(fileName).name
            File(imagesDir, safeName).writeBytes(bytes)
            Timber.d(
                "[BipSale][Product][IMAGE] Restored image name=%s bytes=%d", safeName, bytes.size
            )
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            val removed = imagesDir.listFiles()?.count { it.delete() } ?: 0
            Timber.d("[BipSale][Product][IMAGE] Cleared image store, removed=%d", removed)
        }
    }

    /**
     * Samples the source down while decoding so a 12 MP photo never reaches the heap at full size.
     */
    private fun decodeDownscaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // With inJustDecodeBounds set, decodeStream returns null on SUCCESS — the dimensions land
        // in `bounds`. Null-check the stream, never the decode result, or every image is rejected.
        val boundsStream = context.contentResolver.openInputStream(uri)
        if (boundsStream == null) {
            Timber.e("[BipSale][Product][IMAGE] Could not open a stream for %s", uri)
            return null
        }
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Timber.e(
                "[BipSale][Product][IMAGE] Source reports no usable size (%dx%d) for %s",
                bounds.outWidth, bounds.outHeight, uri
            )
            return null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (decoded == null) {
            Timber.e("[BipSale][Product][IMAGE] Decode produced no bitmap for %s", uri)
            return null
        }
        Timber.d(
            "[BipSale][Product][IMAGE] Decoded %dx%d (sample=%d) from %dx%d",
            decoded.width, decoded.height, options.inSampleSize, bounds.outWidth, bounds.outHeight
        )

        return applyExifRotation(uri, decoded)
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_DIMENSION_PX || height / sampleSize > MAX_DIMENSION_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Camera photos carry their orientation in EXIF; without this they store sideways. */
    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                when (
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrElse { throwable ->
            Timber.e(throwable, "[BipSale][Product][IMAGE] Could not read EXIF orientation")
            0f
        }

        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun sanitize(productCode: String): String =
        productCode.replace(UNSAFE_FILE_NAME_CHARS, "_").take(MAX_CODE_CHARS).ifBlank { "product" }

    private companion object {
        const val IMAGES_DIR = "product_images"
        const val MAX_DIMENSION_PX = 1024
        const val JPEG_QUALITY = 85
        const val MAX_CODE_CHARS = 48
        val UNSAFE_FILE_NAME_CHARS = Regex("[^A-Za-z0-9-_]")
    }
}
