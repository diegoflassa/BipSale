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
    val quantity: Int,
    /** `NONE` / `PERCENTAGE` / `AMOUNT` — the domain's `ItemDiscount` flattened for storage. */
    val discountType: String = DISCOUNT_TYPE_NONE,
    val discountValue: Double = 0.0
) {
    companion object {
        const val DISCOUNT_TYPE_NONE = "NONE"
        const val DISCOUNT_TYPE_PERCENTAGE = "PERCENTAGE"
        const val DISCOUNT_TYPE_AMOUNT = "AMOUNT"
    }
}
