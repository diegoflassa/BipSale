package dev.diegoflassa.bipsale.ui.export

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.history.HistoryContract
import dev.diegoflassa.bipsale.feature.history.HistoryViewModel

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SPREADSHEET_MIME_TYPE)
    ) { uri: Uri? ->
        val intent = if (uri == null) {
            HistoryContract.Intent.ExportCancelled
        } else {
            HistoryContract.Intent.ExportDestinationChosen(uri.toString())
        }
        viewModel.onIntent(intent)
    }

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is HistoryContract.Effect.PickExportDestination ->
                    createLauncher.launch(effect.suggestedFileName)
            }
        }
    }

    ExportScreenContent(
        saleCount = uiState.sales.size,
        isExporting = uiState.isExporting,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onExport = {
            viewModel.onIntent(
                HistoryContract.Intent.ExportRequested(HistoryContract.ExportScope.ALL)
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScreenContent(
    saleCount: Int,
    isExporting: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = stringResource(R.string.export_title),
                onBack = onBack
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
                stringResource(R.string.export_explainer_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.export_explainer_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.export_sale_count, saleCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onExport,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.export_generate_excel))
            }
        }
    }
}

private const val SPREADSHEET_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

// region Previews

@Preview(name = "ExportScreenContent · Default · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ExportScreenContent · Default · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ExportScreenContentPreview() {
    BipSaleTheme {
        ExportScreenContent(
            saleCount = 42,
            isExporting = false,
            onBack = {},
            onExport = {}
        )
    }
}

@Preview(name = "ExportScreenContent · Default · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExportScreenContentDarkPreview() {
    BipSaleTheme {
        ExportScreenContent(
            saleCount = 0,
            isExporting = false,
            onBack = {},
            onExport = {}
        )
    }
}

// endregion
