package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import javax.inject.Inject

class SaveProductImageUseCase @Inject constructor(
    private val productImageStore: ProductImageStore
) {
    /**
     * Copies the picked image in and drops [previousFileName], whose row is about to stop
     * referencing it.
     */
    suspend operator fun invoke(
        sourceUri: String,
        productCode: String,
        previousFileName: String? = null
    ): Result<String> =
        runCatching {
            val fileName = productImageStore.save(sourceUri, productCode)
            if (previousFileName != null && previousFileName != fileName) {
                productImageStore.delete(previousFileName)
            }
            fileName
        }
}
