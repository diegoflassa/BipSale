package dev.diegoflassa.bipsale.core.data.dao

import androidx.room.*
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Transaction
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    fun getSaleWithItemsById(saleId: String): Flow<SaleWithItems?>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: String): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertFullSale(sale: SaleEntity, items: List<SaleItemEntity>) {
        insertSale(sale)
        insertSaleItems(items)
    }

    @Query("SELECT * FROM sales WHERE customerName LIKE '%' || :query || '%' OR customerCpf LIKE '%' || :query || '%'")
    fun searchSales(query: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date BETWEEN :startDate AND :endDate")
    fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>
}
