package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AudioCaptureManagerTest {
    
    private lateinit var context: Context
    private lateinit var audioCaptureManager: AudioCaptureManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        
        // Mock permission check
        every { 
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // Mock AudioRecord buffer size
        mockkStatic(AudioRecord::class)
        every { 
            AudioRecord.getMinBufferSize(any(), any(), any()) 
        } returns 4096
        
        // Mock AudioTrack buffer size
        mockkStatic(AudioTrack::class)
        every { 
            AudioTrack.getMinBufferSize(any(), any(), any()) 
        } returns 4096
        
        audioCaptureManager = AudioCaptureManager(context, null)
    }
    
    @After
    fun tearDown() {
        audioCaptureManager.release()
        unmockkAll()
    }
    
    @Test
    fun `test initial state with permission granted`() = runBlocking {
        // Then
        val state = audioCaptureManager.state.first()
        assertTrue(state.hasPermission)
        assertFalse(state.isCapturing)
        assertFalse(state.isPlaying)
        assertEquals(null, state.error)
    }
    
    @Test
    fun `test initial state without permission`() = runBlocking {
        // Given
        every { 
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val manager = AudioCaptureManager(context, null)
        
        // Then
        val state = manager.state.first()
        assertFalse(state.hasPermission)
        assertFalse(state.isCapturing)
        assertFalse(state.isPlaying)
    }
    
    @Test
    fun `test start audio capture without permission`() = runBlocking {
        // Given
        every { 
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_DENIED
        val manager = AudioCaptureManager(context, null)
        
        // When
        manager.startAudioCapture()
        
        // Then
        val state = manager.state.first()
        assertFalse(state.isCapturing)
        assertEquals("Audio permission required", state.error)
    }
    
    @Test
    fun `test stop audio capture updates state`() = runBlocking {
        // When
        audioCaptureManager.stopAudioCapture()
        
        // Then
        val state = audioCaptureManager.state.first()
        assertFalse(state.isCapturing)
        assertFalse(state.isPlaying)
    }
    
    @Test
    fun `test update permission state`() = runBlocking {
        // Given
        every { 
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_DENIED
        val manager = AudioCaptureManager(context, null)
        
        // Verify initial state
        assertFalse(manager.state.first().hasPermission)
        
        // When permission is granted
        every { 
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_GRANTED
        manager.updatePermissionState()
        
        // Then
        assertTrue(manager.state.first().hasPermission)
    }
    
    @Test
    fun `test buffer size calculation with invalid values`() = runBlocking {
        // Given
        every { 
            AudioRecord.getMinBufferSize(any(), any(), any()) 
        } returns AudioRecord.ERROR_BAD_VALUE
        
        every { 
            AudioTrack.getMinBufferSize(any(), any(), any()) 
        } returns AudioTrack.ERROR
        
        // When
        val manager = AudioCaptureManager(context, null)
        
        // Then - should not crash and use default values
        assertTrue(manager.state.first().hasPermission)
    }
}