package dev.diegoflassa.bipsale.feature.products

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.print.PrintHelper
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductContract.Effect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message.asString(context)
                    )
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState.selectedProductCodes.isNotEmpty()) {
                        Text("${uiState.selectedProductCodes.size} selecionados")
                    } else {
                        Text("Gerenciar Produtos")
                    }
                },
                navigationIcon = {
                    if (uiState.selectedProductCodes.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onIntent(ProductContract.Intent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar Seleção")
                        }
                    }
                },
                actions = {
                    if (uiState.selectedProductCodes.isNotEmpty()) {
                        IconButton(onClick = {
                            val selectedProducts = uiState.products.filter { it.code in uiState.selectedProductCodes }
                            if (selectedProducts.isNotEmpty()) {
                                printBatchQrCodes(context, selectedProducts)
                            }
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "Imprimir Selecionados")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.products) { product ->
                ProductItem(
                    product = product,
                    isSelected = uiState.selectedProductCodes.contains(product.code),
                    onClick = { 
                        if (uiState.selectedProductCodes.isNotEmpty()) {
                            viewModel.onIntent(ProductContract.Intent.ToggleProductSelection(product.code))
                        } else {
                            onEditProduct(product.code)
                        }
                    },
                    onLongClick = {
                        viewModel.onIntent(ProductContract.Intent.ToggleProductSelection(product.code))
                    },
                    onDelete = { viewModel.onIntent(ProductContract.Intent.DeleteProduct(product)) }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProductItem(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Código: ${product.code}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "R$ ${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!isSelected) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir")
                }
            } else {
                Checkbox(checked = true, onCheckedChange = { onClick() })
            }
        }
    }
}

private fun printBatchQrCodes(context: android.content.Context, products: List<Product>) {
    val qrGenerator = QrGenerator()
    val bitmaps = products.mapNotNull { product ->
        qrGenerator.generateQrCode(product.qrCode ?: product.code, 300, 300)?.let { qr ->
            // Create a bitmap with text and QR
            val combined = Bitmap.createBitmap(400, 450, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combined)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawBitmap(qr, 50f, 20f, null)
            canvas.drawText(product.name, 200f, 350f, paint)
            canvas.drawText("R$ ${String.format("%.2f", product.price)}", 200f, 390f, paint)
            combined
        }
    }

    if (bitmaps.isEmpty()) return

    // Create a final vertical bitmap containing all
    val spacing = 20
    val totalHeight = bitmaps.sumOf { it.getHeight() } + (bitmaps.size + 1) * spacing
    val maxWidth = bitmaps.maxOf { it.getWidth() }
    
    val finalBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    canvas.drawColor(Color.WHITE)
    
    var currentY = spacing.toFloat()
    for (bitmap in bitmaps) {
        canvas.drawBitmap(bitmap, (maxWidth - bitmap.getWidth()) / 2f, currentY, null)
        currentY += bitmap.getHeight() + spacing
    }

    val printHelper = PrintHelper(context)
    printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
    printHelper.printBitmap("Etiquetas QR Code", finalBitmap)
}
