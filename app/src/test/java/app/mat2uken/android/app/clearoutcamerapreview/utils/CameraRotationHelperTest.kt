package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.view.Surface
import androidx.camera.core.CameraSelector
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CameraRotationHelperTest {
    
    @Test
    fun `test isFrontCamera returns true for front camera`() {
        val result = CameraRotationHelper.isFrontCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        assertTrue(result)
    }
    
    @Test
    fun `test isFrontCamera returns false for back camera`() {
        val result = CameraRotationHelper.isFrontCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        assertFalse(result)
    }
    
    @Test
    fun `test getTargetRotation for back camera returns device rotation unchanged`() {
        // Back camera should use device rotation directly
        assertEquals(Surface.ROTATION_0, CameraRotationHelper.getTargetRotation(Surface.ROTATION_0, false))
        assertEquals(Surface.ROTATION_90, CameraRotationHelper.getTargetRotation(Surface.ROTATION_90, false))
        assertEquals(Surface.ROTATION_180, CameraRotationHelper.getTargetRotation(Surface.ROTATION_180, false))
        assertEquals(Surface.ROTATION_270, CameraRotationHelper.getTargetRotation(Surface.ROTATION_270, false))
    }
    
    @Test
    fun `test getTargetRotation for front camera returns device rotation unchanged`() {
        // Front camera now uses device rotation directly like back camera
        assertEquals(Surface.ROTATION_0, CameraRotationHelper.getTargetRotation(Surface.ROTATION_0, true))
        assertEquals(Surface.ROTATION_90, CameraRotationHelper.getTargetRotation(Surface.ROTATION_90, true))
        assertEquals(Surface.ROTATION_180, CameraRotationHelper.getTargetRotation(Surface.ROTATION_180, true))
        assertEquals(Surface.ROTATION_270, CameraRotationHelper.getTargetRotation(Surface.ROTATION_270, true))
    }
    
    @Test
    fun `test getTargetRotation handles invalid rotation values`() {
        // Both cameras now return invalid values unchanged
        assertEquals(-1, CameraRotationHelper.getTargetRotation(-1, true))
        assertEquals(999, CameraRotationHelper.getTargetRotation(999, true))
        assertEquals(-1, CameraRotationHelper.getTargetRotation(-1, false))
        assertEquals(999, CameraRotationHelper.getTargetRotation(999, false))
    }
    
    @Test
    fun `test getRotationCompensation for back camera always returns 180 degrees`() {
        // Back camera uses fixed 180 degree rotation for external display
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_0, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_90, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_180, Surface.ROTATION_180, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_270, Surface.ROTATION_270, false))
        
        // Should be 180 regardless of rotation combination
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_90, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_270, false))
    }
    
    @Test
    fun `test getRotationCompensation for front camera always returns 180 degrees`() {
        // Front camera now uses fixed 180 degree rotation like back camera
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_0, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_90, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_180, Surface.ROTATION_180, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_270, Surface.ROTATION_270, true))
    }
    
    @Test
    fun `test getRotationCompensation for front camera with different rotations`() {
        // Front camera always returns 180 degrees regardless of rotation combination
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_90, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_180, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_270, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_0, true))
    }
    
    @Test
    fun `test getRotationCompensation handles invalid rotation values`() {
        // Should always return 180f for both cameras
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(-1, Surface.ROTATION_0, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, -1, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(-1, -1, false))
    }
    
    @Test
    fun `test all rotation combinations for device and display`() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
        
        // Test all combinations for back camera (should always be 180)
        for (deviceRotation in rotations) {
            for (displayRotation in rotations) {
                val result = CameraRotationHelper.getRotationCompensation(deviceRotation, displayRotation, false)
                assertEquals("Back camera should always return 180 degrees", 180f, result)
            }
        }
        
        // Test all combinations for front camera (should always be 180)
        for (deviceRotation in rotations) {
            for (displayRotation in rotations) {
                val result = CameraRotationHelper.getRotationCompensation(deviceRotation, displayRotation, true)
                assertEquals("Front camera should always return 180 degrees", 180f, result)
            }
        }
    }
}