package com.jworks.kanjilens.ui.rewards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.jworks.kanjilens.R
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.AuthState
import com.jworks.kanjilens.data.jcoin.JCoinBalance
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.data.jcoin.JCoinEarnRules
import com.jworks.kanjilens.data.subscription.SubscriptionManager
import com.jworks.kanjilens.ui.anim.StreakFlameIcon
import com.jworks.kanjilens.ui.anim.rememberAnimatedCount
import com.jworks.kanjilens.ui.theme.KanjiLensColors

@Composable
fun RewardsScreen(
    authRepository: AuthRepository,
    jCoinClient: JCoinClient,
    earnRules: JCoinEarnRules,
    subscriptionManager: SubscriptionManager,
    onBackClick: () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    BackHandler(onBack = onBackClick)

    val authState by authRepository.authState.collectAsState()
    val isPremium by subscriptionManager.isPremiumFlow.collectAsState()
    val context = LocalContext.current

    var balance by remember { mutableStateOf(JCoinBalance()) }
    val isSignedIn = authState is AuthState.SignedIn

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) {
            val token = authRepository.getAccessToken()
            if (token != null) {
                jCoinClient.getBalance(token).onSuccess {
                    balance = it
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KanjiLensColors.DarkBg)
    ) {
        // Header with gradient
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D3B66), Color(0xFF1565C0))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Coin icon in header
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(KanjiLensColors.CoinShine, KanjiLensColors.CoinGold)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = Color(0xFF5D4037), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "J Coin Rewards",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when {
                !isSignedIn -> SignedOutCard()
                !isPremium -> PremiumRequiredCard(onUpgradeClick = onUpgradeClick)
                else -> {
                    val dailyEarned = earnRules.getDailyEarned(context)
                    val streakDays = earnRules.getStreakDays(context)
                    val scansToday = earnRules.getScanCountToday(context)

                    BalanceCard(balance = balance)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Streak card with flame
                    StreakCard(streakDays = streakDays)
                    Spacer(modifier = Modifier.height(20.dp))

                    SectionHeader(title = "Today's Progress")
                    Spacer(modifier = Modifier.height(12.dp))

                    DailyProgressCard(
                        label = "Daily Coins",
                        current = dailyEarned,
                        max = JCoinEarnRules.DAILY_CAP,
                        color = KanjiLensColors.CoinAccent,
                        emoji = "\uD83E\uDE99"  // coin
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyProgressCard(
                        label = "Scan Milestone",
                        current = scansToday.coerceAtMost(10),
                        max = 10,
                        color = KanjiLensColors.PrimaryAction,
                        emoji = "\uD83D\uDCF7"  // camera
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(title = "How to Earn")
                    Spacer(modifier = Modifier.height(12.dp))

                    EarnRuleCard("\uD83D\uDCF8", "Scan your first text today", "+5", "Daily")
                    EarnRuleCard("\uD83D\uDD0D", "Look up a word", "+1", "Up to 5/day")
                    EarnRuleCard("\u2B50", "Save a word to favorites", "+2", "Up to 10/day")
                    EarnRuleCard("\uD83C\uDFAF", "Complete a Scan Challenge", "+10", "Up to 3/day")
                    EarnRuleCard("\uD83D\uDCC8", "Scan 10 texts in one day", "+10", "Daily")
                    EarnRuleCard("\uD83D\uDD25", "Keep a 7-day streak", "+50", "Weekly")
                    EarnRuleCard("\uD83C\uDFC6", "Reach a 30-day streak", "+100", "One-time")
                    EarnRuleCard("\uD83D\uDC8E", "Reach a 90-day streak", "+300", "One-time")
                    EarnRuleCard("\uD83D\uDCE4", "Share a scan result", "+5", "Up to 2/day")

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(title = "Redeem Coins")
                    Spacer(modifier = Modifier.height(12.dp))

                    RedemptionCard(
                        emoji = "\uD83C\uDF93",
                        title = "Free TutoringJay Lesson",
                        cost = "2,000 J",
                        description = "30-minute trial lesson with Jay",
                        enabled = balance.balance >= 2000
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RedemptionCard(
                        emoji = "\uD83C\uDFAE",
                        title = "KanjiQuest Credit",
                        cost = "500 J",
                        description = "\$5 credit toward KanjiQuest subscription",
                        enabled = balance.balance >= 500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SignedOutCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF1E1E1E))
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated coin circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(KanjiLensColors.CoinShine, KanjiLensColors.CoinGold)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = Color(0xFF5D4037),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start Earning J Coins!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to earn coins every time you scan, save words, and keep streaks going. Redeem them for real rewards!",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PremiumRequiredCard(onUpgradeClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF1E1E1E))
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Dimmed coin
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = KanjiLensColors.CoinAccent.copy(alpha = 0.4f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unlock J Coins with Premium",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Premium members earn J Coins every scan, lookup, and streak. Redeem for tutoring sessions and app credits!",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KanjiLensColors.PrimaryAction
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Upgrade to Premium",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: JCoinBalance) {
    val animatedBalance = rememberAnimatedCount(balance.balance)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(KanjiLensColors.CoinBalanceGradient)
            .padding(28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Your J Coins",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Golden coin circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    KanjiLensColors.CoinShine,
                                    KanjiLensColors.CoinGold
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "J",
                        color = Color(0xFF5D4037),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$animatedBalance",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lifetime earned: ${balance.lifetimeEarned}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StreakCard(streakDays: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (streakDays > 0)
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3E2723), Color(0xFF4E342E))
                    )
                else
                    Brush.horizontalGradient(
                        colors = listOf(KanjiLensColors.CardBg, KanjiLensColors.CardBg)
                    )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (streakDays > 0) {
                    StreakFlameIcon(
                        streakDays = streakDays,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Current Streak",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$streakDays day${if (streakDays != 1) "s" else ""}",
                        color = if (streakDays > 0) KanjiLensColors.StreakFlameLight else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (streakDays >= 7) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KanjiLensColors.SuccessGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+50 J",
                        color = KanjiLensColors.SuccessGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (streakDays > 0) {
                Text(
                    text = "${7 - streakDays} more for +50 J",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "Scan today to start!",
                    color = KanjiLensColors.PrimaryAction.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DailyProgressCard(
    label: String,
    current: Int,
    max: Int,
    color: Color,
    emoji: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KanjiLensColors.CardBg)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (emoji.isNotEmpty()) {
                        Text(text = emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "$current / $max",
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Animated progress bar
            val animProgress = remember { Animatable(0f) }
            val targetProgress = (current.toFloat() / max).coerceIn(0f, 1f)
            LaunchedEffect(current) {
                animProgress.animateTo(
                    targetValue = targetProgress,
                    animationSpec = tween(600, easing = EaseOutCubic)
                )
            }

            LinearProgressIndicator(
                progress = { animProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
private fun EarnRuleCard(emoji: String, action: String, reward: String, frequency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KanjiLensColors.CardBg.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = action,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        // Coin badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(KanjiLensColors.CoinAccent.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = reward,
                color = KanjiLensColors.CoinGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = frequency,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RedemptionCard(
    emoji: String,
    title: String,
    cost: String,
    description: String,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1A3A5C), Color(0xFF1E4976))
                    )
                else
                    Brush.horizontalGradient(
                        colors = listOf(KanjiLensColors.CardBg, KanjiLensColors.CardBg)
                    )
            )
            .clickable(enabled = enabled) { /* TODO: Implement redemption flow */ }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = if (enabled) 0.65f else 0.3f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (enabled) KanjiLensColors.CoinAccent.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = cost,
                    color = if (enabled) KanjiLensColors.CoinGold else Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
