package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.SaleDao
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao
) : SaleRepository {
    override fun getAllSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()
    override fun getAllSalesWithItems(): Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()

    override suspend fun getSaleById(saleId: String): SaleEntity? = saleDao.getSaleById(saleId)

    override fun getItemsForSale(saleId: String): Flow<List<SaleItemEntity>> = saleDao.getItemsForSale(saleId)

    override suspend fun insertFullSale(sale: SaleEntity, items: List<SaleItemEntity>) = saleDao.insertFullSale(sale, items)

    override fun searchSales(query: String): Flow<List<SaleEntity>> = saleDao.searchSales(query)

    override fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>> = saleDao.getSalesByDateRange(startDate, endDate)
}
