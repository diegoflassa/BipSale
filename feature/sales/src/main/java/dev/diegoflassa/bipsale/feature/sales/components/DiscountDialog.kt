package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.bipsale.core.domain.model.MAX_SALE_DISCOUNT_PERCENTAGE
import dev.diegoflassa.bipsale.core.domain.util.parseDecimalInput
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

/**
 * Sets the sale-level discount percentage. Confirm stays disabled until the field holds something
 * in range, so an out-of-range value can never reach the cart in the first place.
 */
@Composable
fun DiscountDialog(
    currentPercentage: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember {
        mutableStateOf(if (currentPercentage > 0.0) formatPercent(currentPercentage) else "")
    }
    val parsed = parseDecimalInput(input)?.takeIf { it <= MAX_SALE_DISCOUNT_PERCENTAGE }

    AlertDialog(
        modifier = modifier.testTag(SalesScreenTestTags.DISCOUNT_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discount_dialog_title)) },
        text = {
            OutlinedTextField(
                modifier = Modifier.testTag(SalesScreenTestTags.DISCOUNT_FIELD),
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                isError = input.isNotBlank() && parsed == null,
                label = { Text(stringResource(R.string.discount_dialog_hint)) },
                supportingText = {
                    if (input.isNotBlank() && parsed == null) {
                        Text(stringResource(R.string.sales_discount_out_of_range))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = { parsed?.let(onConfirm) }
            ) {
                Text(stringResource(R.string.sales_discount_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (currentPercentage > 0.0) onConfirm(0.0) else onDismiss()
            }) {
                Text(
                    stringResource(
                        if (currentPercentage > 0.0) {
                            R.string.sales_discount_clear
                        } else {
                            R.string.sales_discount_cancel
                        }
                    )
                )
            }
        }
    )
}

/** Drops a trailing `.0` so a whole percentage reads as "10", not "10.0". */
private fun formatPercent(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

// region Previews

@Preview(name = "DiscountDialog · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "DiscountDialog · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun DiscountDialogEmptyPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            DiscountDialog(currentPercentage = 0.0, onConfirm = {}, onDismiss = {})
        }
    }
}

@Preview(name = "DiscountDialog · Preenchido · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "DiscountDialog · Preenchido · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun DiscountDialogFilledPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            DiscountDialog(currentPercentage = 15.0, onConfirm = {}, onDismiss = {})
        }
    }
}

@Preview(name = "DiscountDialog · Preenchido · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscountDialogFilledDarkPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            DiscountDialog(currentPercentage = 15.0, onConfirm = {}, onDismiss = {})
        }
    }
}

// endregion
