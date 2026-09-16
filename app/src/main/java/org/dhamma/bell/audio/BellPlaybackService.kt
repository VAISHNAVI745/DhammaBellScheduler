package org.dhamma.bell.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.dhamma.bell.DhammaBellApp
import org.dhamma.bell.MainActivity
import org.dhamma.bell.data.AppDatabase
import org.dhamma.bell.data.EventType

/**
 * Foreground Service that actually plays a gong/audio track for one ScheduledBellEntity.
 * Runs as a foreground service (not just a MediaPlayer in the Activity) because the
 * triggering BroadcastReceiver may fire while the Activity/TV screensaver is not in front,
 * and background playback would otherwise be throttled or killed by the OS.
 */
class BellPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bellId = intent?.getLongExtra(EXTRA_BELL_ID, -1L) ?: -1L
        startForeground(NOTIFICATION_ID, buildNotification("Preparing bell..."))

        if (bellId == -1L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        acquireWakeLock()

        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val bell = db.bellDao().getById(bellId)
                if (bell == null) {
                    Log.w(TAG, "Bell $bellId no longer exists in DB; nothing to play.")
                    finishPlayback(startId)
                    return@launch
                }

                updateNotification(bell.eventLabel)

                val resolvedFile = AudioResolver.resolve(applicationContext, bell.audioFileName, bell.eventType)

                AudioPlaybackManager.playWithHdmiWakeBurst(
                    context = applicationContext,
                    audioFile = resolvedFile,
                    isGongEvent = bell.eventType == EventType.GONG_SHORT || bell.eventType == EventType.GONG_LONG,
                    onComplete = { finishPlayback(startId) }
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Playback failed for bell $bellId", t)
                finishPlayback(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun finishPlayback(startId: Int) {
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DhammaBell:PlaybackWakeLock").apply {
            setReferenceCounted(false)
            acquire(5 * 60 * 1000L) // 5 min safety timeout in case completion listener never fires
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, DhammaBellApp.CHANNEL_ID)
            .setContentTitle("DhammaBell")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        AudioPlaybackManager.stopAndRelease()
    }

    companion object {
        private const val TAG = "BellPlaybackService"
        private const val NOTIFICATION_ID = 4201
        const val EXTRA_BELL_ID = "extra_bell_id"
    }
}
