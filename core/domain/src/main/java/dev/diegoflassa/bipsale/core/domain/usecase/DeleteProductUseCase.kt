package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val productImageStore: ProductImageStore
) {
    suspend operator fun invoke(product: Product): Result<Unit> =
        runCatching {
            productRepository.deleteProduct(product)
            product.imageFileName?.let { productImageStore.delete(it) }
        }
}
