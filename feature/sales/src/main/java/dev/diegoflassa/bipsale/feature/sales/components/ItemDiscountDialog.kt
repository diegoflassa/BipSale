package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.util.parseDecimalInput
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

/**
 * Discounts one cart line, either as a percentage of it or as a fixed amount off it.
 *
 * The amount is bounded by the line total rather than left open: a fixed discount larger than the
 * line would otherwise read as paying the customer to take it.
 */
@Composable
fun ItemDiscountDialog(
    productName: String,
    lineTotal: Double,
    currentDiscount: ItemDiscount,
    onConfirm: (ItemDiscount) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var kind by remember {
        mutableStateOf(
            when (currentDiscount) {
                is ItemDiscount.Amount -> ItemDiscountKind.AMOUNT
                else -> ItemDiscountKind.PERCENTAGE
            }
        )
    }
    var input by remember {
        mutableStateOf(
            when (currentDiscount) {
                is ItemDiscount.None -> ""
                is ItemDiscount.Percentage -> formatValue(currentDiscount.percent)
                is ItemDiscount.Amount -> formatValue(currentDiscount.amount)
            }
        )
    }

    val upperBound = when (kind) {
        ItemDiscountKind.PERCENTAGE -> ItemDiscount.MAX_PERCENT
        ItemDiscountKind.AMOUNT -> lineTotal
    }
    val parsed = parseDecimalInput(input)?.takeIf { it <= upperBound }

    AlertDialog(
        modifier = modifier.testTag(SalesScreenTestTags.ITEM_DISCOUNT_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_item_discount_title)) },
        text = {
            ItemDiscountFields(
                productName = productName,
                lineTotal = lineTotal,
                kind = kind,
                onKindChange = { kind = it },
                input = input,
                onInputChange = { input = it },
                isInvalid = input.isNotBlank() && parsed == null
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = { parsed?.let { onConfirm(it.toDiscount(kind)) } }
            ) {
                Text(stringResource(R.string.sales_discount_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (currentDiscount is ItemDiscount.None) onDismiss() else onConfirm(ItemDiscount.None)
            }) {
                Text(
                    stringResource(
                        if (currentDiscount is ItemDiscount.None) {
                            R.string.sales_discount_cancel
                        } else {
                            R.string.sales_discount_clear
                        }
                    )
                )
            }
        }
    )
}

@Composable
private fun ItemDiscountFields(
    productName: String,
    lineTotal: Double,
    kind: ItemDiscountKind,
    onKindChange: (ItemDiscountKind) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    isInvalid: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(productName, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.sales_item_line_total, lineTotal),
            style = MaterialTheme.typography.bodySmall
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ItemDiscountKind.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = kind == entry,
                    onClick = { onKindChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ItemDiscountKind.entries.size
                    )
                ) {
                    Text(stringResource(entry.labelRes()))
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SalesScreenTestTags.ITEM_DISCOUNT_FIELD),
            value = input,
            onValueChange = onInputChange,
            singleLine = true,
            isError = isInvalid,
            label = { Text(stringResource(kind.hintRes())) },
            supportingText = {
                if (isInvalid) {
                    Text(stringResource(R.string.sales_item_discount_invalid))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

/** Zero is how the operator clears a discount, so it maps back to "none", not to a 0% discount. */
private fun Double.toDiscount(kind: ItemDiscountKind): ItemDiscount = when {
    this == 0.0 -> ItemDiscount.None
    kind == ItemDiscountKind.PERCENTAGE -> ItemDiscount.Percentage(this)
    else -> ItemDiscount.Amount(this)
}

private fun ItemDiscountKind.labelRes(): Int = when (this) {
    ItemDiscountKind.PERCENTAGE -> R.string.sales_item_discount_kind_percentage
    ItemDiscountKind.AMOUNT -> R.string.sales_item_discount_kind_amount
}

private fun ItemDiscountKind.hintRes(): Int = when (this) {
    ItemDiscountKind.PERCENTAGE -> R.string.sales_item_discount_percent_hint
    ItemDiscountKind.AMOUNT -> R.string.sales_item_discount_amount_hint
}

private fun formatValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

// region Previews

private const val PREVIEW_COTURNO_TOTAL = 130.0
private const val PREVIEW_PADARIA_TOTAL = 89.90
private const val PREVIEW_CAFE_TOTAL = 25.0
private val previewPercentageDiscount = ItemDiscount.Percentage(15.0)
private val previewAmountDiscount = ItemDiscount.Amount(5.0)

@Preview(name = "ItemDiscountDialog · Sem desconto · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ItemDiscountDialog · Sem desconto · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ItemDiscountDialogNonePreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ItemDiscountDialog(
                productName = "Coturno cano alto rosa",
                lineTotal = PREVIEW_COTURNO_TOTAL,
                currentDiscount = ItemDiscount.None,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "ItemDiscountDialog · Porcentagem · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ItemDiscountDialog · Porcentagem · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ItemDiscountDialogPercentagePreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ItemDiscountDialog(
                productName = "Padaria e Confeitaria Gourmet do Centro",
                lineTotal = PREVIEW_PADARIA_TOTAL,
                currentDiscount = previewPercentageDiscount,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "ItemDiscountDialog · Valor fixo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ItemDiscountDialog · Valor fixo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ItemDiscountDialogAmountPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ItemDiscountDialog(
                productName = "Café Premium 200ml",
                lineTotal = PREVIEW_CAFE_TOTAL,
                currentDiscount = previewAmountDiscount,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "ItemDiscountDialog · Valor fixo · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ItemDiscountDialogAmountDarkPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ItemDiscountDialog(
                productName = "Café Premium 200ml",
                lineTotal = PREVIEW_CAFE_TOTAL,
                currentDiscount = previewAmountDiscount,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

// endregion
