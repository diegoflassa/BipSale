package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(catalog, key = { it.code }) { product ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(product.code) },
                            headlineContent = { Text(product.name) },
                            supportingContent = { Text(product.code) },
                            trailingContent = {
                                Text(stringResource(R.string.currency_format, product.price))
                            }
                        )
                        HorizontalDivider()
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

// region Previews

private val previewCatalog = listOf(
    SalesContract.CatalogProduct("CT-A-RoS", "Coturno cano alto rosa", 130.0),
    SalesContract.CatalogProduct("CF-200", "Café Premium 200ml", 12.50),
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
