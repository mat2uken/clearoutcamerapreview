package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioDeviceSelectionTest {
    
    private lateinit var context: android.content.Context
    private lateinit var mockAudioManager: AudioManager
    private lateinit var audioDeviceMonitor: AudioDeviceMonitor
    private lateinit var audioCaptureManager: AudioCaptureManager
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockAudioManager = mockk(relaxed = true)
        
        // Mock the AudioManager service
        every { context.getSystemService(android.content.Context.AUDIO_SERVICE) } returns mockAudioManager
        
        audioDeviceMonitor = AudioDeviceMonitor(context)
        audioCaptureManager = AudioCaptureManager(context, audioDeviceMonitor)
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `test available output devices are tracked correctly`() = runTest {
        // Create mock audio devices
        val mockSpeaker = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            every { productName } returns "Built-in Speaker"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        val mockBluetoothDevice = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            every { productName } returns "Bluetooth Headphones"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        val mockUsbDevice = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_USB_DEVICE
            every { productName } returns "USB Audio Device"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        // Mock getDevices to return our test devices
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            mockSpeaker,
            mockBluetoothDevice,
            mockUsbDevice
        )
        
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // Start monitoring
        audioDeviceMonitor.startMonitoring()
        
        // Get available devices
        val availableDevices = audioDeviceMonitor.availableOutputDevices.first()
        
        // Verify all devices are available
        assertEquals(3, availableDevices.size)
        assert(availableDevices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER })
        assert(availableDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP })
        assert(availableDevices.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE })
    }
    
    @Test
    fun `test output device selection updates state correctly`() = runTest {
        // Create mock audio devices
        val mockSpeaker = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            every { productName } returns "Built-in Speaker"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        val mockHeadphones = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            every { productName } returns "Wired Headphones"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        // Mock getDevices
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            mockSpeaker,
            mockHeadphones
        )
        
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // Start monitoring
        audioDeviceMonitor.startMonitoring()
        
        // Select headphones
        audioCaptureManager.setOutputDevice(mockHeadphones)
        
        // Check that state is updated
        val state = audioCaptureManager.state.first()
        assertEquals("Wired Headphones", state.outputDeviceName)
    }
    
    @Test
    fun `test device display names are formatted correctly`() {
        // Create mock devices with different configurations
        val deviceWithProductName = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            every { productName } returns "Sony WH-1000XM4"
            every { isSink } returns true
        }
        
        val deviceWithoutProductName = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_USB_HEADSET
            every { productName } returns ""
            every { isSink } returns true
        }
        
        // Test display names
        val displayName1 = audioDeviceMonitor.getDeviceDisplayName(deviceWithProductName)
        assertEquals("Sony WH-1000XM4 (Bluetooth)", displayName1)
        
        val displayName2 = audioDeviceMonitor.getDeviceDisplayName(deviceWithoutProductName)
        assertEquals("USB Headset", displayName2)
    }
    
    @Test
    fun `test external audio detection works correctly`() = runTest {
        // Test with only built-in speaker
        val mockSpeaker = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            every { productName } returns "Built-in Speaker"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(mockSpeaker)
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        audioDeviceMonitor.startMonitoring()
        
        // Should not have external audio
        val hasExternal1 = audioDeviceMonitor.hasExternalAudioOutput.first()
        assertEquals(false, hasExternal1)
        
        // Add external device
        val mockHeadphones = mockk<AudioDeviceInfo> {
            every { type } returns AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            every { productName } returns "Headphones"
            every { isSink } returns true
            every { isSource } returns false
        }
        
        every { mockAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            mockSpeaker,
            mockHeadphones
        )
        
        // Manually trigger update (in real scenario, this would be triggered by AudioDeviceCallback)
        val updateMethod = audioDeviceMonitor.javaClass.getDeclaredMethod("updateAudioDeviceState")
        updateMethod.isAccessible = true
        updateMethod.invoke(audioDeviceMonitor)
        
        // Should now have external audio
        val hasExternal2 = audioDeviceMonitor.hasExternalAudioOutput.first()
        assertEquals(true, hasExternal2)
    }
}