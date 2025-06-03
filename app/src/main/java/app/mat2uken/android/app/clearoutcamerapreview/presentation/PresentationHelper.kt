package app.mat2uken.android.app.clearoutcamerapreview.presentation

import android.util.DisplayMetrics
import android.view.Display
import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import app.mat2uken.android.app.clearoutcamerapreview.utils.CameraUtils
import app.mat2uken.android.app.clearoutcamerapreview.utils.DisplayUtils

/**
 * Helper class for presentation logic
 */
class PresentationHelper {
    
    /**
     * Calculates preview dimensions and rotation for external display
     */
    fun calculatePreviewConfiguration(
        displayMetrics: DisplayMetrics,
        cameraPreviewSize: Size? = null
    ): PreviewConfiguration {
        val displayWidth = displayMetrics.widthPixels
        val displayHeight = displayMetrics.heightPixels
        val isDisplayPortrait = CameraUtils.isDisplayPortrait(displayWidth, displayHeight)
        
        // Camera preview is typically 16:9 in landscape orientation
        val cameraAspectRatio = cameraPreviewSize?.let { 
            CameraUtils.calculateAspectRatio(it.width, it.height)
        } ?: (16f / 9f)
        
        val (optimalWidth, optimalHeight) = CameraUtils.calculateOptimalPreviewSize(
            displayWidth, displayHeight, cameraAspectRatio, isDisplayPortrait
        )
        
        // Always apply 180 degree rotation to fix upside-down issue
        val rotation = 180f
        
        return PreviewConfiguration(
            width = optimalWidth,
            height = optimalHeight,
            rotation = rotation,
            isPortrait = isDisplayPortrait
        )
    }
    
    /**
     * Data class for preview configuration
     */
    data class PreviewConfiguration(
        val width: Int,
        val height: Int,
        val rotation: Float,
        val isPortrait: Boolean
    )
}