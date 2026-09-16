package org.dhamma.bell.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Survives power cuts: on BOOT_COMPLETED (or QUICKBOOT_POWERON for some TV boxes,
 * or MY_PACKAGE_REPLACED after an app update), reads every future un-triggered
 * alarm out of Room and re-registers it with AlarmManager, since all previously
 * armed alarms are wiped by the reboot itself.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in relevantActions) return

        Log.i(TAG, "Boot/upgrade event received (${intent.action}); re-arming alarms from Room.")

        val pendingResult = goAsync()
        val scheduler = AlarmScheduler(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rearmAllFromDatabase()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to re-arm alarms after boot", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
