package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.SleepDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.entities.SleepRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SleepTrackingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val sleepDao: SleepDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SleepTrackingUiState())
    val uiState: StateFlow<SleepTrackingUiState> = _uiState.asStateFlow()
    
    init {
        loadSleepData()
    }
    
    private fun loadSleepData() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    // Load today's sleep record
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val todayRecord = sleepDao.getSleepRecordByDate(profile.id, today)
                    
                    // Load weekly data (last 7 days)
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
                    
                    sleepDao.getSleepRecordsByDateRange(profile.id, sevenDaysAgo, todayEnd).collect { records ->
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
                            val dayLabel = when (dayOffset) {
                                0 -> "Today"
                                1 -> "Yesterday"
                                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
                            }
                            
                            DailySleepData(
                                date = dayStart,
                                hours = dayRecord?.sleepHours ?: 0.0,
                                dayLabel = dayLabel,
                                quality = dayRecord?.quality ?: 0
                            )
                        }.reversed()
                        
                        val avgSleep = records.map { it.sleepHours }.average().takeIf { !it.isNaN() } ?: 0.0
                        
                        _uiState.update { state ->
                            state.copy(
                                todaySleepHours = todayRecord?.sleepHours ?: 0.0,
                                todayBedtime = todayRecord?.bedtime,
                                todayWakeTime = todayRecord?.wakeTime,
                                todayQuality = todayRecord?.quality ?: 0,
                                weeklySleepData = weeklyData,
                                averageSleepHours = avgSleep,
                                hasTodayRecord = todayRecord != null
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun logSleep(bedtime: Long, wakeTime: Long, quality: Int, notes: String? = null) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val today = calendar.timeInMillis
            
            // Calculate sleep hours
            val sleepDuration = wakeTime - bedtime
            val sleepHours = (sleepDuration / (1000.0 * 60 * 60)).coerceIn(0.0, 24.0)
            
            val sleepRecord = SleepRecord(
                userId = user.id,
                date = today,
                sleepHours = sleepHours,
                bedtime = bedtime,
                wakeTime = wakeTime,
                quality = quality,
                notes = notes
            )
            
            sleepDao.insertSleepRecord(sleepRecord)
        }
    }
    
    fun updateTodaySleep(bedtime: Long? = null, wakeTime: Long? = null, quality: Int? = null, notes: String? = null) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val existing = sleepDao.getSleepRecordByDate(user.id, today)
            
            if (existing != null) {
                val updated = existing.copy(
                    bedtime = bedtime ?: existing.bedtime,
                    wakeTime = wakeTime ?: existing.wakeTime,
                    quality = quality ?: existing.quality,
                    notes = notes ?: existing.notes,
                    sleepHours = if (bedtime != null && wakeTime != null) {
                        val sleepDuration = wakeTime - bedtime
                        (sleepDuration / (1000.0 * 60 * 60)).coerceIn(0.0, 24.0)
                    } else {
                        existing.sleepHours
                    }
                )
                sleepDao.updateSleepRecord(updated)
            } else {
                // Create new record if doesn't exist
                if (bedtime != null && wakeTime != null) {
                    val sleepDuration = wakeTime - bedtime
                    val sleepHours = (sleepDuration / (1000.0 * 60 * 60)).coerceIn(0.0, 24.0)
                    val sleepRecord = SleepRecord(
                        userId = user.id,
                        date = today,
                        sleepHours = sleepHours,
                        bedtime = bedtime,
                        wakeTime = wakeTime,
                        quality = quality ?: 3,
                        notes = notes
                    )
                    sleepDao.insertSleepRecord(sleepRecord)
                }
            }
        }
    }
    
    fun deleteTodaySleep() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            sleepDao.deleteSleepRecordByDate(user.id, today)
        }
    }
}

data class SleepTrackingUiState(
    val todaySleepHours: Double = 0.0,
    val todayBedtime: Long? = null,
    val todayWakeTime: Long? = null,
    val todayQuality: Int = 0,
    val weeklySleepData: List<DailySleepData> = emptyList(),
    val averageSleepHours: Double = 0.0,
    val hasTodayRecord: Boolean = false
)

data class DailySleepData(
    val date: Long,
    val hours: Double,
    val dayLabel: String,
    val quality: Int = 0
)

