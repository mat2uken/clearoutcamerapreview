package app.mat2uken.android.app.clearoutcamerapreview

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mat2uken.android.app.clearoutcamerapreview.audio.AudioCoordinator
import app.mat2uken.android.app.clearoutcamerapreview.ui.theme.ClearoutcamerapreviewTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var audioCoordinator: AudioCoordinator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow both landscape orientations (normal and reverse)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        // Initialize audio coordinator
        audioCoordinator = AudioCoordinator(this)
        
        enableEdgeToEdge()
        setContent {
            ClearoutcamerapreviewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraPermissionScreen(audioCoordinator)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up audio coordinator
        audioCoordinator.release()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionScreen(audioCoordinator: AudioCoordinator) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // Update audio coordinator permission state when permissions change
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            audioCoordinator.updatePermissionState()
            audioCoordinator.start()
        }
    }
    
    // Clean up audio coordinator when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            audioCoordinator.stop()
        }
    }
    
    if (permissionsState.allPermissionsGranted) {
        SimplifiedMultiDisplayCameraScreen(audioCoordinator)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = buildString {
                        append("This app needs the following permissions:\n\n")
                        permissionsState.permissions.forEach { permission ->
                            when (permission.permission) {
                                Manifest.permission.CAMERA -> {
                                    append("• Camera: To show camera preview\n")
                                }
                                Manifest.permission.RECORD_AUDIO -> {
                                    append("• Microphone: To capture audio when external speakers are connected\n")
                                }
                            }
                        }
                    },
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() }
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}