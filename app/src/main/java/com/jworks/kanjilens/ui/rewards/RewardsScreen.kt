package com.jworks.kanjilens.ui.rewards

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    // Fetch balance on screen load
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
            .background(Color(0xFF1B1B1B))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D3B66))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                !isSignedIn -> {
                    SignedOutCard()
                }
                !isPremium -> {
                    PremiumRequiredCard(onUpgradeClick = onUpgradeClick)
                }
                else -> {
                    // Daily progress
                    val dailyEarned = earnRules.getDailyEarned(context)
                    val streakDays = earnRules.getStreakDays(context)
                    val scansToday = earnRules.getScanCountToday(context)

                    BalanceCard(balance = balance)
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Today's Progress",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DailyProgressCard(
                        label = "Daily Coins",
                        current = dailyEarned,
                        max = JCoinEarnRules.DAILY_CAP,
                        color = Color(0xFFFFB74D)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DailyProgressCard(
                        label = "Scan Milestone",
                        current = scansToday.coerceAtMost(10),
                        max = 10,
                        color = Color(0xFF4FC3F7)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Streak",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$streakDays day${if (streakDays != 1) "s" else ""}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (streakDays >= 7) {
                                Text(
                                    text = "+50 J",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "${7 - streakDays} days to bonus",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "How to Earn",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    EarnRuleRow("First scan of the day", "5 coins", "Daily")
                    EarnRuleRow("Save to favorites", "2 coins", "Cap: 20/day")
                    EarnRuleRow("10 scans milestone", "10 coins", "Daily")
                    EarnRuleRow("7-day streak", "50 coins", "Weekly")
                    EarnRuleRow("Share scan result", "5 coins", "Cap: 10/day")

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Redeem Coins",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    RedemptionCard(
                        title = "Free TutoringJay Lesson",
                        cost = "2,000 J",
                        description = "30-minute trial lesson with a tutor",
                        enabled = balance.balance >= 2000
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RedemptionCard(
                        title = "KanjiQuest Credit",
                        cost = "500 J",
                        description = "\$5 credit toward KanjiQuest subscription",
                        enabled = balance.balance >= 500
                    )
                }
            }
        }
    }
}

@Composable
private fun SignedOutCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sign in to earn J Coins",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Earn coins by scanning, saving favorites, and maintaining streaks.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
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
            .background(Color(0xFF2A2A2A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "J",
                color = Color(0xFFFFB74D).copy(alpha = 0.4f),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "J Coins are a Premium feature",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upgrade to Premium to earn J Coins with every scan, save favorites, and redeem rewards.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onUpgradeClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4FC3F7)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Upgrade to Premium",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: JCoinBalance) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0D3B66), Color(0xFF1A5276))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "J Coin Balance",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "J",
                    color = Color(0xFFFFB74D),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${balance.balance}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lifetime earned: ${balance.lifetimeEarned}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DailyProgressCard(
    label: String,
    current: Int,
    max: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "$current / $max",
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progressValue = (current.toFloat() / max).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
private fun EarnRuleRow(action: String, reward: String, frequency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = action,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = reward,
            color = Color(0xFFFFB74D),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = frequency,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RedemptionCard(
    title: String,
    cost: String,
    description: String,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color(0xFF1A3A5C) else Color(0xFF2A2A2A))
            .clickable(enabled = enabled) { /* TODO: Implement redemption flow */ }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = if (enabled) 0.6f else 0.3f),
                    fontSize = 13.sp
                )
            }
            Text(
                text = cost,
                color = if (enabled) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.3f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
