package com.jworks.kanjilens.domain.repository

import com.jworks.kanjilens.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}
