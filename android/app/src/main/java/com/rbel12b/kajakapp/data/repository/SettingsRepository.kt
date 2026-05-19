package com.rbel12b.kajakapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        const val DEFAULT_TOKEN = "Aa0b48e6f9ef4646b77cf6d702f52d"
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_SELECTED_ATHLETE_ID = stringPreferencesKey("selected_athlete_id")
        private val KEY_SELECTED_ATHLETE_NAME = stringPreferencesKey("selected_athlete_name")
        private val KEY_FAVORITE_IDS = stringPreferencesKey("favorite_athlete_ids")
    }

    val tokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]?.takeIf { it.isNotBlank() } ?: DEFAULT_TOKEN
    }

    val selectedAthleteIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_ATHLETE_ID]?.takeIf { it.isNotBlank() }
    }

    val selectedAthleteNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_ATHLETE_NAME]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[KEY_TOKEN] = token }
    }

    suspend fun saveSelectedAthlete(id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_ATHLETE_ID] = id
            prefs[KEY_SELECTED_ATHLETE_NAME] = name
        }
    }

    suspend fun clearSelectedAthlete() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SELECTED_ATHLETE_ID)
            prefs.remove(KEY_SELECTED_ATHLETE_NAME)
        }
    }

    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_IDS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet()
                ?: mutableSetOf()
            if (id in current) current.remove(id) else current.add(id)
            prefs[KEY_FAVORITE_IDS] = current.joinToString(",")
        }
    }
}
