package com.jworks.kanjisage.ui.bookmarks

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.R
import com.jworks.kanjisage.domain.models.BookmarkEntry
import com.jworks.kanjisage.domain.repository.BookmarkRepository
import com.jworks.kanjisage.ui.theme.GlassCard
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import com.jworks.kanjisage.ui.theme.glassSurface
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

private val TanAccent = KanjiSageColors.PanelBorder
private val CreamText = KanjiSageColors.PanelBackground
private val MutedText = KanjiSageColors.MutedText
private val BookmarkGold = KanjiSageColors.CoinAccent
private val TabBg = KanjiSageColors.TabBg

private fun BookmarkEntry.isKanji(): Boolean {
    return word.length == 1 && word[0].let { c ->
        c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarkRepository: BookmarkRepository,
    onBackClick: () -> Unit,
    onWordClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf<List<BookmarkEntry>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        bookmarks = bookmarkRepository.getAll()
    }

    val kanjiBookmarks = bookmarks.filter { it.isKanji() }
    val wordBookmarks = bookmarks.filter { !it.isKanji() }
    val totalCount = bookmarks.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KanjiSageColors.DarkBg)
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
                contentDescription = stringResource(R.string.bookmarks_back),
                modifier = Modifier
                    .size(24.dp)
                    .focusBorder(CircleShape)
                    .clickable { onBackClick() },
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.bookmarks_title, totalCount),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = TabBg,
            contentColor = BookmarkGold,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    color = BookmarkGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = stringResource(R.string.bookmarks_tab_words, wordBookmarks.size),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) BookmarkGold else MutedText
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = stringResource(R.string.bookmarks_tab_kanji, kanjiBookmarks.size),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) BookmarkGold else MutedText
                    )
                }
            )
        }

        val currentList = if (selectedTab == 0) wordBookmarks else kanjiBookmarks

        if (currentList.isEmpty()) {
            // Encouraging empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Large illustrative emoji
                    Text(
                        text = if (selectedTab == 0) "\uD83D\uDCDA" else "\u2728",
                        fontSize = 56.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            if (selectedTab == 0) R.string.bookmarks_word_awaits
                            else R.string.bookmarks_kanji_awaits
                        ),
                        color = CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(
                            if (selectedTab == 0) R.string.bookmarks_word_hint
                            else R.string.bookmarks_kanji_hint
                        ),
                        color = MutedText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            if (selectedTab == 0) R.string.bookmarks_word_tip
                            else R.string.bookmarks_kanji_tip
                        ),
                        color = BookmarkGold.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentList, key = { it.word }) { entry ->
                    if (entry.isKanji()) {
                        KanjiBookmarkRow(
                            entry = entry,
                            onClick = { onWordClick(entry.word) },
                            onDelete = {
                                scope.launch {
                                    bookmarkRepository.delete(entry.word)
                                    bookmarks = bookmarkRepository.getAll()
                                }
                            }
                        )
                    } else {
                        WordBookmarkRow(
                            entry = entry,
                            onClick = { onWordClick(entry.word) },
                            onDelete = {
                                scope.launch {
                                    bookmarkRepository.delete(entry.word)
                                    bookmarks = bookmarkRepository.getAll()
                                }
                            }
                        )
                    }
                }

                // KanjiJourney promo card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    KanjiJourneyPromoCard(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjijourney")
                            )
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WordBookmarkRow(
    entry: BookmarkEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(12.dp))
            .focusBorder(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.word,
                color = CreamText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (entry.reading.isNotEmpty()) {
                Text(
                    text = entry.reading,
                    color = TanAccent,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = formatRelativeTime(LocalContext.current, entry.bookmarkedAt),
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = stringResource(R.string.bookmarks_remove),
            modifier = Modifier
                .size(20.dp)
                .focusBorder(CircleShape)
                .clickable { onDelete() },
            tint = MutedText
        )
    }
}

@Composable
private fun KanjiBookmarkRow(
    entry: BookmarkEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(12.dp))
            .focusBorder(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large kanji character
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KanjiSageColors.CardBgLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.word,
                color = BookmarkGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.word,
                color = CreamText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatRelativeTime(LocalContext.current, entry.bookmarkedAt),
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = stringResource(R.string.bookmarks_remove),
            modifier = Modifier
                .size(20.dp)
                .focusBorder(CircleShape)
                .clickable { onDelete() },
            tint = MutedText
        )
    }
}

@Composable
private fun KanjiJourneyPromoCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .focusBorder(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        fillColor = KanjiSageColors.PromoCardBg,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.bookmarks_practice),
                color = KanjiSageColors.PromoCardText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.bookmarks_practice_desc),
                color = KanjiSageColors.PromoCardText.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.bookmarks_play_store),
                color = KanjiSageColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

private fun formatRelativeTime(context: android.content.Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> context.getString(R.string.bookmarks_days_ago, days.toInt())
        hours > 0 -> context.getString(R.string.bookmarks_hours_ago, hours.toInt())
        minutes > 0 -> context.getString(R.string.bookmarks_minutes_ago, minutes.toInt())
        else -> context.getString(R.string.bookmarks_just_now)
    }
}
