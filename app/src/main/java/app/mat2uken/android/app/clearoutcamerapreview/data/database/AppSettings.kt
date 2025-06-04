package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing general app settings
 * Single row table with id = 1
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // Always 1, single row table
    val lastSelectedCamera: String = "back", // "front" or "back"
    val audioOutputDeviceId: Int? = null // Last selected audio output device
)