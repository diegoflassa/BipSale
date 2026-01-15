package dev.diegoflassa.bipsale.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey 
    val id: String,
    val customerName: String?,
    val customerCpf: String?,
    val totalAmount: Double,
    val discountPercentage: Double,
    val finalAmount: Double,
    val paymentMethod: String,
    val date: Long,
    val isSynced: Boolean = false
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)
