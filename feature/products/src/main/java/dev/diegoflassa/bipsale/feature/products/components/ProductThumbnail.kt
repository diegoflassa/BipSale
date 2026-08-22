package dev.diegoflassa.bipsale.feature.products.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.feature.products.R
import java.io.File

/**
 * Product image with a placeholder for products that have none.
 *
 * Missing files fall through to the same placeholder via the loader's error slot — probing with
 * `File.exists()` instead would put a disk read inside composition, once per row per frame.
 */
@Composable
fun ProductThumbnail(
    imagePath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    placeholderIconSize: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imagePath == null) {
            PlaceholderIcon(placeholderIconSize)
        } else {
            SubcomposeAsyncImage(
                model = File(imagePath),
                contentDescription = stringResource(R.string.products_product_image),
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
                loading = { PlaceholderIcon(placeholderIconSize) },
                error = { PlaceholderIcon(placeholderIconSize) }
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(iconSize: Dp) {
    Icon(
        imageVector = Icons.Default.Inventory2,
        contentDescription = stringResource(R.string.products_product_placeholder),
        modifier = Modifier.size(iconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// region Previews

@Preview(
    name = "ProductThumbnail · Placeholder · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductThumbnail · Placeholder · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductThumbnailPlaceholderPreview() {
    BipSaleTheme {
        ProductThumbnail(imagePath = null)
    }
}

@Preview(
    name = "ProductThumbnail · Placeholder · Phone · Dark",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ProductThumbnailPlaceholderDarkPreview() {
    BipSaleTheme {
        ProductThumbnail(imagePath = null)
    }
}

@Preview(
    name = "ProductThumbnail · Imagem Ausente · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ProductThumbnail · Imagem Ausente · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ProductThumbnailMissingFilePreview() {
    BipSaleTheme {
        ProductThumbnail(imagePath = "/nao/existe/cafe.jpg")
    }
}

// endregion
