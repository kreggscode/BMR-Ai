package com.kreggscode.bmr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.airbnb.lottie.compose.*
import com.kreggscode.bmr.presentation.screens.*
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var themePreferences: com.kreggscode.bmr.data.preferences.ThemePreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable true edge-to-edge with dark system bars matching app theme
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.parseColor("#0F172A") // Match BackgroundDark3
            )
        )
        setContent {
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
            
            BMRTheme(darkTheme = isDarkMode) {
                var showSplash by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    delay(1500) // Show splash for 1.5 seconds
                    showSplash = false
                }
                
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
                ) {
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 1.1f)
                    ) {
                        AnimatedSplashScreen()
                    }
                    
                    AnimatedVisibility(
                        visible = !showSplash,
                        enter = fadeIn(animationSpec = tween(500))
                    ) {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Logo animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Gradient animation
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryIndigo.copy(alpha = 0.3f),
                        PrimaryPurple.copy(alpha = 0.2f),
                        PrimaryPink.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(gradientOffset, gradientOffset)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(logoScale)
        ) {
            // App icon with pulsing effect
            PulsingIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = PrimaryTeal
                    )
                },
                modifier = Modifier
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name with gradient
            Text(
                text = "BMR Studio",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Your AI Nutrition Companion",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = PrimaryTeal,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = currentRoute(navController)
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {}
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                                listOf(BackgroundDark1, BackgroundDark2, BackgroundDark3)
                            } else {
                                listOf(BackgroundLight1, BackgroundLight2, BackgroundLight3)
                            }
                        )
                    )
            ) {
                NavigationHost(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        
        // Floating Bottom Nav as overlay
        if (shouldShowBottomBar(currentRoute)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                FloatingBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute ?: "",
                    onItemClick = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(400)
            ) + fadeIn(tween(400))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(400)
            ) + fadeOut(tween(400))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(400)
            ) + fadeIn(tween(400))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(400)
            ) + fadeOut(tween(400))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Calculator.route) {
            BMRCalculatorScreen(navController)
        }
        composable(Screen.Scanner.route) {
            FoodScannerScreen(navController)
        }
        composable(Screen.Diet.route) {
            DietPlansScreen(navController)
        }
        composable(Screen.Chat.route) {
            AINutritionistScreen(navController)
        }
        composable(Screen.Progress.route) {
            ProgressScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }
        composable(Screen.WaterTracking.route) {
            WaterTrackingScreen(navController)
        }
        composable(Screen.FoodLogs.route) {
            FoodLogsScreen(navController)
        }
        composable(Screen.SleepTracking.route) {
            SleepTrackingScreen(navController)
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Screen.Home.route,
        Screen.Calculator.route,
        Screen.Scanner.route,
        Screen.Diet.route,
        Screen.Progress.route
    )
}

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = Screen.Calculator.route,
        label = "Calculator",
        icon = Icons.Default.Calculate
    ),
    BottomNavItem(
        route = Screen.Scanner.route,
        label = "Scanner",
        icon = Icons.Default.CameraAlt
    ),
    BottomNavItem(
        route = Screen.Diet.route,
        label = "Diet",
        icon = Icons.Default.RestaurantMenu
    ),
    BottomNavItem(
        route = Screen.Chat.route,
        label = "AI Chat",
        icon = Icons.Default.SmartToy
    ),
    BottomNavItem(
        route = Screen.Progress.route,
        label = "Progress",
        icon = Icons.Default.TrendingUp
    )
)

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calculator : Screen("calculator")
    object Scanner : Screen("scanner")
    object Diet : Screen("diet")
    object Chat : Screen("chat")
    object Progress : Screen("progress")
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object WaterTracking : Screen("water_tracking")
    object FoodLogs : Screen("food_logs")
    object SleepTracking : Screen("sleep_tracking")
}
