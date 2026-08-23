package dev.diegoflassa.bipsale.core.data.mapper

import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.data.model.SaleWithItems
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
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
        id = id,
        saleId = saleId,
        productCode = productCode,
        productName = productName,
        unitPrice = unitPrice,
        quantity = quantity,
        discount = readDiscount()
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
    val (type, value) = when (val applied = discount) {
        is ItemDiscount.None -> SaleItemEntity.DISCOUNT_TYPE_NONE to 0.0
        is ItemDiscount.Percentage -> SaleItemEntity.DISCOUNT_TYPE_PERCENTAGE to applied.percent
        is ItemDiscount.Amount -> SaleItemEntity.DISCOUNT_TYPE_AMOUNT to applied.amount
    }
    return SaleItemEntity(
        id = id,
        saleId = saleId,
        productCode = productCode,
        productName = productName,
        unitPrice = unitPrice,
        quantity = quantity,
        discountType = type,
        discountValue = value
    )
}

/**
 * `ItemDiscount` rejects an out-of-range value at construction, so a row written by a future build
 * — or by a hand-edited backup — must be coerced here rather than crashing the history screen.
 */
private fun SaleItemEntity.readDiscount(): ItemDiscount {
    if (!discountValue.isFinite()) return ItemDiscount.None
    return when (discountType) {
        SaleItemEntity.DISCOUNT_TYPE_PERCENTAGE ->
            ItemDiscount.Percentage(discountValue.coerceIn(0.0, ItemDiscount.MAX_PERCENT))
        SaleItemEntity.DISCOUNT_TYPE_AMOUNT ->
            ItemDiscount.Amount(discountValue.coerceAtLeast(0.0))
        else -> ItemDiscount.None
    }
}
