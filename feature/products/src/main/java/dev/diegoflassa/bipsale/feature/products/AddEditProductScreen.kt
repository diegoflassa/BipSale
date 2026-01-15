package dev.diegoflassa.bipsale.feature.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.print.PrintHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import dev.diegoflassa.bipsale.feature.qrcode.QrGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productCode: String?,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf(productCode ?: "") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
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
            val product = viewModel.getProductByCode(productCode!!)
            product?.let {
                name = it.productName
                price = it.price.toString()
            }
        }
    }

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
                onValueChange = { if (!isEdit) code = it },
                label = { Text("Código do Produto") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEdit,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Produto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Preço Unitário") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    viewModel.onIntent(ProductContract.Intent.SaveProduct(code, name, priceVal))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = code.isNotBlank() && name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Salvar")
            }

            if (isEdit || (code.isNotBlank() && price.isNotBlank())) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("QR Code do Produto:", style = MaterialTheme.typography.labelMedium)
                
                val qrData = "bipsale://product?code=$code&price=$price"
                val bitmap = remember(qrData) {
                    QrGenerator().generateQrCode(qrData, 400, 400)
                }
                
                bitmap?.let { btm ->
                    Image(
                        bitmap = btm.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val printHelper = PrintHelper(context)
                            printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
                            printHelper.printBitmap("QR Code - $name", btm)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Imprimir QR Code")
                    }
                }
            }
        }
    }
}
