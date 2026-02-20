package com.jworks.kanjilens.ui.camera

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.jworks.kanjilens.domain.models.ScanChallenge
import com.jworks.kanjilens.ui.anim.CoinBurstEffect
import com.jworks.kanjilens.ui.theme.KanjiLensColors

/**
 * Scan timer pill displayed for free-tier users during an active scan.
 */
@Composable
fun ScanTimerPill(
    scanTimerSeconds: Int,
    modifier: Modifier = Modifier
) {
    val timerColor = if (scanTimerSeconds <= 10) KanjiLensColors.TimerWarning else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${scanTimerSeconds / 60}:${String.format("%02d", scanTimerSeconds % 60)}",
            color = timerColor,
            fontSize = 16.sp,
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
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (challenge.isCompleted) KanjiLensColors.ChallengeComplete.copy(alpha = 0.85f)
                else KanjiLensColors.ChallengeActive.copy(alpha = 0.85f)
            )
            .clickable { if (challenge.isCompleted) onNextChallenge() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (challenge.isCompleted) {
                    Text(
                        text = "Found ${challenge.targetKanji}! +10J  Tap for next",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Find: ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = challenge.targetKanji,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " (${challenge.reading})",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            if (challenge.isCompleted) {
                Text(
                    text = "Love this? Try Camera Challenge in KanjiQuest!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
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
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            KanjiLensColors.CoinToastBackground,
                            Color(0xFF1565C0)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = KanjiLensColors.CoinGold,
                fontSize = 17.sp,
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
                text = "Time's Up!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your free scan session has ended",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Button(
                onClick = onStartNewScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KanjiLensColors.PrimaryAction
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Use Another Free Scan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            TextButton(onClick = onUpgrade) {
                Text(
                    text = "Go Unlimited with Premium",
                    color = KanjiLensColors.PrimaryAction,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(onClick = onBackToCamera) {
                Text(
                    text = "Review detected words",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
