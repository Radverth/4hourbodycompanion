package com.tom.fourhourbody.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tom.fourhourbody.FourHourBodyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Alarms do not survive a reboot, a reinstall or a clock change on their own. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as FourHourBodyApp).container

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = container.settingsRepository.current()
                ReminderScheduler.reschedule(context, settings, LocalDateTime.now())
                ReminderWorker.enqueue(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
