package dev.diegoflassa.bipsale.core.qrcode.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.ui.R

/**
 * Lets the operator choose whether the QR carries the sale amount or leaves it for the customer to
 * type in their bank app — matching the physical poster flow.
 */
@Composable
fun PixAmountToggle(
    carriesAmount: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PixAmountToggleTestTags.ROOT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                if (carriesAmount) R.string.common_pix_amount_with_value
                else R.string.common_pix_amount_customer_types
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = carriesAmount,
            onCheckedChange = onToggle
        )
    }
}

object PixAmountToggleTestTags {
    const val ROOT = "pix_amount_toggle"
}

// region Previews

@Preview(name = "PixAmountToggle · Com valor · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "PixAmountToggle · Com valor · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun PixAmountToggleWithValuePreview() {
    BipSaleTheme {
        PixAmountToggle(carriesAmount = true, onToggle = {})
    }
}

@Preview(name = "PixAmountToggle · Sem valor · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PixAmountToggleCustomerTypesPreview() {
    BipSaleTheme {
        PixAmountToggle(carriesAmount = false, onToggle = {})
    }
}

// endregion
