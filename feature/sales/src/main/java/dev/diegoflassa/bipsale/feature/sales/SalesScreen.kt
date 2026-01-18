package dev.diegoflassa.bipsale.feature.sales

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.qrcode.QrScannerView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
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

    LaunchedEffect(Unit) {
        viewModel.onIntent(SalesContract.Intent.UpdateCustomerInfo(customerName, customerCpf, isAnonymous))
        viewModel.effect.collect { effect ->
            when (effect) {
                is SalesContract.Effect.NavigateBack -> onFinish()
                is SalesContract.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
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
                    onFinalize = { viewModel.onIntent(SalesContract.Intent.FinalizeSale) }
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
    onFinalize: () -> Unit
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
