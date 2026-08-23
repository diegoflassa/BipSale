package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.model.saleTotals
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
        val totals = saleTotals(items, discountPercentage)
        val saleId = UUID.randomUUID().toString()
        val sale = Sale(
            id = saleId,
            customerName = customerName,
            customerCpf = customerCpf,
            totalAmount = totals.grossAmount,
            discountPercentage = discountPercentage,
            finalAmount = totals.finalAmount,
            paymentMethod = paymentMethod,
            date = System.currentTimeMillis(),
            items = items.map { it.copy(saleId = saleId) }
        )
        saleRepository.insertFullSale(sale)
        sale
    }
}
