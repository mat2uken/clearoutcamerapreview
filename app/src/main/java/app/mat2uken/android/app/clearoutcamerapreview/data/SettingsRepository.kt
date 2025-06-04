package app.mat2uken.android.app.clearoutcamerapreview.data

import android.content.Context
import androidx.camera.core.CameraSelector
import app.mat2uken.android.app.clearoutcamerapreview.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app settings with Room database
 */
class SettingsRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.settingsDao()
    
    // Initialize default settings
    suspend fun initialize() {
        dao.initializeDefaultSettings()
    }
    
    // Display Settings
    suspend fun getDisplaySettings(displayId: String): DisplaySettings {
        return dao.getDisplaySettings(displayId) ?: DisplaySettings(displayId = displayId)
    }
    
    fun getDisplaySettingsFlow(displayId: String): Flow<DisplaySettings> {
        return dao.getDisplaySettingsFlow(displayId).map { 
            it ?: DisplaySettings(displayId = displayId)
        }
    }
    
    suspend fun updateDisplayFlipSettings(
        displayId: String, 
        displayName: String,
        isVerticallyFlipped: Boolean, 
        isHorizontallyFlipped: Boolean
    ) {
        dao.insertOrUpdateDisplaySettings(
            DisplaySettings(
                displayId = displayId,
                displayName = displayName,
                isVerticallyFlipped = isVerticallyFlipped,
                isHorizontallyFlipped = isHorizontallyFlipped
            )
        )
    }
    
    // Camera Settings
    suspend fun getCameraSettings(cameraSelector: CameraSelector): CameraSettings {
        val cameraId = getCameraId(cameraSelector)
        return dao.getCameraSettings(cameraId) ?: CameraSettings(cameraId = cameraId)
    }
    
    fun getCameraSettingsFlow(cameraSelector: CameraSelector): Flow<CameraSettings> {
        val cameraId = getCameraId(cameraSelector)
        return dao.getCameraSettingsFlow(cameraId).map { 
            it ?: CameraSettings(cameraId = cameraId)
        }
    }
    
    suspend fun updateCameraZoom(
        cameraSelector: CameraSelector, 
        zoomRatio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ) {
        val cameraId = getCameraId(cameraSelector)
        dao.insertOrUpdateCameraSettings(
            CameraSettings(
                cameraId = cameraId,
                zoomRatio = zoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )
        )
    }
    
    // App Settings
    suspend fun getLastSelectedCamera(): CameraSelector {
        val settings = dao.getAppSettings() ?: AppSettings()
        return if (settings.lastSelectedCamera == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    fun getAppSettingsFlow(): Flow<AppSettings> {
        return dao.getAppSettingsFlow().map { it ?: AppSettings() }
    }
    
    suspend fun updateLastSelectedCamera(cameraSelector: CameraSelector) {
        val cameraId = getCameraId(cameraSelector)
        dao.updateLastSelectedCamera(cameraId)
    }
    
    suspend fun updateAudioOutputDevice(deviceId: Int?) {
        dao.updateAudioOutputDevice(deviceId)
    }
    
    suspend fun getAudioOutputDeviceId(): Int? {
        return dao.getAppSettings()?.audioOutputDeviceId
    }
    
    // Helper function to convert CameraSelector to string ID
    private fun getCameraId(cameraSelector: CameraSelector): String {
        return if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
    }
}