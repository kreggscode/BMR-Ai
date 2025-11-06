package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
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
import com.kreggscode.bmr.presentation.viewmodels.WaterTrackingViewModel
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WaterTrackingScreen(
    navController: NavController,
    viewModel: WaterTrackingViewModel = hiltViewModel()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
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
                            text = "Water Tracking",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Stay hydrated, stay healthy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { viewModel.resetToday() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = PrimaryTeal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Water Progress Circle
            WaterProgressCircle(
                totalWaterMl = uiState.todayWaterMl,
                waterIntake = uiState.todayGlasses,
                targetMl = 2000 // 2L target
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick Add Buttons
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QuickAddWaterButtons(
                onAddWater = { glassSize -> viewModel.addWater(glassSize) },
                onRemoveWater = { glassSize -> viewModel.removeWater(glassSize) },
                currentTotal = uiState.todayWaterMl
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Daily Log
            Text(
                text = "Today's Log",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DailyWaterLog(
                totalWaterMl = uiState.todayWaterMl,
                waterIntake = uiState.todayGlasses
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Weekly Chart
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Weekly Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "💧 Track your daily water intake throughout the week. Each bar shows how much water (ml) you drank that day. Aim for 2000ml (8 glasses) daily for optimal hydration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            WeeklyWaterChart(weeklyData = uiState.weeklyWaterData)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tips Card
            WaterTipsCard()
        }
    }
}

@Composable
private fun WaterProgressCircle(
    totalWaterMl: Int,
    waterIntake: Int,
    targetMl: Int
) {
    val progress = (totalWaterMl.toFloat() / targetMl).coerceIn(0f, 1f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentSky.copy(alpha = 0.1f),
                            PrimaryTeal.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            // Progress Ring - centered
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center),
                color = PrimaryTeal,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "${totalWaterMl}ml",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = PrimaryTeal
                )
                Text(
                    text = "of $targetMl ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$waterIntake",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentSky
                        )
                        Text(
                            text = "glasses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${((progress * 100).toInt())}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Success
                        )
                        Text(
                            text = "completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddWaterButtons(
    onAddWater: (Int) -> Unit,
    onRemoveWater: (Int) -> Unit,
    currentTotal: Int
) {
    data class GlassSize(val label: String, val size: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    
    val glassSizes = listOf(
        GlassSize("Small", 150, Icons.Default.WaterDrop),
        GlassSize("Medium", 250, Icons.Default.WaterDrop),
        GlassSize("Large", 350, Icons.Default.WaterDrop),
        GlassSize("Bottle", 500, Icons.Default.WaterDrop),
        GlassSize("Large Bottle", 750, Icons.Default.WaterDrop)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Add buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            glassSizes.forEach { glassSize ->
                val interactionSource = remember { MutableInteractionSource() }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AccentSky.copy(alpha = 0.2f), PrimaryTeal.copy(alpha = 0.1f))
                            )
                        )
                        .clickable(
                            indication = rememberRipple(bounded = true),
                            interactionSource = interactionSource
                        ) { 
                            android.util.Log.d("WaterTracking", "Button clicked for ${glassSize.size}ml")
                            onAddWater(glassSize.size) 
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = glassSize.icon,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = glassSize.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${glassSize.size}ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        // Remove buttons (only show if water has been consumed)
        if (currentTotal > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                glassSizes.take(3).forEach { glassSize ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Error.copy(alpha = 0.2f), AccentCoral.copy(alpha = 0.1f))
                                )
                            )
                            .clickable { 
                                if (currentTotal >= glassSize.size) {
                                    onRemoveWater(glassSize.size) 
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = glassSize.icon,
                            contentDescription = null,
                            tint = Error.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${glassSize.size}ml",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyWaterLog(
    totalWaterMl: Int,
    waterIntake: Int
) {
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
                Column {
                    Text(
                        text = "Total Consumed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${totalWaterMl}ml",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Glasses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$waterIntake",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentSky
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = (totalWaterMl.toFloat() / 2000f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = PrimaryTeal,
                trackColor = PrimaryTeal.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Target: 2000ml (${((totalWaterMl.toFloat() / 2000f) * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeeklyWaterChart(weeklyData: List<com.kreggscode.bmr.presentation.viewmodels.DailyWaterData>) {
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
                        val progress = (data.ml.toFloat() / 2000f).coerceIn(0f, 1f)
                        
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
                                    .fillMaxHeight(progress)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(PrimaryTeal, AccentSky)
                                        )
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show date (e.g., "Nov 5")
                        Text(
                            text = data.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        
                        // Show day label (Today, Yesterday, or day name)
                        Text(
                            text = data.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                        
                        // Show ml value
                        Text(
                            text = "${data.ml}ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTipsCard() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Hydration Tips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val tips = listOf(
                "Drink a glass of water first thing in the morning",
                "Keep a water bottle with you throughout the day",
                "Set reminders every 2 hours to drink water",
                "Drink water before, during, and after exercise",
                "Eat water-rich foods like cucumbers and watermelon",
                "Listen to your body - thirst is a sign you're already dehydrated"
            )
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

