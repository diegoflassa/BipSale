package dev.diegoflassa.bipsale.feature.products

import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore

/** In-memory stand-in for the on-disk store, so the ViewModel suite stays a plain JVM test. */
class FakeProductImageStore(private val failOnSave: Throwable? = null) : ProductImageStore {

    val files = mutableMapOf<String, ByteArray>()
    val deleted = mutableListOf<String>()
    var savedFromUri: String? = null

    override suspend fun save(sourceUri: String, productCode: String): String {
        failOnSave?.let { throw it }
        savedFromUri = sourceUri
        val fileName = "${productCode.lowercase()}_${files.size + 1}.jpg"
        files[fileName] = ByteArray(0)
        return fileName
    }

    override suspend fun delete(fileName: String) {
        files.remove(fileName)
        deleted += fileName
    }

    override fun resolvePath(fileName: String): String = "/data/product_images/$fileName"

    override suspend fun listFileNames(): List<String> = files.keys.toList()

    override suspend fun readBytes(fileName: String): ByteArray? = files[fileName]

    override suspend fun writeBytes(fileName: String, bytes: ByteArray) {
        files[fileName] = bytes
    }

    override suspend fun deleteAll() {
        files.clear()
    }
}
