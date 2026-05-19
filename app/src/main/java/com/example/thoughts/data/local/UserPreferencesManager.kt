package com.example.thoughts.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesManager(private val context: Context) {

    private val profileJson = stringPreferencesKey("user_profile_json")
    private val preferencesJson = stringPreferencesKey("app_preferences_json")

    val userProfileFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { it[profileJson] }

    val appPreferencesFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { it[preferencesJson] }

    suspend fun saveUserProfile(json: String) {
        context.dataStore.edit { preferences ->
            preferences[profileJson] = json
        }
    }

    suspend fun saveAppPreferences(json: String) {
        context.dataStore.edit { preferences ->
            preferences[preferencesJson] = json
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
