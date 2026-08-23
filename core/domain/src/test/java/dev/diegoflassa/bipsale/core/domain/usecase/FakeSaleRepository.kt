package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Shared by the use-case suites; `failWith` drives the failure paths. */
class FakeSaleRepository(private val failWith: Throwable? = null) : SaleRepository {
    val inserted = mutableListOf<Sale>()

    override fun getAllSales(): Flow<List<Sale>> = flowOf(inserted.toList())

    override suspend fun getSaleById(saleId: String): Sale? =
        inserted.firstOrNull { it.id == saleId }

    override suspend fun insertFullSale(sale: Sale) {
        failWith?.let { throw it }
        inserted += sale
    }

    override fun searchSales(query: String): Flow<List<Sale>> = flowOf(emptyList())

    override fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>> =
        flowOf(emptyList())
}
