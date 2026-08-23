package com.iris.gallery

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class IrisPhotoWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("IrisWidget", "receive ${intent.action}")
        if (intent.action == ACTION_NEXT) {
            val pending = goAsync()
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        val prefs = context.getSharedPreferences("iris_widget", Context.MODE_PRIVATE)
                        prefs.edit().putInt("offset_$id", prefs.getInt("offset_$id", 0) + 1).apply()
                        update(context, AppWidgetManager.getInstance(context), id)
                    }
                } finally { pending.finish() }
            }
            return
        }
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED) {
            val pending = goAsync()
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: intArrayOf(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)).filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }.toIntArray()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(context)
                    ids.forEach { update(context, manager, it) }
                } catch (error: Throwable) {
                    Log.e("IrisWidget", "update failed", error)
                } finally {
                    pending.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        update(context, manager, id)
    }

    private fun update(context: Context, manager: AppWidgetManager, id: Int) {
        Log.i("IrisWidget", "building widget $id")
        val views = RemoteViews(context.packageName, R.layout.iris_widget_memories)
        val options = manager.getAppWidgetOptions(id)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220).coerceAtLeast(110)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180).coerceAtLeast(110)
        val cardRatio = (widthDp.toFloat() / heightDp).coerceIn(.72f, 2.2f)
        val shortestSide = minOf(widthDp, heightDp).toFloat()
        val buttonSize = (shortestSide * .22f).coerceIn(36f, 58f)
        val buttonMargin = (shortestSide * .065f).coerceIn(8f, 17f)
        val allMemories = loadMemories(context)
        val slot = (System.currentTimeMillis() / 1_800_000L).toInt()
        val offset = context.getSharedPreferences("iris_widget", Context.MODE_PRIVATE).getInt("offset_$id", 0)
        val memories = if (allMemories.isEmpty()) emptyList()
            else listOf(allMemories[Math.floorMod(slot + offset, allMemories.size)])
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java).apply {
            putExtra("open_memories", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_flipper, open)
        val next = PendingIntent.getBroadcast(context, id, Intent(context, IrisPhotoWidget::class.java).apply {
            action = ACTION_NEXT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_next, next)
        if (Build.VERSION.SDK_INT >= 31) {
            views.setViewLayoutWidth(R.id.widget_next, buttonSize, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.widget_next, buttonSize, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutMargin(R.id.widget_next, RemoteViews.MARGIN_END, buttonMargin, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutMargin(R.id.widget_next, RemoteViews.MARGIN_BOTTOM, buttonMargin, TypedValue.COMPLEX_UNIT_DIP)
            val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, buttonSize * .24f,
                context.resources.displayMetrics).toInt()
            views.setViewPadding(R.id.widget_next, paddingPx, paddingPx, paddingPx, paddingPx)
        }

        PHOTO_IDS.forEachIndexed { index, viewId ->
            val memory = memories.getOrNull(index)
            if (memory == null && index > 0) {
                views.setViewVisibility(viewId, View.GONE)
            } else {
                views.setViewVisibility(viewId, View.VISIBLE)
                views.setImageViewBitmap(viewId, memory?.let { memoryCard(context, it, index, cardRatio) }
                    ?: emptyCard(cardRatio))
            }
        }
        manager.updateAppWidget(id, views)
        Log.i("IrisWidget", "published widget $id with ${memories.size} memories")
    }

    private data class Memory(val id: Long, val date: LocalDate)

    private fun loadMemories(context: Context): List<Memory> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val locked = context.getSharedPreferences("library_preferences", Context.MODE_PRIVATE)
            .getStringSet("locked", emptySet()).orEmpty().mapNotNullTo(hashSetOf(), String::toLongOrNull)
        return buildList {
            context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED),
                null, null, "${MediaStore.Images.Media.DATE_TAKEN} DESC")?.use { cursor ->
                while (cursor.moveToNext() && size < 30) {
                    val mediaId = cursor.getLong(0)
                    if (mediaId in locked) continue
                    val timestamp = cursor.getLong(1).takeIf { it > 0 } ?: cursor.getLong(2) * 1_000
                    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                    if (date.year < today.year && date.month == today.month && date.dayOfMonth == today.dayOfMonth) {
                        add(Memory(mediaId, date))
                    }
                }
            }
        }
    }

    private fun memoryCard(context: Context, memory: Memory, index: Int, ratio: Float): Bitmap {
        val outputWidth = 840
        val outputHeight = (outputWidth / ratio).toInt().coerceIn(420, 900)
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, memory.id)
        val source = runCatching { context.contentResolver.loadThumbnail(uri, Size(outputWidth, outputHeight), null) }.getOrNull()
            ?: return emptyCard(ratio)
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val corner = 54f
        val inset = 10f
        val adaptiveSurface = adaptiveSurfaceColor(source)
        canvas.drawRoundRect(RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat()), corner, corner,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = adaptiveSurface })
        val photoBounds = RectF(inset, inset, outputWidth - inset, outputHeight - inset)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(photoBounds, corner - inset, corner - inset, Path.Direction.CW) })
        val sourceRatio = source.width.toFloat() / source.height
        val targetRatio = outputWidth.toFloat() / outputHeight
        val crop = if (sourceRatio > targetRatio) {
            val width = (source.height * targetRatio).toInt()
            Rect((source.width - width) / 2, 0, (source.width + width) / 2, source.height)
        } else {
            val height = (source.width / targetRatio).toInt()
            Rect(0, (source.height - height) / 2, source.width, (source.height + height) / 2)
        }
        canvas.drawBitmap(source, crop, Rect(inset.toInt(), inset.toInt(), outputWidth - inset.toInt(), outputHeight - inset.toInt()),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        val gradientStart = outputHeight * .38f
        val scrim = Paint().apply { shader = LinearGradient(0f, gradientStart, 0f, outputHeight.toFloat(),
            intArrayOf(Color.TRANSPARENT, Color.argb(55, 0, 0, 0), Color.argb(218, 5, 4, 8)),
            floatArrayOf(0f, .34f, 1f), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, gradientStart, outputWidth.toFloat(), outputHeight.toFloat(), scrim)

        // A quiet Material-style identity chip that stays readable on any photo.
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(166, 24, 21, 29) }
        canvas.drawRoundRect(RectF(28f, 26f, 260f, 78f), 26f, 26f, chipPaint)
        canvas.drawCircle(55f, 52f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(218, 184, 255) })
        canvas.drawText("IRIS  ·  MEMORIES", 75f, 61f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        val years = LocalDate.now().year - memory.date.year
        val phrase = when (index % 4) {
            0 -> "From this day"
            1 -> "From ${memory.date.year}"
            2 -> "$years ${if (years == 1) "year" else "years"} ago"
            else -> "A moment worth remembering"
        }
        val compact = outputHeight < 540
        val titleSize = if (compact) 35f else 43f
        val titleY = outputHeight - if (compact) 58f else 78f
        canvas.drawText(phrase, 34f, titleY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = titleSize
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setShadowLayer(5f, 0f, 2f, Color.argb(120, 0, 0, 0))
        })
        if (!compact) {
            val detail = "${memory.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}  ·  $years ${if (years == 1) "year" else "years"} ago"
            canvas.drawText(detail, 36f, outputHeight - 34f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(215, 255, 255, 255); textSize = 24f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                })
        }
        canvas.drawRoundRect(RectF(inset + 1.5f, inset + 1.5f, outputWidth - inset - 1.5f, outputHeight - inset - 1.5f),
            corner - inset, corner - inset,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(55, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 3f })
        canvas.restore()
        source.recycle()
        return output
    }

    private fun adaptiveSurfaceColor(bitmap: Bitmap): Int {
        var red = 0L; var green = 0L; var blue = 0L; var samples = 0L
        val stepX = (bitmap.width / 18).coerceAtLeast(1)
        val stepY = (bitmap.height / 18).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) for (x in 0 until bitmap.width step stepX) {
            val color = bitmap.getPixel(x, y)
            red += Color.red(color); green += Color.green(color); blue += Color.blue(color); samples++
        }
        if (samples == 0L) return Color.rgb(34, 31, 39)
        // Darken toward a rich tonal surface so it complements the photo while
        // retaining enough contrast against the launcher and white typography.
        fun tonal(value: Long) = ((value / samples) * .52f + 18f).toInt().coerceIn(18, 150)
        return Color.rgb(tonal(red), tonal(green), tonal(blue))
    }

    private fun emptyCard(ratio: Float): Bitmap {
        val width = 840
        val height = (width / ratio).toInt().coerceIn(420, 900)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 54f, 54f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(34, 31, 39) })
        canvas.drawCircle(width / 2f, height / 2f - 40f, 22f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(218, 184, 255) })
        canvas.drawText("No memories from this day yet", 48f, height / 2f + 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(234, 221, 255); textSize = 34f; typeface = Typeface.DEFAULT_BOLD
        })
        }
    }

    companion object {
        private const val ACTION_NEXT = "com.iris.gallery.widget.NEXT_MEMORY"
        private val PHOTO_IDS = intArrayOf(R.id.widget_photo_0)

        fun refreshAll(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = AppWidgetManager.getInstance(context)
                manager.getAppWidgetIds(ComponentName(context, IrisPhotoWidget::class.java)).forEach { id ->
                    runCatching { IrisPhotoWidget().update(context.applicationContext, manager, id) }
                        .onFailure { Log.e("IrisWidget", "recovery update failed", it) }
                }
            }
        }
    }
}
