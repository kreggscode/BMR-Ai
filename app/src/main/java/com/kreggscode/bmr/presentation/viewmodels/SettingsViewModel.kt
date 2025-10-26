package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.preferences.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val userDao: com.kreggscode.bmr.data.local.dao.UserDao
) : ViewModel() {
    
    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let {
                    _uiState.update { state ->
                        state.copy(
                            userName = it.name,
                            userEmail = "${it.name.lowercase().replace(" ", "")}@email.com",
                            isMetric = true,
                            language = "English",
                            dietaryPreference = "None"
                        )
                    }
                }
            }
        }
    }
    
    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            themePreferences.setDarkMode(isDark)
        }
    }
    
    fun updateUserName(newName: String) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull()
            user?.let {
                val updated = it.copy(name = newName, updatedAt = System.currentTimeMillis())
                userDao.updateUser(updated)
                _uiState.update { state -> state.copy(userName = newName) }
            }
        }
    }
    
    fun toggleUnits(isMetric: Boolean) {
        _uiState.update { it.copy(isMetric = isMetric) }
    }
    
    fun updateLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
    }
    
    fun updateDietaryPreference(preference: String) {
        _uiState.update { it.copy(dietaryPreference = preference) }
    }
    
    fun toggleEditNameDialog(show: Boolean) {
        _uiState.update { it.copy(showEditNameDialog = show) }
    }
    
    fun toggleUnitsDialog(show: Boolean) {
        _uiState.update { it.copy(showUnitsDialog = show) }
    }
    
    fun toggleLanguageDialog(show: Boolean) {
        _uiState.update { it.copy(showLanguageDialog = show) }
    }
    
    fun toggleDietaryDialog(show: Boolean) {
        _uiState.update { it.copy(showDietaryDialog = show) }
    }
}

data class SettingsUiState(
    val userName: String = "Guest",
    val userEmail: String = "guest@email.com",
    val isMetric: Boolean = true,
    val language: String = "English",
    val dietaryPreference: String = "None",
    val showEditNameDialog: Boolean = false,
    val showUnitsDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showDietaryDialog: Boolean = false
)
