package com.jworks.kanjilens.ui.profile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.BuildConfig
import com.jworks.kanjilens.R
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.AuthState
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.data.jcoin.JCoinEarnRules
import com.jworks.kanjilens.data.subscription.SubscriptionManager

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    subscriptionManager: SubscriptionManager,
    jCoinClient: JCoinClient,
    jCoinEarnRules: JCoinEarnRules,
    onBackClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val authState by authRepository.authState.collectAsState()
    val isPremium by subscriptionManager.isPremiumFlow.collectAsState()
    val isAdmin by authRepository.isAdminOrDeveloper.collectAsState()
    val showDeveloperTools = isAdmin || BuildConfig.DEBUG
    var premiumOverride by remember { mutableStateOf(subscriptionManager.getPremiumOverride() ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1B1B))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                text = "Profile",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF78909C)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = when (val state = authState) {
                            is AuthState.SignedIn -> state.user.displayName?.firstOrNull()?.uppercase() ?: "?"
                            else -> "?"
                        }
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = when (val state = authState) {
                            is AuthState.SignedIn -> state.user.displayName ?: "User"
                            else -> "Guest"
                        }
                        val email = when (val state = authState) {
                            is AuthState.SignedIn -> state.user.email ?: ""
                            else -> "Not signed in"
                        }

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Subscription badge
                        val badgeColor = if (isPremium) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                        val badgeText = if (isPremium) "Premium" else "Free"
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // App Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Stats",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val remaining = subscriptionManager.getRemainingScans(context)
                    val remainingText = if (isPremium) "Unlimited" else "$remaining / ${SubscriptionManager.FREE_SCAN_LIMIT}"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining Scans", style = MaterialTheme.typography.bodyMedium)
                        Text(remainingText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Connected Apps (Ecosystem)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connected Apps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // KanjiQuest
                    EcosystemAppRow(
                        name = "KanjiQuest",
                        description = "Learn kanji through quests",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjiquest"))
                            context.startActivity(intent)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // TutoringJay
                    EcosystemAppRow(
                        name = "TutoringJay",
                        description = "STEM tutoring with Jay",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tutoringjay.com"))
                            context.startActivity(intent)
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // J Coin
                    EcosystemAppRow(
                        name = "J Coin",
                        description = "Earn rewards across JWorks apps",
                        onClick = onRewardsClick
                    )
                }
            }

            // Admin/Developer Tools (conditional)
            if (showDeveloperTools) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Developer Tools",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (premiumOverride) "Simulating Premium" else "Simulating Free",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Override subscription status for testing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = premiumOverride,
                                onCheckedChange = { checked ->
                                    premiumOverride = checked
                                    subscriptionManager.setPremiumOverride(checked, context)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current: ${if (premiumOverride) "Force premium" else "Force free"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBF360C)
                        )

                        // Two-state toggle buttons
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    premiumOverride = true
                                    subscriptionManager.setPremiumOverride(true, context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (premiumOverride) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Premium", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    premiumOverride = false
                                    subscriptionManager.setPremiumOverride(false, context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!premiumOverride) Color(0xFFF44336) else Color(0xFFBDBDBD)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Free", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Sign Out
            if (authState is AuthState.SignedIn) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Sign Out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EcosystemAppRow(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
