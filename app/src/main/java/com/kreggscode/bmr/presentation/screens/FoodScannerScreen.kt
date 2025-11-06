package com.kreggscode.bmr.presentation.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.*
import com.kreggscode.bmr.presentation.viewmodels.FoodScannerViewModel
import com.kreggscode.bmr.ui.components.*
import com.kreggscode.bmr.ui.theme.*
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FoodScannerScreen(
    navController: NavController,
    viewModel: FoodScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        when {
            cameraPermissionState.status.isGranted -> {
                when (uiState.scanMode) {
                    ScanMode.CAMERA -> CameraView(viewModel)
                    ScanMode.GALLERY -> GalleryPicker(viewModel)
                    ScanMode.MANUAL -> ManualFoodEntry(viewModel)
                    ScanMode.BARCODE -> BarcodeScanner(viewModel)
                    ScanMode.RESULTS -> ScanResults(viewModel, navController)
                }
            }
            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
            else -> {
                LaunchedEffect(Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
                PermissionDenied()
            }
        }
        
        // Mode Selector (always visible except in results)
        if (uiState.scanMode != ScanMode.RESULTS) {
            ModeSelectorBar(
                currentMode = uiState.scanMode,
                onModeChange = viewModel::changeScanMode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
        
        // Loading Overlay
        if (uiState.isProcessing) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun CameraView(viewModel: FoodScannerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Camera Overlay
        CameraOverlay()
        
        // Capture Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            CaptureButton(
                onClick = {
                    val photoFile = File(context.cacheDir, "food_photo_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.processImage(Uri.fromFile(photoFile))
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                // Handle error
                            }
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun CameraOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Viewfinder frame
        Box(
            modifier = Modifier
                .size(300.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Corner indicators
            val cornerSize = 40.dp
            val cornerWidth = 3.dp
            
            // Top-left corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(cornerSize)
                    .border(
                        BorderStroke(cornerWidth, PrimaryTeal),
                        RoundedCornerShape(topStart = 24.dp)
                    )
            )
        }
        
        // Instruction text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Point at food",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
                ),
                shape = CircleShape
            )
            .clickable(
                indication = rememberRipple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Capture",
            tint = PrimaryIndigo,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun GalleryPicker(viewModel: FoodScannerViewModel) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImage(it) }
    }
    
    LaunchedEffect(Unit) {
        launcher.launch("image/*")
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Return to camera if gallery was cancelled
            if (viewModel.uiState.value.scanMode == ScanMode.GALLERY) {
                viewModel.changeScanMode(ScanMode.CAMERA)
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryIndigo,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Opening gallery...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ManualFoodEntry(viewModel: FoodScannerViewModel) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
            .padding(top = 80.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Text(
                text = "Add Food Manually",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Food name input
            OutlinedTextField(
                value = viewModel.manualFoodName,
                onValueChange = { viewModel.updateManualFoodName(it) },
                label = { Text("Food Name") },
                placeholder = { Text("e.g., Grilled Chicken Breast") },
                leadingIcon = {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = PrimaryIndigo)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Calories input
            OutlinedTextField(
                value = viewModel.manualCalories,
                onValueChange = { viewModel.updateManualCalories(it) },
                label = { Text("Calories") },
                placeholder = { Text("165") },
                leadingIcon = {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AccentCoral)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Serving size
            OutlinedTextField(
                value = viewModel.manualServing,
                onValueChange = { viewModel.updateManualServing(it) },
                label = { Text("Serving Size") },
                placeholder = { Text("100g") },
                leadingIcon = {
                    Icon(Icons.Default.Scale, contentDescription = null, tint = PrimaryTeal)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Macros section
            Text(
                text = "Macronutrients (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = viewModel.manualProtein,
                    onValueChange = { viewModel.updateManualProtein(it) },
                    label = { Text("Protein (g)") },
                    placeholder = { Text("31") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = viewModel.manualCarbs,
                    onValueChange = { viewModel.updateManualCarbs(it) },
                    label = { Text("Carbs (g)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = viewModel.manualFat,
                    onValueChange = { viewModel.updateManualFat(it) },
                    label = { Text("Fat (g)") },
                    placeholder = { Text("3.6") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedGradientButton(
                text = "Add Food",
                onClick = { viewModel.addManualFood() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BarcodeScanner(viewModel: FoodScannerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (e: Exception) {
                // Handle error
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Barcode Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Scanning frame
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryTeal, PrimaryIndigo, PrimaryPurple)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            
            // Instruction text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan product barcode",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
        
        // Capture Button for manual barcode capture
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        ) {
            CaptureButton(
                onClick = {
                    val photoFile = File(context.cacheDir, "barcode_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.processBarcode(Uri.fromFile(photoFile))
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                // Handle error
                            }
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ScanResults(
    viewModel: FoodScannerViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Food Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Powered by AI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryPurple
                )
            }
            
            IconButton(onClick = { viewModel.changeScanMode(ScanMode.CAMERA) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        // Scanned Image
        uiState.scannedImageUri?.let { uri ->
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cornerRadius = 20.dp
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Scanned food",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Recognized Foods
        Text(
            text = "Recognized Items",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        uiState.recognizedFoods.forEachIndexed { index, food ->
            var showEditDialog by remember { mutableStateOf(false) }
            var editedName by remember { mutableStateOf(food.name) }
            
            FoodResultCard(
                food = food,
                onConfirm = { viewModel.confirmFood(food) },
                onEdit = { showEditDialog = true }
            )
            
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = {
                        Text(
                            text = "Edit Food Name",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Food Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (editedName.isNotBlank()) {
                                    viewModel.updateFoodName(index, editedName)
                                    showEditDialog = false
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        
        // Action Buttons
        var showSaveConfirmation by remember { mutableStateOf(false) }
        
        if (showSaveConfirmation) {
            AlertDialog(
                onDismissRequest = { showSaveConfirmation = false },
                title = {
                    Text(
                        text = "Save Food Log?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Add ${uiState.recognizedFoods.size} item(s) to your food log?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveAllFoods()
                            showSaveConfirmation = false
                            navController.navigateUp()
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { viewModel.changeScanMode(ScanMode.CAMERA) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rescan")
            }
            
            AnimatedGradientButton(
                text = "Save All",
                onClick = {
                    viewModel.saveAllFoods()
                    navController.navigateUp()
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun FoodResultCard(
    food: RecognizedFoodItem,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Confidence badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    food.confidence > 0.8f -> Success.copy(alpha = 0.1f)
                                    food.confidence > 0.6f -> Warning.copy(alpha = 0.1f)
                                    else -> Error.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(food.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                food.confidence > 0.8f -> Success
                                food.confidence > 0.6f -> Warning
                                else -> Error
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Nutrition info
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NutritionChip(
                        label = "Calories",
                        value = "${food.calories.toInt()}",
                        color = AccentCoral
                    )
                    NutritionChip(
                        label = "Portion",
                        value = food.portion,
                        color = PrimaryTeal
                    )
                }
                
                food.protein?.let { protein ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "P: ${protein.toInt()}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        food.carbs?.let {
                            Text(
                                text = "C: ${it.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        food.fat?.let {
                            Text(
                                text = "F: ${it.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Success.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionChip(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ModeSelectorBar(
    currentMode: ScanMode,
    onModeChange: (ScanMode) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        cornerRadius = 20.dp,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModeButton(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                selected = currentMode == ScanMode.CAMERA,
                onClick = { onModeChange(ScanMode.CAMERA) }
            )
            ModeButton(
                icon = Icons.Default.PhotoLibrary,
                label = "Gallery",
                selected = currentMode == ScanMode.GALLERY,
                onClick = { onModeChange(ScanMode.GALLERY) }
            )
            ModeButton(
                icon = Icons.Default.Edit,
                label = "Manual",
                selected = currentMode == ScanMode.MANUAL,
                onClick = { onModeChange(ScanMode.MANUAL) }
            )
            ModeButton(
                icon = Icons.Default.QrCodeScanner,
                label = "Barcode",
                selected = currentMode == ScanMode.BARCODE,
                onClick = { onModeChange(ScanMode.BARCODE) }
            )
        }
    }
}

@Composable
private fun ModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(300)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Pulsing animation for scanning effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Rotating animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Scanning line animation
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated scanning icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    PrimaryTeal,
                                    AccentSky,
                                    PrimaryTeal.copy(alpha = 0.3f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Inner pulsing circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PrimaryTeal.copy(alpha = 0.8f),
                                    AccentSky.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Center icon
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Scanning text with animation
            Text(
                text = "🔍 Scanning Food...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "AI is analyzing your food image",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Powered by Pollinations AI",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryPurple.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = dotAlpha))
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We need camera access to scan food items and analyze their nutritional content.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedGradientButton(
                text = "Grant Permission",
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionDenied() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Camera permission is required for food scanning",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

enum class ScanMode {
    CAMERA, GALLERY, MANUAL, BARCODE, RESULTS
}

data class RecognizedFoodItem(
    val name: String,
    val confidence: Float,
    val calories: Double,
    val portion: String,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null
)
