package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.SaleDao
import dev.diegoflassa.bipsale.core.data.mapper.toDomain
import dev.diegoflassa.bipsale.core.data.mapper.toEntity
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao
) : SaleRepository {

    override fun getAllSales(): Flow<List<Sale>> =
        saleDao.getAllSalesWithItems().map { list -> list.map { it.toDomain() } }

    override suspend fun getSaleById(saleId: String): Sale? =
        runCatching { saleDao.getFullSaleById(saleId)?.toDomain() }
            .onFailure { Timber.e(it, "[BipSale][Sale] Error fetching sale by id: $saleId") }
            .getOrNull()

    override suspend fun insertFullSale(sale: Sale) {
        // The customer fields never reach the log: id, counts and total are what reconciles a sale
        // that failed at a terminal, and none of them identify anybody (CORE_RULES section 8.3).
        Timber.d(
            "[BipSale][Sale][CHECKOUT] Persisting sale id=%s items=%d total=%.2f final=%.2f",
            sale.id, sale.items.size, sale.totalAmount, sale.finalAmount
        )
        runCatching { saleDao.insertFullSale(sale.toEntity(), sale.items.map { it.toEntity() }) }
            .onSuccess {
                Timber.i(
                    "[BipSale][Sale][CHECKOUT] Sale persisted id=%s items=%d final=%.2f",
                    sale.id, sale.items.size, sale.finalAmount
                )
            }
            .onFailure { throwable ->
                Timber.e(
                    throwable,
                    "[BipSale][Sale][CHECKOUT] Persisting sale failed id=%s items=%d final=%.2f",
                    sale.id, sale.items.size, sale.finalAmount
                )
                throw throwable
            }
    }

    override fun searchSales(query: String): Flow<List<Sale>> =
        saleDao.searchSalesWithItems(query).map { list -> list.map { it.toDomain() } }

    override fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>> =
        saleDao.getSalesByDateRangeWithItems(startDate, endDate).map { list -> list.map { it.toDomain() } }
}
