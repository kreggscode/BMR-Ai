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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
                    subtitle = "Set meal reminder times",
                    onClick = { /* TODO: Open time picker */ }
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
                    onClick = { /* TODO: Export */ }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup & Sync",
                    subtitle = "Sync data to cloud",
                    onClick = { /* TODO: Backup */ }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Data",
                    subtitle = "Delete all local data",
                    titleColor = Error,
                    onClick = { /* TODO: Clear data */ }
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
                    onClick = { /* TODO: Navigate to profile management */ }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                SettingItem(
                    icon = Icons.Default.Add,
                    title = "Create New Profile",
                    subtitle = "Add a family member or friend",
                    onClick = { /* TODO: Create profile */ }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Reset App Button
            OutlinedButton(
                onClick = { /* TODO: Reset confirmation dialog */ },
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
                    text = "JD",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileBadge(text = "Pro", color = PrimaryIndigo)
                    ProfileBadge(text = "12 day streak", color = AccentCoral)
                }
            }
            
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Name") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
    AlertDialog(
        onDismissRequest = onDismiss,
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
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
