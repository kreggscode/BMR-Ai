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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.Stroke
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
            DayMealPlan(
                day = selectedDay,
                planType = uiState.selectedPlanType,
                generatedPlan = uiState.generatedPlan
            )
            
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
        
        // Show generated plan with loading indicator
        if (uiState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryTeal,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating your personalized diet plan...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Using AI with your BMR: ${uiState.bmr.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryTeal.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        // Show generated plan
        if (uiState.generatedPlan.isNotEmpty() && !uiState.showPlanDialog && !uiState.isGenerating) {
            GeneratedPlanView(
                plan = uiState.generatedPlan,
                onClose = { viewModel.closePlan() },
                onSave = { viewModel.savePlan() }
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
                .height(48.dp)
                .widthIn(min = 120.dp)
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
private fun DayMealPlan(
    day: Int,
    planType: com.kreggscode.bmr.presentation.viewmodels.DietPlanType,
    generatedPlan: String
) {
    // Generate different meals for each day
    val meals = remember(day, planType) {
        when (planType) {
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.WEIGHT_LOSS -> when (day % 7) {
                0 -> listOf( // Monday
                    MealData("Breakfast", "Oatmeal with Berries & Almonds", 350, "8:00 AM", Icons.Default.WbSunny),
        MealData("Snack", "Greek Yogurt", 150, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Grilled Chicken Salad with Quinoa", 450, "1:00 PM", Icons.Default.LunchDining),
        MealData("Snack", "Apple with Almond Butter", 180, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Baked Salmon & Steamed Vegetables", 420, "7:00 PM", Icons.Default.DinnerDining)
                )
                1 -> listOf( // Tuesday
                    MealData("Breakfast", "Scrambled Eggs (2) + Whole Grain Toast", 320, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Smoothie", 160, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Turkey Wrap with Vegetables", 480, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Nuts (30g)", 180, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Grilled Chicken & Brown Rice", 440, "7:00 PM", Icons.Default.DinnerDining)
                )
                2 -> listOf( // Wednesday
                    MealData("Breakfast", "Greek Yogurt Parfait with Berries", 340, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Orange & Walnuts", 150, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Lentil Soup & Whole Grain Bread", 460, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Cucumber & Hummus", 170, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Baked Cod & Roasted Vegetables", 430, "7:00 PM", Icons.Default.DinnerDining)
                )
                3 -> listOf( // Thursday
                    MealData("Breakfast", "Whole Grain Cereal with Blueberries", 330, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Hard-Boiled Eggs (2)", 140, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken & Vegetable Stir-Fry", 470, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Pear with Almonds", 190, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Lean Turkey Meatballs & Zucchini Noodles", 410, "7:00 PM", Icons.Default.DinnerDining)
                )
                4 -> listOf( // Friday
                    MealData("Breakfast", "Protein Pancakes with Berries", 360, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cottage Cheese with Pineapple", 155, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tuna Salad with Mixed Greens", 445, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Apple Slices & Peanut Butter", 175, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Grilled Shrimp & Quinoa Salad", 435, "7:00 PM", Icons.Default.DinnerDining)
                )
                5 -> listOf( // Saturday
                    MealData("Breakfast", "Avocado Toast with Poached Eggs", 380, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Bar", 165, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken Caesar Salad (Light Dressing)", 455, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Berries", 185, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Baked Chicken Breast & Sweet Potato", 415, "7:00 PM", Icons.Default.DinnerDining)
                )
                else -> listOf( // Sunday
                    MealData("Breakfast", "Overnight Oats with Chia Seeds", 345, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Greek Yogurt with Honey", 145, "10:30 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Grilled Fish Tacos with Slaw", 465, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Carrot Sticks & Hummus", 160, "3:30 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Herb-Crusted Chicken & Roasted Vegetables", 425, "7:00 PM", Icons.Default.DinnerDining)
                )
            }
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.MUSCLE_GAIN -> when (day % 7) {
                0 -> listOf(
                    MealData("Breakfast", "Scrambled Eggs (4) + Whole Grain Toast + Avocado", 520, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Shake + Banana", 280, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Grilled Chicken Breast + Sweet Potato + Broccoli", 620, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Oatmeal + Peanut Butter", 210, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Lean Beef Steak + Quinoa + Asparagus", 570, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Protein Shake + Rice Cakes", 320, "9:00 PM", Icons.Default.FitnessCenter)
                )
                1 -> listOf(
                    MealData("Breakfast", "Protein Oatmeal + Eggs + Berries", 510, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Greek Yogurt + Granola + Honey", 290, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Salmon + Brown Rice + Vegetables", 630, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Banana + Almonds", 220, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Chicken Thighs + Quinoa + Mixed Veggies", 580, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Whey Protein + Banana", 310, "9:00 PM", Icons.Default.FitnessCenter)
                )
                2 -> listOf(
                    MealData("Breakfast", "Egg Scramble + Hash Browns + Toast", 540, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Smoothie + Berries", 270, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Turkey + Rice Bowl + Vegetables", 610, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Dates + Almond Butter", 200, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Lean Beef + Pasta + Green Beans", 560, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Casein Protein + Oats", 330, "9:00 PM", Icons.Default.FitnessCenter)
                )
                3 -> listOf(
                    MealData("Breakfast", "Protein Pancakes + Eggs + Syrup", 550, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cottage Cheese + Fruit + Nuts", 300, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Grilled Chicken + Potatoes + Salad", 640, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Rice Cakes + Jam", 190, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Pork Tenderloin + Rice + Vegetables", 590, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Protein Bar + Milk", 340, "9:00 PM", Icons.Default.FitnessCenter)
                )
                4 -> listOf(
                    MealData("Breakfast", "Steak & Eggs + Hash Browns", 560, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Shake + Oats", 260, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken + Pasta + Mixed Vegetables", 650, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Banana + Honey", 230, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Salmon + Quinoa + Asparagus", 600, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Whey + Dextrose", 350, "9:00 PM", Icons.Default.FitnessCenter)
                )
                5 -> listOf(
                    MealData("Breakfast", "Eggs Benedict + Hash Browns", 580, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Greek Yogurt + Protein Powder + Berries", 310, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Beef + Sweet Potato + Broccoli", 660, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Dates + Nuts", 240, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Chicken + Rice + Vegetables", 610, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Protein Shake + Rice + Banana", 360, "9:00 PM", Icons.Default.FitnessCenter)
                )
                else -> listOf(
                    MealData("Breakfast", "Full English Breakfast (Lean)", 570, "7:30 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Smoothie Bowl", 280, "10:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Turkey + Rice + Vegetables", 640, "12:30 PM", Icons.Default.LunchDining),
                    MealData("Pre-Workout", "Oatmeal + Whey", 210, "3:00 PM", Icons.Default.FitnessCenter),
                    MealData("Dinner", "Lean Beef + Quinoa + Roasted Veggies", 580, "7:00 PM", Icons.Default.DinnerDining),
                    MealData("Post-Workout", "Casein Protein + Oats", 320, "9:00 PM", Icons.Default.FitnessCenter)
                )
            }
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.KETO -> when (day % 7) {
                0 -> listOf(
                    MealData("Breakfast", "Scrambled Eggs (3) + Bacon + Avocado", 420, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cheese Cubes + Macadamia Nuts", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Grilled Salmon + Cauliflower Rice + Butter", 520, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Keto Fat Bombs", 210, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Ribeye Steak + Asparagus + Butter", 470, "7:00 PM", Icons.Default.DinnerDining)
                )
                1 -> listOf(
                    MealData("Breakfast", "Keto Pancakes + Eggs + Sausage", 410, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Pork Rinds + Cheese", 150, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken Thighs + Zucchini Noodles", 530, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Almonds + Dark Chocolate (90%)", 180, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Lamb Chops + Broccoli + Butter", 480, "7:00 PM", Icons.Default.DinnerDining)
                )
                2 -> listOf(
                    MealData("Breakfast", "Eggs + Sausage + Avocado", 430, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cheese Crisps", 155, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Bacon-Wrapped Chicken + Cauliflower", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Keto Smoothie (MCT Oil)", 200, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Salmon + Spinach + Olive Oil", 460, "7:00 PM", Icons.Default.DinnerDining)
                )
                3 -> listOf(
                    MealData("Breakfast", "Keto Omelette + Cheese + Bacon", 440, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Macadamia Nuts", 170, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Beef + Cauliflower Rice + Butter", 540, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Cheese + Olives", 190, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Pork Chops + Green Beans + Butter", 490, "7:00 PM", Icons.Default.DinnerDining)
                )
                4 -> listOf(
                    MealData("Breakfast", "Bulletproof Coffee + Eggs", 400, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Pecans + Cheese", 165, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tuna Salad (No Bread) + Avocado", 525, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Keto Fat Bombs", 205, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Steak + Asparagus + Butter", 475, "7:00 PM", Icons.Default.DinnerDining)
                )
                5 -> listOf(
                    MealData("Breakfast", "Eggs + Bacon + Avocado + Cheese", 450, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Pork Rinds", 145, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken + Cauliflower Mash + Butter", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Almonds + Coconut Oil", 195, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Lamb + Cauliflower + Butter", 485, "7:00 PM", Icons.Default.DinnerDining)
                )
                else -> listOf(
                    MealData("Breakfast", "Keto Breakfast Casserole", 435, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cheese + Nuts", 175, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Salmon + Zucchini + Butter", 505, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Keto Smoothie", 185, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Beef + Broccoli + Butter", 465, "7:00 PM", Icons.Default.DinnerDining)
                )
            }
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.MAINTENANCE -> when (day % 7) {
                0 -> listOf(
                    MealData("Breakfast", "Whole Grain Cereal + Milk + Banana", 410, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Mixed Nuts", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Turkey Sandwich + Side Salad", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Hummus + Vegetables", 210, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Grilled Fish + Roasted Vegetables + Quinoa", 460, "7:00 PM", Icons.Default.DinnerDining)
                )
                1 -> listOf(
                    MealData("Breakfast", "Avocado Toast + Eggs", 420, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Apple + Peanut Butter", 165, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken Salad Wrap + Fruit", 500, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Greek Yogurt + Berries", 195, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Pasta with Marinara + Salad", 470, "7:00 PM", Icons.Default.DinnerDining)
                )
                2 -> listOf(
                    MealData("Breakfast", "Oatmeal + Berries + Nuts", 400, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Trail Mix", 170, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Quinoa Bowl + Vegetables + Chicken", 520, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Orange + Almonds", 180, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Salmon + Rice + Vegetables", 480, "7:00 PM", Icons.Default.DinnerDining)
                )
                3 -> listOf(
                    MealData("Breakfast", "Scrambled Eggs + Toast + Fruit", 430, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cottage Cheese + Pineapple", 155, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Burrito Bowl + Vegetables", 505, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Nuts", 200, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Chicken + Potatoes + Vegetables", 490, "7:00 PM", Icons.Default.DinnerDining)
                )
                4 -> listOf(
                    MealData("Breakfast", "Greek Yogurt Parfait", 390, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Banana + Peanut Butter", 175, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tuna Salad + Whole Grain Bread", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Hummus + Carrots", 190, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Pork Tenderloin + Rice + Vegetables", 485, "7:00 PM", Icons.Default.DinnerDining)
                )
                5 -> listOf(
                    MealData("Breakfast", "Pancakes + Syrup + Berries", 440, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Protein Bar", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Chicken + Pasta + Salad", 525, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Berries", 185, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Fish + Quinoa + Vegetables", 495, "7:00 PM", Icons.Default.DinnerDining)
                )
                else -> listOf(
                    MealData("Breakfast", "French Toast + Berries", 450, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Apple + Cheese", 150, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Turkey + Rice + Vegetables", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Nuts + Dried Fruit", 205, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Beef + Potatoes + Vegetables", 500, "7:00 PM", Icons.Default.DinnerDining)
                )
            }
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.VEGETARIAN -> when (day % 7) {
                0 -> listOf(
                    MealData("Breakfast", "Tofu Scramble + Whole Grain Toast + Fruit", 410, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Hummus + Carrots", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Lentil Curry + Brown Rice + Naan", 520, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Greek Yogurt + Honey", 210, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Chickpea Buddha Bowl + Quinoa + Tahini", 460, "7:00 PM", Icons.Default.DinnerDining)
                )
                1 -> listOf(
                    MealData("Breakfast", "Overnight Oats + Berries + Nuts", 400, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cheese + Crackers", 165, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Black Bean Burger + Sweet Potato Fries", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Apple + Almond Butter", 195, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Vegetable Lasagna + Salad", 470, "7:00 PM", Icons.Default.DinnerDining)
                )
                2 -> listOf(
                    MealData("Breakfast", "Scrambled Eggs + Toast + Fruit", 420, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Trail Mix", 170, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Falafel Wrap + Hummus + Vegetables", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Greek Yogurt + Granola", 180, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Tempeh Stir-Fry + Brown Rice", 485, "7:00 PM", Icons.Default.DinnerDining)
                )
                3 -> listOf(
                    MealData("Breakfast", "Avocado Toast + Eggs + Fruit", 430, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cottage Cheese + Berries", 155, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Quinoa Salad + Feta + Vegetables", 505, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Nuts + Dried Fruit", 200, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Lentil Soup + Whole Grain Bread", 490, "7:00 PM", Icons.Default.DinnerDining)
                )
                4 -> listOf(
                    MealData("Breakfast", "Greek Yogurt + Granola + Berries", 390, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Cheese + Crackers", 175, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Egg Salad Sandwich + Salad", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Hummus + Vegetables", 190, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Chickpea Curry + Rice + Naan", 485, "7:00 PM", Icons.Default.DinnerDining)
                )
                5 -> listOf(
                    MealData("Breakfast", "Protein Pancakes + Berries", 440, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Apple + Peanut Butter", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tofu + Rice + Vegetables", 525, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Trail Mix", 185, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Vegetable Pizza + Salad", 495, "7:00 PM", Icons.Default.DinnerDining)
                )
                else -> listOf(
                    MealData("Breakfast", "Omelette + Vegetables + Toast", 450, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Greek Yogurt + Honey", 150, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Bean Burrito + Vegetables", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Nuts", 205, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Eggplant Parmesan + Pasta", 500, "7:00 PM", Icons.Default.DinnerDining)
                )
            }
            com.kreggscode.bmr.presentation.viewmodels.DietPlanType.VEGAN -> when (day % 7) {
                0 -> listOf(
                    MealData("Breakfast", "Oatmeal + Plant Milk + Chia Seeds + Berries", 410, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Apple + Almond Butter", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tempeh Stir-Fry + Brown Rice + Vegetables", 520, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Trail Mix", 210, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Black Bean Burger + Sweet Potato Fries + Salad", 460, "7:00 PM", Icons.Default.DinnerDining)
                )
                1 -> listOf(
                    MealData("Breakfast", "Smoothie Bowl + Granola + Berries", 400, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Hummus + Carrots", 165, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Lentil Curry + Rice + Vegetables", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Nuts + Dried Fruit", 195, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Tofu + Quinoa + Roasted Vegetables", 485, "7:00 PM", Icons.Default.DinnerDining)
                )
                2 -> listOf(
                    MealData("Breakfast", "Avocado Toast + Chickpeas + Vegetables", 420, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Trail Mix", 170, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Falafel Bowl + Hummus + Vegetables", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Apple + Almonds", 180, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Chickpea Curry + Rice + Naan", 490, "7:00 PM", Icons.Default.DinnerDining)
                )
                3 -> listOf(
                    MealData("Breakfast", "Tofu Scramble + Vegetables + Toast", 430, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Hummus + Vegetables", 155, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Quinoa Salad + Beans + Vegetables", 505, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Berries", 200, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Lentil Soup + Whole Grain Bread", 480, "7:00 PM", Icons.Default.DinnerDining)
                )
                4 -> listOf(
                    MealData("Breakfast", "Overnight Oats + Plant Milk + Berries", 390, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Almonds + Dates", 175, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Black Bean Bowl + Rice + Vegetables", 525, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Trail Mix", 190, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Tempeh + Sweet Potato + Vegetables", 495, "7:00 PM", Icons.Default.DinnerDining)
                )
                5 -> listOf(
                    MealData("Breakfast", "Vegan Pancakes + Maple Syrup + Berries", 440, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Apple + Peanut Butter", 160, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Tofu + Rice + Vegetables", 515, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Mixed Nuts", 185, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Vegan Pizza + Salad", 500, "7:00 PM", Icons.Default.DinnerDining)
                )
                else -> listOf(
                    MealData("Breakfast", "Smoothie + Protein Powder + Berries", 450, "8:00 AM", Icons.Default.WbSunny),
                    MealData("Snack", "Hummus + Vegetables", 150, "11:00 AM", Icons.Default.Cookie),
                    MealData("Lunch", "Bean Burrito + Vegetables", 510, "1:00 PM", Icons.Default.LunchDining),
                    MealData("Snack", "Trail Mix + Dried Fruit", 205, "4:00 PM", Icons.Default.Cookie),
                    MealData("Dinner", "Chickpea Buddha Bowl + Quinoa + Tahini", 490, "7:00 PM", Icons.Default.DinnerDining)
                )
            }
        }
    }
    
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
        onDismissRequest = if (isGenerating) ({}) else onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Generate AI Diet Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isGenerating) {
                    Text(
                        text = "Our AI will create a personalized meal plan based on your BMR, goals, and selected diet type.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    
                    // Features list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        FeatureItem("✓ Personalized calorie targets")
                        FeatureItem("✓ Macro-balanced meals")
                        FeatureItem("✓ Daily meal schedule")
                        FeatureItem("✓ Nutrition tips")
                    }
                } else {
                    // Loading state with animated progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = PrimaryIndigo,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "🤖 AI is creating your plan...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This may take a few seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isGenerating) {
                AnimatedGradientButton(
                    text = "Generate Plan",
                    onClick = onGenerate,
                    isLoading = false,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(0.6f)
                )
            }
        },
        dismissButton = {
            if (!isGenerating) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GeneratedPlanView(
    plan: String,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(2.dp, PrimaryTeal.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Generated Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onSave,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Plan",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                                .background(Error.copy(alpha = 0.2f))
                    ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Error,
                                modifier = Modifier.size(20.dp)
                        )
                        }
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
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Format the plan text properly
                    val formattedPlan = plan
                        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove markdown bold
                        .replace(Regex("^\\s*\\*\\s+"), "• ") // Convert * to bullets
                        .replace(Regex("^\\s*\\d+\\.\\s+"), "") // Remove numbers
                    
                    // Split into paragraphs and format
                    val paragraphs = formattedPlan.split("\n\n").filter { it.isNotBlank() }
                    
                    paragraphs.forEach { paragraph ->
                        val trimmed = paragraph.trim()
                        if (trimmed.isNotEmpty()) {
                            // Check if it's a heading (all caps or starts with #)
                            val isHeading = trimmed.matches(Regex("^[A-Z\\s]+$")) || 
                                          trimmed.startsWith("#") ||
                                          trimmed.matches(Regex("^\\*\\*.*\\*\\*$"))
                            
                            if (isHeading) {
                    Text(
                                    text = trimmed.replace("#", "").trim(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Text(
                                    text = trimmed,
                                    style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 28.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                    )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
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
