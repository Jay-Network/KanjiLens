package com.jworks.kanjilens.ui.camera

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.jworks.kanjilens.R
import com.jworks.kanjilens.domain.ai.ScopeLevel
import com.jworks.kanjilens.domain.models.KanjiSegment
import com.jworks.kanjilens.ui.dictionary.DictionaryDetailView
import com.jworks.kanjilens.ui.dictionary.KanjiDetailView
import com.jworks.kanjilens.ui.theme.KanjiLensColors

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Data class for jukugo with reading
private data class JukugoEntry(val text: String, val reading: String)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onSettingsClick: () -> Unit,
    onRewardsClick: () -> Unit = {},
    onPaywallNeeded: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Prevent Android back button from exiting app on camera screen
    BackHandler { /* consume back press — camera is the main screen */ }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(viewModel, onSettingsClick, onRewardsClick, onPaywallNeeded, onProfileClick, onFeedbackClick, onBookmarksClick)
    } else {
        CameraPermissionRequest(
            showRationale = cameraPermissionState.status.shouldShowRationale,
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )
    }
}

@Composable
private fun CameraContent(
    viewModel: CameraViewModel,
    onSettingsClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onPaywallNeeded: () -> Unit,
    onProfileClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onBookmarksClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val detectedTexts by viewModel.detectedTexts.collectAsState()
    val sourceImageSize by viewModel.sourceImageSize.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isEnhanced by viewModel.isEnhanced.collectAsState()
    val ocrStats by viewModel.ocrStats.collectAsState()
    val visibleRegion by viewModel.visibleRegion.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanTimerSeconds by viewModel.scanTimerSeconds.collectAsState()
    val isScanActive by viewModel.isScanActive.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Auto-start scan when entering camera
    LaunchedEffect(Unit) {
        // Avoid immediate paywall loop when free scans are exhausted.
        viewModel.startScan(context, allowPaywall = false)
        // Auto-start a scan challenge if none active
        if (viewModel.scanChallenge.value == null) {
            viewModel.startNewChallenge()
        }
    }

    // Navigate to paywall when triggered
    LaunchedEffect(showPaywall) {
        if (showPaywall) {
            viewModel.dismissPaywall()
            onPaywallNeeded()
        }
    }

    // Re-check premium status when returning (e.g. from paywall purchase)
    LaunchedEffect(isPremium) {
        if (isPremium && !isScanActive) {
            viewModel.startScan(context)
        }
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var frozenBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Capture/release frozen preview when pause state changes
    LaunchedEffect(isPaused) {
        if (isPaused) {
            previewViewRef?.bitmap?.let { bmp ->
                frozenBitmap = bmp.asImageBitmap()
            }
        } else {
            frozenBitmap = null
        }
    }

    // Boundary animation: smooth transition between 0.25 (partial) and 1.0 (full screen)
    val boundaryAnim = remember { Animatable(1.0f) }
    val displayBoundary = boundaryAnim.value

    // Sync with saved setting on first load only (not on every settings change)
    LaunchedEffect(Unit) {
        boundaryAnim.snapTo(settings.partialModeBoundaryRatio)
    }

    // Draggable button positions (in px)
    var settingsBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var flashBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var modeBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var verticalBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var pauseBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var profileBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var feedbackBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var jcoinBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var bookmarkBtnOffset by remember { mutableStateOf(Offset.Zero) }
    var buttonsInitialized by remember { mutableStateOf(false) }
    var lastLayoutWidth by remember { mutableFloatStateOf(0f) }
    var lastLayoutHeight by remember { mutableFloatStateOf(0f) }

    // Bottom pad in vertical partial mode (ratio of screen height: 0.5 = pad covers bottom half)
    val verticalPadTopRatio = PartialModeConstants.VERT_PAD_TOP_RATIO

    // Auto-toggle vertical/horizontal mode on orientation change
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var lastOrientation by remember { mutableStateOf(configuration.orientation) }
    LaunchedEffect(configuration.orientation) {
        if (configuration.orientation != lastOrientation) {
            val wasLandscape = lastOrientation == Configuration.ORIENTATION_LANDSCAPE
            val nowLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (wasLandscape != nowLandscape) {
                // Swap mode: portrait vertical ↔ landscape horizontal
                val newVertical = !settings.verticalTextMode
                if (settings.partialModeBoundaryRatio < CameraDimens.FULL_MODE_THRESHOLD) {
                    val newRatio = if (newVertical) CameraDimens.VERTICAL_PARTIAL_RATIO else CameraDimens.HORIZONTAL_PARTIAL_RATIO
                    viewModel.updateVerticalModeAndBoundary(newVertical, newRatio)
                    boundaryAnim.snapTo(newRatio)
                } else {
                    viewModel.updateVerticalTextMode(newVertical)
                }
            }
            lastOrientation = configuration.orientation
            // Reset button positions for new screen dimensions
            buttonsInitialized = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        }
    }

    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val statusBarPx = with(density) { CameraDimens.STATUS_BAR_ESTIMATE_DP.dp.toPx() }
        val btnSizePx = with(density) { CameraDimens.BUTTON_SIZE.toPx() }

        // Report canvas size to ViewModel
        LaunchedEffect(maxWidthPx, maxHeightPx) {
            viewModel.updateCanvasSize(android.util.Size(maxWidthPx.toInt(), maxHeightPx.toInt()))
        }

        // Fixed split ratios: camera portion of screen
        val isPartial = displayBoundary < CameraDimens.FULL_MODE_THRESHOLD
        val vertCameraRatio = PartialModeConstants.VERT_CAMERA_WIDTH_RATIO
        val horizCameraRatio = PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO
        val isVerticalPartial = isPartial && settings.verticalTextMode
        val isHorizontalPartial = isPartial && !settings.verticalTextMode
        val leftMarginDp = CameraDimens.LEFT_MARGIN
        val topMargin = statusBarPx + CameraDimens.TOP_MARGIN_EXTRA_PX
        val rightMargin = if (isLandscape) with(density) { CameraDimens.RIGHT_MARGIN_LANDSCAPE_DP.dp.toPx() } else CameraDimens.RIGHT_MARGIN_PORTRAIT_PX
        val bottomPadding = if (isLandscape) CameraDimens.BOTTOM_PADDING_LANDSCAPE_PX else with(density) { CameraDimens.BOTTOM_PADDING_PORTRAIT_DP.dp.toPx() }

        // Reset button grid when screen dimensions change (e.g. rotation)
        if (maxWidthPx != lastLayoutWidth || maxHeightPx != lastLayoutHeight) {
            buttonsInitialized = false
            lastLayoutWidth = maxWidthPx
            lastLayoutHeight = maxHeightPx
        }

        if (!buttonsInitialized) {
            val offsets = calculateButtonGrid(maxWidthPx, maxHeightPx, btnSizePx, rightMargin, bottomPadding, topMargin)
            modeBtnOffset = offsets.mode
            verticalBtnOffset = offsets.vertical
            pauseBtnOffset = offsets.pause
            flashBtnOffset = offsets.flash
            settingsBtnOffset = offsets.settings
            profileBtnOffset = offsets.profile
            bookmarkBtnOffset = offsets.bookmark
            jcoinBtnOffset = offsets.jcoin
            feedbackBtnOffset = offsets.feedback
            buttonsInitialized = true
        }

        val boundaryYDp = with(density) { (maxHeightPx * displayBoundary).toDp() }

        // Layer 1: Full-screen camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewViewRef = it }.apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val cam = camera ?: return@setOnTouchListener true
                            val factory = meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cam.cameraControl.startFocusAndMetering(action)
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
                                    viewModel.processFrame(imageProxy)
                                }
                            }

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 1b: Frozen preview snapshot when paused
        frozenBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Paused camera",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: Text overlay (full screen to match camera coordinates)
        if (detectedTexts.isNotEmpty()) {
            TextOverlay(
                detectedTexts = detectedTexts,
                imageWidth = sourceImageSize.width,
                imageHeight = sourceImageSize.height,
                rotationDegrees = rotationDegrees,
                settings = settings,
                isVerticalMode = settings.verticalTextMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Shared jukugo/kanji accumulation state — used by both focus-mode panel and full-mode-paused panel
        val modeKey = Pair(settings.verticalTextMode, displayBoundary)
        var jukugoList by remember(modeKey) { mutableStateOf<List<JukugoEntry>>(emptyList()) }
        var jukugoAccumulator by remember(modeKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
        var kanjiList by remember(modeKey) { mutableStateOf<List<JukugoEntry>>(emptyList()) }
        var kanjiAccumulator by remember(modeKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
        var selectedJukugo by remember(modeKey) { mutableStateOf<JukugoEntry?>(null) }
        var selectedKanji by remember(modeKey) { mutableStateOf<String?>(null) }

        // Load all bookmarked kanji for highlighting in jukugo list
        LaunchedEffect(modeKey) { viewModel.refreshAllBookmarkedKanji() }

        // Accumulate jukugo from current frame (runs in both focus and full-paused modes)
        LaunchedEffect(detectedTexts, modeKey) {
            val imgSize = sourceImageSize
            val imgW = imgSize.width.toFloat()
            val imgH = imgSize.height.toFloat()
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val effW = if (isRotated) imgH else imgW
            val effH = if (isRotated) imgW else imgH
            val scale = if (effW > 0 && effH > 0) maxOf(maxWidthPx / effW, maxHeightPx / effH) else 1f
            val cropOffsetX = (effW * scale - maxWidthPx) / 2f
            val cropOffsetY = (effH * scale - maxHeightPx) / 2f

            val screenYBoundary = if (settings.verticalTextMode) {
                maxHeightPx * PartialModeConstants.VERT_PAD_TOP_RATIO
            } else {
                maxHeightPx * PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO
            }

            val screenXBoundary = if (settings.verticalTextMode) {
                maxWidthPx * (1f - PartialModeConstants.VERT_CAMERA_WIDTH_RATIO)
            } else {
                0f
            }

            fun isSegmentVisible(segment: KanjiSegment, bounds: Rect?, elemTextLen: Int): Boolean {
                // In full mode (not partial), all segments are visible
                if (!isPartial) return true
                if (bounds == null || effW <= 0 || elemTextLen <= 0) return true
                if (settings.verticalTextMode) {
                    val charHeight = bounds.height().toFloat() / elemTextLen
                    val segTopImg = bounds.top + segment.startIndex * charHeight
                    val segBottomImg = bounds.top + segment.endIndex * charHeight
                    val segTopScreen = segTopImg * scale - cropOffsetY
                    val segBottomScreen = segBottomImg * scale - cropOffsetY
                    val yOk = segBottomScreen <= screenYBoundary && segTopScreen >= 0
                    val elemCenterXScreen = bounds.centerX() * scale - cropOffsetX
                    val xOk = elemCenterXScreen >= screenXBoundary
                    return yOk && xOk
                } else {
                    val charWidth = bounds.width().toFloat() / elemTextLen
                    val segLeftImg = bounds.left + segment.startIndex * charWidth
                    val segRightImg = bounds.left + segment.endIndex * charWidth
                    val segLeftScreen = segLeftImg * scale - cropOffsetX
                    val segRightScreen = segRightImg * scale - cropOffsetX
                    val elemBottomScreen = bounds.bottom * scale - cropOffsetY
                    val yOk = elemBottomScreen <= screenYBoundary
                    val edgeMargin = charWidth * scale * 0.5f
                    val notCutoff = segLeftScreen >= -edgeMargin &&
                            segRightScreen <= maxWidthPx + edgeMargin
                    return yOk && notCutoff
                }
            }

            val newJukugo = mutableMapOf<String, String>()
            val newKanji = mutableMapOf<String, String>()

            detectedTexts.forEach { detected ->
                detected.elements.forEach { element ->
                    val bounds = element.bounds
                    val elemTextLen = element.text.length
                    element.kanjiSegments
                        .filter { isSegmentVisible(it, bounds, elemTextLen) }
                        .forEach { segment ->
                            if (segment.text.length == 1) {
                                newKanji[segment.text] = segment.reading
                            } else {
                                newJukugo[segment.text] = segment.reading
                            }
                        }
                }
            }

            jukugoAccumulator = jukugoAccumulator + newJukugo
            kanjiAccumulator = kanjiAccumulator + newKanji
        }

        // Refresh list every 1 second while live, or once when AI enhancement completes
        LaunchedEffect(modeKey, isPaused, isEnhanced) {
            if (isPaused && !isEnhanced) return@LaunchedEffect
            jukugoList = jukugoAccumulator.map { (text, reading) ->
                JukugoEntry(text, reading)
            }.sortedBy { it.text }
            jukugoAccumulator = emptyMap()
            kanjiList = kanjiAccumulator.map { (text, reading) ->
                JukugoEntry(text, reading)
            }.sortedBy { it.text }
            kanjiAccumulator = emptyMap()
            if (isPaused) return@LaunchedEffect
            while (true) {
                kotlinx.coroutines.delay(1000)
                jukugoList = jukugoAccumulator.map { (text, reading) ->
                    JukugoEntry(text, reading)
                }.sortedBy { it.text }
                jukugoAccumulator = emptyMap()
                kanjiList = kanjiAccumulator.map { (text, reading) ->
                    JukugoEntry(text, reading)
                }.sortedBy { it.text }
                kanjiAccumulator = emptyMap()
            }
        }

        // Layer 3: Panel area with jukugo list
        // Vertical mode: LEFT 75% panel, RIGHT 25% camera
        // Horizontal mode: TOP 25% camera, BOTTOM 75% panel
        if (isPartial) {
            val panelModifier = if (settings.verticalTextMode) {
                // Vertical mode: panel on the LEFT (60% width)
                val panelWidthDp = with(density) { (maxWidthPx * (1f - vertCameraRatio)).toDp() }
                Modifier
                    .width(panelWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(KanjiLensColors.PanelBackground)
                    .border(width = 2.dp, color = KanjiLensColors.PanelBorder)
                    .statusBarsPadding()
            } else {
                // Horizontal mode: panel on the BOTTOM (75% height)
                val whiteHeightDp = with(density) { (maxHeightPx * (1f - horizCameraRatio)).toDp() }
                Modifier
                    .fillMaxWidth()
                    .height(whiteHeightDp)
                    .align(Alignment.BottomCenter)
                    .background(KanjiLensColors.PanelBackground)
                    .border(width = 2.dp, color = KanjiLensColors.PanelBorder)
            }

            // Dictionary state
            val dictionaryResult by viewModel.dictionaryResult.collectAsState()
            val isDictionaryLoading by viewModel.isDictionaryLoading.collectAsState()
            val bookmarkedKanji by viewModel.bookmarkedKanji.collectAsState()
            val isWordBookmarked by viewModel.isWordBookmarked.collectAsState()
            val allBookmarkedKanji by viewModel.allBookmarkedKanji.collectAsState()
            val kanjiInfo by viewModel.kanjiInfo.collectAsState()
            val isKanjiInfoLoading by viewModel.isKanjiInfoLoading.collectAsState()

            // Trigger lookup when a jukugo is selected
            LaunchedEffect(selectedJukugo) {
                selectedJukugo?.let { viewModel.lookupWord(it.text, context) }
            }

            // Trigger kanji info lookup when a kanji is selected
            LaunchedEffect(selectedKanji) {
                if (selectedKanji != null) {
                    viewModel.loadKanjiInfo(selectedKanji!!)
                } else {
                    viewModel.clearKanjiInfo()
                }
            }

            Box(modifier = panelModifier) {
                if (selectedKanji != null) {
                    // Kanji detail page — matches KanjiQuest design
                    val kanjiStr = selectedKanji!!
                    val isKanjiBM = kanjiStr in bookmarkedKanji
                    KanjiDetailView(
                        kanji = kanjiStr,
                        kanjiInfo = kanjiInfo,
                        isLoading = isKanjiInfoLoading,
                        isBookmarked = isKanjiBM,
                        onBackClick = { selectedKanji = null },
                        onBookmarkToggle = {
                            viewModel.toggleKanjiBookmark(kanjiStr)
                            viewModel.refreshAllBookmarkedKanji()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (selectedJukugo != null) {
                    DictionaryDetailView(
                        result = dictionaryResult,
                        isLoading = isDictionaryLoading,
                        onBackClick = {
                            selectedJukugo = null
                            viewModel.clearDictionaryResult()
                        },
                        wordText = selectedJukugo?.text ?: "",
                        wordReading = selectedJukugo?.reading ?: "",
                        isWordBookmarked = isWordBookmarked,
                        onWordBookmarkToggle = {
                            val entry = selectedJukugo ?: return@DictionaryDetailView
                            viewModel.toggleWordBookmark(entry.text, entry.reading)
                        },
                        bookmarkedKanji = bookmarkedKanji,
                        onKanjiClick = { kanji ->
                            selectedKanji = kanji
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DetectedJukugoList(
                        jukugo = jukugoList,
                        singleKanji = kanjiList,
                        onJukugoClick = { entry ->
                            selectedJukugo = entry
                        },
                        onKanjiClick = { entry ->
                            selectedKanji = entry.text
                        },
                        onBackToCamera = {
                            // Collapse panel → full camera mode
                            scope.launch {
                                boundaryAnim.animateTo(1f, spring(dampingRatio = CameraDimens.SPRING_DAMPING, stiffness = CameraDimens.SPRING_STIFFNESS))
                            }
                            viewModel.updatePartialModeBoundaryRatio(1f)
                        },
                        onAiAnalyze = { viewModel.analyzeFullText() },
                        isAiAvailable = viewModel.isAiAnalysisAvailable,
                        aiAnalysisState = aiAnalysisState,
                        onDismissAi = { viewModel.dismissAiAnalysis() },
                        bookmarkedKanji = allBookmarkedKanji,
                        onShare = {
                            val shareText = (jukugoList + kanjiList).joinToString("\n") { "${it.text}（${it.reading}）" }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "KanjiLens detected:\n$shareText")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share detected words"))
                            viewModel.awardShareCoin(context)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Layer 3a: Full-mode paused panel (draggable bottom sheet with detected words + AI)
        if (isPaused && !isPartial) {
            // Dictionary state for full-mode panel
            val dictionaryResult by viewModel.dictionaryResult.collectAsState()
            val isDictionaryLoading by viewModel.isDictionaryLoading.collectAsState()
            val bookmarkedKanji by viewModel.bookmarkedKanji.collectAsState()
            val isWordBookmarked by viewModel.isWordBookmarked.collectAsState()
            val allBookmarkedKanji by viewModel.allBookmarkedKanji.collectAsState()
            val kanjiInfo by viewModel.kanjiInfo.collectAsState()
            val isKanjiInfoLoading by viewModel.isKanjiInfoLoading.collectAsState()

            LaunchedEffect(selectedJukugo) {
                selectedJukugo?.let { viewModel.lookupWord(it.text, context) }
            }
            LaunchedEffect(selectedKanji) {
                if (selectedKanji != null) viewModel.loadKanjiInfo(selectedKanji!!)
                else viewModel.clearKanjiInfo()
            }

            val hasContent = jukugoList.isNotEmpty() || kanjiList.isNotEmpty() || aiAnalysisState !is AiPanelState.Idle

            DraggablePanel(hasContent = hasContent) {
                // AI analysis states take over the panel
                when (val aiState = aiAnalysisState) {
                    is AiPanelState.Loading -> {
                        val allText = detectedTexts.joinToString("\n") { detected ->
                            detected.elements.joinToString("") { it.text }
                        }
                        AiLoadingPanel(
                            selectedText = allText,
                            scopeLevel = ScopeLevel.FullSnapshot,
                            onDismiss = { viewModel.dismissAiAnalysis() }
                        )
                    }
                    is AiPanelState.Result -> {
                        AiAnalysisPanel(
                            selectedText = aiState.detectedText,
                            scopeLevel = ScopeLevel.FullSnapshot,
                            response = aiState.response,
                            onDismiss = { viewModel.dismissAiAnalysis() }
                        )
                    }
                    is AiPanelState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Analysis failed: ${aiState.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.dismissAiAnalysis() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                    is AiPanelState.Idle -> {
                        // Show detected words list (same as focus mode panel)
                        if (selectedKanji != null) {
                            val kanjiStr = selectedKanji!!
                            val isKanjiBM = kanjiStr in bookmarkedKanji
                            KanjiDetailView(
                                kanji = kanjiStr,
                                kanjiInfo = kanjiInfo,
                                isLoading = isKanjiInfoLoading,
                                isBookmarked = isKanjiBM,
                                onBackClick = { selectedKanji = null },
                                onBookmarkToggle = {
                                    viewModel.toggleKanjiBookmark(kanjiStr)
                                    viewModel.refreshAllBookmarkedKanji()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (selectedJukugo != null) {
                            DictionaryDetailView(
                                result = dictionaryResult,
                                isLoading = isDictionaryLoading,
                                onBackClick = {
                                    selectedJukugo = null
                                    viewModel.clearDictionaryResult()
                                },
                                wordText = selectedJukugo?.text ?: "",
                                wordReading = selectedJukugo?.reading ?: "",
                                isWordBookmarked = isWordBookmarked,
                                onWordBookmarkToggle = {
                                    val entry = selectedJukugo ?: return@DictionaryDetailView
                                    viewModel.toggleWordBookmark(entry.text, entry.reading)
                                },
                                bookmarkedKanji = bookmarkedKanji,
                                onKanjiClick = { kanji -> selectedKanji = kanji },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            DetectedJukugoList(
                                jukugo = jukugoList,
                                singleKanji = kanjiList,
                                onJukugoClick = { entry -> selectedJukugo = entry },
                                onKanjiClick = { entry -> selectedKanji = entry.text },
                                onBackToCamera = { viewModel.togglePause() },
                                onAiAnalyze = { viewModel.analyzeFullText() },
                                isAiAvailable = viewModel.isAiAnalysisAvailable,
                                aiAnalysisState = aiAnalysisState,
                                onDismissAi = { viewModel.dismissAiAnalysis() },
                                bookmarkedKanji = allBookmarkedKanji,
                                onShare = {
                                    val shareText = (jukugoList + kanjiList).joinToString("\n") { "${it.text}（${it.reading}）" }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "KanjiLens detected:\n$shareText")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share detected words"))
                                    viewModel.awardShareCoin(context)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Layer 3b: Draggable bottom pad in vertical partial mode
        if (isVerticalPartial) {
            val panelWidthPx = maxWidthPx * (1f - vertCameraRatio)
            val padTopPx = maxHeightPx * verticalPadTopRatio
            val padHeightDp = with(density) { (maxHeightPx - padTopPx).toDp() }
            val panelWidthDp = with(density) { panelWidthPx.toDp() }

            // Pad area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(padHeightDp)
                    .align(Alignment.BottomEnd)
                    .padding(start = panelWidthDp)
                    .background(KanjiLensColors.PanelBackground)
                    .border(width = 1.dp, color = KanjiLensColors.PanelBorder)
            )

        }

        // Layer 4: Processing indicator
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(CameraDimens.PROCESSING_INDICATOR_PADDING)
                    .size(CameraDimens.PROCESSING_INDICATOR_SIZE),
                color = KanjiLensColors.ProcessingGreen,
                strokeWidth = CameraDimens.PROCESSING_INDICATOR_STROKE_WIDTH.dp
            )
        }

        // Layer 5: Debug HUD (in camera area)
        if (settings.showDebugHud && ocrStats.framesProcessed > 0) {
            val hudModifier = if (isVerticalPartial) {
                // Vertical partial: HUD in top-right camera area
                val hudX = with(density) { (maxWidthPx * (1f - vertCameraRatio) + 12.dp.toPx()).toDp() }
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = hudX)
                    .padding(top = 12.dp)
            } else {
                val hudY = with(density) { (maxHeightPx * displayBoundary - 80.dp.toPx()).coerceAtLeast(0f).toDp() }
                Modifier
                    .align(Alignment.TopStart)
                    .offset(y = hudY)
                    .padding(start = 12.dp)
            }
            DebugStatsHud(
                stats = ocrStats,
                modifier = hudModifier
            )
        }

        // Layer 6: Settings button (middle row, center)
        DraggableFloatingButton(
            offset = settingsBtnOffset,
            onOffsetChange = { settingsBtnOffset = it },
            onClick = onSettingsClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) { SettingsButtonContent() }

        // Layer 7: Flash button (middle row, left)
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                DraggableFloatingButton(
                    offset = flashBtnOffset,
                    onOffsetChange = { flashBtnOffset = it },
                    onClick = { viewModel.toggleFlash() },
                    maxWidth = maxWidthPx,
                    maxHeight = maxHeightPx,
                    btnSize = btnSizePx
                ) { FlashButtonContent(isFlashOn) }
            }
        }

        // Layer 8: Mode toggle button (top row, left)
        DraggableFloatingButton(
            offset = modeBtnOffset,
            onOffsetChange = { modeBtnOffset = it },
            onClick = {
                val currentMode = settings.partialModeBoundaryRatio
                val partialTarget = if (settings.verticalTextMode) CameraDimens.VERTICAL_PARTIAL_RATIO else CameraDimens.HORIZONTAL_PARTIAL_RATIO
                val newMode = if (currentMode > CameraDimens.MODE_TOGGLE_THRESHOLD) partialTarget else 1f

                scope.launch {
                    boundaryAnim.animateTo(
                        newMode,
                        spring(dampingRatio = CameraDimens.SPRING_DAMPING, stiffness = CameraDimens.SPRING_STIFFNESS)
                    )
                }
                viewModel.updatePartialModeBoundaryRatio(newMode)
            },
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) { ModeButtonContent(isFullMode = settings.partialModeBoundaryRatio > CameraDimens.MODE_TOGGLE_THRESHOLD) }

        // Layer 9: Vertical/Horizontal toggle (top row, center)
        DraggableFloatingButton(
            offset = verticalBtnOffset,
            onOffsetChange = { verticalBtnOffset = it },
            onClick = {
                val goingVertical = !settings.verticalTextMode
                if (settings.partialModeBoundaryRatio < CameraDimens.FULL_MODE_THRESHOLD) {
                    val newRatio = if (goingVertical) CameraDimens.VERTICAL_PARTIAL_RATIO else CameraDimens.HORIZONTAL_PARTIAL_RATIO
                    scope.launch {
                        boundaryAnim.animateTo(
                            newRatio,
                            spring(dampingRatio = CameraDimens.SPRING_DAMPING, stiffness = CameraDimens.SPRING_STIFFNESS)
                        )
                    }
                    viewModel.updateVerticalModeAndBoundary(goingVertical, newRatio)
                } else {
                    viewModel.updateVerticalTextMode(goingVertical)
                }
            },
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) { VerticalModeButtonContent(settings.verticalTextMode) }

        // Layer 10: Pause/Play toggle (top row, right)
        DraggableFloatingButton(
            offset = pauseBtnOffset,
            onOffsetChange = { pauseBtnOffset = it },
            onClick = { viewModel.togglePause() },
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) { PauseButtonContent(isPaused) }

        // Version label (bottom-left)
        Text(
            text = "v${com.jworks.kanjilens.BuildConfig.VERSION_NAME}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = leftMarginDp, bottom = CameraDimens.VERSION_LABEL_BOTTOM_PADDING)
        )

        // Profile button (middle row, right)
        DraggableFloatingButton(
            offset = profileBtnOffset,
            onOffsetChange = { profileBtnOffset = it },
            onClick = onProfileClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) { ProfileButtonContent() }

        // J Coin rewards button (bottom row, center) with balance badge
        Box {
            DraggableFloatingButton(
                offset = jcoinBtnOffset,
                onOffsetChange = { jcoinBtnOffset = it },
                onClick = onRewardsClick,
                maxWidth = maxWidthPx,
                maxHeight = maxHeightPx,
                btnSize = btnSizePx,
                bgColor = KanjiLensColors.JCoinButtonBg.copy(alpha = 0.85f)
            ) { JCoinButtonContent() }

            val jCoinBalance by viewModel.jCoinBalance.collectAsState()
            if (jCoinBalance > 0) {
                val displayBalance = if (jCoinBalance >= 1000) "${jCoinBalance / 1000}K" else "$jCoinBalance"
                Text(
                    text = displayBalance,
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(KanjiLensColors.PrimaryAction, RoundedCornerShape(6.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }

        // Feedback button (bottom row, right)
        DraggableFloatingButton(
            offset = feedbackBtnOffset,
            onOffsetChange = { feedbackBtnOffset = it },
            onClick = onFeedbackClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx,
            bgColor = KanjiLensColors.FeedbackButtonBg.copy(alpha = 0.85f)
        ) { FeedbackButtonContent() }

        // Bookmark button (bottom row, left)
        DraggableFloatingButton(
            offset = bookmarkBtnOffset,
            onOffsetChange = { bookmarkBtnOffset = it },
            onClick = onBookmarksClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx,
            bgColor = KanjiLensColors.BookmarkButtonBg.copy(alpha = 0.85f)
        ) { BookmarkButtonContent() }


        // Layer 11: Scan timer pill (free users only)
        if (!isPremium && isScanActive) {
            ScanTimerPill(
                scanTimerSeconds = scanTimerSeconds,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            )
        }

        // Layer 12: Scan Challenge pill (bottom-right, above button grid)
        val scanChallenge by viewModel.scanChallenge.collectAsState()
        scanChallenge?.let { challenge ->
            // Check if target kanji is in current detections
            LaunchedEffect(detectedTexts) {
                viewModel.checkChallengeInDetections(context)
            }

            // Position: right-aligned with button grid, just above top row
            val challengeEndPad = with(density) { (rightMargin).toDp() }
            val challengeBottomPad = with(density) { (maxHeightPx - modeBtnOffset.y + CameraDimens.TOP_MARGIN_EXTRA_PX).toDp() }.coerceAtLeast(0.dp)
            ScanChallengePill(
                challenge = challenge,
                onNextChallenge = { viewModel.startNewChallenge() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = challengeEndPad, bottom = challengeBottomPad)
            )
        }

        // Layer 13: J Coin reward toast
        val coinToast by viewModel.coinRewardToast.collectAsState()
        coinToast?.let { message ->
            CoinRewardToast(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = CameraDimens.COIN_TOAST_BOTTOM_PADDING)
            )
        }

        // Layer 14: Scan-expired overlay (free users, timer hit 0)
        if (!isPremium && !isScanActive && isPaused) {
            ScanExpiredOverlay(
                onStartNewScan = { viewModel.startScan(context) },
                onBackToCamera = { viewModel.dismissScanOverlay() },
                onUpgrade = onPaywallNeeded
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectedJukugoList(
    jukugo: List<JukugoEntry>,
    singleKanji: List<JukugoEntry> = emptyList(),
    onJukugoClick: (JukugoEntry) -> Unit,
    onKanjiClick: (JukugoEntry) -> Unit = {},
    onBackToCamera: () -> Unit,
    onAiAnalyze: () -> Unit = {},
    isAiAvailable: Boolean = false,
    aiAnalysisState: AiPanelState = AiPanelState.Idle,
    onDismissAi: () -> Unit = {},
    bookmarkedKanji: Set<String> = emptySet(),
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // If AI analysis is active, show AI panel instead of word list
    when (val aiState = aiAnalysisState) {
        is AiPanelState.Loading -> {
            val allText = (jukugo + singleKanji).joinToString(", ") { "${it.text} (${it.reading})" }
            AiLoadingPanel(
                selectedText = allText,
                scopeLevel = ScopeLevel.FullSnapshot,
                onDismiss = onDismissAi,
                modifier = modifier
            )
            return
        }
        is AiPanelState.Result -> {
            AiAnalysisPanel(
                selectedText = aiState.detectedText,
                scopeLevel = ScopeLevel.FullSnapshot,
                response = aiState.response,
                onDismiss = onDismissAi,
                modifier = modifier
            )
            return
        }
        is AiPanelState.Error -> {
            Column(
                modifier = modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Analysis failed: ${aiState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismissAi) { Text("Dismiss") }
            }
            return
        }
        is AiPanelState.Idle -> { /* show normal word list below */ }
    }

    val totalCount = singleKanji.size + jukugo.size
    Column(modifier = modifier) {
        // Header with close button and AI Analyze button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KanjiLensColors.PanelBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Back to camera",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackToCamera() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Detected Words ($totalCount)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (totalCount > 0) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = "Share detected words",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onShare() },
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isAiAvailable && totalCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF7C4DFF).copy(alpha = 0.9f))
                        .clickable { onAiAnalyze() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "AI Analyze",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (totalCount == 0) {
                Text(
                    text = "Point your camera at Japanese text",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KanjiLensColors.JukugoSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detected words will appear here",
                    style = MaterialTheme.typography.bodySmall,
                    color = KanjiLensColors.JukugoSecondary.copy(alpha = 0.7f)
                )
            } else {
                // Single kanji section
                if (singleKanji.isNotEmpty()) {
                    Text(
                        text = "漢字 (${singleKanji.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = KanjiLensColors.JukugoSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    // Horizontal wrap layout for single kanji chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        singleKanji.forEach { entry ->
                            val isBookmarked = entry.text in bookmarkedKanji
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KanjiLensColors.PanelItemBackground)
                                    .clickable { onKanjiClick(entry) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = entry.text,
                                    fontSize = 22.sp,
                                    fontWeight = if (isBookmarked) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isBookmarked) KanjiLensColors.BookmarkedKanjiHighlight else KanjiLensColors.JukugoText
                                )
                                Text(
                                    text = entry.reading,
                                    fontSize = 11.sp,
                                    color = KanjiLensColors.JukugoSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Jukugo section
                if (jukugo.isNotEmpty()) {
                    Text(
                        text = "熟語 (${jukugo.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = KanjiLensColors.JukugoSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        jukugo.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KanjiLensColors.PanelItemBackground)
                                    .clickable { onJukugoClick(entry) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        entry.text.forEach { ch ->
                                            val isKanjiBM = ch.toString() in bookmarkedKanji
                                            if (isKanjiBM) {
                                                withStyle(SpanStyle(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = KanjiLensColors.BookmarkedKanjiHighlight
                                                )) { append(ch) }
                                            } else {
                                                append(ch)
                                            }
                                        }
                                    },
                                    fontSize = 18.sp,
                                    color = KanjiLensColors.JukugoText,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = " - ",
                                    fontSize = 14.sp,
                                    color = KanjiLensColors.JukugoSecondary
                                )
                                Text(
                                    text = entry.reading,
                                    fontSize = 14.sp,
                                    color = KanjiLensColors.JukugoSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Divider between entries (except after last)
                            if (index < jukugo.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(KanjiLensColors.PanelBorder.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }

                // Hint text at bottom
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap a word to look it up",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


@Composable
private fun DebugStatsHud(stats: OCRStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(8.dp)
    ) {
        Text(
            text = "OCR: ${stats.avgFrameMs}ms avg",
            color = when {
                stats.avgFrameMs < 200 -> KanjiLensColors.HudFast
                stats.avgFrameMs < 400 -> KanjiLensColors.HudMedium
                else -> KanjiLensColors.HudSlow
            },
            fontSize = 11.sp
        )
        Text(
            text = "Lines: ${stats.linesDetected} | #${stats.framesProcessed}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CameraPermissionRequest(
    showRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (showRationale) {
                "KanjiLens uses your camera to detect Japanese text in real time. No photos are saved or uploaded."
            } else {
                "To read Japanese text around you, KanjiLens needs access to your camera."
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Allow Camera Access")
        }
    }
}
