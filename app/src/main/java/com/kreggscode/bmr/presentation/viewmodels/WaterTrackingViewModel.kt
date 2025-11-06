package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.dao.WaterDao
import com.kreggscode.bmr.data.local.entities.UserProfile
import com.kreggscode.bmr.data.local.entities.WaterIntake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WaterTrackingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val waterDao: WaterDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WaterTrackingUiState())
    val uiState: StateFlow<WaterTrackingUiState> = _uiState.asStateFlow()
    
    init {
        loadWaterData()
    }
    
    private fun loadWaterData() {
        viewModelScope.launch {
            userDao.getCurrentUser()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(WaterTrackingUiState())
                    } else {
                        // Load today's water intake
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        // Load last 7 days for chart
                        val sevenDaysAgo = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -6)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        
                        // Combine both flows to update when either changes
                        combine(
                            waterDao.getWaterIntakeByDateFlow(user.id, today),
                            waterDao.getWaterIntakeByDateRange(user.id, sevenDaysAgo, todayEnd)
                        ) { todayRecord, records ->
                            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                            
                            val weeklyData = (0..6).map { dayOffset ->
                                val calendar = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -dayOffset)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val dayStart = calendar.timeInMillis
                                
                                val dayRecord = records.find { it.date == dayStart }
                                
                                DailyWaterData(
                                    date = dayStart,
                                    ml = dayRecord?.totalMl ?: 0,
                                    glasses = dayRecord?.glasses ?: 0,
                                    dateLabel = dateFormat.format(calendar.time),
                                    dayLabel = when (dayOffset) {
                                        0 -> "Today"
                                        1 -> "Yesterday"
                                        else -> dayFormat.format(calendar.time)
                                    }
                                )
                            }.reversed()
                            
                            WaterTrackingUiState(
                                todayWaterMl = todayRecord?.totalMl ?: 0,
                                todayGlasses = todayRecord?.glasses ?: 0,
                                weeklyWaterData = weeklyData
                            )
                        }
                    }
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }
    
    fun addWater(glassSize: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("WaterTracking", "addWater called with size: $glassSize")
                
                var user = userDao.getCurrentUser().firstOrNull()
                
                // If no user exists, create a default user
                if (user == null) {
                    android.util.Log.d("WaterTracking", "No user found, creating default user...")
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.YEAR, -30) // Default age 30
                    
                    user = com.kreggscode.bmr.data.local.entities.UserProfile(
                        name = "User",
                        dateOfBirth = calendar.timeInMillis,
                        sex = "male",
                        heightCm = 175.0,
                        weightKg = 70.0,
                        activityLevel = "moderate",
                        units = "metric",
                        goalType = "maintain",
                        goalRate = 0.0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val userId = userDao.insertUser(user)
                    user = user.copy(id = userId)
                    android.util.Log.d("WaterTracking", "Created default user with ID: $userId")
                }
                
                android.util.Log.d("WaterTracking", "User found: ${user.id}, ${user.name}")
                
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                android.util.Log.d("WaterTracking", "Today timestamp: $today")
                
                val existing = waterDao.getWaterIntakeByDate(user.id, today)
                
                if (existing != null) {
                    android.util.Log.d("WaterTracking", "Updating existing record: ${existing.totalMl}ml -> ${existing.totalMl + glassSize}ml")
                    val updated = existing.copy(
                        totalMl = existing.totalMl + glassSize,
                        glasses = existing.glasses + 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                    waterDao.updateWaterIntake(updated)
                } else {
                    android.util.Log.d("WaterTracking", "Creating new record: ${glassSize}ml")
                    val newRecord = WaterIntake(
                        userId = user.id,
                        date = today,
                        totalMl = glassSize,
                        glasses = 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                    waterDao.insertWaterIntake(newRecord)
                }
                
                android.util.Log.d("WaterTracking", "Water added successfully")
                // UI will update automatically via Flow
            } catch (e: Exception) {
                android.util.Log.e("WaterTracking", "Error adding water", e)
                e.printStackTrace()
            }
        }
    }
    
    fun removeWater(glassSize: Int) {
        viewModelScope.launch {
            try {
                var user = userDao.getCurrentUser().firstOrNull()
                
                // If no user exists, create a default user
                if (user == null) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.YEAR, -30)
                    user = UserProfile(
                        name = "User",
                        dateOfBirth = calendar.timeInMillis,
                        sex = "male",
                        heightCm = 175.0,
                        weightKg = 70.0,
                        activityLevel = "moderate",
                        units = "metric",
                        goalType = "maintain",
                        goalRate = 0.0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val userId = userDao.insertUser(user)
                    user = user.copy(id = userId)
                }
                
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val existing = waterDao.getWaterIntakeByDate(user.id, today)
                existing?.let { record ->
                    val newMl = (record.totalMl - glassSize).coerceAtLeast(0)
                    val newGlasses = (record.glasses - 1).coerceAtLeast(0)
                    
                    if (newMl == 0 && newGlasses == 0) {
                        waterDao.deleteWaterIntakeByDate(user.id, today)
                    } else {
                        val updated = record.copy(
                            totalMl = newMl,
                            glasses = newGlasses,
                            lastUpdated = System.currentTimeMillis()
                        )
                        waterDao.updateWaterIntake(updated)
                    }
                }
                
                // UI will update automatically via Flow
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun resetToday() {
        viewModelScope.launch {
            try {
                var user = userDao.getCurrentUser().firstOrNull()
                
                // If no user exists, nothing to reset
                if (user == null) {
                    return@launch
                }
                
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                waterDao.deleteWaterIntakeByDate(user.id, today)
                // UI will update automatically via Flow
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class WaterTrackingUiState(
    val todayWaterMl: Int = 0,
    val todayGlasses: Int = 0,
    val weeklyWaterData: List<DailyWaterData> = emptyList()
)

data class DailyWaterData(
    val date: Long,
    val ml: Int,
    val glasses: Int,
    val dateLabel: String, // "Nov 5", "Nov 6", etc.
    val dayLabel: String // "Today", "Yesterday", "Mon", etc.
)

