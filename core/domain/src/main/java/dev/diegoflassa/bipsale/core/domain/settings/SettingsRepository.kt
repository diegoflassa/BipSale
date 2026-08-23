package dev.diegoflassa.bipsale.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** A one-shot read, for the paths that snapshot rather than observe — a backup, for one. */
    suspend fun current(): AppSettings

    suspend fun save(settings: AppSettings)
}
