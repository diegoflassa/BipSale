package dev.diegoflassa.bipsale.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productCode: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int
)
