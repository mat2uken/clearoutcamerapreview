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
    fun `test getTargetRotation returns device rotation unchanged`() {
        // Should use device rotation directly
        assertEquals(Surface.ROTATION_0, CameraRotationHelper.getTargetRotation(Surface.ROTATION_0))
        assertEquals(Surface.ROTATION_90, CameraRotationHelper.getTargetRotation(Surface.ROTATION_90))
        assertEquals(Surface.ROTATION_180, CameraRotationHelper.getTargetRotation(Surface.ROTATION_180))
        assertEquals(Surface.ROTATION_270, CameraRotationHelper.getTargetRotation(Surface.ROTATION_270))
    }
    
    @Test
    fun `test getTargetRotation handles invalid rotation values`() {
        // Should return invalid values unchanged
        assertEquals(-1, CameraRotationHelper.getTargetRotation(-1))
        assertEquals(999, CameraRotationHelper.getTargetRotation(999))
    }
    
    @Test
    fun `test deprecated getTargetRotation with boolean parameter`() {
        // Test that deprecated method still works
        assertEquals(Surface.ROTATION_0, CameraRotationHelper.getTargetRotation(Surface.ROTATION_0, false))
        assertEquals(Surface.ROTATION_0, CameraRotationHelper.getTargetRotation(Surface.ROTATION_0, true))
        assertEquals(Surface.ROTATION_90, CameraRotationHelper.getTargetRotation(Surface.ROTATION_90, false))
        assertEquals(Surface.ROTATION_90, CameraRotationHelper.getTargetRotation(Surface.ROTATION_90, true))
    }
    
    @Test
    fun `test getRotationCompensation always returns 180 degrees`() {
        // Fixed 180 degree rotation for external display
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_0))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_90))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_180, Surface.ROTATION_180))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_270, Surface.ROTATION_270))
        
        // Should be 180 regardless of rotation combination
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_90))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_270))
    }
    
    @Test
    fun `test getRotationCompensation with different rotations`() {
        // Always returns 180 degrees regardless of rotation combination
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_90))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_180))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_270))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_0))
    }
    
    @Test
    fun `test getRotationCompensation handles invalid rotation values`() {
        // Should always return 180f
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(-1, Surface.ROTATION_0))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, -1))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(-1, -1))
    }
    
    @Test
    fun `test deprecated getRotationCompensation with boolean parameter`() {
        // Test that deprecated method still works
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_0, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_0, Surface.ROTATION_0, true))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_90, false))
        assertEquals(180f, CameraRotationHelper.getRotationCompensation(Surface.ROTATION_90, Surface.ROTATION_90, true))
    }
    
    @Test
    fun `test all rotation combinations for device and display`() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
        
        // Test all combinations (should always be 180)
        for (deviceRotation in rotations) {
            for (displayRotation in rotations) {
                val result = CameraRotationHelper.getRotationCompensation(deviceRotation, displayRotation)
                assertEquals("Should always return 180 degrees", 180f, result)
            }
        }
    }
}