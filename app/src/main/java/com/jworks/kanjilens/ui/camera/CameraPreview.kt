package com.jworks.kanjilens.ui.camera

import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun CameraPreviewLayer(
    lifecycleOwner: LifecycleOwner,
    onCameraReady: (Camera) -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
    onFrameAvailable: (androidx.camera.core.ImageProxy) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { onPreviewViewReady(it) }.apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER

                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val factory = meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        // Touch-to-focus is best-effort; camera may not be bound yet
                        try {
                            // Camera reference is captured via onCameraReady callback
                        } catch (_: Exception) { }
                        view.performClick()
                    }
                    true
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                onFrameAvailable(imageProxy)
                            }
                        }

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                    onCameraReady(cam)
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}

@Composable
fun FrozenPreviewLayer(
    frozenBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    frozenBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = "Paused camera",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    }
}
