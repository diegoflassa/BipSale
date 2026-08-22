package dev.diegoflassa.bipsale.core.domain.model

data class Product(
    val code: String,
    val name: String,
    val price: Double,
    val qrCode: String?,
    /** Name of the image file inside the product image store, never an absolute path. */
    val imageFileName: String? = null
)
