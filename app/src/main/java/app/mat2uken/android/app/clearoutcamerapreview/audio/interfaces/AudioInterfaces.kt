package app.mat2uken.android.app.clearoutcamerapreview.audio.interfaces

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack

/**
 * Interface for audio recording operations
 * Allows for easier testing by abstracting Android's AudioRecord
 */
interface AudioRecordWrapper {
    val state: Int
    val recordingState: Int
    
    fun startRecording()
    fun stop()
    fun release()
    fun read(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int
}

/**
 * Interface for audio playback operations
 * Allows for easier testing by abstracting Android's AudioTrack
 */
interface AudioTrackWrapper {
    val state: Int
    val playState: Int
    var preferredDevice: AudioDeviceInfo?
    
    fun play()
    fun pause()
    fun flush()
    fun stop()
    fun release()
    fun write(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int
}

/**
 * Factory interface for creating audio components
 */
interface AudioComponentFactory {
    fun createAudioRecord(
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int
    ): AudioRecordWrapper
    
    fun createAudioTrack(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int,
        mode: Int
    ): AudioTrackWrapper
}

/**
 * Default implementation using actual Android audio classes
 */
class DefaultAudioRecordWrapper(
    private val audioRecord: AudioRecord
) : AudioRecordWrapper {
    override val state: Int
        get() = audioRecord.state
    
    override val recordingState: Int
        get() = audioRecord.recordingState
    
    override fun startRecording() = audioRecord.startRecording()
    override fun stop() = audioRecord.stop()
    override fun release() = audioRecord.release()
    override fun read(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int =
        audioRecord.read(audioData, offsetInBytes, sizeInBytes)
}

/**
 * Default implementation using actual Android audio classes
 */
class DefaultAudioTrackWrapper(
    private val audioTrack: AudioTrack
) : AudioTrackWrapper {
    override val state: Int
        get() = audioTrack.state
    
    override val playState: Int
        get() = audioTrack.playState
    
    override var preferredDevice: AudioDeviceInfo?
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            audioTrack.preferredDevice
        } else null
        set(value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioTrack.preferredDevice = value
            }
        }
    
    override fun play() = audioTrack.play()
    override fun pause() = audioTrack.pause()
    override fun flush() = audioTrack.flush()
    override fun stop() = audioTrack.stop()
    override fun release() = audioTrack.release()
    override fun write(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int =
        audioTrack.write(audioData, offsetInBytes, sizeInBytes)
}