package app.mat2uken.android.app.clearoutcamerapreview

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages external display detection and camera preview presentation
 */
class ExternalDisplayManager(private val context: Context) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var externalPresentation: CameraPresentation? = null
    
    private val _externalDisplayState = MutableStateFlow<ExternalDisplayState>(ExternalDisplayState.NotConnected)
    val externalDisplayState: StateFlow<ExternalDisplayState> = _externalDisplayState.asStateFlow()
    
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.d(TAG, "Display added: $displayId")
            updateExternalDisplay()
        }
        
        override fun onDisplayRemoved(displayId: Int) {
            Log.d(TAG, "Display removed: $displayId")
            if (externalPresentation?.display?.displayId == displayId) {
                dismissPresentation()
            }
            updateExternalDisplay()
        }
        
        override fun onDisplayChanged(displayId: Int) {
            Log.d(TAG, "Display changed: $displayId")
            updateExternalDisplay()
        }
    }
    
    init {
        displayManager.registerDisplayListener(displayListener, null)
        updateExternalDisplay()
    }
    
    private fun updateExternalDisplay() {
        val displays = displayManager.displays
        val externalDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        
        if (externalDisplay != null) {
            _externalDisplayState.value = ExternalDisplayState.Connected(externalDisplay)
        } else {
            _externalDisplayState.value = ExternalDisplayState.NotConnected
        }
    }
    
    fun showCameraOnExternalDisplay(
        display: Display,
        previewProvider: (PreviewView) -> Unit
    ) {
        dismissPresentation()
        
        try {
            externalPresentation = CameraPresentation(context, display, previewProvider).apply {
                show()
            }
            Log.d(TAG, "External display presentation shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show external display presentation", e)
            _externalDisplayState.value = ExternalDisplayState.Error(e.message ?: "Unknown error")
        }
    }
    
    fun dismissPresentation() {
        externalPresentation?.dismiss()
        externalPresentation = null
    }
    
    fun release() {
        dismissPresentation()
        displayManager.unregisterDisplayListener(displayListener)
    }
    
    companion object {
        private const val TAG = "ExternalDisplayManager"
    }
}

/**
 * Represents the state of external display connection
 */
sealed class ExternalDisplayState {
    object NotConnected : ExternalDisplayState()
    data class Connected(val display: Display) : ExternalDisplayState()
    data class Error(val message: String) : ExternalDisplayState()
}

/**
 * Presentation for showing camera preview on external display
 */
class CameraPresentation(
    context: Context,
    display: Display,
    private val previewProvider: (PreviewView) -> Unit
) : Presentation(context, display), LifecycleOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private lateinit var previewView: PreviewView
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Make the presentation fullscreen
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            
            // Create PreviewView for external display
            previewView = PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
            
            setContentView(previewView)
            
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        } catch (e: Exception) {
            Log.e("CameraPresentation", "Error in onCreate", e)
        }
    }
    
    override fun onStart() {
        super.onStart()
        try {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            
            // Provide the preview view to the camera
            previewProvider(previewView)
        } catch (e: Exception) {
            Log.e("CameraPresentation", "Error in onStart", e)
        }
    }
    
    override fun onStop() {
        try {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        } catch (e: Exception) {
            Log.e("CameraPresentation", "Error in onStop", e)
        }
        super.onStop()
    }
    
    override fun dismiss() {
        try {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        } catch (e: Exception) {
            Log.e("CameraPresentation", "Error in dismiss", e)
        }
        super.dismiss()
    }
}