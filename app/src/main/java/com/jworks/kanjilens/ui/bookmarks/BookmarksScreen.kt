package com.jworks.kanjilens.ui.bookmarks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.jworks.kanjilens.R
import com.jworks.kanjilens.domain.models.BookmarkEntry
import com.jworks.kanjilens.domain.repository.BookmarkRepository
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1B1B1B)
private val CardBg = Color(0xFF2A2A2A)
private val TanAccent = Color(0xFFD4B896)
private val CreamText = Color(0xFFF5E6D3)
private val MutedText = Color(0xFF999999)
private val BookmarkGold = Color(0xFFFFB74D)
private val TabBg = Color(0xFF252525)

private fun BookmarkEntry.isKanji(): Boolean {
    return word.length == 1 && word[0].let { c ->
        c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
    }
}

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
            .background(DarkBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1B1B))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Bookmarks ($totalCount)",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TabBg,
            contentColor = BookmarkGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BookmarkGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Words (${wordBookmarks.size})",
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
                        text = "Kanji (${kanjiBookmarks.size})",
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
                        text = if (selectedTab == 0)
                            "Your word collection awaits!"
                        else
                            "Start your kanji collection!",
                        color = CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = if (selectedTab == 0)
                            "Scan some Japanese text, tap a word\nto see its meaning, then hit the\nbookmark icon to save it here."
                        else
                            "When you find an interesting kanji,\ntap the star to add it to your\npersonal collection.",
                        color = MutedText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (selectedTab == 0)
                            "Tip: Save words you see often — they're probably the most useful!"
                        else
                            "Tip: Collecting kanji earns you +2 J Coins each time!",
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

                // KanjiQuest promo card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    KanjiQuestPromoCard(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjiquest")
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
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
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
                text = formatRelativeTime(entry.bookmarkedAt),
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = "Remove bookmark",
            modifier = Modifier
                .size(20.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large kanji character
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3A3A3A)),
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
                text = formatRelativeTime(entry.bookmarkedAt),
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = "Remove bookmark",
            modifier = Modifier
                .size(20.dp)
                .clickable { onDelete() },
            tint = MutedText
        )
    }
}

@Composable
private fun KanjiQuestPromoCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Practice these kanji in KanjiQuest",
                color = Color(0xFF81C784),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AI checks your handwriting \u2022 Compete with other learners",
                color = Color(0xFF81C784).copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open on Play Store >",
                color = Color(0xFF4CAF50),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
