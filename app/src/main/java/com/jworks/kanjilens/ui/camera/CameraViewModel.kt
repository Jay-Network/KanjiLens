package com.jworks.kanjilens.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.TextElement
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
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val enrichWithFurigana: EnrichWithFuriganaUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CameraVM"
        private const val STATS_WINDOW = 30 // rolling average over N frames
        private const val SMOOTHING_ALPHA = 0.3f // 30% new position, 70% old (higher = less smooth)
        private const val MAX_TRACKING_DISTANCE = 100f // Max pixel distance to match elements
    }

    // Position stabilization: track text elements across frames
    private data class TrackedElement(
        val text: String,
        val smoothedBounds: Rect,
        val lastSeenTime: Long
    )

    private var trackedElements = mutableMapOf<String, TrackedElement>()

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

    private val _canvasSize = MutableStateFlow(Size(1080, 2400)) // Default estimate
    val canvasSize: StateFlow<Size> = _canvasSize.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _ocrStats = MutableStateFlow(OCRStats())
    val ocrStats: StateFlow<OCRStats> = _ocrStats.asStateFlow()

    private var frameCount = 0
    private val recentTimings = ArrayDeque<Long>(STATS_WINDOW)
    private var modeSwitchPauseFrames = 0  // Pause frames after mode switch

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun updateCanvasSize(size: Size) {
        _canvasSize.value = size
    }

    fun updateVerticalTextMode(enabled: Boolean) {
        viewModelScope.launch {
            val updated = settings.value.copy(verticalTextMode = enabled)
            settingsRepository.updateSettings(updated)
        }
    }

    fun updatePartialModeBoundaryRatio(ratio: Float) {
        viewModelScope.launch {
            // Clear detections when switching modes (fresh start)
            _detectedTexts.value = emptyList()

            // Pause processing for 15 frames (~0.5 seconds) to let UI settle
            modeSwitchPauseFrames = 15

            // Update settings
            val updated = settings.value.copy(partialModeBoundaryRatio = ratio)
            settingsRepository.updateSettings(updated)
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        frameCount++

        // Skip frames during mode switch pause
        if (modeSwitchPauseFrames > 0) {
            modeSwitchPauseFrames--
            imageProxy.close()
            return
        }

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

        // Use MediaImage directly (simpler and faster)
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val cropRect = mediaImage.cropRect
        val imageSize = Size(cropRect.width(), cropRect.height())

        // Debug: Log processing dimensions
        if (frameCount % (settings.value.frameSkip * 5) == 0) {
            Log.d(TAG, "Processing: ${imageSize.width}x${imageSize.height}, rotation=$rotation°, boundary=${settings.value.partialModeBoundaryRatio}")
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

                // Filter to visible region in partial mode
                val filtered = if (settings.value.partialModeBoundaryRatio < 0.99f) {
                    val canvasSize = _canvasSize.value
                    val visibleRegion = calculateVisibleRegion(
                        result.imageSize,
                        canvasSize,
                        settings.value.partialModeBoundaryRatio,
                        rotation
                    )

                    enriched.mapNotNull { detected ->
                        val visibleElements = detected.elements.filter { element ->
                            val bounds = element.bounds ?: return@filter false
                            Rect.intersects(bounds, visibleRegion)
                        }
                        if (visibleElements.isEmpty()) null
                        else detected.copy(elements = visibleElements)
                    }
                } else {
                    enriched
                }

                // Sort by position (top-to-bottom, left-to-right) to prevent order jumping
                val sorted = filtered.sortedWith(compareBy(
                    { it.bounds?.top ?: Int.MAX_VALUE },
                    { it.bounds?.left ?: Int.MAX_VALUE }
                ))
                _detectedTexts.value = sorted
                _sourceImageSize.value = result.imageSize
                updateStats(result.processingTimeMs, sorted.size)
            } catch (_: Exception) {
                // OCR failed for this frame, keep previous results
            } finally {
                _isProcessing.value = false
                imageProxy.close()
            }
        }
    }

    /**
     * Calculate which portion of camera frame is visible on screen.
     * Uses FILL_CENTER scaling to map canvas area to image coordinates.
     */
    private fun calculateVisibleRegion(
        imageSize: Size,
        canvasSize: Size,
        displayBoundary: Float,
        rotationDegrees: Int
    ): Rect {
        // Handle rotation (swap dimensions if rotated)
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = (if (isRotated) imageSize.height else imageSize.width).toFloat()
        val effectiveHeight = (if (isRotated) imageSize.width else imageSize.height).toFloat()

        // FILL_CENTER scale (matches OverlayCanvas.kt logic)
        val scale = maxOf(
            canvasSize.width / effectiveWidth,
            canvasSize.height / effectiveHeight
        )

        // Full mode: entire image visible
        if (displayBoundary >= 0.99f) {
            return Rect(0, 0, imageSize.width, imageSize.height)
        }

        // Partial mode: map visible canvas height to image coordinates
        val visibleCanvasHeight = canvasSize.height * displayBoundary
        val visibleImageHeight = (visibleCanvasHeight / scale).toInt()

        // FILL_CENTER crops equally from edges
        val scaledImageHeight = effectiveHeight * scale
        if (scaledImageHeight > canvasSize.height) {
            // Vertical crop: offset from top
            val cropOffsetTop = ((effectiveHeight - canvasSize.height / scale) / 2f).toInt()
            return Rect(0, cropOffsetTop, imageSize.width, cropOffsetTop + visibleImageHeight)
        } else {
            // Horizontal crop: offset from left
            val cropOffsetLeft = ((effectiveWidth - canvasSize.width / scale) / 2f).toInt()
            return Rect(cropOffsetLeft, 0, cropOffsetLeft + (canvasSize.width / scale).toInt(), visibleImageHeight)
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

    /**
     * Apply position stabilization to minimize jitter from hand shake.
     * Smooths bounding box positions for text elements seen in consecutive frames.
     */
    private fun applyPositionStabilization(texts: List<DetectedText>): List<DetectedText> {
        val currentTime = System.currentTimeMillis()
        val newTracked = mutableMapOf<String, TrackedElement>()

        // Process each detected text and its elements
        val stabilizedTexts = texts.map { detectedText ->
            val stabilizedElements = detectedText.elements.map { element ->
                val bounds = element.bounds ?: return@map element
                val key = "${element.text}_${bounds.centerX()}_${bounds.centerY()}"

                // Try to find matching tracked element from previous frame
                val matched = findMatchingElement(element, bounds)

                val smoothedBounds = if (matched != null) {
                    // Apply exponential smoothing: new = alpha * raw + (1-alpha) * old
                    lerpRect(matched.smoothedBounds, bounds, SMOOTHING_ALPHA)
                } else {
                    // New element, use raw bounds (no smoothing on first appearance)
                    bounds
                }

                // Track this element for next frame
                newTracked[element.text] = TrackedElement(
                    text = element.text,
                    smoothedBounds = smoothedBounds,
                    lastSeenTime = currentTime
                )

                // Return element with smoothed bounds
                element.copy(bounds = smoothedBounds)
            }

            detectedText.copy(elements = stabilizedElements)
        }

        // Update tracked elements (keep only recent ones)
        trackedElements = newTracked

        return stabilizedTexts
    }

    /**
     * Find a matching element from previous frame based on text content and proximity
     */
    private fun findMatchingElement(element: TextElement, bounds: Rect): TrackedElement? {
        val tracked = trackedElements[element.text] ?: return null

        // Check if positions are close enough (within MAX_TRACKING_DISTANCE pixels)
        val distance = distanceBetweenRects(tracked.smoothedBounds, bounds)
        return if (distance < MAX_TRACKING_DISTANCE) tracked else null
    }

    /**
     * Calculate distance between centers of two rectangles
     */
    private fun distanceBetweenRects(r1: Rect, r2: Rect): Float {
        val dx = r1.centerX() - r2.centerX()
        val dy = r1.centerY() - r2.centerY()
        return sqrt((dx * dx + dy * dy).toFloat())
    }

    /**
     * Linear interpolation between two rectangles
     * @param alpha: 0.0 = fully r1, 1.0 = fully r2
     */
    private fun lerpRect(r1: Rect, r2: Rect, alpha: Float): Rect {
        val invAlpha = 1f - alpha
        return Rect(
            (r1.left * invAlpha + r2.left * alpha).toInt(),
            (r1.top * invAlpha + r2.top * alpha).toInt(),
            (r1.right * invAlpha + r2.right * alpha).toInt(),
            (r1.bottom * invAlpha + r2.bottom * alpha).toInt()
        )
    }

    /**
     * Convert ImageProxy to Bitmap for explicit cropping
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image ?: throw IllegalStateException("Image is null")
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}

data class OCRStats(
    val lastFrameMs: Long = 0,
    val avgFrameMs: Long = 0,
    val framesProcessed: Int = 0,
    val linesDetected: Int = 0
)
