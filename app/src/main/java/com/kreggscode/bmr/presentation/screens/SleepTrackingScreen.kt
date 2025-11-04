package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kreggscode.bmr.presentation.viewmodels.SleepTrackingViewModel
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SleepTrackingScreen(
    navController: NavController,
    viewModel: SleepTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    var showBedtimeDialog by remember { mutableStateOf(false) }
    var showWakeTimeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp)
        ) {
            // Header
            SleepHeader(
                onBackClick = { navController.navigateUp() },
                onDeleteClick = {
                    viewModel.deleteTodaySleep()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Sleep Card
            TodaySleepCard(
                sleepHours = uiState.todaySleepHours,
                bedtime = uiState.todayBedtime,
                wakeTime = uiState.todayWakeTime,
                quality = uiState.todayQuality,
                hasRecord = uiState.hasTodayRecord,
                onLogBedtime = { showBedtimeDialog = true },
                onLogWakeTime = { showWakeTimeDialog = true },
                onSetQuality = { showQualityDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Weekly Chart
            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "🌙 Track your sleep patterns. Aim for 7-9 hours of quality sleep each night.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            WeeklySleepChart(weeklyData = uiState.weeklySleepData)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Average Sleep
            AverageSleepCard(
                averageHours = uiState.averageSleepHours,
                weeklyData = uiState.weeklySleepData
            )
        }
    }
    
    // Dialogs
    if (showBedtimeDialog) {
        TimePickerDialog(
            title = "Set Bedtime",
            onDismiss = { showBedtimeDialog = false },
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.updateTodaySleep(bedtime = calendar.timeInMillis)
                showBedtimeDialog = false
            }
        )
    }
    
    if (showWakeTimeDialog) {
        TimePickerDialog(
            title = "Set Wake Time",
            onDismiss = { showWakeTimeDialog = false },
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.updateTodaySleep(wakeTime = calendar.timeInMillis)
                showWakeTimeDialog = false
            }
        )
    }
    
    if (showQualityDialog) {
        QualityPickerDialog(
            onDismiss = { showQualityDialog = false },
            onQualitySelected = { quality ->
                viewModel.updateTodaySleep(quality = quality)
                showQualityDialog = false
            }
        )
    }
}

@Composable
private fun SleepHeader(
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column {
                Text(
                    text = "Sleep Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track your sleep quality",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Today's Sleep",
                tint = Error
            )
        }
    }
}

@Composable
private fun TodaySleepCard(
    sleepHours: Double,
    bedtime: Long?,
    wakeTime: Long?,
    quality: Int,
    hasRecord: Boolean,
    onLogBedtime: () -> Unit,
    onLogWakeTime: () -> Unit,
    onSetQuality: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Sleep",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sleep Hours Display
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = (sleepHours / 9.0).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryIndigo,
                    strokeWidth = 12.dp,
                    trackColor = PrimaryIndigo.copy(alpha = 0.2f)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (sleepHours > 0) "${String.format("%.1f", sleepHours)}" else "--",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Text(
                        text = "hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bedtime and Wake Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SleepTimeButton(
                    label = "Bedtime",
                    time = bedtime,
                    onClick = onLogBedtime,
                    icon = Icons.Default.Bedtime
                )
                SleepTimeButton(
                    label = "Wake Time",
                    time = wakeTime,
                    onClick = onLogWakeTime,
                    icon = Icons.Default.Alarm
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quality Button
            QualityButton(
                quality = quality,
                onClick = onSetQuality
            )
            
            if (!hasRecord) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap buttons above to log your sleep",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SleepTimeButton(
    label: String,
    time: Long?,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryIndigo, PrimaryPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (time != null) {
            val calendar = Calendar.getInstance().apply { timeInMillis = time }
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            Text(
                text = timeStr,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryIndigo
            )
        }
    }
}

@Composable
private fun QualityButton(
    quality: Int,
    onClick: () -> Unit
) {
    val qualityLabels = listOf("Poor", "Fair", "Good", "Very Good", "Excellent")
    val qualityColors = listOf(Error, Warning, PrimaryTeal, PrimaryIndigo, Success)
    
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (quality > 0) qualityColors[quality.coerceIn(0, 4)] else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (quality > 0) "Quality: ${qualityLabels[quality.coerceIn(0, 4)]}" else "Set Sleep Quality",
            color = if (quality > 0) qualityColors[quality.coerceIn(0, 4)] else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklySleepChart(weeklyData: List<com.kreggscode.bmr.presentation.viewmodels.DailySleepData>) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weeklyData.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val progress = (data.hours / 9.0).coerceIn(0.0, 1.0)
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress.toFloat())
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(PrimaryIndigo, PrimaryPurple)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (data.hours > 0) "${String.format("%.1f", data.hours)}h" else "--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = data.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AverageSleepCard(
    averageHours: Double,
    weeklyData: List<com.kreggscode.bmr.presentation.viewmodels.DailySleepData>
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format("%.1f", averageHours)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )
                Text(
                    text = "Avg Hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val daysWithSleep = weeklyData.count { it.hours > 0 }
                Text(
                    text = "$daysWithSleep",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                Text(
                    text = "Days Tracked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour picker (simplified - in production use TimePicker)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { selectedHour = (selectedHour - 1).coerceIn(0, 23) }) {
                                Icon(Icons.Default.Remove, null)
                            }
                            Text(
                                text = String.format("%02d", selectedHour),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { selectedHour = (selectedHour + 1).coerceIn(0, 23) }) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { selectedMinute = (selectedMinute - 5).coerceIn(0, 59) }) {
                                Icon(Icons.Default.Remove, null)
                            }
                            Text(
                                text = String.format("%02d", selectedMinute),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { selectedMinute = (selectedMinute + 5).coerceIn(0, 59) }) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QualityPickerDialog(
    onDismiss: () -> Unit,
    onQualitySelected: (Int) -> Unit
) {
    var selectedQuality by remember { mutableStateOf(3) }
    val qualityLabels = listOf("Poor", "Fair", "Good", "Very Good", "Excellent")
    val qualityColors = listOf(Error, Warning, PrimaryTeal, PrimaryIndigo, Success)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Quality") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = qualityLabels[selectedQuality],
                    style = MaterialTheme.typography.headlineSmall,
                    color = qualityColors[selectedQuality],
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { selectedQuality = index }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "${index + 1} stars",
                                tint = if (index <= selectedQuality) qualityColors[selectedQuality] else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onQualitySelected(selectedQuality) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

