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
     * Both front and back cameras should use the same rotation
     */
    fun getTargetRotation(deviceRotation: Int, isFrontCamera: Boolean): Int {
        // Use the device rotation directly for both cameras
        // CameraX handles the sensor orientation differences internally
        return deviceRotation
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
        
        // Both cameras need 180 degree rotation for external display
        // This ensures the image appears correctly on external displays
        return 180f
    }
}