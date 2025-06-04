package app.mat2uken.android.app.clearoutcamerapreview.model

import android.util.Range
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraFormatTest {
    
    private fun createRange(lower: Int, upper: Int): Range<Int> {
        return Range(lower, upper)
    }
    
    @Test
    fun `maxFrameRate returns highest upper bound from frame rate ranges`() {
        val size = Size(1920, 1080)
        val frameRateRanges = listOf(
            createRange(15, 30),
            createRange(30, 60),
            createRange(8, 15)
        )
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(60, format.maxFrameRate)
    }
    
    @Test
    fun `maxFrameRate returns 0 for empty frame rate ranges`() {
        val size = Size(1920, 1080)
        val frameRateRanges = emptyList<Range<Int>>()
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(0, format.maxFrameRate)
    }
    
    @Test
    fun `supportsFrameRate returns true when fps is within range`() {
        val size = Size(1920, 1080)
        val frameRateRanges = listOf(
            createRange(15, 30),
            createRange(30, 60)
        )
        val format = CameraFormat(size, frameRateRanges)
        
        assertTrue(format.supportsFrameRate(20)) // Within first range
        assertTrue(format.supportsFrameRate(30)) // At boundary (both ranges)
        assertTrue(format.supportsFrameRate(45)) // Within second range
        assertTrue(format.supportsFrameRate(15)) // At lower boundary
        assertTrue(format.supportsFrameRate(60)) // At upper boundary
    }
    
    @Test
    fun `supportsFrameRate returns false when fps is outside ranges`() {
        val size = Size(1920, 1080)
        val frameRateRanges = listOf(
            createRange(15, 30),
            createRange(40, 60)
        )
        val format = CameraFormat(size, frameRateRanges)
        
        assertFalse(format.supportsFrameRate(10)) // Below all ranges
        assertFalse(format.supportsFrameRate(35)) // Between ranges
        assertFalse(format.supportsFrameRate(65)) // Above all ranges
    }
    
    @Test
    fun `supportsFrameRate returns false for empty frame rate ranges`() {
        val size = Size(1920, 1080)
        val frameRateRanges = emptyList<Range<Int>>()
        val format = CameraFormat(size, frameRateRanges)
        
        assertFalse(format.supportsFrameRate(30))
    }
    
    @Test
    fun `getBestFrameRateRange returns exact match when fps is within range`() {
        val size = Size(1920, 1080)
        val range1 = createRange(15, 30)
        val range2 = createRange(30, 60)
        val frameRateRanges = listOf(range1, range2)
        val format = CameraFormat(size, frameRateRanges)
        
        // Should return first matching range
        assertEquals(range1, format.getBestFrameRateRange(25))
        assertEquals(range2, format.getBestFrameRateRange(45))
    }
    
    @Test
    fun `getBestFrameRateRange returns range containing fps when multiple ranges match`() {
        val size = Size(1920, 1080)
        val range1 = createRange(15, 30)
        val range2 = createRange(30, 60)
        val frameRateRanges = listOf(range1, range2)
        val format = CameraFormat(size, frameRateRanges)
        
        // fps=30 is in both ranges, should return first match
        assertEquals(range1, format.getBestFrameRateRange(30))
    }
    
    @Test
    fun `getBestFrameRateRange returns closest range when fps is outside all ranges`() {
        val size = Size(1920, 1080)
        val range1 = createRange(15, 30)  // distance from 35: |30-35| = 5
        val range2 = createRange(40, 60)  // distance from 35: |40-35| = 5, but upper bound |60-35| = 25
        val range3 = createRange(32, 38)  // distance from 35: |38-35| = 3 (closest)
        val frameRateRanges = listOf(range1, range2, range3)
        val format = CameraFormat(size, frameRateRanges)
        
        // Should return range3 as it has the closest upper bound to 35
        assertEquals(range3, format.getBestFrameRateRange(35))
    }
    
    @Test
    fun `getBestFrameRateRange returns closest range for fps below all ranges`() {
        val size = Size(1920, 1080)
        val range1 = createRange(20, 30)  // distance from 10: |30-10| = 20
        val range2 = createRange(15, 25)  // distance from 10: |25-10| = 15 (closest)
        val frameRateRanges = listOf(range1, range2)
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(range2, format.getBestFrameRateRange(10))
    }
    
    @Test
    fun `getBestFrameRateRange returns closest range for fps above all ranges`() {
        val size = Size(1920, 1080)
        val range1 = createRange(15, 30)  // distance from 70: |30-70| = 40
        val range2 = createRange(40, 60)  // distance from 70: |60-70| = 10 (closest)
        val frameRateRanges = listOf(range1, range2)
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(range2, format.getBestFrameRateRange(70))
    }
    
    @Test
    fun `getBestFrameRateRange returns null for empty frame rate ranges`() {
        val size = Size(1920, 1080)
        val frameRateRanges = emptyList<Range<Int>>()
        val format = CameraFormat(size, frameRateRanges)
        
        assertNull(format.getBestFrameRateRange(30))
    }
    
    @Test
    fun `getBestFrameRateRange returns single range when only one available`() {
        val size = Size(1920, 1080)
        val range = createRange(30, 60)
        val frameRateRanges = listOf(range)
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(range, format.getBestFrameRateRange(10)) // Below range
        assertEquals(range, format.getBestFrameRateRange(45)) // Within range
        assertEquals(range, format.getBestFrameRateRange(70)) // Above range
    }
    
    @Test
    fun `CameraFormat data class properties are accessible`() {
        val size = Size(1920, 1080)
        val frameRateRanges = listOf(createRange(30, 60))
        val format = CameraFormat(size, frameRateRanges)
        
        assertEquals(size, format.size)
        assertEquals(frameRateRanges, format.frameRateRanges)
        assertEquals(1920, format.size.width)
        assertEquals(1080, format.size.height)
    }
}