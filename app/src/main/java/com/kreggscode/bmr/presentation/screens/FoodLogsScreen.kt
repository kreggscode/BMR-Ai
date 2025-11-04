package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
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
import com.kreggscode.bmr.Screen
import com.kreggscode.bmr.presentation.viewmodels.FoodLogsViewModel
import com.kreggscode.bmr.presentation.viewmodels.DailyCalorieData
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FoodLogsScreen(
    navController: NavController,
    viewModel: FoodLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigateToAI by viewModel.navigateToAI.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    // Handle AI navigation
    LaunchedEffect(navigateToAI) {
        navigateToAI?.let { prompt ->
            // Navigate to AI Chat with the prompt pre-filled
            navController.navigate(Screen.Chat.route)
            viewModel.clearNavigation()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Column {
                    Text(
                        text = "Food Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track your meals and nutrition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(
                onClick = { navController.navigate(Screen.Scanner.route) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Food",
                    tint = PrimaryTeal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Summary Tracker Card
        TodaySummaryCard(
            totalCalories = uiState.todayTotalCalories,
            totalProtein = uiState.todayTotalProtein,
            totalCarbs = uiState.todayTotalCarbs,
            totalFat = uiState.todayTotalFat
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryTeal
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tracker") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Today") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Favorites") }
            )
        }
        
        // Content
        when (selectedTab) {
            0 -> TrackerTab(viewModel = viewModel, navController = navController)
            1 -> TodayTab(viewModel = viewModel, navController = navController)
            2 -> FavoritesTab(viewModel = viewModel, navController = navController)
        }
    }
}

@Composable
private fun TrackerTab(
    viewModel: FoodLogsViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Calorie Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track your daily calorie consumption",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            CalorieTrackerGraph(weeklyData = uiState.weeklyCalorieData)
        }
        
        if (uiState.historyMeals.isEmpty() && uiState.todayMeals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.ShowChart,
                    title = "No Data Yet",
                    message = "Start logging meals to see your calorie tracker",
                    actionLabel = "Add Food",
                    onAction = { navController.navigate(Screen.Scanner.route) }
                )
            }
        }
    }
}

@Composable
private fun TodayTab(
    viewModel: FoodLogsViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.todayMeals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Restaurant,
                    title = "No Meals Today",
                    message = "Log your first meal to start tracking",
                    actionLabel = "Log Food",
                    onAction = { navController.navigate(Screen.Scanner.route) }
                )
            }
        } else {
            items(uiState.todayMeals) { meal ->
                MealLogCard(
                    meal = meal,
                    onAskAI = { viewModel.askAIAboutMeal(meal) },
                    onDelete = { viewModel.deleteMeal(meal.id) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    viewModel: FoodLogsViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.favoriteFoods.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Star,
                    title = "No Favorites Yet",
                    message = "Favorite foods you log frequently will appear here",
                    actionLabel = null,
                    onAction = null
                )
            }
        } else {
            items(uiState.favoriteFoods) { food ->
                FavoriteFoodCard(
                    food = food,
                    onAdd = { viewModel.addFavoriteToToday(food) },
                    onRemove = { viewModel.removeFavorite(food.id) }
                )
            }
        }
    }
}

@Composable
private fun MealLogCard(
    meal: MealLogItem,
    onAskAI: () -> Unit,
    onDelete: () -> Unit,
    viewModel: FoodLogsViewModel
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${meal.mealType.capitalize()} • ${meal.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(meal.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (meal.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (meal.isFavorite) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onAskAI,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Ask AI",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroStat(label = "Calories", value = "${meal.calories.toInt()} kcal", color = AccentCoral)
                MacroStat(label = "Protein", value = "${meal.protein.toInt()}g", color = PrimaryIndigo)
                MacroStat(label = "Carbs", value = "${meal.carbs.toInt()}g", color = PrimaryTeal)
                MacroStat(label = "Fat", value = "${meal.fat.toInt()}g", color = PrimaryPurple)
            }
        }
    }
}

@Composable
private fun MacroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodaySummaryCard(
    totalCalories: Double,
    totalProtein: Double,
    totalCarbs: Double,
    totalFat: Double
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroStat(label = "Calories", value = "${totalCalories.toInt()}", color = AccentCoral)
                MacroStat(label = "Protein", value = "${totalProtein.toInt()}g", color = PrimaryIndigo)
                MacroStat(label = "Carbs", value = "${totalCarbs.toInt()}g", color = PrimaryTeal)
                MacroStat(label = "Fat", value = "${totalFat.toInt()}g", color = PrimaryPurple)
            }
        }
    }
}

@Composable
private fun FavoriteFoodCard(
    food: FavoriteFoodItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${food.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Success
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Remove Favorite",
                        tint = Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        actionLabel?.let { label ->
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedGradientButton(
                text = label,
                onClick = { onAction?.invoke() },
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }
    }
}

@Composable
private fun CalorieTrackerGraph(weeklyData: List<DailyCalorieData>) {
    val maxCalories = if (weeklyData.isNotEmpty()) {
        weeklyData.maxOfOrNull { it.calories }?.coerceAtLeast(100.0) ?: 100.0
    } else {
        100.0
    }
    val chartHeight = 200.dp
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Last 7 Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (weeklyData.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start logging meals to see your weekly progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val dataSize = weeklyData.size.coerceAtLeast(1)
                        val barWidth = size.width / (dataSize * 2f)
                        val spacing = barWidth
                        val maxHeight = size.height - 40.dp.toPx()
                        
                        weeklyData.forEachIndexed { index, data ->
                            val x = (index * (barWidth + spacing)) + barWidth / 2
                            val barHeight = (data.calories / maxCalories * maxHeight).toFloat().coerceAtLeast(4.dp.toPx())
                            val barY = size.height - barHeight - 20.dp.toPx()
                            
                            // Draw bar
                            drawRoundRect(
                                color = PrimaryTeal,
                                topLeft = Offset(x - barWidth / 2, barY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                    
                    // Day labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weeklyData.forEach { data ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(40.dp)
                            ) {
                                Text(
                                    text = "${data.calories.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = data.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MealLogItem(
    val id: Long,
    val foodName: String,
    val mealType: String,
    val time: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val date: Long,
    val isFavorite: Boolean = false
)

data class FavoriteFoodItem(
    val id: Long,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

