package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kreggscode.bmr.presentation.viewmodels.BMRCalculatorViewModel
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*

@Composable
fun BMRCalculatorScreen(
    navController: NavController,
    viewModel: BMRCalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp), // Space for floating nav
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            CalculatorHeader(
                onBackClick = { navController.navigateUp() },
                isMetric = uiState.isMetric,
                onToggleUnit = viewModel::toggleUnitSystem
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Input Section
            AnimatedVisibility(
                visible = !uiState.showResults,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                CalculatorInputSection(
                    age = uiState.age,
                    sex = uiState.sex,
                    height = uiState.height,
                    weight = uiState.weight,
                    activityLevel = uiState.activityLevel,
                    formula = uiState.formula,
                    goal = uiState.goal,
                    isMetric = uiState.isMetric,
                    onAgeChange = viewModel::updateAge,
                    onSexChange = viewModel::updateSex,
                    onHeightChange = viewModel::updateHeight,
                    onWeightChange = viewModel::updateWeight,
                    onActivityChange = viewModel::updateActivityLevel,
                    onFormulaChange = viewModel::updateFormula,
                    onGoalChange = viewModel::updateGoal
                )
            }
            
            // Calculate Button
            AnimatedVisibility(
                visible = !uiState.showResults,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                AnimatedGradientButton(
                    text = "Calculate BMR",
                    onClick = viewModel::calculateBMR,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isCalculating
                )
            }
            
            // Results Section
            AnimatedVisibility(
                visible = uiState.showResults,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Column {
                    BMRResultsSection(
                        bmr = uiState.calculatedBMR,
                        tdee = uiState.calculatedTDEE,
                        targetCalories = uiState.targetCalories,
                        proteinGrams = uiState.proteinGrams,
                        carbsGrams = uiState.carbsGrams,
                        fatGrams = uiState.fatGrams,
                        isLoadingAI = uiState.isLoadingAI,
                        onSave = {
                            viewModel.saveBMRRecord()
                            navController.navigateUp() // Navigate back after saving
                        },
                        onRecalculate = viewModel::resetCalculation,
                        onAIAnalysis = viewModel::requestAIAnalysis
                    )
                    
                    // AI Analysis
                    uiState.aiAnalysis?.let { analysis ->
                        Spacer(modifier = Modifier.height(20.dp))
                        AIAnalysisCard(
                            analysis = analysis,
                            bmr = uiState.calculatedBMR,
                            tdee = uiState.calculatedTDEE,
                            targetCalories = uiState.targetCalories,
                            proteinGrams = uiState.proteinGrams,
                            carbsGrams = uiState.carbsGrams,
                            fatGrams = uiState.fatGrams
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorHeader(
    onBackClick: () -> Unit,
    isMetric: Boolean,
    onToggleUnit: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top Row: Back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BMR Calculator",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Calculate your metabolic rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Toggle Row: Centered
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
        
        // Metric/Imperial Toggle - Enhanced Design
        GlassmorphicCard(
            modifier = Modifier.wrapContentSize(),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Metric Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isMetric) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryTeal, PrimaryIndigo)
                                    )
                                )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onToggleUnit(true) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Metric",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isMetric) Color.White 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = if (isMetric) FontWeight.Bold else FontWeight.Medium
                    )
                }
                
                // Imperial Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (!isMetric) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryTeal, PrimaryIndigo)
                                    )
                                )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onToggleUnit(false) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Imperial",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (!isMetric) Color.White 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = if (!isMetric) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun CalculatorInputSection(
    age: String,
    sex: String,
    height: String,
    weight: String,
    activityLevel: String,
    formula: String,
    goal: String,
    isMetric: Boolean,
    onAgeChange: (String) -> Unit,
    onSexChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onActivityChange: (String) -> Unit,
    onFormulaChange: (String) -> Unit,
    onGoalChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Age Slider
        SliderInputCard(
            title = "Age",
            value = age.toFloatOrNull() ?: 25f,
            onValueChange = { onAgeChange(it.toInt().toString()) },
            unit = "years",
            range = 1f..120f,
            step = 1f,
            icon = {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
        )
        
        // Sex Selection Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Text(
                text = "Sex",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectableChip(
                    text = "Male",
                    selected = sex == "male",
                    onClick = { onSexChange("male") },
                    modifier = Modifier.weight(1f)
                )
                SelectableChip(
                    text = "Female",
                    selected = sex == "female",
                    onClick = { onSexChange("female") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Height Slider
        SliderInputCard(
            title = "Height",
            value = height.toFloatOrNull() ?: if (isMetric) 170f else 67f,
            onValueChange = { onHeightChange(it.toInt().toString()) },
            unit = if (isMetric) "cm" else "in",
            range = if (isMetric) 10f..250f else 4f..98f,
            step = 1f,
            icon = {
                Icon(
                    imageVector = Icons.Default.Height,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        )
        
        // Weight Slider
        SliderInputCard(
            title = "Weight",
            value = weight.toFloatOrNull() ?: if (isMetric) 70f else 154f,
            onValueChange = { onWeightChange(String.format("%.1f", it)) },
            unit = if (isMetric) "kg" else "lbs",
            range = if (isMetric) 1f..300f else 2f..660f,
            step = if (isMetric) 0.5f else 1f,
            icon = {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            gradientColors = listOf(Color(0xFFEC4899), Color(0xFFEF4444))
        )
        
        // Activity & Formula Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Text(
                text = "Activity & Formula",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Activity Level
            Text(
                text = "Activity Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ActivityLevelSelector(
                selected = activityLevel,
                onSelect = onActivityChange
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Formula Selection
            Text(
                text = "Formula",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectableChip(
                    text = "Mifflin-St Jeor",
                    selected = formula == "mifflin",
                    onClick = { onFormulaChange("mifflin") },
                    modifier = Modifier.weight(1f)
                )
                SelectableChip(
                    text = "Harris-Benedict",
                    selected = formula == "harris",
                    onClick = { onFormulaChange("harris") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Goal Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Text(
                text = "Your Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalOption(
                    title = "Lose Weight",
                    description = "Create a caloric deficit",
                    icon = Icons.Default.TrendingDown,
                    selected = goal == "lose",
                    gradientColors = listOf(AccentCoral, Warning),
                    onClick = { onGoalChange("lose") }
                )
                GoalOption(
                    title = "Maintain Weight",
                    description = "Keep your current weight",
                    icon = Icons.Default.Balance,
                    selected = goal == "maintain",
                    gradientColors = listOf(PrimaryTeal, AccentMint),
                    onClick = { onGoalChange("maintain") }
                )
                GoalOption(
                    title = "Gain Weight",
                    description = "Create a caloric surplus",
                    icon = Icons.Default.TrendingUp,
                    selected = goal == "gain",
                    gradientColors = listOf(PrimaryIndigo, PrimaryPurple),
                    onClick = { onGoalChange("gain") }
                )
            }
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigo
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryIndigo,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = PrimaryIndigo
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectableChip(
    text: String,
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
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActivityLevelSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val levels = listOf(
        "sedentary" to "Little or no exercise",
        "light" to "Light exercise 1-3 days/week",
        "moderate" to "Moderate exercise 3-5 days/week",
        "active" to "Heavy exercise 6-7 days/week",
        "very_active" to "Very heavy physical job"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (value, description) ->
            ActivityLevelOption(
                title = value.replace("_", " ").capitalize(),
                description = description,
                selected = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun ActivityLevelOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = PrimaryIndigo,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) gradientColors[0].copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.5.dp,
                color = if (selected) gradientColors[0] else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = if (selected) Brush.linearGradient(gradientColors)
                    else Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BMRResultsSection(
    bmr: Double,
    tdee: Double,
    targetCalories: Double,
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
    isLoadingAI: Boolean,
    onSave: () -> Unit,
    onRecalculate: () -> Unit,
    onAIAnalysis: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular BMR Display
        CircularBMRDisplay(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories
        )
        
        // Macros Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Text(
                text = "Recommended Macros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MacroDisplay(
                protein = proteinGrams,
                carbs = carbsGrams,
                fat = fatGrams
            )
        }
        
        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onRecalculate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryIndigo
                ),
                border = BorderStroke(1.dp, PrimaryIndigo)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recalculate")
            }
            
            AnimatedGradientButton(
                text = "Save & Use",
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }
        
        // AI Analysis Button
        AnimatedGradientButton(
            text = "Get AI Analysis",
            onClick = onAIAnalysis,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.SmartToy,
            isLoading = isLoadingAI
        )
        
        // Analysis & Progression Tips
        Spacer(modifier = Modifier.height(8.dp))
        BMRAnalysisCard(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories
        )
    }
}

@Composable
private fun CircularBMRDisplay(
    bmr: Double,
    tdee: Double,
    targetCalories: Double
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryIndigo.copy(alpha = 0.05f),
                        PrimaryPurple.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )
    ) {
        // Rotating gradient border
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            PrimaryTeal,
                            PrimaryIndigo,
                            PrimaryPurple,
                            PrimaryPink,
                            PrimaryTeal
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BMR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${bmr.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = PrimaryIndigo
            )
            Text(
                text = "kcal/day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TDEE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tdee.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${targetCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroDisplay(
    protein: Double,
    carbs: Double,
    fat: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroItem(
            label = "Protein",
            value = "${protein.toInt()}g",
            color = AccentCoral
        )
        MacroItem(
            label = "Carbs",
            value = "${carbs.toInt()}g",
            color = PrimaryTeal
        )
        MacroItem(
            label = "Fat",
            value = "${fat.toInt()}g",
            color = PrimaryPurple
        )
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BMRAnalysisCard(
    bmr: Double,
    tdee: Double,
    targetCalories: Double
) {
    val deficit = targetCalories - tdee
    val goalType = when {
        deficit < -200 -> "Weight Loss"
        deficit > 200 -> "Weight Gain"
        else -> "Maintenance"
    }
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Analysis & Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Divider()
            
            // Goal Analysis
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Goal: $goalType",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryIndigo
                )
                when {
                    deficit < -200 -> {
                        Text(
                            text = "• You're in a ${kotlin.math.abs(deficit.toInt())} kcal/day deficit for healthy weight loss.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Expected loss: ~${String.format("%.1f", kotlin.math.abs(deficit) * 7 / 7700)} kg/week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    deficit > 200 -> {
                        Text(
                            text = "• You're in a ${deficit.toInt()} kcal/day surplus for muscle gain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Expected gain: ~${String.format("%.1f", deficit * 7 / 7700)} kg/week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "• Your calories are balanced for weight maintenance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Divider()
            
            // Progression Tips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "💡 Progression Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
                Text(
                    text = "• Track your daily intake consistently for best results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Recalculate your BMR every 4-6 weeks or after significant weight changes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Monitor your progress in the Progress screen to see trends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Adjust your activity level if your daily routine changes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Key Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${bmr.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Text(
                        text = "Base BMR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tdee.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "TDEE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${targetCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AIAnalysisCard(
    analysis: String,
    bmr: Double,
    tdee: Double,
    targetCalories: Double,
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Card with Gradient
        PremiumCard(
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(PrimaryPurple, PrimaryIndigo, PrimaryTeal)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "AI Metabolic Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Personalized Health Insights",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        
        // Progressive Circles Display
        ProgressiveCirclesAnalysis(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams
        )
        
        // Formatted Text Summary
        FormattedAnalysisSummary(analysis = analysis)
    }
}

@Composable
private fun ProgressiveCirclesAnalysis(
    bmr: Double,
    tdee: Double,
    targetCalories: Double,
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double
) {
    // Calculate progress values (normalized to 0-1)
    val bmrProgress = (bmr / 3000.0).coerceIn(0.0, 1.0)
    val tdeeProgress = (tdee / 4000.0).coerceIn(0.0, 1.0)
    val targetProgress = (targetCalories / 3500.0).coerceIn(0.0, 1.0)
    
    // Calculate macro percentages from grams
    val totalCalories = targetCalories.coerceAtLeast(1.0)
    val proteinCalories = proteinGrams * 4
    val carbsCalories = carbsGrams * 4
    val fatCalories = fatGrams * 9
    
    val proteinPercent = ((proteinCalories / totalCalories) * 100).toInt().coerceIn(0, 100)
    val carbsPercent = ((carbsCalories / totalCalories) * 100).toInt().coerceIn(0, 100)
    val fatPercent = ((fatCalories / totalCalories) * 100).toInt().coerceIn(0, 100)
    
    val proteinProgress = (proteinPercent / 100.0).coerceIn(0.0, 1.0)
    val carbsProgress = (carbsPercent / 100.0).coerceIn(0.0, 1.0)
    val fatProgress = (fatPercent / 100.0).coerceIn(0.0, 1.0)
    
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Metabolic Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // BMR, TDEE, Target Circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (bmr > 0) {
                    ProgressCircle(
                        label = "BMR",
                        value = bmr.toInt(),
                        unit = "kcal",
                        progress = bmrProgress.toFloat(),
                        color = PrimaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (tdee > 0) {
                    ProgressCircle(
                        label = "TDEE",
                        value = tdee.toInt(),
                        unit = "kcal",
                        progress = tdeeProgress.toFloat(),
                        color = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (targetCalories > 0) {
                    ProgressCircle(
                        label = "Target",
                        value = targetCalories.toInt(),
                        unit = "kcal",
                        progress = targetProgress.toFloat(),
                        color = PrimaryPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Macronutrient Balance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Macro Circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressCircle(
                    label = "Protein",
                    value = proteinPercent,
                    unit = "%",
                    progress = proteinProgress.toFloat(),
                    color = AccentCoral,
                    modifier = Modifier.weight(1f)
                )
                ProgressCircle(
                    label = "Carbs",
                    value = carbsPercent,
                    unit = "%",
                    progress = carbsProgress.toFloat(),
                    color = PrimaryTeal,
                    modifier = Modifier.weight(1f)
                )
                ProgressCircle(
                    label = "Fat",
                    value = fatPercent,
                    unit = "%",
                    progress = fatProgress.toFloat(),
                    color = PrimaryPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressCircle(
    label: String,
    value: Int,
    unit: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 8.dp,
                trackColor = color.copy(alpha = 0.2f)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FormattedAnalysisSummary(analysis: String) {
    // Clean and format the analysis text
    val cleanedText = analysis
        .replace(Regex("^[A-Z\\s]+:", RegexOption.MULTILINE), "") // Remove ALL CAPS headers
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove markdown bold
        .replace(Regex("^\\s*\\*\\s+"), "• ") // Convert * to bullet points
        .replace(Regex("^\\s*\\d+\\.\\s+"), "") // Remove numbered list prefixes
        .replace(Regex("\\n{3,}"), "\n\n") // Remove excessive line breaks
        .replace(Regex("COMPREHENSIVE METABOLIC ANALYSIS REPORT", RegexOption.IGNORE_CASE), "")
        .replace(Regex("PERSONAL METABOLIC PROFILE", RegexOption.IGNORE_CASE), "")
        .replace(Regex("ANALYSIS SECTIONS", RegexOption.IGNORE_CASE), "")
        .replace(Regex("FORMATTING REQUIREMENTS", RegexOption.IGNORE_CASE), "")
        .replace(Regex("MEDICAL DISCLAIMER", RegexOption.IGNORE_CASE), "")
        .trim()
    
    // Split into paragraphs
    val paragraphs = cleanedText.split("\n\n").filter { 
        it.isNotBlank() && 
        it.length > 20 && 
        !it.contains("DISCLAIMER", ignoreCase = true) &&
        !it.contains("MEDICAL", ignoreCase = true)
    }
    
    if (paragraphs.isNotEmpty()) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Key Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                paragraphs.take(5).forEach { paragraph ->
                    val trimmed = paragraph.trim()
                    if (trimmed.isNotEmpty()) {
                        Text(
                            text = trimmed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun parseAndDisplayAnalysis(analysis: String) {
    // Split analysis into sections
    val sections = analysis.split("\n\n").filter { it.isNotBlank() }
    
    sections.forEachIndexed { index, section ->
        when {
            section.contains("PERSONAL METABOLIC PROFILE", ignoreCase = true) ||
            section.contains("UNDERSTANDING YOUR METABOLISM", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Your Metabolic Profile",
                    content = section,
                    icon = Icons.Default.Person,
                    gradientColors = listOf(PrimaryTeal, AccentMint)
                )
            }
            section.contains("CALORIE MANAGEMENT", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Calorie Strategy",
                    content = section,
                    icon = Icons.Default.LocalFireDepartment,
                    gradientColors = listOf(AccentCoral, Warning)
                )
            }
            section.contains("MACRONUTRIENT", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Macronutrient Breakdown",
                    content = section,
                    icon = Icons.Default.Restaurant,
                    gradientColors = listOf(PrimaryIndigo, PrimaryPurple)
                )
            }
            section.contains("MEAL TIMING", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Meal Timing Protocol",
                    content = section,
                    icon = Icons.Default.Schedule,
                    gradientColors = listOf(PrimaryPurple, PrimaryPink)
                )
            }
            section.contains("METABOLIC ENHANCEMENT", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Optimization Strategies",
                    content = section,
                    icon = Icons.Default.TrendingUp,
                    gradientColors = listOf(Success, PrimaryTeal)
                )
            }
            section.contains("MISTAKES TO AVOID", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Critical Mistakes to Avoid",
                    content = section,
                    icon = Icons.Default.Warning,
                    gradientColors = listOf(Warning, AccentCoral)
                )
            }
            section.contains("PROGRESS TIMELINE", ignoreCase = true) ||
            section.contains("EXPECTATIONS", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Progress Timeline",
                    content = section,
                    icon = Icons.Default.Timeline,
                    gradientColors = listOf(PrimaryIndigo, PrimaryTeal)
                )
            }
            section.contains("RECOVERY", ignoreCase = true) ||
            section.contains("HYDRATION", ignoreCase = true) -> {
                AnalysisSection(
                    title = "Recovery & Hydration",
                    content = section,
                    icon = Icons.Default.WaterDrop,
                    gradientColors = listOf(PrimaryTeal, AccentMint)
                )
            }
            section.contains("DISCLAIMER", ignoreCase = true) -> {
                DisclaimerSection(content = section)
            }
            section.length > 50 && !section.contains("COMPREHENSIVE", ignoreCase = true) -> {
                // Generic section for any other content
                AnalysisSection(
                    title = "Additional Insights",
                    content = section,
                    icon = Icons.Default.Lightbulb,
                    gradientColors = listOf(PrimaryPurple, PrimaryIndigo)
                )
            }
        }
    }
}

@Composable
private fun AnalysisSection(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column {
            // Section Header with Gradient
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        Brush.linearGradient(gradientColors)
                    )
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content with properly formatted text
            val formattedContent = content
                .replace(Regex("^[A-Z\\s]+:", RegexOption.MULTILINE), "") // Remove ALL CAPS headers
                .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove markdown bold
                .replace(Regex("^\\s*\\*\\s+"), "• ") // Convert * to bullet points
                .replace(Regex("^\\s*\\d+\\.\\s+"), "") // Remove numbered list prefixes
                .replace(Regex("\\n{3,}"), "\n\n") // Remove excessive line breaks
                .trim()
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Split by lines and format each paragraph
                val paragraphs = formattedContent.split("\n\n").filter { it.isNotBlank() }
                
                paragraphs.forEach { paragraph ->
                    val trimmedParagraph = paragraph.trim()
                    if (trimmedParagraph.isNotEmpty()) {
                        Text(
                            text = trimmedParagraph,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSection(content: String) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Medical Disclaimer",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content.replace("MEDICAL DISCLAIMER", "").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
