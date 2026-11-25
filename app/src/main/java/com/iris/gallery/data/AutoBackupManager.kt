package com.iris.gallery.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.iris.gallery.MainActivity
import com.iris.gallery.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "IrisAutoBackup"
private const val SINGLE_BACKUP_FILENAME = "iris-gallery-backup.json"
private const val TEMP_BACKUP_FILENAME = "iris-gallery-backup.tmp"
private const val DATED_BACKUP_PREFIX = "iris-gallery-backup-"
private const val BACKUP_EXT = ".json"
private const val MAX_BACKUP_READ_BYTES = 10 * 1024 * 1024
private const val FAILURE_NOTIFY_THRESHOLD = 3
private const val FAILURE_CHANNEL_ID = "auto_backup_alerts"
private const val FAILURE_NOTIFICATION_ID = 4209
private val DATED_BACKUP_REGEX = Regex("^iris-gallery-backup-\\d{4}-\\d{2}-\\d{2}\\.json$")

data class AutoBackupFolderFile(
    val fileName: String,
    val rawJson: String,
)

data class AutoBackupWriteOutcome(
    val fileName: String,
    val skippedUnchanged: Boolean,
)

private data class TreeChildDoc(
    val docId: String,
    val name: String,
    val lastModified: Long,
    val flags: Int,
)

object AutoBackupManager {

    fun hasPersistedTreePermission(context: Context, treeUriString: String): Boolean {
        if (treeUriString.isBlank()) return false
        val target = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return false
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == target && perm.isReadPermission && perm.isWritePermission
        }
    }

    fun takeTreePermission(context: Context, treeUri: Uri, previousUriString: String = ""): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
            true
        }.getOrElse { error ->
            Log.w(TAG, "Could not persist tree permission for $treeUri", error)
            false
        }
        if (granted && previousUriString.isNotBlank() && previousUriString != treeUri.toString()) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(previousUriString), flags)
            }
        }
        return granted
    }

    fun releaseTreePermission(context: Context, treeUriString: String) {
        if (treeUriString.isBlank()) return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(treeUriString), flags)
        }
    }

    fun formatTreeDisplayName(context: Context, treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull().orEmpty()
        if (docId.contains(':')) {
            val volume = docId.substringBefore(':')
            val relPath = docId.substringAfter(':').trim('/')
            val rootLabel = if (volume.equals("primary", ignoreCase = true)) "Internal Storage" else volume
            return if (relPath.isBlank()) rootLabel else "$rootLabel/$relPath"
        }
        val queried = runCatching {
            val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                rootUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return queried?.ifBlank { null } ?: docId.ifBlank { treeUri.lastPathSegment.orEmpty() }
    }

    fun writeBackupToFolder(
        context: Context,
        treeUriString: String,
        json: String,
        retentionCount: Int,
        contentHash: String = "",
        previousHash: String = "",
        skipIfUnchanged: Boolean = false,
    ): Result<AutoBackupWriteOutcome> = runCatching {
        require(treeUriString.isNotBlank()) { "No backup folder selected" }
        require(hasPersistedTreePermission(context, treeUriString)) {
            "Backup folder permission expired"
        }

        val cr = context.contentResolver
        val treeUri = Uri.parse(treeUriString)
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

        val targetName = if (retentionCount <= 1) {
            SINGLE_BACKUP_FILENAME
        } else {
            val dateStamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            "$DATED_BACKUP_PREFIX$dateStamp$BACKUP_EXT"
        }

        val children = listTreeChildren(cr, treeUri, rootDocId)
        val existingTarget = children.firstOrNull { it.name == targetName }
        if (skipIfUnchanged && existingTarget != null && contentHash.isNotBlank() && contentHash == previousHash) {
            return@runCatching AutoBackupWriteOutcome(fileName = targetName, skippedUnchanged = true)
        }

        val bytes = json.toByteArray(Charsets.UTF_8)

        // Stage into a temporary file first and fsync so a storage-full or killed-process
        // interruption never leaves the real backup file truncated or empty.
        children.filter { it.name == TEMP_BACKUP_FILENAME }.forEach { staleTmp ->
            runCatching {
                DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(treeUri, staleTmp.docId))
            }
        }
        val tmpUri = DocumentsContract.createDocument(cr, parentDocUri, "application/octet-stream", TEMP_BACKUP_FILENAME)
            ?: error("Could not create temporary backup file")
        try {
            writeBytesAndSync(cr, tmpUri, bytes)
        } catch (err: Throwable) {
            runCatching { DocumentsContract.deleteDocument(cr, tmpUri) }
            throw err
        }

        val tmpDoc = listTreeChildren(cr, treeUri, rootDocId).firstOrNull { it.name == TEMP_BACKUP_FILENAME }
        val tmpCanRename = tmpDoc != null && (tmpDoc.flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME) != 0
        val existingCanDelete = existingTarget == null ||
            (existingTarget.flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE) != 0

        var replacedByRename = false
        if (tmpCanRename && existingCanDelete) {
            if (existingTarget != null) {
                val existingUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, existingTarget.docId)
                runCatching { DocumentsContract.deleteDocument(cr, existingUri) }
            }
            val renamed = runCatching {
                DocumentsContract.renameDocument(cr, tmpUri, targetName)
            }.getOrNull()
            if (renamed != null) {
                replacedByRename = true
            }
        }

        if (!replacedByRename) {
            // Fallback for SAF providers without rename support: write directly while tmp still
            // acts as a verified staging check, then remove tmp only after target succeeds.
            val currentChildren = listTreeChildren(cr, treeUri, rootDocId)
            val currentTarget = currentChildren.firstOrNull { it.name == targetName }
            val targetUri = if (currentTarget != null) {
                DocumentsContract.buildDocumentUriUsingTree(treeUri, currentTarget.docId)
            } else {
                DocumentsContract.createDocument(cr, parentDocUri, "application/json", targetName)
                    ?: error("Could not create backup file in folder")
            }
            writeBytesAndSync(cr, targetUri, bytes)
            runCatching { DocumentsContract.deleteDocument(cr, tmpUri) }
        }

        if (retentionCount > 1) {
            val datedDocs = listTreeChildren(cr, treeUri, rootDocId)
                .filter { DATED_BACKUP_REGEX.matches(it.name) }
                .sortedWith(compareByDescending<TreeChildDoc> { it.name }.thenByDescending { it.lastModified })
            val toDelete = datedDocs.drop(retentionCount.coerceAtLeast(1))
            for (stale in toDelete) {
                runCatching {
                    val staleUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, stale.docId)
                    DocumentsContract.deleteDocument(cr, staleUri)
                }
            }
        }

        AutoBackupWriteOutcome(fileName = targetName, skippedUnchanged = false)
    }

    fun readLatestBackupFromFolder(
        context: Context,
        treeUriString: String,
    ): Result<AutoBackupFolderFile> = runCatching {
        require(treeUriString.isNotBlank()) { "No backup folder selected" }
        require(hasPersistedTreePermission(context, treeUriString)) {
            "Backup folder permission expired"
        }

        val cr = context.contentResolver
        val treeUri = Uri.parse(treeUriString)
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val candidates = listTreeChildren(cr, treeUri, rootDocId)
            .filter {
                it.name == SINGLE_BACKUP_FILENAME ||
                    (it.name.startsWith(DATED_BACKUP_PREFIX) && it.name.endsWith(BACKUP_EXT))
            }
            .sortedWith(compareByDescending<TreeChildDoc> { it.lastModified }.thenByDescending { it.name })

        require(candidates.isNotEmpty()) { "No Iris Gallery backup found in folder" }

        var lastError: Throwable? = null
        for (candidate in candidates) {
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, candidate.docId)
            val readResult = runCatching {
                val raw = readBoundedUtf8(cr, fileUri)
                BackupManager.parseBackupPreview(raw).getOrThrow()
                AutoBackupFolderFile(fileName = candidate.name, rawJson = raw)
            }
            if (readResult.isSuccess) return@runCatching readResult.getOrThrow()
            lastError = readResult.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("No valid backup found in folder")
    }

    fun runScheduledBackup(context: Context): Result<AutoBackupWriteOutcome> {
        val settingsPreferences = SettingsPreferences(context)
        val settings = settingsPreferences.state.value
        if (!settings.autoBackupEnabled || settings.autoBackupTreeUri.isBlank()) {
            return Result.failure(IllegalStateException("Auto-backup is not configured"))
        }

        val libraryPreferences = LibraryPreferences(context)
        val galleryPrefs = context.getSharedPreferences("gallery", Context.MODE_PRIVATE)
        val favorites = galleryPrefs.getStringSet("favorites", emptySet())
            .orEmpty()
            .mapNotNullTo(mutableSetOf(), String::toLongOrNull)
        val images = MediaRepository(context).loadSnapshot()
        val built = BackupManager.buildBackup(
            settings = settings,
            library = libraryPreferences.state.value,
            customTitles = libraryPreferences.getAllCustomTitles(),
            favorites = favorites,
            images = images,
        )

        val writeResult = writeBackupToFolder(
            context = context,
            treeUriString = settings.autoBackupTreeUri,
            json = built.json,
            retentionCount = settings.autoBackupRetention,
            contentHash = built.contentHash,
            previousHash = settings.lastAutoBackupHash,
            skipIfUnchanged = true,
        )
        writeResult
            .onSuccess { outcome ->
                val timestamp = if (outcome.skippedUnchanged && settings.lastAutoBackupTimestamp > 0L) {
                    settings.lastAutoBackupTimestamp
                } else {
                    System.currentTimeMillis()
                }
                settingsPreferences.recordAutoBackupSuccess(
                    timestamp = timestamp,
                    contentHash = built.contentHash,
                )
            }
            .onFailure { err ->
                val failures = settingsPreferences.recordAutoBackupFailure(err.message ?: "Backup failed")
                notifyFailureIfNeeded(context, failures)
            }
        return writeResult
    }

    fun notifyFailureIfNeeded(context: Context, consecutiveFailures: Int) {
        if (consecutiveFailures != FAILURE_NOTIFY_THRESHOLD) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    FAILURE_CHANNEL_ID,
                    context.getString(R.string.notification_channel_backup),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            val openIntent = PendingIntent.getActivity(
                context,
                FAILURE_NOTIFICATION_ID,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, FAILURE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_auto_backup_failed_title))
                .setContentText(context.getString(R.string.notification_auto_backup_failed_desc))
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            manager.notify(FAILURE_NOTIFICATION_ID, notification)
        }
    }

    private fun writeBytesAndSync(cr: ContentResolver, uri: Uri, bytes: ByteArray) {
        val out = cr.openOutputStream(uri, "wt")
            ?: cr.openOutputStream(uri, "w")
            ?: error("Could not open backup file for writing")
        out.use { stream ->
            if (stream is FileOutputStream) {
                runCatching { stream.channel.truncate(0L) }
            }
            stream.write(bytes)
            stream.flush()
            if (stream is FileOutputStream) {
                runCatching { stream.fd.sync() }
            }
        }
    }

    private fun listTreeChildren(
        cr: ContentResolver,
        treeUri: Uri,
        rootDocId: String,
    ): List<TreeChildDoc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
        val results = mutableListOf<TreeChildDoc>()
        cr.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val flagsCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            while (cursor.moveToNext()) {
                val docId = if (idCol >= 0) cursor.getString(idCol).orEmpty() else ""
                val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                val lastMod = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L
                val flags = if (flagsCol >= 0 && !cursor.isNull(flagsCol)) cursor.getInt(flagsCol) else 0
                if (docId.isNotBlank() && name.isNotBlank()) {
                    results.add(TreeChildDoc(docId = docId, name = name, lastModified = lastMod, flags = flags))
                }
            }
        }
        return results
    }

    private fun readBoundedUtf8(cr: ContentResolver, uri: Uri): String {
        val bytes = cr.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                require(total <= MAX_BACKUP_READ_BYTES) { "Backup file is too large" }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: error("Could not open backup file")
        return String(bytes, Charsets.UTF_8)
    }
}

object AutoBackupScheduler {
    private const val ALARM_REQUEST_CODE = 4208

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, AutoBackupReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarm.cancel(pending)
            pending.cancel()
        }
    }

    fun scheduleFromSettings(context: Context) {
        val settings = SettingsPreferences(context).state.value
        if (!settings.autoBackupEnabled || settings.autoBackupTreeUri.isBlank()) {
            cancel(context)
            return
        }
        val intervalMs = when (settings.autoBackupFrequency) {
            AutoBackupFrequency.ON_CHANGE -> AlarmManager.INTERVAL_DAY
            AutoBackupFrequency.DAILY -> AlarmManager.INTERVAL_DAY
            AutoBackupFrequency.WEEKLY -> AlarmManager.INTERVAL_DAY * 7L
        }
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, AutoBackupReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val now = System.currentTimeMillis()
        val lastTs = settings.lastAutoBackupTimestamp
        // Anchor next trigger to last successful backup so repeated app launches never push
        // Daily or Weekly schedules indefinitely into the future.
        val firstTriggerAt = if (lastTs > 0L) {
            val nextDue = lastTs + intervalMs
            if (nextDue <= now) now + 60_000L else nextDue
        } else {
            now + intervalMs
        }
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTriggerAt,
            intervalMs,
            pending,
        )
    }
}

class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsPreferences(context).state.value
        if (!settings.autoBackupEnabled || settings.autoBackupTreeUri.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AutoBackupManager.runScheduledBackup(context.applicationContext)
            } catch (error: Throwable) {
                Log.e(TAG, "Scheduled auto-backup failed", error)
            } finally {
                pending.finish()
            }
        }
    }
}
