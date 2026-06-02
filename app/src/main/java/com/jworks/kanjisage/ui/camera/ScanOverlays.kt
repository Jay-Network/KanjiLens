package com.jworks.kanjisage.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.domain.models.ScanChallenge
import com.jworks.kanjisage.ui.anim.CoinBurstEffect
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.focusBorder
import com.jworks.kanjisage.ui.theme.KanjiSageTypography
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

/**
 * Scan timer pill displayed for free-tier users during an active scan.
 */
@Composable
fun ScanTimerPill(
    scanTimerSeconds: Int,
    modifier: Modifier = Modifier
) {
    val timerColor = if (scanTimerSeconds <= 10) KanjiSageColors.TimerWarning else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .clip(KanjiSageShapes.Large)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${scanTimerSeconds / 60}:${String.format("%02d", scanTimerSeconds % 60)}",
            color = timerColor,
            fontSize = KanjiSageTypography.Body,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Scan challenge pill showing target kanji to find.
 */
@Composable
fun ScanChallengePill(
    challenge: ScanChallenge,
    onNextChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(KanjiSageShapes.Large)
            .background(
                if (challenge.isCompleted) KanjiSageColors.ChallengeComplete.copy(alpha = 0.85f)
                else KanjiSageColors.ChallengeActive.copy(alpha = 0.85f)
            )
            .focusBorder(KanjiSageShapes.Large)
            .clickable { if (challenge.isCompleted) onNextChallenge() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (challenge.isCompleted) {
                    Text(
                        text = stringResource(R.string.scan_challenge_found, challenge.targetKanji),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = KanjiSageTypography.BodySmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.scan_challenge_find),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = KanjiSageTypography.Label
                    )
                    Text(
                        text = challenge.targetKanji,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = KanjiSageTypography.Title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " (${challenge.reading})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = KanjiSageTypography.LabelSmall
                    )
                }
            }
            if (challenge.isCompleted) {
                Text(
                    text = stringResource(R.string.scan_challenge_promo),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = KanjiSageTypography.LabelSmall
                )
            }
        }
    }
}

/**
 * Toast showing J Coin reward with bounce-in animation and coin burst particles.
 */
@Composable
fun CoinRewardToast(
    message: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        visible = true
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = EaseOutBack)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Coin burst particles behind the toast
        CoinBurstEffect(modifier = Modifier.size(120.dp))

        // The toast pill itself
        Row(
            modifier = Modifier
                .scale(scale.value)
                .clip(KanjiSageShapes.ExtraLarge)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            KanjiSageColors.CoinToastBackground,
                            KanjiSageColors.CoinGradientEnd
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin icon circle
            Box(
                modifier = Modifier
                    .size(28.dp)
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
                    fontSize = KanjiSageTypography.BodySmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = KanjiSageColors.CoinGold,
                fontSize = KanjiSageTypography.BodyAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Full-screen overlay shown when free scan timer expires.
 */
@Composable
fun ScanExpiredOverlay(
    onStartNewScan: () -> Unit,
    onBackToCamera: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.scan_expired_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = KanjiSageTypography.Title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.scan_expired_desc),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = KanjiSageTypography.BodySmall
            )

            Button(
                onClick = onStartNewScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KanjiSageColors.PrimaryAction
                ),
                shape = KanjiSageShapes.ExtraLarge
            ) {
                Text(
                    text = stringResource(R.string.scan_expired_new),
                    fontSize = KanjiSageTypography.Body,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            TextButton(onClick = onUpgrade) {
                Text(
                    text = stringResource(R.string.scan_expired_premium),
                    color = KanjiSageColors.PrimaryAction,
                    fontSize = KanjiSageTypography.BodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(onClick = onBackToCamera) {
                Text(
                    text = stringResource(R.string.scan_expired_review),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = KanjiSageTypography.Label
                )
            }
        }
    }
}
