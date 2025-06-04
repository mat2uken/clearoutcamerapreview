package app.mat2uken.android.app.clearoutcamerapreview.model

import android.util.Range

/**
 * Represents a camera format with resolution and supported frame rate ranges
 */
data class CameraFormat(
    val size: Size,
    val frameRateRanges: List<Range<Int>>
) {
    /**
     * Gets the maximum frame rate supported by this format
     */
    val maxFrameRate: Int
        get() = frameRateRanges.maxOfOrNull { it.upper } ?: 0
    
    /**
     * Checks if this format supports a specific frame rate
     */
    fun supportsFrameRate(fps: Int): Boolean {
        return frameRateRanges.any { range ->
            fps >= range.lower && fps <= range.upper
        }
    }
    
    /**
     * Gets the best frame rate range for a target frame rate
     * Prefers ranges that include the target fps, otherwise returns the closest range
     */
    fun getBestFrameRateRange(targetFps: Int): Range<Int>? {
        // First, try to find a range that contains the target fps
        frameRateRanges.find { range ->
            targetFps >= range.lower && targetFps <= range.upper
        }?.let { return it }
        
        // If not found, return the range with upper bound closest to target
        return frameRateRanges.minByOrNull { range ->
            kotlin.math.abs(range.upper - targetFps)
        }
    }
}