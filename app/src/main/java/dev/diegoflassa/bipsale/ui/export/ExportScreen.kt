package dev.diegoflassa.bipsale.ui.export

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.utils.ExcelExporter
import dev.diegoflassa.bipsale.feature.history.HistoryContract
import dev.diegoflassa.bipsale.feature.history.HistoryViewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exporter = remember { ExcelExporter() }

    ExportScreenContent(
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExport = {
            try {
                val file = File(context.getExternalFilesDir(null), "vendas_bipsale_${System.currentTimeMillis()}.xlsx")
                FileOutputStream(file).use { outputStream ->
                    exporter.exportSalesToExcel(outputStream, uiState.sales)
                }
                viewModel.onIntent(HistoryContract.Intent.RefreshSales) // Ensure data is loaded

                // Show success message
                if (uiState.sales.isEmpty()) {
                     // trigger a load if needed
                }
            } catch (e: Exception) {
                Timber.e(e, "[BipSale][Export] Error exporting sales")
            }
        },
    )

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.onIntent(HistoryContract.Intent.RefreshSales)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreenContent(
    onBack: () -> Unit,
    onExport: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.export_back)
                        )
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Exportar Histórico de Vendas",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Gere um arquivo Excel (.xlsx) com todas as vendas registradas.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.export_generate_excel))
            }
        }
    }
}

// region Previews

@Preview(name = "ExportScreenContent · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ExportScreenContent · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ExportScreenContentPreview() {
    BipSaleTheme {
        ExportScreenContent(onBack = {}, onExport = {})
    }
}

@Preview(name = "ExportScreenContent · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExportScreenContentDarkPreview() {
    BipSaleTheme {
        ExportScreenContent(onBack = {}, onExport = {})
    }
}

// endregion
