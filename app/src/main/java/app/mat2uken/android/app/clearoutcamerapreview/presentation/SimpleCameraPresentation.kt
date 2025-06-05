package app.mat2uken.android.app.clearoutcamerapreview.presentation

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.camera.view.PreviewView
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraRotationHelper

private const val TAG = "SimpleCameraPresentation"

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
        try {
            super.onCreate(savedInstanceState)
            // Make fullscreen
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window?.decorView?.windowInsetsController?.let { controller ->
                        controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                        controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    window?.decorView?.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set fullscreen mode", e)
            }
            
            // Get display metrics and info
            val displayMetrics = android.util.DisplayMetrics()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    val bounds = windowManager?.currentWindowMetrics?.bounds
                    if (bounds != null) {
                        displayMetrics.widthPixels = bounds.width()
                        displayMetrics.heightPixels = bounds.height()
                    } else {
                        // Fallback to deprecated method
                        @Suppress("DEPRECATION")
                        display.getRealMetrics(displayMetrics)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting display metrics on API 30+", e)
                    // Fallback to deprecated method
                    @Suppress("DEPRECATION")
                    display.getRealMetrics(displayMetrics)
                }
            } else {
                @Suppress("DEPRECATION")
                display.getRealMetrics(displayMetrics)
            }
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
                // Use default implementation mode for compatibility
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
            // Try to dismiss if we encounter an error
            try {
                dismiss()
            } catch (dismissError: Exception) {
                Log.e(TAG, "Error dismissing presentation after creation failure", dismissError)
            }
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
            displayRotation
        )
    }
}