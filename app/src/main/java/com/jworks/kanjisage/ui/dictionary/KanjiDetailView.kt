package com.jworks.kanjisage.ui.dictionary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.domain.models.KanjiInfo
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.focusBorder
import com.jworks.kanjisage.ui.theme.KanjiSageTypography

// Match KanjiJourney theme colors exactly for visual consistency
private val OrangeBar = KanjiSageColors.AccentOrange
private val CreamBg = KanjiSageColors.KJCreamBg
private val SurfaceCard = Color.White
private val BookmarkGold = KanjiSageColors.KJBookmarkGold
private val GreenPractice = KanjiSageColors.Primary
private val DarkText = KanjiSageColors.KJDarkText
private val OrangePrimary = KanjiSageColors.AccentOrange
private val MutedText = KanjiSageColors.MutedTextDark

@Composable
fun KanjiDetailView(
    kanji: String,
    kanjiInfo: KanjiInfo?,
    isLoading: Boolean,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onKanjiClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.background(CreamBg)) {
        // Header bar — matches KanjiJourney TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangeBar)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2190",
                fontSize = KanjiSageTypography.TitleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.focusBorder().clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = kanjiInfo?.literal ?: kanji,
                fontSize = KanjiSageTypography.TitleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Star bookmark — same as KanjiJourney (★/☆)
            Text(
                text = if (isBookmarked) "\u2605" else "\u2606",
                fontSize = KanjiSageTypography.TitleMedium,
                color = if (isBookmarked) BookmarkGold else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .focusBorder()
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
                    fontSize = KanjiSageTypography.DisplayKanji,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(localeList = LocaleList("ja"))
                )

                // Info chips — same as KanjiJourney
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
                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.kanji_strokes, kanjiInfo.strokeCount), color = DarkText) })
                        }
                        kanjiInfo.frequency?.let {
                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.kanji_freq, it), color = DarkText) })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meanings — section card
                    if (kanjiInfo.meanings.isNotEmpty()) {
                        SectionCard(title = stringResource(R.string.kanji_meanings)) {
                            Text(
                                text = kanjiInfo.meanings.joinToString(", "),
                                fontSize = KanjiSageTypography.BodyMedium,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // On'yomi readings
                    if (kanjiInfo.onReadings.isNotEmpty()) {
                        SectionCard(title = stringResource(R.string.kanji_onyomi)) {
                            Text(
                                text = kanjiInfo.onReadings.joinToString("   "),
                                fontSize = KanjiSageTypography.BodyMedium,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Kun'yomi readings
                    if (kanjiInfo.kunReadings.isNotEmpty()) {
                        SectionCard(title = stringResource(R.string.kanji_kunyomi)) {
                            Text(
                                text = kanjiInfo.kunReadings.joinToString("   "),
                                fontSize = KanjiSageTypography.BodyMedium,
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Kyujitai / Shinjitai variant section
                    if (kanjiInfo.hasVariant) {
                        val isTraditional = kanjiInfo.isKyujitai
                        val title = if (isTraditional)
                            stringResource(R.string.kanji_modern_form)
                        else
                            stringResource(R.string.kanji_traditional_form)
                        val variants = if (isTraditional)
                            listOfNotNull(kanjiInfo.shinjitaiVariant)
                        else
                            kanjiInfo.kyujitaiVariants

                        SectionCard(title = title) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                variants.forEach { variant ->
                                    AssistChip(
                                        onClick = { onKanjiClick(variant) },
                                        label = {
                                            Text(
                                                text = variant,
                                                fontSize = KanjiSageTypography.TitleSmall,
                                                color = DarkText,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    localeList = LocaleList("ja")
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Practice section — matches KanjiJourney Writing button
                SectionCard(title = stringResource(R.string.kanji_practice)) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjijourney")
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
                                text = stringResource(R.string.kanji_practice_writing, kanji),
                                fontSize = KanjiSageTypography.Label,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.kanji_ai_handwriting),
                                fontSize = KanjiSageTypography.LabelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Jisho.org link
                Text(
                    text = stringResource(R.string.kanji_more_jisho),
                    fontSize = KanjiSageTypography.Label,
                    color = KanjiSageColors.LinkBlue,
                    modifier = Modifier.focusBorder().clickable {
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
                fontSize = KanjiSageTypography.BodySmall,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
