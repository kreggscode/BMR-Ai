package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kreggscode.bmr.Screen
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark1,
                        BackgroundDark2,
                        BackgroundDark3
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(
                    onClick = { navController.navigate(Screen.Home.route) }
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(page = onboardingPages[page])
            }
            
            // Page indicator
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    PageIndicator(
                        selected = pagerState.currentPage == index
                    )
                }
            }
            
            // Navigation buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                if (pagerState.currentPage == onboardingPages.size - 1) {
                    AnimatedGradientButton(
                        text = "Get Started",
                        onClick = { 
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Previous",
                                tint = if (pagerState.currentPage > 0) Color.White 
                                       else Color.Transparent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Back",
                                color = if (pagerState.currentPage > 0) Color.White 
                                       else Color.Transparent
                            )
                        }
                        
                        AnimatedGradientButton(
                            text = "Next",
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPageData) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            page.color.copy(alpha = 0.3f),
                            page.color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.color,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Features
        if (page.features.isNotEmpty()) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderWidth = 1.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    page.features.forEach { feature ->
                        FeatureItem(feature = feature)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(PrimaryTeal)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PageIndicator(selected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (selected) 24.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val color by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(300)
    )
    
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(color)
    )
}

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val features: List<String> = emptyList()
)

private val onboardingPages = listOf(
    OnboardingPageData(
        title = "Welcome to BMR Studio",
        description = "Your personal AI-powered nutrition companion for achieving your health goals",
        icon = Icons.Default.FitnessCenter,
        color = PrimaryTeal,
        features = listOf(
            "Calculate your BMR & TDEE",
            "Track daily calories",
            "Monitor your progress"
        )
    ),
    OnboardingPageData(
        title = "AI Food Recognition",
        description = "Simply take a photo of your meal and let our AI analyze the nutritional content instantly",
        icon = Icons.Default.CameraAlt,
        color = PrimaryIndigo,
        features = listOf(
            "Instant food recognition",
            "Accurate calorie estimation",
            "Detailed macro breakdown"
        )
    ),
    OnboardingPageData(
        title = "Personalized Diet Plans",
        description = "Get AI-generated meal plans tailored to your goals, preferences, and dietary restrictions",
        icon = Icons.Default.Restaurant,
        color = PrimaryPurple,
        features = listOf(
            "Custom meal plans",
            "Shopping lists",
            "Recipe suggestions"
        )
    ),
    OnboardingPageData(
        title = "AI Nutritionist Chat",
        description = "Chat with your personal AI nutritionist anytime for instant advice and guidance",
        icon = Icons.Default.SmartToy,
        color = AccentCoral,
        features = listOf(
            "24/7 nutrition support",
            "Personalized advice",
            "Evidence-based recommendations"
        )
    )
)
