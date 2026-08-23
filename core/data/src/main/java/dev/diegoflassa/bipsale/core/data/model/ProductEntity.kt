package dev.diegoflassa.bipsale.core.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val productCode: String,
    val productName: String,
    val price: Double,
    val qrCodeData: String?,
    val imageFileName: String? = null,
    val lastUpdated: Long,
    // Declared so the column a migration adds to existing rows and the one a fresh install creates
    // are the same column; without it the two shapes differ and Room rejects the migrated database.
    @ColumnInfo(defaultValue = "0")
    val quantity: Int = 0
)
