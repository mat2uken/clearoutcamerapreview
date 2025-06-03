package app.mat2uken.android.app.clearoutcamerapreview.camera

import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import androidx.camera.core.CameraSelector

/**
 * Represents the state of the camera
 */
data class CameraState(
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val selectedResolution: Size? = null,
    val actualPreviewSize: Size? = null,
    val isExternalDisplayConnected: Boolean = false,
    val externalDisplayId: Int? = null
) {
    /**
     * Updates zoom ratio with clamping
     */
    fun updateZoomRatio(newRatio: Float): CameraState {
        val clampedRatio = newRatio.coerceIn(minZoomRatio, maxZoomRatio)
        return copy(zoomRatio = clampedRatio)
    }
    
    /**
     * Updates zoom bounds
     */
    fun updateZoomBounds(min: Float, max: Float): CameraState {
        require(min <= max) { "Min zoom must be less than or equal to max zoom" }
        return copy(
            minZoomRatio = min,
            maxZoomRatio = max,
            zoomRatio = zoomRatio.coerceIn(min, max)
        )
    }
    
    /**
     * Toggles camera between front and back
     */
    fun toggleCamera(): CameraState {
        val newSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        return copy(cameraSelector = newSelector)
    }
    
    /**
     * Updates external display connection state
     */
    fun updateExternalDisplay(connected: Boolean, displayId: Int? = null): CameraState {
        return copy(
            isExternalDisplayConnected = connected,
            externalDisplayId = if (connected) displayId else null
        )
    }
}