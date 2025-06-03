package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

private const val TAG = "AudioConfigurationHelper"

data class AudioConfiguration(
    val sampleRate: Int,
    val channelConfig: Int,
    val channelCount: Int,
    val encoding: Int = AudioFormat.ENCODING_PCM_16BIT
)

/**
 * Helper class to determine optimal audio configuration based on hardware capabilities
 */
object AudioConfigurationHelper {
    
    // Preferred sample rates in order of preference
    private val PREFERRED_SAMPLE_RATES = intArrayOf(48000, 44100)
    
    // Common sample rates to check if preferred ones aren't available
    private val COMMON_SAMPLE_RATES = intArrayOf(
        48000, 44100, 32000, 24000, 22050, 16000, 11025, 8000
    )
    
    /**
     * Get the optimal audio configuration for recording based on hardware capabilities
     */
    fun getOptimalRecordingConfiguration(context: Context): AudioConfiguration {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // First, try to get sample rate from AudioManager
        val nativeSampleRate = getNativeSampleRate(audioManager)
        Log.d(TAG, "Native sample rate from AudioManager: $nativeSampleRate")
        
        // Determine channel configuration
        val (channelConfig, channelCount) = getOptimalChannelConfiguration()
        
        // Determine sample rate
        val sampleRate = getOptimalSampleRate(channelConfig, nativeSampleRate)
        
        return AudioConfiguration(
            sampleRate = sampleRate,
            channelConfig = channelConfig,
            channelCount = channelCount,
            encoding = AudioFormat.ENCODING_PCM_16BIT
        ).also {
            Log.d(TAG, "Optimal recording configuration: " +
                    "sampleRate=$sampleRate, " +
                    "channels=${if (channelCount == 2) "STEREO" else "MONO"}, " +
                    "encoding=16BIT_PCM")
        }
    }
    
    /**
     * Get native sample rate from AudioManager
     */
    private fun getNativeSampleRate(audioManager: AudioManager): Int? {
        return try {
            val sampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            sampleRateStr?.toIntOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting native sample rate", e)
            null
        }
    }
    
    /**
     * Determine optimal channel configuration (stereo if supported, otherwise mono)
     */
    private fun getOptimalChannelConfiguration(): Pair<Int, Int> {
        // Try stereo first
        val stereoConfig = AudioFormat.CHANNEL_IN_STEREO
        val stereoBufferSize = AudioRecord.getMinBufferSize(
            44100, // Use common sample rate for testing
            stereoConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        if (stereoBufferSize > 0 && stereoBufferSize != AudioRecord.ERROR_BAD_VALUE) {
            // Test if we can actually create a stereo AudioRecord
            if (canCreateAudioRecord(44100, stereoConfig)) {
                Log.d(TAG, "Device supports stereo recording")
                return Pair(stereoConfig, 2)
            }
        }
        
        // Fall back to mono
        Log.d(TAG, "Device only supports mono recording")
        return Pair(AudioFormat.CHANNEL_IN_MONO, 1)
    }
    
    /**
     * Determine optimal sample rate based on preferences and hardware support
     */
    private fun getOptimalSampleRate(channelConfig: Int, nativeSampleRate: Int?): Int {
        // If native sample rate is one of our preferred rates, use it
        if (nativeSampleRate != null && nativeSampleRate in PREFERRED_SAMPLE_RATES) {
            if (isSampleRateSupported(nativeSampleRate, channelConfig)) {
                Log.d(TAG, "Using native sample rate: $nativeSampleRate")
                return nativeSampleRate
            }
        }
        
        // Try preferred sample rates
        for (rate in PREFERRED_SAMPLE_RATES) {
            if (isSampleRateSupported(rate, channelConfig)) {
                Log.d(TAG, "Using preferred sample rate: $rate")
                return rate
            }
        }
        
        // If preferred rates aren't supported, find the highest supported rate
        val supportedRates = COMMON_SAMPLE_RATES.filter { rate ->
            isSampleRateSupported(rate, channelConfig)
        }.sortedDescending()
        
        if (supportedRates.isNotEmpty()) {
            val highestRate = supportedRates.first()
            Log.d(TAG, "Using highest available sample rate: $highestRate")
            return highestRate
        }
        
        // Fallback to 44100 if nothing else works
        Log.w(TAG, "No supported sample rates found, using fallback: 44100")
        return 44100
    }
    
    /**
     * Check if a sample rate is supported for recording
     */
    private fun isSampleRateSupported(sampleRate: Int, channelConfig: Int): Boolean {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        if (bufferSize <= 0 || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return false
        }
        
        // Try to create an AudioRecord instance to verify support
        return canCreateAudioRecord(sampleRate, channelConfig)
    }
    
    /**
     * Test if we can actually create an AudioRecord with given parameters
     */
    private fun canCreateAudioRecord(sampleRate: Int, channelConfig: Int): Boolean {
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (bufferSize <= 0) return false
            
            val audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }
            
            val isInitialized = audioRecord.state == AudioRecord.STATE_INITIALIZED
            audioRecord.release()
            
            isInitialized
        } catch (e: Exception) {
            Log.w(TAG, "Cannot create AudioRecord with sampleRate=$sampleRate, channelConfig=$channelConfig", e)
            false
        }
    }
    
    /**
     * Get the corresponding output channel configuration for playback
     */
    fun getOutputChannelConfig(inputChannelCount: Int): Int {
        return if (inputChannelCount == 2) {
            AudioFormat.CHANNEL_OUT_STEREO
        } else {
            AudioFormat.CHANNEL_OUT_MONO
        }
    }
}