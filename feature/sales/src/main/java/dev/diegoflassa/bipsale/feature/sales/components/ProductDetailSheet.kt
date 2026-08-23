package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.components.ProductThumbnail
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.feature.sales.SalesContract
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

/**
 * What the operator needs to answer "is this the right product?" at the counter, read-only.
 *
 * The one action it offers is the line discount, because that is the question that follows once
 * the product is confirmed — and reaching it from here saves hunting for the row's chip.
 */
@Composable
fun ProductDetailSheet(
    item: SaleItem,
    catalogEntry: SalesContract.CatalogProduct?,
    onApplyDiscount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.testTag(SalesScreenTestTags.PRODUCT_DETAIL_SHEET),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_product_detail_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductThumbnail(imagePath = catalogEntry?.imagePath, size = 72.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.productName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.sales_code_prefix, item.productCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DetailRow(
                    label = stringResource(R.string.sales_unit_price_label),
                    value = stringResource(R.string.sales_currency, item.unitPrice)
                )
                DetailRow(
                    label = stringResource(R.string.sales_quantity_label),
                    value = item.quantity.toString()
                )
                DetailRow(
                    label = stringResource(R.string.sales_line_total_label),
                    value = stringResource(R.string.sales_currency, item.netAmount)
                )
                catalogEntry?.let {
                    DetailRow(
                        label = stringResource(R.string.sales_product_stock, it.quantity),
                        value = ""
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApplyDiscount,
                modifier = Modifier.testTag(SalesScreenTestTags.PRODUCT_DETAIL_DISCOUNT)
            ) {
                Text(stringResource(R.string.sales_product_detail_discount))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sales_product_detail_close))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Previews

private val previewItem = SaleItem(
    id = "line-1",
    saleId = "sale-1",
    productCode = "CF-200",
    productName = "Café Premium 200ml",
    unitPrice = 12.50,
    quantity = 2
)

@Preview(name = "ProductDetailSheet · Padrão · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ProductDetailSheet · Padrão · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ProductDetailSheetPreview() {
    BipSaleTheme {
        ProductDetailSheet(
            item = previewItem,
            catalogEntry = SalesContract.CatalogProduct(
                code = "CF-200",
                name = "Café Premium 200ml",
                price = 12.50,
                quantity = 8
            ),
            onApplyDiscount = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "ProductDetailSheet · Padrão · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProductDetailSheetDarkPreview() {
    BipSaleTheme {
        ProductDetailSheet(
            item = previewItem,
            catalogEntry = null,
            onApplyDiscount = {},
            onDismiss = {}
        )
    }
}

// endregion
