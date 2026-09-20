package com.tom.fourhourbody.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.entity.SettingsEntity
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Turns the current settings into a set of alarms. Cancel-then-schedule on every run, so
 * switching a pillar off in Settings drops every alarm it owns and switching it back on
 * restores them, with no bookkeeping in between.
 */
object ReminderScheduler {

    const val EXTRA_KIND = "reminder_kind"

    fun reschedule(context: Context, settings: SettingsEntity, now: LocalDateTime = LocalDateTime.now()) {
        cancelAll(context)
        Notifier.ensureChannels(context)

        if (settings.isEnabled(Pillar.TRAINING)) {
            schedule(context, ReminderKind.TRAINING_SESSION, ReminderTimes.nextDaily(now, settings.reminderTimeMinutes))
            schedule(
                context,
                ReminderKind.SKIPPED_SESSION_NUDGE,
                ReminderTimes.nextDaily(now, ReminderTimes.nudgeMinutes(settings))
            )
            // The weigh-in reminder is independent of the other pillar reminders, but there is
            // no separate "progress" toggle — it rides with training.
            schedule(
                context,
                ReminderKind.WEEKLY_WEIGH_IN,
                ReminderTimes.nextWeekly(now, settings.weighInDay, settings.weighInTimeMinutes)
            )
        }

        if (settings.isEnabled(Pillar.STRETCHES)) {
            if (settings.deskResetRemindersEnabled) {
                ReminderTimes.nextDeskResetTicks(now, settings).forEachIndexed { index, at ->
                    schedule(context, ReminderKind.DESK_RESET_INTERVAL, at, index)
                }
            }
            schedule(
                context,
                ReminderKind.WEEKLY_DESK_RESET,
                ReminderTimes.nextWeekly(now, settings.weeklyDeskResetDay, settings.weeklyRoutineTimeMinutes)
            )
            schedule(
                context,
                ReminderKind.WEEKLY_MOBILITY,
                ReminderTimes.nextWeekly(now, settings.weeklyMobilityDay, settings.weeklyRoutineTimeMinutes)
            )
        }

        if (settings.isEnabled(Pillar.SLEEP)) {
            schedule(
                context,
                ReminderKind.SLEEP_CHECKLIST,
                ReminderTimes.nextDaily(now, settings.sleepReminderMinutes)
            )
        }

        if (settings.isEnabled(Pillar.COLD) && settings.coldRemindersEnabled) {
            ReminderTimes.nextColdReminder(now, settings)?.let {
                schedule(context, ReminderKind.COLD_EXPOSURE, it)
            }
        }

        if (settings.isEnabled(Pillar.CREATINE) && settings.creatineRemindersEnabled) {
            schedule(
                context,
                ReminderKind.CREATINE_MORNING,
                ReminderTimes.nextDaily(now, settings.creatineMorningMinutes)
            )
            schedule(
                context,
                ReminderKind.CREATINE_EVENING,
                ReminderTimes.nextDaily(now, settings.creatineEveningMinutes)
            )
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        ReminderKind.entries.forEach { kind ->
            val slots = if (kind == ReminderKind.DESK_RESET_INTERVAL) {
                ReminderKind.MAX_DESK_RESET_TICKS
            } else {
                1
            }
            repeat(slots) { index ->
                alarmManager.cancel(pendingIntent(context, kind, index, mutable = false))
            }
        }
    }

    private fun schedule(context: Context, kind: ReminderKind, at: LocalDateTime, index: Int = 0) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = pendingIntent(context, kind, index, mutable = false)

        // Exact where the platform allows it; a windowed alarm otherwise, so a denied
        // exact-alarm permission degrades the reminder rather than losing it.
        val canBeExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        runCatching {
            if (canBeExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun pendingIntent(
        context: Context,
        kind: ReminderKind,
        index: Int,
        mutable: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.tom.fourhourbody.REMINDER.${kind.name}.$index"
            putExtra(EXTRA_KIND, kind.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, kind.requestCodeBase + index, intent, flags)
    }
}
