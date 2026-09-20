package com.tom.fourhourbody.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tom.fourhourbody.MainActivity
import com.tom.fourhourbody.R

object Notifier {

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            Channels.TRAINING to "Training",
            Channels.STRETCHES to "Static stretches",
            Channels.NUTRITION to "Nutrition",
            Channels.SLEEP to "Sleep",
            Channels.COLD to "Cold exposure",
            Channels.CREATINE to "Creatine",
            Channels.PROGRESS to "Progress"
        )
        channels.forEach { (id, name) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun show(context: Context, kind: ReminderKind, bodyOverride: String? = null) {
        if (!hasPermission(context)) return
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, kind.route)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            kind.requestCodeBase,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(kind.title)
            .setContentText(bodyOverride ?: kind.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyOverride ?: kind.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(kind.requestCodeBase, notification)
        }
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}
