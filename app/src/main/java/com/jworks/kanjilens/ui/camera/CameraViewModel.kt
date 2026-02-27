package com.jworks.kanjilens.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.jworks.kanjilens.data.ai.AiProviderManager
import com.jworks.kanjilens.data.ai.GeminiOcrCorrector
import com.jworks.kanjilens.data.ai.OcrTextMerger
import com.jworks.kanjilens.domain.ai.AiResponse
import com.jworks.kanjilens.domain.ai.AnalysisContext
import com.jworks.kanjilens.domain.ai.ScopeLevel
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.data.jcoin.JCoinEarnRules
import com.jworks.kanjilens.data.subscription.SubscriptionManager
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.LuminanceSampler
import com.jworks.kanjilens.domain.models.DictionaryResult
import com.jworks.kanjilens.domain.models.KanjiInfo
import com.jworks.kanjilens.domain.models.ScanChallenge
import com.jworks.kanjilens.domain.models.ScanChallengeKanji
import com.jworks.kanjilens.domain.repository.BookmarkRepository
import com.jworks.kanjilens.domain.repository.DictionaryRepository
import com.jworks.kanjilens.domain.repository.KanjiInfoRepository
import com.jworks.kanjilens.domain.repository.SettingsRepository
import com.jworks.kanjilens.domain.usecases.EnrichWithFuriganaUseCase
import com.jworks.kanjilens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val enrichWithFurigana: EnrichWithFuriganaUseCase,
    private val settingsRepository: SettingsRepository,
    private val subscriptionManager: SubscriptionManager,
    private val dictionaryRepository: DictionaryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val kanjiInfoRepository: KanjiInfoRepository,
    private val earnRules: JCoinEarnRules,
    private val jCoinClient: JCoinClient,
    private val authRepository: AuthRepository,
    private val geminiOcrCorrector: GeminiOcrCorrector,
    private val aiProviderManager: AiProviderManager
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

    private val _bookmarkedKanji = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedKanji: StateFlow<Set<String>> = _bookmarkedKanji.asStateFlow()

    private val _isWordBookmarked = MutableStateFlow(false)
    val isWordBookmarked: StateFlow<Boolean> = _isWordBookmarked.asStateFlow()

    private val _kanjiInfo = MutableStateFlow<KanjiInfo?>(null)
    val kanjiInfo: StateFlow<KanjiInfo?> = _kanjiInfo.asStateFlow()

    private val _isKanjiInfoLoading = MutableStateFlow(false)
    val isKanjiInfoLoading: StateFlow<Boolean> = _isKanjiInfoLoading.asStateFlow()

    // AI Enhance state
    private val _isEnhancing = MutableStateFlow(false)
    val isEnhancing: StateFlow<Boolean> = _isEnhancing.asStateFlow()

    private val _isEnhanced = MutableStateFlow(false)
    val isEnhanced: StateFlow<Boolean> = _isEnhanced.asStateFlow()

    val isAiAvailable: Boolean get() = geminiOcrCorrector.isAvailable

    // AI Analysis state
    private val _aiAnalysisState = MutableStateFlow<AiPanelState>(AiPanelState.Idle)
    val aiAnalysisState: StateFlow<AiPanelState> = _aiAnalysisState.asStateFlow()

    val isAiAnalysisAvailable: Boolean get() = aiProviderManager.isAiAvailable

    @Volatile private var lastFrameBitmap: Bitmap? = null
    private var enhanceJob: Job? = null

    fun loadKanjiInfo(literal: String) {
        viewModelScope.launch {
            _isKanjiInfoLoading.value = true
            _kanjiInfo.value = try {
                kanjiInfoRepository.getKanji(literal)
            } catch (e: Exception) {
                Log.w(TAG, "Kanji info lookup failed for '$literal'", e)
                null
            }
            _isKanjiInfoLoading.value = false
        }
    }

    fun clearKanjiInfo() {
        _kanjiInfo.value = null
    }

    // Scan Challenge state
    private val _scanChallenge = MutableStateFlow<ScanChallenge?>(null)
    val scanChallenge: StateFlow<ScanChallenge?> = _scanChallenge.asStateFlow()

    // J Coin reward toast (shows "+X J" briefly)
    private val _coinRewardToast = MutableStateFlow<String?>(null)
    val coinRewardToast: StateFlow<String?> = _coinRewardToast.asStateFlow()

    // J Coin balance (for badge on camera screen)
    private val _jCoinBalance = MutableStateFlow(0)
    val jCoinBalance: StateFlow<Int> = _jCoinBalance.asStateFlow()

    fun startNewChallenge() {
        _scanChallenge.value = ScanChallengeKanji.getRandomChallenge()
    }

    fun dismissChallenge() {
        _scanChallenge.value = null
    }

    fun dismissCoinToast() {
        _coinRewardToast.value = null
    }

    private fun showCoinToast(coins: Int, reason: String) {
        _coinRewardToast.value = "+$coins J ($reason)"
        viewModelScope.launch {
            delay(2500)
            _coinRewardToast.value = null
        }
        // Refresh balance after a short delay for backend to process
        viewModelScope.launch {
            delay(1500)
            refreshJCoinBalance()
        }
    }

    fun refreshJCoinBalance() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: return@launch
            jCoinClient.getBalance(token).onSuccess {
                _jCoinBalance.value = it.balance
            }
        }
    }

    fun lookupWord(word: String, context: Context? = null) {
        viewModelScope.launch {
            _isDictionaryLoading.value = true
            val result = try {
                dictionaryRepository.lookup(word)
            } catch (e: Exception) {
                Log.w(TAG, "Dictionary lookup failed for '$word'", e)
                null
            }
            _dictionaryResult.value = result

            // Check word-level bookmark status
            _isWordBookmarked.value = try {
                bookmarkRepository.isBookmarked(word)
            } catch (e: Exception) { false }

            // Check bookmark status for each kanji in the word
            _bookmarkedKanji.value = try {
                val kanjiChars = word.filter { c ->
                    c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
                }
                kanjiChars.filter { bookmarkRepository.isBookmarked(it.toString()) }
                    .map { it.toString() }.toSet()
            } catch (e: Exception) {
                emptySet()
            }

            // Award J Coin for dictionary lookup
            if (result != null && context != null) {
                awardDictionaryLookupCoin(context, word)
            }
            _isDictionaryLoading.value = false
        }
    }

    fun toggleKanjiBookmark(kanji: String) {
        viewModelScope.launch {
            try {
                val nowBookmarked = bookmarkRepository.toggle(kanji, "")
                _bookmarkedKanji.value = if (nowBookmarked) {
                    _bookmarkedKanji.value + kanji
                } else {
                    _bookmarkedKanji.value - kanji
                }
                Log.d(TAG, "Kanji bookmark toggled: '$kanji' → $nowBookmarked")
                // J Coin: award favorite coin when bookmarking
                if (nowBookmarked) {
                    val ctx = lastContext ?: return@launch
                    awardFavoriteCoin(ctx, kanji)
                    earnRules.incrementTotalWordsSaved(ctx)
                    val wordMilestones = earnRules.checkCumulativeWordMilestones(ctx)
                    for (milestone in wordMilestones) { awardMilestoneCoin(ctx, milestone) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to toggle kanji bookmark for '$kanji'", e)
            }
        }
    }

    fun toggleWordBookmark(word: String, reading: String) {
        viewModelScope.launch {
            try {
                val nowBookmarked = bookmarkRepository.toggle(word, reading)
                _isWordBookmarked.value = nowBookmarked
                Log.d(TAG, "Word bookmark toggled: '$word' → $nowBookmarked")
                // J Coin: award favorite coin when bookmarking
                if (nowBookmarked) {
                    val ctx = lastContext ?: return@launch
                    awardFavoriteCoin(ctx, word)
                    earnRules.incrementTotalWordsSaved(ctx)
                    val wordMilestones = earnRules.checkCumulativeWordMilestones(ctx)
                    for (milestone in wordMilestones) { awardMilestoneCoin(ctx, milestone) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to toggle word bookmark for '$word'", e)
            }
        }
    }

    private suspend fun awardDictionaryLookupCoin(context: Context, word: String) {
        val earnAction = earnRules.checkDictionaryLookup(context) ?: return
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(
                accessToken = token,
                sourceType = earnAction.sourceType,
                baseAmount = earnAction.coins,
                metadata = mapOf("word" to word)
            ).onSuccess {
                Log.d(TAG, "Dictionary lookup coin awarded: ${it.coinsAwarded} for '$word'")
                withContext(Dispatchers.Main) {
                    showCoinToast(earnAction.coins, "lookup")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award dictionary lookup coin", e)
        }
    }

    private suspend fun awardFirstScanCoin(context: Context) {
        val earnAction = earnRules.checkFirstScan(context) ?: return
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(
                accessToken = token,
                sourceType = earnAction.sourceType,
                baseAmount = earnAction.coins
            ).onSuccess {
                Log.d(TAG, "First scan coin awarded: ${it.coinsAwarded}")
                withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "first scan") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award first scan coin", e)
        }
    }

    private suspend fun awardFavoriteCoin(context: Context, item: String) {
        val earnAction = earnRules.checkFavorite(context) ?: return
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(
                accessToken = token,
                sourceType = earnAction.sourceType,
                baseAmount = earnAction.coins,
                metadata = mapOf("item" to item)
            ).onSuccess {
                Log.d(TAG, "Favorite coin awarded: ${it.coinsAwarded} for '$item'")
                withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "bookmark") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award favorite coin", e)
        }
    }

    private suspend fun awardScanMilestoneCoin(context: Context) {
        val earnAction = earnRules.checkScanMilestone(context) ?: return
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(
                accessToken = token,
                sourceType = earnAction.sourceType,
                baseAmount = earnAction.coins
            ).onSuccess {
                Log.d(TAG, "Scan milestone coin awarded: ${it.coinsAwarded}")
                withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "10 scans!") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award scan milestone coin", e)
        }
    }

    private suspend fun awardStreakCoin(context: Context) {
        // Check all streak tiers (7, 30, 90 days)
        val token = authRepository.getAccessToken() ?: return
        earnRules.checkStreak(context)?.let { earnAction ->
            try {
                jCoinClient.earn(token, earnAction.sourceType, earnAction.coins).onSuccess {
                    Log.d(TAG, "7-day streak coin awarded: ${it.coinsAwarded}")
                    withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "7-day streak!") }
                }
            } catch (e: Exception) { Log.w(TAG, "Failed to award streak coin", e) }
        }
        earnRules.checkStreak30(context)?.let { earnAction ->
            try {
                jCoinClient.earn(token, earnAction.sourceType, earnAction.coins).onSuccess {
                    Log.d(TAG, "30-day streak coin awarded: ${it.coinsAwarded}")
                    withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "30-day streak!") }
                }
            } catch (e: Exception) { Log.w(TAG, "Failed to award 30-day streak coin", e) }
        }
        earnRules.checkStreak90(context)?.let { earnAction ->
            try {
                jCoinClient.earn(token, earnAction.sourceType, earnAction.coins).onSuccess {
                    Log.d(TAG, "90-day streak coin awarded: ${it.coinsAwarded}")
                    withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "90-day streak!") }
                }
            } catch (e: Exception) { Log.w(TAG, "Failed to award 90-day streak coin", e) }
        }
    }

    fun checkChallengeInDetections(context: Context) {
        val challenge = _scanChallenge.value ?: return
        if (challenge.isCompleted) return

        val texts = _detectedTexts.value
        val found = texts.any { detected ->
            detected.elements.any { element ->
                element.text.contains(challenge.targetKanji) ||
                element.kanjiSegments.any { it.text.contains(challenge.targetKanji) }
            }
        }

        if (found) {
            _scanChallenge.value = challenge.copy(isCompleted = true)
            viewModelScope.launch {
                awardScanChallengeCoin(context, challenge.targetKanji)
            }
        }
    }

    private suspend fun awardScanChallengeCoin(context: Context, kanji: String) {
        val earnAction = earnRules.checkScanChallenge(context) ?: return
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(
                accessToken = token,
                sourceType = earnAction.sourceType,
                baseAmount = earnAction.coins,
                metadata = mapOf("targetKanji" to kanji)
            ).onSuccess {
                Log.d(TAG, "Scan challenge coin awarded: ${it.coinsAwarded} for '$kanji'")
                withContext(Dispatchers.Main) {
                    showCoinToast(earnAction.coins, "challenge")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award scan challenge coin", e)
        }
    }

    fun awardShareCoin(context: Context) {
        viewModelScope.launch {
            val earnAction = earnRules.checkShare(context) ?: return@launch
            val token = authRepository.getAccessToken() ?: return@launch
            try {
                jCoinClient.earn(token, earnAction.sourceType, earnAction.coins).onSuccess {
                    Log.d(TAG, "Share coin awarded: ${it.coinsAwarded}")
                    withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, "shared!") }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to award share coin", e)
            }
        }
    }

    private suspend fun awardMilestoneCoin(context: Context, earnAction: com.jworks.kanjilens.data.jcoin.EarnAction) {
        val token = authRepository.getAccessToken() ?: return
        try {
            jCoinClient.earn(token, earnAction.sourceType, earnAction.coins).onSuccess {
                Log.d(TAG, "Milestone coin awarded: ${it.coinsAwarded} for ${earnAction.sourceType}")
                val label = earnAction.sourceType.replace("milestone_", "").replace("_", " ")
                withContext(Dispatchers.Main) { showCoinToast(earnAction.coins, label) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to award milestone coin for ${earnAction.sourceType}", e)
        }
    }

    /**
     * Trigger AI enhancement on the current frozen frame.
     * Called automatically when the user pauses the camera.
     */
    private fun triggerEnhance() {
        Log.d(TAG, "AI ENHANCE: triggerEnhance() called — available=${geminiOcrCorrector.isAvailable}, premium=${subscriptionManager.isPremium()}, aiEnabled=${settings.value.aiEnhanceEnabled}, enhancing=${_isEnhancing.value}, hasBitmap=${lastFrameBitmap != null}")
        if (!geminiOcrCorrector.isAvailable) { Log.w(TAG, "AI ENHANCE: BLOCKED — Gemini not available (no API key?)"); return }
        if (!subscriptionManager.isPremium()) { Log.w(TAG, "AI ENHANCE: BLOCKED — not premium"); return }
        if (!settings.value.aiEnhanceEnabled) { Log.w(TAG, "AI ENHANCE: BLOCKED — disabled in settings"); return }
        if (_isEnhancing.value) { Log.w(TAG, "AI ENHANCE: BLOCKED — already enhancing"); return }

        val bitmap = lastFrameBitmap ?: run {
            Log.w(TAG, "AI ENHANCE: BLOCKED — no frame bitmap available")
            return
        }

        Log.d(TAG, "AI ENHANCE: ★ STARTING — bitmap ${bitmap.width}x${bitmap.height}, detectedTexts=${_detectedTexts.value.size} lines")
        _isEnhancing.value = true
        _isEnhanced.value = false

        enhanceJob?.cancel()
        enhanceJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "AI ENHANCE: calling Gemini API...")
                val startMs = System.currentTimeMillis()
                val result = geminiOcrCorrector.extractTextWithReadings(bitmap)
                val elapsedMs = System.currentTimeMillis() - startMs
                result.onSuccess { geminiLines ->
                    Log.d(TAG, "AI ENHANCE: Gemini returned ${geminiLines.size} lines in ${elapsedMs}ms")
                    geminiLines.forEachIndexed { i, line ->
                        Log.d(TAG, "AI ENHANCE:   line[$i]: text='${line.text}', words=${line.words.map { "${it.w}→${it.r}" }}")
                    }
                    val currentTexts = _detectedTexts.value
                    Log.d(TAG, "AI ENHANCE: current ML Kit texts: ${currentTexts.size} lines")
                    if (currentTexts.isNotEmpty() && geminiLines.isNotEmpty()) {
                        val merged = OcrTextMerger.merge(currentTexts, geminiLines)
                        _detectedTexts.value = merged
                        _isEnhanced.value = true
                        Log.d(TAG, "AI ENHANCE: ★ DONE — merged ${geminiLines.size} lines, isEnhanced=true")
                        merged.forEach { dt ->
                            dt.elements.forEach { elem ->
                                elem.kanjiSegments.forEach { seg ->
                                    Log.d(TAG, "AI ENHANCE:   segment: '${seg.text}' → '${seg.reading}'")
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "AI ENHANCE: skipped merge — currentTexts=${currentTexts.size}, geminiLines=${geminiLines.size}")
                    }
                }.onFailure { e ->
                    Log.w(TAG, "AI ENHANCE: ✗ Gemini API FAILED after ${elapsedMs}ms", e)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI ENHANCE: ✗ ERROR", e)
            } finally {
                Log.d(TAG, "AI ENHANCE: enhancing complete, isEnhancing→false")
                _isEnhancing.value = false
            }
        }
    }

    private fun resetAiEnhanceState() {
        _isEnhanced.value = false
        _isEnhancing.value = false
        enhanceJob?.cancel()
    }

    fun clearDictionaryResult() {
        _dictionaryResult.value = null
    }

    // All bookmarked kanji (for highlighting in jukugo list)
    private val _allBookmarkedKanji = MutableStateFlow<Set<String>>(emptySet())
    val allBookmarkedKanji: StateFlow<Set<String>> = _allBookmarkedKanji.asStateFlow()

    fun refreshAllBookmarkedKanji() {
        viewModelScope.launch {
            try {
                _allBookmarkedKanji.value = bookmarkRepository.getAll()
                    .map { it.word }.toSet()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load bookmarked kanji", e)
            }
        }
    }

    private var timerJob: Job? = null
    private var lastContext: Context? = null

    fun startScan(context: Context, allowPaywall: Boolean = true) {
        lastContext = context
        // Track total scans for handle prompt
        authRepository.incrementTotalScans()

        // J Coin: record scan + check earn triggers
        earnRules.recordScan(context)
        viewModelScope.launch {
            try {
                awardFirstScanCoin(context)
                awardScanMilestoneCoin(context)
                awardStreakCoin(context)
                // Cumulative scan milestones (100/500/1000)
                val scanMilestones = earnRules.checkCumulativeScanMilestones(context)
                for (milestone in scanMilestones) { awardMilestoneCoin(context, milestone) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to award scan coins", e)
            }
        }

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
    @Volatile private var processingStartTimeMs = 0L

    init {
        // Always start in FULL mode on cold start (don't persist partial mode across restarts)
        viewModelScope.launch {
            // Wait for DataStore to emit the persisted value before resetting
            val current = settingsRepository.settings.first()
            if (current.partialModeBoundaryRatio < 0.99f) {
                settingsRepository.updateSettings(current.copy(partialModeBoundaryRatio = 1f))
            }
        }
        viewModelScope.launch {
            settings.collect { cachedFrameSkip = it.frameSkip }
        }
        refreshJCoinBalance()
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun togglePause() {
        val wasPaused = _isPaused.value
        _isPaused.value = !wasPaused
        Log.d(TAG, "togglePause: wasPaused=$wasPaused → isPaused=${!wasPaused}")
        if (!wasPaused) {
            // Just paused — trigger AI enhancement immediately
            Log.d(TAG, "AI ENHANCE: pause triggered, calling triggerEnhance()")
            triggerEnhance()
        } else {
            // Unpaused — reset enhancement state
            resetAiEnhanceState()
            _aiAnalysisState.value = AiPanelState.Idle
        }
    }

    fun analyzeFullText() {
        val allText = _detectedTexts.value.joinToString("\n") { detected ->
            detected.elements.joinToString("") { it.text }
        }
        if (allText.isBlank()) return

        _aiAnalysisState.value = AiPanelState.Loading
        viewModelScope.launch {
            val context = AnalysisContext(
                selectedText = allText,
                fullSnapshotText = allText,
                scopeLevel = ScopeLevel.FullSnapshot
            )
            aiProviderManager.analyze(context)
                .onSuccess { response ->
                    _aiAnalysisState.value = AiPanelState.Result(response, allText)
                    settingsRepository.addTokenUsage(
                        response.provider,
                        response.inputTokens ?: 0,
                        response.outputTokens ?: 0
                    )
                }
                .onFailure { error ->
                    _aiAnalysisState.value = AiPanelState.Error(error.message ?: "Analysis failed")
                }
        }
    }

    fun dismissAiAnalysis() {
        _aiAnalysisState.value = AiPanelState.Idle
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
            resetAiEnhanceState()

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
            resetAiEnhanceState()
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
        if (frameCount % frameSkip != 0) {
            imageProxy.close()
            return
        }

        // Watchdog: if processing has been stuck for > 2 seconds, force-reset
        if (_isProcessing.value) {
            val elapsed = System.currentTimeMillis() - processingStartTimeMs
            if (elapsed > 2000L) {
                Log.w(TAG, "Processing watchdog: force-resetting after ${elapsed}ms")
                _isProcessing.value = false
            } else {
                imageProxy.close()
                return
            }
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        _isProcessing.value = true
        processingStartTimeMs = System.currentTimeMillis()

        // Prepare image for processing — protected so _isProcessing can't get stuck
        val rotation: Int
        val inputImage: InputImage
        val imageSize: Size
        try {
            rotation = imageProxy.imageInfo.rotationDegrees
            _rotationDegrees.value = rotation
            inputImage = InputImage.fromMediaImage(mediaImage, rotation)
            val cropRect = mediaImage.cropRect
            imageSize = Size(cropRect.width(), cropRect.height())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to prepare image for processing", e)
            _isProcessing.value = false
            imageProxy.close()
            return
        }

        // Capture bitmap for AI enhancement (latest frame always available for pause-triggered enhance)
        try {
            lastFrameBitmap = imageProxy.toBitmap()
        } catch (_: Exception) { /* bitmap capture optional */ }

        // Sample luminance from bitmap center (more reliable than Y-plane buffer)
        val globalLuminance: Int? = if (settings.value.furiganaAdaptiveColor) {
            try {
                val bmp = lastFrameBitmap
                if (bmp != null) sampleBitmapLuminance(bmp) else null
            } catch (e: Exception) { null }
        } else null

        // Debug: Log processing dimensions
        if (frameCount % (settings.value.frameSkip * 5) == 0) {
            Log.d(TAG, "Processing: ${imageSize.width}x${imageSize.height}, rotation=$rotation°, boundary=${settings.value.partialModeBoundaryRatio}")
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = processCameraFrame.execute(inputImage, imageSize)
                // Try to enrich with furigana, but fall back to raw OCR if it fails
                val enriched = try {
                    enrichWithFurigana.execute(result.texts)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Furigana enrichment failed, using raw OCR", e)
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

                    // No fallback — correctly show nothing when filter removes all elements.
                    // The old fallback was returning unfiltered results, causing text from
                    // behind the jukugo panel to appear in vertical partial mode.
                    if (totalBefore > 0 && totalAfter == 0) {
                        Log.d(TAG, "Filter: all $totalBefore elements outside visible region (expected in partial mode)")
                    }
                    result2
                } else {
                    enriched.also { _visibleRegion.value = null }
                }

                // Annotate elements with pre-sampled global luminance for adaptive furigana color
                val withLuminance = if (globalLuminance != null) {
                    filtered.map { detected ->
                        detected.copy(elements = detected.elements.map { element ->
                            element.copy(backgroundLuminance = globalLuminance)
                        })
                    }
                } else filtered

                // Sort by position (top-to-bottom, left-to-right) to prevent order jumping
                val sorted = withLuminance.sortedWith(compareBy(
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

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e  // Never swallow coroutine cancellation
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

    /** Sample average luminance from center 20% of a bitmap using 5x5 grid. */
    private fun sampleBitmapLuminance(bitmap: Bitmap): Int? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val cx = w / 2f
        val cy = h / 2f
        val rw = w * 0.2f
        val rh = h * 0.2f
        val left = cx - rw / 2f
        val top = cy - rh / 2f

        var sum = 0L
        var count = 0
        val gridSize = 5
        for (gy in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                val px = (left + rw * (gx + 0.5f) / gridSize).toInt().coerceIn(0, w - 1)
                val py = (top + rh * (gy + 0.5f) / gridSize).toInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(px, py)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // ITU-R BT.601 luma
                sum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                count++
            }
        }
        return if (count > 0) (sum / count).toInt() else null
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

sealed class AiPanelState {
    data object Idle : AiPanelState()
    data object Loading : AiPanelState()
    data class Result(val response: AiResponse, val detectedText: String) : AiPanelState()
    data class Error(val message: String) : AiPanelState()
}
