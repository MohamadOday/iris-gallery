package com.iris.gallery

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

object MemoriesNotifications {
    const val CHANNEL = "daily_memories"
    private const val ALARM_REQUEST_CODE = 4102

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, MemoriesReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (intent != null) {
            alarm.cancel(intent)
            intent.cancel()
        }
    }

    fun schedule(context: Context, enabled: Boolean = true, hour: Int = 10, minute: Int = 0) {
        if (!enabled) {
            cancel(context)
            return
        }
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, MemoriesReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, intent)
    }

    fun scheduleFromSettings(context: Context) {
        val settings = SettingsPreferences(context).state.value
        schedule(
            context = context,
            enabled = settings.memoriesNotificationEnabled,
            hour = settings.memoriesNotificationHour,
            minute = settings.memoriesNotificationMinute
        )
    }
}

class MemoriesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsPreferences(context).state.value
        if (!settings.memoriesNotificationEnabled) return

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val memories = MediaRepository(context).loadImages().filter {
                    val date = Instant.ofEpochMilli(it.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
                    date.year < today.year && date.month == today.month && date.dayOfMonth == today.dayOfMonth
                }
                if (memories.isEmpty()) return@launch
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(MemoriesNotifications.CHANNEL,
                    context.getString(R.string.notification_channel_memories), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.notification_channel_desc)
                })
                val phrases = listOf("From this day", "A look back", "Remember this day?", "Your story from today")
                val title = phrases[today.dayOfYear % phrases.size]
                val preview = runCatching { context.contentResolver.loadThumbnail(memories.first().uri, Size(720, 480), null) }.getOrNull()
                val open = PendingIntent.getActivity(context, 4103, Intent(context, MainActivity::class.java)
                    .putExtra("open_memories", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val text = if (memories.size == 1) {
                    context.getString(R.string.notification_memory_waiting, memories.size)
                } else {
                    context.getString(R.string.notification_memories_waiting, memories.size)
                }
                val notification = NotificationCompat.Builder(context, MemoriesNotifications.CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true)
                    .apply { if (preview != null) setStyle(NotificationCompat.BigPictureStyle().bigPicture(preview).bigLargeIcon(null as android.graphics.Bitmap?)) }
                    .build()
                manager.notify(4104, notification)
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = MemoriesNotifications.scheduleFromSettings(context)
}
