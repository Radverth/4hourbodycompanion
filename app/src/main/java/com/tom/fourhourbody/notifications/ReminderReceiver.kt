package com.tom.fourhourbody.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.FourHourBodyApp
import com.tom.fourhourbody.domain.training.SessionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Fires a reminder, then re-arms the whole set. Conditions are checked here, at fire time,
 * rather than baked into the alarm — training frequency is rule-driven, so whether a session
 * is due today can only be known now.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kindName = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val kind = runCatching { ReminderKind.valueOf(kindName) }.getOrNull() ?: return

        val pendingResult = goAsync()
        val container = (context.applicationContext as FourHourBodyApp).container

        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (shouldFire(container, kind)) {
                    Notifier.show(context, kind, bodyOverride(container, kind))
                }
                val settings = container.settingsRepository.current()
                ReminderScheduler.reschedule(context, settings, LocalDateTime.now())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun shouldFire(container: AppContainer, kind: ReminderKind): Boolean =
        when (kind) {
            ReminderKind.TRAINING_SESSION, ReminderKind.SKIPPED_SESSION_NUDGE -> {
                val today = LocalDate.now()
                val schedule = container.trainingRepository.schedule(today).first()
                val loggedToday = container.trainingRepository.observeSessionsOn(today).first()
                    .any { it.completed }
                schedule.dueToday && !loggedToday
            }

            else -> true
        }

    private suspend fun bodyOverride(container: AppContainer, kind: ReminderKind): String? =
        when (kind) {
            ReminderKind.TRAINING_SESSION -> {
                val schedule = container.trainingRepository.schedule(LocalDate.now()).first()
                val last = schedule.lastSessionDate
                if (last == null) {
                    kind.body
                } else {
                    val next = SessionScheduler.nextSessionDate(last, schedule.restDaysBetween)
                    "${kind.body}\nScheduled gap is currently ${schedule.restDaysBetween} rest days " +
                        "(due from $next)."
                }
            }

            else -> null
        }
}
