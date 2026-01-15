package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<ProductEntity>>
    suspend fun getProductByCode(code: String): ProductEntity?
    suspend fun insertProduct(product: ProductEntity)
    suspend fun updateProduct(product: ProductEntity)
    suspend fun deleteProduct(product: ProductEntity)
}
