package com.kreggscode.bmr.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import android.net.Uri
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.kreggscode.bmr.presentation.viewmodels.SettingsViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var editNameText by remember { mutableStateOf("") }
    
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
                .padding(bottom = 20.dp)
        ) {
            // Header
            SettingsHeader(onBackClick = { navController.navigateUp() })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Section
            ProfileSection(
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                onEditClick = { 
                    editNameText = uiState.userName
                    viewModel.toggleEditNameDialog(true) 
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Settings
            SettingsSection(title = "Appearance") {
                SettingItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    trailing = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryTeal,
                                checkedTrackColor = PrimaryTeal.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f),
                                uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notifications Settings
            SettingsSection(title = "Notifications") {
                SettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    subtitle = "Get meal reminders",
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryIndigo,
                                checkedTrackColor = PrimaryIndigo.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Schedule,
                    title = "Reminder Time",
                    subtitle = if (uiState.reminderEnabled) 
                        "${String.format("%02d", uiState.reminderHour)}:${String.format("%02d", uiState.reminderMinute)}" 
                        else "Not set",
                    onClick = { viewModel.toggleReminderDialog(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preferences Settings
            SettingsSection(title = "Preferences") {
                SettingItem(
                    icon = Icons.Default.Straighten,
                    title = "Units",
                    subtitle = if (uiState.isMetric) "Metric (kg, cm)" else "Imperial (lb, in)",
                    onClick = { viewModel.toggleUnitsDialog(true) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = uiState.language,
                    onClick = { viewModel.toggleLanguageDialog(true) }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Restaurant,
                    title = "Dietary Preferences",
                    subtitle = uiState.dietaryPreference,
                    onClick = { viewModel.toggleDietaryDialog(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data & Privacy
            SettingsSection(title = "Data & Privacy") {
                SettingItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Export Data",
                    subtitle = "Download your data",
                    onClick = { 
                        val exportData = viewModel.exportData()
                        // Show toast or share dialog
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup & Sync",
                    subtitle = "Sync data to cloud",
                    onClick = { 
                        // Cloud sync functionality
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Data",
                    subtitle = "Delete all local data",
                    titleColor = Error,
                    onClick = { viewModel.toggleClearDataDialog(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Support
            SettingsSection(title = "Support") {
                SettingItem(
                    icon = Icons.Default.Star,
                    title = "Rate App",
                    subtitle = "Rate us on Play Store",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=com.kreggscode.bmr")
                                setPackage("com.android.vending")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to browser if Play Store not available
                            val intent = Intent(Intent.ACTION_VIEW, 
                                Uri.parse("https://play.google.com/store/apps/details?id=com.kreggscode.bmr"))
                            context.startActivity(intent)
                        }
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    subtitle = "Share with friends and family",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out BMR Studio!")
                            putExtra(Intent.EXTRA_TEXT, 
                                "Hey! Check out BMR Studio - Your AI-powered nutrition companion! 🔥\n\n" +
                                "Track calories, scan food with AI, get personalized diet plans, and chat with an AI nutritionist.\n\n" +
                                "Download now: https://play.google.com/store/apps/details?id=com.kreggscode.bmr")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share BMR Studio"))
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Help,
                    title = "Help & FAQ",
                    subtitle = "Get answers to common questions",
                    onClick = { /* TODO: Help */ }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0",
                    onClick = { /* TODO: About */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Management
            SettingsSection(title = "Profile Management") {
                SettingItem(
                    icon = Icons.Default.Person,
                    title = "Manage Profiles",
                    subtitle = "Switch or create new profiles",
                    onClick = { 
                        viewModel.toggleManageProfilesDialog(true)
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Add,
                    title = "Create New Profile",
                    subtitle = "Add a family member or friend",
                    onClick = { viewModel.toggleCreateProfileDialog(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Reset App Button
            OutlinedButton(
                onClick = { viewModel.toggleClearDataDialog(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Error
                ),
                border = BorderStroke(1.dp, Error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Data")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Edit Name Dialog
        if (uiState.showEditNameDialog) {
            EditNameDialog(
                currentName = editNameText,
                onNameChange = { editNameText = it },
                onDismiss = { viewModel.toggleEditNameDialog(false) },
                onConfirm = {
                    viewModel.updateUserName(editNameText)
                    viewModel.toggleEditNameDialog(false)
                }
            )
        }
        
        // Units Dialog
        if (uiState.showUnitsDialog) {
            UnitsDialog(
                currentIsMetric = uiState.isMetric,
                onDismiss = { viewModel.toggleUnitsDialog(false) },
                onSelect = { isMetric ->
                    viewModel.toggleUnits(isMetric)
                    viewModel.toggleUnitsDialog(false)
                }
            )
        }
        
        // Language Dialog
        if (uiState.showLanguageDialog) {
            LanguageDialog(
                currentLanguage = uiState.language,
                onDismiss = { viewModel.toggleLanguageDialog(false) },
                onSelect = { language ->
                    viewModel.updateLanguage(language)
                    viewModel.toggleLanguageDialog(false)
                }
            )
        }
        
        // Dietary Preferences Dialog
        if (uiState.showDietaryDialog) {
            DietaryDialog(
                currentPreference = uiState.dietaryPreference,
                onDismiss = { viewModel.toggleDietaryDialog(false) },
                onSelect = { preference ->
                    viewModel.updateDietaryPreference(preference)
                    viewModel.toggleDietaryDialog(false)
                }
            )
        }
        
        // Reminder Time Dialog
        if (uiState.showReminderDialog) {
            ReminderTimeDialog(
                currentHour = uiState.reminderHour,
                currentMinute = uiState.reminderMinute,
                onDismiss = { viewModel.toggleReminderDialog(false) },
                onConfirm = { hour, minute ->
                    viewModel.setReminderTime(hour, minute)
                    viewModel.toggleReminderDialog(false)
                }
            )
        }
        
        // Clear Data Confirmation Dialog
        if (uiState.showClearDataDialog) {
            ClearDataDialog(
                onDismiss = { viewModel.toggleClearDataDialog(false) },
                onConfirm = {
                    viewModel.clearAllData()
                    viewModel.toggleClearDataDialog(false)
                }
            )
        }
        
        // Create Profile Dialog
        if (uiState.showCreateProfileDialog) {
            CreateProfileDialog(
                onDismiss = { viewModel.toggleCreateProfileDialog(false) },
                onCreate = { name, age, sex, height, weight ->
                    viewModel.createNewProfile(name, age, sex, height, weight)
                    viewModel.toggleCreateProfileDialog(false)
                }
            )
        }
        
        // Manage Profiles Dialog
        if (uiState.showManageProfilesDialog) {
            ManageProfilesDialog(
                profiles = uiState.allProfiles,
                currentUserId = uiState.currentUserId,
                onDismiss = { viewModel.toggleManageProfilesDialog(false) },
                onSwitchProfile = { userId ->
                    viewModel.switchProfile(userId)
                    viewModel.toggleManageProfilesDialog(false)
                },
                onDeleteProfile = { userId ->
                    viewModel.deleteProfile(userId)
                }
            )
        }
    }
}

@Composable
private fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ProfileSection(
    userName: String,
    userEmail: String,
    onEditClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryIndigo, PrimaryPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Edit Button
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryIndigo,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        content()
    }
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Edit Name") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface,
                    focusedLabelColor = colors.primary,
                    unfocusedLabelColor = colors.onSurfaceVariant,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline,
                    cursorColor = colors.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnitsDialog(
    currentIsMetric: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Select Units") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(true) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentIsMetric,
                        onClick = { onSelect(true) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Metric (kg, cm)")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(false) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !currentIsMetric,
                        onClick = { onSelect(false) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imperial (lb, in)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf("English", "Spanish", "French", "German", "Chinese", "Japanese")
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Select Language") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DietaryDialog(
    currentPreference: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val preferences = listOf("None", "Vegetarian", "Vegan", "Keto", "Paleo", "Gluten-Free", "Dairy-Free")
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Dietary Preferences") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                preferences.forEach { preference ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(preference) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentPreference == preference,
                            onClick = { onSelect(preference) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(preference)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ReminderTimeDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(currentHour) }
    var selectedMinute by remember { mutableStateOf(currentMinute) }
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Set Reminder Time") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Choose meal reminder time",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase hour")
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease hour")
                        }
                    }
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedMinute = (selectedMinute + 15) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase minute")
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedMinute = if (selectedMinute < 15) 45 else selectedMinute - 15 }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease minute")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { 
            Text(
                "Clear All Data?",
                color = Error,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Text(
                "This will permanently delete:\n\n" +
                "• All user profiles\n" +
                "• BMR calculation history\n" +
                "• Food logs and meal entries\n" +
                "• Custom food items\n\n" +
                "This action cannot be undone!",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error
                )
            ) {
                Text("Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = colors.background.luminance() < 0.5f
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
        titleContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        textContentColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
        title = { Text("Create New Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                
                // Sex selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sex == "male",
                        onClick = { sex = "male" },
                        label = { Text("Male") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = sex == "female",
                        onClick = { sex = "female" },
                        label = { Text("Female") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ageInt = age.toIntOrNull() ?: 25
                    val heightDouble = height.toDoubleOrNull() ?: 170.0
                    val weightDouble = weight.toDoubleOrNull() ?: 70.0
                    if (name.isNotBlank()) {
                        onCreate(name, ageInt, sex, heightDouble, weightDouble)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
