package dev.diegoflassa.bipsale.feature.sales

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerInfoScreen(
    onNext: (String, String, Boolean) -> Unit,
    onBack: () -> Unit,
    initialName: String = "",
    initialCpf: String = "",
    initialIsAnonymous: Boolean = false,
) {
    var name by remember { mutableStateOf(initialName) }
    var cpf by remember { mutableStateOf(initialCpf) }
    var isAnonymous by remember { mutableStateOf(initialIsAnonymous) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dados do Cliente") })
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
            Text(
                "Informe os dados do cliente para esta venda ou selecione 'Cliente Anônimo'.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                Text("Cliente Anônimo")
            }

            if (!isAnonymous) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Cliente") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = it },
                    label = { Text("CPF") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onNext(name, cpf, isAnonymous) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isAnonymous || (name.isNotBlank() && cpf.isNotBlank())
            ) {
                Text("Iniciar Venda")
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
            
            TextButton(onClick = onBack) {
                Text("Cancelar")
            }
        }
    }
}

// region Previews

@Preview(name = "CustomerInfoScreen · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CustomerInfoScreen · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CustomerInfoScreenDefaultPreview() {
    BipSaleTheme {
        CustomerInfoScreen(onNext = { _, _, _ -> }, onBack = {})
    }
}

@Preview(name = "CustomerInfoScreen · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CustomerInfoScreenDefaultDarkPreview() {
    BipSaleTheme {
        CustomerInfoScreen(onNext = { _, _, _ -> }, onBack = {})
    }
}

@Preview(name = "CustomerInfoScreen · Preenchido · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CustomerInfoScreen · Preenchido · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CustomerInfoScreenFilledPreview() {
    BipSaleTheme {
        CustomerInfoScreen(
            onNext = { _, _, _ -> },
            onBack = {},
            initialName = "Maria Aparecida Souza",
            initialCpf = "123.456.789-00",
        )
    }
}

@Preview(name = "CustomerInfoScreen · Anônimo · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "CustomerInfoScreen · Anônimo · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun CustomerInfoScreenAnonymousPreview() {
    BipSaleTheme {
        CustomerInfoScreen(onNext = { _, _, _ -> }, onBack = {}, initialIsAnonymous = true)
    }
}

// endregion
