package com.jworks.kanjisage.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.jworks.kanjisage.ui.theme.focusBorder
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
import androidx.compose.material3.HorizontalDivider
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
import com.jworks.kanjisage.BuildConfig
import com.jworks.kanjisage.R
import com.jworks.kanjisage.data.auth.AuthRepository
import com.jworks.kanjisage.data.auth.AuthState
import com.jworks.kanjisage.ui.auth.HandlePromptDialog
import com.jworks.kanjisage.data.jcoin.JCoinClient
import com.jworks.kanjisage.data.jcoin.JCoinEarnRules
import com.jworks.kanjisage.data.subscription.SubscriptionManager
import com.jworks.kanjisage.ui.theme.GlassCard
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.ui.theme.KanjiSageTypography

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    subscriptionManager: SubscriptionManager,
    jCoinClient: JCoinClient,
    jCoinEarnRules: JCoinEarnRules,
    onBackClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onLinkAccountClick: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val authState by authRepository.authState.collectAsState()
    val isPremium by subscriptionManager.isPremiumFlow.collectAsState()
    val isAdmin by authRepository.isAdminOrDeveloper.collectAsState()
    val showDeveloperTools = isAdmin || BuildConfig.DEBUG
    var premiumOverride by remember { mutableStateOf(subscriptionManager.getPremiumOverride() ?: false) }
    var showHandleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KanjiSageColors.DarkBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.profile_back),
                modifier = Modifier
                    .size(24.dp)
                    .focusBorder(CircleShape)
                    .clickable { onBackClick() },
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.profile_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = KanjiSageTypography.TitleSmall,
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAnonymous = (authState as? AuthState.SignedIn)?.isAnonymous ?: true
                    val handle = (authState as? AuthState.SignedIn)?.user?.handle

                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (handle != null) KanjiSageColors.LinkBlue else if (isAnonymous) KanjiSageColors.MutedBlueGrey else KanjiSageColors.BookmarkButtonBg),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = when {
                            handle != null -> handle.first().uppercase()
                            isAnonymous -> stringResource(R.string.profile_guest_initial)
                            authState is AuthState.SignedIn ->
                                (authState as AuthState.SignedIn).user.email?.firstOrNull()?.uppercase() ?: "?"
                            else -> "?"
                        }
                        Text(
                            text = initial,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = KanjiSageTypography.TitleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Primary display: handle > email > Guest
                        val displayTitle = when {
                            handle != null -> handle
                            !isAnonymous -> (authState as? AuthState.SignedIn)?.user?.email ?: stringResource(R.string.profile_linked_account)
                            else -> stringResource(R.string.profile_guest)
                        }
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Secondary info
                        val subtitle = when {
                            handle != null && !isAnonymous ->
                                (authState as? AuthState.SignedIn)?.user?.email ?: ""
                            handle != null -> stringResource(R.string.profile_change_name)
                            isAnonymous -> stringResource(R.string.profile_no_account)
                            else -> ""
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Subscription badge
                        val badgeColor = if (isPremium) KanjiSageColors.Primary else KanjiSageColors.DisabledGrey
                        val badgeText = if (isPremium) stringResource(R.string.profile_premium) else stringResource(R.string.profile_free)
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = KanjiSageTypography.LabelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Display name action
            val currentHandle = (authState as? AuthState.SignedIn)?.user?.handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusBorder()
                    .clickable { showHandleDialog = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentHandle != null) stringResource(R.string.profile_change_name) else stringResource(R.string.profile_set_name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentHandle ?: stringResource(R.string.profile_choose_name),
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

            // App Stats Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_app_stats),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val remaining = subscriptionManager.getRemainingScans(context)
                    val remainingText = if (isPremium) stringResource(R.string.profile_unlimited) else stringResource(R.string.profile_remaining_format, remaining, SubscriptionManager.FREE_SCAN_LIMIT)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.profile_remaining_scans), style = MaterialTheme.typography.bodyMedium)
                        Text(remainingText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Connected Apps (Ecosystem)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_connected_apps),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // KanjiJourney
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_kanjijourney),
                        description = stringResource(R.string.profile_app_kanjijourney_desc),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjijourney"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // TutoringJay
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_tutoringjay),
                        description = stringResource(R.string.profile_app_tutoringjay_desc),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tutoringjay.com"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // J Coin
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_jcoin),
                        description = stringResource(R.string.profile_app_jcoin_desc),
                        onClick = onRewardsClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Discord Community
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_discord),
                        description = stringResource(R.string.profile_app_discord_desc),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/bwHQA6GC"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // JWorks AI
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_jworks),
                        description = stringResource(R.string.profile_app_jworks_desc),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jworks-ai.com"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Creator
                    EcosystemAppRow(
                        name = stringResource(R.string.profile_app_creator),
                        description = stringResource(R.string.profile_app_creator_desc),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jayismocking.com"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Admin/Developer Tools (conditional)
            if (showDeveloperTools) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KanjiSageColors.WarningLightBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.profile_dev_tools),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KanjiSageColors.WarningDarkText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (premiumOverride) stringResource(R.string.profile_sim_premium) else stringResource(R.string.profile_sim_free),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.profile_override_desc),
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
                            text = if (premiumOverride) stringResource(R.string.profile_current_premium) else stringResource(R.string.profile_current_free),
                            style = MaterialTheme.typography.bodySmall,
                            color = KanjiSageColors.DangerText
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
                                    containerColor = if (premiumOverride) KanjiSageColors.Primary else KanjiSageColors.InactiveToggle
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.profile_premium), fontSize = KanjiSageTypography.LabelSmall)
                            }
                            Button(
                                onClick = {
                                    premiumOverride = false
                                    subscriptionManager.setPremiumOverride(false, context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!premiumOverride) KanjiSageColors.HudSlow else KanjiSageColors.InactiveToggle
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.profile_free), fontSize = KanjiSageTypography.LabelSmall)
                            }
                        }
                    }
                }
            }

            // Link Account / Sign Out
            val isAnon = (authState as? AuthState.SignedIn)?.isAnonymous ?: true
            if (isAnon) {
                Button(
                    onClick = onLinkAccountClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KanjiSageColors.LinkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_in_link),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.profile_sign_in_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (authState is AuthState.SignedIn) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KanjiSageColors.DangerButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_out),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Handle prompt dialog
    if (showHandleDialog) {
        HandlePromptDialog(
            onSave = { handle ->
                authRepository.setHandle(handle)
                showHandleDialog = false
            },
            onDismiss = { showHandleDialog = false }
        )
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
            .focusBorder()
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
