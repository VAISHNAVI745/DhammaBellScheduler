package org.dhamma.bell.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dhamma.bell.audio.BellPlaybackService
import org.dhamma.bell.data.AppDatabase

/**
 * Fires when AlarmManager delivers an exact alarm. Because the process may be
 * cold (TV box just resumed from a low-power state), we must:
 *  1. Hold a goAsync() wake window while we hit Room on a background thread.
 *  2. Immediately hand off to a foreground Service for actual playback, since
 *     BroadcastReceivers get ~10s before the system may kill background work.
 */
class BellAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_TRIGGER_BELL) return

        val bellId = intent.getLongExtra(AlarmScheduler.EXTRA_BELL_ID, -1L)
        if (bellId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val bell = db.bellDao().getById(bellId)

                if (bell != null && !bell.isCancelled) {
                    db.bellDao().markTriggered(bellId)

                    val serviceIntent = Intent(context, BellPlaybackService::class.java).apply {
                        putExtra(BellPlaybackService.EXTRA_BELL_ID, bellId)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    Log.w(TAG, "Bell $bellId was cancelled or missing at fire time; skipping playback.")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to handle bell alarm $bellId", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BellAlarmReceiver"
    }
}
