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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calorie Progress Circle
            CalorieProgressCircle(
                progress = uiState.progress,
                caloriesConsumed = uiState.avgCalories,
                caloriesGoal = uiState.targetCalories,
                deficit = uiState.deficit,
                burned = uiState.burned,
                active = uiState.active
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Grid
            ProgressStatsGrid(
                weightLost = uiState.weightLost,
                streak = uiState.streak,
                waterAvg = uiState.waterAvg
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Weight Progress Chart
            WeightProgressChart()
            
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
private fun CalorieProgressCircle(
    progress: Float,
    caloriesConsumed: Int,
    caloriesGoal: Int,
    deficit: Int,
    burned: Int,
    active: Int
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
                        text = "Daily Calorie Intake",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Average for selected period",
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
    waterAvg: Int
) {
    val stats = listOf(
        ProgressStat("Weight Change", if (weightLost > 0) "-${String.format("%.1f", weightLost)}" else "0.0", "kg", Icons.Default.TrendingDown, if (weightLost > 0) Success else MaterialTheme.colorScheme.onSurfaceVariant),
        ProgressStat("Logging Streak", if (streak > 0) streak.toString() else "0", "days", Icons.Default.LocalFireDepartment, if (streak > 0) AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant),
        ProgressStat("Sleep Goal", "8", "hours", Icons.Default.Bedtime, PrimaryIndigo),
        ProgressStat("Water Goal", "8", "glasses", Icons.Default.WaterDrop, AccentSky)
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
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
private fun WeightProgressChart() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column {
            Text(
                text = "Weight Trend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Simplified chart visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PrimaryIndigo.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw trend line
                    val points = listOf(
                        Offset(0f, height * 0.2f),
                        Offset(width * 0.2f, height * 0.3f),
                        Offset(width * 0.4f, height * 0.25f),
                        Offset(width * 0.6f, height * 0.4f),
                        Offset(width * 0.8f, height * 0.5f),
                        Offset(width, height * 0.6f)
                    )
                    
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Draw points
                    points.forEach { point ->
                        drawCircle(
                            color = PrimaryIndigo,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                WeightStat(label = "Start", value = "--")
                WeightStat(label = "Current", value = "--")
                WeightStat(label = "Goal", value = "--")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "💡 Track your weight in Settings to see progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
    val color: Color
)

data class Achievement(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
