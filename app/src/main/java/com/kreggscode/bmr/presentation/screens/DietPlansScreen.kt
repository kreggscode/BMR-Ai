package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*

@Composable
fun DietPlansScreen(
    navController: NavController,
    viewModel: com.kreggscode.bmr.presentation.viewmodels.DietPlansViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf(0) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    
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
            DietPlanHeader(
                onBackClick = { navController.navigateUp() },
                onGenerateClick = { viewModel.togglePlanDialog(true) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Plan Type Selector
            PlanTypeSelector(
                selectedType = uiState.selectedPlanType,
                onTypeSelect = viewModel::selectPlanType
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Plan Overview
            CurrentPlanCard(
                planType = uiState.selectedPlanType,
                targetCalories = uiState.targetCalories,
                proteinGrams = uiState.proteinGrams,
                carbsGrams = uiState.carbsGrams,
                fatGrams = uiState.fatGrams
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Day Selector
            DaySelector(
                selectedDay = selectedDay,
                onDaySelect = { selectedDay = it }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Meals for Selected Day
            DayMealPlan(day = selectedDay)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Shopping List
            ShoppingListCard(shoppingList = uiState.shoppingList)
        }
        
        if (uiState.showPlanDialog) {
            GeneratePlanDialog(
                onDismiss = { viewModel.togglePlanDialog(false) },
                onGenerate = { viewModel.generateDietPlan() },
                isGenerating = uiState.isGenerating
            )
        }
        
        // Show generated plan
        if (uiState.generatedPlan.isNotEmpty() && !uiState.showPlanDialog) {
            GeneratedPlanView(
                plan = uiState.generatedPlan,
                onClose = { viewModel.closePlan() }
            )
        }
    }
}

@Composable
private fun DietPlanHeader(
    onBackClick: () -> Unit,
    onGenerateClick: () -> Unit
) {
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
                    text = "Diet Plans",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "AI-Generated Meal Plans",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryPurple
                )
            }
        }
        
        AnimatedGradientButton(
            text = "Generate",
            onClick = onGenerateClick,
            modifier = Modifier
                .height(40.dp)
                .widthIn(min = 100.dp)
        )
    }
}

@Composable
private fun PlanTypeSelector(
    selectedType: com.kreggscode.bmr.presentation.viewmodels.DietPlanType,
    onTypeSelect: (com.kreggscode.bmr.presentation.viewmodels.DietPlanType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(com.kreggscode.bmr.presentation.viewmodels.DietPlanType.values().size) { index ->
            val planType = com.kreggscode.bmr.presentation.viewmodels.DietPlanType.values()[index]
            PlanTypeChip(
                planType = planType,
                selected = selectedType == planType,
                onClick = { onTypeSelect(planType) }
            )
        }
    }
}

@Composable
private fun PlanTypeChip(
    planType: com.kreggscode.bmr.presentation.viewmodels.DietPlanType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo else Color.Transparent,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = planType.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CurrentPlanCard(
    planType: com.kreggscode.bmr.presentation.viewmodels.DietPlanType,
    targetCalories: Double,
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${planType.displayName} Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlanStat(label = "Daily", value = targetCalories.toInt().toString(), unit = "kcal", color = AccentCoral)
                    PlanStat(label = "Protein", value = proteinGrams.toInt().toString(), unit = "g", color = PrimaryIndigo)
                    PlanStat(label = "Carbs", value = carbsGrams.toInt().toString(), unit = "g", color = PrimaryTeal)
                    PlanStat(label = "Fat", value = fatGrams.toInt().toString(), unit = "g", color = PrimaryPurple)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = 0.7f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Success,
                    trackColor = Success.copy(alpha = 0.1f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Day 5 of 7",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanStat(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: Int,
    onDaySelect: (Int) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days.size) { index ->
            DayChip(
                day = days[index],
                dayNumber = index + 1,
                selected = selectedDay == index,
                onClick = { onDaySelect(index) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: String,
    dayNumber: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo else Color.Transparent,
        animationSpec = tween(300)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Day $dayNumber",
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) Color.White.copy(alpha = 0.8f) 
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DayMealPlan(day: Int) {
    val meals = listOf(
        MealData("Breakfast", "Oatmeal with Berries", 350, "8:00 AM", Icons.Default.WbSunny),
        MealData("Snack", "Greek Yogurt", 150, "10:30 AM", Icons.Default.Cookie),
        MealData("Lunch", "Grilled Chicken Salad", 450, "1:00 PM", Icons.Default.LunchDining),
        MealData("Snack", "Apple with Almond Butter", 180, "3:30 PM", Icons.Default.Cookie),
        MealData("Dinner", "Salmon with Vegetables", 420, "7:00 PM", Icons.Default.DinnerDining)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Meal Schedule",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        meals.forEach { meal ->
            MealCard(meal = meal)
        }
    }
}

@Composable
private fun MealCard(meal: MealData) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            PrimaryTeal.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meal.icon,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = meal.type,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = meal.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${meal.calories}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentCoral
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCoral.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ShoppingListCard(shoppingList: Map<String, List<String>>) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shopping List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                TextButton(onClick = { /* TODO: Export list */ }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (shoppingList.isEmpty()) {
                Text(
                    text = "Select a diet plan to see shopping list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                shoppingList.forEach { (category, items) ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryIndigo,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun GeneratePlanDialog(
    onDismiss: () -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Generate AI Diet Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Our AI will create a personalized meal plan based on your BMR, goals, and selected diet type.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generating plan...",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPurple
                        )
                    }
                }
            }
        },
        confirmButton = {
            AnimatedGradientButton(
                text = "Generate",
                onClick = onGenerate,
                isLoading = isGenerating,
                modifier = Modifier.height(48.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GeneratedPlanView(
    plan: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .systemBarsPadding()
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) { },
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your AI Diet Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp) // Prevent text cutoff
                ) {
                    Text(
                        text = plan,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
                }
            }
        }
    }
}

private data class MealData(
    val type: String,
    val name: String,
    val calories: Int,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
