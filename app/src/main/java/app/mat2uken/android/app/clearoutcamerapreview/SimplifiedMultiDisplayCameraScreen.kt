package app.mat2uken.android.app.clearoutcamerapreview

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.util.Size
import androidx.camera.core.AspectRatio
import kotlin.math.abs
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.min
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraUtils
import app.mat2uken.android.app.clearoutcamerapreview.utils.DisplayUtils
import app.mat2uken.android.app.clearoutcamerapreview.model.Size as CustomSize
import app.mat2uken.android.app.clearoutcamerapreview.camera.CameraState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import android.media.AudioDeviceInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import android.view.Surface
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import app.mat2uken.android.app.clearoutcamerapreview.audio.AudioCoordinator
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraRotationHelper
import app.mat2uken.android.app.clearoutcamerapreview.data.SettingsRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private const val TAG = "SimplifiedMultiDisplay"

/**
 * Finds the best preview resolution based on the following criteria:
 * 1. Prefers 1920x1080 if available
 * 2. Otherwise, selects the resolution closest to 16:9 aspect ratio
 */
private fun selectOptimalResolution(availableSizes: List<Size>): Size? {
    // Convert Android Size to CustomSize
    val customSizes = availableSizes.map { size -> CustomSize(size.width, size.height) }
    val bestSize = CameraUtils.selectOptimalResolution(customSizes)
    if (bestSize != null) {
        Log.d(TAG, "Selected resolution: ${bestSize.width}x${bestSize.height} (aspect ratio: ${bestSize.width.toDouble() / bestSize.height})")
        // Convert CustomSize back to Android Size
        return Size(bestSize.width, bestSize.height)
    }
    return null
}


/**
 * Gets supported camera preview sizes
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
private fun getSupportedPreviewSizes(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    cameraSelector: CameraSelector
): List<Size> {
    return try {
        val cameraInfo = cameraProvider.availableCameraInfos
            .find { it.cameraSelector == cameraSelector }
        
        cameraInfo?.let { info ->
            val camera2Info = androidx.camera.camera2.interop.Camera2CameraInfo.from(info)
            val cameraId = camera2Info.cameraId
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(
                android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            
            streamConfigMap?.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                ?.map { androidSize -> Size(androidSize.width, androidSize.height) }
                ?.also { sizes ->
                    Log.d(TAG, "Available preview sizes: ${sizes.joinToString { size -> "${size.width}x${size.height}" }}")
                }
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Error getting camera sizes", e)
        emptyList()
    }
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun SimplifiedMultiDisplayCameraScreen(audioCoordinator: AudioCoordinator? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    
    // Get the current rotation
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val rotation = windowManager.defaultDisplay.rotation
    
    // Settings repository
    val settingsRepository = remember { SettingsRepository(context) }
    
    // Camera state
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraState by remember { mutableStateOf(CameraState()) }
    
    // Initialize settings
    LaunchedEffect(Unit) {
        settingsRepository.initialize()
        // Load last selected camera
        val lastCamera = settingsRepository.getLastSelectedCamera()
        cameraState = cameraState.copy(cameraSelector = lastCamera)
    }
    var selectedResolution by remember { mutableStateOf<Size?>(null) }
    var actualPreviewSize by remember { mutableStateOf<Size?>(null) }
    
    // External display flip states
    var isVerticallyFlipped by remember { mutableStateOf(false) }
    var isHorizontallyFlipped by remember { mutableStateOf(false) }
    var currentDisplayId by remember { mutableStateOf<String?>(null) }
    
    // Display management
    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    var externalDisplay by remember { mutableStateOf<Display?>(null) }
    var externalPresentation by remember { mutableStateOf<SimpleCameraPresentation?>(null) }
    
    // Dialog states
    var showCameraDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showFlipDialog by remember { mutableStateOf(false) }
    var showAudioOutputDialog by remember { mutableStateOf(false) }
    
    // Sidebar visibility state
    var showSidebar by remember { mutableStateOf(true) }
    
    // Audio state
    val audioState by audioCoordinator?.audioState?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // Load saved audio output device preference when audio coordinator is ready
    LaunchedEffect(audioCoordinator, audioState?.hasExternalOutput) {
        if (audioCoordinator != null && audioState?.hasExternalOutput == true) {
            val savedDeviceId = settingsRepository.getAudioOutputDeviceId()
            savedDeviceId?.let { deviceId ->
                val availableDevices = audioCoordinator.getAvailableOutputDevices().value
                availableDevices.find { it.id == deviceId }?.let { device ->
                    audioCoordinator.setOutputDevice(device)
                }
            }
        }
    }
    
    // Create a single preview view for the main display with proper aspect ratio
    val mainPreviewView = remember { 
        PreviewView(context).apply {
            // Use PERFORMANCE mode for better performance
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            // Set scale type to FIT_CENTER to show full image with letterboxing
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    
    // Function to check for external displays
    fun checkExternalDisplays() {
        val displays = displayManager.displays
        val external = DisplayUtils.findExternalDisplay(displays)
        externalDisplay = external
        cameraState = cameraState.updateExternalDisplay(
            connected = external != null,
            displayId = external?.displayId
        )
        
        // Load display settings if external display is found
        external?.let { display ->
            val displayId = display.displayId.toString()
            currentDisplayId = displayId
            coroutineScope.launch {
                val displaySettings = settingsRepository.getDisplaySettings(displayId)
                isVerticallyFlipped = displaySettings.isVerticallyFlipped
                isHorizontallyFlipped = displaySettings.isHorizontallyFlipped
            }
        }
        
        Log.d(TAG, "Checked displays. External display ${if (external != null) "found" else "not found"}")
    }
    
    // Original implementation preserved for reference
    fun checkExternalDisplaysOld() {
        val displays = displayManager.displays
        externalDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        Log.d(TAG, "External display check: ${externalDisplay?.displayId}")
    }
    
    // Initialize display state
    LaunchedEffect(Unit) {
        checkExternalDisplays()
    }
    
    // Monitor display changes
    DisposableEffect(displayManager) {
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                Log.d(TAG, "Display added: $displayId")
                checkExternalDisplays()
            }
            
            override fun onDisplayRemoved(displayId: Int) {
                Log.d(TAG, "Display removed: $displayId")
                if (externalPresentation?.display?.displayId == displayId) {
                    try {
                        externalPresentation?.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing presentation", e)
                    }
                    externalPresentation = null
                }
                checkExternalDisplays()
            }
            
            override fun onDisplayChanged(displayId: Int) {
                Log.d(TAG, "Display changed: $displayId")
            }
        }
        
        displayManager.registerDisplayListener(displayListener, null)
        
        onDispose {
            displayManager.unregisterDisplayListener(displayListener)
            try {
                externalPresentation?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up presentation", e)
            }
        }
    }
    
    // Camera setup - recreate when camera selector or rotation changes
    DisposableEffect(cameraState.cameraSelector, rotation) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Unbind all use cases
                cameraProvider.unbindAll()
                
                // Get supported output sizes for the preview
                val supportedSizes = getSupportedPreviewSizes(context, cameraProvider, cameraState.cameraSelector)
                
                // Select optimal resolution
                val targetResolution = selectOptimalResolution(supportedSizes)
                selectedResolution = targetResolution
                
                // Create preview with selected resolution
                // Use getTargetRotation only for front camera
                val isFront = CameraRotationHelper.isFrontCamera(cameraState.cameraSelector)
                val targetRotation = CameraRotationHelper.getTargetRotation(rotation, isFront)
                
                val preview = if (targetResolution != null) {
                    Preview.Builder()
                        .setTargetAspectRatio(
                            if (targetResolution.width * 9 == targetResolution.height * 16) {
                                AspectRatio.RATIO_16_9
                            } else {
                                AspectRatio.RATIO_4_3
                            }
                        )
                        .setTargetRotation(targetRotation)
                        .build()
                        .also {
                            Log.d(TAG, "Preview created with target aspect ratio for resolution: ${targetResolution.width}x${targetResolution.height}, rotation: $targetRotation, isFront: $isFront")
                        }
                } else {
                    Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(targetRotation)
                        .build()
                        .also {
                            Log.d(TAG, "Preview created with default 16:9 aspect ratio, rotation: $targetRotation, isFront: $isFront")
                        }
                }
                
                // Set surface provider for main display
                preview.setSurfaceProvider { surfaceRequest ->
                    // Get the actual preview size from the surface request
                    val resolution = surfaceRequest.resolution
                    actualPreviewSize = Size(resolution.width, resolution.height)
                    Log.d(TAG, "Actual preview size from surface request: ${resolution.width}x${resolution.height}")
                    
                    // Set scale type based on camera type
                    mainPreviewView.scaleType = PreviewView.ScaleType.FIT_CENTER
                    
                    // PreviewView handles rotation automatically when using PERFORMANCE mode
                    // No manual rotation is needed
                    mainPreviewView.rotation = 0f
                    
                    // Forward to the actual surface provider
                    mainPreviewView.surfaceProvider.onSurfaceRequested(surfaceRequest)
                }
                
                // Bind to lifecycle
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraState.cameraSelector,
                    preview
                )
                
                camera = cam
                
                // Update zoom state
                cam.cameraInfo.zoomState.value?.let { zoomState ->
                    cameraState = cameraState.updateZoomBounds(
                        zoomState.minZoomRatio,
                        zoomState.maxZoomRatio
                    ).updateZoomRatio(zoomState.zoomRatio)
                    
                    // Load saved zoom settings
                    coroutineScope.launch {
                        val savedSettings = settingsRepository.getCameraSettings(cameraState.cameraSelector)
                        val savedZoom = if (savedSettings.cameraId.isNotEmpty()) {
                            // Use saved zoom if within current bounds
                            savedSettings.zoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                        } else {
                            zoomState.zoomRatio
                        }
                        
                        if (savedZoom != zoomState.zoomRatio) {
                            try {
                                cam.cameraControl.setZoomRatio(savedZoom)
                                cameraState = cameraState.updateZoomRatio(savedZoom)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to restore saved zoom", e)
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Camera bound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProviderFuture.get()?.unbindAll()
        }
    }
    
    // Handle external display
    LaunchedEffect(externalDisplay, camera, rotation, isVerticallyFlipped, isHorizontallyFlipped) {
        val display = externalDisplay
        val cam = camera
        
        if (display != null && cam != null) {
            try {
                // Dismiss existing presentation
                externalPresentation?.dismiss()
                
                // Create new presentation
                val presentation = SimpleCameraPresentation(
                    context, 
                    display, 
                    rotation, 
                    CameraRotationHelper.isFrontCamera(cameraState.cameraSelector),
                    isVerticallyFlipped,
                    isHorizontallyFlipped
                )
                // Set the actual camera preview size if available
                val previewSize = actualPreviewSize ?: selectedResolution
                previewSize?.let { size ->
                    presentation.setCameraPreviewSize(size)
                    Log.d(TAG, "Setting external display camera size: ${size.width}x${size.height}")
                }
                presentation.show()
                
                // Setup camera for external display
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Get supported output sizes for the preview
                        val supportedSizes = getSupportedPreviewSizes(context, cameraProvider, cameraState.cameraSelector)
                        
                        // Select optimal resolution
                        val targetResolution = selectOptimalResolution(supportedSizes)
                        selectedResolution = targetResolution
                        
                        // Create previews with selected resolution
                        // Use getTargetRotation only for front camera
                        val isFrontCam = CameraRotationHelper.isFrontCamera(cameraState.cameraSelector)
                        val externalTargetRotation = CameraRotationHelper.getTargetRotation(rotation, isFrontCam)
                        
                        val previewBuilder = if (targetResolution != null) {
                            Preview.Builder()
                                .setTargetAspectRatio(
                                    if (targetResolution.width * 9 == targetResolution.height * 16) {
                                        AspectRatio.RATIO_16_9
                                    } else {
                                        AspectRatio.RATIO_4_3
                                    }
                                )
                                .setTargetRotation(externalTargetRotation)
                        } else {
                            Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .setTargetRotation(externalTargetRotation)
                        }
                        
                        // Create a second preview for external display
                        val externalPreview = previewBuilder.build()
                        externalPreview.setSurfaceProvider { surfaceRequest ->
                            // Update the actual preview size
                            val resolution = surfaceRequest.resolution
                            actualPreviewSize = Size(resolution.width, resolution.height)
                            presentation.setCameraPreviewSize(actualPreviewSize!!)
                            Log.d(TAG, "External preview size from surface request: ${resolution.width}x${resolution.height}")
                            
                            // Forward to the actual surface provider
                            presentation.previewView.surfaceProvider.onSurfaceRequested(surfaceRequest)
                        }
                        
                        // Unbind and rebind with both previews
                        cameraProvider.unbindAll()
                        
                        val mainPreview = previewBuilder.build()
                        mainPreview.setSurfaceProvider { surfaceRequest ->
                            // Update the actual preview size
                            val resolution = surfaceRequest.resolution
                            actualPreviewSize = Size(resolution.width, resolution.height)
                            Log.d(TAG, "Main preview size from surface request: ${resolution.width}x${resolution.height}")
                            
                            // Forward to the actual surface provider
                            mainPreviewView.surfaceProvider.onSurfaceRequested(surfaceRequest)
                        }
                        
                        // Bind both previews
                        val newCam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraState.cameraSelector,
                            mainPreview,
                            externalPreview
                        )
                        
                        camera = newCam
                        
                        // Update zoom state
                        newCam.cameraInfo.zoomState.value?.let { zoomState ->
                            cameraState = cameraState.updateZoomBounds(
                                zoomState.minZoomRatio,
                                zoomState.maxZoomRatio
                            ).updateZoomRatio(zoomState.zoomRatio)
                        }
                        
                        externalPresentation = presentation
                        Log.d(TAG, "External display setup complete")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to setup external display", e)
                        presentation.dismiss()
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                Log.e(TAG, "Error creating presentation", e)
            }
        } else {
            // No external display, ensure we're showing on main display only
            externalPresentation?.dismiss()
            externalPresentation = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main camera preview (full screen)
        AndroidView(
            factory = { mainPreviewView },
            modifier = Modifier
                .fillMaxSize()
                .clickable { showSidebar = !showSidebar }
        )
        
        // Top icons when sidebar is hidden
        if (!showSidebar) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio indicator
                audioState?.let { state ->
                    if (state.isCapturing) {
                        Icon(
                            imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (state.isMuted) "Audio muted" else "Audio recording active",
                            modifier = Modifier.size(24.dp),
                            tint = if (state.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Menu icon
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle sidebar",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Sidebar overlay
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Camera Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Divider()
                
                // Display Status Section
                SidebarSection(title = "Display Status") {
                    StatusRow(
                        label = "External Display",
                        value = if (cameraState.isExternalDisplayConnected) "Connected" else "Not Connected",
                        valueColor = if (cameraState.isExternalDisplayConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        showBadge = cameraState.isExternalDisplayConnected,
                        badgeText = "LIVE"
                    )
                }
                
                // Audio Status Section
                audioState?.let { state ->
                    SidebarSection(title = "Audio Status") {
                        StatusRow(
                            label = "External Audio",
                            value = if (state.hasExternalOutput) "Connected" else "Not Connected",
                            valueColor = if (state.hasExternalOutput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.hasExternalOutput) {
                            StatusRow(
                                label = "Status",
                                value = if (state.isCapturing) "Active" else "Inactive",
                                valueColor = if (state.isCapturing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                showBadge = state.isCapturing && !state.isMuted,
                                badgeText = "REC"
                            )
                        }
                        state.error?.let { error ->
                            StatusRow(
                                label = "Error",
                                value = error,
                                valueColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Microphone Information Section
                    if (state.isCapturing) {
                        SidebarSection(title = "Microphone") {
                            StatusRow(
                                label = "Device",
                                value = state.microphoneName
                            )
                            StatusRow(
                                label = "Format",
                                value = "${state.sampleRate / 1000}kHz, " +
                                        "${if (state.channelCount == 2) "Stereo" else "Mono"}, " +
                                        "16-bit PCM"
                            )
                        }
                        
                        // Audio Output Section
                        SidebarSection(title = "Audio Output") {
                            ClickableRow(
                                label = "Device",
                                value = state.outputDeviceName,
                                onClick = { showAudioOutputDialog = true }
                            )
                            if (state.hasExternalOutput) {
                                MuteControlRow(
                                    label = "Output",
                                    isMuted = state.isMuted,
                                    isManuallyMuted = state.isManuallyMuted,
                                    onClick = { audioCoordinator?.toggleMute() }
                                )
                            } else {
                                StatusRow(
                                    label = "Output",
                                    value = "Disabled (No External Audio)",
                                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Camera Information Section
                SidebarSection(title = "Camera Information") {
                    actualPreviewSize?.let { size ->
                        StatusRow(
                            label = "Resolution",
                            value = "${size.width}x${size.height}"
                        )
                        StatusRow(
                            label = "Aspect Ratio",
                            value = String.format("%.2f", size.width.toFloat() / size.height)
                        )
                    }
                }
                
                // Camera Controls Section
                SidebarSection(title = "Camera Controls") {
                    ClickableRow(
                        label = "Camera",
                        value = when (cameraState.cameraSelector) {
                            CameraSelector.DEFAULT_BACK_CAMERA -> "Back Camera"
                            CameraSelector.DEFAULT_FRONT_CAMERA -> "Front Camera"
                            else -> "Unknown"
                        },
                        onClick = { showCameraDialog = true }
                    )
                    
                    camera?.let { cam ->
                        ClickableRow(
                            label = "Zoom",
                            value = "${String.format("%.1f", cameraState.zoomRatio)}x",
                            onClick = { showZoomDialog = true }
                        )
                    }
                }
                
                // External Display Controls Section
                if (cameraState.isExternalDisplayConnected) {
                    SidebarSection(title = "External Display") {
                        ClickableRow(
                            label = "Flip Controls",
                            value = buildString {
                                if (isVerticallyFlipped || isHorizontallyFlipped) {
                                    if (isVerticallyFlipped) append("V")
                                    if (isHorizontallyFlipped) append("H")
                                } else {
                                    append("None")
                                }
                            },
                            onClick = { showFlipDialog = true }
                        )
                    }
                }
            }
        }
        }
    }
    
    // Camera Selection Dialog
    if (showCameraDialog) {
        CameraSelectionDialog(
            currentSelector = cameraState.cameraSelector,
            onSelectorChanged = { newSelector ->
                cameraState = cameraState.copy(cameraSelector = newSelector)
                // Save the selected camera
                coroutineScope.launch {
                    settingsRepository.updateLastSelectedCamera(newSelector)
                }
                showCameraDialog = false
            },
            onDismiss = { showCameraDialog = false }
        )
    }
    
    // Zoom Control Dialog
    if (showZoomDialog) {
        camera?.let { cam ->
            ZoomControlDialog(
                zoomRatio = cameraState.zoomRatio,
                minZoomRatio = cameraState.minZoomRatio,
                maxZoomRatio = cameraState.maxZoomRatio,
                onZoomChanged = { newZoom ->
                    cameraState = cameraState.updateZoomRatio(newZoom)
                    coroutineScope.launch {
                        try {
                            cam.cameraControl.setZoomRatio(newZoom)
                            // Save zoom settings
                            settingsRepository.updateCameraZoom(
                                cameraState.cameraSelector,
                                newZoom,
                                cameraState.minZoomRatio,
                                cameraState.maxZoomRatio
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to set zoom", e)
                        }
                    }
                },
                onDismiss = { showZoomDialog = false }
            )
        }
    }
    
    // Flip Control Dialog
    if (showFlipDialog) {
        FlipControlDialog(
            isVerticallyFlipped = isVerticallyFlipped,
            isHorizontallyFlipped = isHorizontallyFlipped,
            onVerticalFlipChanged = { flipped ->
                isVerticallyFlipped = flipped
                externalPresentation?.updateFlipStates(isVerticallyFlipped, isHorizontallyFlipped)
                // Save display settings
                currentDisplayId?.let { displayId ->
                    coroutineScope.launch {
                        settingsRepository.updateDisplayFlipSettings(
                            displayId,
                            externalDisplay?.name ?: "External Display",
                            isVerticallyFlipped,
                            isHorizontallyFlipped
                        )
                    }
                }
            },
            onHorizontalFlipChanged = { flipped ->
                isHorizontallyFlipped = flipped
                externalPresentation?.updateFlipStates(isVerticallyFlipped, isHorizontallyFlipped)
                // Save display settings
                currentDisplayId?.let { displayId ->
                    coroutineScope.launch {
                        settingsRepository.updateDisplayFlipSettings(
                            displayId,
                            externalDisplay?.name ?: "External Display",
                            isVerticallyFlipped,
                            isHorizontallyFlipped
                        )
                    }
                }
            },
            onDismiss = { showFlipDialog = false }
        )
    }
    
    // Audio Output Selection Dialog
    if (showAudioOutputDialog) {
        audioCoordinator?.let { coordinator ->
            val availableDevices by coordinator.getAvailableOutputDevices().collectAsState()
            val currentDeviceName = audioState?.outputDeviceName ?: "Unknown"
            
            AudioOutputSelectionDialog(
                availableDevices = availableDevices,
                currentDeviceName = currentDeviceName,
                onDeviceSelected = { device ->
                    coordinator.setOutputDevice(device)
                    // Save audio output device preference
                    coroutineScope.launch {
                        settingsRepository.updateAudioOutputDevice(device?.id)
                    }
                    showAudioOutputDialog = false
                },
                onDismiss = { showAudioOutputDialog = false },
                getDeviceName = { device -> coordinator.getDeviceDisplayName(device) }
            )
        }
    }
}

/**
 * Simple presentation for external display with proper aspect ratio handling
 */
class SimpleCameraPresentation(
    context: Context,
    display: Display,
    private val deviceRotation: Int = Surface.ROTATION_0,
    private val isFrontCamera: Boolean = false,
    private var isVerticallyFlipped: Boolean = false,
    private var isHorizontallyFlipped: Boolean = false
) : android.app.Presentation(context, display) {
    
    lateinit var previewView: PreviewView
    private var textureView: TextureView? = null
    
    // Store the camera preview resolution for aspect ratio calculations
    private var cameraPreviewSize: Size? = null
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Make fullscreen
            window?.decorView?.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            
            // Get display metrics and info
            val displayMetrics = android.util.DisplayMetrics()
            display.getRealMetrics(displayMetrics)
            val displayWidth = displayMetrics.widthPixels
            val displayHeight = displayMetrics.heightPixels
            val displayRotation = display.rotation
            val displayAspectRatio = displayWidth.toFloat() / displayHeight.toFloat()
            
            Log.d(TAG, "External display info: ${displayWidth}x${displayHeight}, " +
                      "aspect ratio: $displayAspectRatio, rotation: $displayRotation, " +
                      "density: ${displayMetrics.density}, densityDpi: ${displayMetrics.densityDpi}")
            
            // Determine if display is in portrait mode
            val isDisplayPortrait = displayHeight > displayWidth
            
            // Create the main container with black background for letterboxing
            val mainContainer = android.widget.FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            // Create PreviewView
            previewView = PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                // Start with FIT_CENTER to ensure no cropping
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }
            
            // Camera preview is typically 16:9 in landscape orientation
            val cameraAspectRatio = 16f / 9f
            
            Log.d(TAG, "Camera aspect ratio: $cameraAspectRatio, Display: ${displayWidth}x${displayHeight}")
            
            // Calculate the optimal size to fit the display while maintaining aspect ratio
            val optimalWidth: Int
            val optimalHeight: Int
            
            // Since the camera preview appears to be 270 degrees rotated,
            // we need to compensate with a 90 degree rotation (270 + 90 = 360 = 0)
            
            if (isDisplayPortrait) {
                // Portrait display - camera is rotated, so we need to fix it
                // Camera preview seems to be in wrong orientation, compensate with rotation
                
                // For portrait, we want the 16:9 preview to fit vertically
                val rotatedAspectRatio = 9f / 16f  // After rotation
                
                if (displayWidth.toFloat() / displayHeight > rotatedAspectRatio) {
                    // Display is wider than rotated preview - fit height
                    optimalHeight = displayHeight
                    optimalWidth = (displayHeight * rotatedAspectRatio).toInt()
                } else {
                    // Display is narrower than rotated preview - fit width
                    optimalWidth = displayWidth
                    optimalHeight = (displayWidth / rotatedAspectRatio).toInt()
                }
                
                // Calculate rotation based on device orientation
                val rotationDegrees = getRotationCompensation()
                previewView.rotation = rotationDegrees
                previewView.layoutParams = android.widget.FrameLayout.LayoutParams(
                    optimalWidth,
                    optimalHeight
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                
                mainContainer.addView(previewView)
                
                Log.d(TAG, "Portrait display: 180° rotation to fix upside-down, size ${optimalWidth}x${optimalHeight}")
            } else {
                // Landscape display - also needs rotation compensation
                // If preview is 270 degrees rotated in landscape, we need to fix it
                
                if (displayWidth.toFloat() / displayHeight > cameraAspectRatio) {
                    // Display is wider than preview - fit height
                    optimalHeight = displayHeight
                    optimalWidth = (displayHeight * cameraAspectRatio).toInt()
                } else {
                    // Display is narrower than preview - fit width
                    optimalWidth = displayWidth
                    optimalHeight = (displayWidth / cameraAspectRatio).toInt()
                }
                
                // Calculate rotation based on device orientation
                val rotationDegrees = getRotationCompensation()
                previewView.rotation = rotationDegrees
                previewView.layoutParams = android.widget.FrameLayout.LayoutParams(
                    optimalWidth,
                    optimalHeight
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                
                mainContainer.addView(previewView)
                
                Log.d(TAG, "Landscape display: 180° rotation to fix upside-down, size ${optimalWidth}x${optimalHeight}")
            }
            
            setContentView(mainContainer)
            
            // Apply initial flip transformation
            applyFlipTransformation()
            
            Log.d(TAG, "Presentation created for display ${display.displayId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating presentation", e)
        }
    }
    
    
    fun setCameraPreviewSize(size: Size) {
        cameraPreviewSize = size
        Log.d(TAG, "Camera preview size set to: ${size.width}x${size.height}")
        
        // If the presentation is already showing, we need to reconfigure the layout
        if (isShowing) {
            window?.decorView?.post {
                reconfigureLayout(size)
            }
        }
    }
    
    fun updateFlipStates(verticalFlip: Boolean, horizontalFlip: Boolean) {
        isVerticallyFlipped = verticalFlip
        isHorizontallyFlipped = horizontalFlip
        
        // Apply the flip transformation to the preview view
        if (isShowing) {
            applyFlipTransformation()
        }
    }
    
    private fun applyFlipTransformation() {
        val scaleX = if (isHorizontallyFlipped) -1f else 1f
        val scaleY = if (isVerticallyFlipped) -1f else 1f
        
        previewView.scaleX = scaleX
        previewView.scaleY = scaleY
        
        Log.d(TAG, "Applied flip transformation: scaleX=$scaleX, scaleY=$scaleY")
    }
    
    private fun reconfigureLayout(cameraSize: Size) {
        // Layout reconfiguration is handled in onCreate now
        Log.d(TAG, "Camera size updated to: ${cameraSize.width}x${cameraSize.height}")
    }
    
    private fun getRotationCompensation(): Float {
        // Get display rotation
        val displayRotation = display.rotation
        
        // Use CameraRotationHelper for rotation compensation calculation
        return CameraRotationHelper.getRotationCompensation(
            deviceRotation,
            displayRotation,
            isFrontCamera
        )
    }
}

// Helper Composables

@Composable
private fun SidebarSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
            if (showBadge && badgeText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(badgeText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ClickableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = valueColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp),
                    tint = valueColor
                )
            }
        }
    }
}

@Composable
private fun MuteControlRow(
    label: String,
    isMuted: Boolean,
    isManuallyMuted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMuted) {
                        if (isManuallyMuted) "Muted" else "Muted (No Ext. Audio)"
                    } else {
                        "Active"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isMuted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    modifier = Modifier.size(20.dp),
                    tint = if (isMuted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

// Dialog Implementations

@Composable
private fun CameraSelectionDialog(
    currentSelector: CameraSelector,
    onSelectorChanged: (CameraSelector) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Camera",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                RadioButtonOption(
                    text = "Back Camera",
                    selected = currentSelector == CameraSelector.DEFAULT_BACK_CAMERA,
                    onClick = { onSelectorChanged(CameraSelector.DEFAULT_BACK_CAMERA) }
                )
                
                RadioButtonOption(
                    text = "Front Camera",
                    selected = currentSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
                    onClick = { onSelectorChanged(CameraSelector.DEFAULT_FRONT_CAMERA) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomControlDialog(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Zoom Control",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Zoom: ${String.format("%.1f", zoomRatio)}x",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Slider(
                    value = zoomRatio,
                    onValueChange = onZoomChanged,
                    valueRange = minZoomRatio..maxZoomRatio,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format("%.1f", minZoomRatio)}x",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${String.format("%.1f", maxZoomRatio)}x",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipControlDialog(
    isVerticallyFlipped: Boolean,
    isHorizontallyFlipped: Boolean,
    onVerticalFlipChanged: (Boolean) -> Unit,
    onHorizontalFlipChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Flip Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SwitchOption(
                    text = "Vertical Flip",
                    checked = isVerticallyFlipped,
                    onCheckedChange = onVerticalFlipChanged
                )
                
                SwitchOption(
                    text = "Horizontal Flip",
                    checked = isHorizontallyFlipped,
                    onCheckedChange = onHorizontalFlipChanged
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SwitchOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AudioOutputSelectionDialog(
    availableDevices: List<AudioDeviceInfo>,
    currentDeviceName: String,
    onDeviceSelected: (AudioDeviceInfo?) -> Unit,
    onDismiss: () -> Unit,
    getDeviceName: (AudioDeviceInfo) -> String
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Audio Output",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (availableDevices.isEmpty()) {
                    Text(
                        text = "No audio output devices available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    availableDevices.forEach { device ->
                        val deviceName = getDeviceName(device)
                        val isSelected = deviceName == currentDeviceName
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onDeviceSelected(device) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
