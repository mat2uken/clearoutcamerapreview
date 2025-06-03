package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "AudioCoordinator"

/**
 * Coordinates audio device monitoring and capture/playback based on external audio output availability
 */
class AudioCoordinator(private val context: Context) {
    
    private val audioDeviceMonitor = AudioDeviceMonitor(context)
    private val audioCaptureManager = AudioCaptureManager(context, audioDeviceMonitor)
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringJob: Job? = null
    
    // Combined state
    data class AudioState(
        val hasExternalOutput: Boolean = false,
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
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()
    
    /**
     * Start monitoring audio devices and managing capture/playback
     */
    fun start() {
        Log.d(TAG, "Starting audio coordinator")
        
        // Start device monitoring
        audioDeviceMonitor.startMonitoring()
        
        // Monitor device changes and capture state
        monitoringJob = scope.launch {
            combine(
                audioDeviceMonitor.hasExternalAudioOutput,
                audioCaptureManager.state
            ) { hasExternal, captureState ->
                AudioState(
                    hasExternalOutput = hasExternal,
                    isCapturing = captureState.isCapturing,
                    isPlaying = captureState.isPlaying,
                    hasPermission = captureState.hasPermission,
                    error = captureState.error,
                    sampleRate = captureState.sampleRate,
                    channelCount = captureState.channelCount,
                    isMuted = captureState.isMuted,
                    isManuallyMuted = captureState.isManuallyMuted,
                    microphoneName = captureState.microphoneName,
                    outputDeviceName = captureState.outputDeviceName
                )
            }.collect { newState ->
                _audioState.value = newState
                
                // Start or stop audio capture based on external output availability
                if (newState.hasExternalOutput && newState.hasPermission && !newState.isCapturing) {
                    Log.d(TAG, "External audio output detected, starting capture")
                    audioCaptureManager.startAudioCapture()
                } else if (!newState.hasExternalOutput && newState.isCapturing) {
                    Log.d(TAG, "No external audio output, stopping capture")
                    audioCaptureManager.stopAudioCapture()
                }
            }
        }
    }
    
    /**
     * Stop monitoring and clean up resources
     */
    fun stop() {
        Log.d(TAG, "Stopping audio coordinator")
        
        monitoringJob?.cancel()
        monitoringJob = null
        
        audioDeviceMonitor.stopMonitoring()
        audioCaptureManager.stopAudioCapture()
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stop()
        audioCaptureManager.release()
        scope.cancel()
    }
    
    /**
     * Update permission state after permission is granted
     */
    fun updatePermissionState() {
        audioCaptureManager.updatePermissionState()
    }
    
    /**
     * Manually toggle audio capture (for testing or user control)
     */
    fun toggleAudioCapture() {
        if (_audioState.value.isCapturing) {
            audioCaptureManager.stopAudioCapture()
        } else if (_audioState.value.hasExternalOutput) {
            audioCaptureManager.startAudioCapture()
        }
    }
    
    /**
     * Toggle mute state
     */
    fun toggleMute() {
        audioCaptureManager.toggleMute()
    }
    
    /**
     * Get available output devices
     */
    fun getAvailableOutputDevices(): StateFlow<List<AudioDeviceInfo>> {
        return audioDeviceMonitor.availableOutputDevices
    }
    
    /**
     * Set the output device
     */
    fun setOutputDevice(device: AudioDeviceInfo?) {
        audioCaptureManager.setOutputDevice(device)
    }
    
    /**
     * Get device display name
     */
    fun getDeviceDisplayName(device: AudioDeviceInfo): String {
        return audioDeviceMonitor.getDeviceDisplayName(device)
    }
}