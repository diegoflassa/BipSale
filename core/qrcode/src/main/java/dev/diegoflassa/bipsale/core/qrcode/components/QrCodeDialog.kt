package dev.diegoflassa.bipsale.core.qrcode.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.diegoflassa.bipsale.core.ui.R

/**
 * A QR code filling the screen, for when a customer's phone will not read the inline one.
 *
 * Drawn on a white plate rather than the theme surface: a dark background behind a code inverts
 * its quiet zone, and many scanners refuse to read that at all.
 */
@Composable
fun QrCodeDialog(
    bitmap: Bitmap,
    caption: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .testTag(QrCodeDialogTestTags.ROOT),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.White)
                        .padding(16.dp)
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(QrCodeDialogTestTags.CLOSE)
                ) {
                    Text(text = stringResource(R.string.common_close), color = Color.White)
                }
            }
        }
    }
}

object QrCodeDialogTestTags {
    const val ROOT = "qr_code_dialog"
    const val CLOSE = "qr_code_dialog_close"
}

private const val SCRIM_ALPHA = 0.92f
