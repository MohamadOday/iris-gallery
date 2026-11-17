package com.iris.gallery.data

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Preserves original MediaStore DATE_ADDED timestamps for items restored from the
 * private trash or vault, since MediaProvider treats DATE_ADDED as read-only for
 * third-party callers on insert and scan.
 */
object RestoredTimestamps {
    private const val PREFS_NAME = "restored_timestamps"
    private val initLock = Any()

    @Volatile
    private var initialized = false
    private val dateAddedByPath = ConcurrentHashMap<String, Long>()

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureLoaded(context: Context) {
        if (initialized) return
        synchronized(initLock) {
            if (initialized) return
            val all = prefs(context).all
            for ((key, value) in all) {
                val timestamp = (value as? Long) ?: continue
                if (key.isNotBlank() && timestamp > 0L) {
                    dateAddedByPath[key] = timestamp
                }
            }
            initialized = true
        }
    }

    fun getDateAdded(context: Context, path: String): Long? {
        if (path.isBlank()) return null
        ensureLoaded(context)
        if (dateAddedByPath.isEmpty()) return null
        return dateAddedByPath[path]
    }

    fun recordDateAdded(context: Context, path: String, dateAddedMs: Long) {
        if (path.isBlank() || dateAddedMs <= 0L) return
        ensureLoaded(context)
        dateAddedByPath[path] = dateAddedMs
        prefs(context).edit().putLong(path, dateAddedMs).apply()
    }

    fun moveDateAdded(context: Context, oldPath: String, newPath: String) {
        if (oldPath.isBlank() || newPath.isBlank() || oldPath == newPath) return
        ensureLoaded(context)
        if (dateAddedByPath.isEmpty()) return
        val existing = dateAddedByPath.remove(oldPath) ?: return
        dateAddedByPath[newPath] = existing
        prefs(context).edit().remove(oldPath).putLong(newPath, existing).apply()
    }

    fun removeDateAdded(context: Context, path: String) {
        if (path.isBlank()) return
        ensureLoaded(context)
        if (dateAddedByPath.isEmpty()) return
        if (dateAddedByPath.remove(path) != null) {
            prefs(context).edit().remove(path).apply()
        }
    }
}
