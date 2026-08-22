package dev.diegoflassa.bipsale.ui.backup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.ui.backup.BackupScreenTestTags
import java.text.DateFormat
import java.util.Date

/**
 * Confirms a restore, showing what the archive holds before any of it is applied.
 *
 * Restore replaces every product, sale and image on the device, so the operator sees what they are
 * swapping in — and what it costs them — rather than a bare yes/no.
 */
@Composable
fun RestoreConfirmationDialog(
    metadata: BackupMetadata,
    currentSummary: BackupSummary?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(BackupScreenTestTags.CONFIRM_DIALOG),
        icon = {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(32.dp))
        },
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = { ArchiveContents(metadata = metadata, currentSummary = currentSummary) },
        confirmButton = {
            // Destructive, so it is styled as such rather than reading like the safe default.
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag(BackupScreenTestTags.CONFIRM_ACCEPT)
            ) {
                Text(stringResource(R.string.backup_restore_confirm_accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(BackupScreenTestTags.CONFIRM_CANCEL)
            ) {
                Text(stringResource(R.string.backup_restore_confirm_cancel))
            }
        }
    )
}

@Composable
private fun ArchiveContents(metadata: BackupMetadata, currentSummary: BackupSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.backup_metadata_heading),
            style = MaterialTheme.typography.titleSmall
        )
        Text(stringResource(R.string.backup_metadata_products, metadata.products))
        Text(stringResource(R.string.backup_metadata_sales, metadata.sales, metadata.saleItems))
        Text(stringResource(R.string.backup_metadata_images, metadata.images))
        Caption(
            stringResource(
                R.string.backup_metadata_created_at,
                DateFormat.getDateTimeInstance().format(Date(metadata.createdAt))
            )
        )
        if (metadata.appVersionName.isNotBlank()) {
            Caption(
                stringResource(R.string.backup_metadata_app_version, metadata.appVersionName)
            )
        }
        currentSummary?.let {
            Caption(
                stringResource(R.string.backup_metadata_replacing, it.products, it.sales)
            )
        }
        Text(
            text = stringResource(R.string.backup_restore_confirm_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// region Previews

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

private val previewCurrent = BackupSummary(
    products = 42,
    sales = 318,
    saleItems = 927,
    images = 37,
    createdAt = 1_787_400_000_000
)

@Preview(
    name = "RestoreConfirmationDialog · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "RestoreConfirmationDialog · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun RestoreConfirmationDialogPreview() {
    BipSaleTheme {
        // Over a scrim, matching how a dialog actually presents (PREVIEW_STANDARD section 8).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
        ) {
            RestoreConfirmationDialog(
                metadata = previewMetadata,
                currentSummary = previewCurrent,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(
    name = "RestoreConfirmationDialog · Sem Dados Atuais · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "RestoreConfirmationDialog · Sem Dados Atuais · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun RestoreConfirmationDialogNoCurrentPreview() {
    BipSaleTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
        ) {
            RestoreConfirmationDialog(
                metadata = previewMetadata,
                currentSummary = null,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

// endregion
