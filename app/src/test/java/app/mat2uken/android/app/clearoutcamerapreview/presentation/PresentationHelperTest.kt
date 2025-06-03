package app.mat2uken.android.app.clearoutcamerapreview.presentation

import android.util.DisplayMetrics
import app.mat2uken.android.app.clearoutcamerapreview.model.Size
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PresentationHelperTest {
    
    private lateinit var helper: PresentationHelper
    
    @Before
    fun setup() {
        helper = PresentationHelper()
    }
    
    @Test
    fun `calculatePreviewConfiguration for landscape display`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1920
            heightPixels = 1080
        }
        
        val config = helper.calculatePreviewConfiguration(displayMetrics)
        
        assertNotNull(config)
        assertEquals(1920, config.width)
        assertEquals(1080, config.height)
        assertEquals(180f, config.rotation, 0.001f)
        assertFalse(config.isPortrait)
    }
    
    @Test
    fun `calculatePreviewConfiguration for portrait display`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }
        
        val config = helper.calculatePreviewConfiguration(displayMetrics)
        
        assertNotNull(config)
        // For portrait, the preview size should be calculated for rotated aspect ratio
        assertTrue(config.width <= 1080)
        assertTrue(config.height <= 1920)
        assertEquals(180f, config.rotation, 0.001f)
        assertTrue(config.isPortrait)
    }
    
    @Test
    fun `calculatePreviewConfiguration with custom camera size`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1920
            heightPixels = 1080
        }
        
        val cameraSize = Size(1280, 720) // 16:9
        val config = helper.calculatePreviewConfiguration(displayMetrics, cameraSize)
        
        assertNotNull(config)
        assertEquals(1920, config.width)
        assertEquals(1080, config.height)
        assertEquals(180f, config.rotation, 0.001f)
        assertFalse(config.isPortrait)
    }
    
    @Test
    fun `calculatePreviewConfiguration for ultra-wide display`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 3440
            heightPixels = 1440
        }
        
        val config = helper.calculatePreviewConfiguration(displayMetrics)
        
        assertNotNull(config)
        // Should be height-constrained
        assertEquals(1440, config.height)
        assertEquals(2560, config.width) // 1440 * 16/9
        assertEquals(180f, config.rotation, 0.001f)
        assertFalse(config.isPortrait)
    }
    
    @Test
    fun `calculatePreviewConfiguration for square display`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1000
            heightPixels = 1000
        }
        
        val config = helper.calculatePreviewConfiguration(displayMetrics)
        
        assertNotNull(config)
        // Square is treated as landscape
        assertFalse(config.isPortrait)
        assertEquals(180f, config.rotation, 0.001f)
        
        // Should fit within square bounds
        assertTrue(config.width <= 1000)
        assertTrue(config.height <= 1000)
    }
}