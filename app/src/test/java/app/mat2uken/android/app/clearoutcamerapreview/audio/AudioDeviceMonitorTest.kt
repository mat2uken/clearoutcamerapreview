package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class AudioDeviceMonitorTest {
    
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var audioDeviceMonitor: AudioDeviceMonitor
    
    @Before
    fun setup() {
        // Ensure Looper is prepared for the test
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        
        context = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        
        // Mock the callback registration
        every { audioManager.registerAudioDeviceCallback(any(), any()) } just Runs
        
        // Default to no external devices
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns emptyArray()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // Manually trigger update to simulate callback
        audioDeviceMonitor.javaClass.getDeclaredMethod("updateAudioDeviceState").apply {
            isAccessible = true
            invoke(audioDeviceMonitor)
        }
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        audioDeviceMonitor.startMonitoring()
        
        // Verify initial state has external device
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
        
        // When device is removed
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns emptyArray()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // Manually trigger update to simulate callback
        audioDeviceMonitor.javaClass.getDeclaredMethod("updateAudioDeviceState").apply {
            isAccessible = true
            invoke(audioDeviceMonitor)
        }
        
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
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
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
    
    @Test
    fun `test available output devices tracking`() = runBlocking {
        // Given
        val devices = arrayOf(
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "Speaker"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, true, "Headphones"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true, "Bluetooth"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_MIC, false, "Microphone") // Not a sink
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns devices
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then
        val availableDevices = audioDeviceMonitor.availableOutputDevices.first()
        assertEquals(3, availableDevices.size) // Only sinks should be included
        assertTrue(availableDevices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER })
        assertTrue(availableDevices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES })
        assertTrue(availableDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP })
    }
    
    @Test
    fun `test device display name formatting`() = runBlocking {
        // Test various device types
        val testCases = listOf(
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "") to "Built-in Speaker",
            createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET, true, "My Headset") to "My Headset (Wired Headset)",
            createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true, "Sony WH-1000XM4") to "Sony WH-1000XM4 (Bluetooth)",
            createMockAudioDevice(AudioDeviceInfo.TYPE_USB_DEVICE, true, "") to "USB",
            createMockAudioDevice(AudioDeviceInfo.TYPE_HDMI, true, "LG TV") to "LG TV (HDMI)"
        )
        
        for ((device, expectedName) in testCases) {
            val displayName = audioDeviceMonitor.getDeviceDisplayName(device)
            assertEquals(expectedName, displayName)
        }
    }
    
    @Test
    fun `test microphone name formatting`() = runBlocking {
        // Test various microphone types
        val builtInMic = createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_MIC, false, "")
        assertEquals("Built-in Microphone", audioDeviceMonitor.getMicrophoneName(builtInMic))
        
        val headsetMic = createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET, false, "")
        assertEquals("Headset Microphone", audioDeviceMonitor.getMicrophoneName(headsetMic))
        
        val bluetoothMic = createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, false, "")
        assertEquals("Bluetooth Microphone", audioDeviceMonitor.getMicrophoneName(bluetoothMic))
        
        val usbMic = createMockAudioDevice(AudioDeviceInfo.TYPE_USB_DEVICE, false, "Blue Yeti")
        assertEquals("Blue Yeti", audioDeviceMonitor.getMicrophoneName(usbMic))
        
        // Test null device
        assertEquals("No Microphone", audioDeviceMonitor.getMicrophoneName(null))
    }
    
    @Test
    fun `test output device name formatting`() = runBlocking {
        // Test various output device types
        val speaker = createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "")
        assertEquals("Built-in Speaker", audioDeviceMonitor.getOutputDeviceName(speaker))
        
        val headphones = createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, true, "")
        assertEquals("Wired Headphones", audioDeviceMonitor.getOutputDeviceName(headphones))
        
        val bluetoothSpeaker = createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true, "JBL Flip")
        assertEquals("JBL Flip", audioDeviceMonitor.getOutputDeviceName(bluetoothSpeaker))
        
        // Test null device
        assertEquals("No Output", audioDeviceMonitor.getOutputDeviceName(null))
    }
    
    @Test
    fun `test device hot-plug scenario`() = runBlocking {
        // Given - Start with only built-in speaker
        val speaker = createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "Speaker")
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speaker)
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        audioDeviceMonitor.startMonitoring()
        
        // Verify initial state
        assertFalse(audioDeviceMonitor.hasExternalAudioOutput.first())
        
        // When - Simulate headphones being plugged in
        val headphones = createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, true, "Headphones")
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speaker, headphones)
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // Manually trigger the internal update to simulate callback
        // This tests the logic without needing to capture the actual callback
        audioDeviceMonitor.javaClass.getDeclaredMethod("updateAudioDeviceState").apply {
            isAccessible = true
            invoke(audioDeviceMonitor)
        }
        
        // Then - Should detect external audio
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
        
        // When - Simulate headphones being unplugged
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speaker)
        
        // Manually trigger update again
        audioDeviceMonitor.javaClass.getDeclaredMethod("updateAudioDeviceState").apply {
            isAccessible = true
            invoke(audioDeviceMonitor)
        }
        
        // Then - Should no longer have external audio
        assertFalse(audioDeviceMonitor.hasExternalAudioOutput.first())
    }
    
    @Test
    fun `test current device tracking`() = runBlocking {
        // Given - Multiple devices with different priorities
        val devices = arrayOf(
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "Speaker"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, true, "Headphones"),
            createMockAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true, "Bluetooth")
        )
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns devices
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns arrayOf(
            createMockAudioDevice(AudioDeviceInfo.TYPE_BUILTIN_MIC, false, "Microphone")
        )
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then - Should prefer external devices
        val currentOutput = audioDeviceMonitor.currentOutputDevice.first()
        assertNotNull(currentOutput)
        assertTrue(currentOutput!!.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        
        // Should have built-in mic as current input
        val currentInput = audioDeviceMonitor.currentInputDevice.first()
        assertNotNull(currentInput)
        assertEquals(AudioDeviceInfo.TYPE_BUILTIN_MIC, currentInput!!.type)
    }
    
    @Test
    fun `test unknown device type handling`() = runBlocking {
        // Given - Device with unknown type
        val unknownDevice = mockk<AudioDeviceInfo> {
            every { type } returns 999 // Unknown type
            every { isSink } returns true
            every { productName } returns "Mystery Device"
        }
        
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(unknownDevice)
        every { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) } returns emptyArray()
        
        // When
        audioDeviceMonitor.startMonitoring()
        
        // Then - Should treat unknown device as external
        assertTrue(audioDeviceMonitor.hasExternalAudioOutput.first())
        
        // Display name should handle unknown type
        val displayName = audioDeviceMonitor.getDeviceDisplayName(unknownDevice)
        assertEquals("Mystery Device (Audio Device)", displayName)
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
            every { isSource } returns !isSink
        }
    }
    
    // Add missing import
    private fun assertNotNull(value: Any?) {
        if (value == null) {
            throw AssertionError("Expected non-null value")
        }
    }
}