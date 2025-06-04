package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.util.Log
import android.util.Range
import app.mat2uken.android.app.clearoutcamerapreview.model.CameraFormat
import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import kotlin.math.abs

/**
 * Utility functions for camera operations
 */
object CameraUtils {
    
    private const val TAG = "CameraUtils"
    
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
     * Selects the optimal camera format (resolution + frame rate) based on CLAUDE.md specifications:
     * 1. Resolution:
     *    - 1920x1080 if available
     *    - Otherwise, 16:9 aspect ratio closest to and <= 1920x1080
     * 2. Frame rate:
     *    - 60fps if available for the selected resolution
     *    - Otherwise, highest frame rate <= 60fps
     * 
     * @return Pair of selected format and optimal frame rate range, or null if no suitable format found
     */
    fun selectOptimalCameraFormat(availableFormats: List<CameraFormat>): Pair<CameraFormat, Range<Int>>? {
        if (availableFormats.isEmpty()) return null
        
        // First, try to find 1920x1080
        val fullHdFormat = availableFormats.find { 
            it.size.width == 1920 && it.size.height == 1080 
        }
        
        if (fullHdFormat != null) {
            val frameRateRange = selectOptimalFrameRateRange(fullHdFormat)
            if (frameRateRange != null) {
                Log.d(TAG, "Selected Full HD format with frame rate range: ${frameRateRange.lower}-${frameRateRange.upper} fps")
                return Pair(fullHdFormat, frameRateRange)
            }
        }
        
        // If Full HD not available or doesn't support good frame rates,
        // find the best 16:9 resolution <= 1920x1080
        val targetRatio = 16.0 / 9.0
        
        val candidateFormats = availableFormats
            .filter { format ->
                format.size.width > 0 && 
                format.size.height > 0 && 
                format.size.width <= 1920 && 
                format.size.height <= 1080 &&
                format.frameRateRanges.isNotEmpty()
            }
            .mapNotNull { format ->
                val ratio = format.size.width.toDouble() / format.size.height.toDouble()
                val ratioDiff = abs(ratio - targetRatio)
                val frameRateRange = selectOptimalFrameRateRange(format)
                
                if (frameRateRange != null) {
                    Triple(format, frameRateRange, ratioDiff)
                } else {
                    null
                }
            }
            .sortedWith(compareBy(
                { it.third }, // Sort by aspect ratio difference (smaller is better)
                { -(it.first.size.width * it.first.size.height) } // Then by resolution (larger is better)
            ))
        
        return candidateFormats.firstOrNull()?.let { (format, range, _) ->
            Log.d(TAG, "Selected format: ${format.size.width}x${format.size.height} with frame rate range: ${range.lower}-${range.upper} fps")
            Pair(format, range)
        }
    }
    
    /**
     * Selects the optimal frame rate range for a given camera format
     * Prefers ranges that include 60fps, otherwise selects the highest available <= 60fps
     */
    private fun selectOptimalFrameRateRange(format: CameraFormat): Range<Int>? {
        if (format.frameRateRanges.isEmpty()) return null
        
        // First, try to find a range that includes 60fps
        format.frameRateRanges.find { range ->
            60 >= range.lower && 60 <= range.upper
        }?.let { return it }
        
        // Otherwise, find the range with the highest upper bound <= 60
        val validRanges = format.frameRateRanges.filter { it.upper <= 60 }
        if (validRanges.isNotEmpty()) {
            return validRanges.maxByOrNull { it.upper }
        }
        
        // If no ranges <= 60fps, return null (this format is not suitable)
        return null
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
        // Handle inverted bounds case
        val actualMin = minOf(min, max)
        val actualMax = maxOf(min, max)
        return value.coerceIn(actualMin, actualMax)
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