package dev.diegoflassa.bipsale.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = BipSaleDatabase.VERSION,
    exportSchema = true
)
abstract class BipSaleDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    companion object {
        const val NAME = "bipsale_database"
        const val VERSION = 1

        /**
         * Every migration, oldest first. This array is both what the builder registers and what
         * the migration test drives, so a migration that gets written but never registered fails
         * in CI rather than on a terminal that can no longer open its sales history.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
