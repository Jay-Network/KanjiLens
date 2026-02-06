package com.jworks.kanjilens.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateStrokeWidth(value: Float) = updateSettings { it.copy(strokeWidth = value) }

    fun updateLabelFontSize(value: Float) = updateSettings { it.copy(labelFontSize = value) }

    fun updateLabelBackgroundAlpha(value: Float) = updateSettings { it.copy(labelBackgroundAlpha = value) }

    fun updateFrameSkip(value: Int) = updateSettings { it.copy(frameSkip = value) }

    fun updateShowDebugHud(value: Boolean) = updateSettings { it.copy(showDebugHud = value) }

    fun updateShowBoxes(value: Boolean) = updateSettings { it.copy(showBoxes = value) }

    fun updateFuriganaIsBold(value: Boolean) = updateSettings { it.copy(furiganaIsBold = value) }

    fun updateFuriganaUseWhiteText(value: Boolean) = updateSettings { it.copy(furiganaUseWhiteText = value) }

    fun updateVerticalTextMode(value: Boolean) = updateSettings { it.copy(verticalTextMode = value) }

    fun applyColorPreset(kanjiColor: Long, kanaColor: Long) = updateSettings {
        it.copy(kanjiColor = kanjiColor, kanaColor = kanaColor)
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(settings.value)
            settingsRepository.updateSettings(updated)
        }
    }
}
