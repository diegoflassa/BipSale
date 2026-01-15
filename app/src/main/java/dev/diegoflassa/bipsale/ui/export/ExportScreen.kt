package dev.diegoflassa.bipsale.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Exportar Dados") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                onClick = {
                    try {
                        val file = File(context.getExternalFilesDir(null), "vendas_bipsale_${System.currentTimeMillis()}.xlsx")
                        FileOutputStream(file).use { outputStream ->
                            exporter.exportSalesToExcel(outputStream, uiState.salesWithItems)
                        }
                        viewModel.onIntent(HistoryContract.Intent.RefreshSales) // Ensure data is loaded
                        
                        // Show success message
                        if (uiState.salesWithItems.isEmpty()) {
                             // trigger a load if needed
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error exporting sales")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gerar Arquivo Excel")
            }
        }
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        viewModel.onIntent(HistoryContract.Intent.RefreshSales)
    }
}
