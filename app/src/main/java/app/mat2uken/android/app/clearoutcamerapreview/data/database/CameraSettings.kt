package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing camera-specific settings
 * Each camera (front/back) has its own zoom level
 */
@Entity(tableName = "camera_settings")
data class CameraSettings(
    @PrimaryKey
    val cameraId: String, // "front" or "back"
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f
)