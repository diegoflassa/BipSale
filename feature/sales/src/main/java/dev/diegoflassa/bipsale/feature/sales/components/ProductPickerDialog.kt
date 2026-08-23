package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.components.ProductThumbnail
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.feature.sales.SalesContract
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

/** Adds a registered product to the cart without a scan — for a label that will not read. */
@Composable
fun ProductPickerDialog(
    catalog: List<SalesContract.CatalogProduct>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.testTag(SalesScreenTestTags.CATALOG_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_catalog_title)) },
        text = {
            if (catalog.isEmpty()) {
                Text(
                    stringResource(R.string.sales_catalog_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(catalog, key = { it.code }) { product ->
                        CatalogProductCard(product = product, onPick = { onPick(product.code) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sales_discount_cancel))
            }
        }
    )
}

/**
 * The same card shape the product list uses, photo included. Picking by name alone means squinting
 * at two similar names on a busy counter; the photo is what the operator actually recognises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogProductCard(
    product: SalesContract.CatalogProduct,
    onPick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SalesScreenTestTags.catalogRow(product.code)),
        onClick = onPick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Not expandable here: the tap belongs to picking the product, and a dialog opening
            // over a dialog is not something an operator asked for mid-sale.
            ProductThumbnail(imagePath = product.imagePath, size = 48.dp, expandable = false)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sales_product_stock, product.quantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.quantity == 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = stringResource(R.string.currency_format, product.price),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// region Previews

private val previewCatalog = listOf(
    SalesContract.CatalogProduct("CT-A-RoS", "Coturno cano alto rosa", 130.0, quantity = 4),
    SalesContract.CatalogProduct("CF-200", "Café Premium 200ml", 12.50, quantity = 12),
    SalesContract.CatalogProduct("PD-GC", "Padaria e Confeitaria Gourmet do Centro", 45.90)
)

@Preview(name = "ProductPickerDialog · Com produtos · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ProductPickerDialog · Com produtos · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ProductPickerDialogWithProductsPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ProductPickerDialog(catalog = previewCatalog, onPick = {}, onDismiss = {})
        }
    }
}

@Preview(name = "ProductPickerDialog · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ProductPickerDialog · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ProductPickerDialogEmptyPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ProductPickerDialog(catalog = emptyList(), onPick = {}, onDismiss = {})
        }
    }
}

@Preview(name = "ProductPickerDialog · Com produtos · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProductPickerDialogWithProductsDarkPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ProductPickerDialog(catalog = previewCatalog, onPick = {}, onDismiss = {})
        }
    }
}

// endregion
