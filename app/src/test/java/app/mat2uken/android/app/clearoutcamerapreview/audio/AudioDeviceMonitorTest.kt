package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
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
import junit.framework.TestCase.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AudioDeviceMonitorTest {
    
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var audioDeviceMonitor: AudioDeviceMonitor
    private lateinit var capturedCallback: AudioDeviceCallback
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        
        // Capture the callback when registered
        val slot = slot<AudioDeviceCallback>()
        every { audioManager.registerAudioDeviceCallback(capture(slot), any()) } answers {
            capturedCallback = slot.captured
            // Return nothing
        }
        
        // Default to no external devices
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns emptyArray()
        
        audioDeviceMonitor = AudioDeviceMonitor(context)
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `test initial state with no external devices`() = runBlocking {
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        assertFalse(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test detection of wired headphones`() = runBlocking {
        // Given
        val headphoneDevice = createMockAudioDevice(
            type = AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            isSink = true,
            productName = "Test Headphones"
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(headphoneDevice)
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test detection of bluetooth headset`() = runBlocking {
        // Given
        val bluetoothDevice = createMockAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            isSink = true,
            productName = "Bluetooth Headset"
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(bluetoothDevice)
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test built-in speaker is not external`() = runBlocking {
        // Given
        val speakerDevice = createMockAudioDevice(
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            isSink = true,
            productName = "Built-in Speaker"
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speakerDevice)
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        assertFalse(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test device added callback`() = runBlocking {
        // Given
        audioDeviceMonitor.startMonitoring()
        
        val headphoneDevice = createMockAudioDevice(
            type = AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            isSink = true,
            productName = "Added Headphones"
        )
        
        // When device is added
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(headphoneDevice)
        capturedCallback.onAudioDevicesAdded(arrayOf(headphoneDevice))
        
        // Then
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test device removed callback`() = runBlocking {
        // Given
        val headphoneDevice = createMockAudioDevice(
            type = AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            isSink = true,
            productName = "Headphones"
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(headphoneDevice)
        audioDeviceMonitor.startMonitoring()
        
        // Verify initial state has external device
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
        
        // When device is removed
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns emptyArray()
        capturedCallback.onAudioDevicesRemoved(arrayOf(headphoneDevice))
        
        // Then
        assertFalse(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test multiple external devices`() = runBlocking {
        // Given
        val devices = arrayOf(
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "Speaker"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, true, "Headphones"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true, "Bluetooth")
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns devices
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test stop monitoring unregisters callback`() {
        // Given
        audioDeviceMonitor.startMonitoring()
        
        // When
        audioDeviceMonitor.stopMonitoring()
        
        // Then
        verify { audioManager.unregisterAudioDeviceCallback(any()) }
    }
    
    private fun createMockAudioDevice(
        type: Int,
        isSink: Boolean,
        productName: String
    ): AudioDeviceInfo {
        return mockk {
            every { getType() } returns type
            every { this@mockk.isSink } returns isSink
            every { this@mockk.productName } returns productName
        }
    }
}