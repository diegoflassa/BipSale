package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Adds a registered product to the cart by its code — the path behind picking from the catalogue
 * and behind typing a code when a label will not scan.
 *
 * Unlike the QR path this refuses an unknown code: a scanned label carries its own price, but a
 * hand-typed code that matches nothing would otherwise ring up a R$ 0,00 line under its own name.
 */
class AddProductByCodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(code: String): Result<SaleItem> = runCatching {
        val trimmed = code.trim()
        require(trimmed.isNotBlank()) { "Product code cannot be blank." }
        val product = productRepository.getProductByCode(trimmed)
            ?: throw NoSuchElementException("No product is registered under code $trimmed.")
        SaleItem(
            saleId = "",
            productCode = product.code,
            productName = product.name,
            unitPrice = product.price,
            quantity = 1
        )
    }
}
