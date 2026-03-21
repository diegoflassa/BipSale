package dev.diegoflassa.bipsale.feature.sales

import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.util.UiText

class SalesContract {
    data class State(
        val customerName: String = "",
        val customerCpf: String = "",
        val isAnonymous: Boolean = false,
        val items: List<SaleItem> = emptyList(),
        val discountPercentage: Double = 0.0,
        val totalAmount: Double = 0.0,
        val finalAmount: Double = 0.0,
        val paymentMethod: PaymentMethod = PaymentMethod.PIX,
        val isSaleFinished: Boolean = false
    )

    sealed interface Intent {
        data class UpdateCustomerInfo(val name: String, val cpf: String, val anonymous: Boolean) : Intent
        data class AddProductByQr(val qrData: String) : Intent
        data class AddManualItem(val code: String, val name: String, val price: Double) : Intent
        data class RemoveItem(val item: SaleItem) : Intent
        data class UpdateDiscount(val percentage: Double) : Intent
        data class SelectPaymentMethod(val method: PaymentMethod) : Intent
        data object FinalizeSale : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: UiText) : Effect
    }
}
