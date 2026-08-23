package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Shared by the use-case suites; `failWith` drives the failure paths. */
class FakeProductRepository(
    initial: List<Product> = emptyList(),
    private val failWith: Throwable? = null
) : ProductRepository {
    val stored = initial.toMutableList()

    override fun getAllProducts(): Flow<List<Product>> = flowOf(stored.toList())

    override suspend fun getProductByCode(code: String): Product? {
        failWith?.let { throw it }
        return stored.firstOrNull { it.code == code }
    }

    override suspend fun insertProduct(product: Product) {
        failWith?.let { throw it }
        stored += product
    }

    override suspend fun updateProduct(product: Product) = Unit

    override suspend fun deleteProduct(product: Product) {
        stored.remove(product)
    }
}
