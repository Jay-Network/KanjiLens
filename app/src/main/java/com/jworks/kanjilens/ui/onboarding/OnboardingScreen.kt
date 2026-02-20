package com.jworks.kanjilens.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.ui.theme.KanjiLensColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage(
        symbol = "\u6F22\u5B57",  // 漢字
        title = "See Japanese Everywhere",
        subtitle = "Real-time kanji recognition",
        description = "Point your camera at any Japanese text\nand instantly see readings and meanings\nright on top of the text.",
        accentColor = Color(0xFF4FC3F7)
    ),
    OnboardingPage(
        symbol = "\uD83D\uDD0D",
        title = "Tap. Learn. Remember.",
        subtitle = "215,000+ dictionary entries",
        description = "Tap any detected word for its full definition.\nSave your favorites to study later.\nWorks completely offline.",
        accentColor = Color(0xFF66BB6A)
    ),
    OnboardingPage(
        symbol = "J_COIN",
        title = "Earn J Coins",
        subtitle = "Real rewards for learning",
        description = "Every scan, every lookup, every streak\nearns you J Coins. Redeem them for\ntutoring sessions and app credits.",
        accentColor = KanjiLensColors.CoinGold
    ),
    OnboardingPage(
        symbol = "\uD83D\uDE80",
        title = "Ready to Scan!",
        subtitle = "5 free scans daily, no sign-up needed",
        description = "Start scanning right now.\nCreate an account to earn J Coins,\nsync your bookmarks, and go unlimited.",
        accentColor = Color(0xFFFF4081)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF121212),
                        Color(0xFF0D3B66)
                    )
                )
            )
    ) {
        // Skip button (top-right)
        if (!isLastPage) {
            TextButton(
                onClick = onComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                // Calculate page offset for parallax
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue

                val pageAlpha by animateFloatAsState(
                    targetValue = if (pageOffset < 0.5f) 1f else 0.5f,
                    animationSpec = tween(300),
                    label = "pageAlpha"
                )
                val pageScale by animateFloatAsState(
                    targetValue = if (pageOffset < 0.5f) 1f else 0.85f,
                    animationSpec = tween(300),
                    label = "pageScale"
                )

                OnboardingPageContent(
                    page = pages[page],
                    alpha = pageAlpha,
                    scale = pageScale
                )
            }

            // Page indicators with animated widths
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val indicatorWidth by animateFloatAsState(
                        targetValue = if (isSelected) 24f else 8f,
                        animationSpec = tween(300),
                        label = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(indicatorWidth.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) pages[pagerState.currentPage].accentColor
                                else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Bottom button
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isLastPage) "Start Scanning" else "Next",
                    color = if (isLastPage) Color.White else Color(0xFF121212),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    alpha: Float,
    scale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .alpha(alpha)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large symbol with accent glow background
        Box(contentAlignment = Alignment.Center) {
            // Glow circle behind symbol
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.1f))
            )
            if (page.symbol == "J_COIN") {
                // Custom J Coin branded symbol
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color(0xFFFFA726),
                                    Color(0xFFFF8F00)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "J",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = page.symbol,
                    fontSize = 64.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = page.title,
            fontSize = 28.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle with accent color
        Text(
            text = page.subtitle,
            fontSize = 15.sp,
            color = page.accentColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
