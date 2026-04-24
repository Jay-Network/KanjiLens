package com.jworks.kanjisage.ui.rewards

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.jworks.kanjisage.R
import com.jworks.kanjisage.data.auth.AuthRepository
import com.jworks.kanjisage.data.auth.AuthState
import com.jworks.kanjisage.data.jcoin.JCoinBalance
import com.jworks.kanjisage.data.jcoin.JCoinClient
import com.jworks.kanjisage.data.jcoin.JCoinEarnRules
import com.jworks.kanjisage.data.subscription.SubscriptionManager
import com.jworks.kanjisage.ui.anim.StreakFlameIcon
import com.jworks.kanjisage.ui.anim.rememberAnimatedCount
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.ui.theme.focusBorder

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
            .background(KanjiSageColors.DarkBg)
    ) {
        // Header with gradient
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(KanjiSageColors.HeaderBg, KanjiSageColors.CoinGradientEnd)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.rewards_back),
                modifier = Modifier
                    .size(24.dp)
                    .focusBorder(CircleShape)
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
                            colors = listOf(KanjiSageColors.CoinShine, KanjiSageColors.CoinGold)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = KanjiSageColors.CoinLetterBrown, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rewards_title),
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
                    val totalScans = earnRules.getTotalScans(context)
                    val totalWordsSaved = earnRules.getTotalWordsSaved(context)
                    val scope = rememberCoroutineScope()

                    // Store purchase state
                    var purchaseItem by remember { mutableStateOf<StoreItem?>(null) }
                    var purchaseMessage by remember { mutableStateOf<String?>(null) }
                    var purchaseSuccess by remember { mutableStateOf(false) }

                    BalanceCard(balance = balance)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Streak card with flame
                    StreakCard(streakDays = streakDays)
                    Spacer(modifier = Modifier.height(20.dp))

                    SectionHeader(title = stringResource(R.string.rewards_today_progress))
                    Spacer(modifier = Modifier.height(12.dp))

                    DailyProgressCard(
                        label = stringResource(R.string.rewards_daily_coins),
                        current = dailyEarned,
                        max = JCoinEarnRules.DAILY_CAP,
                        color = KanjiSageColors.CoinAccent,
                        emoji = "\uD83E\uDE99"  // coin
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyProgressCard(
                        label = stringResource(R.string.rewards_scan_milestone),
                        current = scansToday.coerceAtMost(10),
                        max = 10,
                        color = KanjiSageColors.PrimaryAction,
                        emoji = "\uD83D\uDCF7"  // camera
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Cumulative progress cards
                    val nextScanMilestone = when {
                        totalScans < 100 -> 100
                        totalScans < 500 -> 500
                        else -> 1000
                    }
                    DailyProgressCard(
                        label = stringResource(R.string.rewards_total_scans),
                        current = totalScans.coerceAtMost(nextScanMilestone),
                        max = nextScanMilestone,
                        color = KanjiSageColors.AccentTeal,
                        emoji = "\uD83D\uDCCA"  // chart
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val nextWordMilestone = when {
                        totalWordsSaved < 100 -> 100
                        totalWordsSaved < 500 -> 500
                        else -> 1000
                    }
                    DailyProgressCard(
                        label = stringResource(R.string.rewards_words_saved),
                        current = totalWordsSaved.coerceAtMost(nextWordMilestone),
                        max = nextWordMilestone,
                        color = KanjiSageColors.CoinGradientEnd,
                        emoji = "\uD83D\uDCDA"  // books
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(title = stringResource(R.string.rewards_how_to_earn))
                    Spacer(modifier = Modifier.height(12.dp))

                    EarnRuleCard("\uD83D\uDCF8", stringResource(R.string.rewards_earn_first_scan), "+5", stringResource(R.string.rewards_freq_daily))
                    EarnRuleCard("\uD83D\uDD0D", stringResource(R.string.rewards_earn_lookup), "+1", stringResource(R.string.rewards_freq_5day))
                    EarnRuleCard("\u2B50", stringResource(R.string.rewards_earn_save), "+2", stringResource(R.string.rewards_freq_10day))
                    EarnRuleCard("\uD83C\uDFAF", stringResource(R.string.rewards_earn_challenge), "+10", stringResource(R.string.rewards_freq_3day))
                    EarnRuleCard("\uD83D\uDCC8", stringResource(R.string.rewards_earn_10_scans), "+10", stringResource(R.string.rewards_freq_daily))
                    EarnRuleCard("\uD83D\uDD25", stringResource(R.string.rewards_earn_7day_streak), "+50", stringResource(R.string.rewards_freq_weekly))
                    EarnRuleCard("\uD83C\uDFC6", stringResource(R.string.rewards_earn_30day_streak), "+100", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDC8E", stringResource(R.string.rewards_earn_90day_streak), "+300", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCE4", stringResource(R.string.rewards_earn_share), "+5", stringResource(R.string.rewards_freq_2day))
                    // Cumulative milestones
                    EarnRuleCard("\uD83D\uDCCA", stringResource(R.string.rewards_earn_100_scans), "+25", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCCA", stringResource(R.string.rewards_earn_500_scans), "+100", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCCA", stringResource(R.string.rewards_earn_1000_scans), "+500", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCDA", stringResource(R.string.rewards_earn_100_words), "+25", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCDA", stringResource(R.string.rewards_earn_500_words), "+100", stringResource(R.string.rewards_freq_onetime))
                    EarnRuleCard("\uD83D\uDCDA", stringResource(R.string.rewards_earn_1000_words), "+500", stringResource(R.string.rewards_freq_onetime))

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionHeader(title = stringResource(R.string.rewards_redeem_title))
                    Spacer(modifier = Modifier.height(12.dp))

                    val storeItems = remember { listOf(
                        StoreItem("🌙", stringResource(R.string.rewards_store_dark_theme), 200, "theme_dark", stringResource(R.string.rewards_store_dark_desc)),
                        StoreItem("🌸", stringResource(R.string.rewards_store_sakura_theme), 200, "theme_sakura", stringResource(R.string.rewards_store_sakura_desc)),
                        StoreItem("📥", stringResource(R.string.rewards_store_scan_export), 150, "scan_export", stringResource(R.string.rewards_store_scan_desc)),
                        StoreItem("🔍", stringResource(R.string.rewards_store_adv_ocr), 100, "advanced_ocr_trial", stringResource(R.string.rewards_store_adv_ocr_desc)),
                        StoreItem("⭐", stringResource(R.string.rewards_store_1day_pass), 50, "premium_1day", stringResource(R.string.rewards_store_1day_desc)),
                        StoreItem("💎", stringResource(R.string.rewards_store_3day_pass), 100, "premium_3day", stringResource(R.string.rewards_store_3day_desc))
                    ) }

                    storeItems.forEach { item ->
                        val canAfford = balance.balance >= item.cost
                        RedemptionCard(
                            emoji = item.emoji,
                            title = item.title,
                            cost = "${item.cost} J",
                            description = item.description,
                            enabled = canAfford,
                            onClick = { purchaseItem = item }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Purchase confirmation dialog
                    purchaseItem?.let { item ->
                        AlertDialog(
                            onDismissRequest = { purchaseItem = null },
                            title = { Text(stringResource(R.string.rewards_confirm_title), fontWeight = FontWeight.Bold) },
                            text = { Text(stringResource(R.string.rewards_confirm_msg, item.cost, item.title)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    val buying = item
                                    purchaseItem = null
                                    scope.launch {
                                        val token = authRepository.getAccessToken()
                                        if (token == null) {
                                            purchaseMessage = context.getString(R.string.rewards_sign_in_required)
                                            return@launch
                                        }
                                        jCoinClient.spend(token, buying.sourceType, buying.cost, buying.title)
                                            .onSuccess { resp ->
                                                balance = balance.copy(balance = resp.newBalance.toInt())
                                                purchaseSuccess = true
                                                purchaseMessage = context.getString(R.string.rewards_purchased, buying.title)
                                                // Refresh full balance
                                                jCoinClient.getBalance(token).onSuccess { balance = it }
                                            }
                                            .onFailure { e ->
                                                val msg = e.message ?: ""
                                                purchaseMessage = if (msg.contains("INSUFFICIENT_BALANCE"))
                                                    context.getString(R.string.rewards_insufficient) else context.getString(R.string.rewards_failed)
                                            }
                                    }
                                }) { Text(stringResource(R.string.rewards_buy), color = KanjiSageColors.PrimaryAction) }
                            },
                            dismissButton = {
                                TextButton(onClick = { purchaseItem = null }) { Text(stringResource(R.string.rewards_cancel)) }
                            }
                        )
                    }

                    // Purchase result feedback
                    purchaseMessage?.let { msg ->
                        LaunchedEffect(msg) {
                            kotlinx.coroutines.delay(2500)
                            purchaseMessage = null
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (purchaseSuccess) KanjiSageColors.SuccessGreen.copy(alpha = 0.2f)
                                    else Color.Red.copy(alpha = 0.2f)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg,
                                color = if (purchaseSuccess) KanjiSageColors.SuccessGreen else KanjiSageColors.ErrorLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
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
                    colors = listOf(KanjiSageColors.CardBg, KanjiSageColors.CardBgDark)
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
                            colors = listOf(KanjiSageColors.CoinShine, KanjiSageColors.CoinGold)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = KanjiSageColors.CoinLetterBrown,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.rewards_start_earning),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rewards_signed_out_desc),
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
                    colors = listOf(KanjiSageColors.CardBg, KanjiSageColors.CardBgDark)
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
                    .background(KanjiSageColors.CardBgLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = KanjiSageColors.CoinAccent.copy(alpha = 0.4f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.rewards_unlock_premium),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rewards_premium_desc),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KanjiSageColors.PrimaryAction
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.rewards_upgrade),
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
            .background(KanjiSageColors.CoinBalanceGradient)
            .padding(28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.rewards_your_coins),
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
                                    KanjiSageColors.CoinShine,
                                    KanjiSageColors.CoinGold
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "J",
                        color = KanjiSageColors.CoinLetterBrown,
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
                text = stringResource(R.string.rewards_lifetime, balance.lifetimeEarned),
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
                        colors = listOf(KanjiSageColors.CardBrownStart, KanjiSageColors.CardBrownEnd)
                    )
                else
                    Brush.horizontalGradient(
                        colors = listOf(KanjiSageColors.CardBg, KanjiSageColors.CardBg)
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
                        text = stringResource(R.string.rewards_current_streak),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (streakDays != 1) stringResource(R.string.rewards_days_plural, streakDays) else stringResource(R.string.rewards_days_singular, streakDays),
                        color = if (streakDays > 0) KanjiSageColors.StreakFlameLight else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (streakDays >= 7) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KanjiSageColors.SuccessGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+50 J",
                        color = KanjiSageColors.SuccessGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (streakDays > 0) {
                Text(
                    text = stringResource(R.string.rewards_streak_remaining, 7 - streakDays),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.rewards_scan_to_start),
                    color = KanjiSageColors.PrimaryAction.copy(alpha = 0.8f),
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
            .background(KanjiSageColors.CardBg)
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
                trackColor = KanjiSageColors.CardBgLight
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
            .background(KanjiSageColors.CardBg.copy(alpha = 0.5f))
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
                .background(KanjiSageColors.CoinAccent.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = reward,
                color = KanjiSageColors.CoinGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = frequency,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp
        )
    }
}

private data class StoreItem(
    val emoji: String,
    val title: String,
    val cost: Int,
    val sourceType: String,
    val description: String
)

@Composable
private fun RedemptionCard(
    emoji: String,
    title: String,
    cost: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(
                        colors = listOf(KanjiSageColors.ActiveCard, KanjiSageColors.ActiveCardLight)
                    )
                else
                    Brush.horizontalGradient(
                        colors = listOf(KanjiSageColors.CardBg, KanjiSageColors.CardBg)
                    )
            )
            .focusBorder(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
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
                        if (enabled) KanjiSageColors.CoinAccent.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = cost,
                    color = if (enabled) KanjiSageColors.CoinGold else Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
