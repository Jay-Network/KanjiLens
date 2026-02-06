package com.jworks.kanjilens.ui.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.repository.SettingsRepository
import com.jworks.kanjilens.domain.usecases.EnrichWithFuriganaUseCase
import com.jworks.kanjilens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val enrichWithFurigana: EnrichWithFuriganaUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CameraVM"
        private const val STATS_WINDOW = 30 // rolling average over N frames
    }

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _detectedTexts = MutableStateFlow<List<DetectedText>>(emptyList())
    val detectedTexts: StateFlow<List<DetectedText>> = _detectedTexts.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _sourceImageSize = MutableStateFlow(Size(480, 640))
    val sourceImageSize: StateFlow<Size> = _sourceImageSize.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0)
    val rotationDegrees: StateFlow<Int> = _rotationDegrees.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _ocrStats = MutableStateFlow(OCRStats())
    val ocrStats: StateFlow<OCRStats> = _ocrStats.asStateFlow()

    private var frameCount = 0
    private val recentTimings = ArrayDeque<Long>(STATS_WINDOW)

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun updatePartialModeBoundaryRatio(ratio: Float) {
        viewModelScope.launch {
            val updated = settings.value.copy(partialModeBoundaryRatio = ratio)
            settingsRepository.updateSettings(updated)
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        frameCount++
        val frameSkip = settings.value.frameSkip
        if (frameCount % frameSkip != 0 || _isProcessing.value) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        _isProcessing.value = true
        val rotation = imageProxy.imageInfo.rotationDegrees
        _rotationDegrees.value = rotation

        // In partial mode, crop image to visible region for FPS boost
        val boundaryRatio = settings.value.partialModeBoundaryRatio
        if (boundaryRatio < 0.95f) {
            val rect = mediaImage.cropRect
            val w = rect.width()
            val h = rect.height()
            mediaImage.cropRect = when (rotation) {
                90 -> {
                    // Screen top = raw image left
                    val cropW = (w * boundaryRatio).toInt().coerceAtLeast(100)
                    android.graphics.Rect(rect.left, rect.top, rect.left + cropW, rect.bottom)
                }
                270 -> {
                    // Screen top = raw image right
                    val cropW = (w * boundaryRatio).toInt().coerceAtLeast(100)
                    android.graphics.Rect(rect.right - cropW, rect.top, rect.right, rect.bottom)
                }
                180 -> {
                    // Screen top = raw image bottom
                    val cropH = (h * boundaryRatio).toInt().coerceAtLeast(100)
                    android.graphics.Rect(rect.left, rect.bottom - cropH, rect.right, rect.bottom)
                }
                else -> {
                    // rotation=0: Screen top = raw image top
                    val cropH = (h * boundaryRatio).toInt().coerceAtLeast(100)
                    android.graphics.Rect(rect.left, rect.top, rect.right, rect.top + cropH)
                }
            }
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val cropRect = mediaImage.cropRect
        val imageSize = Size(cropRect.width(), cropRect.height())

        // Debug: Log actual camera dimensions (especially for Z Flip 7)
        if (frameCount % (settings.value.frameSkip * 20) == 0) {
            Log.d(TAG, "Camera frame: ${mediaImage.width}x${mediaImage.height}, crop=${cropRect.width()}x${cropRect.height()}, rotation=$rotation°, boundary=$boundaryRatio")
        }

        viewModelScope.launch {
            try {
                val result = processCameraFrame.execute(inputImage, imageSize)
                // Try to enrich with furigana, but fall back to raw OCR if it fails
                val enriched = try {
                    enrichWithFurigana.execute(result.texts)
                } catch (e: Exception) {
                    android.util.Log.w("CameraVM", "Furigana enrichment failed, using raw OCR", e)
                    result.texts
                }
                _detectedTexts.value = enriched
                _sourceImageSize.value = result.imageSize
                updateStats(result.processingTimeMs, enriched.size)
            } catch (_: Exception) {
                // OCR failed for this frame, keep previous results
            } finally {
                _isProcessing.value = false
                imageProxy.close()
            }
        }
    }

    private fun updateStats(processingTimeMs: Long, lineCount: Int) {
        if (recentTimings.size >= STATS_WINDOW) recentTimings.removeFirst()
        recentTimings.addLast(processingTimeMs)

        val avgMs = recentTimings.average().toLong()
        val frameSkip = settings.value.frameSkip
        _ocrStats.value = OCRStats(
            lastFrameMs = processingTimeMs,
            avgFrameMs = avgMs,
            framesProcessed = frameCount / frameSkip,
            linesDetected = lineCount
        )

        if (frameCount % (frameSkip * 10) == 0) {
            Log.d(TAG, "OCR stats: avg=${avgMs}ms, last=${processingTimeMs}ms, lines=$lineCount")
        }
    }
}

data class OCRStats(
    val lastFrameMs: Long = 0,
    val avgFrameMs: Long = 0,
    val framesProcessed: Int = 0,
    val linesDetected: Int = 0
)
