package app.mat2uken.android.app.clearoutcamerapreview

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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

@Composable
fun MultiDisplayCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    
    // External display management
    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    var externalDisplay by remember { mutableStateOf<Display?>(null) }
    var externalPresentation by remember { mutableStateOf<CameraPresentation?>(null) }
    
    // Monitor display changes
    DisposableEffect(displayManager) {
        fun updateExternalDisplay() {
            val displays = displayManager.displays
            externalDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        }
        
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                updateExternalDisplay()
            }
            
            override fun onDisplayRemoved(displayId: Int) {
                if (externalPresentation?.display?.displayId == displayId) {
                    externalPresentation?.dismiss()
                    externalPresentation = null
                }
                updateExternalDisplay()
            }
            
            override fun onDisplayChanged(displayId: Int) {
                updateExternalDisplay()
            }
        }
        
        displayManager.registerDisplayListener(displayListener, null)
        updateExternalDisplay()
        
        onDispose {
            displayManager.unregisterDisplayListener(displayListener)
            externalPresentation?.dismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main camera preview
        CameraPreviewComposable(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = cameraSelector,
            externalDisplay = externalDisplay,
            onCameraInitialized = { cameraInstance ->
                camera = cameraInstance
                val zoomState = cameraInstance.cameraInfo.zoomState.value
                zoomState?.let {
                    minZoomRatio = it.minZoomRatio
                    maxZoomRatio = it.maxZoomRatio
                    zoomRatio = it.zoomRatio
                }
            },
            onExternalPresentationCreated = { presentation ->
                externalPresentation?.dismiss()
                externalPresentation = presentation
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // External display status
            ExternalDisplayStatusCard(externalDisplay != null)
            
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
                            cam.cameraControl.setZoomRatio(newZoom)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPreviewComposable(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector,
    externalDisplay: Display?,
    onCameraInitialized: (androidx.camera.core.Camera) -> Unit,
    onExternalPresentationCreated: (CameraPresentation?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(cameraSelector, externalDisplay) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            try {
                cameraProvider.unbindAll()
                
                val preview = Preview.Builder().build()
                
                // Set up preview for main display
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                // Set up external display if connected
                if (externalDisplay != null) {
                    try {
                        val presentation = CameraPresentation(context, externalDisplay) { externalPreviewView ->
                            // Create a second preview for external display
                            val externalPreview = Preview.Builder().build()
                            externalPreview.setSurfaceProvider(externalPreviewView.surfaceProvider)
                            
                            // Bind both previews
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                externalPreview
                            )
                            onCameraInitialized(camera)
                        }
                        presentation.show()
                        onExternalPresentationCreated(presentation)
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Failed to create external display", e)
                        // Fall back to single display
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        onCameraInitialized(camera)
                    }
                } else {
                    // Single display setup
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    onCameraInitialized(camera)
                    onExternalPresentationCreated(null)
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProviderFuture.get()?.unbindAll()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun ExternalDisplayStatusCard(isConnected: Boolean) {
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Display status",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "External Display Connected" else "No External Display",
                    style = MaterialTheme.typography.bodyMedium
                )
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