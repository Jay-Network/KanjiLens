package com.jworks.kanjilens.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBg = Color(0xFF000000)
private val TealAccent = Color(0xFF0D9488)

@Composable
fun SplashScreen(
    onSplashFinished: (navigateToOnboarding: Boolean) -> Unit,
    hasSeenOnboarding: Boolean?
) {
    // Animation values
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val shimmerOffset = remember { Animatable(-1f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val fadeOut = remember { Animatable(1f) }

    LaunchedEffect(hasSeenOnboarding) {
        // Wait until DataStore has loaded (null = still loading)
        if (hasSeenOnboarding == null) return@LaunchedEffect

        // Phase 1: Logo fade in + scale (0-800ms)
        launch { logoAlpha.animateTo(1f, tween(800)) }
        launch { logoScale.animateTo(1f, tween(800, easing = EaseOutBack)) }
        delay(800)

        // Phase 2: Shimmer sweep (800-1400ms)
        shimmerOffset.animateTo(2f, tween(600, easing = LinearEasing))
        // shimmer ends at 1400ms

        // Phase 3: Title fade in (1400-1900ms)
        titleAlpha.animateTo(1f, tween(500))
        // title done at 1900ms

        // Phase 4: Subtitle fade in (1900-2300ms)
        subtitleAlpha.animateTo(1f, tween(400))
        // subtitle done at 2300ms

        // Phase 5: Hold (600ms)
        delay(600)

        // Phase 6: Fade out (700ms)
        fadeOut.animateTo(0f, tween(700))

        // Navigate
        onSplashFinished(!hasSeenOnboarding)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBg)
            .graphicsLayer { alpha = fadeOut.value },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with shimmer
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_jworks_logo),
                    contentDescription = "JWorks Logo",
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer {
                            alpha = logoAlpha.value
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .then(ShimmerElement(shimmerOffset.value))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "KanjiLens" title
            Text(
                text = "KanjiLens",
                color = TealAccent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "by JWorks" subtitle
            Text(
                text = "by JWorks",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}

// Shimmer overlay using Modifier.Node API (Compose 1.6 compatible)
private class ShimmerNode(var offset: Float) : DrawModifierNode, Modifier.Node() {
    override fun ContentDrawScope.draw() {
        drawContent()
        if (offset > -1f) {
            val shimmerWidth = size.width * 0.5f
            val x = size.width * offset
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(x - shimmerWidth, 0f),
                    end = Offset(x + shimmerWidth, 0f)
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
        }
    }
}

private data class ShimmerElement(val offset: Float) : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode(offset)
    override fun update(node: ShimmerNode) {
        node.offset = offset
    }
}
