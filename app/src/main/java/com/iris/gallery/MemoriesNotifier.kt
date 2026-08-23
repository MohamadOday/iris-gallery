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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

object MemoriesNotifications {
    const val CHANNEL = "daily_memories"

    fun schedule(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(context, 4102, Intent(context, MemoriesReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, intent)
    }
}

class MemoriesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
                    "Daily memories", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Photos and videos from this day in past years"
                })
                val phrases = listOf("From this day", "A look back", "Remember this day?", "Your story from today")
                val title = phrases[today.dayOfYear % phrases.size]
                val preview = runCatching { context.contentResolver.loadThumbnail(memories.first().uri, Size(720, 480), null) }.getOrNull()
                val open = PendingIntent.getActivity(context, 4103, Intent(context, MainActivity::class.java)
                    .putExtra("open_memories", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification = NotificationCompat.Builder(context, MemoriesNotifications.CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
                    .setContentText("${memories.size} ${if (memories.size == 1) "memory" else "memories"} waiting for you")
                    .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true)
                    .apply { if (preview != null) setStyle(NotificationCompat.BigPictureStyle().bigPicture(preview).bigLargeIcon(null as android.graphics.Bitmap?)) }
                    .build()
                manager.notify(4104, notification)
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = MemoriesNotifications.schedule(context)
}
