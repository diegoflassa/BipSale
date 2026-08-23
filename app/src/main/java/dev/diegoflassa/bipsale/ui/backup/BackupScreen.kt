package dev.diegoflassa.bipsale.ui.backup

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.ui.components.BipSaleTopAppBar
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.ui.backup.components.BackupSummaryCard
import dev.diegoflassa.bipsale.ui.backup.components.RestoreConfirmationDialog

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // The system document picker is what puts Google Drive (and any other storage provider the
    // device has) in reach, without this app holding Drive credentials of its own.
    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(BackupContract.Intent.CreateBackup(it.toString())) }
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(BackupContract.Intent.RestoreRequested(it.toString())) }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BackupContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message.asString(context))

                is BackupContract.Effect.ShareFile -> context.shareArchive(effect)
            }
        }
    }

    BackupScreenContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
        onPickDestination = { createLauncher.launch(viewModel.suggestedFileName()) },
        onPickSource = { openLauncher.launch(arrayOf(BACKUP_MIME_TYPE, ANY_MIME_TYPE)) }
    )
}

private fun Context.shareArchive(effect: BackupContract.Effect.ShareFile) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = BACKUP_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, Uri.parse(effect.uri))
        putExtra(Intent.EXTRA_SUBJECT, effect.fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(share, getString(R.string.backup_share_chooser)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupScreenContent(
    state: BackupContract.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (BackupContract.Intent) -> Unit,
    onPickDestination: () -> Unit,
    onPickSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(BackupScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BipSaleTopAppBar(
                title = stringResource(R.string.backup_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.backup_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BackupActions(
                enabled = !state.isBusy,
                onCreate = onPickDestination,
                onShare = { onIntent(BackupContract.Intent.ShareBackup) },
                onRestore = onPickSource
            )

            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(BackupScreenTestTags.BUSY)
                )
            }

            state.lastSummary?.let { BackupSummaryCard(it, Modifier.fillMaxWidth()) }
        }
    }

    state.pendingRestore?.let { pending ->
        RestoreConfirmationDialog(
            metadata = pending.metadata,
            currentSummary = state.lastSummary,
            onConfirm = { onIntent(BackupContract.Intent.RestoreConfirmed) },
            onDismiss = { onIntent(BackupContract.Intent.RestoreCancelled) }
        )
    }
}

@Composable
private fun ColumnScope.BackupActions(
    enabled: Boolean,
    onCreate: () -> Unit,
    onShare: () -> Unit,
    onRestore: () -> Unit
) {
    Button(
        onClick = onCreate,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BackupScreenTestTags.CREATE_BUTTON)
    ) {
        Icon(Icons.Default.CloudUpload, contentDescription = null)
        Text(
            text = stringResource(R.string.backup_create),
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    OutlinedButton(
        onClick = onShare,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BackupScreenTestTags.SHARE_BUTTON)
    ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Text(
            text = stringResource(R.string.backup_share),
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    // Destructive, so it carries the error colour rather than sitting level with the safe actions.
    OutlinedButton(
        onClick = onRestore,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BackupScreenTestTags.RESTORE_BUTTON)
    ) {
        Icon(Icons.Default.Restore, contentDescription = null)
        Text(
            text = stringResource(R.string.backup_restore),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private const val BACKUP_MIME_TYPE = "application/zip"
private const val ANY_MIME_TYPE = "*/*"

// region Previews

private val previewSummary = BackupSummary(
    products = 42,
    sales = 318,
    saleItems = 927,
    images = 37,
    createdAt = 1_787_400_000_000
)

private val previewMetadata = BackupMetadata(
    products = 40,
    sales = 300,
    saleItems = 880,
    images = 35,
    createdAt = 1_787_300_000_000,
    appVersionName = "0.0.2-alpha-build_109",
    formatVersion = 1,
    schemaVersion = 1
)

@Preview(
    name = "BackupScreenContent · Idle · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupScreenContent · Idle · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupScreenContentIdlePreview() {
    BipSaleTheme {
        BackupScreenContent(
            state = BackupContract.State(),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {},
            onPickDestination = {},
            onPickSource = {}
        )
    }
}

@Preview(
    name = "BackupScreenContent · Idle · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BackupScreenContentIdleDarkPreview() {
    BipSaleTheme {
        BackupScreenContent(
            state = BackupContract.State(),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {},
            onPickDestination = {},
            onPickSource = {}
        )
    }
}

@Preview(
    name = "BackupScreenContent · Executando · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupScreenContent · Executando · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupScreenContentBusyPreview() {
    BipSaleTheme {
        BackupScreenContent(
            state = BackupContract.State(isBusy = true),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {},
            onPickDestination = {},
            onPickSource = {}
        )
    }
}

@Preview(
    name = "BackupScreenContent · Concluido · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupScreenContent · Concluido · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupScreenContentDonePreview() {
    BipSaleTheme {
        BackupScreenContent(
            state = BackupContract.State(lastSummary = previewSummary),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {},
            onPickDestination = {},
            onPickSource = {}
        )
    }
}

@Preview(
    name = "BackupScreenContent · Confirmar Restauracao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupScreenContent · Confirmar Restauracao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupScreenContentConfirmPreview() {
    BipSaleTheme {
        BackupScreenContent(
            state = BackupContract.State(
                lastSummary = previewSummary,
                pendingRestore = BackupContract.PendingRestore(
                    sourceUri = "content://backup.zip",
                    metadata = previewMetadata
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onIntent = {},
            onPickDestination = {},
            onPickSource = {}
        )
    }
}

// endregion
