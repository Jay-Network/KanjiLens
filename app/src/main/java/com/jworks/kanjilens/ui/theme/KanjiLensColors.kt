package com.jworks.kanjilens.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * App-wide color constants for KanjiLens.
 * Replaces inline Color() hex literals throughout the codebase.
 */
object KanjiLensColors {
    // Primary brand
    val Primary = Color(0xFF4CAF50)
    val PrimaryAction = Color(0xFF4FC3F7)

    // Panel / parchment theme
    val PanelBackground = Color(0xFFF5E6D3)
    val PanelBorder = Color(0xFFD4B896)
    val PanelItemBackground = Color(0xFFEDD9C0)

    // J Coin
    val CoinAccent = Color(0xFFFFB74D)
    val CoinGold = Color(0xFFFFD54F)
    val CoinShine = Color(0xFFFFF176)
    val CoinToastBackground = Color(0xFF0D3B66)
    val JCoinButtonBg = Color(0xFFFFB74D)

    // Celebration / gamification
    val StreakFlame = Color(0xFFFF6D00)
    val StreakFlameLight = Color(0xFFFFAB40)
    val StreakFlameHot = Color(0xFFFF3D00)
    val LevelUpPurple = Color(0xFFAB47BC)
    val LevelUpGlow = Color(0xFFCE93D8)
    val SuccessGreen = Color(0xFF66BB6A)
    val ConfettiPink = Color(0xFFFF4081)
    val ConfettiBlue = Color(0xFF40C4FF)
    val ConfettiYellow = Color(0xFFFFD740)
    val ConfettiGreen = Color(0xFF69F0AE)

    // Feedback
    val FeedbackButtonBg = Color(0xFF66BB6A)

    // Bookmark
    val BookmarkButtonBg = Color(0xFF78909C)
    val BookmarkedKanjiHighlight = Color(0xFFBF6900)

    // Processing / status
    val ProcessingGreen = Color(0xFF4CAF50)

    // OCR HUD
    val HudFast = Color(0xFF4CAF50)
    val HudMedium = Color(0xFFFF9800)
    val HudSlow = Color(0xFFF44336)

    // Scan challenge
    val ChallengeActive = Color(0xFFFF6F00)
    val ChallengeComplete = Color(0xFF4CAF50)

    // Timer
    val TimerWarning = Color(0xFFFF5252)

    // Text
    val JukugoText = Color(0xFF2C2C2C)
    val JukugoSecondary = Color(0xFF666666)

    // Disabled / muted
    val DisabledCard = Color(0xFF2A2A2A)
    val ActiveCard = Color(0xFF1A3A5C)

    // Surfaces
    val DarkBg = Color(0xFF1B1B1B)
    val CardBg = Color(0xFF2A2A2A)
    val HeaderBg = Color(0xFF0D3B66)

    // Gradient brushes
    val CoinBalanceGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0D3B66), Color(0xFF1565C0))
    )
    val StreakGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF6D00), Color(0xFFFF3D00))
    )
    val PremiumGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
    )
}
