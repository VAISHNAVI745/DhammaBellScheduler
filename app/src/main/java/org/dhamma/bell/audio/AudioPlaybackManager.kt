package org.dhamma.bell.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sin
import kotlin.math.PI

/**
 * Owns actual audio output for one bell event:
 *  1. Fires a short 200ms near-silent priming burst first, so a sleeping HDMI
 *     audio DAC / external amplifier that only wakes on active signal has
 *     already woken up before the real gong starts (prevents the first
 *     0.5-1s of the gong being clipped/truncated on cheap TV box HDMI audio).
 *  2. Plays the resolved MP3 via MediaPlayer, OR a synthesized sine-wave gong
 *     tone via AudioTrack if no file at all could be resolved.
 *  3. Invokes onComplete (which releases the wake lock / stops the service)
 *     from MediaPlayer's OnCompletionListener - never releases the lock early.
 */
object AudioPlaybackManager {

    private const val TAG = "AudioPlaybackManager"
    private var mediaPlayer: MediaPlayer? = null

    suspend fun playWithHdmiWakeBurst(
        context: Context,
        audioFile: File?,
        isGongEvent: Boolean,
        onComplete: () -> Unit
    ) {
        try {
            primeHdmiOutput(context)
            delay(220) // let the burst finish and the DAC fully wake before the real audio
        } catch (t: Throwable) {
            Log.w(TAG, "HDMI priming burst failed (non-fatal), continuing to main playback.", t)
        }

        if (audioFile != null && audioFile.exists()) {
            playFile(context, audioFile, onComplete)
        } else {
            playSynthesizedGong(context, onComplete)
        }
    }

    /** Plays 200ms of very low-amplitude audio through the media stream to wake the DAC. */
    private fun primeHdmiOutput(context: Context) {
        val sampleRate = 44100
        val durationMs = 200
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ShortArray(numSamples)

        // Very quiet 50Hz tone - audible-range but low amplitude, just enough to be a
        // real active signal rather than digital silence some DACs never wake from.
        for (i in buffer.indices) {
            val angle = 2.0 * PI * 50.0 * (i.toDouble() / sampleRate)
            buffer[i] = (sin(angle) * 300).toInt().toShort() // low amplitude of max 32767
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack(
            attributes,
            format,
            buffer.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(buffer, 0, buffer.size)
        track.play()
        // Fire-and-forget: release shortly after; we already delay() in the caller.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                track.stop()
                track.release()
            } catch (_: Throwable) { }
        }, 250)
    }

    private fun playFile(context: Context, file: File, onComplete: () -> Unit) {
        stopAndRelease()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopAndRelease()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error playing ${file.name}: what=$what extra=$extra")
                    stopAndRelease()
                    onComplete()
                    true
                }
                prepare()
                start()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to play ${file.absolutePath}, falling back to synthesized gong.", t)
            playSynthesizedGong(context, onComplete)
        }
    }

    /** Last-resort fallback: a simple synthesized bell-like decaying tone, no MP3 needed at all. */
    private fun playSynthesizedGong(context: Context, onComplete: () -> Unit) {
        try {
            val sampleRate = 44100
            val durationSeconds = 2.5
            val numSamples = (sampleRate * durationSeconds).toInt()
            val buffer = ShortArray(numSamples)

            // Two summed sine partials with exponential decay to approximate a gong strike.
            for (i in buffer.indices) {
                val tSec = i.toDouble() / sampleRate
                val decay = Math.exp(-tSec * 1.8)
                val fundamental = sin(2.0 * PI * 220.0 * tSec)
                val overtone = 0.5 * sin(2.0 * PI * 330.0 * tSec)
                val sample = (fundamental + overtone) * decay * 20000
                buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack(
                attributes, format, buffer.size * 2,
                AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buffer, 0, buffer.size)
            track.play()

            val durationMs = (durationSeconds * 1000).toLong()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) { }
                onComplete()
            }, durationMs)
        } catch (t: Throwable) {
            Log.e(TAG, "Synthesized gong fallback also failed.", t)
            onComplete()
        }
    }

    fun stopAndRelease() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Throwable) { }
            it.release()
        }
        mediaPlayer = null
    }
}
