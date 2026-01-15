package dev.diegoflassa.bipsale.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey 
    val productCode: String,
    val productName: String,
    val price: Double,
    val qrCodeData: String?,
    val lastUpdated: Long
)
