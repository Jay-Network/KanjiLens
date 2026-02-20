package com.jworks.kanjilens.ui.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.jworks.kanjilens.ui.theme.KanjiLensColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated coin burst effect — particles radiate from center when coins are earned.
 */
@Composable
fun CoinBurstEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 8
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
        )
    }

    val particles = remember {
        List(particleCount) {
            val angle = (2 * PI * it / particleCount).toFloat()
            val speed = 0.6f + Random.nextFloat() * 0.4f
            CoinParticle(angle = angle, speed = speed)
        }
    }

    Canvas(modifier = modifier.size(120.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2

        particles.forEach { particle ->
            val dist = maxRadius * progress.value * particle.speed
            val x = centerX + cos(particle.angle) * dist
            val y = centerY + sin(particle.angle) * dist
            val particleSize = 6.dp.toPx() * (1f - progress.value * 0.5f)
            val alpha = 1f - progress.value

            drawCircle(
                color = KanjiLensColors.CoinGold.copy(alpha = alpha),
                radius = particleSize,
                center = Offset(x, y)
            )
            // Inner shine
            drawCircle(
                color = KanjiLensColors.CoinShine.copy(alpha = alpha * 0.6f),
                radius = particleSize * 0.5f,
                center = Offset(x, y)
            )
        }
    }
}

private data class CoinParticle(val angle: Float, val speed: Float)

/**
 * Animated streak flame icon — flickers and glows for active streaks.
 */
@Composable
fun StreakFlameIcon(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")

    val flickerScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerScale"
    )

    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    val flameColor = when {
        streakDays >= 30 -> KanjiLensColors.StreakFlameHot
        streakDays >= 7 -> KanjiLensColors.StreakFlame
        else -> KanjiLensColors.StreakFlameLight
    }

    Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width
        val h = size.height
        val scale = flickerScale

        // Outer glow
        drawCircle(
            color = flameColor.copy(alpha = flickerAlpha * 0.2f),
            radius = w * 0.5f * scale,
            center = Offset(w / 2, h * 0.55f)
        )
        // Main flame body (teardrop approximation with circles)
        drawFlameShape(
            color = flameColor.copy(alpha = flickerAlpha * 0.9f),
            centerX = w / 2,
            bottomY = h * 0.85f,
            width = w * 0.4f * scale,
            height = h * 0.65f * scale
        )
        // Inner bright core
        drawFlameShape(
            color = KanjiLensColors.CoinGold.copy(alpha = flickerAlpha * 0.8f),
            centerX = w / 2,
            bottomY = h * 0.8f,
            width = w * 0.2f * scale,
            height = h * 0.35f * scale
        )
    }
}

private fun DrawScope.drawFlameShape(
    color: Color,
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float
) {
    // Approximate a flame with overlapping circles
    val tipY = bottomY - height
    val midY = bottomY - height * 0.4f

    // Tip (small)
    drawCircle(
        color = color,
        radius = width * 0.4f,
        center = Offset(centerX, tipY + width * 0.3f)
    )
    // Middle (wide)
    drawCircle(
        color = color,
        radius = width,
        center = Offset(centerX, midY)
    )
    // Base (medium)
    drawCircle(
        color = color,
        radius = width * 0.7f,
        center = Offset(centerX, bottomY - width * 0.5f)
    )
}

/**
 * Confetti burst effect for major achievements (30-day streak, milestones).
 */
@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    particleCount: Int = 20
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic)
        )
    }

    val confettiColors = listOf(
        KanjiLensColors.ConfettiPink,
        KanjiLensColors.ConfettiBlue,
        KanjiLensColors.ConfettiYellow,
        KanjiLensColors.ConfettiGreen,
        KanjiLensColors.CoinGold
    )

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                angle = Random.nextFloat() * 2 * PI.toFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                rotationSpeed = Random.nextFloat() * 360f,
                color = confettiColors[it % confettiColors.size],
                size = 4f + Random.nextFloat() * 4f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension * 0.4f

        particles.forEach { particle ->
            val t = progress.value
            val dist = maxRadius * t * particle.speed
            val gravity = t * t * 40f  // gravity pull down
            val x = centerX + cos(particle.angle) * dist
            val y = centerY + sin(particle.angle) * dist + gravity
            val alpha = (1f - t).coerceAtLeast(0f)
            val rectSize = particle.size.dp.toPx() * (1f - t * 0.3f)

            rotate(degrees = particle.rotationSpeed * t, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - rectSize / 2, y - rectSize / 2),
                    size = androidx.compose.ui.geometry.Size(rectSize, rectSize)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float
)

/**
 * Pulsing glow effect for the J Coin button on camera screen.
 */
@Composable
fun CoinPulseGlow(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coinPulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 2

        drawCircle(
            color = KanjiLensColors.CoinGold.copy(alpha = pulseAlpha),
            radius = baseRadius * pulseScale,
            center = center
        )
    }
}

/**
 * Animated number counter that counts up to the target value.
 */
@Composable
fun rememberAnimatedCount(targetValue: Int): Int {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = 600,
                easing = EaseOutBack
            )
        )
    }

    return animatable.value.toInt()
}
