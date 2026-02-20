package com.jworks.kanjilens

import android.net.Uri
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.map
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.AuthState
import com.jworks.kanjilens.data.billing.BillingManager
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.data.jcoin.JCoinEarnRules
import com.jworks.kanjilens.data.preferences.SettingsDataStore
import com.jworks.kanjilens.data.subscription.SubscriptionManager
import com.jworks.kanjilens.domain.repository.BookmarkRepository
import com.jworks.kanjilens.domain.repository.DictionaryRepository
import com.jworks.kanjilens.ui.auth.AuthScreen
import com.jworks.kanjilens.ui.auth.HandlePromptDialog
import com.jworks.kanjilens.ui.bookmarks.BookmarksScreen
import com.jworks.kanjilens.ui.camera.CameraScreen
import com.jworks.kanjilens.ui.feedback.FeedbackDialog
import com.jworks.kanjilens.ui.feedback.FeedbackViewModel
import com.jworks.kanjilens.ui.help.HelpScreen
import com.jworks.kanjilens.ui.onboarding.OnboardingScreen
import com.jworks.kanjilens.ui.paywall.PaywallScreen
import com.jworks.kanjilens.ui.profile.ProfileScreen
import com.jworks.kanjilens.ui.rewards.RewardsScreen
import com.jworks.kanjilens.ui.settings.SettingsScreen
import com.jworks.kanjilens.ui.dictionary.DictionaryDetailView
import com.jworks.kanjilens.domain.models.DictionaryResult
import com.jworks.kanjilens.ui.splash.SplashScreen
import com.jworks.kanjilens.ui.theme.KanjiLensTheme
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var jCoinClient: JCoinClient
    @Inject lateinit var jCoinEarnRules: JCoinEarnRules
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var dictionaryRepository: DictionaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        billingManager.initialize()

        setContent {
            KanjiLensTheme {
                val navController = rememberNavController()
                val authState by authRepository.authState.collectAsState()
                val feedbackViewModel: FeedbackViewModel = hiltViewModel()
                val feedbackUiState by feedbackViewModel.uiState.collectAsState()
                val hasSeenOnboarding by settingsDataStore.hasSeenOnboardingFlow
                    .map<Boolean, Boolean?> { it }
                    .collectAsState(initial = null) // null = still loading from DataStore
                var showHandlePrompt by remember { mutableStateOf(false) }

                // Ensure session on launch (auto-creates anonymous if needed)
                LaunchedEffect(Unit) {
                    authRepository.ensureSession(this@MainActivity)
                }

                // Sync auth metadata
                LaunchedEffect(authState) {
                    val prefs = getSharedPreferences("kanjilens_prefs", Context.MODE_PRIVATE)
                    when (val state = authState) {
                        is AuthState.SignedIn -> {
                            prefs.edit().putString("user_email", state.user.email).apply()
                        }
                        else -> {
                            prefs.edit().remove("user_email").apply()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                hasSeenOnboarding = hasSeenOnboarding,
                                onSplashFinished = { goToOnboarding ->
                                    if (goToOnboarding) {
                                        navController.navigate("onboarding") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        // Skip auth — go straight to camera
                                        navController.navigate("camera") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                onComplete = {
                                    lifecycleScope.launch {
                                        settingsDataStore.setOnboardingSeen()
                                    }
                                    // Go straight to camera after onboarding
                                    navController.navigate("camera") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Auth screen — now "Link Account", only reachable from Settings/Profile
                        composable("auth") {
                            AuthScreen(
                                authRepository = authRepository,
                                onBackClick = { navController.popBackStack() },
                                onSignedIn = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("camera") {
                            // Check handle prompt each time camera screen is shown
                            LaunchedEffect(Unit) {
                                if (authRepository.shouldShowHandlePrompt()) {
                                    showHandlePrompt = true
                                }
                            }
                            CameraScreen(
                                onSettingsClick = { navController.navigate("settings") },
                                onRewardsClick = { navController.navigate("rewards") },
                                onPaywallNeeded = { navController.navigate("paywall") },
                                onProfileClick = { navController.navigate("profile") },
                                onFeedbackClick = { feedbackViewModel.openDialog() },
                                onBookmarksClick = { navController.navigate("bookmarks") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() },
                                onLogout = {
                                    lifecycleScope.launch {
                                        authRepository.signOut()
                                        // signOut auto-creates anonymous session; go back to camera
                                        navController.popBackStack()
                                    }
                                },
                                onHelpClick = { navController.navigate("help") },
                                onLinkAccountClick = { navController.navigate("auth") },
                                authRepository = authRepository
                            )
                        }

                        composable("help") {
                            HelpScreen(
                                onBackClick = { navController.popBackStack() },
                                onFeedbackClick = {
                                    navController.popBackStack()
                                    feedbackViewModel.openDialog()
                                }
                            )
                        }

                        composable("paywall") {
                            PaywallScreen(
                                billingManager = billingManager,
                                activity = this@MainActivity,
                                remainingScans = subscriptionManager.getRemainingScans(this@MainActivity),
                                onDismiss = { navController.popBackStack() }
                            )
                        }

                        composable("rewards") {
                            RewardsScreen(
                                authRepository = authRepository,
                                jCoinClient = jCoinClient,
                                earnRules = jCoinEarnRules,
                                subscriptionManager = subscriptionManager,
                                onBackClick = { navController.popBackStack() },
                                onUpgradeClick = {
                                    navController.navigate("paywall")
                                }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                authRepository = authRepository,
                                subscriptionManager = subscriptionManager,
                                jCoinClient = jCoinClient,
                                jCoinEarnRules = jCoinEarnRules,
                                onBackClick = { navController.popBackStack() },
                                onRewardsClick = { navController.navigate("rewards") },
                                onLinkAccountClick = { navController.navigate("auth") },
                                onSignOut = {
                                    lifecycleScope.launch {
                                        authRepository.signOut()
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable("bookmarks") {
                            BookmarksScreen(
                                bookmarkRepository = bookmarkRepository,
                                onBackClick = { navController.popBackStack() },
                                onWordClick = { word ->
                                    navController.navigate("dictionary/${Uri.encode(word)}")
                                }
                            )
                        }

                        composable(
                            "dictionary/{word}",
                            arguments = listOf(navArgument("word") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val word = Uri.decode(backStackEntry.arguments?.getString("word") ?: "")
                            val scope = rememberCoroutineScope()
                            var dictResult by remember { mutableStateOf<DictionaryResult?>(null) }
                            var isLoading by remember { mutableStateOf(true) }
                            var isBookmarked by remember { mutableStateOf(false) }
                            var savedKanji by remember { mutableStateOf<Set<String>>(emptySet()) }

                            LaunchedEffect(word) {
                                isLoading = true
                                dictResult = dictionaryRepository.lookup(word)
                                isBookmarked = bookmarkRepository.isBookmarked(word)
                                // Check which kanji in this word are bookmarked
                                val kanjiSet = mutableSetOf<String>()
                                word.forEach { ch ->
                                    val s = ch.toString()
                                    if ((ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF)
                                        && bookmarkRepository.isBookmarked(s)) {
                                        kanjiSet.add(s)
                                    }
                                }
                                savedKanji = kanjiSet
                                isLoading = false
                            }

                            DictionaryDetailView(
                                result = dictResult,
                                isLoading = isLoading,
                                onBackClick = { navController.popBackStack() },
                                wordText = word,
                                wordReading = "",
                                isWordBookmarked = isBookmarked,
                                onWordBookmarkToggle = {
                                    scope.launch {
                                        val reading = dictResult?.reading ?: ""
                                        bookmarkRepository.toggle(word, reading)
                                        isBookmarked = bookmarkRepository.isBookmarked(word)
                                    }
                                },
                                bookmarkedKanji = savedKanji,
                                onKanjiClick = { kanji ->
                                    navController.navigate("dictionary/${Uri.encode(kanji)}")
                                },
                                modifier = Modifier.fillMaxSize().statusBarsPadding()
                            )
                        }
                    }

                    // Feedback dialog overlay
                    if (feedbackUiState.isDialogOpen) {
                        FeedbackDialog(
                            onDismiss = { feedbackViewModel.closeDialog() },
                            viewModel = feedbackViewModel
                        )
                    }

                    // Handle prompt dialog (after 3rd scan)
                    if (showHandlePrompt) {
                        HandlePromptDialog(
                            onSave = { handle ->
                                authRepository.setHandle(handle)
                                showHandlePrompt = false
                            },
                            onDismiss = {
                                authRepository.dismissHandlePrompt()
                                showHandlePrompt = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        billingManager.queryPurchases()
    }

    override fun onDestroy() {
        billingManager.endConnection()
        super.onDestroy()
    }
}
