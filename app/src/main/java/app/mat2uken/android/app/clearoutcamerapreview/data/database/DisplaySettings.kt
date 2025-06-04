package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing display-specific settings
 * Each external display has its own flip settings
 */
@Entity(tableName = "display_settings")
data class DisplaySettings(
    @PrimaryKey
    val displayId: String, // Display unique identifier
    val isVerticallyFlipped: Boolean = false,
    val isHorizontallyFlipped: Boolean = false,
    val displayName: String = "" // For reference
)