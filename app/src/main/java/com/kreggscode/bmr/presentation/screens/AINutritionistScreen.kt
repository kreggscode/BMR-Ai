package com.kreggscode.bmr.presentation.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kreggscode.bmr.presentation.viewmodels.AINutritionistViewModel
import com.kreggscode.bmr.presentation.viewmodels.ChatMessage
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AINutritionistScreen(
    navController: NavController,
    viewModel: AINutritionistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    var userInput by remember { mutableStateOf("") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentSpeakingMessageId by remember { mutableStateOf<String?>(null) }
    
    // Initialize TTS
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            delay(100) // Small delay to ensure content is laid out
            listState.animateScrollToItem(uiState.messages.size)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
            // Header
            ChatHeader(
                onBackClick = { navController.navigateUp() },
                onClearChat = { viewModel.clearChat() }
            )
            
            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Welcome message
                item {
                    AIWelcomeCard()
                }
                
                // Quick actions
                if (uiState.messages.isEmpty()) {
                    item {
                        QuickPrompts(
                            onPromptClick = { prompt ->
                                userInput = prompt
                                viewModel.sendMessage(prompt)
                                userInput = ""
                            }
                        )
                    }
                }
                
                // Chat messages
                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message,
                        isUser = message.isUser,
                        isSpeaking = currentSpeakingMessageId == message.timestamp,
                        onSpeakToggle = {
                            if (currentSpeakingMessageId == message.timestamp) {
                                // Stop speaking
                                tts?.stop()
                                currentSpeakingMessageId = null
                            } else {
                                // Start speaking
                                tts?.stop()
                                currentSpeakingMessageId = message.timestamp
                                tts?.speak(message.content, TextToSpeech.QUEUE_FLUSH, null, message.timestamp)
                                // Reset after speaking completes
                                coroutineScope.launch {
                                    delay(message.content.length * 50L) // Rough estimate
                                    if (currentSpeakingMessageId == message.timestamp) {
                                        currentSpeakingMessageId = null
                                    }
                                }
                            }
                        }
                    )
                }
                
                // Typing indicator
                if (uiState.isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Input Section - floating above keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                ChatInputSection(
                    value = userInput,
                    onValueChange = { userInput = it },
                    onSend = {
                        if (userInput.isNotBlank()) {
                            viewModel.sendMessage(userInput)
                            userInput = ""
                            focusManager.clearFocus()
                        }
                    },
                    isLoading = uiState.isTyping,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }
        }
}

@Composable
private fun ChatHeader(
    onBackClick: () -> Unit,
    onClearChat: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        cornerRadius = 20.dp,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "AI Nutritionist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Success)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                }
            }
            
            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Chat",
                    tint = Error
                )
            }
        }
    }
}

@Composable
private fun AIWelcomeCard() {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            PulsingIcon(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryPurple.copy(alpha = 0.3f),
                                        PrimaryPurple.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                modifier = Modifier
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Hello! I'm your AI Nutritionist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "I can help you with meal planning, nutrition advice, calorie tracking, and answer any diet-related questions. How can I assist you today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun QuickPrompts(onPromptClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick Questions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        val prompts = listOf(
            "Create a 1500 calorie meal plan" to Icons.Default.Restaurant,
            "How to increase protein intake?" to Icons.Default.FitnessCenter,
            "Best foods for weight loss" to Icons.Default.TrendingDown,
            "Healthy snack ideas" to Icons.Default.Cookie,
            "Vegetarian protein sources" to Icons.Default.Eco,
            "Pre-workout meal suggestions" to Icons.Default.DirectionsRun
        )
        
        prompts.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { (prompt, icon) ->
                    QuickPromptChip(
                        text = prompt,
                        icon = icon,
                        onClick = { onPromptClick(prompt) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPromptChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .height(56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isUser: Boolean,
    isSpeaking: Boolean = false,
    onSpeakToggle: () -> Unit = {}
) {
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryPurple.copy(alpha = 0.2f), PrimaryPink.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isUser) 16.dp else 4.dp,
                                topEnd = if (isUser) 4.dp else 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(
                            if (isUser) {
                                Brush.linearGradient(
                                    colors = listOf(PrimaryIndigo, PrimaryPurple)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = message.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // TTS Button for AI messages only
                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = onSpeakToggle,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSpeaking) PrimaryPurple.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop speaking" else "Speak",
                            tint = if (isSpeaking) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "AI is thinking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Dot(alpha = dot1Alpha)
            Dot(alpha = dot2Alpha)
            Dot(alpha = dot3Alpha)
        }
    }
}

@Composable
private fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(PrimaryPurple.copy(alpha = alpha))
    )
}

@Composable
private fun ChatInputSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Ask me anything about nutrition...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send Button
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank()) {
                            Brush.linearGradient(
                                colors = listOf(PrimaryIndigo, PrimaryPurple)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) Color.White 
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
