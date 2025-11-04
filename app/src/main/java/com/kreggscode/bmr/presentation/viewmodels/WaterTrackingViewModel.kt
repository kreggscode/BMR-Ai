package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.dao.WaterDao
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
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    // Load today's water intake
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val todayRecord = waterDao.getWaterIntakeByDate(profile.id, today)
                    
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
                    
                    waterDao.getWaterIntakeByDateRange(profile.id, sevenDaysAgo, todayEnd).collect { records ->
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
                        
                        _uiState.update { state ->
                            state.copy(
                                todayWaterMl = todayRecord?.totalMl ?: 0,
                                todayGlasses = todayRecord?.glasses ?: 0,
                                weeklyWaterData = weeklyData
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun addWater(glassSize: Int) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val existing = waterDao.getWaterIntakeByDate(user.id, today)
            
            if (existing != null) {
                val updated = existing.copy(
                    totalMl = existing.totalMl + glassSize,
                    glasses = existing.glasses + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                waterDao.updateWaterIntake(updated)
            } else {
                val newRecord = WaterIntake(
                    userId = user.id,
                    date = today,
                    totalMl = glassSize,
                    glasses = 1,
                    lastUpdated = System.currentTimeMillis()
                )
                waterDao.insertWaterIntake(newRecord)
            }
        }
    }
    
    fun removeWater(glassSize: Int) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
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
        }
    }
    
    fun resetToday() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            waterDao.deleteWaterIntakeByDate(user.id, today)
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

