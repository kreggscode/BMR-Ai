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
    private val userDao: com.kreggscode.bmr.data.local.dao.UserDao,
    private val bmrDao: com.kreggscode.bmr.data.local.dao.BMRDao,
    private val foodDao: com.kreggscode.bmr.data.local.dao.FoodDao
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
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Delete all user data
                val users = userDao.getAllUsers().firstOrNull() ?: emptyList()
                users.forEach { user ->
                    bmrDao.deleteBMRRecordsForUser(user.id)
                    foodDao.deleteMealEntriesForUser(user.id)
                }
                // Delete all food items
                foodDao.deleteAllFoodItems()
                // Delete all users
                users.forEach { userDao.deleteUser(it) }
                
                _uiState.update { it.copy(dataCleared = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.update { 
            it.copy(
                reminderHour = hour,
                reminderMinute = minute,
                reminderEnabled = true
            ) 
        }
    }
    
    fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }
    
    fun createNewProfile(name: String, age: Int, sex: String, height: Double, weight: Double) {
        viewModelScope.launch {
            try {
                val dateOfBirth = System.currentTimeMillis() - (age.toLong() * 365 * 24 * 60 * 60 * 1000)
                val newUser = com.kreggscode.bmr.data.local.entities.UserProfile(
                    name = name,
                    dateOfBirth = dateOfBirth,
                    sex = sex,
                    heightCm = height,
                    weightKg = weight,
                    activityLevel = "moderate",
                    goalType = "maintain"
                )
                userDao.insertUser(newUser)
                _uiState.update { it.copy(profileCreated = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun deleteProfile(userId: Long) {
        viewModelScope.launch {
            try {
                val user = userDao.getUserById(userId).firstOrNull()
                user?.let {
                    bmrDao.deleteBMRRecordsForUser(userId)
                    foodDao.deleteMealEntriesForUser(userId)
                    userDao.deleteUser(it)
                    _uiState.update { state -> state.copy(profileDeleted = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun exportData(): String {
        // Generate CSV or JSON export
        return "Export functionality - data will be saved to file"
    }
    
    fun toggleClearDataDialog(show: Boolean) {
        _uiState.update { it.copy(showClearDataDialog = show) }
    }
    
    fun toggleReminderDialog(show: Boolean) {
        _uiState.update { it.copy(showReminderDialog = show) }
    }
    
    fun toggleCreateProfileDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateProfileDialog = show) }
    }
    
    fun toggleManageProfilesDialog(show: Boolean) {
        if (show) {
            loadAllProfiles()
        }
        _uiState.update { it.copy(showManageProfilesDialog = show) }
    }
    
    private fun loadAllProfiles() {
        viewModelScope.launch {
            val profiles = userDao.getAllUsers().firstOrNull() ?: emptyList()
            val currentUser = userDao.getCurrentUser().firstOrNull()
            _uiState.update { 
                it.copy(
                    allProfiles = profiles,
                    currentUserId = currentUser?.id ?: 0L
                ) 
            }
        }
    }
    
    fun switchProfile(userId: Long) {
        viewModelScope.launch {
            val user = userDao.getUserById(userId).firstOrNull()
            user?.let {
                // In a real app, you'd set this as the "current" user in preferences
                // For now, we'll just update the UI state
                _uiState.update { state ->
                    state.copy(
                        userName = it.name,
                        userEmail = "${it.name.lowercase().replace(" ", "")}@email.com",
                        currentUserId = userId,
                        profileSwitched = true
                    )
                }
            }
        }
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
    val showDietaryDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showReminderDialog: Boolean = false,
    val showCreateProfileDialog: Boolean = false,
    val showManageProfilesDialog: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 12,
    val reminderMinute: Int = 0,
    val dataCleared: Boolean = false,
    val profileCreated: Boolean = false,
    val profileDeleted: Boolean = false,
    val profileSwitched: Boolean = false,
    val allProfiles: List<com.kreggscode.bmr.data.local.entities.UserProfile> = emptyList(),
    val currentUserId: Long = 0L,
    val error: String? = null
)
