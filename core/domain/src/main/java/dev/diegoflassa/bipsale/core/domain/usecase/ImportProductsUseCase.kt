package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.product.ProductImportReport
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRepository
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

/** Raised instead of reporting a success for a file that produced nothing to write. */
class NoProductsToImport : IllegalStateException("No products to import")

class ImportProductsUseCase @Inject constructor(
    private val productImportRepository: ProductImportRepository,
    private val productRepository: ProductRepository
) {
    /**
     * Reads the sheet and writes every accepted row. A product whose code already exists is
     * overwritten — re-importing a corrected sheet is how an operator fixes a typo, so a second
     * import has to update rather than fail.
     */
    suspend operator fun invoke(sourceUri: String): Result<ProductImportReport> = runCatching {
        val report = productImportRepository.read(sourceUri)
        if (report.accepted.isEmpty()) throw NoProductsToImport()

        report.accepted.forEach { imported ->
            productRepository.insertProduct(
                Product(
                    code = imported.code,
                    name = imported.name,
                    price = imported.price,
                    qrCode = SaveProductUseCase.buildQrPayload(imported.code, imported.price),
                    // Images are attached per product after the import; a sheet cannot carry them.
                    imageFileName = null,
                    quantity = imported.quantity
                )
            )
        }
        report
    }

    suspend fun writeTemplate(destinationUri: String): Result<Unit> = runCatching {
        productImportRepository.writeTemplate(destinationUri)
    }

    fun suggestedTemplateName(): String = productImportRepository.suggestedTemplateName()
}
