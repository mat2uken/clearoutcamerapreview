package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.AudioTrack
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import junit.framework.TestCase.assertTrue

/**
 * This test is isolated to test buffer size calculation with invalid values
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class AudioCaptureManagerBufferTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        // Clear all mocks before test
        clearAllMocks()
        
        // Ensure Looper is prepared for the test
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        context = mockk(relaxed = true)
        
        // Mock ContextCompat for permission checking
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // Mock AudioRecord and AudioTrack to return error values
        mockkStatic(AudioRecord::class)
        every { 
            AudioRecord.getMinBufferSize(any(), any(), any()) 
        } returns AudioRecord.ERROR_BAD_VALUE
        
        mockkStatic(AudioTrack::class)
        every { 
            AudioTrack.getMinBufferSize(any<Int>(), any<Int>(), any<Int>()) 
        } returns AudioTrack.ERROR
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test buffer size calculation with invalid values`() = runBlocking {
        // When
        val manager = AudioCaptureManager(context, null)
        
        // Then - should not crash and use default values
        assertTrue(manager.state.first().hasPermission)
        
        // Clean up
        manager.release()
    }
}