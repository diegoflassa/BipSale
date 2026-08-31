package dev.diegoflassa.bipsale.core.qrcode.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.qrcode.QrGenerator
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.ui.R

/**
 * The PIX code the customer scans to pay this sale.
 *
 * The payload already carries the amount, so the customer confirms a pre-filled value instead of
 * typing one — which is where a wrong amount at a counter otherwise comes from.
 */
@Composable
fun PixQrCard(
    payload: String,
    modifier: Modifier = Modifier
) {
    // Encoding is pure CPU over a short string, so it is cached against the payload rather than
    // redone on every recomposition of the sale screen.
    val bitmap = remember(payload) { QrGenerator().generateQrCode(payload, QR_SIZE_PX, QR_SIZE_PX) }
    var expanded by remember { mutableStateOf(false) }

    if (expanded && bitmap != null) {
        QrCodeDialog(
            bitmap = bitmap,
            caption = stringResource(R.string.common_pix_qr_title),
            onDismiss = { expanded = false }
        )
    }

    Card(modifier = modifier.fillMaxWidth().testTag(PixCardTestTags.QR_CARD)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.common_pix_qr_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (bitmap == null) {
                Text(
                    text = stringResource(R.string.common_pix_qr_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                // A phone held at an angle across a counter often will not read the inline code;
                // expanding it is the difference between a paid sale and a retyped amount.
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.common_pix_qr_title),
                    modifier = Modifier
                        .size(QR_SIZE_DP.dp)
                        .clickable(
                            onClickLabel = stringResource(R.string.common_pix_qr_expand),
                            role = Role.Image,
                            onClick = { expanded = true }
                        )
                )
                Text(
                    text = stringResource(R.string.common_pix_qr_expand_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

object PixCardTestTags {
    const val QR_CARD = "pix_qr_card"
}

private const val QR_SIZE_PX = 600
private const val QR_SIZE_DP = 220

// region Previews

private const val PREVIEW_PAYLOAD =
    "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
        "520400005303986540510.005802BR5913FULANO DE TAL6008BRASILIA62070503***6304C5A0"

@Preview(name = "PixQrCard · Padrão · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Preview(name = "PixQrCard · Padrão · Tablet", showBackground = true, locale = "pt", device = "spec:width=1200px,height=2000px,dpi=240")
@Composable
private fun PixQrCardPreview() {
    BipSaleTheme {
        PixQrCard(payload = PREVIEW_PAYLOAD)
    }
}

@Preview(name = "PixQrCard · Padrão · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PixQrCardDarkPreview() {
    BipSaleTheme {
        PixQrCard(payload = PREVIEW_PAYLOAD)
    }
}

// endregion

