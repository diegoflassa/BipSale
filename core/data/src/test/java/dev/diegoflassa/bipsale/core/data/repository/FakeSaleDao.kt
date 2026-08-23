package dev.diegoflassa.bipsale.core.data.repository

import dev.diegoflassa.bipsale.core.data.dao.SaleDao
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Stands in for Room so the repository's own error handling is what the suite exercises.
 * `failWith` is what a closed database, a constraint violation or a disk error looks like here.
 */
class FakeSaleDao(
    initial: List<SaleWithItems> = emptyList(),
    private val failWith: Throwable? = null
) : SaleDao {

    val sales = initial.map { it.sale }.toMutableList()
    val items = initial.flatMap { it.items }.toMutableList()

    /** Units taken off each product code, so a test can assert stock moved with the sale. */
    val stockTaken = mutableMapOf<String, Int>()

    override suspend fun decrementProductStock(code: String, soldUnits: Int) {
        stockTaken[code] = (stockTaken[code] ?: 0) + soldUnits
    }

    private fun assemble(sale: SaleEntity) =
        SaleWithItems(sale, items.filter { it.saleId == sale.id })

    override fun getAllSalesWithItems(): Flow<List<SaleWithItems>> =
        flowOf(sales.sortedByDescending { it.date }.map(::assemble))

    override suspend fun getFullSaleById(saleId: String): SaleWithItems? {
        failWith?.let { throw it }
        return sales.firstOrNull { it.id == saleId }?.let(::assemble)
    }

    override fun searchSalesWithItems(query: String): Flow<List<SaleWithItems>> =
        flowOf(
            sales.filter {
                it.customerName.orEmpty().contains(query, ignoreCase = true) ||
                    it.customerCpf.orEmpty().contains(query, ignoreCase = true)
            }.map(::assemble)
        )

    override fun getSalesByDateRangeWithItems(
        startDate: Long,
        endDate: Long
    ): Flow<List<SaleWithItems>> =
        flowOf(sales.filter { it.date in startDate..endDate }.map(::assemble))

    override suspend fun insertSale(sale: SaleEntity) {
        failWith?.let { throw it }
        sales += sale
    }

    override suspend fun insertSaleItems(items: List<SaleItemEntity>) {
        failWith?.let { throw it }
        this.items += items
    }

    override suspend fun getAllSalesOnce(): List<SaleEntity> = sales.toList()

    override suspend fun getAllSaleItemsOnce(): List<SaleItemEntity> = items.toList()

    override suspend fun insertSales(sales: List<SaleEntity>) {
        this.sales += sales
    }

    override suspend fun deleteAllSales() {
        sales.clear()
    }

    override suspend fun deleteAllSaleItems() {
        items.clear()
    }
}
