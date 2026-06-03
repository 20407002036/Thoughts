package com.example.thoughts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode {
    Light,
    Dark,
    Auto;

    companion object {
        fun fromString(value: String): ThemeMode {
            return when (value.lowercase()) {
                "light" -> Light
                "dark" -> Dark
                else -> Auto
            }
        }
    }
}

class ThemeViewModel : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = JournalRepository.getPreferencesFlow()
        .map { prefs ->
            ThemeMode.fromString(prefs?.appearance_mode ?: "auto")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.Auto
        )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            val currentPrefs = JournalRepository.getPreferencesFlow().stateIn(viewModelScope).value 
                ?: PreferencesResponse()
            val updatedPrefs = currentPrefs.copy(theme = mode.name.lowercase())
            JournalRepository.savePreferences(updatedPrefs)
        }
    }

    fun toggleTheme() {
        val current = themeMode.value
        val next = when (current) {
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark -> ThemeMode.Auto
            ThemeMode.Auto -> ThemeMode.Light
        }
        setTheme(next)
    }
}
