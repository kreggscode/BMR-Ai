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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    
    // Refresh data when screen is focused to ensure latest data
    LaunchedEffect(Unit) {
        // The Flow should automatically update, but we ensure it's triggered
        // by checking if we need to reload
    }
    val navigateToAI by viewModel.navigateToAI.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Today, 1 = Scanned Food, 2 = Tracker, 3 = Favorites
    
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
            totalFat = uiState.todayTotalFat,
            targetCalories = uiState.targetCalories,
            caloriesRemaining = uiState.caloriesRemaining
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tabs - styled to match design (Today first!)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryTeal,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PrimaryTeal,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Today") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Scanned Food") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Tracker") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Favorites") }
            )
        }
        
        // Content
        when (selectedTab) {
            0 -> TodayTab(viewModel = viewModel, navController = navController)
            1 -> ScannedFoodTab(viewModel = viewModel, navController = navController)
            2 -> TrackerTab(viewModel = viewModel, navController = navController)
            3 -> FavoritesTab(viewModel = viewModel, navController = navController)
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
            var selectedPeriod by remember { mutableStateOf(TrackerPeriod.WEEKLY) }
            CalorieTrackerGraph(
                weeklyData = uiState.weeklyCalorieData,
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it }
            )
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
            // Reverse order so newest meals appear at top
            items(uiState.todayMeals.reversed()) { meal ->
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
private fun ScannedFoodTab(
    viewModel: FoodLogsViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Filter meals that were scanned (source = "scanner")
    val scannedMeals = uiState.todayMeals.filter { 
        it.source == "scanner"
    }.sortedByDescending { it.timestamp } // Latest first by timestamp
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (scannedMeals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CameraAlt,
                    title = "No Scanned Food",
                    message = "Scan food items to see them here",
                    actionLabel = "Scan Food",
                    onAction = { navController.navigate(Screen.Scanner.route) }
                )
            }
        } else {
            items(scannedMeals) { meal ->
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
                    // Format date and time
                    val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                    val dateTimeStr = dateTimeFormat.format(java.util.Date(meal.date))
                    
                    Text(
                        text = "${meal.mealType.capitalize()} • $dateTimeStr",
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
    totalFat: Double,
    targetCalories: Double,
    caloriesRemaining: Double
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${targetCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${caloriesRemaining.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (caloriesRemaining > 0) Success else Error
                    )
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
private fun CalorieTrackerGraph(
    weeklyData: List<DailyCalorieData>,
    selectedPeriod: TrackerPeriod = TrackerPeriod.WEEKLY,
    onPeriodChange: (TrackerPeriod) -> Unit = {}
) {
    val maxCalories = if (weeklyData.isNotEmpty()) {
        val max = weeklyData.maxOfOrNull { it.calories }?.coerceAtLeast(100.0) ?: 100.0
        // Round up to nearest 100 for cleaner Y-axis
        (kotlin.math.ceil(max / 100.0) * 100.0).coerceAtLeast(200.0)
    } else {
        200.0
    }
    // Balanced graph - proper proportions
    val chartHeight = 220.dp // Compact but not too thin
    val yAxisWidth = 32.dp
    val paddingTop = 12.dp
    val paddingBottom = 4.dp // MINIMAL space for X-axis labels - they should be CLOSE
    val paddingHorizontal = 4.dp
    val xAxisLabelHeight = 20.dp // Space reserved for labels
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Period selector buttons - All 4 buttons visible side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerPeriodButton(
                    period = TrackerPeriod.DAILY,
                    isSelected = selectedPeriod == TrackerPeriod.DAILY,
                    onClick = { onPeriodChange(TrackerPeriod.DAILY) },
                    modifier = Modifier.weight(1f)
                )
                TrackerPeriodButton(
                    period = TrackerPeriod.WEEKLY,
                    isSelected = selectedPeriod == TrackerPeriod.WEEKLY,
                    onClick = { onPeriodChange(TrackerPeriod.WEEKLY) },
                    modifier = Modifier.weight(1f)
                )
                TrackerPeriodButton(
                    period = TrackerPeriod.MONTHLY,
                    isSelected = selectedPeriod == TrackerPeriod.MONTHLY,
                    onClick = { onPeriodChange(TrackerPeriod.MONTHLY) },
                    modifier = Modifier.weight(1f)
                )
                TrackerPeriodButton(
                    period = TrackerPeriod.YEARLY,
                    isSelected = selectedPeriod == TrackerPeriod.YEARLY,
                    onClick = { onPeriodChange(TrackerPeriod.YEARLY) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check if all days have 0 calories (truly empty)
            val hasData = weeklyData.any { it.calories > 0 }
            
            if (!hasData) {
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
                        text = "Start logging meals to see your progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Chart container with proper spacing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight + xAxisLabelHeight)
                    ) {
                        // Y-axis labels
                        Column(
                            modifier = Modifier
                                .height(chartHeight)
                                .width(yAxisWidth)
                                .padding(top = paddingTop)
                        ) {
                            val yAxisSteps = 4
                            val stepValue = maxCalories / yAxisSteps
                            
                            for (i in yAxisSteps downTo 0) {
                                val value = (stepValue * i).toInt()
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "$value",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        
                        // Chart area - full width usage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight)
                                .padding(start = yAxisWidth + 4.dp, top = paddingTop, end = paddingHorizontal)
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val chartWidth = size.width
                                val chartHeight = size.height
                                val dataSize = weeklyData.size.coerceAtLeast(1)
                                val spacing = chartWidth / (dataSize - 1).coerceAtLeast(1)
                                
                                // Draw subtle horizontal grid lines
                                val gridLines = 4
                                for (i in 0..gridLines) {
                                    val y = (chartHeight / gridLines) * i
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.15f),
                                        start = Offset(0f, y),
                                        end = Offset(chartWidth, y),
                                        strokeWidth = 0.5.dp.toPx()
                                    )
                                }
                                
                                // Calculate points for smooth curve
                                val points = weeklyData.mapIndexed { index, data ->
                                    val x = if (dataSize > 1) {
                                        spacing * index
                                    } else {
                                        chartWidth / 2f
                                    }
                                    val yValue = if (maxCalories > 0) {
                                        chartHeight - ((data.calories / maxCalories) * chartHeight)
                                    } else {
                                        chartHeight
                                    }
                                    Offset(x, yValue.toFloat())
                                }
                                
                                // Draw SMOOTH PARABOLIC/CURVED line using cubic bezier
                                if (points.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        if (points.size == 2) {
                                            // Simple line for 2 points
                                            moveTo(points[0].x, points[0].y)
                                            lineTo(points[1].x, points[1].y)
                                        } else {
                                            // Smooth curve using cubic bezier
                                            moveTo(points[0].x, points[0].y)
                                            
                                            for (i in 0 until points.size - 1) {
                                                val currentPoint = points[i]
                                                val nextPoint = points[i + 1]
                                                
                                                // Calculate control points for smooth curve
                                                val controlPoint1X = currentPoint.x + (nextPoint.x - currentPoint.x) / 3f
                                                val controlPoint1Y = currentPoint.y
                                                val controlPoint2X = nextPoint.x - (nextPoint.x - currentPoint.x) / 3f
                                                val controlPoint2Y = nextPoint.y
                                                
                                                cubicTo(
                                                    controlPoint1X, controlPoint1Y,
                                                    controlPoint2X, controlPoint2Y,
                                                    nextPoint.x, nextPoint.y
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Draw smooth curved line with gradient
                                    drawPath(
                                        path = path,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(PrimaryTeal, AccentSky, PrimaryTeal),
                                            startX = 0f,
                                            endX = chartWidth
                                        ),
                                        style = Stroke(width = 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                    )
                                    
                                    // Draw filled area under curve for better visual
                                    val filledPath = androidx.compose.ui.graphics.Path().apply {
                                        // Copy the path
                                        if (points.size == 2) {
                                            moveTo(points[0].x, points[0].y)
                                            lineTo(points[1].x, points[1].y)
                                        } else {
                                            moveTo(points[0].x, points[0].y)
                                            for (i in 0 until points.size - 1) {
                                                val currentPoint = points[i]
                                                val nextPoint = points[i + 1]
                                                val controlPoint1X = currentPoint.x + (nextPoint.x - currentPoint.x) / 3f
                                                val controlPoint1Y = currentPoint.y
                                                val controlPoint2X = nextPoint.x - (nextPoint.x - currentPoint.x) / 3f
                                                val controlPoint2Y = nextPoint.y
                                                cubicTo(
                                                    controlPoint1X, controlPoint1Y,
                                                    controlPoint2X, controlPoint2Y,
                                                    nextPoint.x, nextPoint.y
                                                )
                                            }
                                        }
                                        // Close the path to create filled area
                                        lineTo(points.last().x, chartHeight)
                                        lineTo(points.first().x, chartHeight)
                                        close()
                                    }
                                    
                                    drawPath(
                                        path = filledPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                PrimaryTeal.copy(alpha = 0.15f),
                                                PrimaryTeal.copy(alpha = 0.0f)
                                            ),
                                            startY = 0f,
                                            endY = chartHeight
                                        )
                                    )
                                    
                                    // Draw points with nice styling
                                    points.forEachIndexed { index, point ->
                                        // Outer glow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    PrimaryTeal.copy(alpha = 0.3f),
                                                    PrimaryTeal.copy(alpha = 0.0f)
                                                )
                                            ),
                                            radius = 14.dp.toPx(),
                                            center = point
                                        )
                                        // Main point circle
                                        drawCircle(
                                            color = PrimaryTeal,
                                            radius = 6.dp.toPx(),
                                            center = point
                                        )
                                        // Inner highlight
                                        drawCircle(
                                            color = Color.White,
                                            radius = 3.dp.toPx(),
                                            center = point
                                        )
                                        
                                        // Value labels will be drawn separately above canvas
                                    }
                                }
                            }
                            
                            // Value labels above points
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weeklyData.forEach { data ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        if (data.calories > 0) {
                                            Text(
                                                text = "${data.calories.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // X-axis labels - PERFECTLY aligned with data points, MINIMAL gap
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(xAxisLabelHeight)
                            .padding(start = yAxisWidth + 4.dp, end = paddingHorizontal)
                            .padding(top = 1.dp) // MINIMAL gap - labels RIGHT below graph
                    ) {
                        val labelAreaWidth = maxWidth
                        val dataSize = weeklyData.size.coerceAtLeast(1)
                        val spacing = labelAreaWidth / (dataSize - 1).coerceAtLeast(1)
                        
                        weeklyData.forEachIndexed { index, data ->
                            // Calculate EXACT same X position as data points
                            val xPosition = if (dataSize > 1) {
                                spacing * index
                            } else {
                                labelAreaWidth / 2f
                            }
                            
                            Text(
                                text = data.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .offset(x = xPosition - 15.dp) // Center text under point
                                    .width(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class TrackerPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

@Composable
private fun TrackerPeriodButton(
    period: TrackerPeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp) // Fixed height to ensure visibility
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Text(
            text = period.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            maxLines = 1
        )
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
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "manual",
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

