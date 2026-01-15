package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.ProductDao
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {
    override fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    override suspend fun getProductByCode(code: String): ProductEntity? = productDao.getProductByCode(code)

    override suspend fun insertProduct(product: ProductEntity) = productDao.insertProduct(product)

    override suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    override suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)
}
