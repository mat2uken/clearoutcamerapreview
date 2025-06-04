package app.mat2uken.android.app.clearoutcamerapreview.data.database

import androidx.camera.core.CameraSelector
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

@RunWith(RobolectricTestRunner::class)
class SettingsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: SettingsDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.settingsDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `test insert and get display settings`() = runBlocking {
        // Given
        val displaySettings = DisplaySettings(
            displayId = "display123",
            displayName = "External Display",
            cameraId = CameraSelector.LENS_FACING_BACK.toString(),
            isVerticallyFlipped = true,
            isHorizontallyFlipped = false
        )
        
        // When
        dao.insertOrUpdateDisplaySettings(displaySettings)
        val retrieved = dao.getDisplaySettings("display123", CameraSelector.LENS_FACING_BACK.toString())
        
        // Then
        assertNotNull(retrieved)
        assertEquals(displaySettings.displayId, retrieved!!.displayId)
        assertEquals(displaySettings.displayName, retrieved.displayName)
        assertEquals(displaySettings.cameraId, retrieved.cameraId)
        assertEquals(displaySettings.isVerticallyFlipped, retrieved.isVerticallyFlipped)
        assertEquals(displaySettings.isHorizontallyFlipped, retrieved.isHorizontallyFlipped)
    }
    
    @Test
    fun `test update display settings`() = runBlocking {
        // Given
        val displaySettings = DisplaySettings(
            displayId = "display123",
            displayName = "External Display",
            cameraId = CameraSelector.LENS_FACING_BACK.toString(),
            isVerticallyFlipped = false,
            isHorizontallyFlipped = false
        )
        dao.insertOrUpdateDisplaySettings(displaySettings)
        
        // When
        val updated = displaySettings.copy(
            isVerticallyFlipped = true,
            isHorizontallyFlipped = true
        )
        dao.insertOrUpdateDisplaySettings(updated)
        val retrieved = dao.getDisplaySettings("display123", CameraSelector.LENS_FACING_BACK.toString())
        
        // Then
        assertNotNull(retrieved)
        assertEquals(true, retrieved!!.isVerticallyFlipped)
        assertEquals(true, retrieved.isHorizontallyFlipped)
    }
    
    @Test
    fun `test get display settings returns null for non-existent display`() = runBlocking {
        // When
        val retrieved = dao.getDisplaySettings("non-existent", CameraSelector.LENS_FACING_BACK.toString())
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun `test insert and get camera settings`() = runBlocking {
        // Given
        val cameraSettings = CameraSettings(
            cameraId = CameraSelector.LENS_FACING_FRONT.toString(),
            zoomRatio = 2.5f,
            minZoomRatio = 1.0f,
            maxZoomRatio = 5.0f
        )
        
        // When
        dao.insertOrUpdateCameraSettings(cameraSettings)
        val retrieved = dao.getCameraSettings(CameraSelector.LENS_FACING_FRONT.toString())
        
        // Then
        assertNotNull(retrieved)
        assertEquals(cameraSettings.cameraId, retrieved!!.cameraId)
        assertEquals(cameraSettings.zoomRatio, retrieved.zoomRatio)
        assertEquals(cameraSettings.minZoomRatio, retrieved.minZoomRatio)
        assertEquals(cameraSettings.maxZoomRatio, retrieved.maxZoomRatio)
    }
    
    @Test
    fun `test update camera settings`() = runBlocking {
        // Given
        val cameraSettings = CameraSettings(
            cameraId = CameraSelector.LENS_FACING_BACK.toString(),
            zoomRatio = 1.0f,
            minZoomRatio = 1.0f,
            maxZoomRatio = 10.0f
        )
        dao.insertOrUpdateCameraSettings(cameraSettings)
        
        // When
        val updated = cameraSettings.copy(zoomRatio = 3.5f)
        dao.insertOrUpdateCameraSettings(updated)
        val retrieved = dao.getCameraSettings(CameraSelector.LENS_FACING_BACK.toString())
        
        // Then
        assertNotNull(retrieved)
        assertEquals(3.5f, retrieved!!.zoomRatio)
    }
    
    @Test
    fun `test insert and get app settings`() = runBlocking {
        // Given
        val appSettings = AppSettings(
            id = 1,
            lastSelectedCamera = CameraSelector.LENS_FACING_FRONT.toString(),
            audioOutputDeviceId = 42
        )
        
        // When
        dao.insertOrUpdateAppSettings(appSettings)
        val retrieved = dao.getAppSettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(appSettings.lastSelectedCamera, retrieved!!.lastSelectedCamera)
        assertEquals(appSettings.audioOutputDeviceId, retrieved.audioOutputDeviceId)
    }
    
    @Test
    fun `test update app settings`() = runBlocking {
        // Given
        val appSettings = AppSettings(
            id = 1,
            lastSelectedCamera = CameraSelector.LENS_FACING_BACK.toString(),
            audioOutputDeviceId = null
        )
        dao.insertOrUpdateAppSettings(appSettings)
        
        // When
        val updated = appSettings.copy(
            lastSelectedCamera = CameraSelector.LENS_FACING_FRONT.toString(),
            audioOutputDeviceId = 123
        )
        dao.insertOrUpdateAppSettings(updated)
        val retrieved = dao.getAppSettings()
        
        // Then
        assertNotNull(retrieved)
        assertEquals(CameraSelector.LENS_FACING_FRONT.toString(), retrieved!!.lastSelectedCamera)
        assertEquals(123, retrieved.audioOutputDeviceId)
    }
    
    @Test
    fun `test separate settings for different cameras on same display`() = runBlocking {
        // Given
        val frontCameraSettings = DisplaySettings(
            displayId = "display123",
            displayName = "External Display",
            cameraId = CameraSelector.LENS_FACING_FRONT.toString(),
            isVerticallyFlipped = true,
            isHorizontallyFlipped = true
        )
        val backCameraSettings = DisplaySettings(
            displayId = "display123",
            displayName = "External Display",
            cameraId = CameraSelector.LENS_FACING_BACK.toString(),
            isVerticallyFlipped = false,
            isHorizontallyFlipped = false
        )
        
        // When
        dao.insertOrUpdateDisplaySettings(frontCameraSettings)
        dao.insertOrUpdateDisplaySettings(backCameraSettings)
        
        val retrievedFront = dao.getDisplaySettings("display123", CameraSelector.LENS_FACING_FRONT.toString())
        val retrievedBack = dao.getDisplaySettings("display123", CameraSelector.LENS_FACING_BACK.toString())
        
        // Then
        assertNotNull(retrievedFront)
        assertNotNull(retrievedBack)
        assertEquals(true, retrievedFront!!.isVerticallyFlipped)
        assertEquals(true, retrievedFront.isHorizontallyFlipped)
        assertEquals(false, retrievedBack!!.isVerticallyFlipped)
        assertEquals(false, retrievedBack.isHorizontallyFlipped)
    }
}