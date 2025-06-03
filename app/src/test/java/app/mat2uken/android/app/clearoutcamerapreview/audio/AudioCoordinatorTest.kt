package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioManager
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AudioCoordinatorTest {
    
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var audioCoordinator: AudioCoordinator
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { audioManager.getDevices(any()) } returns emptyArray()
        every { audioManager.registerAudioDeviceCallback(any(), any()) } just Runs
        
        // Mock permission as granted
        every { context.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        audioCoordinator = AudioCoordinator(context)
    }
    
    @After
    fun tearDown() {
        audioCoordinator.release()
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `test initial state`() = testScope.runTest {
        // When
        val state = audioCoordinator.audioState.first()
        
        // Then
        assertFalse(state.hasExternalOutput)
        assertFalse(state.isCapturing)
        assertFalse(state.isPlaying)
        assertTrue(state.hasPermission)
    }
    
    @Test
    fun `test start monitoring`() = testScope.runTest {
        // When
        audioCoordinator.start()
        
        // Then
        verify { audioManager.registerAudioDeviceCallback(any(), any()) }
    }
    
    @Test
    fun `test stop monitoring`() = testScope.runTest {
        // Given
        audioCoordinator.start()
        
        // When
        audioCoordinator.stop()
        
        // Then
        verify { audioManager.unregisterAudioDeviceCallback(any()) }
    }
    
    @Test
    fun `test manual toggle when no external output`() = testScope.runTest {
        // Given
        audioCoordinator.start()
        delay(100) // Allow state to settle
        
        // When
        audioCoordinator.toggleAudioCapture()
        delay(100)
        
        // Then
        val state = audioCoordinator.audioState.first()
        assertFalse(state.isCapturing) // Should not capture without external output
    }
}