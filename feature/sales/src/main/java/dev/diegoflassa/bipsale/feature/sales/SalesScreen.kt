package dev.diegoflassa.bipsale.feature.sales

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.qrcode.QrScannerScreen
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.components.DiscountDialog
import dev.diegoflassa.bipsale.feature.sales.components.ItemDiscountDialog
import dev.diegoflassa.bipsale.feature.sales.components.ManualCodeDialog
import dev.diegoflassa.bipsale.feature.sales.components.ProductPickerDialog
import dev.diegoflassa.bipsale.feature.sales.components.SaleBottomBar
import dev.diegoflassa.bipsale.core.qrcode.components.PixAmountToggle
import dev.diegoflassa.bipsale.core.qrcode.components.PixQrCard
import dev.diegoflassa.bipsale.feature.sales.components.ProductDetailSheet
import dev.diegoflassa.bipsale.feature.sales.components.SaleItemRow

/** Which overlay the screen is currently showing. Stateless cases, so an enum. */
private enum class SalesOverlay { NONE, SCANNER, CATALOG, MANUAL_CODE, SALE_DISCOUNT }

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun SalesScreen(
    customerName: String,
    customerCpf: String,
    isAnonymous: Boolean,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var overlay by remember { mutableStateOf(SalesOverlay.NONE) }
    var discountingItemId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) overlay = SalesOverlay.SCANNER
    }

    LaunchedEffect(customerName, customerCpf, isAnonymous) {
        viewModel.onIntent(
            SalesContract.Intent.UpdateCustomerInfo(customerName, customerCpf, isAnonymous)
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SalesContract.Effect.NavigateBack -> onFinish()
                is SalesContract.Effect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))
            }
        }
    }

    BipSaleTheme {
        SalesScreenContent(
            state = uiState,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onScanClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    overlay = SalesOverlay.SCANNER
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPickFromCatalog = { overlay = SalesOverlay.CATALOG },
            onTypeCode = { overlay = SalesOverlay.MANUAL_CODE },
            onRemoveItem = { viewModel.onIntent(SalesContract.Intent.RemoveItem(it)) },
            onDiscountItem = { discountingItemId = it },
            onSaleDiscountClick = { overlay = SalesOverlay.SALE_DISCOUNT },
            onPaymentMethodChange = {
                viewModel.onIntent(SalesContract.Intent.SelectPaymentMethod(it))
            },
            onShowDetail = { viewModel.onIntent(SalesContract.Intent.ShowProductDetail(it)) },
            onTogglePixAmount = { viewModel.onIntent(SalesContract.Intent.TogglePixAmount(it)) },
            onFinalize = { viewModel.onIntent(SalesContract.Intent.FinalizeSale) }
        )

        SalesOverlays(
            overlay = overlay,
            state = uiState,
            onDismiss = { overlay = SalesOverlay.NONE },
            onIntent = viewModel::onIntent
        )

        SaleDialogs(
            state = uiState,
            onIntent = viewModel::onIntent,
            onDiscountItem = { discountingItemId = it }
        )

        uiState.items.firstOrNull { it.id == discountingItemId }?.let { item ->
            ItemDiscountDialog(
                productName = item.productName,
                lineTotal = item.grossAmount,
                currentDiscount = item.discount,
                onConfirm = { discount ->
                    viewModel.onIntent(SalesContract.Intent.UpdateItemDiscount(item.id, discount))
                    discountingItemId = null
                },
                onDismiss = { discountingItemId = null }
            )
        }
    }
}

/** The dialogs the sale screen raises from its own state, rather than from an overlay choice. */
@Composable
private fun SaleDialogs(
    state: SalesContract.State,
    onIntent: (SalesContract.Intent) -> Unit,
    onDiscountItem: (String) -> Unit
) {
    if (state.isAwaitingPixPayment) {
        CompletedPixDialog(
            payload = state.pixPayload,
            onDone = { onIntent(SalesContract.Intent.PixPaymentAcknowledged) }
        )
    }

    state.detailItem?.let { item ->
        ProductDetailSheet(
            item = item,
            catalogEntry = state.catalogEntry(item.productCode),
            onApplyDiscount = {
                onIntent(SalesContract.Intent.HideProductDetail)
                onDiscountItem(item.id)
            },
            onDismiss = { onIntent(SalesContract.Intent.HideProductDetail) }
        )
    }
}

/**
 * Shown once a PIX sale is written. The screen used to navigate away the instant the sale landed,
 * which took the QR with it before the customer had scanned anything.
 */
@Composable
private fun CompletedPixDialog(
    payload: String?,
    onDone: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag(SalesScreenTestTags.COMPLETED_PIX_DIALOG),
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.sales_completed_pix_title)) },
        text = {
            if (payload != null) {
                PixQrCard(payload = payload)
            } else {
                Text(stringResource(R.string.sales_completed_pix_title))
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.sales_completed_pix_done))
            }
        }
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun SalesOverlays(
    overlay: SalesOverlay,
    state: SalesContract.State,
    onDismiss: () -> Unit,
    onIntent: (SalesContract.Intent) -> Unit
) {
    when (overlay) {
        SalesOverlay.NONE -> Unit

        SalesOverlay.SCANNER -> Dialog(
            onDismissRequest = onDismiss,
            // Without this the dialog keeps the platform's inset width and the camera preview
            // renders as a letterboxed rectangle floating over the sale.
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QrScannerScreen(
                onQrCodeScanned = { qr ->
                    onIntent(SalesContract.Intent.AddProductByQr(qr))
                    onDismiss()
                },
                onClose = onDismiss
            )
        }

        SalesOverlay.CATALOG -> ProductPickerDialog(
            catalog = state.catalog,
            onPick = { code ->
                onIntent(SalesContract.Intent.AddProductByCode(code))
                onDismiss()
            },
            onDismiss = onDismiss
        )

        SalesOverlay.MANUAL_CODE -> ManualCodeDialog(
            onConfirm = { code ->
                onIntent(SalesContract.Intent.AddProductByCode(code))
                onDismiss()
            },
            onDismiss = onDismiss
        )

        SalesOverlay.SALE_DISCOUNT -> DiscountDialog(
            currentPercentage = state.discountPercentage,
            onConfirm = { percentage ->
                onIntent(SalesContract.Intent.UpdateDiscount(percentage))
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesScreenContent(
    state: SalesContract.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScanClick: () -> Unit,
    onPickFromCatalog: () -> Unit,
    onTypeCode: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onDiscountItem: (String) -> Unit,
    onSaleDiscountClick: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onShowDetail: (String) -> Unit,
    onTogglePixAmount: (Boolean) -> Unit,
    onFinalize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(SalesScreenTestTags.ROOT),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = if (state.isAnonymous) {
                    stringResource(R.string.sale_title_anonymous)
                } else {
                    stringResource(R.string.sale_title_format, state.customerName)
                },
                onBack = onBack,
                actions = {
                    IconButton(
                        modifier = Modifier.testTag(SalesScreenTestTags.SCAN_BUTTON),
                        onClick = onScanClick
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_qr_description)
                        )
                    }
                    AddProductMenu(
                        onPickFromCatalog = onPickFromCatalog,
                        onTypeCode = onTypeCode
                    )
                }
            )
        },
        bottomBar = {
            SaleBottomBar(
                subtotal = state.totalAmount,
                itemDiscountAmount = state.itemDiscountAmount,
                discountPercentage = state.discountPercentage,
                total = state.finalAmount,
                paymentMethod = state.paymentMethod,
                canFinalize = state.canFinalize,
                onDiscountClick = onSaleDiscountClick,
                onPaymentMethodChange = onPaymentMethodChange,
                onFinalize = onFinalize
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val pixCardModifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            if (state.pixPayload != null) {
                PixQrCard(payload = state.pixPayload, modifier = pixCardModifier)
                PixAmountToggle(
                    carriesAmount = state.pixCarriesAmount,
                    onToggle = onTogglePixAmount,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        modifier = Modifier.testTag(SalesScreenTestTags.EMPTY_MESSAGE),
                        text = stringResource(R.string.empty_items_message),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SalesScreenTestTags.ITEM_LIST),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SaleItemRow(
                            item = item,
                            onDelete = { onRemoveItem(item.id) },
                            onDiscount = { onDiscountItem(item.id) },
                            imagePath = state.imagePathFor(item.productCode),
                            onClick = { onShowDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductMenu(
    onPickFromCatalog: () -> Unit,
    onTypeCode: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            modifier = Modifier.testTag(SalesScreenTestTags.ADD_PRODUCT_BUTTON),
            onClick = { expanded = true }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.sales_add_product)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sales_pick_from_catalog)) },
                onClick = {
                    expanded = false
                    onPickFromCatalog()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sales_type_code)) },
                onClick = {
                    expanded = false
                    onTypeCode()
                }
            )
        }
    }
}

// region Previews

private val previewItems = listOf(
    SaleItem(
        id = "1",
        saleId = "1",
        productCode = "CF-200",
        productName = "Café Premium 200ml",
        unitPrice = 12.50,
        quantity = 2
    ),
    SaleItem(
        id = "2",
        saleId = "1",
        productCode = "CT-A-RoS",
        productName = "Coturno cano alto rosa",
        unitPrice = 130.0,
        quantity = 1,
        discount = ItemDiscount.Percentage(10.0)
    )
)

private val previewEmptyState = SalesContract.State(
    customerName = "Maria Aparecida Souza",
    customerCpf = "123.456.789-00"
)

private val previewFilledState = SalesContract.State(
    customerName = "Maria Aparecida Souza",
    customerCpf = "123.456.789-00",
    items = previewItems,
    discountPercentage = 10.0,
    totalAmount = 155.0,
    itemDiscountAmount = 13.0,
    finalAmount = 127.80
)

@Composable
private fun PreviewContent(state: SalesContract.State) {
    BipSaleTheme {
        SalesScreenContent(
            state = state,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onScanClick = {},
            onPickFromCatalog = {},
            onTypeCode = {},
            onRemoveItem = {},
            onDiscountItem = {},
            onSaleDiscountClick = {},
            onPaymentMethodChange = {},
            onShowDetail = {},
            onTogglePixAmount = {},
            onFinalize = {}
        )
    }
}

@Preview(name = "SalesScreenContent · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SalesScreenContent · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SalesScreenContentEmptyPreview() = PreviewContent(previewEmptyState)

@Preview(name = "SalesScreenContent · Com itens · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SalesScreenContent · Com itens · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SalesScreenContentFilledPreview() = PreviewContent(previewFilledState)

@Preview(name = "SalesScreenContent · Anônimo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SalesScreenContent · Anônimo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SalesScreenContentAnonymousPreview() =
    PreviewContent(previewFilledState.copy(isAnonymous = true))

@Preview(name = "SalesScreenContent · Com itens · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SalesScreenContentFilledDarkPreview() = PreviewContent(previewFilledState)

// endregion
