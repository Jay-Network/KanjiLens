package com.jworks.kanjisage.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jworks.kanjisage.domain.ai.AiResponse
import com.jworks.kanjisage.domain.ai.ScopeLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    response: AiResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        AiPanelHeader(
            scopeLevel = scopeLevel,
            provider = response.provider,
            onDismiss = onDismiss
        )

        // Tabs: Summary | Full Text
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Summary") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Full Text") }
            )
        }

        when (selectedTab) {
            0 -> SummaryContent(response = response, modifier = Modifier.weight(1f))
            1 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Footer
        AiPanelFooter(
            processingTimeMs = response.processingTimeMs,
            tokensUsed = response.tokensUsed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLoadingPanel(
    selectedText: String,
    scopeLevel: ScopeLevel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        AiPanelHeader(
            scopeLevel = scopeLevel,
            provider = "Loading...",
            onDismiss = onDismiss
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Summary")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Full Text") }
            )
        }

        when (selectedTab) {
            0 -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.ai_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(response: AiResponse, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val sections = parseAiSections(response.content)
        items(sections) { section ->
            AiSectionCard(section)
        }
    }
}

@Composable
private fun AiPanelHeader(
    scopeLevel: ScopeLevel,
    provider: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScopeBadge(scopeLevel)
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Text("✕", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScopeBadge(scopeLevel: ScopeLevel) {
    val (label, color) = when (scopeLevel) {
        is ScopeLevel.Word -> "Word" to MaterialTheme.colorScheme.primary
        is ScopeLevel.Phrase -> "Phrase" to MaterialTheme.colorScheme.secondary
        is ScopeLevel.Sentence -> "Sentence" to MaterialTheme.colorScheme.secondary
        is ScopeLevel.FullSnapshot -> "Full Text" to MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class AiSection(val title: String, val bodyLines: List<String>)

private fun parseAiSections(raw: String): List<AiSection> {
    val lines = raw.lines()
    val out = mutableListOf<AiSection>()
    var currentTitle = "Analysis"
    val currentBody = mutableListOf<String>()

    fun flush() {
        if (currentBody.isNotEmpty()) {
            out += AiSection(currentTitle, currentBody.toList())
            currentBody.clear()
        }
    }

    lines.forEach { line ->
        when {
            line.startsWith("### ") -> { flush(); currentTitle = line.removePrefix("### ").trim() }
            line.startsWith("## ") -> { flush(); currentTitle = line.removePrefix("## ").trim() }
            line.startsWith("# ") -> { flush(); currentTitle = line.removePrefix("# ").trim() }
            else -> currentBody += line
        }
    }
    flush()
    return out.ifEmpty { listOf(AiSection("Analysis", lines)) }
}

@Composable
private fun AiSectionCard(section: AiSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (line in section.bodyLines) {
                    when {
                        line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                        line.startsWith("- ") || line.startsWith("* ") -> {
                            Row(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "\u2022  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StyledText(
                                    text = line.removePrefix("- ").removePrefix("* "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        else -> {
                            StyledText(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPanelFooter(processingTimeMs: Long, tokensUsed: Int?) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = "${processingTimeMs}ms",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        tokensUsed?.let {
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "$it tokens",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun StyledText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        var remaining = text
        while (remaining.contains("**")) {
            val start = remaining.indexOf("**")
            val end = remaining.indexOf("**", start + 2)
            if (end == -1) {
                append(remaining)
                remaining = ""
                break
            }
            append(remaining.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(remaining.substring(start + 2, end))
            }
            remaining = remaining.substring(end + 2)
        }
        append(remaining)
    }

    Text(
        text = annotated,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
