package dev.diegoflassa.bipsale.core.qrcode

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme

/**
 * Dims everything but the scan window, so the operator knows where to aim.
 */
@Composable
fun QrScannerOverlay(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF7FC8F8)
) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val window = scanWindow()
        drawScrimAround(window)
        drawCornerBrackets(window, accentColor)
        drawSweepLine(window, accentColor, sweep)
    }
}

private data class ScanWindow(val topLeft: Offset, val size: Size)

/** A square window, sized to the narrower edge so it fits portrait and landscape alike. */
private fun DrawScope.scanWindow(): ScanWindow {
    val side = minOf(size.width, size.height) * WINDOW_FRACTION
    return ScanWindow(
        topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
        size = Size(side, side)
    )
}

/**
 * Four bands around the window rather than a punched-out hole: clearing a hole needs its own
 * compositing layer, and four plain rects cost nothing and cannot go wrong on any GPU.
 */
private fun DrawScope.drawScrimAround(window: ScanWindow) {
    val scrim = Color.Black.copy(alpha = SCRIM_ALPHA)
    val left = window.topLeft.x
    val top = window.topLeft.y
    val right = left + window.size.width
    val bottom = top + window.size.height

    drawRect(color = scrim, topLeft = Offset.Zero, size = Size(size.width, top))
    drawRect(
        color = scrim,
        topLeft = Offset(0f, bottom),
        size = Size(size.width, size.height - bottom)
    )
    drawRect(color = scrim, topLeft = Offset(0f, top), size = Size(left, window.size.height))
    drawRect(
        color = scrim,
        topLeft = Offset(right, top),
        size = Size(size.width - right, window.size.height)
    )
}

private fun DrawScope.drawCornerBrackets(window: ScanWindow, color: Color) {
    val stroke = BRACKET_STROKE_DP.dp.toPx()
    val arm = window.size.width * BRACKET_ARM_FRACTION
    val left = window.topLeft.x
    val top = window.topLeft.y
    val right = left + window.size.width
    val bottom = top + window.size.height

    val path = Path().apply {
        moveTo(left, top + arm); lineTo(left, top); lineTo(left + arm, top)
        moveTo(right - arm, top); lineTo(right, top); lineTo(right, top + arm)
        moveTo(right, bottom - arm); lineTo(right, bottom); lineTo(right - arm, bottom)
        moveTo(left + arm, bottom); lineTo(left, bottom); lineTo(left, bottom - arm)
    }
    drawPath(path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

private fun DrawScope.drawSweepLine(window: ScanWindow, color: Color, progress: Float) {
    val y = window.topLeft.y + window.size.height * progress
    val inset = window.size.width * SWEEP_INSET_FRACTION
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color, Color.Transparent)
        ),
        start = Offset(window.topLeft.x + inset, y),
        end = Offset(window.topLeft.x + window.size.width - inset, y),
        strokeWidth = SWEEP_STROKE_DP.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private const val SCRIM_ALPHA = 0.6f
private const val WINDOW_FRACTION = 0.7f
private const val BRACKET_STROKE_DP = 4
private const val BRACKET_ARM_FRACTION = 0.18f
private const val SWEEP_STROKE_DP = 3
private const val SWEEP_INSET_FRACTION = 0.06f
private const val SWEEP_MILLIS = 2200

// region Previews

@Preview(
    name = "QrScannerOverlay · Padrao · Phone",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1080px,height=2520px,dpi=420"
)
@Preview(
    name = "QrScannerOverlay · Padrao · Tablet",
    showBackground = true,
    locale = "pt",
    device = "spec:width=1200px,height=2000px,dpi=240"
)
@Composable
private fun QrScannerOverlayPreview() {
    BipSaleTheme {
        QrScannerOverlay()
    }
}

// endregion
