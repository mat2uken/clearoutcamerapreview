package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.view.WindowMetrics
import android.graphics.Rect
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.spyk
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After

class DisplayUtilsTest {
    
    @Before
    fun setUp() {
        // Mock static methods if needed
    }
    
    @After
    fun tearDown() {
        // Clean up mocks
        unmockkStatic(android.os.Build.VERSION::class)
    }
    
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
    
    // Tests for new DisplayInfo data class
    @Test
    fun `DisplayInfo getDisplaySizeString formats correctly`() {
        val displayInfo = DisplayInfo(
            displayId = 1,
            name = "Test Display",
            width = 1920,
            height = 1080,
            rotation = Surface.ROTATION_0,
            densityDpi = 160,
            isDefaultDisplay = false
        )
        
        assertEquals("1920x1080", displayInfo.getDisplaySizeString())
    }
    
    @Test
    fun `DisplayInfo getDisplayIdString returns correct string`() {
        val displayInfo = DisplayInfo(
            displayId = 42,
            name = "Test Display",
            width = 1920,
            height = 1080,
            rotation = Surface.ROTATION_0,
            densityDpi = 160,
            isDefaultDisplay = false
        )
        
        assertEquals("42", displayInfo.getDisplayIdString())
    }
    
    @Test
    fun `DisplayInfo getDisplayDescription formats correctly`() {
        val displayInfo = DisplayInfo(
            displayId = 1,
            name = "External Monitor",
            width = 1920,
            height = 1080,
            rotation = Surface.ROTATION_0,
            densityDpi = 160,
            isDefaultDisplay = false
        )
        
        assertEquals("External Monitor (ID: 1)", displayInfo.getDisplayDescription())
    }
    
    // Tests for findAllExternalDisplays
    @Test
    fun `findAllExternalDisplays returns empty list for no external displays`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        
        val result = DisplayUtils.findAllExternalDisplays(arrayOf(defaultDisplay))
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `findAllExternalDisplays returns all external displays`() {
        val defaultDisplay = mockk<Display>()
        every { defaultDisplay.displayId } returns Display.DEFAULT_DISPLAY
        
        val externalDisplay1 = mockk<Display>()
        every { externalDisplay1.displayId } returns 1
        
        val externalDisplay2 = mockk<Display>()
        every { externalDisplay2.displayId } returns 2
        
        val result = DisplayUtils.findAllExternalDisplays(
            arrayOf(defaultDisplay, externalDisplay1, externalDisplay2)
        )
        
        assertEquals(2, result.size)
        assertEquals(1, result[0].displayId)
        assertEquals(2, result[1].displayId)
    }
    
    @Test
    fun `findAllExternalDisplays returns empty list for empty array`() {
        val result = DisplayUtils.findAllExternalDisplays(emptyArray())
        assertTrue(result.isEmpty())
    }
    
    // Tests for getDisplayInfo
    @Test
    fun `getDisplayInfo creates correct DisplayInfo with display name`() {
        val display = mockk<Display>()
        every { display.displayId } returns 1
        every { display.name } returns "Test Monitor"
        every { display.rotation } returns Surface.ROTATION_0
        every { display.getRealMetrics(any()) } answers {
            val metrics = firstArg<DisplayMetrics>()
            metrics.widthPixels = 1920
            metrics.heightPixels = 1080
            metrics.densityDpi = 160
        }
        
        val result = DisplayUtils.getDisplayInfo(display)
        
        assertEquals(1, result.displayId)
        assertEquals("Test Monitor", result.name)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
        assertEquals(Surface.ROTATION_0, result.rotation)
        assertEquals(160, result.densityDpi)
        assertFalse(result.isDefaultDisplay)
    }
    
    @Test
    fun `getDisplayInfo creates correct DisplayInfo with default display`() {
        val display = mockk<Display>()
        every { display.displayId } returns Display.DEFAULT_DISPLAY
        every { display.name } returns "Built-in Screen"
        every { display.rotation } returns Surface.ROTATION_0
        every { display.getRealMetrics(any()) } answers {
            val metrics = firstArg<DisplayMetrics>()
            metrics.widthPixels = 1080
            metrics.heightPixels = 1920
            metrics.densityDpi = 320
        }
        
        val result = DisplayUtils.getDisplayInfo(display)
        
        assertEquals(Display.DEFAULT_DISPLAY, result.displayId)
        assertEquals("Built-in Screen", result.name)
        assertEquals(1080, result.width)
        assertEquals(1920, result.height)
        assertTrue(result.isDefaultDisplay)
    }
    
    @Test
    fun `getDisplayInfo handles null display name`() {
        val display = mockk<Display>()
        every { display.displayId } returns 1
        every { display.name } returns null
        every { display.rotation } returns Surface.ROTATION_0
        every { display.getRealMetrics(any()) } answers {
            val metrics = firstArg<DisplayMetrics>()
            metrics.widthPixels = 1920
            metrics.heightPixels = 1080
            metrics.densityDpi = 160
        }
        
        val result = DisplayUtils.getDisplayInfo(display)
        
        assertEquals("Display 1", result.name)
    }
    
    // Tests for getAllDisplayInfo
    @Test
    fun `getAllDisplayInfo returns info for all displays`() {
        val display1 = mockk<Display>()
        every { display1.displayId } returns Display.DEFAULT_DISPLAY
        every { display1.name } returns "Default"
        every { display1.rotation } returns Surface.ROTATION_0
        every { display1.getRealMetrics(any()) } answers {
            val metrics = firstArg<DisplayMetrics>()
            metrics.widthPixels = 1080
            metrics.heightPixels = 1920
            metrics.densityDpi = 320
        }
        
        val display2 = mockk<Display>()
        every { display2.displayId } returns 1
        every { display2.name } returns "External"
        every { display2.rotation } returns Surface.ROTATION_0
        every { display2.getRealMetrics(any()) } answers {
            val metrics = firstArg<DisplayMetrics>()
            metrics.widthPixels = 1920
            metrics.heightPixels = 1080
            metrics.densityDpi = 160
        }
        
        val displayManager = mockk<DisplayManager>()
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = DisplayUtils.getAllDisplayInfo(displayManager)
        
        assertEquals(2, result.size)
        assertEquals(Display.DEFAULT_DISPLAY, result[0].displayId)
        assertEquals("Default", result[0].name)
        assertTrue(result[0].isDefaultDisplay)
        
        assertEquals(1, result[1].displayId)
        assertEquals("External", result[1].name)
        assertFalse(result[1].isDefaultDisplay)
    }
    
    @Test
    fun `getAllDisplayInfo returns empty list for no displays`() {
        val displayManager = mockk<DisplayManager>()
        every { displayManager.displays } returns emptyArray()
        
        val result = DisplayUtils.getAllDisplayInfo(displayManager)
        
        assertTrue(result.isEmpty())
    }
    
    // Tests for getDisplayName
    @Test
    fun `getDisplayName returns display name when available`() {
        val display = mockk<Display>()
        every { display.name } returns "Test Monitor"
        every { display.displayId } returns 1
        
        val result = DisplayUtils.getDisplayName(display)
        assertEquals("Test Monitor", result)
    }
    
    @Test
    fun `getDisplayName returns fallback when name is null`() {
        val display = mockk<Display>()
        every { display.name } returns null
        every { display.displayId } returns 1
        
        val result = DisplayUtils.getDisplayName(display)
        assertEquals("Display 1", result)
    }
    
    // Tests for getDisplayIdentifier
    @Test
    fun `getDisplayIdentifier formats correctly`() {
        val display = mockk<Display>()
        every { display.name } returns "Test Monitor"
        every { display.displayId } returns 1
        
        val result = DisplayUtils.getDisplayIdentifier(display)
        assertEquals("Test Monitor (ID: 1)", result)
    }
    
    @Test
    fun `getDisplayIdentifier formats correctly with null name`() {
        val display = mockk<Display>()
        every { display.name } returns null
        every { display.displayId } returns 2
        
        val result = DisplayUtils.getDisplayIdentifier(display)
        assertEquals("Display 2 (ID: 2)", result)
    }
}