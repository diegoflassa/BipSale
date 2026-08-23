package dev.diegoflassa.bipsale.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getFullSaleById(saleId: String): SaleWithItems?

    @Transaction
    @Query("SELECT * FROM sales WHERE customerName LIKE '%' || :query || '%' OR customerCpf LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchSalesWithItems(query: String): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSalesByDateRangeWithItems(startDate: Long, endDate: Long): Flow<List<SaleWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    /**
     * Stock lives on `products`, but it moves in the same transaction as the sale that moved it —
     * a committed sale whose stock decrement was rolled back is inventory nobody can reconcile.
     * That is why this query sits in the sale DAO rather than the product one.
     */
    @Query("UPDATE products SET quantity = MAX(0, quantity - :soldUnits) WHERE productCode = :code")
    suspend fun decrementProductStock(code: String, soldUnits: Int)

    @Transaction
    suspend fun insertFullSale(sale: SaleEntity, items: List<SaleItemEntity>) {
        insertSale(sale)
        insertSaleItems(items)
        items.forEach { decrementProductStock(it.productCode, it.quantity) }
    }

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesOnce(): List<SaleEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsOnce(): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun deleteAllSaleItems()
}
