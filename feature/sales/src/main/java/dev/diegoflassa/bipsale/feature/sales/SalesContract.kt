package dev.diegoflassa.bipsale.feature.sales

import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity

class SalesContract {
    data class State(
        val customerName: String = "",
        val customerCpf: String = "",
        val isAnonymous: Boolean = false,
        val items: List<SaleItemEntity> = emptyList(),
        val discountPercentage: Double = 0.0,
        val totalAmount: Double = 0.0,
        val finalAmount: Double = 0.0,
        val isSaleFinished: Boolean = false
    )

    sealed interface Intent {
        data class UpdateCustomerInfo(val name: String, val cpf: String, val anonymous: Boolean) : Intent
        data class AddProductByQr(val qrData: String) : Intent
        data class AddManualItem(val code: String, val name: String, val price: Double) : Intent
        data class RemoveItem(val item: SaleItemEntity) : Intent
        data class UpdateDiscount(val percentage: Double) : Intent
        data object FinalizeSale : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: String) : Effect
    }
}
