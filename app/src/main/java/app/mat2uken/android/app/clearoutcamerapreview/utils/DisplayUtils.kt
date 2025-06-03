package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Utility functions for display operations
 */
object DisplayUtils {
    
    /**
     * Finds the first external display from the list of displays
     */
    fun findExternalDisplay(displays: Array<Display>): Display? {
        return displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
    }
    
    /**
     * Checks if a display is an external display
     */
    fun isExternalDisplay(display: Display): Boolean {
        return display.displayId != Display.DEFAULT_DISPLAY
    }
    
    /**
     * Gets display aspect ratio
     */
    fun getDisplayAspectRatio(width: Int, height: Int): Float {
        require(width > 0 && height > 0) { "Width and height must be positive" }
        return width.toFloat() / height.toFloat()
    }
    
    /**
     * Determines the required rotation based on display orientation
     */
    fun calculateRotationDegrees(isDisplayPortrait: Boolean, displayRotation: Int): Float {
        return when {
            isDisplayPortrait && displayRotation == android.view.Surface.ROTATION_0 -> 180f
            isDisplayPortrait && displayRotation == android.view.Surface.ROTATION_180 -> 180f
            else -> 180f // Default rotation to fix upside-down issue
        }
    }
    
    /**
     * Formats display info for logging
     */
    fun formatDisplayInfo(displayId: Int, width: Int, height: Int, rotation: Int): String {
        return "Display $displayId: ${width}x${height}, rotation: $rotation"
    }
}