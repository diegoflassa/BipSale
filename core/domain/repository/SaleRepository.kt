package dev.diegoflassa.bipsale.core.domain.repository

import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getAllSales(): Flow<List<Sale>>
    suspend fun getSaleById(saleId: String): Sale?
    suspend fun insertFullSale(sale: Sale)
    fun searchSales(query: String): Flow<List<Sale>>
    fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>>
}
