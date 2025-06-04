package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameRateUtilsTest {
    
    private fun createRange(lower: Int, upper: Int): Range<Int> {
        return Range(lower, upper)
    }
    
    @Before
    fun setUp() {
        mockkStatic(Camera2CameraInfo::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(Camera2CameraInfo::class)
    }
    
    @Test
    fun `detectActualFrameRate returns 60fps for 1920x1080 when available`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(15, 30),
            createRange(30, 60),
            createRange(60, 60)
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNotNull(result)
        assertEquals(60, result!!.lower)
        assertEquals(60, result.upper)
    }
    
    @Test
    fun `detectActualFrameRate returns 30fps for 1920x1080 when 60fps not available`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(15, 30),
            createRange(20, 40)
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNotNull(result)
        assertEquals(15, result!!.lower)
        assertEquals(30, result.upper)
    }
    
    @Test
    fun `detectActualFrameRate returns 30fps for non-1080p resolutions`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(15, 30),
            createRange(30, 60),
            createRange(60, 60)
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val actualPreviewSize = Size(1280, 720)
        val selectedResolution = Size(1280, 720)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNotNull(result)
        assertEquals(15, result!!.lower)
        assertEquals(30, result.upper)
    }
    
    @Test
    fun `detectActualFrameRate uses selectedResolution when actualPreviewSize is null`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(30, 60),
            createRange(60, 60)
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, null, selectedResolution)
        
        assertNotNull(result)
        assertEquals(60, result!!.lower)
        assertEquals(60, result.upper)
    }
    
    @Test
    fun `detectActualFrameRate returns first range when no preferred fps found`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(8, 15),
            createRange(15, 24)
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNotNull(result)
        assertEquals(8, result!!.lower)
        assertEquals(15, result.upper)
    }
    
    @Test
    fun `detectActualFrameRate returns null when fps ranges are null`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns null
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNull(result)
    }
    
    @Test
    fun `detectActualFrameRate returns null when fps ranges are empty`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns emptyArray()
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNull(result)
    }
    
    @Test
    fun `detectActualFrameRate returns null when exception occurs`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } throws RuntimeException("Camera error")
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNull(result)
    }
    
    @Test
    fun `detectActualFrameRate prefers exact 60fps range for 1080p`() {
        val camera = mockk<Camera>()
        val context = mockk<Context>()
        val cameraInfo = mockk<CameraInfo>()
        val camera2Info = mockk<Camera2CameraInfo>()
        val cameraManager = mockk<CameraManager>()
        val characteristics = mockk<CameraCharacteristics>()
        
        every { camera.cameraInfo } returns cameraInfo
        every { Camera2CameraInfo.from(cameraInfo) } returns camera2Info
        every { camera2Info.cameraId } returns "0"
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { cameraManager.getCameraCharacteristics("0") } returns characteristics
        
        val fpsRanges = arrayOf(
            createRange(30, 60), // This includes 60 but is not exact 60fps
            createRange(60, 60)  // This should be preferred for 1080p
        )
        every { characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns fpsRanges
        
        val actualPreviewSize = Size(1920, 1080)
        val selectedResolution = Size(1920, 1080)
        
        val result = FrameRateUtils.detectActualFrameRate(camera, context, actualPreviewSize, selectedResolution)
        
        assertNotNull(result)
        assertEquals(60, result!!.lower)
        assertEquals(60, result.upper)
    }
}