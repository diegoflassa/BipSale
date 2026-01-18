package dev.diegoflassa.bipsale.core.data.mapper

import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.domain.model.Product

fun ProductEntity.toDomain(): Product {
    return Product(
        code = productCode,
        name = productName,
        price = price,
        qrCode = qrCodeData
    )
}

fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        productCode = code,
        productName = name,
        price = price,
        qrCodeData = qrCode,
        lastUpdated = System.currentTimeMillis()
    )
}
