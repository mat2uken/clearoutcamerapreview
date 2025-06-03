package app.mat2uken.android.app.clearoutcamerapreview

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun CameraScreenWithExternalDisplay() {
    val context = LocalContext.current
    val externalDisplayManager = remember { ExternalDisplayManager(context) }
    val externalDisplayState by externalDisplayManager.externalDisplayState.collectAsState()
    
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Store external display state for later use
    var externalDisplay by remember { mutableStateOf<android.view.Display?>(null) }
    
    LaunchedEffect(externalDisplayState) {
        when (val state = externalDisplayState) {
            is ExternalDisplayState.Connected -> {
                externalDisplay = state.display
            }
            else -> {
                externalDisplay = null
                externalDisplayManager.dismissPresentation()
            }
        }
    }
    
    DisposableEffect(externalDisplayManager) {
        onDispose {
            externalDisplayManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main camera preview
        CameraPreviewWithExternalSupport(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = cameraSelector,
            onCameraInitialized = { cameraInstance, previewInstance ->
                camera = cameraInstance
                preview = previewInstance
                val zoomState = cameraInstance.cameraInfo.zoomState.value
                zoomState?.let {
                    minZoomRatio = it.minZoomRatio
                    maxZoomRatio = it.maxZoomRatio
                    zoomRatio = it.zoomRatio
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // External display status
            ExternalDisplayStatus(externalDisplayState)
            
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
fun CameraPreviewWithExternalSupport(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector,
    onCameraInitialized: (androidx.camera.core.Camera, Preview) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            try {
                cameraProvider.unbindAll()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                
                onCameraInitialized(camera, preview)
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
fun ExternalDisplayStatus(displayState: ExternalDisplayState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (displayState) {
                is ExternalDisplayState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is ExternalDisplayState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
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
                    text = when (displayState) {
                        is ExternalDisplayState.Connected -> "External Display Connected"
                        is ExternalDisplayState.Error -> "Display Error: ${displayState.message}"
                        else -> "No External Display"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (displayState is ExternalDisplayState.Connected) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("LIVE", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}