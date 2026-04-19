package dev.diegoflassa.bipsale.core.domain.model

data class SaleItem(
    val saleId: String,
    val productCode: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int
)
