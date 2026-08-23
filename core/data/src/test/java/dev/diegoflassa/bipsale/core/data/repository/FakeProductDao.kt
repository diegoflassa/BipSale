package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.ProductDao
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Stands in for Room so the repository's own error handling is what the suite exercises.
 * `failWith` is what a closed database, a constraint violation or a disk error looks like here.
 */
class FakeProductDao(
    initial: List<ProductEntity> = emptyList(),
    private val failWith: Throwable? = null
) : ProductDao {

    val rows = initial.toMutableList()

    override fun getAllProducts(): Flow<List<ProductEntity>> = flowOf(rows.toList())

    override suspend fun getProductByCode(code: String): ProductEntity? {
        failWith?.let { throw it }
        return rows.firstOrNull { it.productCode == code }
    }

    override suspend fun insertProduct(product: ProductEntity) {
        failWith?.let { throw it }
        rows.removeAll { it.productCode == product.productCode }
        rows += product
    }

    override suspend fun updateProduct(product: ProductEntity) {
        failWith?.let { throw it }
        rows.replaceAll { if (it.productCode == product.productCode) product else it }
    }

    override suspend fun deleteProduct(product: ProductEntity) {
        failWith?.let { throw it }
        rows.removeAll { it.productCode == product.productCode }
    }

    override suspend fun getAllProductsOnce(): List<ProductEntity> = rows.toList()

    override suspend fun insertProducts(products: List<ProductEntity>) {
        rows += products
    }

    override suspend fun deleteAllProducts() {
        rows.clear()
    }
}
