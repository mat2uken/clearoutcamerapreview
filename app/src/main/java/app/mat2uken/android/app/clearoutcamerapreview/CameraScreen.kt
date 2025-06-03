package app.mat2uken.android.app.clearoutcamerapreview

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = cameraSelector,
            onCameraInitialized = { cameraInstance ->
                camera = cameraInstance
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
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector,
    onCameraInitialized: (androidx.camera.core.Camera) -> Unit
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
                
                onCameraInitialized(camera)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSelectorDropdown(
    currentSelector: CameraSelector,
    onSelectorChanged: (CameraSelector) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "Back Camera" to CameraSelector.DEFAULT_BACK_CAMERA,
        "Front Camera" to CameraSelector.DEFAULT_FRONT_CAMERA
    )
    val currentOption = options.find { it.second == currentSelector }?.first ?: "Back Camera"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = currentOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            colors = TextFieldDefaults.colors()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, selector) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelectorChanged(selector)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ZoomSlider(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Zoom: ${String.format("%.1fx", zoomRatio)}",
                style = MaterialTheme.typography.labelMedium
            )
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
                    text = String.format("%.1fx", minZoomRatio),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = String.format("%.1fx", maxZoomRatio),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}