package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AudioDeviceMonitor"

/**
 * Monitors audio device connections and determines if external audio output is available
 */
class AudioDeviceMonitor(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    
    private val _hasExternalAudioOutput = MutableStateFlow(false)
    val hasExternalAudioOutput: StateFlow<Boolean> = _hasExternalAudioOutput.asStateFlow()
    
    private val _currentOutputDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val currentOutputDevice: StateFlow<AudioDeviceInfo?> = _currentOutputDevice.asStateFlow()
    
    private val _currentInputDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val currentInputDevice: StateFlow<AudioDeviceInfo?> = _currentInputDevice.asStateFlow()
    
    private val _availableOutputDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    val availableOutputDevices: StateFlow<List<AudioDeviceInfo>> = _availableOutputDevices.asStateFlow()
    
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            Log.d(TAG, "Audio devices added: ${addedDevices.size}")
            addedDevices.forEach { device ->
                Log.d(TAG, "Added device: ${getDeviceTypeName(device.type)} - ${device.productName}")
            }
            updateAudioDeviceState()
        }
        
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            Log.d(TAG, "Audio devices removed: ${removedDevices.size}")
            removedDevices.forEach { device ->
                Log.d(TAG, "Removed device: ${getDeviceTypeName(device.type)} - ${device.productName}")
            }
            updateAudioDeviceState()
        }
    }
    
    init {
        // Check initial state
        updateAudioDeviceState()
    }
    
    /**
     * Start monitoring audio device changes
     */
    fun startMonitoring() {
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
            Log.d(TAG, "Started monitoring audio devices")
            updateAudioDeviceState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio device monitoring", e)
        }
    }
    
    /**
     * Stop monitoring audio device changes
     */
    fun stopMonitoring() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            Log.d(TAG, "Stopped monitoring audio devices")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio device monitoring", e)
        }
    }
    
    /**
     * Update the current audio device state
     */
    private fun updateAudioDeviceState() {
        // Get output devices
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        // Get input devices
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        
        // Get all available output devices
        val availableOutputs = outputDevices.filter { device ->
            device.isSink
        }
        _availableOutputDevices.value = availableOutputs
        
        // Find the active output device - prefer the one that's currently routing audio
        val activeOutputDevice = availableOutputs.firstOrNull { device ->
            // Check if this device is currently routing audio
            device.isSink && isDeviceActive(device)
        } ?: availableOutputs.firstOrNull { device ->
            // Fallback to any available sink
            device.isSink
        }
        
        _currentOutputDevice.value = activeOutputDevice
        
        // Check if we have external audio output
        val hasExternal = outputDevices.any { device ->
            device.isSink && isExternalAudioDevice(device)
        }
        
        _hasExternalAudioOutput.value = hasExternal
        
        // Find the active microphone (built-in mic is usually the default)
        val activeInputDevice = inputDevices.firstOrNull { device ->
            device.isSource && device.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
        } ?: inputDevices.firstOrNull { device ->
            device.isSource
        }
        
        _currentInputDevice.value = activeInputDevice
        
        Log.d(TAG, "Audio device state updated:")
        Log.d(TAG, "  Has external output: $hasExternal")
        Log.d(TAG, "  Active output: ${activeOutputDevice?.let { "${getDeviceTypeName(it.type)} - ${it.productName}" } ?: "None"}")
        Log.d(TAG, "  Active input: ${activeInputDevice?.let { "${getDeviceTypeName(it.type)} - ${it.productName}" } ?: "None"}")
        Log.d(TAG, "  Available output devices:")
        outputDevices.forEach { device ->
            if (device.isSink) {
                Log.d(TAG, "    - ${getDeviceTypeName(device.type)} - ${device.productName} (External: ${isExternalAudioDevice(device)})")
            }
        }
        Log.d(TAG, "  Available input devices:")
        inputDevices.forEach { device ->
            if (device.isSource) {
                Log.d(TAG, "    - ${getDeviceTypeName(device.type)} - ${device.productName}")
            }
        }
    }
    
    /**
     * Determine if an audio device is external (not built-in speaker)
     */
    private fun isExternalAudioDevice(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> false
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> false
            AudioDeviceInfo.TYPE_TELEPHONY -> false
            
            // External devices
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_DOCK,
            AudioDeviceInfo.TYPE_FM,
            AudioDeviceInfo.TYPE_AUX_LINE,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_IP,
            AudioDeviceInfo.TYPE_BUS,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> true
            
            else -> {
                // For unknown types, check if it's not a built-in device
                Log.w(TAG, "Unknown device type: ${device.type}")
                true
            }
        }
    }
    
    /**
     * Get a human-readable name for the device type
     */
    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
            AudioDeviceInfo.TYPE_DOCK -> "Dock"
            AudioDeviceInfo.TYPE_FM -> "FM"
            AudioDeviceInfo.TYPE_AUX_LINE -> "AUX Line"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line Analog"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line Digital"
            AudioDeviceInfo.TYPE_IP -> "IP"
            AudioDeviceInfo.TYPE_BUS -> "Bus"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
            AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "Remote Submix"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM Tuner"
            else -> "Unknown ($type)"
        }
    }
    
    /**
     * Get formatted microphone name
     */
    fun getMicrophoneName(device: AudioDeviceInfo?): String {
        return device?.let {
            // If device has a product name, use it
            if (it.productName.isNotEmpty() && it.productName.toString() != "null") {
                it.productName.toString()
            } else {
                // Otherwise use type-specific names
                when (it.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Microphone"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headset Microphone"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Microphone"
                    AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Microphone"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset Microphone"
                    else -> getDeviceTypeName(it.type)
                }
            }
        } ?: "No Microphone"
    }
    
    /**
     * Get formatted output device name
     */
    fun getOutputDeviceName(device: AudioDeviceInfo?): String {
        return device?.let {
            // If device has a product name, use it
            if (it.productName.isNotEmpty() && it.productName.toString() != "null") {
                it.productName.toString()
            } else {
                // Otherwise use type-specific names
                when (it.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Speaker"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Headset"
                    AudioDeviceInfo.TYPE_HDMI -> "HDMI Audio"
                    AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                    else -> getDeviceTypeName(it.type)
                }
            }
        } ?: "No Output"
    }
    
    /**
     * Check if a device is currently active (routing audio)
     * This is a simplified check - in real scenarios, you might need more complex logic
     */
    private fun isDeviceActive(device: AudioDeviceInfo): Boolean {
        // For now, we'll consider a device active if it's not the built-in speaker
        // when external devices are available, or if it's the only device
        val hasExternal = _availableOutputDevices.value.any { isExternalAudioDevice(it) }
        return if (hasExternal) {
            isExternalAudioDevice(device)
        } else {
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }
    
    /**
     * Get a display name for an audio device suitable for UI
     */
    fun getDeviceDisplayName(device: AudioDeviceInfo): String {
        val typeName = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (Call)"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Audio Device"
        }
        
        return if (device.productName.isNotEmpty() && device.productName.toString() != "null") {
            "${device.productName} ($typeName)"
        } else {
            typeName
        }
    }
}