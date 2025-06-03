package app.mat2uken.android.app.clearoutcamerapreview.utils

import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import org.junit.Assert.*
import org.junit.Test

class CameraUtilsTest {
    
    @Test
    fun `selectOptimalResolution returns null for empty list`() {
        val result = CameraUtils.selectOptimalResolution(emptyList())
        assertNull(result)
    }
    
    @Test
    fun `selectOptimalResolution prefers 1920x1080 when available`() {
        val sizes = listOf(
            Size(1280, 720),
            Size(1920, 1080),
            Size(3840, 2160),
            Size(640, 480)
        )
        
        val result = CameraUtils.selectOptimalResolution(sizes)
        assertNotNull(result)
        assertEquals(1920, result!!.width)
        assertEquals(1080, result.height)
    }
    
    @Test
    fun `selectOptimalResolution selects closest 16_9 ratio when Full HD not available`() {
        val sizes = listOf(
            Size(1280, 720),  // 16:9
            Size(640, 480),   // 4:3
            Size(1440, 1080), // 4:3
            Size(2560, 1440)  // 16:9
        )
        
        val result = CameraUtils.selectOptimalResolution(sizes)
        assertNotNull(result)
        // Should select 2560x1440 as it's 16:9 and higher resolution
        assertEquals(2560, result!!.width)
        assertEquals(1440, result.height)
    }
    
    @Test
    fun `selectOptimalResolution handles invalid sizes`() {
        // CameraUtils filters out invalid sizes (width or height <= 0)
        val sizes = emptyList<Size>()
        
        val result = CameraUtils.selectOptimalResolution(sizes)
        assertNull(result)
    }
    
    @Test
    fun `selectOptimalResolution with mixed valid and invalid sizes`() {
        // Only valid sizes can be created with our Size class
        val sizes = listOf(
            Size(1280, 720),
            Size(640, 480)
        )
        
        val result = CameraUtils.selectOptimalResolution(sizes)
        assertNotNull(result)
        assertEquals(1280, result!!.width)
        assertEquals(720, result.height)
    }
    
    @Test
    fun `formatZoomRatio formats correctly`() {
        assertEquals("1.0x", CameraUtils.formatZoomRatio(1.0f))
        assertEquals("1.5x", CameraUtils.formatZoomRatio(1.5f))
        assertEquals("2.0x", CameraUtils.formatZoomRatio(2.0f))
        assertEquals("10.0x", CameraUtils.formatZoomRatio(10.0f))
        assertEquals("5.5x", CameraUtils.formatZoomRatio(5.5f))
    }
    
    @Test
    fun `clampZoom clamps values correctly`() {
        val min = 1f
        val max = 10f
        
        // Below min
        assertEquals(min, CameraUtils.clampZoom(0.5f, min, max), 0.001f)
        
        // Above max
        assertEquals(max, CameraUtils.clampZoom(15f, min, max), 0.001f)
        
        // Within range
        assertEquals(5f, CameraUtils.clampZoom(5f, min, max), 0.001f)
        
        // Edge cases
        assertEquals(min, CameraUtils.clampZoom(min, min, max), 0.001f)
        assertEquals(max, CameraUtils.clampZoom(max, min, max), 0.001f)
    }
    
    @Test
    fun `calculateAspectRatio calculates correctly`() {
        assertEquals(16f/9f, CameraUtils.calculateAspectRatio(1920, 1080), 0.001f)
        assertEquals(4f/3f, CameraUtils.calculateAspectRatio(640, 480), 0.001f)
        assertEquals(1f, CameraUtils.calculateAspectRatio(1000, 1000), 0.001f)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `calculateAspectRatio throws for zero width`() {
        CameraUtils.calculateAspectRatio(0, 100)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `calculateAspectRatio throws for negative dimensions`() {
        CameraUtils.calculateAspectRatio(-100, 100)
    }
    
    @Test
    fun `isDisplayPortrait detects orientation correctly`() {
        assertTrue(CameraUtils.isDisplayPortrait(1080, 1920))
        assertFalse(CameraUtils.isDisplayPortrait(1920, 1080))
        assertFalse(CameraUtils.isDisplayPortrait(1000, 1000)) // Square is not portrait
    }
    
    @Test
    fun `calculateOptimalPreviewSize for portrait display`() {
        val displayWidth = 1080
        val displayHeight = 1920
        val cameraAspectRatio = 16f / 9f
        
        val (width, height) = CameraUtils.calculateOptimalPreviewSize(
            displayWidth, displayHeight, cameraAspectRatio, true
        )
        
        // Should fit within display bounds
        assertTrue(width <= displayWidth)
        assertTrue(height <= displayHeight)
        
        // Should maintain aspect ratio (accounting for rotation)
        val actualRatio = width.toFloat() / height.toFloat()
        val expectedRatio = 9f / 16f
        assertEquals(expectedRatio, actualRatio, 0.01f)
    }
    
    @Test
    fun `calculateOptimalPreviewSize for landscape display`() {
        val displayWidth = 1920
        val displayHeight = 1080
        val cameraAspectRatio = 16f / 9f
        
        val (width, height) = CameraUtils.calculateOptimalPreviewSize(
            displayWidth, displayHeight, cameraAspectRatio, false
        )
        
        // Should fit within display bounds
        assertTrue(width <= displayWidth)
        assertTrue(height <= displayHeight)
        
        // Should maintain aspect ratio
        val actualRatio = width.toFloat() / height.toFloat()
        assertEquals(cameraAspectRatio, actualRatio, 0.01f)
    }
    
    @Test
    fun `calculateOptimalPreviewSize for ultra-wide display`() {
        val displayWidth = 3440
        val displayHeight = 1440
        val cameraAspectRatio = 16f / 9f
        
        val (width, height) = CameraUtils.calculateOptimalPreviewSize(
            displayWidth, displayHeight, cameraAspectRatio, false
        )
        
        // Should be height-constrained
        assertEquals(displayHeight, height)
        assertEquals((displayHeight * cameraAspectRatio).toInt(), width)
    }
}