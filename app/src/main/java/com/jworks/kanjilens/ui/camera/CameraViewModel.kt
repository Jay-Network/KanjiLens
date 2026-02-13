package com.jworks.kanjilens.ui.camera

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.jworks.kanjilens.data.subscription.SubscriptionManager
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.DictionaryResult
import com.jworks.kanjilens.domain.repository.DictionaryRepository
import com.jworks.kanjilens.domain.repository.SettingsRepository
import com.jworks.kanjilens.domain.usecases.EnrichWithFuriganaUseCase
import com.jworks.kanjilens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val settingsRepository: SettingsRepository,
    private val subscriptionManager: SubscriptionManager,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CameraVM"
        private const val STATS_WINDOW = 30 // rolling average over N frames
        private const val PERSIST_FRAMES = 3 // Keep previous results for N sparse frames
        const val FREE_SCAN_DURATION_SECONDS = 60
        private const val JITTER_FREEZE_PX = 8f
        private const val JITTER_SIZE_FREEZE_PX = 8f
        private const val JITTER_BLEND_DISTANCE_PX = 30f
        private const val JITTER_NEW_WEIGHT = 0.20f
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

    private val _canvasSize = MutableStateFlow(Size(1080, 2400)) // Default estimate
    val canvasSize: StateFlow<Size> = _canvasSize.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _visibleRegion = MutableStateFlow<Rect?>(null)
    val visibleRegion: StateFlow<Rect?> = _visibleRegion.asStateFlow()

    private val _ocrStats = MutableStateFlow(OCRStats())
    val ocrStats: StateFlow<OCRStats> = _ocrStats.asStateFlow()

    // Scan session state (free-tier limits)
    private val _scanTimerSeconds = MutableStateFlow(FREE_SCAN_DURATION_SECONDS)
    val scanTimerSeconds: StateFlow<Int> = _scanTimerSeconds.asStateFlow()

    private val _isScanActive = MutableStateFlow(false)
    val isScanActive: StateFlow<Boolean> = _isScanActive.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    val isPremium: StateFlow<Boolean> = subscriptionManager.isPremiumFlow

    private val _dictionaryResult = MutableStateFlow<DictionaryResult?>(null)
    val dictionaryResult: StateFlow<DictionaryResult?> = _dictionaryResult.asStateFlow()

    private val _isDictionaryLoading = MutableStateFlow(false)
    val isDictionaryLoading: StateFlow<Boolean> = _isDictionaryLoading.asStateFlow()

    fun lookupWord(word: String) {
        viewModelScope.launch {
            _isDictionaryLoading.value = true
            _dictionaryResult.value = try {
                dictionaryRepository.lookup(word)
            } catch (e: Exception) {
                Log.w(TAG, "Dictionary lookup failed for '$word'", e)
                null
            }
            _isDictionaryLoading.value = false
        }
    }

    fun clearDictionaryResult() {
        _dictionaryResult.value = null
    }

    private var timerJob: Job? = null

    fun startScan(context: Context, allowPaywall: Boolean = true) {
        if (subscriptionManager.isPremium()) {
            // Premium: no limits
            _isScanActive.value = true
            _isPaused.value = false
            return
        }

        if (!subscriptionManager.canScan(context)) {
            _isScanActive.value = false
            _isPaused.value = allowPaywall
            if (allowPaywall) {
                _showPaywall.value = true
            }
            return
        }

        subscriptionManager.incrementScanCount(context)
        _isScanActive.value = true
        _isPaused.value = false
        _scanTimerSeconds.value = FREE_SCAN_DURATION_SECONDS

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_scanTimerSeconds.value > 0) {
                delay(1000)
                _scanTimerSeconds.value = _scanTimerSeconds.value - 1
            }
            // Timer expired
            _isScanActive.value = false
            _isPaused.value = true
        }
    }

    fun stopScan() {
        timerJob?.cancel()
        _isScanActive.value = false
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    fun dismissScanOverlay() {
        _isPaused.value = false
    }

    private var frameCount = 0
    private val recentTimings = ArrayDeque<Long>(STATS_WINDOW)
    private val modeSwitchPauseFrames = java.util.concurrent.atomic.AtomicInteger(0)
    private var emptyFrameCount = 0  // Consecutive frames with fewer results than previous
    @Volatile private var cachedFrameSkip = 3

    init {
        viewModelScope.launch {
            settings.collect { cachedFrameSkip = it.frameSkip }
        }
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
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
            modeSwitchPauseFrames.set(8)

            // Update settings
            val updated = settings.value.copy(partialModeBoundaryRatio = ratio)
            settingsRepository.updateSettings(updated)
        }
    }

    /**
     * Update both vertical mode and boundary ratio atomically to avoid race condition
     * where two separate coroutines read stale settings.value and overwrite each other.
     */
    fun updateVerticalModeAndBoundary(verticalMode: Boolean, ratio: Float) {
        viewModelScope.launch {
            _detectedTexts.value = emptyList()
            modeSwitchPauseFrames.set(8)

            val updated = settings.value.copy(
                verticalTextMode = verticalMode,
                partialModeBoundaryRatio = ratio
            )
            settingsRepository.updateSettings(updated)
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        // Skip all processing when paused or scan not active (keep previous results frozen)
        if (_isPaused.value || !_isScanActive.value) {
            imageProxy.close()
            return
        }

        frameCount++

        // Skip frames during mode switch pause
        if (modeSwitchPauseFrames.get() > 0) {
            modeSwitchPauseFrames.decrementAndGet()
            imageProxy.close()
            return
        }

        val frameSkip = cachedFrameSkip
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
                        rotation,
                        settings.value.verticalTextMode
                    )
                    _visibleRegion.value = visibleRegion

                    val totalBefore = enriched.sumOf { it.elements.size }

                    val result2 = enriched.mapNotNull { detected ->
                        val visibleElements = detected.elements.filter { element ->
                            val bounds = element.bounds ?: return@filter false
                            Rect.intersects(bounds, visibleRegion)
                        }
                        if (visibleElements.isEmpty()) null
                        else detected.copy(elements = visibleElements)
                    }

                    val totalAfter = result2.sumOf { it.elements.size }

                    if (frameCount % (settings.value.frameSkip * 5) == 0) {
                        Log.d(TAG, "Filter: region=$visibleRegion, vertical=${settings.value.verticalTextMode}, boundary=${settings.value.partialModeBoundaryRatio}")
                        Log.d(TAG, "Filter: canvas=${canvasSize}, image=${result.imageSize}, rot=$rotation")
                        Log.d(TAG, "Filter: elements $totalBefore -> $totalAfter")
                        enriched.flatMap { it.elements }.take(3).forEach { elem ->
                            Log.d(TAG, "Filter: sample elem bounds=${elem.bounds}, text=${elem.text.take(10)}")
                        }
                    }

                    // Fallback: if OCR found elements but partial-region filter removed all,
                    // prefer showing unfiltered results to avoid "no recognition" on
                    // device-specific aspect-ratio/rotation mapping edge cases.
                    if (totalBefore > 0 && totalAfter == 0) {
                        Log.w(TAG, "Filter fallback triggered: preserving $totalBefore OCR elements")
                        enriched
                    } else {
                        result2
                    }
                } else {
                    enriched.also { _visibleRegion.value = null }
                }

                // Sort by position (top-to-bottom, left-to-right) to prevent order jumping
                val sorted = filtered.sortedWith(compareBy(
                    { it.bounds?.top ?: Int.MAX_VALUE },
                    { it.bounds?.left ?: Int.MAX_VALUE }
                ))
                val stabilized = stabilizeDetections(sorted, _detectedTexts.value)

                // Persist previous results when current frame has fewer elements (reduces flicker)
                // Vertical partial mode has a very narrow detection area (~110px in image space)
                // so OCR results are inherently intermittent — use longer persistence
                val isVerticalPartial = settings.value.verticalTextMode &&
                        settings.value.partialModeBoundaryRatio < 0.99f
                val persistThreshold = if (isVerticalPartial) PERSIST_FRAMES * 2 else PERSIST_FRAMES

                val prevCount = _detectedTexts.value.sumOf { it.elements.size }
                val newCount = stabilized.sumOf { it.elements.size }
                if (newCount >= prevCount || emptyFrameCount >= persistThreshold) {
                    // Good frame (same or more elements) or waited long enough — accept
                    _detectedTexts.value = stabilized
                    emptyFrameCount = if (newCount < prevCount) 1 else 0
                } else {
                    // Sparse frame — keep previous results a bit longer
                    emptyFrameCount++
                }
                _sourceImageSize.value = result.imageSize
                updateStats(result.processingTimeMs, stabilized.size)
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
     * Uses FILL_CENTER scaling to map screen region back to image coordinates.
     *
     * Horizontal partial: top HORIZ_CAMERA_HEIGHT_RATIO of screen height, full width
     * Vertical partial: right VERT_CAMERA_WIDTH_RATIO of screen width, top VERT_PAD_TOP_RATIO of height
     */
    private fun calculateVisibleRegion(
        imageSize: Size,
        canvasSize: Size,
        displayBoundary: Float,
        rotationDegrees: Int,
        isVerticalMode: Boolean
    ): Rect {
        // Full mode: entire image visible
        if (displayBoundary >= 0.99f) {
            return Rect(0, 0, imageSize.width, imageSize.height)
        }

        // Handle rotation (swap dimensions if rotated)
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = (if (isRotated) imageSize.height else imageSize.width).toFloat()
        val effectiveHeight = (if (isRotated) imageSize.width else imageSize.height).toFloat()

        if (effectiveWidth <= 0f || effectiveHeight <= 0f) {
            return Rect(0, 0, imageSize.width, imageSize.height)
        }

        // FILL_CENTER scale (matches OverlayCanvas.kt logic)
        val scale = maxOf(
            canvasSize.width / effectiveWidth,
            canvasSize.height / effectiveHeight
        )

        // Crop offsets: how much of the scaled image is cropped from each edge
        val cropOffsetX = (effectiveWidth * scale - canvasSize.width) / 2f
        val cropOffsetY = (effectiveHeight * scale - canvasSize.height) / 2f

        // Convert screen coordinates to image coordinates:
        // imageCoord = (screenCoord + cropOffset) / scale

        if (isVerticalMode) {
            // Vertical partial: right 40% of width, top 50% of height
            val screenLeft = canvasSize.width * (1f - PartialModeConstants.VERT_CAMERA_WIDTH_RATIO)
            val screenTop = 0f
            val screenRight = canvasSize.width.toFloat()
            val screenBottom = canvasSize.height * PartialModeConstants.VERT_PAD_TOP_RATIO

            val imageLeft = ((screenLeft + cropOffsetX) / scale).toInt().coerceAtLeast(0)
            val imageTop = ((screenTop + cropOffsetY) / scale).toInt().coerceAtLeast(0)
            val imageRight = ((screenRight + cropOffsetX) / scale).toInt().coerceAtMost(imageSize.width)
            val imageBottom = ((screenBottom + cropOffsetY) / scale).toInt().coerceAtMost(imageSize.height)

            return Rect(imageLeft, imageTop, imageRight, imageBottom)
        } else {
            // Horizontal partial: full width, top 25% of height
            val screenLeft = 0f
            val screenTop = 0f
            val screenRight = canvasSize.width.toFloat()
            val screenBottom = canvasSize.height * PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO

            val imageLeft = ((screenLeft + cropOffsetX) / scale).toInt().coerceAtLeast(0)
            val imageTop = ((screenTop + cropOffsetY) / scale).toInt().coerceAtLeast(0)
            val imageRight = ((screenRight + cropOffsetX) / scale).toInt().coerceAtMost(imageSize.width)
            val imageBottom = ((screenBottom + cropOffsetY) / scale).toInt().coerceAtMost(imageSize.height)

            return Rect(imageLeft, imageTop, imageRight, imageBottom)
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

    private fun stabilizeDetections(
        current: List<DetectedText>,
        previous: List<DetectedText>
    ): List<DetectedText> {
        if (previous.isEmpty()) return current

        return current.map { line ->
            // Spatial matching: find closest previous line with same/similar text
            val prevLine = previous.minByOrNull { prev ->
                spatialTextDistance(line, prev)
            }?.takeIf { spatialTextDistance(line, it) < 200f }

            val stabilizedLineBounds = stabilizeRect(line.bounds, prevLine?.bounds)

            val stabilizedElements = line.elements.map { element ->
                // Find closest previous element with matching text (spatial, not index-based)
                val prevElement = prevLine?.elements?.minByOrNull { prev ->
                    if (prev.text != element.text) Float.MAX_VALUE
                    else rectDistance(element.bounds, prev.bounds)
                }?.takeIf { it.text == element.text }

                element.copy(bounds = stabilizeRect(element.bounds, prevElement?.bounds))
            }

            line.copy(
                bounds = stabilizedLineBounds,
                elements = stabilizedElements
            )
        }
    }

    private fun spatialTextDistance(a: DetectedText, b: DetectedText): Float {
        val centerDist = rectDistance(a.bounds, b.bounds)
        val textPenalty = if (a.text == b.text) 0f else 100f
        return centerDist + textPenalty
    }

    private fun rectDistance(a: android.graphics.Rect?, b: android.graphics.Rect?): Float {
        if (a == null || b == null) return Float.MAX_VALUE
        val dx = (a.centerX() - b.centerX()).toFloat()
        val dy = (a.centerY() - b.centerY()).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun stabilizeRect(current: Rect?, previous: Rect?): Rect? {
        if (current == null) return null
        if (previous == null || previous.isEmpty) return Rect(current)

        val centerDx = kotlin.math.abs(current.centerX() - previous.centerX()).toFloat()
        val centerDy = kotlin.math.abs(current.centerY() - previous.centerY()).toFloat()
        val widthDiff = kotlin.math.abs(current.width() - previous.width()).toFloat()
        val heightDiff = kotlin.math.abs(current.height() - previous.height()).toFloat()

        if (centerDx <= JITTER_FREEZE_PX &&
            centerDy <= JITTER_FREEZE_PX &&
            widthDiff <= JITTER_SIZE_FREEZE_PX &&
            heightDiff <= JITTER_SIZE_FREEZE_PX
        ) {
            return Rect(previous)
        }

        if (centerDx <= JITTER_BLEND_DISTANCE_PX && centerDy <= JITTER_BLEND_DISTANCE_PX) {
            return Rect(
                lerp(previous.left, current.left, JITTER_NEW_WEIGHT),
                lerp(previous.top, current.top, JITTER_NEW_WEIGHT),
                lerp(previous.right, current.right, JITTER_NEW_WEIGHT),
                lerp(previous.bottom, current.bottom, JITTER_NEW_WEIGHT)
            )
        }

        return Rect(current)
    }

    private fun lerp(oldValue: Int, newValue: Int, newWeight: Float): Int {
        val oldWeight = 1f - newWeight
        return (oldValue * oldWeight + newValue * newWeight).toInt()
    }

}

data class OCRStats(
    val lastFrameMs: Long = 0,
    val avgFrameMs: Long = 0,
    val framesProcessed: Int = 0,
    val linesDetected: Int = 0
)
