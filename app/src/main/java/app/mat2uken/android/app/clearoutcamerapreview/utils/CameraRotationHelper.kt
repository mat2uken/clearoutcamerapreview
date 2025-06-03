package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.view.Surface
import androidx.camera.core.CameraSelector

/**
 * Helper class for camera rotation calculations
 */
object CameraRotationHelper {
    
    /**
     * Check if the camera is front facing
     */
    fun isFrontCamera(cameraSelector: CameraSelector): Boolean {
        return cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }
    
    /**
     * Calculate the target rotation for the preview
     * Front camera needs different handling than back camera
     */
    fun getTargetRotation(deviceRotation: Int, isFrontCamera: Boolean): Int {
        return if (isFrontCamera) {
            // Front camera sensor is typically oriented differently
            // We need to compensate for the sensor orientation
            when (deviceRotation) {
                Surface.ROTATION_0 -> Surface.ROTATION_0
                Surface.ROTATION_90 -> Surface.ROTATION_270  // Swap for front camera
                Surface.ROTATION_180 -> Surface.ROTATION_180
                Surface.ROTATION_270 -> Surface.ROTATION_90  // Swap for front camera
                else -> Surface.ROTATION_0
            }
        } else {
            // Back camera uses the device rotation directly without modification
            // This was working correctly before
            deviceRotation
        }
    }
    
    /**
     * Calculate rotation compensation for external display
     * @param deviceRotation The device's current rotation
     * @param displayRotation The external display's rotation
     * @param isFrontCamera Whether the front camera is being used
     * @return The rotation compensation in degrees
     */
    fun getRotationCompensation(
        deviceRotation: Int,
        displayRotation: Int,
        isFrontCamera: Boolean
    ): Float {
        // Calculate base rotation compensation
        val baseRotation = when (deviceRotation) {
            Surface.ROTATION_0 -> when (displayRotation) {
                Surface.ROTATION_0 -> 0f
                Surface.ROTATION_90 -> 270f
                Surface.ROTATION_180 -> 180f
                Surface.ROTATION_270 -> 90f
                else -> 0f
            }
            Surface.ROTATION_90 -> when (displayRotation) {
                Surface.ROTATION_0 -> 90f
                Surface.ROTATION_90 -> 0f
                Surface.ROTATION_180 -> 270f
                Surface.ROTATION_270 -> 180f
                else -> 0f
            }
            Surface.ROTATION_180 -> when (displayRotation) {
                Surface.ROTATION_0 -> 180f
                Surface.ROTATION_90 -> 90f
                Surface.ROTATION_180 -> 0f
                Surface.ROTATION_270 -> 270f
                else -> 0f
            }
            Surface.ROTATION_270 -> when (displayRotation) {
                Surface.ROTATION_0 -> 270f
                Surface.ROTATION_90 -> 180f
                Surface.ROTATION_180 -> 90f
                Surface.ROTATION_270 -> 0f
                else -> 0f
            }
            else -> 0f
        }
        
        // For front camera, we need to compensate for the different sensor orientation
        return if (isFrontCamera) {
            // Front camera needs 270 degree compensation to fix the 90-degree clockwise rotation
            (baseRotation + 270f) % 360f
        } else {
            // Back camera originally used 180 degree fixed rotation for external display
            180f
        }
    }
}