package com.skigpxrecorder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User preferences using DataStore
 */
class UserPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

        private val UNIT_SYSTEM_KEY = stringPreferencesKey("unit_system")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val HISTORY_GROUPING_KEY = stringPreferencesKey("history_grouping")
    }

    enum class UnitSystem {
        METRIC,
        IMPERIAL
    }

    enum class Language {
        EN,
        IT,
        DE,
        FR
    }

    enum class HistoryGrouping {
        LOCATION,  // Standort
        DATE       // Datum
    }

    val unitSystemFlow: Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        val systemString = preferences[UNIT_SYSTEM_KEY] ?: UnitSystem.METRIC.name
        try {
            UnitSystem.valueOf(systemString)
        } catch (e: IllegalArgumentException) {
            UnitSystem.METRIC
        }
    }

    val languageFlow: Flow<Language> = context.dataStore.data.map { preferences ->
        val langString = preferences[LANGUAGE_KEY] ?: Language.EN.name
        try {
            Language.valueOf(langString)
        } catch (e: IllegalArgumentException) {
            Language.EN
        }
    }

    val historyGroupingFlow: Flow<HistoryGrouping> = context.dataStore.data.map { preferences ->
        val groupingString = preferences[HISTORY_GROUPING_KEY] ?: HistoryGrouping.DATE.name
        try {
            HistoryGrouping.valueOf(groupingString)
        } catch (e: IllegalArgumentException) {
            HistoryGrouping.DATE
        }
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[UNIT_SYSTEM_KEY] = unitSystem.name
        }
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    suspend fun setHistoryGrouping(grouping: HistoryGrouping) {
        context.dataStore.edit { preferences ->
            preferences[HISTORY_GROUPING_KEY] = grouping.name
        }
    }
}
