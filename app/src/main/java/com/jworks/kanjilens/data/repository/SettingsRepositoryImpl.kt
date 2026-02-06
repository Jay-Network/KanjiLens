package com.jworks.kanjilens.data.repository

import com.jworks.kanjilens.data.preferences.SettingsDataStore
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.updateSettings(settings)
    }
}
