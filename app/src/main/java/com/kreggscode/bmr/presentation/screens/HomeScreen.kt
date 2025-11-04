package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kreggscode.bmr.Screen
import com.kreggscode.bmr.presentation.viewmodels.HomeViewModel
import com.kreggscode.bmr.presentation.viewmodels.SettingsViewModel
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
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
                .padding(bottom = 100.dp) // Space for floating nav
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            HomeHeader(
                userName = uiState.userName,
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                viewModel = viewModel
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Stats
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Grid
            StatsGrid(
                bmr = uiState.bmr,
                caloriesConsumed = uiState.caloriesConsumed,
                caloriesRemaining = uiState.caloriesRemaining,
                waterIntake = uiState.waterIntake,
                onWaterIncrement = { navController.navigate(Screen.WaterTracking.route) },
                navController = navController
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calorie Tracker Graph
            CalorieTrackerCard(
                targetCalories = uiState.targetCalories,
                consumed = uiState.caloriesConsumed,
                remaining = uiState.caloriesRemaining
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QuickActionButtons(navController)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Motivation Card
            MotivationCard(
                quote = uiState.motivationalQuote,
                onRefresh = { viewModel.refreshMotivation() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Meals Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Meals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                TextButton(
                    onClick = { navController.navigate(Screen.FoodLogs.route) }
                ) {
                    Text(
                        text = "View All",
                        color = PrimaryTeal
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.recentMeals.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentMeals) { meal ->
                        MealCard(meal = meal)
                    }
                }
            } else {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No meals logged today",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { navController.navigate(Screen.Scanner.route) }
                        ) {
                            Text("Log Your First Meal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    
    // Get theme preferences directly
    val themeViewModel: SettingsViewModel = hiltViewModel()
    val darkModeState by themeViewModel.isDarkMode.collectAsState(initial = false)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dark/Light Mode Toggle
            IconButton(
                onClick = { themeViewModel.toggleDarkMode(!darkModeState) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryTeal.copy(alpha = 0.1f),
                                AccentSky.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = if (darkModeState) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (darkModeState) "Light Mode" else "Dark Mode",
                    tint = if (darkModeState) Warning else PrimaryTeal
                )
            }
            
            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryIndigo.copy(alpha = 0.1f),
                                PrimaryPurple.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = PrimaryIndigo
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    bmr: Double,
    caloriesConsumed: Double,
    caloriesRemaining: Double,
    waterIntake: Int,
    onWaterIncrement: () -> Unit,
    navController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                title = "BMR",
                value = "${bmr.toInt()}",
                subtitle = "kcal/day",
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(AccentCoral, Warning),
                maxLines = 1
            )
            
            StatCard(
                title = "Consumed",
                value = "${caloriesConsumed.toInt()}",
                subtitle = "kcal",
                icon = {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(PrimaryTeal, AccentMint),
                maxLines = 1
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                title = "Remaining",
                value = "${caloriesRemaining.toInt()}",
                subtitle = "kcal\n(TDEE - Consumed)",
                icon = {
                    Icon(
                        Icons.Default.BatteryStd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(PrimaryIndigo, PrimaryPurple),
                maxLines = 2
            )
            
            StatCard(
                title = "Water",
                value = "$waterIntake",
                subtitle = "glasses",
                icon = {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.weight(1f).clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { onWaterIncrement() },
                gradientColors = listOf(AccentSky, PrimaryTeal),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalorieTrackerCard(
    targetCalories: Double,
    consumed: Double,
    remaining: Double
) {
    val progress: Float = if (targetCalories > 0) {
        (consumed / targetCalories).toFloat().coerceIn(0f, 1f)
    } else {
        0f
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
                    text = "Calorie Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            val progressColor: Color = when {
                progress < 0.5f -> Success
                progress < 0.8f -> Warning
                progress < 1.0f -> AccentCoral
                else -> Error
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${consumed.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "Consumed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${remaining.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0) Success else Error
                    )
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${targetCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Text(
                        text = "Target (TDEE)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Remaining = Target (TDEE) - Consumed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuickActionButtons(navController: NavController) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickActionCard(
                icon = Icons.Default.AddCircle,
                label = "Log Food",
                gradientColors = listOf(PrimaryTeal, AccentMint),
                onClick = { navController.navigate(Screen.Scanner.route) }
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.CameraAlt,
                label = "Scan Food",
                gradientColors = listOf(PrimaryIndigo, PrimaryPurple),
                onClick = { navController.navigate(Screen.Scanner.route) }
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.Calculate,
                label = "Calculate BMR",
                gradientColors = listOf(AccentCoral, PrimaryPink),
                onClick = { navController.navigate(Screen.Calculator.route) }
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.SmartToy,
                label = "AI Analysis",
                gradientColors = listOf(PrimaryPurple, PrimaryPink),
                onClick = { navController.navigate(Screen.Chat.route) }
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.Bedtime,
                label = "Sleep",
                gradientColors = listOf(PrimaryIndigo, PrimaryPurple),
                onClick = { navController.navigate(Screen.SleepTracking.route) }
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    GlassmorphicCard(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MotivationCard(
    quote: String,
    onRefresh: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = PrimaryIndigo.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MealCard(meal: RecentMeal) {
    GlassmorphicCard(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = meal.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = meal.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${meal.calories} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WaterIntakeDialog(
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit,
    onReset: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Water",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset water intake",
                        tint = PrimaryTeal
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select glass size:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val glassSizes = listOf(
                    "Small (150ml)" to 150,
                    "Medium (250ml)" to 250,
                    "Large (350ml)" to 350,
                    "Bottle (500ml)" to 500,
                    "Large Bottle (750ml)" to 750,
                    "Extra Large (1000ml)" to 1000
                )
                
                glassSizes.forEach { (label, size) ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddWater(size) },
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

data class RecentMeal(
    val type: String,
    val name: String,
    val calories: Int,
    val time: String
)
