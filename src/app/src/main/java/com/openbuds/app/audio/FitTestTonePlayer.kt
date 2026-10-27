package com.openbuds.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Acoustic test tone player for ear tip seal diagnostics.
 *
 * Synthesizes a structured harmonic multi-frequency chime (~3.6s) directly via [AudioTrack]
 * in [AudioTrack.MODE_STATIC]. This exercises low (bass seal), midrange (isolation), and high
 * frequencies without requiring bundled asset audio files or external codecs.
 */
class FitTestTonePlayer {

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying: Boolean = false

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 3.6
    }

    /**
     * Synthesizes 16-bit PCM stereo audio samples for the acoustic seal sweep.
     */
    private fun generateSignal(): ShortArray {
        val totalSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val numChannels = 2
        val buffer = ShortArray(totalSamples * numChannels)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Harmonic chords across 3 progressive phases
            val freq1: Double
            val freq2: Double
            val freq3: Double
            val segProgress: Double

            when {
                t < 1.2 -> {
                    // Phase 1: Warm bass anchor & low-mid seal
                    freq1 = 220.00 // A3
                    freq2 = 329.63 // E4
                    freq3 = 110.00 // A2
                    segProgress = t / 1.2
                }
                t < 2.4 -> {
                    // Phase 2: Mid-range acoustic isolation
                    freq1 = 440.00 // A4
                    freq2 = 554.37 // C#5
                    freq3 = 220.00 // A3
                    segProgress = (t - 1.2) / 1.2
                }
                else -> {
                    // Phase 3: High harmonic clarity
                    freq1 = 659.25 // E5
                    freq2 = 880.00 // A5
                    freq3 = 1318.51 // E6
                    segProgress = (t - 2.4) / 1.2
                }
            }

            // Cosine bell envelope for each chord segment
            val segEnvelope = sin(segProgress * PI)

            // Master envelope with gentle attack (0.25s) and release (0.35s)
            val masterEnvelope = when {
                t < 0.25 -> t / 0.25
                t > (DURATION_SECONDS - 0.35) -> (DURATION_SECONDS - t) / 0.35
                else -> 1.0
            }

            val sample = (
                0.45 * sin(2.0 * PI * freq1 * t) +
                0.35 * sin(2.0 * PI * freq2 * t) +
                0.20 * sin(2.0 * PI * freq3 * t)
            ) * segEnvelope * masterEnvelope

            val pcmValue = (sample * Short.MAX_VALUE * 0.72)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            val index = i * numChannels
            buffer[index] = pcmValue     // Left channel
            buffer[index + 1] = pcmValue // Right channel
        }
        return buffer
    }

    /**
     * Starts playing the acoustic test tone in background IO.
     * Invokes [onComplete] when finished or interrupted.
     */
    suspend fun play(onComplete: () -> Unit = {}) = withContext(Dispatchers.IO) {
        stop()
        try {
            val pcm = generateSignal()
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val totalFrames = pcm.size / 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(totalFrames)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    isPlaying = false
                    try {
                        t?.stop()
                        t?.release()
                    } catch (_: Exception) {}
                    if (audioTrack == t) {
                        audioTrack = null
                    }
                    onComplete()
                }

                override fun onPeriodicNotification(track: AudioTrack?) {}
            })

            audioTrack = track
            isPlaying = true
            track.play()
        } catch (_: Exception) {
            isPlaying = false
            onComplete()
        }
    }

    /**
     * Halts playback and frees native audio hardware buffers immediately.
     */
    fun stop() {
        isPlaying = false
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
    }
}
