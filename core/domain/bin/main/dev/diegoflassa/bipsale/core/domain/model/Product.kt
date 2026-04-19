package dev.diegoflassa.bipsale.core.domain.model

data class Product(
    val code: String,
    val name: String,
    val price: Double,
    val qrCode: String?
)
