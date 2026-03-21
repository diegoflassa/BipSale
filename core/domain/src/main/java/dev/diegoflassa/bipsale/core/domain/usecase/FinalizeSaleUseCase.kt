package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import java.util.UUID
import javax.inject.Inject

class FinalizeSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(
        customerName: String,
        customerCpf: String,
        items: List<SaleItem>,
        discountPercentage: Double,
        paymentMethod: PaymentMethod
    ): Result<Sale> = runCatching {
        require(items.isNotEmpty()) { "Cannot finalize a sale with no items." }
        val saleId = UUID.randomUUID().toString()
        val total = items.sumOf { it.unitPrice * it.quantity }
        val finalAmount = total - total * (discountPercentage / 100.0)
        val sale = Sale(
            id = saleId,
            customerName = customerName,
            customerCpf = customerCpf,
            totalAmount = total,
            discountPercentage = discountPercentage,
            finalAmount = finalAmount,
            paymentMethod = paymentMethod,
            date = System.currentTimeMillis(),
            items = items.map { it.copy(saleId = saleId) }
        )
        saleRepository.insertFullSale(sale)
        sale
    }
}
