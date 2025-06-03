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
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraUtils
import app.mat2uken.android.app.clearoutcamerapreview.utils.DisplayUtils
import app.mat2uken.android.app.clearoutcamerapreview.model.Size as CustomSize
import app.mat2uken.android.app.clearoutcamerapreview.camera.CameraState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

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

@Composable
fun SimplifiedMultiDisplayCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // Camera state
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraState by remember { mutableStateOf(CameraState()) }
    var selectedResolution by remember { mutableStateOf<Size?>(null) }
    var actualPreviewSize by remember { mutableStateOf<Size?>(null) }
    
    // Display management
    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    var externalDisplay by remember { mutableStateOf<Display?>(null) }
    var externalPresentation by remember { mutableStateOf<SimpleCameraPresentation?>(null) }
    
    // Create a single preview view for the main display with proper aspect ratio
    val mainPreviewView = remember { 
        PreviewView(context).apply {
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
    
    // Camera setup
    DisposableEffect(cameraState.cameraSelector) {
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
                val preview = if (targetResolution != null) {
                    Preview.Builder()
                        .setTargetAspectRatio(
                            if (targetResolution.width * 9 == targetResolution.height * 16) {
                                AspectRatio.RATIO_16_9
                            } else {
                                AspectRatio.RATIO_4_3
                            }
                        )
                        .build()
                        .also {
                            Log.d(TAG, "Preview created with target aspect ratio for resolution: ${targetResolution.width}x${targetResolution.height}")
                        }
                } else {
                    Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build()
                        .also {
                            Log.d(TAG, "Preview created with default 16:9 aspect ratio")
                        }
                }
                
                // Set surface provider for main display
                preview.setSurfaceProvider { surfaceRequest ->
                    // Get the actual preview size from the surface request
                    val resolution = surfaceRequest.resolution
                    actualPreviewSize = Size(resolution.width, resolution.height)
                    Log.d(TAG, "Actual preview size from surface request: ${resolution.width}x${resolution.height}")
                    
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
    LaunchedEffect(externalDisplay, camera) {
        val display = externalDisplay
        val cam = camera
        
        if (display != null && cam != null) {
            try {
                // Dismiss existing presentation
                externalPresentation?.dismiss()
                
                // Create new presentation
                val presentation = SimpleCameraPresentation(context, display)
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
                        val previewBuilder = if (targetResolution != null) {
                            Preview.Builder()
                                .setTargetAspectRatio(
                                    if (targetResolution.width * 9 == targetResolution.height * 16) {
                                        AspectRatio.RATIO_16_9
                                    } else {
                                        AspectRatio.RATIO_4_3
                                    }
                                )
                                .setTargetRotation(android.view.Surface.ROTATION_90) // Force rotation
                        } else {
                            Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .setTargetRotation(android.view.Surface.ROTATION_90) // Force rotation
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
        // Main camera preview
        AndroidView(
            factory = { mainPreviewView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // External display status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (cameraState.isExternalDisplayConnected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Display status",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (cameraState.isExternalDisplayConnected) {
                                "External Display Connected"
                            } else {
                                "No External Display"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (cameraState.isExternalDisplayConnected) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("LIVE", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Resolution info
            if (selectedResolution != null || actualPreviewSize != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        selectedResolution?.let { resolution ->
                            Text(
                                text = "Target Resolution: ${resolution.width}x${resolution.height}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        actualPreviewSize?.let { size ->
                            Text(
                                text = "Actual Preview: ${size.width}x${size.height} (${String.format("%.2f", size.width.toFloat() / size.height)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Camera selector dropdown
            CameraSelectorDropdown(
                currentSelector = cameraState.cameraSelector,
                onSelectorChanged = { newSelector ->
                    cameraState = cameraState.copy(cameraSelector = newSelector)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Zoom slider
            camera?.let { cam ->
                ZoomSlider(
                    zoomRatio = cameraState.zoomRatio,
                    minZoomRatio = cameraState.minZoomRatio,
                    maxZoomRatio = cameraState.maxZoomRatio,
                    onZoomChanged = { newZoom ->
                        cameraState = cameraState.updateZoomRatio(newZoom)
                        coroutineScope.launch {
                            try {
                                cam.cameraControl.setZoomRatio(newZoom)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to set zoom", e)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Simple presentation for external display with proper aspect ratio handling
 */
class SimpleCameraPresentation(
    context: Context,
    display: Display
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
                
                // Apply 180 degree rotation to fix upside-down issue
                previewView.rotation = 180f
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
                
                // Apply 180 degree rotation to fix upside-down issue
                previewView.rotation = 180f
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
    
    private fun reconfigureLayout(cameraSize: Size) {
        // Layout reconfiguration is handled in onCreate now
        Log.d(TAG, "Camera size updated to: ${cameraSize.width}x${cameraSize.height}")
    }
}