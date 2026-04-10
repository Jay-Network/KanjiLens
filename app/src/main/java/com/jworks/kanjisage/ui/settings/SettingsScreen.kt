package com.jworks.kanjisage.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import com.jworks.kanjisage.data.auth.AuthRepository
import com.jworks.kanjisage.data.auth.AuthState
import com.jworks.kanjisage.domain.models.AppSettings
import kotlin.math.roundToInt

private data class ColorPreset(
    val name: String,
    val kanjiColor: Long,
    val kanaColor: Long
)

private val COLOR_PRESETS = listOf(
    ColorPreset("Forest", 0xFF4CAF50, 0xFF2196F3),
    ColorPreset("Sunset", 0xFFFF9800, 0xFF9C27B0),
    ColorPreset("Ocean", 0xFF00BCD4, 0xFF009688),
    ColorPreset("Neon", 0xFFE91E63, 0xFFFFEB3B)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    onLinkAccountClick: (() -> Unit)? = null,
    authRepository: AuthRepository? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val authState = authRepository?.authState?.collectAsState()
    val isAnonymous = (authState?.value as? AuthState.SignedIn)?.isAnonymous ?: true
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader("Overlay")

            SliderSetting(
                label = "Label font size",
                value = settings.labelFontSize,
                valueLabel = "${settings.labelFontSize.roundToInt()}sp",
                range = 5f..24f,
                onValueChange = viewModel::updateLabelFontSize
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Adaptive furigana color", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.furiganaAdaptiveColor,
                    onCheckedChange = viewModel::updateFuriganaAdaptiveColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "White furigana text",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settings.furiganaAdaptiveColor)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = settings.furiganaUseWhiteText,
                    onCheckedChange = viewModel::updateFuriganaUseWhiteText,
                    enabled = !settings.furiganaAdaptiveColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show bounding boxes", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.showBoxes,
                    onCheckedChange = viewModel::updateShowBoxes
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Color theme", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ColorPresetRow(settings = settings, onPresetClick = viewModel::applyColorPreset)

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Performance")

            SliderSetting(
                label = "Frame skip",
                value = settings.frameSkip.toFloat(),
                valueLabel = when (settings.frameSkip) {
                    1 -> "No skip (real-time)"
                    2 -> "Skip 1 frame"
                    else -> "Skip ${settings.frameSkip - 1} frames"
                },
                range = 1f..10f,
                steps = 8,
                onValueChange = { viewModel.updateFrameSkip(it.roundToInt()) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("AI Enhancement")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable AI-enhanced readings", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.aiEnhanceEnabled,
                    onCheckedChange = viewModel::updateAiEnhanceEnabled
                )
            }

            GeminiApiKeySection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Token Usage")
            TokenUsageCard(settings = settings, onReset = viewModel::resetTokenUsage)

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Debug")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show debug HUD", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.showDebugHud,
                    onCheckedChange = viewModel::updateShowDebugHud
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Subscription")

            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Manage Subscription on Google Play")
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Account")

            if (isAnonymous && onLinkAccountClick != null) {
                // Anonymous user — offer to link an account
                androidx.compose.material3.OutlinedButton(
                    onClick = onLinkAccountClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign In / Link Account")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in to sync across devices and earn J Coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!isAnonymous && onLogout != null) {
                // Linked user — show sign out
                val email = (authState?.value as? AuthState.SignedIn)?.user?.email ?: ""
                if (email.isNotEmpty()) {
                    Text(
                        text = "Signed in as $email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            }

            if (onHelpClick != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusBorder()
                        .clickable { onHelpClick() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Help & About", style = MaterialTheme.typography.bodyMedium)
                    Text(">", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.bodySmall, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

@Composable
private fun GeminiApiKeySection(viewModel: SettingsViewModel) {
    val currentKey by viewModel.geminiApiKey.collectAsState()
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    val hasKey = currentKey.isNotBlank()
    val keyError = keyInput.isNotBlank() && keyInput.trim().length < 10

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Gemini API key (for AI-enhanced readings)",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasKey) "Key is set" else "No key configured",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasKey) KanjiSageColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = keyError,
            supportingText = if (keyError) {
                { Text("API key is too short") }
            } else null,
            visualTransformation = if (showKey) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showKey = !showKey }) {
                        Text(
                            text = if (showKey) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.setGeminiApiKey(keyInput.trim())
                },
                enabled = keyInput.trim() != currentKey && !keyError,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
            if (hasKey) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        keyInput = ""
                        viewModel.setGeminiApiKey("")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Get your key at ai.google.dev",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TokenUsageCard(settings: AppSettings, onReset: () -> Unit) {
    // Pricing per 1M tokens
    val geminiInputRate = 0.15 / 1_000_000.0
    val geminiOutputRate = 0.60 / 1_000_000.0
    val claudeInputRate = 0.80 / 1_000_000.0
    val claudeOutputRate = 4.00 / 1_000_000.0

    val geminiCost = settings.geminiInputTokens * geminiInputRate + settings.geminiOutputTokens * geminiOutputRate
    val claudeCost = settings.claudeInputTokens * claudeInputRate + settings.claudeOutputTokens * claudeOutputRate

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (settings.geminiInputTokens > 0 || settings.geminiOutputTokens > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gemini", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${settings.geminiInputTokens} in / ${settings.geminiOutputTokens} out (~${"$%.4f".format(geminiCost)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (settings.claudeInputTokens > 0 || settings.claudeOutputTokens > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Claude", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${settings.claudeInputTokens} in / ${settings.claudeOutputTokens} out (~${"$%.4f".format(claudeCost)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (settings.geminiInputTokens == 0L && settings.claudeInputTokens == 0L) {
            Text(
                text = "No AI analysis tokens used yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (settings.geminiInputTokens > 0 || settings.claudeInputTokens > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reset Token Counters")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Gemini 2.5 Flash: $0.15/$0.60 per 1M • Claude Haiku: $0.80/$4.00 per 1M",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ColorPresetRow(settings: AppSettings, onPresetClick: (Long, Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        COLOR_PRESETS.forEach { preset ->
            val isSelected = settings.kanjiColor == preset.kanjiColor && settings.kanaColor == preset.kanaColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .focusBorder(RoundedCornerShape(8.dp))
                    .clickable { onPresetClick(preset.kanjiColor, preset.kanaColor) }
                    .padding(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(preset.kanjiColor))
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(preset.kanaColor))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(preset.name, fontSize = 11.sp)
            }
        }
    }
}
