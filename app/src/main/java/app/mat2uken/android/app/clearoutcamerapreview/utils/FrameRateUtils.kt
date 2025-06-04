package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera

/**
 * Utility functions for frame rate detection and selection
 */
object FrameRateUtils {
    private const val TAG = "FrameRateUtils"
    
    /**
     * Detects the actual frame rate range for a camera when not explicitly set
     * Prioritizes 60fps for 1920x1080 resolution, otherwise 30fps
     */
    @ExperimentalCamera2Interop
    fun detectActualFrameRate(
        camera: Camera,
        context: Context,
        actualPreviewSize: Size?,
        selectedResolution: Size?
    ): Range<Int>? {
        return try {
            val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val cameraId = camera2Info.cameraId
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val fpsRanges = characteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )
            
            if (!fpsRanges.isNullOrEmpty()) {
                val previewSize = actualPreviewSize ?: selectedResolution
                
                // For 1920x1080, prefer 60fps if available, otherwise 30fps
                val targetFps = if (previewSize?.width == 1920 && previewSize.height == 1080) {
                    // Try to find exact 60fps range first (60-60)
                    fpsRanges.find { range ->
                        range.upper == 60 && range.lower == 60
                    } ?: fpsRanges.find { range ->
                        // Then try any range that includes 60fps
                        range.upper == 60 && range.lower <= 60
                    } ?: fpsRanges.find { range ->
                        // Finally fallback to 30fps
                        range.upper == 30 || (range.lower <= 30 && range.upper >= 30)
                    }
                } else {
                    // For other resolutions, prefer 30fps
                    fpsRanges.find { range ->
                        range.upper == 30 || (range.lower <= 30 && range.upper >= 30)
                    }
                } ?: fpsRanges[0]
                
                val frameRateRange = Range(targetFps.lower, targetFps.upper)
                Log.d(TAG, "Detected frame rate range: ${targetFps.lower}-${targetFps.upper} fps for resolution ${previewSize?.width}x${previewSize?.height}")
                frameRateRange
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get camera frame rate info", e)
            null
        }
    }
}