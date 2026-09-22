package com.tom.fourhourbody.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tom.fourhourbody.data.entity.SettingsEntity
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Turns the current settings into a set of alarms. Cancel-then-schedule on every run, so a
 * changed reminder time takes effect immediately with no bookkeeping in between.
 */
object ReminderScheduler {

    const val EXTRA_KIND = "reminder_kind"

    fun reschedule(context: Context, settings: SettingsEntity, now: LocalDateTime = LocalDateTime.now()) {
        cancelAll(context)
        Notifier.ensureChannels(context)

        schedule(context, ReminderKind.TRAINING_SESSION, ReminderTimes.nextDaily(now, settings.reminderTimeMinutes))
        schedule(
            context,
            ReminderKind.SKIPPED_SESSION_NUDGE,
            ReminderTimes.nextDaily(now, ReminderTimes.nudgeMinutes(settings.reminderTimeMinutes))
        )
        schedule(
            context,
            ReminderKind.WEEKLY_WEIGH_IN,
            ReminderTimes.nextWeekly(now, settings.weighInDay, settings.weighInTimeMinutes)
        )
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        ReminderKind.entries.forEach { kind ->
            alarmManager.cancel(pendingIntent(context, kind, mutable = false))
        }
    }

    private fun schedule(context: Context, kind: ReminderKind, at: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = pendingIntent(context, kind, mutable = false)

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

    private fun pendingIntent(context: Context, kind: ReminderKind, mutable: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.tom.fourhourbody.REMINDER.${kind.name}"
            putExtra(EXTRA_KIND, kind.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, kind.requestCodeBase, intent, flags)
    }
}
