package org.dhamma.bell.audio

import android.content.Context
import android.util.Log
import org.dhamma.bell.data.EventType
import java.io.File

/**
 * Resolves the audio file a bell should play, with graceful fallback.
 * Field-installed audio lives on external storage so course staff can update
 * chanting/discourse MP3s without rebuilding the APK.
 */
object AudioResolver {

    private const val TAG = "AudioResolver"
    const val AUDIO_SUBDIR = "DhammaAudio"
    const val DEFAULT_GONG_FILE = "gong_single.mp3"

    fun audioDirectory(context: Context): File {
        val dir = context.getExternalFilesDir(AUDIO_SUBDIR)
            ?: File(context.filesDir, AUDIO_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Returns a resolved, existing File to play, or null if playback should be a
     * synthesized fallback tone (no file exists on disk at all, not even the default gong).
     */
    fun resolve(context: Context, requestedFileName: String?, eventType: String): File? {
        val dir = audioDirectory(context)

        val candidateName = requestedFileName
            ?: if (eventType == EventType.GONG_SHORT || eventType == EventType.GONG_LONG) DEFAULT_GONG_FILE else null

        if (candidateName != null) {
            val candidate = File(dir, candidateName)
            if (candidate.exists() && candidate.length() > 0) {
                return candidate
            }
            Log.w(TAG, "ALERT: Missing expected audio file '$candidateName' in ${dir.absolutePath}. Falling back to default gong.")
        }

        val defaultGong = File(dir, DEFAULT_GONG_FILE)
        if (defaultGong.exists() && defaultGong.length() > 0) {
            return defaultGong
        }

        Log.w(TAG, "ALERT: Default gong file also missing from ${dir.absolutePath}. Using synthesized tone fallback.")
        return null
    }
}
