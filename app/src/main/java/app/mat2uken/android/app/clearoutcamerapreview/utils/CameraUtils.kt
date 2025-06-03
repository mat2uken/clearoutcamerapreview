package app.mat2uken.android.app.clearoutcamerapreview.utils

import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import kotlin.math.abs

/**
 * Utility functions for camera operations
 */
object CameraUtils {
    
    /**
     * Finds the best preview resolution based on the following criteria:
     * 1. Prefers 1920x1080 if available
     * 2. Otherwise, selects the resolution closest to 16:9 aspect ratio
     */
    fun selectOptimalResolution(availableSizes: List<Size>): Size? {
        if (availableSizes.isEmpty()) return null
        
        // First, check if 1920x1080 is available
        val fullHd = availableSizes.find { it.width == 1920 && it.height == 1080 }
        if (fullHd != null) {
            return fullHd
        }
        
        // Target aspect ratio is 16:9 (1.777...)
        val targetRatio = 16.0 / 9.0
        
        // Sort by how close the aspect ratio is to 16:9, then by resolution (higher is better)
        return availableSizes
            .filter { it.width > 0 && it.height > 0 }
            .map { size ->
                val ratio = size.width.toDouble() / size.height.toDouble()
                val ratioDiff = abs(ratio - targetRatio)
                size to ratioDiff
            }
            .sortedWith(compareBy(
                { it.second }, // Sort by aspect ratio difference (smaller is better)
                { -(it.first.width * it.first.height) } // Then by resolution (larger is better)
            ))
            .firstOrNull()?.first
    }
    
    /**
     * Formats zoom ratio for display
     */
    fun formatZoomRatio(zoomRatio: Float): String {
        return String.format("%.1fx", zoomRatio)
    }
    
    /**
     * Clamps zoom value between min and max
     */
    fun clampZoom(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
    
    /**
     * Calculates the camera aspect ratio
     */
    fun calculateAspectRatio(width: Int, height: Int): Float {
        require(width > 0 && height > 0) { "Width and height must be positive" }
        return width.toFloat() / height.toFloat()
    }
    
    /**
     * Determines if a display is in portrait orientation
     */
    fun isDisplayPortrait(width: Int, height: Int): Boolean {
        return height > width
    }
    
    /**
     * Calculates optimal preview dimensions for a given display
     */
    fun calculateOptimalPreviewSize(
        displayWidth: Int,
        displayHeight: Int,
        cameraAspectRatio: Float,
        isPortrait: Boolean
    ): Pair<Int, Int> {
        return if (isPortrait) {
            // For portrait display showing landscape camera
            val rotatedCameraRatio = 1f / cameraAspectRatio  // 9:16
            
            if (displayWidth.toFloat() / displayHeight > rotatedCameraRatio) {
                // Display is wider relative to rotated camera
                val height = displayHeight
                val width = (height * rotatedCameraRatio).toInt()
                Pair(width, height)
            } else {
                // Display is taller relative to rotated camera
                val width = displayWidth
                val height = (width / rotatedCameraRatio).toInt()
                Pair(width, height)
            }
        } else {
            // For landscape display showing landscape camera
            if (displayWidth.toFloat() / displayHeight > cameraAspectRatio) {
                // Display is wider than camera
                val height = displayHeight
                val width = (height * cameraAspectRatio).toInt()
                Pair(width, height)
            } else {
                // Display is taller than camera
                val width = displayWidth
                val height = (width / cameraAspectRatio).toInt()
                Pair(width, height)
            }
        }
    }
}