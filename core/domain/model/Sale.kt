package dev.diegoflassa.bipsale.core.domain.model

data class Sale(
    val id: String,
    val customerName: String,
    val customerCpf: String,
    val totalAmount: Double,
    val discountPercentage: Double,
    val finalAmount: Double,
    val paymentMethod: String,
    val date: Long,
    val items: List<SaleItem> = emptyList()
)
