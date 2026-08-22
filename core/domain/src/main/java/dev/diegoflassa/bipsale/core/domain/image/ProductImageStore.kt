package dev.diegoflassa.bipsale.core.domain.image

/**
 * Owns the product image files. Images are copied into app-private storage on pick, so the store
 * addresses them by file name only — an absolute path would embed the Android user id and break
 * after a backup restore or on a secondary profile.
 */
interface ProductImageStore {
    /** Copies the image behind [sourceUri] into the store and returns its file name. */
    suspend fun save(sourceUri: String, productCode: String): String

    suspend fun delete(fileName: String)

    /** Absolute path for [fileName], for handing to an image loader. */
    fun resolvePath(fileName: String): String

    suspend fun listFileNames(): List<String>

    suspend fun readBytes(fileName: String): ByteArray?

    /** Writes an image restored from an archive, replacing any file of the same name. */
    suspend fun writeBytes(fileName: String, bytes: ByteArray)

    suspend fun deleteAll()
}
