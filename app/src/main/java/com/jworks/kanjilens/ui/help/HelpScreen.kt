package com.jworks.kanjilens.ui.help

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.BuildConfig
import com.jworks.kanjilens.R

private val AccentBlue = Color(0xFF4FC3F7)
private val AccentTeal = Color(0xFF0D9488)
private val AccentOrange = Color(0xFFFF8C42)
private val BadgeBg = Color(0xFF2A2A2A)
private val RowLabelBg = Color(0xFF1B3A4B)

@Composable
fun HelpScreen(
    onBackClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                text = "Help & About",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\u6F22\u5B57",  // 漢字
                        fontSize = 48.sp,
                        color = Color(0xFF4FC3F7),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KanjiLens",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "by JWorks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // User Guide header
            Text(
                text = "User Guide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Button Grid Guide
            GuideSection("Camera Buttons", "\u2328") {
                RowLabel("Top Row")
                ButtonTip("FULL / 25%", "Toggle between full-screen camera and partial mode (camera strip + word list).")
                ButtonTip("\u7e26 / \u6a2a", "Switch between vertical and horizontal text detection mode.")
                ButtonTip("\u25b6 / \u23f8", "Freeze or resume the camera scanning.")
                Spacer(modifier = Modifier.height(10.dp))
                RowLabel("Middle Row")
                ButtonTip("Flash", "Toggle the flashlight on/off.")
                ButtonTip("Settings", "Adjust furigana size, colors, stroke width, and more.")
                ButtonTip("Profile", "View your account, sign in/out, and access ecosystem apps.")
                Spacer(modifier = Modifier.height(10.dp))
                RowLabel("Bottom Row")
                ButtonTip("Bookmarks", "View all your saved words.")
                ButtonTip("J Coin", "Check your J Coin balance and earn rules.")
                ButtonTip("Feedback", "Send feedback to the development team.")
            }

            // Jukugo & Dictionary Guide
            GuideSection("Word List & Dictionary", "\uD83D\uDCD6") {
                StepItem(1, "Tap ", "FULL/25%", " to enter partial mode. The screen splits into a camera strip and a word list.")
                StepItem(2, "The word list shows all detected ", "kanji compounds", " with their readings.")
                StepItem(3, "Tap any word to open its ", "dictionary definition", " with meanings, part-of-speech tags, and kanji breakdown.")
                StepItem(4, "Tap the ", "bookmark icon", " (right side of the dictionary header) to save a word.")
                StepItem(5, "Tap any ", "kanji square", " in the breakdown section to see detailed kanji info (grade, JLPT level, readings, meanings).")
            }

            // Vertical Mode Guide
            GuideSection("Vertical Text Mode", "\u7e26") {
                StepItem(1, "Tap ", "\u7e26/\u6a2a", " to switch to vertical mode for manga, signs, and traditional Japanese text.")
                StepItem(2, "In partial mode: the camera strip moves to the ", "right 40%", " of the screen, and the word list appears on the left 60%.")
                StepItem(3, "Furigana appears to the ", "right", " of each vertical kanji column.")
            }

            // Scan Challenge Guide
            GuideSection("Scan Challenge", "\uD83C\uDFAF") {
                StepItem(1, "An orange ", "\"Find:\"", " badge appears on the camera screen with a target kanji character.")
                StepItem(2, "Point your camera at text containing that kanji to ", "complete the challenge", ".")
                StepItem(3, "When found, the badge turns ", "green", " and you earn +10 J Coins. Tap it to get a new challenge.")
            }

            // Links Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Links",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LinkRow(
                        label = "Privacy Policy",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jworks-ai.com/apps/kanjilens/privacy"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = "Rate on Google Play",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jworks.kanjilens"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = "Send Feedback",
                        onClick = onFeedbackClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = "JWorks AI",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jworks-ai.com"))
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LinkRow(
                        label = "Creator \u2014 Jay",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jayismocking.com"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Credits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Credits & Attributions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CreditItem("ML Kit", "Google ML Kit Japanese Text Recognition")
                    CreditItem("JMDict", "Japanese-Multilingual Dictionary Project (EDRDG)")
                    CreditItem("Kuromoji", "Japanese morphological analyzer by Atilika")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuideSection(title: String, icon: String = "", content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon.isNotEmpty()) {
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RowLabel(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(RowLabelBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun ButtonTip(buttonName: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(BadgeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = buttonName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StepItem(number: Int, prefix: String, highlight: String, suffix: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentTeal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append(prefix)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AccentBlue)) {
                    append(highlight)
                }
                append(suffix)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreditItem(name: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
