package com.kreggscode.bmr.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.cloudy.Cloudy
import kotlinx.coroutines.launch

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.5.dp,
    blurRadius: Dp = 25.dp,
    removePadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Cloudy(
            radius = blurRadius.value.toInt(),
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isDarkTheme) colors.surface.copy(alpha = 0.95f)
                        else colors.surface.copy(alpha = 0.92f)
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = 0.05f),
                            colors.surface.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    width = borderWidth,
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                colors.outline.copy(alpha = 0.3f),
                                colors.outline.copy(alpha = 0.1f)
                            )
                        } else {
                            listOf(
                                colors.outline.copy(alpha = 0.4f),
                                colors.outline.copy(alpha = 0.2f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .then(
                    if (removePadding) Modifier
                    else Modifier.padding(20.dp)
                ),
            content = content
        )
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors
                )
            )
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
fun AnimatedGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val infiniteTransition = rememberInfiniteTransition()
    val gradientAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF14B8A6).copy(alpha = 0.8f + gradientAnimation * 0.2f),
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFEC4899).copy(alpha = 0.8f + gradientAnimation * 0.2f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f * gradientAnimation, 1000f * gradientAnimation)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun FloatingBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .fillMaxWidth()
            .height(65.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Glassmorphism effect with less transparency (but still glassmorphic)
        Cloudy(
            radius = 20,
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isDarkTheme) 
                            colors.surface.copy(alpha = 0.92f) // Less transparent but still glassmorphic
                        else 
                            colors.surface.copy(alpha = 0.90f)
                    )
            )
        }
        
        // More visible border to distinguish it
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                colors.outline.copy(alpha = 0.5f),
                                colors.outline.copy(alpha = 0.3f)
                            )
                        } else {
                            listOf(
                                colors.outline.copy(alpha = 0.4f),
                                colors.outline.copy(alpha = 0.25f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )
        
        // Content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                FloatingNavBarItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onItemClick(item.route) }
                )
            }
        }
    }
}

@Composable
private fun FloatingNavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF14B8A6)
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(200)
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF14B8A6)
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200)
    )
    
    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
fun PulsingIcon(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Gray.copy(alpha = 0.2f),
                        Color.Gray.copy(alpha = 0.35f),
                        Color.Gray.copy(alpha = 0.2f)
                    ),
                    start = Offset(shimmerTranslateAnim - 500f, 0f),
                    end = Offset(shimmerTranslateAnim, 0f)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        content = content
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color(0xFF14B8A6),
        Color(0xFF6366F1)
    ),
    maxLines: Int = 2
) {
    GlassmorphicCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = maxLines,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
