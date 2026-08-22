package dev.diegoflassa.bipsale.feature.products.components

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.products.R

/**
 * Tappable product photo that opens the system photo picker.
 *
 * The picker grants a one-shot read permission that cannot be persisted, so the caller is expected
 * to copy the image into app storage rather than keep the returned URI.
 */
@Composable
fun ProductImagePicker(
    imagePath: String?,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let(onImagePicked)
    }

    Box(modifier = modifier.size(PICKER_SIZE_DP.dp)) {
        Card(
            onClick = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .size(PICKER_SIZE_DP.dp)
                .semantics { role = Role.Button },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            ProductThumbnail(
                imagePath = imagePath,
                size = PICKER_SIZE_DP.dp,
                shape = CircleShape,
                placeholderIconSize = PLACEHOLDER_ICON_DP.dp
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(BADGE_SIZE_DP.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    // The card behind it already announces the action.
                    contentDescription = null,
                    modifier = Modifier.size(BADGE_ICON_DP.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private const val PICKER_SIZE_DP = 120
private const val PLACEHOLDER_ICON_DP = 48
private const val BADGE_SIZE_DP = 36
private const val BADGE_ICON_DP = 20

// region Previews

@Preview(
    name = "ProductImagePicker · Vazio · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductImagePicker · Vazio · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductImagePickerEmptyPreview() {
    BipSaleTheme {
        ProductImagePicker(imagePath = null, onImagePicked = {})
    }
}

@Preview(
    name = "ProductImagePicker · Vazio · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ProductImagePickerEmptyDarkPreview() {
    BipSaleTheme {
        ProductImagePicker(imagePath = null, onImagePicked = {})
    }
}

@Preview(
    name = "ProductImagePicker · Imagem Ausente · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductImagePicker · Imagem Ausente · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductImagePickerMissingFilePreview() {
    BipSaleTheme {
        ProductImagePicker(imagePath = "/nao/existe/coturno.jpg", onImagePicked = {})
    }
}

// endregion
