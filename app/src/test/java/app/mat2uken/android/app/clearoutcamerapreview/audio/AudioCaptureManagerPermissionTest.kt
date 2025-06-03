package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import junit.framework.TestCase.assertFalse

/**
 * This test is isolated to test permission state without static mocking of AudioTrack/AudioRecord
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class AudioCaptureManagerPermissionTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        // Ensure Looper is prepared for the test
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        context = mockk(relaxed = true)
        
        // Only mock ContextCompat for permission checking
        mockkStatic(ContextCompat::class)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test initial state without permission`() = runBlocking {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val manager = AudioCaptureManager(context, null)
        
        // Then
        val state = manager.state.first()
        assertFalse(state.hasPermission)
        assertFalse(state.isCapturing)
        assertFalse(state.isPlaying)
        
        // Clean up
        manager.release()
    }
}