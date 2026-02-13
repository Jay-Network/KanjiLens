package com.jworks.kanjilens.ui.camera

import android.Manifest
import android.graphics.Bitmap
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.jworks.kanjilens.ui.dictionary.DictionaryDetailView

import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Prevent Android back button from exiting app on camera screen
    BackHandler { /* consume back press — camera is the main screen */ }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(viewModel, onSettingsClick, onRewardsClick, onPaywallNeeded, onProfileClick, onFeedbackClick)
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
    onFeedbackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val detectedTexts by viewModel.detectedTexts.collectAsState()
    val sourceImageSize by viewModel.sourceImageSize.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val ocrStats by viewModel.ocrStats.collectAsState()
    val visibleRegion by viewModel.visibleRegion.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanTimerSeconds by viewModel.scanTimerSeconds.collectAsState()
    val isScanActive by viewModel.isScanActive.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Auto-start scan when entering camera
    LaunchedEffect(Unit) {
        // Avoid immediate paywall loop when free scans are exhausted.
        viewModel.startScan(context, allowPaywall = false)
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
    var buttonsInitialized by remember { mutableStateOf(false) }

    // Bottom pad in vertical partial mode (ratio of screen height: 0.5 = pad covers bottom half)
    val verticalPadTopRatio = PartialModeConstants.VERT_PAD_TOP_RATIO

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
        val statusBarPx = with(density) { 48.dp.toPx() }
        val btnSizePx = with(density) { 44.dp.toPx() }

        // Report canvas size to ViewModel
        LaunchedEffect(maxWidthPx, maxHeightPx) {
            viewModel.updateCanvasSize(android.util.Size(maxWidthPx.toInt(), maxHeightPx.toInt()))
        }

        // Fixed split ratios: camera portion of screen
        val isPartial = displayBoundary < 0.99f
        val vertCameraRatio = PartialModeConstants.VERT_CAMERA_WIDTH_RATIO
        val horizCameraRatio = PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO
        val isVerticalPartial = isPartial && settings.verticalTextMode
        val isHorizontalPartial = isPartial && !settings.verticalTextMode
        val btnGap = 12f
        val rightMargin = 24f
        val leftMarginDp = 18.dp
        val topMargin = statusBarPx + 12f
        val sharedBottomPaddingPx = with(density) { 40.dp.toPx() }

        if (!buttonsInitialized) {
            // All 8 buttons in bottom-right: bottom row (feedback + J Coin),
            // gap, then 2x3 grid above.
            val col2 = maxWidthPx - btnSizePx - rightMargin
            val col1 = col2 - btnSizePx - btnGap
            val groupGap = 24f // gap between bottom pair and 2x3 grid

            // Bottom pair: feedback (right) + J Coin (left)
            val bottomRow = (maxHeightPx - sharedBottomPaddingPx - btnSizePx).coerceAtLeast(topMargin)
            feedbackBtnOffset = Offset(col2, bottomRow)
            jcoinBtnOffset = Offset(col1, bottomRow)

            // 2x3 grid above with gap
            val row2 = (bottomRow - btnSizePx - groupGap).coerceAtLeast(topMargin)
            val row1 = (row2 - btnSizePx - btnGap).coerceAtLeast(topMargin)
            val row0 = (row1 - btnSizePx - btnGap).coerceAtLeast(topMargin)

            pauseBtnOffset = Offset(col1, row0)
            profileBtnOffset = Offset(col2, row0)
            settingsBtnOffset = Offset(col1, row1)
            flashBtnOffset = Offset(col2, row1)
            modeBtnOffset = Offset(col1, row2)
            verticalBtnOffset = Offset(col2, row2)
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

        // Layer 3: Panel area with jukugo list
        // Vertical mode: LEFT 75% panel, RIGHT 25% camera
        // Horizontal mode: TOP 25% camera, BOTTOM 75% panel
        if (displayBoundary < 0.99f) {
            // Jukugo collection state (1-second refresh)
            // Key on verticalTextMode AND boundary ratio so state resets on any mode change
            val modeKey = Pair(settings.verticalTextMode, displayBoundary)
            var jukugoList by remember(modeKey) { mutableStateOf<List<JukugoEntry>>(emptyList()) }
            var jukugoAccumulator by remember(modeKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
            var selectedJukugo by remember(modeKey) { mutableStateOf<JukugoEntry?>(null) }

            // Accumulate jukugo from current frame (skip when paused)
            // Key on modeKey so LaunchedEffect restarts with fresh state refs after mode switch
            LaunchedEffect(detectedTexts, modeKey) {
                if (isPaused) return@LaunchedEffect

                // Compute FILL_CENTER mapping for screen-space jukugo filtering
                // (same math as OverlayCanvas — most reliable filter)
                val imgSize = sourceImageSize
                val imgW = imgSize.width.toFloat()
                val imgH = imgSize.height.toFloat()
                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val effW = if (isRotated) imgH else imgW
                val effH = if (isRotated) imgW else imgH
                val scale = if (effW > 0 && effH > 0) maxOf(maxWidthPx / effW, maxHeightPx / effH) else 1f
                val cropOffsetY = (effH * scale - maxHeightPx) / 2f

                // Screen-space Y boundary: top 25% for horizontal, top 50% for vertical
                val screenBoundary = if (settings.verticalTextMode) {
                    maxHeightPx * PartialModeConstants.VERT_PAD_TOP_RATIO
                } else {
                    maxHeightPx * PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO
                }

                val newJukugo = detectedTexts.flatMap { detected ->
                    detected.elements.flatMap { element ->
                        val bounds = element.bounds
                        element.kanjiSegments
                            .filter { segment ->
                                if (segment.text.length <= 1) return@filter false
                                if (bounds == null || effW <= 0) return@filter true
                                // Convert element center to screen-space Y
                                val screenY = bounds.centerY() * scale - cropOffsetY
                                screenY <= screenBoundary
                            }
                            .map { it.text to it.reading }
                    }
                }.toMap()

                jukugoAccumulator = jukugoAccumulator + newJukugo
            }

            // Refresh list every 1 second (skip when paused to keep list stable)
            // Key on modeKey so timer restarts with fresh state refs after mode switch
            LaunchedEffect(modeKey, isPaused) {
                if (isPaused) return@LaunchedEffect
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    jukugoList = jukugoAccumulator.map { (text, reading) ->
                        JukugoEntry(text, reading)
                    }.sortedBy { it.text }
                    jukugoAccumulator = emptyMap()
                }
            }

            val panelModifier = if (settings.verticalTextMode) {
                // Vertical mode: panel on the LEFT (60% width)
                val panelWidthDp = with(density) { (maxWidthPx * (1f - vertCameraRatio)).toDp() }
                Modifier
                    .width(panelWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(Color(0xFFF5E6D3))
                    .border(width = 2.dp, color = Color(0xFFD4B896))
            } else {
                // Horizontal mode: panel on the BOTTOM (75% height)
                val whiteHeightDp = with(density) { (maxHeightPx * (1f - horizCameraRatio)).toDp() }
                Modifier
                    .fillMaxWidth()
                    .height(whiteHeightDp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFFF5E6D3))
                    .border(width = 2.dp, color = Color(0xFFD4B896))
            }

            // Dictionary state
            val dictionaryResult by viewModel.dictionaryResult.collectAsState()
            val isDictionaryLoading by viewModel.isDictionaryLoading.collectAsState()

            // Trigger lookup when a jukugo is selected
            LaunchedEffect(selectedJukugo) {
                selectedJukugo?.let { viewModel.lookupWord(it.text) }
            }

            Box(modifier = panelModifier) {
                if (selectedJukugo == null) {
                    DetectedJukugoList(
                        jukugo = jukugoList,
                        onJukugoClick = { entry ->
                            selectedJukugo = entry
                        },
                        onBackToCamera = {
                            // Collapse panel → full camera mode
                            scope.launch {
                                boundaryAnim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
                            }
                            viewModel.updatePartialModeBoundaryRatio(1f)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DictionaryDetailView(
                        result = dictionaryResult,
                        isLoading = isDictionaryLoading,
                        onBackClick = {
                            selectedJukugo = null
                            viewModel.clearDictionaryResult()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
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
                    .background(Color(0xFFF5E6D3))
                    .border(width = 1.dp, color = Color(0xFFD4B896))
            )

        }

        // Layer 4: Processing indicator
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(24.dp),
                color = Color(0xFF4CAF50),
                strokeWidth = 2.dp
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

        // Layer 6: Draggable floating settings button
        DraggableFloatingButton(
            offset = settingsBtnOffset,
            onOffsetChange = { settingsBtnOffset = it },
            onClick = onSettingsClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Layer 7: Draggable floating flash button
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                DraggableFloatingButton(
                    offset = flashBtnOffset,
                    onOffsetChange = { flashBtnOffset = it },
                    onClick = { viewModel.toggleFlash() },
                    maxWidth = maxWidthPx,
                    maxHeight = maxHeightPx,
                    btnSize = btnSizePx
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFlashOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
                        ),
                        contentDescription = if (isFlashOn) "Flash On" else "Flash Off",
                        tint = if (isFlashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Layer 8: Mode toggle button (Full ↔ Partial)
        DraggableFloatingButton(
            offset = modeBtnOffset,
            onOffsetChange = { modeBtnOffset = it },
            onClick = {
                val currentMode = settings.partialModeBoundaryRatio
                // Horizontal partial = 0.25, vertical partial = 0.40
                val partialTarget = if (settings.verticalTextMode) 0.40f else 0.25f
                val newMode = if (currentMode > 0.6f) partialTarget else 1f

                scope.launch {
                    boundaryAnim.animateTo(
                        newMode,
                        spring(dampingRatio = 0.7f, stiffness = 300f)
                    )
                }
                viewModel.updatePartialModeBoundaryRatio(newMode)
            },
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) {
            Text(
                if (settings.partialModeBoundaryRatio > 0.6f) "FULL" else "25%",
                color = Color.White,
                fontSize = 10.sp
            )
        }

        // Layer 9: Vertical/Horizontal text mode toggle button
        DraggableFloatingButton(
            offset = verticalBtnOffset,
            onOffsetChange = { verticalBtnOffset = it },
            onClick = {
                val goingVertical = !settings.verticalTextMode
                if (settings.partialModeBoundaryRatio < 0.99f) {
                    // In partial mode: update both atomically to avoid race condition
                    val newRatio = if (goingVertical) 0.40f else 0.25f
                    scope.launch {
                        boundaryAnim.animateTo(
                            newRatio,
                            spring(dampingRatio = 0.7f, stiffness = 300f)
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
        ) {
            Text(
                if (settings.verticalTextMode) "縦" else "横",
                color = if (settings.verticalTextMode) Color.Yellow else Color.White,
                fontSize = 14.sp
            )
        }

        // Layer 10: Pause/Play toggle button
        DraggableFloatingButton(
            offset = pauseBtnOffset,
            onOffsetChange = { pauseBtnOffset = it },
            onClick = { viewModel.togglePause() },
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) {
            Text(
                if (isPaused) "▶" else "⏸",
                color = if (isPaused) Color.Yellow else Color.White,
                fontSize = 14.sp
            )
        }

        // Version label (bottom-left)
        Text(
            text = "v${com.jworks.kanjilens.BuildConfig.VERSION_NAME}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = leftMarginDp, bottom = 40.dp)
        )

        // Profile button (in 2x3 control grid)
        DraggableFloatingButton(
            offset = profileBtnOffset,
            onOffsetChange = { profileBtnOffset = it },
            onClick = onProfileClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // J Coin rewards button (bottom-right pair, left)
        DraggableFloatingButton(
            offset = jcoinBtnOffset,
            onOffsetChange = { jcoinBtnOffset = it },
            onClick = onRewardsClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx,
            bgColor = Color(0xFFFFB74D).copy(alpha = 0.85f)
        ) {
            Text(
                text = "J",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Feedback button (bottom-right pair, right)
        DraggableFloatingButton(
            offset = feedbackBtnOffset,
            onOffsetChange = { feedbackBtnOffset = it },
            onClick = onFeedbackClick,
            maxWidth = maxWidthPx,
            maxHeight = maxHeightPx,
            btnSize = btnSizePx,
            bgColor = Color(0xFF66BB6A).copy(alpha = 0.85f)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_email),
                contentDescription = "Send Feedback",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Layer 11: Scan timer pill (free users only)
        if (!isPremium && isScanActive) {
            val timerColor = if (scanTimerSeconds <= 10) Color(0xFFFF5252) else Color.White
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${scanTimerSeconds / 60}:${String.format("%02d", scanTimerSeconds % 60)}",
                    color = timerColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Layer 12: Scan-expired overlay (free users, timer hit 0)
        if (!isPremium && !isScanActive && isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Scan Complete",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { viewModel.startScan(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4FC3F7)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Start New Scan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(onClick = { viewModel.dismissScanOverlay() }) {
                        Text(
                            text = "Back to Camera",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }

                    TextButton(onClick = onPaywallNeeded) {
                        Text(
                            text = "Upgrade to Premium - No Limits",
                            color = Color(0xFF4FC3F7),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableFloatingButton(
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onClick: () -> Unit,
    maxWidth: Float,
    maxHeight: Float,
    btnSize: Float,
    bgColor: Color = Color.Black.copy(alpha = 0.4f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val firstDown = down.changes.firstOrNull() ?: continue
                        if (!firstDown.pressed) continue
                        firstDown.consume()

                        var totalDrag = Offset.Zero
                        var wasDragged = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                if (!wasDragged) onClick()
                                break
                            }
                            val delta = change.positionChange()
                            totalDrag += delta
                            if (totalDrag.getDistance() > viewConfiguration.touchSlop) {
                                wasDragged = true
                            }
                            if (wasDragged) {
                                onOffsetChange(
                                    Offset(
                                        (offset.x + delta.x).coerceIn(0f, maxWidth - btnSize),
                                        (offset.y + delta.y).coerceIn(0f, maxHeight - btnSize)
                                    )
                                )
                            }
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectedKanjiList(kanji: List<Char>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Detected Kanji (${kanji.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (kanji.isEmpty()) {
            Text(
                text = "No kanji detected yet...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                kanji.forEach { char ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                // TODO: Show dictionary definition
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedJukugoList(
    jukugo: List<JukugoEntry>,
    onJukugoClick: (JukugoEntry) -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD4B896))
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
                text = "Detected 熟語 (${jukugo.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (jukugo.isEmpty()) {
                Text(
                    text = "No jukugo detected yet...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    jukugo.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEDD9C0))
                                .clickable { onJukugoClick(entry) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.text,
                                fontSize = 18.sp,
                                color = Color(0xFF2C2C2C),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = " - ",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = entry.reading,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Divider between entries (except after last)
                        if (index < jukugo.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFD4B896).copy(alpha = 0.3f))
                            )
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
                stats.avgFrameMs < 200 -> Color(0xFF4CAF50)
                stats.avgFrameMs < 400 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
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
                "KanjiLens needs camera access to detect and read Japanese text. Please grant the permission."
            } else {
                "Camera permission is required to use KanjiLens."
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Camera Permission")
        }
    }
}
