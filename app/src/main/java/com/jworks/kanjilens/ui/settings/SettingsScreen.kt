package com.jworks.kanjilens.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjilens.R
import com.jworks.kanjilens.domain.models.AppSettings
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

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

            SliderSetting(
                label = "Box border width",
                value = settings.strokeWidth,
                valueLabel = "${String.format("%.1f", settings.strokeWidth)}px",
                range = 1f..6f,
                onValueChange = viewModel::updateStrokeWidth
            )

            SliderSetting(
                label = "Label opacity",
                value = settings.labelBackgroundAlpha,
                valueLabel = "${(settings.labelBackgroundAlpha * 100).roundToInt()}%",
                range = 0f..1f,
                onValueChange = viewModel::updateLabelBackgroundAlpha
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bold furigana text", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.furiganaIsBold,
                    onCheckedChange = viewModel::updateFuriganaIsBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("White furigana text", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.furiganaUseWhiteText,
                    onCheckedChange = viewModel::updateFuriganaUseWhiteText
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Partial screen mode (better FPS)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.usePartialMode,
                    onCheckedChange = viewModel::updateUsePartialMode
                )
            }

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
