package dev.diegoflassa.bipsale.feature.products

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.print.PrintHelper
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productCode: String?,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf(productCode ?: "") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val context = LocalContext.current

    val isEdit = productCode != null

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is ProductContract.Effect.NavigationBack) {
                onBack()
            }
        }
    }

    LaunchedEffect(productCode) {
        if (isEdit) {
            viewModel.onIntent(ProductContract.Intent.LoadProduct(productCode!!))
        }
    }

    LaunchedEffect(uiState.editProduct) {
        uiState.editProduct?.let {
            name = it.name
            price = it.price.toString()
        }
    }

    val qrData = "bipsale://product?code=$code&price=$price"
    val bitmap = remember(qrData) {
        QrGenerator().generateQrCode(qrData, 400, 400)
    }

    AddEditProductContent(
        isEdit = isEdit,
        code = code,
        name = name,
        price = price,
        qrBitmap = bitmap,
        onBack = onBack,
        onCodeChange = { code = it },
        onNameChange = { name = it },
        onPriceChange = { price = it },
        onSave = {
            val priceVal = price.toDoubleOrNull() ?: 0.0
            viewModel.onIntent(ProductContract.Intent.SaveProduct(code, name, priceVal))
        },
        onPrint = { btm ->
            val printHelper = PrintHelper(context)
            printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
            printHelper.printBitmap("QR Code - $name", btm)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProductContent(
    isEdit: Boolean,
    code: String,
    name: String,
    price: String,
    qrBitmap: Bitmap?,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onPrint: (Bitmap) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar Produto" else "Novo Produto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { if (!isEdit) onCodeChange(it) },
                label = { Text("Código do Produto") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEdit,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nome do Produto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("Preço Unitário") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = code.isNotBlank() && name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Salvar")
            }

            if (isEdit || (code.isNotBlank() && price.isNotBlank())) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("QR Code do Produto:", style = MaterialTheme.typography.labelMedium)

                qrBitmap?.let { btm ->
                    Image(
                        bitmap = btm.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onPrint(btm) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Imprimir QR Code")
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(name = "AddEditProductContent · Novo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "AddEditProductContent · Novo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun AddEditProductContentNewPreview() {
    BipSaleTheme {
        AddEditProductContent(
            isEdit = false,
            code = "",
            name = "",
            price = "",
            qrBitmap = null,
            onBack = {},
            onCodeChange = {},
            onNameChange = {},
            onPriceChange = {},
            onSave = {},
            onPrint = {},
        )
    }
}

@Preview(name = "AddEditProductContent · Novo · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddEditProductContentNewDarkPreview() {
    BipSaleTheme {
        AddEditProductContent(
            isEdit = false,
            code = "",
            name = "",
            price = "",
            qrBitmap = null,
            onBack = {},
            onCodeChange = {},
            onNameChange = {},
            onPriceChange = {},
            onSave = {},
            onPrint = {},
        )
    }
}

@Preview(name = "AddEditProductContent · Editar Preenchido · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "AddEditProductContent · Editar Preenchido · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun AddEditProductContentEditFilledPreview() {
    BipSaleTheme {
        AddEditProductContent(
            isEdit = true,
            code = "7891000100103",
            name = "Café Premium 200ml",
            price = "12.50",
            qrBitmap = null,
            onBack = {},
            onCodeChange = {},
            onNameChange = {},
            onPriceChange = {},
            onSave = {},
            onPrint = {},
        )
    }
}

// endregion
