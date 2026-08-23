package dev.diegoflassa.bipsale.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import dev.diegoflassa.bipsale.core.ui.R
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import java.io.File

/**
 * The product photo at full size.
 *
 * `usePlatformDefaultWidth = false` because the platform's default dialog width would letterbox the
 * image into a small rectangle, which defeats the point of expanding it.
 */
@Composable
fun ProductImageDialog(
    imagePath: String,
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
                .testTag(ProductImageDialogTestTags.ROOT),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = File(imagePath),
                contentDescription = stringResource(R.string.common_product_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit,
                error = {
                    Text(
                        text = stringResource(R.string.common_product_image_unavailable),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .testTag(ProductImageDialogTestTags.CLOSE)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = Color.White
                )
            }
        }
    }
}

object ProductImageDialogTestTags {
    const val ROOT = "product_image_dialog"
    const val CLOSE = "product_image_dialog_close"
}

private const val SCRIM_ALPHA = 0.9f

// region Previews

@Preview(name = "ProductImageDialog · Ausente · Phone", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420")
@Composable
private fun ProductImageDialogMissingPreview() {
    BipSaleTheme {
        ProductImageDialog(imagePath = "/nao/existe/cafe.jpg", onDismiss = {})
    }
}

@Preview(name = "ProductImageDialog · Ausente · Phone · Dark", showBackground = true, locale = "pt", device = "spec:width=1080px,height=2520px,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProductImageDialogMissingDarkPreview() {
    BipSaleTheme {
        ProductImageDialog(imagePath = "/nao/existe/cafe.jpg", onDismiss = {})
    }
}

// endregion
