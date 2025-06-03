package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue

// This test class has been disabled due to MockK static mocking conflicts.
// All tests have been moved to AudioCaptureManagerIsolatedTest.kt
/*
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class AudioCaptureManagerTest {
    
    private lateinit var context: Context
    private lateinit var audioCaptureManager: AudioCaptureManager
    
    @Before
    fun setup() {
        // Ensure Looper is prepared for the test
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        context = mockk(relaxed = true)
        
        // Mock ContextCompat for permission checking
        mockkStatic(ContextCompat::class)
        
        // Mock AudioRecord buffer size
        mockkStatic(AudioRecord::class)
        every { 
            AudioRecord.getMinBufferSize(any(), any(), any()) 
        } returns 4096
        
        // Mock AudioTrack class with default expectation
        mockkStatic(AudioTrack::class)
        every { 
            AudioTrack.getMinBufferSize(any(), any(), any()) 
        } returns 4096
    }
    
    @After
    fun tearDown() {
        if (::audioCaptureManager.isInitialized) {
            audioCaptureManager.release()
        }
        unmockkAll()
    }
    
    // NOTE: "test initial state with permission granted" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test initial state without permission" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test start audio capture without permission" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test stop audio capture updates state" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test update permission state" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test buffer size calculation with invalid values" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test toggle mute functionality" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test setOutputDevice stores selected device" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test setOutputDevice with null clears selection" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test audio state updates when device monitor reports changes" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test multiple start calls don't create multiple capture sessions" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test stop when not capturing doesn't cause errors" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
    
    // NOTE: "test release cleans up resources" test has been moved to 
    // AudioCaptureManagerIsolatedTest.kt to avoid MockK static mocking conflicts
}
*/