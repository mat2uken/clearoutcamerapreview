package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.hardware.display.DisplayManager
import android.view.Display
import android.util.DisplayMetrics
import android.content.Context
import android.view.WindowManager
import android.os.Build

/**
 * Data class to hold comprehensive display information
 */
data class DisplayInfo(
    val displayId: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val densityDpi: Int,
    val isDefaultDisplay: Boolean
) {
    fun getDisplaySizeString(): String = "${width}x${height}"
    fun getDisplayIdString(): String = displayId.toString()
    fun getDisplayDescription(): String = "$name (ID: $displayId)"
}

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
     * Gets all external displays from the list of displays
     */
    fun findAllExternalDisplays(displays: Array<Display>): List<Display> {
        return displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
    }
    
    /**
     * Checks if a display is an external display
     */
    fun isExternalDisplay(display: Display): Boolean {
        return display.displayId != Display.DEFAULT_DISPLAY
    }
    
    /**
     * Gets comprehensive information about a display
     */
    fun getDisplayInfo(display: Display, context: Context? = null): DisplayInfo {
        val displayMetrics = DisplayMetrics()
        
        // Get display metrics using the best available method
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context != null) {
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val bounds = windowManager?.currentWindowMetrics?.bounds
                if (bounds != null) {
                    displayMetrics.widthPixels = bounds.width()
                    displayMetrics.heightPixels = bounds.height()
                    displayMetrics.densityDpi = context.resources.displayMetrics.densityDpi
                } else {
                    @Suppress("DEPRECATION")
                    display.getRealMetrics(displayMetrics)
                }
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                display.getRealMetrics(displayMetrics)
            }
        } else {
            @Suppress("DEPRECATION")
            display.getRealMetrics(displayMetrics)
        }
        
        return DisplayInfo(
            displayId = display.displayId,
            name = display.name ?: "Display ${display.displayId}",
            width = displayMetrics.widthPixels,
            height = displayMetrics.heightPixels,
            rotation = display.rotation,
            densityDpi = displayMetrics.densityDpi,
            isDefaultDisplay = display.displayId == Display.DEFAULT_DISPLAY
        )
    }
    
    /**
     * Gets information about all displays
     */
    fun getAllDisplayInfo(displayManager: DisplayManager, context: Context? = null): List<DisplayInfo> {
        val displays = displayManager.displays
        return displays.map { display -> getDisplayInfo(display, context) }
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
    
    /**
     * Gets a user-friendly display name
     */
    fun getDisplayName(display: Display): String {
        return display.name ?: "Display ${display.displayId}"
    }
    
    /**
     * Gets a concise display identifier string
     */
    fun getDisplayIdentifier(display: Display): String {
        val name = getDisplayName(display)
        return "$name (ID: ${display.displayId})"
    }
}