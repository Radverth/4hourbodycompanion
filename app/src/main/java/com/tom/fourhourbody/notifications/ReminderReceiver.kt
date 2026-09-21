package com.tom.fourhourbody.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tom.fourhourbody.FourHourBodyApp
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.domain.training.SessionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
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
                val settings = container.settingsRepository.current()
                if (settings.isEnabled(kind.pillar) && shouldFire(container, kind)) {
                    Notifier.show(context, kind, bodyOverride(container, kind))
                }
                ReminderScheduler.reschedule(context, settings, LocalDateTime.now())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun shouldFire(
        container: com.tom.fourhourbody.AppContainer,
        kind: ReminderKind
    ): Boolean {
        val today = LocalDate.now()
        return when (kind) {
            ReminderKind.TRAINING_SESSION, ReminderKind.SKIPPED_SESSION_NUDGE -> {
                val schedule = container.trainingRepository.schedule(today).first()
                val loggedToday = container.trainingRepository.observeSessionsOn(today).first()
                    .any { it.completed }
                schedule.dueToday && !loggedToday
            }

            ReminderKind.CREATINE_MORNING, ReminderKind.CREATINE_EVENING -> {
                val settings = container.settingsRepository.current()
                CreatineCycle.stateOn(settings.creatineCycleStartDate, today).day != null
            }

            ReminderKind.WEEKLY_MOBILITY -> {
                // Skip if a session is due today — mobility is a rest-day routine.
                val schedule = container.trainingRepository.schedule(today).first()
                !schedule.dueToday
            }

            else -> true
        }
    }

    private suspend fun bodyOverride(
        container: com.tom.fourhourbody.AppContainer,
        kind: ReminderKind
    ): String? = when (kind) {
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

        ReminderKind.CREATINE_MORNING, ReminderKind.CREATINE_EVENING -> {
            val settings = container.settingsRepository.current()
            val day = CreatineCycle.stateOn(settings.creatineCycleStartDate, LocalDate.now()).day
            day?.let { "${kind.body} Day $it of ${CreatineCycle.CYCLE_LENGTH_DAYS}." }
        }

        ReminderKind.DESK_RESET_INTERVAL -> {
            val settings = container.settingsRepository.current()
            ReminderTimes.sittingHoursFor(LocalDateTime.now(), settings)
                ?.let { hours -> "That's $hours hours sitting. Five minutes resets the hips." }
        }

        else -> null
    }
}
