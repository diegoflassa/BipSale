package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

class SaveProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(code: String, name: String, price: Double): Result<Product> =
        runCatching {
            require(code.isNotBlank()) { "Product code cannot be blank." }
            require(name.isNotBlank()) { "Product name cannot be blank." }
            val qrData = "bipsale://product?code=$code&price=$price"
            val product = Product(code = code, name = name, price = price, qrCode = qrData)
            productRepository.insertProduct(product)
            product
        }
}
