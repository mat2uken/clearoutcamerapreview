package app.mat2uken.android.app.clearoutcamerapreview.camera

import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import androidx.camera.core.CameraSelector
import org.junit.Assert.*
import org.junit.Test

class CameraStateTest {
    
    @Test
    fun `default state has correct values`() {
        val state = CameraState()
        
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, state.cameraSelector)
        assertEquals(1f, state.zoomRatio, 0.001f)
        assertEquals(1f, state.minZoomRatio, 0.001f)
        assertEquals(1f, state.maxZoomRatio, 0.001f)
        assertNull(state.selectedResolution)
        assertNull(state.actualPreviewSize)
        assertFalse(state.isExternalDisplayConnected)
        assertNull(state.externalDisplayId)
    }
    
    @Test
    fun `updateZoomRatio clamps values correctly`() {
        val state = CameraState(minZoomRatio = 1f, maxZoomRatio = 10f)
        
        // Within range
        val updated1 = state.updateZoomRatio(5f)
        assertEquals(5f, updated1.zoomRatio, 0.001f)
        
        // Below min
        val updated2 = state.updateZoomRatio(0.5f)
        assertEquals(1f, updated2.zoomRatio, 0.001f)
        
        // Above max
        val updated3 = state.updateZoomRatio(15f)
        assertEquals(10f, updated3.zoomRatio, 0.001f)
    }
    
    @Test
    fun `updateZoomBounds updates correctly`() {
        val state = CameraState(zoomRatio = 5f)
        
        // Normal update
        val updated1 = state.updateZoomBounds(0.5f, 20f)
        assertEquals(0.5f, updated1.minZoomRatio, 0.001f)
        assertEquals(20f, updated1.maxZoomRatio, 0.001f)
        assertEquals(5f, updated1.zoomRatio, 0.001f) // Unchanged, still in range
        
        // Zoom ratio needs clamping
        val updated2 = state.updateZoomBounds(6f, 10f)
        assertEquals(6f, updated2.minZoomRatio, 0.001f)
        assertEquals(10f, updated2.maxZoomRatio, 0.001f)
        assertEquals(6f, updated2.zoomRatio, 0.001f) // Clamped to min
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `updateZoomBounds throws when min greater than max`() {
        val state = CameraState()
        state.updateZoomBounds(10f, 5f)
    }
    
    @Test
    fun `toggleCamera switches correctly`() {
        val stateBack = CameraState(cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)
        val stateFront = stateBack.toggleCamera()
        
        assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, stateFront.cameraSelector)
        
        val stateBackAgain = stateFront.toggleCamera()
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, stateBackAgain.cameraSelector)
    }
    
    @Test
    fun `updateExternalDisplay handles connection`() {
        val state = CameraState()
        
        // Connect
        val connected = state.updateExternalDisplay(true, 1)
        assertTrue(connected.isExternalDisplayConnected)
        assertEquals(1, connected.externalDisplayId)
        
        // Disconnect
        val disconnected = connected.updateExternalDisplay(false)
        assertFalse(disconnected.isExternalDisplayConnected)
        assertNull(disconnected.externalDisplayId)
    }
    
    @Test
    fun `copy preserves all fields`() {
        val original = CameraState(
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            zoomRatio = 2.5f,
            minZoomRatio = 0.5f,
            maxZoomRatio = 10f,
            selectedResolution = Size(1920, 1080),
            actualPreviewSize = Size(1280, 720),
            isExternalDisplayConnected = true,
            externalDisplayId = 2
        )
        
        val copied = original.copy()
        
        assertEquals(original.cameraSelector, copied.cameraSelector)
        assertEquals(original.zoomRatio, copied.zoomRatio, 0.001f)
        assertEquals(original.minZoomRatio, copied.minZoomRatio, 0.001f)
        assertEquals(original.maxZoomRatio, copied.maxZoomRatio, 0.001f)
        assertEquals(original.selectedResolution, copied.selectedResolution)
        assertEquals(original.actualPreviewSize, copied.actualPreviewSize)
        assertEquals(original.isExternalDisplayConnected, copied.isExternalDisplayConnected)
        assertEquals(original.externalDisplayId, copied.externalDisplayId)
    }
}