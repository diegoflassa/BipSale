package dev.diegoflassa.bipsale.core.qrcode

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera preview that reports the first QR code it decodes.
 *
 * @param onTorchAvailable reports whether this camera has a flash unit, so the caller only offers
 *   the control on hardware that has one.
 */
@ExperimentalGetImage
@Composable
fun QrScannerView(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    onTorchAvailable: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onQrCodeScanned)
    val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)

    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // The analyzer fires on every frame that decodes. Between the first hit and the screen closing,
    // several more frames land — without this latch each one adds another item to the cart.
    val alreadyScanned = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            val provider = runCatching { providerFuture.get() }.getOrElse { throwable ->
                Timber.e(throwable, "[BipSale][Scanner] Camera provider unavailable")
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(
                        executor,
                        QrCodeAnalyzer { code ->
                            if (alreadyScanned.compareAndSet(false, true)) {
                                Timber.d("[BipSale][Scanner] Decoded a code, length=%d", code.length)
                                currentOnScanned(code)
                            }
                        }
                    )
                }

            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }.onSuccess { bound ->
                camera = bound
                val hasFlash = bound.cameraInfo.hasFlashUnit()
                Timber.d("[BipSale][Scanner] Camera bound, flashUnit=%b", hasFlash)
                currentOnTorchAvailable(hasFlash)
            }.onFailure { throwable ->
                Timber.e(throwable, "[BipSale][Scanner] Could not bind the camera")
                currentOnTorchAvailable(false)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
                .onFailure { Timber.e(it, "[BipSale][Scanner] Failed to unbind the camera") }
            executor.shutdown()
            camera = null
        }
    }

    LaunchedEffect(camera, torchEnabled) {
        val control = camera ?: return@LaunchedEffect
        runCatching { control.cameraControl.enableTorch(torchEnabled) }
            .onFailure { Timber.e(it, "[BipSale][Scanner] Torch toggle failed") }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}
