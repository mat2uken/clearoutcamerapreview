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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraUtils
import app.mat2uken.android.app.clearoutcamerapreview.utils.DisplayUtils
import app.mat2uken.android.app.clearoutcamerapreview.utils.DisplayInfo
import app.mat2uken.android.app.clearoutcamerapreview.model.Size as CustomSize
import app.mat2uken.android.app.clearoutcamerapreview.camera.CameraState
import app.mat2uken.android.app.clearoutcamerapreview.presentation.SimpleCameraPresentation
import app.mat2uken.android.app.clearoutcamerapreview.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
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
import app.mat2uken.android.app.clearoutcamerapreview.utils.FrameRateUtils
import app.mat2uken.android.app.clearoutcamerapreview.data.SettingsRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import android.os.PowerManager

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
        // Find the camera info that matches the selector
        val cameraInfos = cameraProvider.availableCameraInfos
        Log.d(TAG, "Total available cameras: ${cameraInfos.size}")
        
        // Try different approaches to find the matching camera
        val cameraInfo = cameraInfos.find { info ->
            try {
                val lensFacing = info.lensFacing
                
                // Match based on lens facing
                when (cameraSelector) {
                    CameraSelector.DEFAULT_BACK_CAMERA -> lensFacing == CameraSelector.LENS_FACING_BACK
                    CameraSelector.DEFAULT_FRONT_CAMERA -> lensFacing == CameraSelector.LENS_FACING_FRONT
                    else -> false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking camera info", e)
                false
            }
        }
        
        cameraInfo?.let { info ->
            val camera2Info = androidx.camera.camera2.interop.Camera2CameraInfo.from(info)
            val cameraId = camera2Info.cameraId
            Log.d(TAG, "Using camera ID: $cameraId")
            
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

/**
 * Gets supported camera formats with resolution and frame rate combinations
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
private fun getSupportedCameraFormats(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    cameraSelector: CameraSelector
): List<app.mat2uken.android.app.clearoutcamerapreview.model.CameraFormat> {
    return try {
        // Find the camera info that matches the selector
        val cameraInfos = cameraProvider.availableCameraInfos
        Log.d(TAG, "getSupportedCameraFormats - Total cameras: ${cameraInfos.size}")
        
        val cameraInfo = cameraInfos.find { info ->
            try {
                val lensFacing = info.lensFacing
                when (cameraSelector) {
                    CameraSelector.DEFAULT_BACK_CAMERA -> lensFacing == CameraSelector.LENS_FACING_BACK
                    CameraSelector.DEFAULT_FRONT_CAMERA -> lensFacing == CameraSelector.LENS_FACING_FRONT
                    else -> false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking camera info in formats", e)
                false
            }
        }
        
        cameraInfo?.let { info ->
            val camera2Info = androidx.camera.camera2.interop.Camera2CameraInfo.from(info)
            val cameraId = camera2Info.cameraId
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(
                android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            
            val formats = mutableListOf<app.mat2uken.android.app.clearoutcamerapreview.model.CameraFormat>()
            
            streamConfigMap?.let { configMap ->
                // Get output sizes for preview
                val sizes = configMap.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                
                sizes?.forEach { androidSize ->
                    // Get supported frame rate ranges for this size
                    val fpsRanges = characteristics.get(
                        android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
                    ) ?: emptyArray()
                    
                    // Check which FPS ranges are actually supported for this size
                    val supportedRanges = fpsRanges.filter { range ->
                        // CameraX preview typically supports all advertised FPS ranges
                        // but we'll be conservative and check if it's reasonable
                        range.upper <= 120 && range.lower >= 5
                    }.map { androidRange ->
                        android.util.Range(androidRange.lower, androidRange.upper)
                    }
                    
                    if (supportedRanges.isNotEmpty()) {
                        formats.add(
                            app.mat2uken.android.app.clearoutcamerapreview.model.CameraFormat(
                                size = CustomSize(androidSize.width, androidSize.height),
                                frameRateRanges = supportedRanges
                            )
                        )
                    }
                }
            }
            
            Log.d(TAG, "Found ${formats.size} camera formats with frame rate information")
            formats.forEach { format ->
                Log.d(TAG, "Format: ${format.size.width}x${format.size.height}, " +
                         "FPS ranges: ${format.frameRateRanges.joinToString { "${it.lower}-${it.upper}" }}")
            }
            
            formats
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Error getting camera formats", e)
        emptyList()
    }
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun SimplifiedMultiDisplayCameraScreen(audioCoordinator: AudioCoordinator? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    
    // Get the current rotation
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.display.rotation
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.rotation
    }
    
    // Settings repository
    val settingsRepository = remember { SettingsRepository(context) }
    
    // Camera state
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraState by remember { mutableStateOf(CameraState()) }
    var selectedFrameRateRange by remember { mutableStateOf<android.util.Range<Int>?>(null) }
    var actualFrameRateRange by remember { mutableStateOf<android.util.Range<Int>?>(null) }
    
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
    var areSettingsLoaded by remember { mutableStateOf(false) }
    
    // Display management
    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    var externalDisplay by remember { mutableStateOf<Display?>(null) }
    var externalPresentation by remember { mutableStateOf<SimpleCameraPresentation?>(null) }
    var availableDisplays by remember { mutableStateOf<List<DisplayInfo>>(emptyList()) }
    var selectedDisplayId by remember { mutableStateOf<Int?>(null) }
    
    // Wake lock management
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val wakeLock = remember { 
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ClearOutCameraPreview:ExternalDisplayWakeLock"
        )
    }
    
    // Dialog states
    var showCameraDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showFlipDialog by remember { mutableStateOf(false) }
    var showAudioOutputDialog by remember { mutableStateOf(false) }
    var showDisplaySelectionDialog by remember { mutableStateOf(false) }
    
    // Sidebar visibility state
    var showSidebar by remember { mutableStateOf(true) }
    
    // Audio state
    val audioState by audioCoordinator?.audioState?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // Load saved audio output device preference when audio coordinator is ready
    // Only run once when the component is first composed
    LaunchedEffect(audioCoordinator) {
        if (audioCoordinator != null) {
            // Wait a bit for audio system to stabilize
            delay(500)
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
            // Use default implementation mode for compatibility
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // Set scale type to FIT_CENTER to show full image with letterboxing
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    
    // Function to check for external displays
    fun checkExternalDisplays() {
        val displays = displayManager.displays
        
        // Get information about all displays
        availableDisplays = DisplayUtils.getAllDisplayInfo(displayManager, context)
        
        // Find external displays
        val externalDisplays = DisplayUtils.findAllExternalDisplays(displays)
        
        // If we have a selected display ID, try to find it; otherwise use the first external display
        val targetDisplay = if (selectedDisplayId != null) {
            displays.find { it.displayId == selectedDisplayId }
        } else {
            externalDisplays.firstOrNull()
        }
        
        // Update selected display ID if we found a target display
        if (targetDisplay != null && selectedDisplayId == null) {
            selectedDisplayId = targetDisplay.displayId
        }
        
        externalDisplay = targetDisplay
        cameraState = cameraState.updateExternalDisplay(
            connected = targetDisplay != null,
            displayId = targetDisplay?.displayId
        )
        
        // Manage wake lock based on external display connection
        if (targetDisplay != null) {
            // Acquire wake lock when external display is connected
            if (!wakeLock.isHeld) {
                try {
                    wakeLock.acquire()
                    Log.d(TAG, "Wake lock acquired for external display")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to acquire wake lock", e)
                }
            }
        } else {
            // Release wake lock when no external display is connected
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.d(TAG, "Wake lock released - no external display")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to release wake lock", e)
                }
            }
        }
        
        // Load display settings if external display is found
        targetDisplay?.let { display ->
            val displayId = display.displayId.toString()
            currentDisplayId = displayId
            areSettingsLoaded = false
            coroutineScope.launch {
                val displaySettings = settingsRepository.getDisplaySettings(displayId, cameraState.cameraSelector)
                isVerticallyFlipped = displaySettings.isVerticallyFlipped
                isHorizontallyFlipped = displaySettings.isHorizontallyFlipped
                areSettingsLoaded = true
                Log.d(TAG, "Display settings loaded for $displayId with ${if (cameraState.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"} camera: V=$isVerticallyFlipped, H=$isHorizontallyFlipped")
            }
        } ?: run {
            // Clear current display ID when no external display is connected
            currentDisplayId = null
            areSettingsLoaded = false
            selectedDisplayId = null
        }
        
        Log.d(TAG, "Checked displays. Available displays: ${availableDisplays.size}, External displays: ${externalDisplays.size}, Selected: ${targetDisplay?.displayId}")
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
                // Also check displays on display change events
                checkExternalDisplays()
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
            // Release wake lock when composable is disposed
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.d(TAG, "Wake lock released on dispose")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to release wake lock on dispose", e)
                }
            }
        }
    }
    
    // Reload display settings when camera changes
    LaunchedEffect(cameraState.cameraSelector, currentDisplayId) {
        if (currentDisplayId != null && externalDisplay != null) {
            val displaySettings = settingsRepository.getDisplaySettings(currentDisplayId!!, cameraState.cameraSelector)
            isVerticallyFlipped = displaySettings.isVerticallyFlipped
            isHorizontallyFlipped = displaySettings.isHorizontallyFlipped
            Log.d(TAG, "Reloaded display settings for camera change: V=$isVerticallyFlipped, H=$isHorizontallyFlipped")
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
                
                // Get supported camera formats with frame rate information
                val supportedFormats = getSupportedCameraFormats(context, cameraProvider, cameraState.cameraSelector)
                Log.d(TAG, "Total supported formats: ${supportedFormats.size}")
                
                // Log all available resolutions for debugging
                supportedFormats.forEach { format ->
                    Log.d(TAG, "Available format: ${format.size.width}x${format.size.height}")
                }
                
                // Also get supported preview sizes for comparison
                val supportedPreviewSizes = getSupportedPreviewSizes(context, cameraProvider, cameraState.cameraSelector)
                Log.d(TAG, "Total supported preview sizes: ${supportedPreviewSizes.size}")
                supportedPreviewSizes.forEach { size ->
                    Log.d(TAG, "Available preview size: ${size.width}x${size.height}")
                }
                
                // Select optimal format (resolution + frame rate)
                val optimalFormat = CameraUtils.selectOptimalCameraFormat(supportedFormats)
                
                if (optimalFormat != null) {
                    val (format, frameRateRange) = optimalFormat
                    selectedResolution = Size(format.size.width, format.size.height)
                    selectedFrameRateRange = frameRateRange
                    Log.d(TAG, "Selected format: ${format.size.width}x${format.size.height} @ ${frameRateRange.lower}-${frameRateRange.upper} fps")
                    Log.d(TAG, "selectedFrameRateRange set to: ${selectedFrameRateRange?.lower}-${selectedFrameRateRange?.upper}")
                } else {
                    // Fallback to resolution-only selection
                    val supportedSizes = getSupportedPreviewSizes(context, cameraProvider, cameraState.cameraSelector)
                    Log.d(TAG, "Fallback - Total supported sizes: ${supportedSizes.size}")
                    supportedSizes.forEach { size ->
                        Log.d(TAG, "Fallback - Available size: ${size.width}x${size.height}")
                    }
                    val targetResolution = selectOptimalResolution(supportedSizes)
                    selectedResolution = targetResolution
                    selectedFrameRateRange = null
                    Log.d(TAG, "Using fallback resolution selection: ${targetResolution?.width}x${targetResolution?.height}")
                }
                
                // Create preview with selected resolution
                val targetRotation = CameraRotationHelper.getTargetRotation(rotation)
                val targetResolution = selectedResolution
                
                val preview = if (targetResolution != null) {
                    Log.d(TAG, "Creating preview with target resolution: ${targetResolution.width}x${targetResolution.height}, rotation: $rotation")
                    
                    // Create resolution selector with multiple strategies to force 1920x1080
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionFilter { supportedSizes, _ ->
                            Log.d(TAG, "ResolutionFilter called with ${supportedSizes.size} sizes: ${supportedSizes.joinToString { "${it.width}x${it.height}" }}")
                            
                            // Sort sizes by prioritizing 1920x1080
                            val sortedSizes = supportedSizes.sortedWith { size1, size2 ->
                                // 1920x1080 always wins
                                if (size1.width == 1920 && size1.height == 1080) -1
                                else if (size2.width == 1920 && size2.height == 1080) 1
                                else (size2.width * size2.height).compareTo(size1.width * size1.height)
                            }
                            
                            // First, try to find exact match
                            val exactMatch = sortedSizes.filter { size ->
                                size.width == targetResolution.width && 
                                size.height == targetResolution.height
                            }
                            
                            if (exactMatch.isNotEmpty()) {
                                Log.d(TAG, "Found exact match for ${targetResolution.width}x${targetResolution.height}")
                                // Return only the exact match to force CameraX to use it
                                exactMatch
                            } else {
                                // If no exact match but we want 1920x1080, check if it exists in a different form
                                val has1080p = supportedSizes.any { it.width == 1920 && it.height == 1080 }
                                if (has1080p) {
                                    Log.w(TAG, "1920x1080 exists but wasn't matched. Returning all 1920x1080 entries.")
                                    supportedSizes.filter { it.width == 1920 && it.height == 1080 }
                                } else {
                                    Log.w(TAG, "No 1920x1080 available, returning sorted sizes")
                                    sortedSizes
                                }
                            }
                        }
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                targetResolution,
                                ResolutionStrategy.FALLBACK_RULE_NONE  // Use NONE to be strict
                            )
                        )
                        .build()
                    
                    val previewBuilder = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setTargetRotation(targetRotation)
                    
                    // Apply frame rate range and low latency settings if available
                    selectedFrameRateRange?.let { fpsRange ->
                        try {
                            val camera2Interop = androidx.camera.camera2.interop.Camera2Interop.Extender(previewBuilder)
                            
                            // Apply frame rate range
                            camera2Interop.setCaptureRequestOption(
                                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                fpsRange
                            )
                            
                            // Apply low latency settings
                            camera2Interop.setCaptureRequestOption(
                                android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                                android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON
                            )
                            camera2Interop.setCaptureRequestOption(
                                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                            )
                            
                            Log.d(TAG, "Applied frame rate range: ${fpsRange.lower}-${fpsRange.upper} fps with low latency settings")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to apply frame rate range", e)
                        }
                    }
                    
                    previewBuilder.build()
                        .also {
                            Log.d(TAG, "Preview created with target resolution: ${targetResolution.width}x${targetResolution.height}, rotation: $targetRotation, isFront: ${CameraRotationHelper.isFrontCamera(cameraState.cameraSelector)}")
                        }
                } else {
                    // Fallback to default 16:9 resolution if no specific resolution is selected
                    val defaultResolutionSelector = ResolutionSelector.Builder()
                        .setResolutionFilter { supportedSizes, _ ->
                            // Prioritize 1920x1080 if available
                            val has1080p = supportedSizes.filter { it.width == 1920 && it.height == 1080 }
                            if (has1080p.isNotEmpty()) {
                                Log.d(TAG, "Fallback: Found 1920x1080, using it exclusively")
                                has1080p
                            } else {
                                Log.d(TAG, "Fallback: No 1920x1080, using all ${supportedSizes.size} sizes")
                                supportedSizes
                            }
                        }
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_NONE
                            )
                        )
                        .build()
                    
                    Preview.Builder()
                        .setResolutionSelector(defaultResolutionSelector)
                        .setTargetRotation(targetRotation)
                        .build()
                        .also {
                            Log.d(TAG, "Preview created with default 1920x1080 resolution, rotation: $targetRotation, isFront: ${CameraRotationHelper.isFrontCamera(cameraState.cameraSelector)}")
                        }
                }
                
                // Set surface provider for main display
                preview.setSurfaceProvider { surfaceRequest ->
                    // Get the actual preview size from the surface request
                    val resolution = surfaceRequest.resolution
                    actualPreviewSize = Size(resolution.width, resolution.height)
                    Log.d(TAG, "Actual preview size from surface request: ${resolution.width}x${resolution.height}")
                    Log.d(TAG, "Requested resolution was: ${selectedResolution?.width}x${selectedResolution?.height}")
                    
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
                
                // Get actual frame rate from camera
                if (selectedFrameRateRange == null) {
                    actualFrameRateRange = FrameRateUtils.detectActualFrameRate(
                        cam, context, actualPreviewSize, selectedResolution
                    )
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
    
    // Handle external display - add areSettingsLoaded to dependencies to wait for settings
    LaunchedEffect(externalDisplay, camera, rotation, isVerticallyFlipped, isHorizontallyFlipped, currentDisplayId, areSettingsLoaded) {
        val display = externalDisplay
        val cam = camera
        
        if (display != null && cam != null && currentDisplayId != null && areSettingsLoaded) {
            // Add a small delay to ensure main preview is stable
            kotlinx.coroutines.delay(500)
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
                
                try {
                    presentation.show()
                    
                    // Check if the presentation is showing properly
                    if (!presentation.isShowing) {
                        Log.e(TAG, "Presentation failed to show")
                        presentation.dismiss()
                        return@LaunchedEffect
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception showing presentation", e)
                    try {
                        presentation.dismiss()
                    } catch (dismissError: Exception) {
                        Log.e(TAG, "Error dismissing presentation after show failure", dismissError)
                    }
                    return@LaunchedEffect
                }
                
                // Setup camera for external display
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Get supported camera formats with frame rate information
                        val supportedFormats = getSupportedCameraFormats(context, cameraProvider, cameraState.cameraSelector)
                        
                        // Select optimal format (resolution + frame rate)
                        val optimalFormat = CameraUtils.selectOptimalCameraFormat(supportedFormats)
                        
                        if (optimalFormat != null) {
                            val (format, frameRateRange) = optimalFormat
                            selectedResolution = Size(format.size.width, format.size.height)
                            selectedFrameRateRange = frameRateRange
                        } else {
                            // Fallback to resolution-only selection
                            val supportedSizes = getSupportedPreviewSizes(context, cameraProvider, cameraState.cameraSelector)
                            val targetResolution = selectOptimalResolution(supportedSizes)
                            selectedResolution = targetResolution
                            selectedFrameRateRange = null
                        }
                        
                        // Create previews with selected resolution
                        val externalTargetRotation = CameraRotationHelper.getTargetRotation(rotation)
                        
                        val previewBuilder = if (selectedResolution != null) {
                            // Create resolution selector with custom resolution filter
                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionFilter { supportedSizes, _ ->
                                    Log.d(TAG, "External ResolutionFilter called with ${supportedSizes.size} sizes")
                                    
                                    // Try to find exact match
                                    val exactMatch = supportedSizes.filter { size ->
                                        size.width == selectedResolution!!.width && 
                                        size.height == selectedResolution!!.height
                                    }
                                    
                                    if (exactMatch.isNotEmpty()) {
                                        Log.d(TAG, "External display: Found exact match for ${selectedResolution!!.width}x${selectedResolution!!.height}")
                                        exactMatch
                                    } else {
                                        Log.w(TAG, "External display: No exact match, using all sizes")
                                        supportedSizes
                                    }
                                }
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        selectedResolution!!,
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                            
                            val builder = Preview.Builder()
                                .setResolutionSelector(resolutionSelector)
                                .setTargetRotation(externalTargetRotation)
                            
                            // Apply frame rate range and low latency settings if available
                            selectedFrameRateRange?.let { fpsRange ->
                                try {
                                    val camera2Interop = androidx.camera.camera2.interop.Camera2Interop.Extender(builder)
                                    
                                    // Apply frame rate range
                                    camera2Interop.setCaptureRequestOption(
                                        android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                        fpsRange
                                    )
                                    
                                    // Apply low latency settings
                                    camera2Interop.setCaptureRequestOption(
                                        android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                                        android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON
                                    )
                                    camera2Interop.setCaptureRequestOption(
                                        android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                                        android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to apply frame rate range for external display", e)
                                }
                            }
                            builder
                        } else {
                            // Fallback to default 16:9 resolution
                            val defaultResolutionStrategy = ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                            
                            val defaultResolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(defaultResolutionStrategy)
                                .build()
                            
                            Preview.Builder()
                                .setResolutionSelector(defaultResolutionSelector)
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
                        
                        // Get actual frame rate from camera (same as above)
                        if (selectedFrameRateRange == null) {
                            actualFrameRateRange = FrameRateUtils.detectActualFrameRate(
                                newCam, context, actualPreviewSize, selectedResolution
                            )
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
            modifier = Modifier.fillMaxSize()
        )
        
        // Floating button when sidebar is hidden
        if (!showSidebar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = statusBarHeight + 16.dp,
                        end = 16.dp
                    )
            ) {
                FloatingActionButton(
                    onClick = { showSidebar = true },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Show sidebar",
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                    .fillMaxHeight()
                    .padding(top = statusBarHeight),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Fixed Header with collapse button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Camera Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showSidebar = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Hide sidebar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                
                // Display Status Section
                SidebarSection(title = "Display Status") {
                    StatusRow(
                        label = "External Display",
                        value = if (cameraState.isExternalDisplayConnected) "Connected" else "Not Connected",
                        valueColor = if (cameraState.isExternalDisplayConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        showBadge = cameraState.isExternalDisplayConnected,
                        badgeText = "LIVE"
                    )
                    
                    // Show current display information if connected
                    if (cameraState.isExternalDisplayConnected) {
                        externalDisplay?.let { display ->
                            val displayInfo = availableDisplays.find { it.displayId == display.displayId }
                            displayInfo?.let { info ->
                                StatusRow(
                                    label = "Display Name",
                                    value = info.name
                                )
                                StatusRow(
                                    label = "Display ID",
                                    value = info.displayId.toString()
                                )
                                StatusRow(
                                    label = "Resolution",
                                    value = info.getDisplaySizeString()
                                )
                            }
                        }
                        
                        // Display selection dropdown if multiple external displays are available
                        val externalDisplays = availableDisplays.filter { !it.isDefaultDisplay }
                        if (externalDisplays.size > 1) {
                            ClickableRow(
                                label = "Select Display",
                                value = externalDisplays.find { it.displayId == selectedDisplayId }?.name ?: "Select...",
                                onClick = { showDisplaySelectionDialog = true }
                            )
                        }
                    }
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
                    // Show frame rate - prefer selected, then actual, then "Not available"
                    val fpsRange = selectedFrameRateRange ?: actualFrameRateRange
                    if (fpsRange != null) {
                        StatusRow(
                            label = "Frame Rate",
                            value = if (fpsRange.lower == fpsRange.upper) {
                                "${fpsRange.upper} fps"
                            } else {
                                "${fpsRange.lower}-${fpsRange.upper} fps"
                            }
                        )
                    } else {
                        // Show default message if frame rate info is not available
                        StatusRow(
                            label = "Frame Rate",
                            value = "Detecting..."
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
                    
                    camera?.let { _ ->
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
                    } // End of scrollable content Column
                } // End of main Column
            } // End of Card
        } // End of AnimatedVisibility
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
                // Save display settings with camera info
                currentDisplayId?.let { displayId ->
                    coroutineScope.launch {
                        settingsRepository.updateDisplayFlipSettings(
                            displayId,
                            externalDisplay?.name ?: "External Display",
                            cameraState.cameraSelector,
                            isVerticallyFlipped,
                            isHorizontallyFlipped
                        )
                    }
                }
            },
            onHorizontalFlipChanged = { flipped ->
                isHorizontallyFlipped = flipped
                externalPresentation?.updateFlipStates(isVerticallyFlipped, isHorizontallyFlipped)
                // Save display settings with camera info
                currentDisplayId?.let { displayId ->
                    coroutineScope.launch {
                        settingsRepository.updateDisplayFlipSettings(
                            displayId,
                            externalDisplay?.name ?: "External Display",
                            cameraState.cameraSelector,
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
    
    // Display Selection Dialog
    if (showDisplaySelectionDialog) {
        val externalDisplays = availableDisplays.filter { !it.isDefaultDisplay }
        val currentDisplayName = externalDisplays.find { it.displayId == selectedDisplayId }?.name ?: "Unknown"
        
        DisplaySelectionDialog(
            availableDisplays = externalDisplays,
            currentDisplayName = currentDisplayName,
            onDisplaySelected = { displayInfo ->
                selectedDisplayId = displayInfo.displayId
                checkExternalDisplays() // Refresh display state with new selection
                showDisplaySelectionDialog = false
            },
            onDismiss = { showDisplaySelectionDialog = false }
        )
    }
}



