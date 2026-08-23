package dev.diegoflassa.bipsale.core.qrcode

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

/**
 * Full-screen QR capture: camera underneath, a dimmed overlay with a scan window on top, and the
 * two controls an operator needs while holding a product one-handed.
 *
 * The caller is responsible for having the camera permission already granted.
 */
@ExperimentalGetImage
@Composable
fun QrScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var torchEnabled by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    // Confirming the hit before handing it over: at a counter the operator is looking at the
    // product, not the screen, so the buzz is what tells them the scan landed.
    LaunchedEffect(scannedCode) {
        val code = scannedCode ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onQrCodeScanned(code)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(QrScannerScreenTestTags.ROOT)
    ) {
        QrScannerView(
            onQrCodeScanned = { scannedCode = it },
            torchEnabled = torchEnabled,
            onTorchAvailable = { torchAvailable = it }
        )

        QrScannerOverlay()

        ScannerControls(
            torchEnabled = torchEnabled,
            torchAvailable = torchAvailable,
            onToggleTorch = { torchEnabled = !torchEnabled },
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Text(
            text = stringResource(R.string.qr_scanner_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp)
                .testTag(QrScannerScreenTestTags.HINT)
        )
    }
}

@Composable
private fun ScannerControls(
    torchEnabled: Boolean,
    torchAvailable: Boolean,
    onToggleTorch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = Color.Black.copy(alpha = 0.45f),
        contentColor = Color.White
    )

    Column(modifier = modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onClose,
                colors = buttonColors,
                shape = CircleShape,
                modifier = Modifier.testTag(QrScannerScreenTestTags.CLOSE_BUTTON)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.qr_scanner_close)
                )
            }

            Text(
                text = stringResource(R.string.qr_scanner_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            if (torchAvailable) {
                FilledIconButton(
                    onClick = onToggleTorch,
                    colors = buttonColors,
                    shape = CircleShape,
                    modifier = Modifier.testTag(QrScannerScreenTestTags.TORCH_BUTTON)
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn
                        else Icons.Default.FlashOff,
                        contentDescription = stringResource(
                            if (torchEnabled) R.string.qr_scanner_torch_off
                            else R.string.qr_scanner_torch_on
                        )
                    )
                }
            } else {
                // Keeps the title centred on hardware with no flash unit.
                IconButton(onClick = {}, enabled = false) {}
            }
        }
    }
}

// region Previews

@Preview(
    name = "ScannerControls · Com Lanterna · Phone",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ScannerControls · Com Lanterna · Tablet",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ScannerControlsTorchPreview() {
    BipSaleTheme {
        ScannerControls(
            torchEnabled = false,
            torchAvailable = true,
            onToggleTorch = {},
            onClose = {}
        )
    }
}

@Preview(
    name = "ScannerControls · Lanterna Ligada · Phone",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ScannerControls · Lanterna Ligada · Tablet",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ScannerControlsTorchOnPreview() {
    BipSaleTheme {
        ScannerControls(
            torchEnabled = true,
            torchAvailable = true,
            onToggleTorch = {},
            onClose = {}
        )
    }
}

@Preview(
    name = "ScannerControls · Sem Lanterna · Phone",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "ScannerControls · Sem Lanterna · Tablet",
    showBackground = true,
    backgroundColor = 0xFF000000,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun ScannerControlsNoTorchPreview() {
    BipSaleTheme {
        ScannerControls(
            torchEnabled = false,
            torchAvailable = false,
            onToggleTorch = {},
            onClose = {}
        )
    }
}

// endregion
