package dev.diegoflassa.bipsale.core.data.mapper

import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem

fun SaleWithItems.toDomain(): Sale {
    return Sale(
        id = sale.id,
        customerName = sale.customerName ?: "",
        customerCpf = sale.customerCpf ?: "",
        totalAmount = sale.totalAmount,
        discountPercentage = sale.discountPercentage,
        finalAmount = sale.finalAmount,
        paymentMethod = PaymentMethod.fromString(sale.paymentMethod),
        date = sale.date,
        items = items.map { it.toDomain() }
    )
}

fun SaleItemEntity.toDomain(): SaleItem {
    return SaleItem(
        saleId = saleId,
        productCode = productCode,
        productName = productName,
        unitPrice = unitPrice,
        quantity = quantity
    )
}

fun Sale.toEntity(): SaleEntity {
    return SaleEntity(
        id = id,
        customerName = customerName,
        customerCpf = customerCpf,
        totalAmount = totalAmount,
        discountPercentage = discountPercentage,
        finalAmount = finalAmount,
        paymentMethod = paymentMethod.serializedName,
        date = date
    )
}

fun SaleItem.toEntity(): SaleItemEntity {
    return SaleItemEntity(
        saleId = saleId,
        productCode = productCode,
        productName = productName,
        unitPrice = unitPrice,
        quantity = quantity
    )
}
