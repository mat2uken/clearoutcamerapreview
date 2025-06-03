package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.view.Display
import android.view.Surface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class DisplayUtilsTest {
    
    @Test
    fun `findExternalDisplay returns null for empty array`() {
        val result = DisplayUtils.findExternalDisplay(emptyArray())
        assertNull(result)
    }
    
    @Test
    fun `findExternalDisplay returns null when only default display exists`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        
        val result = DisplayUtils.findExternalDisplay(arrayOf(defaultDisplay))
        assertNull(result)
    }
    
    @Test
    fun `findExternalDisplay returns external display when present`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        
        val externalDisplay = mockk<Display>()
        every { externalDisplay.displayId } returns 1
        
        val result = DisplayUtils.findExternalDisplay(arrayOf(defaultDisplay, externalDisplay))
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
    }
    
    @Test
    fun `findExternalDisplay returns first external display when multiple exist`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        
        val externalDisplay1 = mockk<Display>()
        every { externalDisplay1.displayId } returns 1
        
        val externalDisplay2 = mockk<Display>()
        every { externalDisplay2.displayId } returns 2
        
        val result = DisplayUtils.findExternalDisplay(
            arrayOf(defaultDisplay, externalDisplay1, externalDisplay2)
        )
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
    }
    
    @Test
    fun `isExternalDisplay detects correctly`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        assertFalse(DisplayUtils.isExternalDisplay(defaultDisplay))
        
        val externalDisplay = mockk<Display>()
        every { externalDisplay.displayId } returns 1
        assertTrue(DisplayUtils.isExternalDisplay(externalDisplay))
    }
    
    @Test
    fun `getDisplayAspectRatio calculates correctly`() {
        assertEquals(16f/9f, DisplayUtils.getDisplayAspectRatio(1920, 1080), 0.001f)
        assertEquals(4f/3f, DisplayUtils.getDisplayAspectRatio(640, 480), 0.001f)
        assertEquals(21f/9f, DisplayUtils.getDisplayAspectRatio(2520, 1080), 0.001f)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `getDisplayAspectRatio throws for zero width`() {
        DisplayUtils.getDisplayAspectRatio(0, 100)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `getDisplayAspectRatio throws for negative dimensions`() {
        DisplayUtils.getDisplayAspectRatio(-100, 100)
    }
    
    @Test
    fun `calculateRotationDegrees returns correct rotation`() {
        // Portrait display with ROTATION_0
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(true, Surface.ROTATION_0), 0.001f)
        
        // Portrait display with ROTATION_180
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(true, Surface.ROTATION_180), 0.001f)
        
        // Landscape display (any rotation)
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(false, Surface.ROTATION_0), 0.001f)
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(false, Surface.ROTATION_90), 0.001f)
        
        // Portrait with other rotations
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(true, Surface.ROTATION_90), 0.001f)
        assertEquals(180f, DisplayUtils.calculateRotationDegrees(true, Surface.ROTATION_270), 0.001f)
    }
    
    @Test
    fun `formatDisplayInfo formats correctly`() {
        val result = DisplayUtils.formatDisplayInfo(1, 1920, 1080, Surface.ROTATION_0)
        assertEquals("Display 1: 1920x1080, rotation: 0", result)
        
        val result2 = DisplayUtils.formatDisplayInfo(2, 3840, 2160, Surface.ROTATION_90)
        assertEquals("Display 2: 3840x2160, rotation: 1", result2)
    }
}