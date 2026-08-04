package com.example.thoughts.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesManager(private val context: Context) {

    private fun profileKey(userId: String) = stringPreferencesKey("user_profile_json_$userId")
    private fun preferencesKey(userId: String) = stringPreferencesKey("app_preferences_json_$userId")

    fun userProfileFlow(userId: String?): Flow<String?> {
        if (userId == null) return flowOf(null)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { it[profileKey(userId)] }
    }

    fun appPreferencesFlow(userId: String?): Flow<String?> {
        if (userId == null) return flowOf(null)
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { it[preferencesKey(userId)] }
    }

    suspend fun saveUserProfile(userId: String, json: String) {
        context.dataStore.edit { preferences ->
            preferences[profileKey(userId)] = json
        }
    }

    suspend fun saveAppPreferences(userId: String, json: String) {
        context.dataStore.edit { preferences ->
            preferences[preferencesKey(userId)] = json
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
