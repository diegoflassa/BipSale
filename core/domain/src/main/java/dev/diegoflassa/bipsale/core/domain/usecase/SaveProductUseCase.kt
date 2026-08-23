package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

class SaveProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        code: String,
        name: String,
        price: Double,
        imageFileName: String? = null,
        quantity: Int = 0
    ): Result<Product> =
        runCatching {
            require(code.isNotBlank()) { "Product code cannot be blank." }
            require(name.isNotBlank()) { "Product name cannot be blank." }
            require(price.isFinite() && price > 0.0) { "Product price must be greater than zero." }
            require(quantity >= 0) { "Product quantity cannot be negative." }
            val product = Product(
                code = code,
                name = name,
                price = price,
                qrCode = buildQrPayload(code, price),
                imageFileName = imageFileName,
                quantity = quantity
            )
            productRepository.insertProduct(product)
            product
        }

    companion object {
        fun buildQrPayload(code: String, price: Double): String =
            "bipsale://product?code=$code&price=$price"
    }
}
