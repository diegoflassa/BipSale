package dev.diegoflassa.bipsale.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.diegoflassa.bipsale.core.data.dao.ProductDao
import dev.diegoflassa.bipsale.core.data.dao.SaleDao
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BipSaleDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
}
