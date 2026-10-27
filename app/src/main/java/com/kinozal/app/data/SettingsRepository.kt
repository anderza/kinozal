package com.kinozal.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val tmdbKey = stringPreferencesKey("tmdb_key")
    private val language = stringPreferencesKey("language")

    val tmdbKeyFlow: Flow<String> = context.dataStore.data.map { it[tmdbKey].orEmpty() }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[language] ?: DEFAULT_LANGUAGE }

    suspend fun currentTmdbKey(): String = tmdbKeyFlow.first()
    suspend fun currentLanguage(): String = languageFlow.first()

    suspend fun setTmdbKey(value: String) {
        context.dataStore.edit { it[tmdbKey] = value.trim() }
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[language] = value }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru-RU"
    }
}
