package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for all settings operations
 */
@Dao
interface SettingsDao {
    // Display Settings
    @Query("SELECT * FROM display_settings WHERE displayId = :displayId")
    suspend fun getDisplaySettings(displayId: String): DisplaySettings?
    
    @Query("SELECT * FROM display_settings WHERE displayId = :displayId")
    fun getDisplaySettingsFlow(displayId: String): Flow<DisplaySettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDisplaySettings(settings: DisplaySettings)
    
    @Query("UPDATE display_settings SET isVerticallyFlipped = :isVerticallyFlipped, isHorizontallyFlipped = :isHorizontallyFlipped WHERE displayId = :displayId")
    suspend fun updateDisplayFlipSettings(displayId: String, isVerticallyFlipped: Boolean, isHorizontallyFlipped: Boolean)
    
    // Camera Settings
    @Query("SELECT * FROM camera_settings WHERE cameraId = :cameraId")
    suspend fun getCameraSettings(cameraId: String): CameraSettings?
    
    @Query("SELECT * FROM camera_settings WHERE cameraId = :cameraId")
    fun getCameraSettingsFlow(cameraId: String): Flow<CameraSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCameraSettings(settings: CameraSettings)
    
    @Query("UPDATE camera_settings SET zoomRatio = :zoomRatio WHERE cameraId = :cameraId")
    suspend fun updateCameraZoom(cameraId: String, zoomRatio: Float)
    
    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getAppSettings(): AppSettings?
    
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettingsFlow(): Flow<AppSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppSettings(settings: AppSettings)
    
    @Query("UPDATE app_settings SET lastSelectedCamera = :cameraId WHERE id = 1")
    suspend fun updateLastSelectedCamera(cameraId: String)
    
    @Query("UPDATE app_settings SET audioOutputDeviceId = :deviceId WHERE id = 1")
    suspend fun updateAudioOutputDevice(deviceId: Int?)
    
    // Initialize default settings if not exists
    @Transaction
    suspend fun initializeDefaultSettings() {
        if (getAppSettings() == null) {
            insertOrUpdateAppSettings(AppSettings())
        }
    }
}