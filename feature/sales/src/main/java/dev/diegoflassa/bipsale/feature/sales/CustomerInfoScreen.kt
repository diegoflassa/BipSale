package dev.diegoflassa.bipsale.feature.sales

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
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
            BipSaleTopAppBar(
                title = stringResource(R.string.sales_customer_title),
                onBack = onBack
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
            Text(
                stringResource(R.string.sales_customer_explainer),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                Text(stringResource(R.string.sales_customer_anonymous))
            }

            if (!isAnonymous) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sales_customer_name_label)) },
                    supportingText = {
                        Text(stringResource(R.string.sales_customer_name_hint))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = it },
                    label = { Text(stringResource(R.string.sales_customer_cpf_label)) },
                    supportingText = {
                        Text(stringResource(R.string.sales_customer_cpf_hint))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onNext(name, cpf, isAnonymous) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isAnonymous || (name.isNotBlank() && cpf.isNotBlank())
            ) {
                Text(stringResource(R.string.sales_customer_start))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            TextButton(onClick = onBack) {
                Text(stringResource(R.string.sales_customer_cancel))
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
