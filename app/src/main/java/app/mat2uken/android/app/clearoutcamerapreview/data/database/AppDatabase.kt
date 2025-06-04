package app.mat2uken.android.app.clearoutcamerapreview.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for app settings persistence
 */
@Database(
    entities = [DisplaySettings::class, CameraSettings::class, AppSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Migration from version 1 to 2:
         * - Add cameraId column to display_settings table
         * - Add cameraName column to display_settings table
         * - Change primary key from displayId to composite key (displayId, cameraId)
         * - Migrate existing data to use "0" (back camera) as default cameraId
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with updated schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS display_settings_new (
                        displayId TEXT NOT NULL,
                        cameraId TEXT NOT NULL,
                        isVerticallyFlipped INTEGER NOT NULL DEFAULT 0,
                        isHorizontallyFlipped INTEGER NOT NULL DEFAULT 0,
                        displayName TEXT NOT NULL DEFAULT '',
                        cameraName TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(displayId, cameraId)
                    )
                """)
                
                // Copy existing data to new table with default cameraId "0" (back camera)
                database.execSQL("""
                    INSERT INTO display_settings_new (displayId, cameraId, isVerticallyFlipped, isHorizontallyFlipped, displayName, cameraName)
                    SELECT displayId, '0', isVerticallyFlipped, isHorizontallyFlipped, displayName, 'Back Camera'
                    FROM display_settings
                """)
                
                // Drop old table
                database.execSQL("DROP TABLE display_settings")
                
                // Rename new table to old table name
                database.execSQL("ALTER TABLE display_settings_new RENAME TO display_settings")
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clearout_camera_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}