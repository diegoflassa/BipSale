package dev.diegoflassa.bipsale.feature.history

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale

import dev.diegoflassa.bipsale.core.qrcode.components.PixQrCard
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.history.components.SaleHistoryItem

@Composable
fun HistoryScreen(
    onSaleClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // The system document picker is what lets the operator drop the sheet into Drive, Downloads or
    // anywhere else they can find it again — app-private storage they cannot browse to.
    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SPREADSHEET_MIME_TYPE)
    ) { uri: Uri? ->
        val intent = if (uri == null) {
            HistoryContract.Intent.ExportCancelled
        } else {
            HistoryContract.Intent.ExportDestinationChosen(uri.toString())
        }
        viewModel.onIntent(intent)
    }

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is HistoryContract.Effect.PickExportDestination ->
                    createLauncher.launch(effect.suggestedFileName)
            }
        }
    }

    if (uiState.isShowingPix) {
        SalePixDialog(
            payload = uiState.pixPayload,
            onDismiss = { viewModel.onIntent(HistoryContract.Intent.HideSalePix) }
        )
    }

    HistoryScreenContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSaleClick = onSaleClick,
        onExportSelected = {
            viewModel.onIntent(
                HistoryContract.Intent.ExportRequested(HistoryContract.ExportScope.SELECTED)
            )
        },
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreenContent(
    state: HistoryContract.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSaleClick: (String) -> Unit,
    onExportSelected: () -> Unit,
    onIntent: (HistoryContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = state.selectedSaleIds.isNotEmpty()

    Scaffold(
        modifier = modifier.testTag(HistoryScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = if (hasSelection) {
                    stringResource(R.string.history_selected_count, state.selectedSaleIds.size)
                } else {
                    stringResource(R.string.history_title)
                },
                onBack = onBack,
                navigationIcon = if (!hasSelection) {
                    null
                } else {
                    {
                        IconButton(
                            onClick = { onIntent(HistoryContract.Intent.ClearSelection) },
                            modifier = Modifier.testTag(
                                HistoryScreenTestTags.CLEAR_SELECTION_BUTTON
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.history_clear_selection
                                )
                            )
                        }
                    }
                },
                actions = {
                    if (hasSelection) {
                        IconButton(
                            onClick = onExportSelected,
                            enabled = !state.isExporting,
                            modifier = Modifier.testTag(
                                HistoryScreenTestTags.EXPORT_SELECTED_BUTTON
                            )
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = stringResource(
                                    R.string.history_export_selected
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onIntent(HistoryContract.Intent.SearchSales(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(HistoryScreenTestTags.SEARCH_FIELD),
                placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (state.isLoading || state.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HistoryScreenTestTags.LOADING)
                )
            }

            if (state.sales.isEmpty() && !state.isLoading) {
                EmptyHistory()
            } else {
                SalesList(
                    state = state,
                    onSaleClick = onSaleClick,
                    onIntent = onIntent
                )
            }
        }
    }
}

/**
 * The PIX code for a sale that is already recorded — a customer who walked off without paying can
 * still be handed the same amount to scan.
 */
@Composable
private fun SalePixDialog(
    payload: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag(HistoryScreenTestTags.PIX_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_pix_title)) },
        text = {
            if (payload != null) {
                PixQrCard(payload = payload)
            } else {
                Text(stringResource(R.string.history_pix_title))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.history_pix_close))
            }
        }
    )
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(HistoryScreenTestTags.EMPTY)
        )
    }
}

@Composable
private fun SalesList(
    state: HistoryContract.State,
    onSaleClick: (String) -> Unit,
    onIntent: (HistoryContract.Intent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(HistoryScreenTestTags.LIST),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.sales, key = { it.id }) { sale ->
            SaleHistoryItem(
                sale = sale,
                isSelected = state.selectedSaleIds.contains(sale.id),
                onClick = {
                    if (state.selectedSaleIds.isEmpty()) {
                        onSaleClick(sale.id)
                    } else {
                        onIntent(HistoryContract.Intent.ToggleSaleSelection(sale.id))
                    }
                },
                onLongClick = {
                    onIntent(HistoryContract.Intent.ToggleSaleSelection(sale.id))
                },
                onShowPix = if (sale.paymentMethod == PaymentMethod.PIX) {
                    { onIntent(HistoryContract.Intent.ShowSalePix(sale.id)) }
                } else {
                    null
                },
                modifier = Modifier.testTag(HistoryScreenTestTags.saleRow(sale.id))
            )
        }
    }
}

private const val SPREADSHEET_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

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

@Preview(name = "HistoryScreenContent · Lista · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HistoryScreenContent · Lista · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun HistoryScreenContentListPreview() {
    BipSaleTheme {
        HistoryScreenContent(
            state = HistoryContract.State(sales = listOf(previewSaleNamed, previewSaleAnonymous)),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onSaleClick = {},
            onExportSelected = {},
            onIntent = {}
        )
    }
}

@Preview(name = "HistoryScreenContent · Lista · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryScreenContentListDarkPreview() {
    BipSaleTheme {
        HistoryScreenContent(
            state = HistoryContract.State(sales = listOf(previewSaleNamed, previewSaleAnonymous)),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onSaleClick = {},
            onExportSelected = {},
            onIntent = {}
        )
    }
}

@Preview(name = "HistoryScreenContent · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HistoryScreenContent · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun HistoryScreenContentEmptyPreview() {
    BipSaleTheme {
        HistoryScreenContent(
            state = HistoryContract.State(),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onSaleClick = {},
            onExportSelected = {},
            onIntent = {}
        )
    }
}

@Preview(name = "HistoryScreenContent · Selecao · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "HistoryScreenContent · Selecao · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun HistoryScreenContentSelectionPreview() {
    BipSaleTheme {
        HistoryScreenContent(
            state = HistoryContract.State(
                sales = listOf(previewSaleNamed, previewSaleAnonymous),
                selectedSaleIds = setOf("1")
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onSaleClick = {},
            onExportSelected = {},
            onIntent = {}
        )
    }
}

// endregion
