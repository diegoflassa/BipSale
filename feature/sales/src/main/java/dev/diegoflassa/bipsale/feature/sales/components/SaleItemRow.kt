package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R

/**
 * One cart line. When the line carries a discount, the original price is struck through next to
 * what is actually being charged — an operator reading the total back to a customer needs both.
 */
@Composable
fun SaleItemRow(
    item: SaleItem,
    onDelete: () -> Unit,
    onDiscount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.sales_quantity_format, item.quantity, item.unitPrice),
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.discount != ItemDiscount.None) {
                        Text(
                            text = stringResource(R.string.currency_format, item.grossAmount),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.currency_format, item.netAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item.discount.badge()?.let { badge ->
                    AssistChip(onClick = onDiscount, label = { Text(badge) })
                }
            }

            IconButton(onClick = onDiscount) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = stringResource(R.string.sales_item_discount_action)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_item_description)
                )
            }
        }
    }
}

@Composable
private fun ItemDiscount.badge(): String? = when (this) {
    is ItemDiscount.None -> null
    is ItemDiscount.Percentage -> stringResource(
        R.string.sales_item_discount_badge_percent,
        if (percent % 1.0 == 0.0) percent.toInt().toString() else percent.toString()
    )
    is ItemDiscount.Amount -> stringResource(R.string.sales_item_discount_badge_amount, amount)
}

// region Previews

private val previewItem = SaleItem(
    saleId = "1",
    productCode = "CF-200",
    productName = "Café Premium 200ml",
    unitPrice = 12.50,
    quantity = 2
)

private val previewPercentageItem = previewItem.copy(discount = ItemDiscount.Percentage(20.0))
private val previewAmountItem = previewItem.copy(discount = ItemDiscount.Amount(5.0))

@Preview(name = "SaleItemRow · Sem desconto · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleItemRow · Sem desconto · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleItemRowPlainPreview() {
    BipSaleTheme {
        SaleItemRow(item = previewItem, onDelete = {}, onDiscount = {})
    }
}

@Preview(name = "SaleItemRow · Desconto percentual · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleItemRow · Desconto percentual · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleItemRowPercentageDiscountPreview() {
    BipSaleTheme {
        SaleItemRow(
            item = previewPercentageItem,
            onDelete = {},
            onDiscount = {}
        )
    }
}

@Preview(name = "SaleItemRow · Desconto fixo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleItemRow · Desconto fixo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleItemRowAmountDiscountPreview() {
    BipSaleTheme {
        SaleItemRow(
            item = previewAmountItem,
            onDelete = {},
            onDiscount = {}
        )
    }
}

@Preview(name = "SaleItemRow · Nome longo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleItemRow · Nome longo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleItemRowLongNamePreview() {
    BipSaleTheme {
        SaleItemRow(
            item = previewItem.copy(
                productName = "Padaria e Confeitaria Gourmet do Centro — Pão de Queijo Congelado 1kg"
            ),
            onDelete = {},
            onDiscount = {}
        )
    }
}

@Preview(name = "SaleItemRow · Desconto percentual · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleItemRowPercentageDiscountDarkPreview() {
    BipSaleTheme {
        SaleItemRow(
            item = previewPercentageItem,
            onDelete = {},
            onDiscount = {}
        )
    }
}

// endregion
