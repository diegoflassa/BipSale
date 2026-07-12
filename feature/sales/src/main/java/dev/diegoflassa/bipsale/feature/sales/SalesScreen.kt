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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.qrcode.QrScannerView
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    customerName: String,
    customerCpf: String,
    isAnonymous: Boolean,
    onFinish: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScanner by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    LaunchedEffect(customerName, customerCpf, isAnonymous) {
        viewModel.onIntent(SalesContract.Intent.UpdateCustomerInfo(customerName, customerCpf, isAnonymous))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SalesContract.Effect.NavigateBack -> onFinish()
                is SalesContract.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message.asString(context))
            }
        }
    }

    BipSaleTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing, // Use safeDrawing for proper edge-to-edge
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        val title = if (uiState.isAnonymous) {
                            stringResource(R.string.sale_title_anonymous)
                        } else {
                            stringResource(R.string.sale_title_format, uiState.customerName)
                        }
                        Text(title)
                    },
                    actions = {
                        IconButton(onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                showScanner = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_qr_description))
                        }
                    }
                )
            },
            bottomBar = {
                SaleBottomBar(
                    total = uiState.totalAmount,
                    final = uiState.finalAmount,
                    discount = uiState.discountPercentage,
                    onDiscountChange = { viewModel.onIntent(SalesContract.Intent.UpdateDiscount(it)) },
                    onFinalize = { viewModel.onIntent(SalesContract.Intent.FinalizeSale) },
                    paymentMethod = uiState.paymentMethod,
                    onPaymentMethodChange = { viewModel.onIntent(SalesContract.Intent.SelectPaymentMethod(it)) }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_items_message), style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.items) { item ->
                            SaleItemRow(item = item, onDelete = { viewModel.onIntent(SalesContract.Intent.RemoveItem(item)) })
                        }
                    }
                }
            }

            if (showScanner) {
                Dialog(onDismissRequest = { showScanner = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(), // Avoid system bars in full screen dialog
                        color = MaterialTheme.colorScheme.background
                    ) {
                        QrScannerView(onQrCodeScanned = { qr ->
                            viewModel.onIntent(SalesContract.Intent.AddProductByQr(qr))
                            showScanner = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItemRow(item: SaleItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.currency_format, item.unitPrice), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_item_description))
            }
        }
    }
}

@Composable
fun SaleBottomBar(
    total: Double,
    final: Double,
    discount: Double,
    onDiscountChange: (Double) -> Unit,
    onFinalize: () -> Unit,
    paymentMethod: PaymentMethod = PaymentMethod.PIX,
    onPaymentMethodChange: (PaymentMethod) -> Unit = {}
) {
    Surface(tonalElevation = 8.dp) {
        Column(modifier = Modifier
            .navigationBarsPadding() // Handled by Edge-to-Edge
            .padding(16.dp)
            .fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.subtotal_label), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.currency_format, total), style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.discount_label), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { /* Could show a dialog to change discount */ }) {
                    Text("${discount.toInt()}%")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.payment_method_label), style = MaterialTheme.typography.bodyMedium)
                PaymentMethodDropdown(
                    selectedMethod = paymentMethod,
                    onMethodSelected = onPaymentMethodChange
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total_label), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.currency_format, final), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onFinalize, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.finalize_sale_button))
            }
        }
    }
}

@Composable
fun PaymentMethodDropdown(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedMethod.serializedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.serializedName) },
                    onClick = {
                        onMethodSelected(method)
                        expanded = false
                    }
                )
            }
        }
    }
}

// region Previews

private val previewSaleItem = SaleItem(
    saleId = "1",
    productCode = "7891000100103",
    productName = "Café Premium 200ml",
    unitPrice = 12.50,
    quantity = 2,
)

@Preview(name = "SaleItemRow · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleItemRow · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleItemRowPreview() {
    BipSaleTheme {
        SaleItemRow(item = previewSaleItem, onDelete = {})
    }
}

@Preview(name = "SaleItemRow · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleItemRowDarkPreview() {
    BipSaleTheme {
        SaleItemRow(item = previewSaleItem, onDelete = {})
    }
}

@Preview(name = "SaleBottomBar · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "SaleBottomBar · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun SaleBottomBarPreview() {
    BipSaleTheme {
        SaleBottomBar(
            total = 89.90,
            final = 80.91,
            discount = 10.0,
            onDiscountChange = {},
            onFinalize = {},
            paymentMethod = PaymentMethod.PIX,
            onPaymentMethodChange = {},
        )
    }
}

@Preview(name = "SaleBottomBar · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SaleBottomBarDarkPreview() {
    BipSaleTheme {
        SaleBottomBar(
            total = 89.90,
            final = 80.91,
            discount = 10.0,
            onDiscountChange = {},
            onFinalize = {},
            paymentMethod = PaymentMethod.PIX,
            onPaymentMethodChange = {},
        )
    }
}

@Preview(name = "PaymentMethodDropdown · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "PaymentMethodDropdown · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun PaymentMethodDropdownPreview() {
    BipSaleTheme {
        PaymentMethodDropdown(selectedMethod = PaymentMethod.CREDIT_CARD, onMethodSelected = {})
    }
}

// endregion
