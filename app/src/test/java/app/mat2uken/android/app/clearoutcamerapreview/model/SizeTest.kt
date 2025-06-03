package app.mat2uken.android.app.clearoutcamerapreview.model

import org.junit.Assert.*
import org.junit.Test

class SizeTest {
    
    @Test
    fun `constructor creates valid size`() {
        val size = Size(1920, 1080)
        assertEquals(1920, size.width)
        assertEquals(1080, size.height)
    }
    
    @Test
    fun `constructor accepts zero dimensions`() {
        val size1 = Size(0, 0)
        assertEquals(0, size1.width)
        assertEquals(0, size1.height)
        
        val size2 = Size(100, 0)
        assertEquals(100, size2.width)
        assertEquals(0, size2.height)
        
        val size3 = Size(0, 100)
        assertEquals(0, size3.width)
        assertEquals(100, size3.height)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative width`() {
        Size(-1, 100)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative height`() {
        Size(100, -1)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative dimensions`() {
        Size(-1, -1)
    }
    
    @Test
    fun `toString formats correctly`() {
        val size = Size(1920, 1080)
        assertEquals("1920x1080", size.toString())
    }
    
    @Test
    fun `data class equality works correctly`() {
        val size1 = Size(1920, 1080)
        val size2 = Size(1920, 1080)
        val size3 = Size(1280, 720)
        
        assertEquals(size1, size2)
        assertNotEquals(size1, size3)
    }
    
    @Test
    fun `data class copy works correctly`() {
        val original = Size(1920, 1080)
        val copied = original.copy()
        val modified = original.copy(width = 1280)
        
        assertEquals(original, copied)
        assertEquals(1280, modified.width)
        assertEquals(1080, modified.height)
    }
    
    // Note: Tests for fromAndroidSize and toAndroidSize are not included
    // because android.util.Size is not available in unit test environment.
    // These would need to be tested in instrumented tests.
}