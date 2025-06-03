package app.mat2uken.android.app.clearoutcamerapreview

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RobustMultiDisplay"

@Composable
fun RobustMultiDisplayCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // Camera state
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    
    // Display management state
    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    var externalDisplay by remember { mutableStateOf<Display?>(null) }
    var externalPresentation by remember { mutableStateOf<CameraPresentation?>(null) }
    var isExternalDisplayConnected by remember { mutableStateOf(false) }
    
    // Camera provider state
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var mainPreview by remember { mutableStateOf<Preview?>(null) }
    
    // Declare function variables first
    lateinit var setupCamera: () -> Unit
    lateinit var setupExternalDisplay: (ProcessCameraProvider) -> Unit
    
    // Function to update external display status
    fun updateExternalDisplayStatus() {
        val displays = displayManager.displays
        val newExternalDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        
        if (newExternalDisplay != null && !isExternalDisplayConnected) {
            // External display connected
            Log.d(TAG, "External display connected: ${newExternalDisplay.displayId}")
            externalDisplay = newExternalDisplay
            isExternalDisplayConnected = true
        } else if (newExternalDisplay == null && isExternalDisplayConnected) {
            // External display disconnected
            Log.d(TAG, "External display disconnected")
            externalDisplay = null
            isExternalDisplayConnected = false
            
            // Clean up presentation
            try {
                externalPresentation?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing presentation", e)
            }
            externalPresentation = null
        }
    }
    
    
    // Monitor display changes
    DisposableEffect(displayManager) {
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                Log.d(TAG, "Display added: $displayId")
                updateExternalDisplayStatus()
                if (isExternalDisplayConnected && cameraProvider != null) {
                    setupCamera()
                }
            }
            
            override fun onDisplayRemoved(displayId: Int) {
                Log.d(TAG, "Display removed: $displayId")
                updateExternalDisplayStatus()
                if (!isExternalDisplayConnected && cameraProvider != null) {
                    setupCamera()
                }
            }
            
            override fun onDisplayChanged(displayId: Int) {
                Log.d(TAG, "Display changed: $displayId")
                updateExternalDisplayStatus()
            }
        }
        
        displayManager.registerDisplayListener(displayListener, null)
        updateExternalDisplayStatus()
        
        onDispose {
            displayManager.unregisterDisplayListener(displayListener)
            try {
                externalPresentation?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up presentation", e)
            }
            externalPresentation = null
        }
    }
    
    // Initialize camera setup functions
    setupExternalDisplay = setupExternalDisplay@ { provider ->
        val display = externalDisplay ?: return@setupExternalDisplay
        
        try {
            // Dismiss any existing presentation
            externalPresentation?.dismiss()
            externalPresentation = null
            
            // Small delay to ensure cleanup
            coroutineScope.launch {
                delay(100)
                
                try {
                    // Unbind all and rebind with dual preview
                    provider.unbindAll()
                    
                    // Create previews for both displays
                    val mainPreviewForDual = Preview.Builder().build()
                    val externalPreview = Preview.Builder().build()
                    
                    // Get main preview view
                    val mainPreviewView = (mainPreview as? Preview)?.let { preview ->
                        // Find the preview view (this is a simplification)
                        preview
                    } ?: return@launch
                    
                    // Set surface provider for main preview
                    mainPreviewForDual.setSurfaceProvider { surface ->
                        mainPreviewView.setSurfaceProvider { surface }
                    }
                    
                    // Create and show presentation
                    val presentation = CameraPresentation(context, display) { externalPreviewView ->
                        externalPreview.setSurfaceProvider(externalPreviewView.surfaceProvider)
                    }
                    
                    presentation.show()
                    externalPresentation = presentation
                    
                    // Bind both previews
                    val cam = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        mainPreviewForDual,
                        externalPreview
                    )
                    
                    camera = cam
                    
                    // Update zoom state
                    cam.cameraInfo.zoomState.value?.let {
                        minZoomRatio = it.minZoomRatio
                        maxZoomRatio = it.maxZoomRatio
                        zoomRatio = it.zoomRatio
                    }
                    
                    Log.d(TAG, "External display setup completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to setup external display", e)
                    // Fallback to single display
                    setupCamera()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up external display", e)
        }
    }
    
    setupCamera = setupCamera@ {
        val provider = cameraProvider ?: return@setupCamera
        
        coroutineScope.launch {
            try {
                // Unbind all use cases
                provider.unbindAll()
                
                // Create preview for main display
                val preview = Preview.Builder().build()
                mainPreview = preview
                
                // Always bind the main preview first
                val cam = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                
                camera = cam
                
                // Update zoom state
                cam.cameraInfo.zoomState.value?.let {
                    minZoomRatio = it.minZoomRatio
                    maxZoomRatio = it.maxZoomRatio
                    zoomRatio = it.zoomRatio
                }
                
                // Handle external display if connected
                if (isExternalDisplayConnected && externalDisplay != null) {
                    setupExternalDisplay(provider)
                }
                
                Log.d(TAG, "Camera setup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
            }
        }
    }
    
    // Camera lifecycle management
    DisposableEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupCamera()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get camera provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            try {
                cameraProvider?.unbindAll()
                externalPresentation?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main camera preview
        MainCameraPreview(
            modifier = Modifier.fillMaxSize(),
            onPreviewReady = { preview ->
                mainPreview = preview
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // External display status
            DisplayStatusCard(
                isConnected = isExternalDisplayConnected,
                displayInfo = externalDisplay?.let { "Display ${it.displayId}" }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Camera selector dropdown
            CameraSelectorDropdown(
                currentSelector = cameraSelector,
                onSelectorChanged = { newSelector ->
                    cameraSelector = newSelector
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Zoom slider
            camera?.let { cam ->
                ZoomSlider(
                    zoomRatio = zoomRatio,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    onZoomChanged = { newZoom ->
                        zoomRatio = newZoom
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

@Composable
fun MainCameraPreview(
    modifier: Modifier = Modifier,
    onPreviewReady: (Preview) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    
    DisposableEffect(previewView) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        onPreviewReady(preview)
        
        onDispose {
            // Cleanup if needed
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun DisplayStatusCard(
    isConnected: Boolean,
    displayInfo: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
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
                    modifier = Modifier.size(20.dp),
                    tint = if (isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isConnected) "External Display Connected" else "No External Display",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isConnected && displayInfo != null) {
                        Text(
                            text = displayInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (isConnected) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("LIVE", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}