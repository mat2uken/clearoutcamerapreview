package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.room.Entity

/**
 * Entity for storing display-specific settings per camera
 * Each combination of external display and camera has its own flip settings
 */
@Entity(
    tableName = "display_settings",
    primaryKeys = ["displayId", "cameraId"]
)
data class DisplaySettings(
    val displayId: String, // Display unique identifier
    val cameraId: String, // Camera identifier (e.g., "0" for back, "1" for front)
    val isVerticallyFlipped: Boolean = false,
    val isHorizontallyFlipped: Boolean = false,
    val displayName: String = "", // For reference
    val cameraName: String = "" // For reference (e.g., "Back Camera", "Front Camera")
)