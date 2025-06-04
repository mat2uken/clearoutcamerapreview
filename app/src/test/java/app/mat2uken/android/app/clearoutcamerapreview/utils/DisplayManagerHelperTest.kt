package app.mat2uken.android.app.clearoutcamerapreview.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DisplayManagerHelperTest {
    
    private lateinit var displayManager: DisplayManager
    private lateinit var context: Context
    private lateinit var helper: DisplayManagerHelper
    
    @Before
    fun setUp() {
        displayManager = mockk()
        context = mockk()
        helper = DisplayManagerHelper(displayManager, context)
    }
    
    @Test
    fun `getAllDisplayInfo returns all display information`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        val display3 = createMockDisplay(2, "External 2")
        
        every { displayManager.displays } returns arrayOf(display1, display2, display3)
        
        val result = helper.getAllDisplayInfo()
        
        assertEquals(3, result.size)
        assertEquals(Display.DEFAULT_DISPLAY, result[0].displayId)
        assertEquals("Default", result[0].name)
        assertTrue(result[0].isDefaultDisplay)
        
        assertEquals(1, result[1].displayId)
        assertEquals("External 1", result[1].name)
        assertFalse(result[1].isDefaultDisplay)
        
        assertEquals(2, result[2].displayId)
        assertEquals("External 2", result[2].name)
        assertFalse(result[2].isDefaultDisplay)
    }
    
    @Test
    fun `getExternalDisplays returns only external displays`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        val display3 = createMockDisplay(2, "External 2")
        
        every { displayManager.displays } returns arrayOf(display1, display2, display3)
        
        val result = helper.getExternalDisplays()
        
        assertEquals(2, result.size)
        assertEquals(1, result[0].displayId)
        assertEquals(2, result[1].displayId)
    }
    
    @Test
    fun `getExternalDisplays returns empty list when no external displays`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display1)
        
        val result = helper.getExternalDisplays()
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `getExternalDisplayInfo returns only external display info`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = helper.getExternalDisplayInfo()
        
        assertEquals(1, result.size)
        assertEquals(1, result[0].displayId)
        assertEquals("External 1", result[0].name)
        assertFalse(result[0].isDefaultDisplay)
    }
    
    @Test
    fun `findDisplayById returns correct display when found`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = helper.findDisplayById(1)
        
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
    }
    
    @Test
    fun `findDisplayById returns null when not found`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display1)
        
        val result = helper.findDisplayById(999)
        
        assertNull(result)
    }
    
    @Test
    fun `getDisplayInfoById returns correct info when display exists`() {
        val display = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display)
        
        val result = helper.getDisplayInfoById(1)
        
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
        assertEquals("External 1", result.name)
    }
    
    @Test
    fun `getDisplayInfoById returns null when display does not exist`() {
        every { displayManager.displays } returns emptyArray()
        
        val result = helper.getDisplayInfoById(999)
        
        assertNull(result)
    }
    
    @Test
    fun `hasExternalDisplays returns true when external displays exist`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = helper.hasExternalDisplays()
        
        assertTrue(result)
    }
    
    @Test
    fun `hasExternalDisplays returns false when no external displays`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display1)
        
        val result = helper.hasExternalDisplays()
        
        assertFalse(result)
    }
    
    @Test
    fun `getFirstExternalDisplay returns first external display`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        val display3 = createMockDisplay(2, "External 2")
        
        every { displayManager.displays } returns arrayOf(display1, display2, display3)
        
        val result = helper.getFirstExternalDisplay()
        
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
    }
    
    @Test
    fun `getFirstExternalDisplay returns null when no external displays`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display1)
        
        val result = helper.getFirstExternalDisplay()
        
        assertNull(result)
    }
    
    @Test
    fun `getTargetDisplay returns selected display when it exists`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        val display3 = createMockDisplay(2, "External 2")
        
        every { displayManager.displays } returns arrayOf(display1, display2, display3)
        
        val result = helper.getTargetDisplay(2)
        
        assertNotNull(result)
        assertEquals(2, result!!.displayId)
    }
    
    @Test
    fun `getTargetDisplay returns first external when selected does not exist`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = helper.getTargetDisplay(999) // Non-existent ID
        
        assertNotNull(result)
        assertEquals(1, result!!.displayId) // Falls back to first external
    }
    
    @Test
    fun `getTargetDisplay returns first external when selectedDisplayId is null`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display1, display2)
        
        val result = helper.getTargetDisplay(null)
        
        assertNotNull(result)
        assertEquals(1, result!!.displayId)
    }
    
    @Test
    fun `isValidExternalDisplayId returns true for valid external display`() {
        val display = createMockDisplay(1, "External 1")
        
        every { displayManager.displays } returns arrayOf(display)
        
        val result = helper.isValidExternalDisplayId(1)
        
        assertTrue(result)
    }
    
    @Test
    fun `isValidExternalDisplayId returns false for default display`() {
        val display = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display)
        
        val result = helper.isValidExternalDisplayId(Display.DEFAULT_DISPLAY)
        
        assertFalse(result)
    }
    
    @Test
    fun `isValidExternalDisplayId returns false for non-existent display`() {
        every { displayManager.displays } returns emptyArray()
        
        val result = helper.isValidExternalDisplayId(999)
        
        assertFalse(result)
    }
    
    @Test
    fun `getDisplaySummary returns correct summary`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        val display2 = createMockDisplay(1, "External 1")
        val display3 = createMockDisplay(2, "External 2")
        
        every { displayManager.displays } returns arrayOf(display1, display2, display3)
        
        val result = helper.getDisplaySummary()
        
        assertEquals("Total displays: 3, External: 2", result)
    }
    
    @Test
    fun `getDisplaySummary returns correct summary with no external displays`() {
        val display1 = createMockDisplay(Display.DEFAULT_DISPLAY, "Default")
        
        every { displayManager.displays } returns arrayOf(display1)
        
        val result = helper.getDisplaySummary()
        
        assertEquals("Total displays: 1, External: 0", result)
    }
    
    private fun createMockDisplay(id: Int, name: String): Display {
        val display = mockk<Display>()
        every { display.displayId } returns id
        every { display.name } returns name
        every { display.rotation } returns 0
        every { display.getRealMetrics(any()) } answers {
            val metrics = firstArg<android.util.DisplayMetrics>()
            metrics.widthPixels = 1920
            metrics.heightPixels = 1080
            metrics.densityDpi = 160
        }
        return display
    }
}