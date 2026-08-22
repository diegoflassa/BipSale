package dev.diegoflassa.bipsale.ui.backup.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.ui.backup.BackupScreenTestTags
import java.text.DateFormat
import java.util.Date

/** What the last backup or restore actually moved, so the operator can sanity-check it. */
@Composable
fun BackupSummaryCard(summary: BackupSummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.testTag(BackupScreenTestTags.SUMMARY)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.backup_summary_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.backup_summary_counts,
                    summary.products,
                    summary.sales,
                    summary.saleItems,
                    summary.images
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.backup_summary_created_at,
                    DateFormat.getDateTimeInstance().format(Date(summary.createdAt))
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// region Previews

private val previewSummary = BackupSummary(
    products = 42,
    sales = 318,
    saleItems = 927,
    images = 37,
    createdAt = 1_787_400_000_000
)

@Preview(
    name = "BackupSummaryCard · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupSummaryCard · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupSummaryCardPreview() {
    BipSaleTheme {
        BackupSummaryCard(summary = previewSummary)
    }
}

@Preview(
    name = "BackupSummaryCard · Padrao · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BackupSummaryCardDarkPreview() {
    BipSaleTheme {
        BackupSummaryCard(summary = previewSummary)
    }
}

@Preview(
    name = "BackupSummaryCard · Vazio · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "BackupSummaryCard · Vazio · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun BackupSummaryCardEmptyPreview() {
    BipSaleTheme {
        BackupSummaryCard(
            summary = BackupSummary(
                products = 0,
                sales = 0,
                saleItems = 0,
                images = 0,
                createdAt = 1_787_400_000_000
            )
        )
    }
}

// endregion
