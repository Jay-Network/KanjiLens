package com.jworks.kanjilens.ui.promo

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Cross-promotion banner for KanjiQuest, shown periodically in KanjiLens.
 * Non-intrusive: small banner at bottom, auto-dismisses after 8 seconds.
 */
@Composable
fun KanjiQuestPromoBanner(
    currentKanji: String?,
    scanCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }

    // Show banner after every 10th scan, with a specific kanji in context
    LaunchedEffect(scanCount) {
        if (scanCount > 0 && scanCount % 10 == 0 && !isDismissed) {
            delay(2000) // Wait for user to see scan results first
            isVisible = true
            delay(8000) // Auto-dismiss after 8 seconds
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && !isDismissed,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF8C42).copy(alpha = 0.95f))
                .clickable {
                    // Try to open KanjiQuest, fall back to Play Store
                    val intent = context.packageManager
                        .getLaunchIntentForPackage("com.jworks.kanjiquest")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        val storeIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjiquest")
                        )
                        context.startActivity(storeIntent)
                    }
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentKanji != null) {
                            "Master \"$currentKanji\" with KanjiQuest!"
                        } else {
                            "Master kanji with KanjiQuest!"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gamified learning + earn J Coins",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                TextButton(
                    onClick = {
                        isDismissed = true
                        isVisible = false
                    }
                ) {
                    Text(
                        text = "\u2715",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
