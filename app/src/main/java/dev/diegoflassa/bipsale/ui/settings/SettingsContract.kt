package dev.diegoflassa.bipsale.ui.settings

import androidx.compose.runtime.Immutable
import dev.diegoflassa.bipsale.core.ui.util.UiText
import dev.diegoflassa.bipsale.feature.sales.components.ItemDiscountKind

class SettingsContract {

    @Immutable
    data class State(
        val pixKey: String = "",
        val merchantName: String = "",
        val merchantCity: String = "",
        val discountKind: ItemDiscountKind = ItemDiscountKind.PERCENTAGE,
        /** Raw text; blank means no default discount, exactly as in the cart dialog. */
        val discountInput: String = "",
        val askCustomerInfo: Boolean = true,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false
    ) {
        val canSave: Boolean get() = !isLoading && !isSaving
    }

    sealed interface Intent {
        data class PixKeyChanged(val value: String) : Intent
        data class MerchantNameChanged(val value: String) : Intent
        data class MerchantCityChanged(val value: String) : Intent
        data class DiscountKindChanged(val kind: ItemDiscountKind) : Intent
        data class DiscountValueChanged(val value: String) : Intent
        data class AskCustomerInfoChanged(val enabled: Boolean) : Intent
        data object Save : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: UiText) : Effect
        data object Saved : Effect
    }
}
