package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getAllSales(): Flow<List<SaleEntity>>
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>
    suspend fun getSaleById(saleId: String): SaleEntity?
    fun getItemsForSale(saleId: String): Flow<List<SaleItemEntity>>
    suspend fun insertFullSale(sale: SaleEntity, items: List<SaleItemEntity>)
    fun searchSales(query: String): Flow<List<SaleEntity>>
    fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>
}
