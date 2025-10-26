package com.kreggscode.bmr.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kreggscode.bmr.ui.theme.*

@Composable
fun SliderInputCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    gradientColors: List<Color> = listOf(
        Color(0xFFEC4899),
        Color(0xFFEF4444)
    ),
    icon: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(colors = gradientColors)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Value Display with +/- buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button
                IconButton(
                    onClick = {
                        val newValue = (value - step).coerceIn(range)
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = gradientColors.map { it.copy(alpha = 0.15f) }
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = gradientColors[0],
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Value Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (value % 1 == 0f) value.toInt().toString() 
                               else String.format("%.1f", value),
                        style = MaterialTheme.typography.displayLarge,
                        color = gradientColors[0],
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Plus Button
                IconButton(
                    onClick = {
                        val newValue = (value + step).coerceIn(range)
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = gradientColors.map { it.copy(alpha = 0.15f) }
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = gradientColors[0],
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Horizontal Slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = ((range.endInclusive - range.start) / step).toInt() - 1,
                colors = SliderDefaults.colors(
                    thumbColor = gradientColors[0],
                    activeTrackColor = gradientColors[0],
                    inactiveTrackColor = gradientColors[0].copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
fun CompactSliderInput(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    icon: @Composable () -> Unit,
    gradientColors: List<Color> = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6)
    ),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.1f) }
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon + Label
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${value.toInt()} $unit",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // +/- Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    val newValue = (value - step).coerceIn(range)
                    onValueChange(newValue)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(gradientColors[0].copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = gradientColors[0],
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = {
                    val newValue = (value + step).coerceIn(range)
                    onValueChange(newValue)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(gradientColors[0].copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = gradientColors[0],
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
