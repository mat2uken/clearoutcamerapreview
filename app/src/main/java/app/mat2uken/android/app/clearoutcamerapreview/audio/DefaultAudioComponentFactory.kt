package app.mat2uken.android.app.clearoutcamerapreview.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build
import app.mat2uken.android.app.clearoutcamerapreview.audio.interfaces.*

/**
 * Default factory implementation for creating audio components
 */
class DefaultAudioComponentFactory : AudioComponentFactory {
    
    @Suppress("MissingPermission")
    override fun createAudioRecord(
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int
    ): AudioRecordWrapper {
        val audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSizeInBytes
            )
        }
        
        return DefaultAudioRecordWrapper(audioRecord)
    }
    
    override fun createAudioTrack(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int,
        mode: Int
    ): AudioTrackWrapper {
        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Configure audio attributes for low latency
            // For API 26+, we use setPerformanceMode
            // For API 24-25, we optimize with USAGE_GAME for better latency
            val usage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes.USAGE_MEDIA
            } else {
                // Use GAME usage for better latency on older APIs
                AudioAttributes.USAGE_GAME
            }
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            
            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(mode)
            
            // Set performance mode for API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            
            trackBuilder.build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSizeInBytes,
                mode
            )
        }
        
        return DefaultAudioTrackWrapper(audioTrack)
    }
}