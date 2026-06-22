package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.ProductDao
import dev.diegoflassa.bipsale.core.data.mapper.toDomain
import dev.diegoflassa.bipsale.core.data.mapper.toEntity
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getProductByCode(code: String): Product? {
        return try {
            productDao.getProductByCode(code)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "[BipSale][Product] Error fetching product by code: $code")
            null
        }
    }

    override suspend fun insertProduct(product: Product) {
        try {
            productDao.insertProduct(product.toEntity())
        } catch (e: Exception) {
            Timber.e(e, "[BipSale][Product] Error inserting product: ${product.code}")
            throw e
        }
    }

    override suspend fun updateProduct(product: Product) {
        try {
            productDao.updateProduct(product.toEntity())
        } catch (e: Exception) {
            Timber.e(e, "[BipSale][Product] Error updating product: ${product.code}")
            throw e
        }
    }

    override suspend fun deleteProduct(product: Product) {
        try {
            productDao.deleteProduct(product.toEntity())
        } catch (e: Exception) {
            Timber.e(e, "[BipSale][Product] Error deleting product: ${product.code}")
            throw e
        }
    }
}
