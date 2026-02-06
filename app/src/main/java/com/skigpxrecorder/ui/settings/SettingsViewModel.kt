package com.skigpxrecorder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val unitSystem: StateFlow<UserPreferences.UnitSystem> = userPreferences.unitSystemFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences.UnitSystem.METRIC
        )

    val language: StateFlow<UserPreferences.Language> = userPreferences.languageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences.Language.EN
        )

    fun setUnitSystem(unitSystem: UserPreferences.UnitSystem) {
        viewModelScope.launch {
            userPreferences.setUnitSystem(unitSystem)
        }
    }

    fun setLanguage(language: UserPreferences.Language) {
        viewModelScope.launch {
            userPreferences.setLanguage(language)
        }
    }
}
