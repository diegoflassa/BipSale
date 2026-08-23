package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R

/**
 * Opens on "select" rather than on a guess. A pre-picked method is the one nobody looks at, and a
 * sale filed under the wrong one cannot be reconciled against the drawer or the card statement.
 */
@Composable
fun PaymentMethodDropdown(
    selectedMethod: PaymentMethod?,
    onMethodSelected: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(stringResource(selectedMethod?.labelRes() ?: R.string.sales_payment_method_select))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(stringResource(method.labelRes())) },
                    onClick = {
                        onMethodSelected(method)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** The wire name (`CREDIT_CARD`) is a storage detail; the operator sees the translated label. */
@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.PIX -> R.string.payment_method_pix
    PaymentMethod.CASH -> R.string.payment_method_cash
    PaymentMethod.CREDIT_CARD -> R.string.payment_method_credit
    PaymentMethod.DEBIT_CARD -> R.string.payment_method_debit
}

// region Previews

@Preview(name = "PaymentMethodDropdown · PIX · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "PaymentMethodDropdown · PIX · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun PaymentMethodDropdownPixPreview() {
    BipSaleTheme {
        PaymentMethodDropdown(selectedMethod = PaymentMethod.PIX, onMethodSelected = {})
    }
}

@Preview(name = "PaymentMethodDropdown · Crédito · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "PaymentMethodDropdown · Crédito · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun PaymentMethodDropdownCreditPreview() {
    BipSaleTheme {
        PaymentMethodDropdown(selectedMethod = PaymentMethod.CREDIT_CARD, onMethodSelected = {})
    }
}

@Preview(name = "PaymentMethodDropdown · PIX · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentMethodDropdownPixDarkPreview() {
    BipSaleTheme {
        PaymentMethodDropdown(selectedMethod = PaymentMethod.PIX, onMethodSelected = {})
    }
}

// endregion
