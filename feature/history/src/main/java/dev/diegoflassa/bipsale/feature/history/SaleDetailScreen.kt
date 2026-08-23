package dev.diegoflassa.bipsale.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: String,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val items by viewModel.getSaleItems(saleId).collectAsState(initial = emptyList())
    // For simplicity, we assume we fetch the sale header from a list or repo
    // In a real app, we'd have a state for the specific sale

    SaleDetailContent(items = items, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleDetailContent(
    items: List<SaleItem>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            BipSaleTopAppBar(
                title = stringResource(R.string.history_detail_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.history_items_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    SaleDetailItemRow(item = item)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SaleDetailItemRow(item: SaleItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(item.productName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.history_item_quantity,
                    item.quantity,
                    item.unitPrice
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = stringResource(R.string.history_currency, item.unitPrice * item.quantity),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// region Previews

private val previewSaleItems = listOf(
    SaleItem(saleId = "1", productCode = "7891000100103", productName = "Café Premium 200ml", unitPrice = 12.50, quantity = 2),
    SaleItem(saleId = "1", productCode = "7891000100202", productName = "Padaria e Confeitaria Gourmet do Centro", unitPrice = 45.90, quantity = 1),
    SaleItem(saleId = "1", productCode = "7891000100301", productName = "Água Mineral 500ml", unitPrice = 3.00, quantity = 4),
)

@Preview(name = "SaleDetailContent · Com Itens · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleDetailContent · Com Itens · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleDetailContentWithItemsPreview() {
    BipSaleTheme {
        SaleDetailContent(items = previewSaleItems, onBack = {})
    }
}

@Preview(name = "SaleDetailContent · Com Itens · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleDetailContentWithItemsDarkPreview() {
    BipSaleTheme {
        SaleDetailContent(items = previewSaleItems, onBack = {})
    }
}

@Preview(name = "SaleDetailContent · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleDetailContent · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleDetailContentEmptyPreview() {
    BipSaleTheme {
        SaleDetailContent(items = emptyList(), onBack = {})
    }
}

// endregion
