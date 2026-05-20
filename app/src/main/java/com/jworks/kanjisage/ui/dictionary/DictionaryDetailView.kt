package com.jworks.kanjisage.ui.dictionary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.ui.theme.focusBorder
import com.jworks.kanjisage.domain.models.DictionaryResult

// POS abbreviation → display label
private val POS_LABELS = mapOf(
    "n" to "Noun",
    "n,vs" to "Noun (suru)",
    "n,suf" to "Noun (suffix)",
    "n,pref" to "Noun (prefix)",
    "n,temp" to "Noun (temporal)",
    "n,adv" to "Adverbial noun",
    "n,prop" to "Proper noun",
    "v1" to "Ichidan verb",
    "v5u" to "Godan verb (u)",
    "v5k" to "Godan verb (ku)",
    "v5s" to "Godan verb (su)",
    "v5t" to "Godan verb (tsu)",
    "v5n" to "Godan verb (nu)",
    "v5b" to "Godan verb (bu)",
    "v5m" to "Godan verb (mu)",
    "v5r" to "Godan verb (ru)",
    "v5g" to "Godan verb (gu)",
    "v5k-s" to "Godan verb (iku)",
    "v5r-i" to "Godan verb (irregular)",
    "vs-i" to "Suru verb",
    "vs-s" to "Suru verb (special)",
    "vk" to "Kuru verb",
    "vi" to "Intransitive",
    "vt" to "Transitive",
    "adj-i" to "i-adjective",
    "adj-na" to "na-adjective",
    "adj-no" to "no-adjective",
    "adj-t" to "taru-adjective",
    "adj-pn" to "Pre-noun adj.",
    "adj-ix" to "i-adjective (ii)",
    "adv" to "Adverb",
    "adv-to" to "Adverb (to)",
    "conj" to "Conjunction",
    "int" to "Interjection",
    "exp" to "Expression",
    "pref" to "Prefix",
    "suf" to "Suffix",
    "prt" to "Particle",
    "v,aux" to "Auxiliary verb",
    "adj,aux" to "Auxiliary adj.",
    "ctr" to "Counter",
    "num" to "Numeric",
    "pn" to "Pronoun",
    "cop" to "Copula",
)

@Composable
fun DictionaryDetailView(
    result: DictionaryResult?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    wordText: String = "",
    wordReading: String = "",
    isWordBookmarked: Boolean = false,
    onWordBookmarkToggle: () -> Unit = {},
    bookmarkedKanji: Set<String> = emptySet(),
    onKanjiClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayWord = result?.word ?: wordText
    val displayReading = result?.reading ?: wordReading

    Column(modifier = modifier.background(KanjiSageColors.PanelBackground)) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KanjiSageColors.PanelBorder)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.dict_back),
                modifier = Modifier
                    .size(24.dp)
                    .focusBorder(CircleShape)
                    .clickable { onBackClick() },
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))

            if (displayWord.isNotEmpty()) {
                Text(
                    text = displayWord,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (displayReading.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayReading,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
                if (result?.isCommon == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(KanjiSageColors.Primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dict_common),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(
                        id = if (isWordBookmarked) R.drawable.ic_bookmark_filled
                        else R.drawable.ic_bookmark_border
                    ),
                    contentDescription = stringResource(if (isWordBookmarked) R.string.dict_remove_bookmark else R.string.dict_bookmark_word),
                    modifier = Modifier
                        .size(28.dp)
                        .focusBorder(CircleShape)
                        .clickable { onWordBookmarkToggle() }
                        .padding(2.dp),
                    tint = if (isWordBookmarked) KanjiSageColors.CoinGold else MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(R.string.dict_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = KanjiSageColors.PanelBorder,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (result == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dict_no_definition),
                    fontSize = 16.sp,
                    color = KanjiSageColors.JukugoSecondary
                )

                // Still show kanji breakdown for words without dictionary entries
                val fallbackKanji = displayWord.filter { c ->
                    c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
                }
                if (fallbackKanji.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.dict_kanji_section),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KanjiSageColors.JukugoSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fallbackKanji.forEach { ch ->
                            val kanjiStr = ch.toString()
                            val isSaved = kanjiStr in bookmarkedKanji
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSaved) KanjiSageColors.WarningLightBg else KanjiSageColors.PanelItemBackground)
                                    .focusBorder(RoundedCornerShape(8.dp))
                                    .clickable { onKanjiClick(kanjiStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = kanjiStr,
                                    fontSize = 26.sp,
                                    color = if (isSaved) KanjiSageColors.BookmarkedKanjiHighlight else KanjiSageColors.JukugoText,
                                    fontWeight = if (isSaved) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // KanjiJourney promo for kanji practice
                if (fallbackKanji.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    KanjiJourneyPracticeButton(kanjiList = fallbackKanji.toList())
                }

                // Jisho.org link
                Spacer(modifier = Modifier.height(24.dp))
                val context = LocalContext.current
                Text(
                    text = stringResource(R.string.dict_search_jisho),
                    fontSize = 13.sp,
                    color = KanjiSageColors.LinkBlue,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .focusBorder()
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://jisho.org/search/${displayWord}")
                            )
                            context.startActivity(intent)
                        }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Senses
                var senseNumber = 1
                var lastPosGroup = ""

                result.senses.forEach { sense ->
                    val posGroup = sense.partOfSpeech.joinToString(", ")

                    // Show POS header when it changes
                    if (posGroup.isNotEmpty() && posGroup != lastPosGroup) {
                        if (senseNumber > 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            sense.partOfSpeech.forEach { pos ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(KanjiSageColors.PosTagBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = POS_LABELS[pos] ?: pos,
                                        fontSize = 12.sp,
                                        color = KanjiSageColors.JukugoText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        lastPosGroup = posGroup
                    }

                    // Numbered gloss
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = KanjiSageColors.PanelBorder, fontWeight = FontWeight.Bold)) {
                                append("$senseNumber. ")
                            }
                            append(sense.glosses.joinToString("; "))
                        },
                        fontSize = 15.sp,
                        color = KanjiSageColors.JukugoText,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    senseNumber++
                }

                // Kanji breakdown
                val kanjiChars = result.word.filter { c ->
                    c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
                }
                if (kanjiChars.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.dict_kanji_section),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KanjiSageColors.JukugoSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        kanjiChars.forEach { ch ->
                            val kanjiStr = ch.toString()
                            val isSaved = kanjiStr in bookmarkedKanji
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSaved) KanjiSageColors.WarningLightBg else KanjiSageColors.PanelItemBackground)
                                    .focusBorder(RoundedCornerShape(8.dp))
                                    .clickable { onKanjiClick(kanjiStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = kanjiStr,
                                    fontSize = 26.sp,
                                    color = if (isSaved) KanjiSageColors.BookmarkedKanjiHighlight else KanjiSageColors.JukugoText,
                                    fontWeight = if (isSaved) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // KanjiJourney promo for kanji practice
                if (kanjiChars.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    KanjiJourneyPracticeButton(kanjiList = kanjiChars.toList())
                }

                // Jisho.org link
                Spacer(modifier = Modifier.height(24.dp))
                val context = LocalContext.current
                Text(
                    text = stringResource(R.string.dict_more_jisho),
                    fontSize = 13.sp,
                    color = KanjiSageColors.LinkBlue,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .focusBorder()
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://jisho.org/search/${result.word}")
                            )
                            context.startActivity(intent)
                        }
                )
            }
        }
    }
}

@Composable
private fun KanjiJourneyPracticeButton(kanjiList: List<Char>) {
    val context = LocalContext.current
    val kanjiCsv = kanjiList.joinToString(",")
    val displayKanji = if (kanjiList.size <= 3) kanjiCsv else "${kanjiList.take(3).joinToString(",")}…"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KanjiSageColors.Primary)
            .focusBorder(RoundedCornerShape(10.dp))
            .clickable {
                val deepLink = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("kanjijourney://import?source=kanjisage&kanji=$kanjiCsv")
                )
                if (deepLink.resolveActivity(context.packageManager) != null) {
                    context.startActivity(deepLink)
                } else {
                    val storeIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjijourney")
                    )
                    context.startActivity(storeIntent)
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.dict_send_to_kj, displayKanji),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.dict_kj_flashcards),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Text(
            text = ">",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
