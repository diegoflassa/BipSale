package dev.diegoflassa.bipsale.feature.sales

import androidx.compose.runtime.Immutable
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.settings.PixField
import dev.diegoflassa.bipsale.core.ui.util.UiText

class SalesContract {

    /** A registered product as the catalogue picker and the cart's detail sheet show it. */
    @Immutable
    data class CatalogProduct(
        val code: String,
        val name: String,
        val price: Double,
        /** Absolute path to the product photo, already resolved so the screen never reads disk. */
        val imagePath: String? = null,
        val quantity: Int = 0
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
        /** Null until the operator picks one — the dropdown opens on "select", never on a guess. */
        val paymentMethod: PaymentMethod? = null,
        /** The PIX "copia e cola" string for the current total, once PIX is picked and configured. */
        val pixPayload: String? = null,
        /** Which PIX fields are still empty, so the screen can name them instead of guessing. */
        val missingPixFields: List<PixField> = emptyList(),
        /**
         * Set once a PIX sale is written. The screen keeps the code on display until the operator
         * dismisses it — navigating away the moment the sale lands takes the QR with it, before
         * the customer has had a chance to scan anything.
         */
        val isAwaitingPixPayment: Boolean = false,
        /** The line whose product the read-only detail sheet is showing. */
        val detailItemId: String? = null,
        val isFinalizing: Boolean = false,
        val isSaleFinished: Boolean = false
    ) {
        // A sale with no payment method recorded cannot be reconciled against anything, so the
        // method is part of what makes the cart finalizable rather than a field with a default.
        val canFinalize: Boolean
            get() = items.isNotEmpty() && paymentMethod != null && !isFinalizing && !isSaleFinished

        val detailItem: SaleItem?
            get() = items.firstOrNull { it.id == detailItemId }

        fun catalogEntry(code: String): CatalogProduct? = catalog.firstOrNull { it.code == code }

        fun imagePathFor(code: String): String? = catalogEntry(code)?.imagePath
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
        data class ShowProductDetail(val itemId: String) : Intent
        data object HideProductDetail : Intent
        data object PixPaymentAcknowledged : Intent
        data object FinalizeSale : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: UiText) : Effect
    }
}
