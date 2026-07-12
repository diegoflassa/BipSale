package dev.diegoflassa.bipsale.feature.history

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.utils.ExcelExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSaleClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.selectedSaleIds.isNotEmpty()) {
                        Text("${uiState.selectedSaleIds.size} selecionados")
                    } else {
                        Text("Histórico de Vendas")
                    }
                },
                navigationIcon = {
                    if (uiState.selectedSaleIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onIntent(HistoryContract.Intent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar Seleção")
                        }
                    }
                },
                actions = {
                    if (uiState.selectedSaleIds.isNotEmpty()) {
                        val exporter = remember { ExcelExporter() }
                        IconButton(onClick = {
                            val selectedSales = uiState.sales.filter { it.id in uiState.selectedSaleIds }
                            if (selectedSales.isNotEmpty()) {
                                val file = java.io.File(context.getExternalFilesDir(null), "vendas_selecionadas_${System.currentTimeMillis()}.xlsx")
                                java.io.FileOutputStream(file).use { outputStream ->
                                    exporter.exportSalesToExcel(outputStream, selectedSales)
                                }
                                viewModel.onIntent(HistoryContract.Intent.ClearSelection)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Exportar Selecionados")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onIntent(HistoryContract.Intent.SearchSales(it)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Pesquisar por nome ou CPF") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sales) { sale ->
                    SaleHistoryItem(
                        sale = sale,
                        isSelected = uiState.selectedSaleIds.contains(sale.id),
                        onClick = {
                            if (uiState.selectedSaleIds.isNotEmpty()) {
                                viewModel.onIntent(HistoryContract.Intent.ToggleSaleSelection(sale.id))
                            } else {
                                onSaleClick(sale.id)
                            }
                        },
                        onLongClick = {
                            viewModel.onIntent(HistoryContract.Intent.ToggleSaleSelection(sale.id))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SaleHistoryItem(
    sale: Sale,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sale.customerName.ifEmpty { "Anônimo" }, style = MaterialTheme.typography.titleMedium)
                if (isSelected) {
                    Checkbox(checked = true, onCheckedChange = { onClick() })
                } else {
                    Text(dateFormat.format(Date(sale.date)), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("CPF: ${sale.customerCpf.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total:", style = MaterialTheme.typography.bodyMedium)
                Text("R$ ${String.format("%.2f", sale.finalAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// region Previews

private val previewSaleNamed = Sale(
    id = "1",
    customerName = "Maria Aparecida Souza",
    customerCpf = "123.456.789-00",
    totalAmount = 89.90,
    discountPercentage = 0.0,
    finalAmount = 89.90,
    paymentMethod = PaymentMethod.PIX,
    date = 1752000000000L,
)

private val previewSaleAnonymous = Sale(
    id = "2",
    customerName = "",
    customerCpf = "",
    totalAmount = 45.00,
    discountPercentage = 10.0,
    finalAmount = 40.50,
    paymentMethod = PaymentMethod.CASH,
    date = 1752003600000L,
)

@Preview(name = "SaleHistoryItem · Nomeado · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Nomeado · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemNamedPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Nomeado · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleHistoryItemNamedDarkPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Anônimo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Anônimo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemAnonymousPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleAnonymous, isSelected = false, onClick = {}, onLongClick = {})
    }
}

@Preview(name = "SaleHistoryItem · Selecionado · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleHistoryItem · Selecionado · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleHistoryItemSelectedPreview() {
    BipSaleTheme {
        SaleHistoryItem(sale = previewSaleNamed, isSelected = true, onClick = {}, onLongClick = {})
    }
}

// endregion
