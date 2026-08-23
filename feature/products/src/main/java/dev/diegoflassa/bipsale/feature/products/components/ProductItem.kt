package dev.diegoflassa.bipsale.feature.products.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.ui.components.ProductThumbnail
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.products.ProductContract
import dev.diegoflassa.bipsale.feature.products.ProductListScreenTestTags
import dev.diegoflassa.bipsale.feature.products.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(
    product: ProductContract.ProductUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ProductListScreenTestTags.productRow(product.code))
            .combinedClickable(
                onClick = onClick,
                onClickLabel = stringResource(R.string.products_edit_product),
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.products_select_product)
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(imagePath = product.imagePath)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.products_code_prefix, product.code),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = product.priceFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.products_stock_format, product.quantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.quantity == 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.testTag(
                        ProductListScreenTestTags.productCheckbox(product.code)
                    )
                )
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(
                        ProductListScreenTestTags.deleteButton(product.code)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.products_delete)
                    )
                }
            }
        }
    }
}

// region Previews

private val previewProductCoffee = ProductContract.ProductUiModel(
    code = "7891000100103",
    name = "Café Premium 200ml",
    priceFormatted = "R$ 12,50",
    imagePath = null,
    label = LabelData("bipsale://product?code=7891000100103", "Café Premium 200ml", "R$ 12,50"),
    quantity = 8
)

private val previewProductLongName = ProductContract.ProductUiModel(
    code = "7891000100202",
    name = "Padaria e Confeitaria Gourmet do Centro - Combo Especial de Fim de Semana",
    priceFormatted = "R$ 45,90",
    imagePath = null,
    label = LabelData("bipsale://product?code=7891000100202", "Combo Especial", "R$ 45,90"),
    quantity = 0
)

@Preview(
    name = "ProductItem · Padrão · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductItem · Padrão · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductItemDefaultPreview() {
    BipSaleTheme {
        ProductItem(
            product = previewProductCoffee,
            isSelected = false,
            onClick = {},
            onLongClick = {},
            onDelete = {}
        )
    }
}

@Preview(
    name = "ProductItem · Padrão · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ProductItemDefaultDarkPreview() {
    BipSaleTheme {
        ProductItem(
            product = previewProductCoffee,
            isSelected = false,
            onClick = {},
            onLongClick = {},
            onDelete = {}
        )
    }
}

@Preview(
    name = "ProductItem · Selecionado · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductItem · Selecionado · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductItemSelectedPreview() {
    BipSaleTheme {
        ProductItem(
            product = previewProductCoffee,
            isSelected = true,
            onClick = {},
            onLongClick = {},
            onDelete = {}
        )
    }
}

@Preview(
    name = "ProductItem · Nome Longo · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductItem · Nome Longo · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductItemLongNamePreview() {
    BipSaleTheme {
        ProductItem(
            product = previewProductLongName,
            isSelected = false,
            onClick = {},
            onLongClick = {},
            onDelete = {}
        )
    }
}

// endregion
