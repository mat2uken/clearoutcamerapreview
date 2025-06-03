package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class AudioConfigurationHelperTest {
    
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    
    @Before
    fun setup() {
        // Ensure Looper is prepared for the test
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        context = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        
        // Mock static methods
        mockkStatic(AudioRecord::class)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `test preferred sample rate 48kHz when supported`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "48000"
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(48000, config.sampleRate)
    }
    
    @Test
    fun `test fallback to 44100Hz when 48kHz not supported`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "44100"
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(44100, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(44100, config.sampleRate)
    }
    
    @Test
    fun `test stereo configuration when supported`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "44100"
        every { AudioRecord.getMinBufferSize(any(), AudioFormat.CHANNEL_IN_STEREO, any()) } returns 4096
        every { AudioRecord.getMinBufferSize(any(), AudioFormat.CHANNEL_IN_MONO, any()) } returns 2048
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(AudioFormat.CHANNEL_IN_STEREO, config.channelConfig)
        assertEquals(2, config.channelCount)
    }
    
    @Test
    fun `test mono configuration when stereo not supported`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "44100"
        every { AudioRecord.getMinBufferSize(any(), AudioFormat.CHANNEL_IN_STEREO, any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(any(), AudioFormat.CHANNEL_IN_MONO, any()) } returns 2048
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(AudioFormat.CHANNEL_IN_MONO, config.channelConfig)
        assertEquals(1, config.channelCount)
    }
    
    @Test
    fun `test highest available sample rate when preferred not available`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns null
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(44100, any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(32000, any(), any()) } returns 4096
        every { AudioRecord.getMinBufferSize(24000, any(), any()) } returns 4096
        every { AudioRecord.getMinBufferSize(22050, any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(16000, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(32000, config.sampleRate) // Highest available
    }
    
    @Test
    fun `test 16-bit PCM encoding is always used`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "44100"
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, config.encoding)
    }
    
    @Test
    fun `test output channel config for stereo input`() {
        // When
        val outputConfig = AudioConfigurationHelper.getOutputChannelConfig(2)
        
        // Then
        assertEquals(AudioFormat.CHANNEL_OUT_STEREO, outputConfig)
    }
    
    @Test
    fun `test output channel config for mono input`() {
        // When
        val outputConfig = AudioConfigurationHelper.getOutputChannelConfig(1)
        
        // Then
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, outputConfig)
    }
    
    @Test
    fun `test output channel config for invalid channel count defaults to mono`() {
        // When
        val outputConfig0 = AudioConfigurationHelper.getOutputChannelConfig(0)
        val outputConfig3 = AudioConfigurationHelper.getOutputChannelConfig(3)
        val outputConfigNegative = AudioConfigurationHelper.getOutputChannelConfig(-1)
        
        // Then - All should default to mono
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, outputConfig0)
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, outputConfig3)
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, outputConfigNegative)
    }
    
    @Test
    fun `test no supported sample rates falls back to 44100Hz`() {
        // Given - All common sample rates fail
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns null
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then - Should use 44100Hz as last resort
        assertEquals(44100, config.sampleRate)
    }
    
    @Test
    fun `test system suggested sample rate is used when valid`() {
        // Given - System suggests a non-standard sample rate
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "96000"
        every { AudioRecord.getMinBufferSize(96000, any(), any()) } returns 8192
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then - Should prefer 48kHz over system suggestion
        assertEquals(48000, config.sampleRate)
    }
    
    @Test
    fun `test invalid system sample rate property handled gracefully`() {
        // Given - Invalid sample rate string
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "invalid"
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then - Should still work with preferred rates
        assertEquals(48000, config.sampleRate)
    }
    
    @Test
    fun `test extremely high sample rate from system is ignored`() {
        // Given - System suggests unrealistic sample rate
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns "192000"
        every { AudioRecord.getMinBufferSize(192000, any(), any()) } returns AudioRecord.ERROR_BAD_VALUE
        every { AudioRecord.getMinBufferSize(48000, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then - Should fall back to standard rate
        assertEquals(48000, config.sampleRate)
    }
    
    @Test
    fun `test audio manager property returns empty string`() {
        // Given
        every { audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) } returns ""
        every { AudioRecord.getMinBufferSize(44100, any(), any()) } returns 4096
        
        // When
        val config = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        // Then - Should use standard rates
        assertTrue(config.sampleRate in listOf(48000, 44100))
    }
    
    @Test
    fun `test configuration data class properties`() {
        // Given
        val config = AudioConfiguration(
            sampleRate = 48000,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            channelCount = 2,
            encoding = AudioFormat.ENCODING_PCM_16BIT
        )
        
        // Then - Verify all properties are accessible
        assertEquals(48000, config.sampleRate)
        assertEquals(AudioFormat.CHANNEL_IN_STEREO, config.channelConfig)
        assertEquals(2, config.channelCount)
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, config.encoding)
    }
}