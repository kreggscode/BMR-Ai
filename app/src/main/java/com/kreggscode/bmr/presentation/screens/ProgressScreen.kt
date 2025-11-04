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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreggscode.bmr.Screen
import com.kreggscode.bmr.presentation.viewmodels.TimePeriod
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: com.kreggscode.bmr.presentation.viewmodels.ProgressViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
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
            ProgressHeader(onBackClick = { navController.navigateUp() })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Period Selector
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelect = viewModel::selectPeriod
            )
            
            // Period Explanation
            PeriodExplanation(selectedPeriod = uiState.selectedPeriod)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calorie Progress Circle
            CalorieProgressCircle(
                progress = uiState.progress,
                caloriesConsumed = if (uiState.selectedPeriod == TimePeriod.WEEK) uiState.todayCalories else uiState.avgCalories,
                caloriesGoal = uiState.targetCalories,
                deficit = uiState.deficit,
                burned = uiState.burned,
                active = uiState.active,
                period = uiState.selectedPeriod
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Grid
            ProgressStatsGrid(
                weightLost = uiState.weightLost,
                streak = uiState.streak,
                waterAvg = uiState.waterAvg,
                sleepAvg = uiState.sleepAvg,
                onSleepClick = { navController.navigate(Screen.SleepTracking.route) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Weight Progress Chart
            WeightProgressChart(viewModel = viewModel)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Macro Distribution
            MacroDistributionCard(
                proteinCurrent = uiState.proteinCurrent,
                proteinTarget = uiState.proteinTarget,
                carbsCurrent = uiState.carbsCurrent,
                carbsTarget = uiState.carbsTarget,
                fatCurrent = uiState.fatCurrent,
                fatTarget = uiState.fatTarget
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Achievements
            AchievementsSection()
        }
    }
}

@Composable
private fun ProgressHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
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
                    text = "Your Progress",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track calories, macros & health metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = { /* TODO: Export data */ }) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Export",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelect: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.values().forEach { period ->
            PeriodChip(
                period = period,
                selected = selectedPeriod == period,
                onClick = { onPeriodSelect(period) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PeriodChip(
    period: TimePeriod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo else Color.Transparent,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = period.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PeriodExplanation(selectedPeriod: TimePeriod) {
    val explanation = when (selectedPeriod) {
        TimePeriod.WEEK -> "📊 Showing today's calories consumed vs. your daily target"
        TimePeriod.MONTH -> "📊 Showing average daily calories over the last 30 days"
        TimePeriod.YEAR -> "📊 Showing average daily calories over the last 365 days"
    }
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalorieProgressCircle(
    progress: Float,
    caloriesConsumed: Int,
    caloriesGoal: Int,
    deficit: Int,
    burned: Int,
    active: Int,
    period: TimePeriod
) {
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Calorie Intake",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your consumption today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgress(
                    progress = progress,
                    strokeWidth = 12.dp,
                    colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$caloriesConsumed",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Text(
                        text = "of $caloriesGoal kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                CalorieStat(
                    label = if (deficit > 0) "Deficit" else "Surplus",
                    value = kotlin.math.abs(deficit).toString(),
                    color = if (deficit > 0) Success else Warning
                )
                CalorieStat(label = "BMR", value = burned.toString(), color = AccentCoral)
                CalorieStat(label = "Activity", value = active.toString(), color = PrimaryTeal)
            }
        }
    }
}

@Composable
private fun CircularProgress(
    progress: Float,
    strokeWidth: androidx.compose.ui.unit.Dp,
    colors: List<Color>
) {
    val sweep = 270f * progress
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension
        val radius = diameter / 2
        val strokePx = strokeWidth.toPx()
        
        // Background circle
        drawCircle(
            color = Color.Gray.copy(alpha = 0.1f),
            radius = radius - strokePx / 2,
            style = Stroke(strokePx)
        )
        
        // Progress arc
        drawArc(
            brush = Brush.sweepGradient(colors),
            startAngle = 135f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(strokePx, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CalorieStat(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "kcal",
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProgressStatsGrid(
    weightLost: Double,
    streak: Int,
    waterAvg: Int,
    sleepAvg: Int,
    onSleepClick: () -> Unit = {}
) {
    val stats = listOf(
        ProgressStat("Weight Change", if (weightLost > 0) "-${String.format("%.1f", weightLost)}" else "--", "kg", Icons.Default.TrendingDown, if (weightLost > 0) Success else MaterialTheme.colorScheme.onSurfaceVariant, onClick = null),
        ProgressStat("Logging Streak", if (streak > 0) streak.toString() else "--", "days", Icons.Default.LocalFireDepartment, if (streak > 0) AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant, onClick = null),
        ProgressStat("Sleep Avg", if (sleepAvg > 0) "$sleepAvg" else "--", "hrs", Icons.Default.Bedtime, PrimaryIndigo, onClick = onSleepClick),
        ProgressStat("Water Intake", if (waterAvg > 0) waterAvg.toString() else "--", "glasses", Icons.Default.WaterDrop, AccentSky, onClick = null)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { stat ->
                    ProgressStatCard(
                        stat = stat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatCard(
    stat: ProgressStat,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .then(if (stat.onClick != null) Modifier.clickable { stat.onClick?.invoke() } else Modifier),
        cornerRadius = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(stat.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = stat.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stat.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightProgressChart(
    viewModel: com.kreggscode.bmr.presentation.viewmodels.ProgressViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentWeight = remember { androidx.compose.runtime.mutableStateOf(0.0) }
    val startWeight = remember { androidx.compose.runtime.mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        // Load current weight from user profile
        viewModel.loadUserWeight()
    }
    
    LaunchedEffect(uiState.currentWeight) {
        if (uiState.currentWeight > 0) {
            currentWeight.value = uiState.currentWeight
            if (startWeight.value == 0.0) {
                startWeight.value = uiState.currentWeight // Use first weight as start
            }
        }
    }
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weight Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { /* TODO: Navigate to weight entry */ }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Weight",
                        tint = PrimaryTeal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show weight data from ViewModel state
            val startWeightValue = if (uiState.startWeight > 0) uiState.startWeight else uiState.currentWeight
            val currentWeightValue = uiState.currentWeight
            val weightChange = if (uiState.startWeight > 0 && uiState.currentWeight > 0) {
                uiState.currentWeight - uiState.startWeight
            } else {
                0.0
            }
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                WeightStat(
                    label = "Start", 
                    value = if (startWeightValue > 0) "${String.format("%.1f", startWeightValue)} kg" else "--"
                )
                WeightStat(
                    label = "Current", 
                    value = if (currentWeightValue > 0) "${String.format("%.1f", currentWeightValue)} kg" else "--"
                )
                WeightStat(
                    label = "Change", 
                    value = if (weightChange != 0.0) {
                        val sign = if (weightChange > 0) "+" else ""
                        "$sign${String.format("%.1f", weightChange)} kg"
                    } else "--"
                )
            }
            
            if (currentWeightValue <= 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 Set your weight in BMR Calculator to track progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (weightChange < 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🎉 Lost ${String.format("%.1f", kotlin.math.abs(weightChange))} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Success,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (weightChange > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "📈 Gained ${String.format("%.1f", weightChange)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Warning,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WeightStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroDistributionCard(
    proteinCurrent: Int,
    proteinTarget: Int,
    carbsCurrent: Int,
    carbsTarget: Int,
    fatCurrent: Int,
    fatTarget: Int
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column {
            Text(
                text = "Macro Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            MacroBar(label = "Protein", value = proteinCurrent, target = proteinTarget, color = AccentCoral)
            Spacer(modifier = Modifier.height(12.dp))
            MacroBar(label = "Carbs", value = carbsCurrent, target = carbsTarget, color = PrimaryTeal)
            Spacer(modifier = Modifier.height(12.dp))
            MacroBar(label = "Fat", value = fatCurrent, target = fatTarget, color = PrimaryPurple)
        }
    }
}

@Composable
private fun MacroBar(
    label: String,
    value: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) {
        (value.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value / $target g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun AchievementsSection() {
    Column {
        Text(
            text = "Recent Achievements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val achievements = listOf(
            Achievement("First Steps", "Start logging your meals to unlock achievements", Icons.Default.EmojiEvents, MaterialTheme.colorScheme.onSurfaceVariant),
            Achievement("Consistency", "Log meals for 7 days to unlock", Icons.Default.Lock, MaterialTheme.colorScheme.onSurfaceVariant),
            Achievement("Macro Master", "Hit macro targets for 5 days to unlock", Icons.Default.Lock, MaterialTheme.colorScheme.onSurfaceVariant)
        )
        
        achievements.forEach { achievement ->
            AchievementCard(achievement = achievement)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                achievement.color.copy(alpha = 0.3f),
                                achievement.color.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = achievement.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class ProgressStat(
    val label: String,
    val value: String,
    val unit: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: (() -> Unit)? = null
)

data class Achievement(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
