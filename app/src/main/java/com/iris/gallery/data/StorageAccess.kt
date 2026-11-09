package com.iris.gallery.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Shared answer to "can this app write to shared storage right now?".
 *
 * Android 10 and older use the legacy storage model here
 * (requestLegacyExternalStorage="true" in the manifest), so WRITE_EXTERNAL_STORAGE
 * is what actually lets us copy and delete files in DCIM, Pictures, Downloads and
 * on an SD card. Reporting true without checking it, as the old code did, made every
 * write path try a direct file operation that silently failed.
 */
object StorageAccess {
    fun hasWriteAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
