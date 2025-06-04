package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.max

private const val TAG = "AudioCaptureManager"

data class AudioCaptureState(
    val isCapturing: Boolean = false,
    val isPlaying: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val isMuted: Boolean = false,
    val isManuallyMuted: Boolean = false,
    val microphoneName: String = "Unknown",
    val outputDeviceName: String = "Unknown"
)

/**
 * Manages audio capture from microphone and playback to output devices
 */
class AudioCaptureManager(
    private val context: Context,
    private val audioDeviceMonitor: AudioDeviceMonitor? = null
) {
    
    // Audio configuration
    private var audioConfig: AudioConfiguration? = null
    private var recordBufferSize = 0
    private var playbackBufferSize = 0
    
    // Audio components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var captureJob: Job? = null
    
    // State management
    private val _state = MutableStateFlow(AudioCaptureState())
    val state: StateFlow<AudioCaptureState> = _state.asStateFlow()
    
    // Manual mute control
    private var isManuallyMuted = false
    
    // Selected output device
    private var selectedOutputDevice: AudioDeviceInfo? = null
    
    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        checkAudioPermission()
    }
    
    /**
     * Check if audio recording permission is granted
     */
    private fun checkAudioPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        _state.value = _state.value.copy(hasPermission = hasPermission)
        
        if (!hasPermission) {
            Log.w(TAG, "Audio recording permission not granted")
        }
    }
    
    /**
     * Determine optimal audio configuration and calculate buffer sizes
     */
    private fun configureAudio() {
        // Get optimal configuration based on hardware capabilities
        audioConfig = AudioConfigurationHelper.getOptimalRecordingConfiguration(context)
        
        audioConfig?.let { config ->
            // Calculate buffer sizes
            recordBufferSize = AudioRecord.getMinBufferSize(
                config.sampleRate,
                config.channelConfig,
                config.encoding
            )
            
            val outputChannelConfig = AudioConfigurationHelper.getOutputChannelConfig(config.channelCount)
            playbackBufferSize = AudioTrack.getMinBufferSize(
                config.sampleRate,
                outputChannelConfig,
                config.encoding
            )
            
            // Ensure buffer sizes are valid
            if (recordBufferSize == AudioRecord.ERROR || recordBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                recordBufferSize = config.sampleRate * config.channelCount * 2 // 1 second of audio
                Log.e(TAG, "Invalid record buffer size, using default: $recordBufferSize")
            }
            
            if (playbackBufferSize == AudioTrack.ERROR || playbackBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                playbackBufferSize = config.sampleRate * config.channelCount * 2 // 1 second of audio
                Log.e(TAG, "Invalid playback buffer size, using default: $playbackBufferSize")
            }
            
            // Use larger buffers for better performance
            recordBufferSize = max(recordBufferSize, config.sampleRate * config.channelCount / 10) // At least 100ms
            playbackBufferSize = max(playbackBufferSize, config.sampleRate * config.channelCount / 10) // At least 100ms
            
            Log.d(TAG, "Audio configuration - Sample rate: ${config.sampleRate}Hz, " +
                    "Channels: ${config.channelCount}, " +
                    "Record buffer: $recordBufferSize, " +
                    "Playback buffer: $playbackBufferSize")
            
            _state.value = _state.value.copy(
                sampleRate = config.sampleRate,
                channelCount = config.channelCount
            )
            
            // Update device information
            updateDeviceInfo()
        }
    }
    
    /**
     * Start audio capture and playback
     */
    fun startAudioCapture() {
        if (!_state.value.hasPermission) {
            Log.e(TAG, "Cannot start audio capture without permission")
            _state.value = _state.value.copy(error = "Audio permission required")
            return
        }
        
        if (_state.value.isCapturing) {
            Log.w(TAG, "Audio capture already running")
            return
        }
        
        captureJob = audioScope.launch {
            try {
                configureAudio()
                initializeAudioComponents()
                startAudioLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio capture", e)
                _state.value = _state.value.copy(
                    error = "Audio capture error: ${e.message}",
                    isCapturing = false,
                    isPlaying = false
                )
                stopAudioCapture()
            }
        }
    }
    
    /**
     * Initialize AudioRecord and AudioTrack
     */
    @Suppress("MissingPermission") // Permission is checked before calling this method
    private suspend fun initializeAudioComponents() = withContext(Dispatchers.IO) {
        val config = audioConfig ?: throw IllegalStateException("Audio configuration not set")
        
        // Initialize AudioRecord
        audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(config.sampleRate)
                        .setChannelMask(config.channelConfig)
                        .setEncoding(config.encoding)
                        .build()
                )
                .setBufferSizeInBytes(recordBufferSize)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                config.sampleRate,
                config.channelConfig,
                config.encoding,
                recordBufferSize
            )
        }
        
        // Initialize AudioTrack
        val outputChannelConfig = AudioConfigurationHelper.getOutputChannelConfig(config.channelCount)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(config.sampleRate)
                        .setChannelMask(outputChannelConfig)
                        .setEncoding(config.encoding)
                        .build()
                )
                .setBufferSizeInBytes(playbackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
        } else {
            null
        }
        
        // Set preferred audio device if available and API level supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && selectedOutputDevice != null && builder != null) {
            try {
                // Note: setPreferredDevice is available from API 23
                // We'll need to use reflection or handle this differently for older versions
                audioTrack = builder.build()
                audioTrack?.preferredDevice = selectedOutputDevice
                Log.d(TAG, "Set preferred output device: ${selectedOutputDevice?.productName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set preferred device", e)
                audioTrack = builder.build()
            }
        } else if (builder != null) {
            audioTrack = builder.build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                config.sampleRate,
                outputChannelConfig,
                config.encoding,
                playbackBufferSize,
                AudioTrack.MODE_STREAM
            )
        }
        
        // Verify initialization
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord failed to initialize")
        }
        
        if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            throw IllegalStateException("AudioTrack failed to initialize")
        }
        
        Log.d(TAG, "Audio components initialized successfully")
    }
    
    /**
     * Main audio capture and playback loop
     */
    private suspend fun startAudioLoop() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(recordBufferSize)
        
        try {
            audioRecord?.startRecording()
            audioTrack?.play()
            
            // Check if we should mute based on external audio availability or manual mute
            val shouldMute = audioDeviceMonitor?.hasExternalAudioOutput?.value == false || isManuallyMuted
            
            _state.value = _state.value.copy(
                isCapturing = true,
                isPlaying = !shouldMute,
                isMuted = shouldMute,
                error = null
            )
            
            Log.d(TAG, "Started audio capture and playback (muted: $shouldMute)")
            
            while (isActive && _state.value.isCapturing) {
                // Read from microphone
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // Check current mute state based on external audio and manual mute
                    val hasExternalOutput = audioDeviceMonitor?.hasExternalAudioOutput?.value == true
                    val currentShouldMute = !hasExternalOutput || isManuallyMuted
                    
                    if (currentShouldMute != _state.value.isMuted) {
                        _state.value = _state.value.copy(
                            isPlaying = !currentShouldMute,
                            isMuted = currentShouldMute,
                            isManuallyMuted = isManuallyMuted
                        )
                        Log.d(TAG, "Audio mute state changed: $currentShouldMute (manual: $isManuallyMuted)")
                        updateDeviceInfo()
                    }
                    
                    // Write to audio output only if not muted
                    if (!currentShouldMute) {
                        audioTrack?.write(buffer, 0, bytesRead)
                    }
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading from AudioRecord: $bytesRead")
                    break
                }
                
                // Small yield to prevent blocking
                yield()
            }
        } finally {
            Log.d(TAG, "Audio loop ended")
        }
    }
    
    /**
     * Stop audio capture and playback
     */
    fun stopAudioCapture() {
        Log.d(TAG, "Stopping audio capture")
        
        captureJob?.cancel()
        captureJob = null
        
        // Stop and release AudioRecord
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
        
        // Stop and release AudioTrack
        audioTrack?.apply {
            if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                stop()
            }
            release()
        }
        audioTrack = null
        
        _state.value = _state.value.copy(
            isCapturing = false,
            isPlaying = false,
            isMuted = false
        )
        
        Log.d(TAG, "Audio capture stopped")
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        stopAudioCapture()
        audioScope.cancel()
    }
    
    /**
     * Update permission state (call this after permission is granted)
     */
    fun updatePermissionState() {
        checkAudioPermission()
    }
    
    /**
     * Toggle manual mute state
     */
    fun toggleMute() {
        isManuallyMuted = !isManuallyMuted
        Log.d(TAG, "Manual mute toggled: $isManuallyMuted")
        
        // Always update the manual mute state
        _state.value = _state.value.copy(
            isManuallyMuted = isManuallyMuted
        )
        
        if (_state.value.isCapturing) {
            audioDeviceMonitor?.let { monitor ->
                _state.value = _state.value.copy(
                    isMuted = !monitor.hasExternalAudioOutput.value || isManuallyMuted,
                    isPlaying = monitor.hasExternalAudioOutput.value && !isManuallyMuted
                )
            }
        }
    }
    
    /**
     * Update device information in state
     */
    private fun updateDeviceInfo() {
        audioDeviceMonitor?.let { monitor ->
            val inputDevice = monitor.currentInputDevice.value
            // Use selected output device if set, otherwise use current device
            val outputDevice = selectedOutputDevice ?: monitor.currentOutputDevice.value
            
            _state.value = _state.value.copy(
                microphoneName = monitor.getMicrophoneName(inputDevice),
                outputDeviceName = monitor.getOutputDeviceName(outputDevice)
            )
        }
    }
    
    /**
     * Set the preferred output device
     */
    fun setOutputDevice(device: AudioDeviceInfo?) {
        selectedOutputDevice = device
        Log.d(TAG, "Output device selected: ${device?.productName ?: "Default"}")
        
        // Update device info in state
        updateDeviceInfo()
        
        // If we're currently playing, we need to restart the audio to apply the change
        if (_state.value.isCapturing && audioTrack != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    audioTrack?.preferredDevice = device
                    Log.d(TAG, "Updated preferred device on active AudioTrack")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update preferred device", e)
                    // Might need to restart audio capture to apply the change
                }
            }
        }
    }
}