package dev.diegoflassa.bipsale.feature.sales

import androidx.compose.runtime.Immutable
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.util.UiText

class SalesContract {

    /** A registered product as the catalogue picker lists it. */
    @Immutable
    data class CatalogProduct(
        val code: String,
        val name: String,
        val price: Double
    )

    @Immutable
    data class State(
        val customerName: String = "",
        val customerCpf: String = "",
        val isAnonymous: Boolean = false,
        val items: List<SaleItem> = emptyList(),
        val catalog: List<CatalogProduct> = emptyList(),
        val discountPercentage: Double = 0.0,
        /** The lines before any discount. */
        val totalAmount: Double = 0.0,
        /** What the per-line discounts took off. */
        val itemDiscountAmount: Double = 0.0,
        /** What the customer pays. */
        val finalAmount: Double = 0.0,
        val paymentMethod: PaymentMethod = PaymentMethod.PIX,
        val isFinalizing: Boolean = false,
        val isSaleFinished: Boolean = false
    ) {
        val canFinalize: Boolean
            get() = items.isNotEmpty() && !isFinalizing && !isSaleFinished
    }

    sealed interface Intent {
        data class UpdateCustomerInfo(val name: String, val cpf: String, val anonymous: Boolean) :
            Intent

        data class AddProductByQr(val qrData: String) : Intent

        /** Behind both the catalogue picker and the manual code field. */
        data class AddProductByCode(val code: String) : Intent
        data class RemoveItem(val itemId: String) : Intent
        data class UpdateItemDiscount(val itemId: String, val discount: ItemDiscount) : Intent
        data class UpdateDiscount(val percentage: Double) : Intent
        data class SelectPaymentMethod(val method: PaymentMethod) : Intent
        data object FinalizeSale : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: UiText) : Effect
    }
}
