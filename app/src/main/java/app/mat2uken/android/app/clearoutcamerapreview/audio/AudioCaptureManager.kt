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
    
    init {
        Log.d(TAG, "AudioCaptureManager init: Starting initialization")
        Log.d(TAG, "AudioCaptureManager init: Context class = ${context.javaClass.simpleName}")
        Log.d(TAG, "AudioCaptureManager init: AudioDeviceMonitor provided = ${audioDeviceMonitor != null}")
    }
    
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
            
            // Use smaller buffers for lower latency
            // Calculate minimum buffer size for low latency (aim for 20ms)
            val targetLatencyMs = 20
            val minLatencyBytes = (config.sampleRate * config.channelCount * 2 * targetLatencyMs) / 1000
            
            // Use the larger of minimum required size or target latency size
            recordBufferSize = max(recordBufferSize, minLatencyBytes)
            playbackBufferSize = max(playbackBufferSize, minLatencyBytes)
            
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
        
        if (_state.value.isCapturing || captureJob?.isActive == true) {
            Log.w(TAG, "Audio capture already running or starting")
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
                // Don't call stopAudioCapture from within the coroutine to avoid recursion
                withContext(Dispatchers.Main) {
                    stopAudioCapture()
                }
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
            // Use VOICE_RECOGNITION for lower latency on supported devices
            val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else {
                MediaRecorder.AudioSource.MIC
            }
            
            AudioRecord.Builder()
                .setAudioSource(audioSource)
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
            val audioAttributesBuilder = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            
            // Add low latency flag for API 24+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                audioAttributesBuilder.setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            }
            
            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributesBuilder.build())
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(config.sampleRate)
                        .setChannelMask(outputChannelConfig)
                        .setEncoding(config.encoding)
                        .build()
                )
                .setBufferSizeInBytes(playbackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
            
            // Set performance mode for API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            
            trackBuilder
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
            audioTrack = AudioTrack(
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
        
        // Verify components are initialized before starting
        val record = audioRecord
        val track = audioTrack
        
        try {
            
            if (record == null || track == null) {
                Log.e(TAG, "Audio components not initialized: record=$record, track=$track")
                _state.value = _state.value.copy(
                    error = "Audio components not initialized"
                )
                return@withContext
            }
            
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized properly: state=${record.state}")
                _state.value = _state.value.copy(
                    error = "AudioRecord not initialized"
                )
                return@withContext
            }
            
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack not initialized properly: state=${track.state}")
                _state.value = _state.value.copy(
                    error = "AudioTrack not initialized"
                )
                return@withContext
            }
            
            record.startRecording()
            track.play()
            
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
                val bytesRead = try {
                    record.read(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from AudioRecord", e)
                    -1
                }
                
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
                        try {
                            // Re-check AudioTrack state with more thorough validation
                            if (track.state == AudioTrack.STATE_INITIALIZED && 
                                track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                
                                // Use write method with error checking
                                val written = track.write(buffer, 0, bytesRead)
                                if (written < 0) {
                                    Log.e(TAG, "AudioTrack write error: $written")
                                    // AudioTrack might be in bad state, break the loop
                                    break
                                }
                            } else {
                                Log.w(TAG, "AudioTrack not ready: state=${track.state}, playState=${track.playState}")
                                // If track is uninitialized, break to restart
                                if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                                    Log.e(TAG, "AudioTrack became uninitialized, breaking loop")
                                    break
                                }
                            }
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "AudioTrack in illegal state", e)
                            // AudioTrack is in bad state, break the loop to restart
                            break
                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing to AudioTrack", e)
                            // For severe exceptions, break the loop
                            break
                        }
                    }
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading from AudioRecord: $bytesRead")
                    break
                }
                
                // Small yield to prevent blocking
                yield()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in audio loop", e)
            _state.value = _state.value.copy(
                error = "Audio loop error: ${e.message}"
            )
        } finally {
            Log.d(TAG, "Audio loop ended, cleaning up")
            // Ensure we update state to reflect that we're no longer capturing
            _state.value = _state.value.copy(
                isCapturing = false,
                isPlaying = false
            )
            
            // Stop recording and playback safely
            try {
                if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioRecord", e)
            }
            
            try {
                if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                    track.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioTrack", e)
            }
        }
    }
    
    /**
     * Stop audio capture and playback
     */
    fun stopAudioCapture() {
        Log.d(TAG, "Stopping audio capture")
        
        // First update state to signal the loop to stop
        _state.value = _state.value.copy(
            isCapturing = false,
            isPlaying = false,
            isMuted = false
        )
        
        // Cancel the coroutine job and wait for it to complete
        captureJob?.cancel()
        
        // Use runBlocking to ensure cleanup happens synchronously
        runBlocking {
            captureJob?.join()
        }
        captureJob = null
        
        // Stop and release AudioRecord safely
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
        
        // Stop and release AudioTrack safely
        try {
            audioTrack?.apply {
                // Pause first to stop playback immediately
                pause()
                // Flush buffers to ensure no pending writes
                flush()
                // Then stop
                if (playState != AudioTrack.PLAYSTATE_STOPPED) {
                    stop()
                }
                // Finally release
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null
        
        Log.d(TAG, "Audio capture stopped")
    }
    
    /**
     * Restart audio capture (useful for recovering from errors or device changes)
     */
    fun restartAudioCapture() {
        Log.d(TAG, "Restarting audio capture")
        audioScope.launch {
            // Stop current capture
            withContext(Dispatchers.Main) {
                stopAudioCapture()
            }
            
            // Small delay to ensure cleanup
            delay(500)
            
            // Start again
            withContext(Dispatchers.Main) {
                startAudioCapture()
            }
        }
    }
    
    /**
     * Update preferred output device
     */
    fun setPreferredOutputDevice(device: AudioDeviceInfo?) {
        selectedOutputDevice = device
        
        // Update AudioTrack's preferred device if it exists and is initialized
        audioTrack?.let { track ->
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        track.preferredDevice = device
                        Log.d(TAG, "Updated preferred output device: ${device?.productName}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting preferred device, restarting audio", e)
                    // If setting device fails, restart audio capture
                    restartAudioCapture()
                }
            }
        }
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
                    // Synchronize access to audioTrack to prevent concurrent modification
                    synchronized(this) {
                        audioTrack?.let { track ->
                            if (track.state == AudioTrack.STATE_INITIALIZED) {
                                track.preferredDevice = device
                                Log.d(TAG, "Updated preferred device on active AudioTrack")
                            } else {
                                Log.w(TAG, "AudioTrack not in valid state for device update: ${track.state}")
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "AudioTrack in illegal state for device update", e)
                    // Need to restart audio capture to apply the change
                    audioScope.launch {
                        restartAudioCapture()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update preferred device", e)
                }
            }
        }
    }
    
}