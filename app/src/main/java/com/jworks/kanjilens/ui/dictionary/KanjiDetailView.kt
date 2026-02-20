package com.jworks.kanjilens.ui.dictionary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.domain.models.KanjiInfo

// Match KanjiQuest theme colors exactly for visual consistency
private val OrangeBar = Color(0xFFFF8C42)       // KanjiQuest primary
private val CreamBg = Color(0xFFFFF8E1)         // KanjiQuest Cream background
private val SurfaceCard = Color.White            // KanjiQuest surface
private val BookmarkGold = Color(0xFFFFD700)     // KanjiQuest star gold
private val GreenPractice = Color(0xFF4CAF50)    // KanjiQuest Writing button
private val DarkText = Color(0xFF1C1B1F)         // KanjiQuest onBackground
private val OrangePrimary = Color(0xFFFF8C42)    // KanjiQuest primary for titles
private val MutedText = Color(0xFF49454F)        // KanjiQuest onSurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KanjiDetailView(
    kanji: String,
    kanjiInfo: KanjiInfo?,
    isLoading: Boolean,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.background(CreamBg)) {
        // Header bar — matches KanjiQuest TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangeBar)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2190",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = kanjiInfo?.literal ?: kanji,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            // Star bookmark — same as KanjiQuest (★/☆)
            Text(
                text = if (isBookmarked) "\u2605" else "\u2606",
                fontSize = 24.sp,
                color = if (isBookmarked) BookmarkGold else Color.White,
                modifier = Modifier
                    .clickable { onBookmarkToggle() }
                    .padding(4.dp)
            )
        }

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = OrangeBar, strokeWidth = 2.dp)
            }
        } else {
            // Content — scrollable body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large kanji display — scaled down in vertical mode to save space
                Text(
                    text = kanji,
                    fontSize = 80.sp,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(localeList = LocaleList("ja"))
                )

                // Info chips — same as KanjiQuest
                if (kanjiInfo != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        kanjiInfo.gradeLabel?.let {
                            AssistChip(onClick = {}, label = { Text(it, color = DarkText) })
                        }
                        kanjiInfo.jlptLabel?.let {
                            AssistChip(onClick = {}, label = { Text(it, color = DarkText) })
                        }
                        if (kanjiInfo.strokeCount > 0) {
                            AssistChip(onClick = {}, label = { Text("${kanjiInfo.strokeCount} strokes", color = DarkText) })
                        }
                        kanjiInfo.frequency?.let {
                            AssistChip(onClick = {}, label = { Text("Freq #$it", color = DarkText) })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meanings — section card
                    if (kanjiInfo.meanings.isNotEmpty()) {
                        SectionCard(title = "Meanings") {
                            Text(
                                text = kanjiInfo.meanings.joinToString(", "),
                                fontSize = 15.sp,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // On'yomi readings
                    if (kanjiInfo.onReadings.isNotEmpty()) {
                        SectionCard(title = "On'yomi (Chinese readings)") {
                            Text(
                                text = kanjiInfo.onReadings.joinToString("   "),
                                fontSize = 15.sp,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Kun'yomi readings
                    if (kanjiInfo.kunReadings.isNotEmpty()) {
                        SectionCard(title = "Kun'yomi (Japanese readings)") {
                            Text(
                                text = kanjiInfo.kunReadings.joinToString("   "),
                                fontSize = 15.sp,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Practice section — matches KanjiQuest Writing button
                SectionCard(title = "Practice") {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjiquest")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPractice)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Practice writing $kanji in KanjiQuest",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI checks your handwriting",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Jisho.org link
                Text(
                    text = "More on Jisho.org",
                    fontSize = 13.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://jisho.org/search/${kanji}%20%23kanji")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
