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
     * 
     * @param deviceRotation The device's current rotation
     * @return The target rotation value
     */
    fun getTargetRotation(deviceRotation: Int): Int {
        // Use the device rotation directly for both cameras
        // CameraX handles the sensor orientation differences internally
        return deviceRotation
    }
    
    /**
     * Calculate the target rotation for the preview
     * @param deviceRotation The device's current rotation
     * @param isFrontCamera Whether the front camera is being used
     * @return The target rotation value
     * @deprecated Use getTargetRotation(deviceRotation) instead. Camera type no longer affects rotation.
     */
    @Deprecated("Camera type no longer affects rotation", ReplaceWith("getTargetRotation(deviceRotation)"))
    fun getTargetRotation(deviceRotation: Int, isFrontCamera: Boolean): Int {
        return getTargetRotation(deviceRotation)
    }
    
    /**
     * Calculate rotation compensation for external display
     * 
     * Current implementation returns a fixed 180 degree rotation for all cases
     * to ensure proper display on external monitors.
     * 
     * @param deviceRotation The device's current rotation
     * @param displayRotation The external display's rotation
     * @return The rotation compensation in degrees (always 180f)
     */
    fun getRotationCompensation(
        deviceRotation: Int,
        displayRotation: Int
    ): Float {
        // Both cameras need 180 degree rotation for external display
        // This ensures the image appears correctly on external displays
        // The device and display rotations are currently not used in the calculation
        // but are kept as parameters for potential future enhancements
        return 180f
    }
    
    /**
     * Calculate rotation compensation for external display
     * @param deviceRotation The device's current rotation
     * @param displayRotation The external display's rotation
     * @param isFrontCamera Whether the front camera is being used
     * @return The rotation compensation in degrees
     * @deprecated Use getRotationCompensation(deviceRotation, displayRotation) instead. Camera type no longer affects rotation.
     */
    @Deprecated("Camera type no longer affects rotation", ReplaceWith("getRotationCompensation(deviceRotation, displayRotation)"))
    fun getRotationCompensation(
        deviceRotation: Int,
        displayRotation: Int,
        isFrontCamera: Boolean
    ): Float {
        return getRotationCompensation(deviceRotation, displayRotation)
    }
}