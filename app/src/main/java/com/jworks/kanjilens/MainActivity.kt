package com.jworks.kanjilens

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.AuthState
import com.jworks.kanjilens.data.billing.BillingManager
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.data.jcoin.JCoinEarnRules
import com.jworks.kanjilens.data.subscription.SubscriptionManager
import com.jworks.kanjilens.ui.auth.AuthScreen
import com.jworks.kanjilens.ui.camera.CameraScreen
import com.jworks.kanjilens.ui.feedback.FeedbackDialog
import com.jworks.kanjilens.ui.feedback.FeedbackViewModel
import com.jworks.kanjilens.ui.paywall.PaywallScreen
import com.jworks.kanjilens.ui.profile.ProfileScreen
import com.jworks.kanjilens.ui.rewards.RewardsScreen
import com.jworks.kanjilens.ui.settings.SettingsScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager.initialize()

        setContent {
            KanjiLensTheme {
                val navController = rememberNavController()
                val authState by authRepository.authState.collectAsState()
                var hasSkippedAuth by remember { mutableStateOf(false) }
                val feedbackViewModel: FeedbackViewModel = hiltViewModel()
                val feedbackUiState by feedbackViewModel.uiState.collectAsState()

                // Check auth state on launch
                LaunchedEffect(Unit) {
                    authRepository.refreshAuthState()
                }

                // Sync auth metadata and auto-navigate when session is restored
                LaunchedEffect(authState) {
                    val prefs = getSharedPreferences("kanjilens_prefs", Context.MODE_PRIVATE)
                    when (val state = authState) {
                        is AuthState.SignedIn -> {
                            prefs.edit().putString("user_email", state.user.email).apply()
                            // Auto-navigate to camera if still on auth screen (session restored)
                            if (navController.currentDestination?.route == "auth") {
                                navController.navigate("camera") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
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
                        startDestination = "auth"
                    ) {
                        composable("auth") {
                            if (hasSkippedAuth) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("camera") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            } else {
                                AuthScreen(
                                    authRepository = authRepository,
                                    onSkip = {
                                        hasSkippedAuth = true
                                        navController.navigate("camera") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    },
                                    onSignedIn = {
                                        navController.navigate("camera") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }

                        composable("camera") {
                            CameraScreen(
                                onSettingsClick = { navController.navigate("settings") },
                                onRewardsClick = { navController.navigate("rewards") },
                                onPaywallNeeded = { navController.navigate("paywall") },
                                onProfileClick = { navController.navigate("profile") },
                                onFeedbackClick = { feedbackViewModel.openDialog() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() },
                                onLogout = {
                                    lifecycleScope.launch {
                                        authRepository.signOut()
                                        hasSkippedAuth = false
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
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
                                onSignOut = {
                                    lifecycleScope.launch {
                                        authRepository.signOut()
                                        hasSkippedAuth = false
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
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
