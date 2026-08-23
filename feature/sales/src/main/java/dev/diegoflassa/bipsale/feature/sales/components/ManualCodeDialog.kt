package dev.diegoflassa.bipsale.feature.sales.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.sales.R
import dev.diegoflassa.bipsale.feature.sales.SalesScreenTestTags

/**
 * Adds a product by typing its code — the fallback when a label is damaged and the camera cannot
 * read it. An unknown code is rejected by the use case, not silently rung up at zero.
 */
@Composable
fun ManualCodeDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier.testTag(SalesScreenTestTags.MANUAL_CODE_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_manual_code_title)) },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SalesScreenTestTags.MANUAL_CODE_FIELD),
                value = code,
                onValueChange = { code = it },
                singleLine = true,
                label = { Text(stringResource(R.string.sales_manual_code_hint)) }
            )
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank(),
                onClick = { onConfirm(code) }
            ) {
                Text(stringResource(R.string.sales_manual_code_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sales_discount_cancel))
            }
        }
    )
}

// region Previews

@Preview(name = "ManualCodeDialog · Vazio · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "ManualCodeDialog · Vazio · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun ManualCodeDialogEmptyPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ManualCodeDialog(onConfirm = {}, onDismiss = {})
        }
    }
}

@Preview(name = "ManualCodeDialog · Vazio · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManualCodeDialogEmptyDarkPreview() {
    BipSaleTheme {
        Box(Modifier.fillMaxSize()) {
            ManualCodeDialog(onConfirm = {}, onDismiss = {})
        }
    }
}

// endregion
