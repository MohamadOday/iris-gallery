package com.iris.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AtomicFile
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

data class VaultItem(
    val id: Long,
    val vaultFileName: String,
    val originalName: String,
    val originalPath: String,
    val originalBucketName: String,
    val originalBucketId: Long,
    val dateTaken: Long,
    val dateLocked: Long,
    val width: Int,
    val height: Int,
    val isVideo: Boolean,
    val durationMs: Long,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val orientation: Int = 0,
    val title: String = "",
    val description: String = "",
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
)

data class VaultMoveResult(
    val vaultedMedia: List<MediaImage>,
    val originalMedia: List<MediaImage>,
    val silentSuccess: Boolean = false,
    val indexWriteFailed: Boolean = false,
    val deletedOriginalIds: Set<Long> = emptySet(),
    val failedCount: Int = 0,
)

data class VaultRestoreResult(
    val restored: List<MediaImage>,
    val restoredOriginalPaths: Set<String> = emptySet(),
    val failed: Int = 0,
)

private data class VaultRestoreTarget(val uri: Uri, val path: String, val needsMediaScan: Boolean = false)

private data class VaultRestoreDestination(val volume: String, val relativePath: String)

private const val VAULT_INDEX_CHECKPOINT_INTERVAL = 20
private const val VAULT_RECOVERED_BUCKET = "Recovered"
private val VAULT_VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "3gp", "3g2", "m4v",
    "mpeg", "mpg", "ts", "flv", "wmv", "mts", "m2ts"
)

class VaultRepository(private val context: Context) {
    private val vaultDir: File = File(context.filesDir, "vault").apply {
        if (!exists()) {
            mkdirs()
        }
        val nomedia = File(this, ".nomedia")
        if (!nomedia.exists()) {
            runCatching { nomedia.createNewFile() }
        }
    }

    private val indexFile = AtomicFile(File(context.filesDir, "vault_index.json"))

    // Every index read-modify-write sequence runs under this lock so a slow batch
    // cannot overwrite an index update from a concurrent operation. Reentrant by
    // design: load calls nested inside a locked operation are safe.
    private val indexLock = Any()

    private val _vaultMedia = MutableStateFlow<List<MediaImage>>(emptyList())
    val vaultMedia: StateFlow<List<MediaImage>> = _vaultMedia.asStateFlow()

    init {
        loadVaultItems()
    }

    fun hasAllFilesAccess(): Boolean = StorageAccess.hasWriteAccess(context)

    fun loadVaultItems(): List<MediaImage> = synchronized(indexLock) {
        val items = readIndexStrict() ?: salvageCorruptIndex()
        val sortedItems = items.sortedWith(
            compareByDescending<VaultItem> { it.dateLocked }
                .thenByDescending { it.dateTaken }
                .thenByDescending { it.id }
        )
        val mediaList = sortedItems.mapNotNull { item ->
            val file = File(vaultDir, item.vaultFileName)
            if (file.exists() && file.length() > 0L) {
                val uniqueId = if (item.id > 0) -item.id else item.id
                MediaImage(
                    id = uniqueId,
                    uri = Uri.fromFile(file),
                    name = item.originalName,
                    dateTaken = item.dateTaken,
                    width = item.width,
                    height = item.height,
                    path = file.absolutePath,
                    bucketId = if (item.originalBucketId != 0L) item.originalBucketId else -1L,
                    bucketName = item.originalBucketName.ifBlank { "Locked" },
                    isVideo = item.isVideo,
                    durationMs = item.durationMs,
                    mimeType = item.mimeType,
                    sizeBytes = if (item.sizeBytes > 0) item.sizeBytes else file.length(),
                    orientation = item.orientation,
                    title = item.title,
                    description = item.description,
                    dateModified = item.dateModified,
                    dateAdded = item.dateAdded,
                )
            } else {
                null
            }
        }
        _vaultMedia.value = mediaList
        mediaList
    }

    /**
     * Adopts files that exist in the vault directory but are missing from the index. These are
     * leftovers from vault moves that were interrupted before the index was persisted; without
     * this pass they would stay invisible to the app forever.
     */
    suspend fun recoverOrphanedItems(): Int = indexLocked {
        // Clean up any leftover staging files from interrupted moves.
        vaultDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".iris_tmp") || file.name.endsWith(".tmp"))) {
                runCatching { file.delete() }
            }
        }

        val initialItems = readIndexStrict() ?: salvageCorruptIndex()
        // Prune index entries whose underlying vault files no longer exist on disk.
        val validItems = initialItems.filter { item ->
            val f = File(vaultDir, item.vaultFileName)
            f.exists() && f.length() > 0L
        }.toMutableList()
        val prunedStaleEntries = validItems.size != initialItems.size

        val knownNames = validItems.mapTo(mutableSetOf()) { it.vaultFileName }
        val usedIds = validItems.mapTo(mutableSetOf()) { it.id }
        val orphans = (vaultDir.listFiles() ?: emptyArray())
            .filter { file ->
                file.isFile &&
                    !file.name.startsWith(".") &&
                    !file.name.endsWith(".iris_tmp") &&
                    !file.name.endsWith(".tmp") &&
                    file.name !in knownNames &&
                    file.length() > 0L
            }
            .sortedBy { it.name }

        if (orphans.isEmpty()) {
            if (prunedStaleEntries) {
                persistIndexVerified(validItems)
                loadVaultItems()
            }
            return@indexLocked 0
        }

        var recovered = 0
        for (file in orphans) {
            val parsedId = parseVaultFileId(file.name)
            val uniqueId = if (parsedId != null && parsedId !in usedIds) parsedId else generateUniqueId(validItems)
            usedIds.add(uniqueId)
            val isVideo = isVideoFileName(file.name)
            val meta = extractOrphanMetadata(file, isVideo)
            val lastMod = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
            validItems.add(
                VaultItem(
                    id = uniqueId,
                    vaultFileName = file.name,
                    originalName = stripVaultPrefix(file.name),
                    originalPath = "",
                    originalBucketName = VAULT_RECOVERED_BUCKET,
                    originalBucketId = 0L,
                    dateTaken = meta.dateTaken.takeIf { it > 0L } ?: lastMod,
                    dateLocked = lastMod,
                    width = meta.width,
                    height = meta.height,
                    isVideo = isVideo,
                    durationMs = meta.durationMs,
                    mimeType = mimeTypeForVaultFile(file.name, isVideo),
                    sizeBytes = file.length(),
                    orientation = meta.orientation,
                    title = "",
                    description = "",
                    dateModified = lastMod,
                    dateAdded = lastMod,
                )
            )
            recovered++
        }

        if (!persistIndexVerified(validItems)) {
            return@indexLocked 0
        }
        loadVaultItems()
        recovered
    }

    suspend fun moveToVault(
        mediaList: List<MediaImage>,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): VaultMoveResult = indexLocked {
        val currentItems = (readIndexStrict() ?: salvageCorruptIndex()).toMutableList()
        val vaultedList = mutableListOf<MediaImage>()
        val originalList = mutableListOf<MediaImage>()
        val hasManagerAccess = hasAllFilesAccess()
        var indexWriteFailed = false
        var copiesSinceCheckpoint = 0
        var failedCount = 0
        val total = mediaList.size

        for ((index, item) in mediaList.withIndex()) {
            onProgress?.invoke(index, total)
            val uniqueId = generateUniqueId(currentItems)
            val prefix = "${uniqueId}_"
            val cleanName = truncateToFileSystemLimit(
                prefix,
                sanitizeFileName(item.name.ifBlank { "media_$uniqueId" })
            )
            val vaultFileName = prefix + cleanName
            val targetFile = File(vaultDir, vaultFileName)
            val tempFile = File(vaultDir, ".${vaultFileName}.iris_tmp")

            val srcFile = if (item.path.isNotBlank()) File(item.path) else null
            val expectedBytes = when {
                srcFile != null && srcFile.exists() && srcFile.length() > 0L -> srcFile.length()
                item.sizeBytes > 0L -> item.sizeBytes
                else -> 0L
            }

            var copied = false
            if (srcFile != null && srcFile.exists()) {
                copied = runCatching {
                    srcFile.copyTo(tempFile, overwrite = true)
                    val copiedBytes = tempFile.length()
                    tempFile.exists() && copiedBytes > 0L && (expectedBytes <= 0L || copiedBytes == expectedBytes)
                }.getOrDefault(false)
            }

            if (!copied) {
                runCatching { tempFile.delete() }
                copied = runCatching {
                    val stream = context.contentResolver.openInputStream(item.uri)
                        ?: canonicalSourceUri(item)?.let { context.contentResolver.openInputStream(it) }
                    stream?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                    }
                    val copiedBytes = tempFile.length()
                    tempFile.exists() && copiedBytes > 0L && (expectedBytes <= 0L || copiedBytes == expectedBytes)
                }.getOrDefault(false)
            }

            val finalized = if (copied) {
                runCatching {
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = tempFile.renameTo(targetFile)
                    if (!renamed) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                    val finalBytes = targetFile.length()
                    targetFile.exists() && finalBytes > 0L && (expectedBytes <= 0L || finalBytes == expectedBytes)
                }.getOrDefault(false)
            } else {
                false
            }

            if (!finalized) {
                runCatching { tempFile.delete() }
                runCatching { targetFile.delete() }
                failedCount++
                continue
            }

            val effectiveMod = when {
                item.dateModified > 0L -> item.dateModified
                srcFile != null && srcFile.lastModified() > 0L -> srcFile.lastModified()
                item.dateTaken > 0L -> item.dateTaken
                else -> System.currentTimeMillis()
            }
            if (effectiveMod > 0L) {
                runCatching { targetFile.setLastModified(effectiveMod) }
            }

            val nowLocked = System.currentTimeMillis()
            val vaultItem = VaultItem(
                id = uniqueId,
                vaultFileName = vaultFileName,
                originalName = item.name,
                originalPath = item.path,
                originalBucketName = item.bucketName,
                originalBucketId = item.bucketId,
                dateTaken = item.dateTaken,
                dateLocked = nowLocked,
                width = item.width,
                height = item.height,
                isVideo = item.isVideo,
                durationMs = item.durationMs,
                mimeType = item.mimeType,
                sizeBytes = targetFile.length(),
                orientation = item.orientation,
                title = item.title,
                description = item.description,
                dateModified = effectiveMod,
                dateAdded = item.dateAdded,
            )
            currentItems.add(vaultItem)
            val media = MediaImage(
                id = -uniqueId,
                uri = Uri.fromFile(targetFile),
                name = item.name,
                dateTaken = item.dateTaken,
                width = item.width,
                height = item.height,
                path = targetFile.absolutePath,
                bucketId = if (item.bucketId != 0L) item.bucketId else -1L,
                bucketName = item.bucketName.ifBlank { "Locked" },
                isVideo = item.isVideo,
                durationMs = item.durationMs,
                mimeType = item.mimeType,
                sizeBytes = targetFile.length(),
                orientation = item.orientation,
                title = item.title,
                description = item.description,
                dateModified = effectiveMod,
                dateAdded = item.dateAdded,
            )
            vaultedList.add(media)
            originalList.add(item)
            copiesSinceCheckpoint++

            // Persist the index periodically so a process kill mid-batch cannot orphan
            // already-copied files.
            if (copiesSinceCheckpoint >= VAULT_INDEX_CHECKPOINT_INTERVAL) {
                if (!writeIndex(currentItems)) {
                    indexWriteFailed = true
                    break
                }
                copiesSinceCheckpoint = 0
            }
        }
        onProgress?.invoke(total, total)

        if (!indexWriteFailed && vaultedList.isNotEmpty()) {
            indexWriteFailed = !persistIndexVerified(currentItems)
        }

        // Originals are only ever removed after the index is safely on disk.
        var silentSuccess = false
        val deletedOriginalIds = mutableSetOf<Long>()
        if (!indexWriteFailed && hasManagerAccess && vaultedList.isNotEmpty()) {
            for (item in originalList) {
                if (deleteFromStorage(item)) {
                    deletedOriginalIds.add(item.id)
                }
            }
            silentSuccess = deletedOriginalIds.size == originalList.size
        }

        loadVaultItems()
        VaultMoveResult(
            vaultedMedia = vaultedList,
            originalMedia = originalList,
            silentSuccess = silentSuccess,
            indexWriteFailed = indexWriteFailed,
            deletedOriginalIds = deletedOriginalIds,
            failedCount = failedCount,
        )
    }

    private fun canonicalSourceUri(item: MediaImage): Uri? {
        if (item.id <= 0) return null
        return if (item.isVideo) {
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        } else {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        }
    }

    private fun deleteFromStorage(item: MediaImage): Boolean {
        var deleted = false
        if (item.path.isNotBlank()) {
            val file = File(item.path)
            if (file.exists()) {
                val fDel = runCatching { file.delete() }.getOrDefault(false)
                val cDel = if (!fDel && file.exists()) runCatching { file.canonicalFile.delete() }.getOrDefault(false) else false
                deleted = fDel || cDel
            } else {
                // The original is already gone on disk, so the vault copy is the only one left.
                deleted = true
            }
        }
        val mediaVolume = if (item.uri.authority == "media") {
            item.uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: "external"
        } else "external"
        val mediaStoreUri = if (item.uri.authority == "media") {
            item.uri.buildUpon().clearQuery().build()
        } else if (item.id > 0) {
            if (item.isVideo) ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
            else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        } else {
            item.uri
        }
        val contentDeleted = runCatching {
            context.contentResolver.delete(mediaStoreUri, null, null) > 0
        }.getOrDefault(false)
        deleted = deleted || contentDeleted

        if (item.id > 0) {
            val table = if (item.isVideo) MediaStore.Video.Media.getContentUri(mediaVolume) else MediaStore.Images.Media.getContentUri(mediaVolume)
            val idDeleted = runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString())) > 0
            }.getOrDefault(false)
            deleted = deleted || idDeleted
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri(mediaVolume), "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString()))
            }
            if (mediaVolume == "external" && Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    context.contentResolver.delete(MediaStore.Files.getContentUri("external_primary"), "${MediaStore.MediaColumns._ID}=?", arrayOf(item.id.toString()))
                }
            }
        }

        if (item.uri != mediaStoreUri) {
            val genericDeleted = runCatching {
                context.contentResolver.delete(item.uri, null, null) > 0
            }.getOrDefault(false)
            deleted = deleted || genericDeleted
        }

        if (item.path.isNotBlank()) {
            val table = if (item.isVideo) MediaStore.Video.Media.getContentUri(mediaVolume) else MediaStore.Images.Media.getContentUri(mediaVolume)
            val pathDeleted = runCatching {
                context.contentResolver.delete(table, "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path)) > 0
            }.getOrDefault(false)
            deleted = deleted || pathDeleted
            runCatching {
                context.contentResolver.delete(MediaStore.Files.getContentUri(mediaVolume), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
            }
            if (mediaVolume == "external" && Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    context.contentResolver.delete(MediaStore.Files.getContentUri("external_primary"), "${MediaStore.MediaColumns.DATA}=?", arrayOf(item.path))
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
            if (!deleted && !File(item.path).exists()) {
                deleted = true
            }
        }
        return deleted
    }

    suspend fun rollbackVault(vaultedItems: List<MediaImage>) = indexLocked {
        val currentItems = (readIndexStrict() ?: salvageCorruptIndex()).toMutableList()
        val idsToRemove = vaultedItems.map { abs(it.id) }.toSet()
        val preservedVaultPaths = mutableSetOf<String>()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove) {
                val originalIntact = item.originalPath.isBlank() ||
                    File(item.originalPath).let { it.exists() && it.length() > 0L }
                if (originalIntact) {
                    val file = File(vaultDir, item.vaultFileName)
                    runCatching { file.delete() }
                    true
                } else {
                    // Never delete the vault copy if the original file on storage is already gone.
                    preservedVaultPaths.add(File(vaultDir, item.vaultFileName).absolutePath)
                    false
                }
            } else {
                false
            }
        }
        // Also drop copies that never made it into the index (interrupted/broken batch persists),
        // unless they are preserved because their original was already deleted.
        for (media in vaultedItems) {
            if (media.path.startsWith(vaultDir.absolutePath) && media.path !in preservedVaultPaths) {
                val indexedMatch = currentItems.any { File(vaultDir, it.vaultFileName).absolutePath == media.path }
                if (!indexedMatch) {
                    runCatching { File(media.path).delete() }
                }
            }
        }
        writeIndex(currentItems)
        loadVaultItems()
    }

    suspend fun restoreFromVault(
        mediaList: List<MediaImage>,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): VaultRestoreResult = indexLocked {
        val currentItems = (readIndexStrict() ?: salvageCorruptIndex()).toMutableList()
        val restoredList = mutableListOf<MediaImage>()
        val restoredOriginalPaths = mutableSetOf<String>()
        val pathsToScan = mutableListOf<Pair<String, String?>>()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()
        var failedCount = 0

        val itemsToRestore = currentItems.filter { it.id in idsToRemove }
        val total = itemsToRestore.size
        for ((index, item) in itemsToRestore.withIndex()) {
            onProgress?.invoke(index, total)
            val vaultFile = File(vaultDir, item.vaultFileName)
            if (!vaultFile.exists()) {
                currentItems.remove(item)
                continue
            }

            val restored = runCatching { restoreFileToStorage(item, vaultFile) }
                .onFailure { android.util.Log.e("IrisVault", "Restore failed for ${item.originalName}", it) }
                .getOrNull()
            if (restored != null) {
                runCatching { vaultFile.delete() }
                currentItems.remove(item)
                if (item.originalPath.isNotBlank()) {
                    restoredOriginalPaths.add(item.originalPath)
                }
                if (restored.path.isNotBlank()) {
                    restoredOriginalPaths.add(restored.path)
                    if (restored.needsMediaScan) {
                        pathsToScan.add(restored.path to item.mimeType.ifBlank { null })
                    }
                }
                restoredList.add(
                    MediaImage(
                        id = -item.id,
                        uri = restored.uri,
                        name = item.originalName,
                        dateTaken = item.dateTaken,
                        width = item.width,
                        height = item.height,
                        path = restored.path,
                        bucketId = item.originalBucketId,
                        bucketName = item.originalBucketName,
                        isVideo = item.isVideo,
                        durationMs = item.durationMs,
                        mimeType = item.mimeType,
                        sizeBytes = item.sizeBytes,
                        orientation = item.orientation,
                        title = item.title,
                        description = item.description,
                        dateModified = item.dateModified,
                        dateAdded = item.dateAdded,
                    )
                )
            } else {
                failedCount++
            }
        }
        onProgress?.invoke(total, total)

        writeIndex(currentItems)
        loadVaultItems()

        if (pathsToScan.isNotEmpty()) {
            awaitMediaScan(pathsToScan)
        }

        VaultRestoreResult(
            restored = restoredList,
            restoredOriginalPaths = restoredOriginalPaths,
            failed = failedCount,
        )
    }

    suspend fun updateItemMetadata(id: Long, request: ExifEditRequest): MediaImage? = indexLocked {
        val targetId = abs(id)
        val currentItems = (readIndexStrict() ?: salvageCorruptIndex()).toMutableList()
        val index = currentItems.indexOfFirst { it.id == targetId }
        if (index < 0) return@indexLocked null
        val existing = currentItems[index]
        val newDescription = when {
            request.imageDescription != null -> request.imageDescription
            request.stripAllExif -> ""
            else -> existing.description
        }
        val updatedItem = existing.copy(
            title = request.title,
            description = newDescription,
            dateTaken = request.dateTakenMillis,
            orientation = request.orientation,
        )
        currentItems[index] = updatedItem
        writeIndex(currentItems)
        val reloaded = loadVaultItems()
        reloaded.firstOrNull { abs(it.id) == targetId }
    }

    suspend fun deletePermanently(mediaList: List<MediaImage>) = indexLocked {
        val currentItems = (readIndexStrict() ?: salvageCorruptIndex()).toMutableList()
        val idsToRemove = mediaList.map { abs(it.id) }.toSet()

        currentItems.removeAll { item ->
            if (item.id in idsToRemove) {
                val vaultFile = File(vaultDir, item.vaultFileName)
                runCatching { vaultFile.delete() }
                true
            } else {
                false
            }
        }
        writeIndex(currentItems)
        loadVaultItems()
    }

    private fun restoreFileToStorage(item: VaultItem, vaultFile: File): VaultRestoreTarget? {
        val effectiveModMs = when {
            item.dateModified > 0L -> item.dateModified
            item.dateTaken > 0L -> item.dateTaken
            else -> vaultFile.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
        }
        val effectiveTakenMs = if (item.dateTaken > 0L) item.dateTaken else effectiveModMs
        val effectiveModSec = effectiveModMs / 1000L
        val effectiveAddedSec = if (item.dateAdded > 0L) item.dateAdded / 1000L else effectiveModSec

        // 1. Prefer a direct file copy when the app has broad storage access, keeping the
        // exact original folder and filename (including on an SD card).
        if (hasAllFilesAccess()) {
            restoreDirect(item, vaultFile, effectiveModMs, includeFallback = false)?.let { return it }
        }

        // 2. Scoped storage on Android 10+: insert into the writable volume (VOLUME_EXTERNAL_PRIMARY
        // or the original SD card volume), falling back to Pictures/Iris or Movies/Iris.
        if (Build.VERSION.SDK_INT >= 29) {
            restoreViaMediaStore(
                item, vaultFile, effectiveModMs, effectiveTakenMs, effectiveModSec, effectiveAddedSec
            )?.let { return it }
        }

        // 3. Last resort for Android 9 and below, or when MediaStore refused the insert.
        if (hasAllFilesAccess() || Build.VERSION.SDK_INT < 29) {
            restoreDirect(item, vaultFile, effectiveModMs, includeFallback = true)?.let { return it }
        }
        android.util.Log.e("IrisVault", "Could not restore ${item.originalName} from vault")
        return null
    }

    private fun restoreDirect(
        item: VaultItem,
        vaultFile: File,
        effectiveModMs: Long,
        includeFallback: Boolean,
    ): VaultRestoreTarget? {
        val preferredParent = if (item.originalPath.isNotBlank()) File(item.originalPath).parentFile else null
        @Suppress("DEPRECATION")
        val fallbackParent = Environment.getExternalStoragePublicDirectory(
            if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        ).resolve("Iris")
        val parents = buildList {
            if (preferredParent != null) add(preferredParent)
            if (includeFallback) add(fallbackParent)
        }.distinctBy { it.absolutePath }
        if (parents.isEmpty()) return null
        val expectedBytes = vaultFile.length()
        for (parent in parents) {
            if (!parent.isDirectory && !runCatching { parent.mkdirs() }.getOrDefault(false)) continue
            val destFile = uniqueDestinationFile(parent, item.originalName.ifBlank { vaultFile.name })
            val copied = runCatching {
                vaultFile.copyTo(destFile, overwrite = false)
                destFile.exists() && destFile.length() > 0L && (expectedBytes <= 0L || destFile.length() == expectedBytes)
            }.getOrDefault(false)
            if (!copied) {
                runCatching { destFile.delete() }
                continue
            }
            if (effectiveModMs > 0L) {
                runCatching { destFile.setLastModified(effectiveModMs) }
            }
            return VaultRestoreTarget(
                uri = Uri.fromFile(destFile),
                path = destFile.absolutePath,
                needsMediaScan = true,
            )
        }
        return null
    }

    private fun restoreViaMediaStore(
        item: VaultItem,
        vaultFile: File,
        effectiveModMs: Long,
        effectiveTakenMs: Long,
        effectiveModSec: Long,
        effectiveAddedSec: Long,
    ): VaultRestoreTarget? {
        val defaultDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val preferred = resolveRestoreDestination(item.originalPath, item.isVideo)
        val fallback = VaultRestoreDestination(MediaStore.VOLUME_EXTERNAL_PRIMARY, "$defaultDir/Iris/")
        val destinations = if (preferred == fallback) listOf(preferred) else listOf(preferred, fallback)
        for (destination in destinations) {
            val target = tryRestoreViaMediaStore(
                item, vaultFile, destination,
                effectiveModMs, effectiveTakenMs, effectiveModSec, effectiveAddedSec
            )
            if (target != null) return target
        }
        return null
    }

    private fun tryRestoreViaMediaStore(
        item: VaultItem,
        vaultFile: File,
        destination: VaultRestoreDestination,
        effectiveModMs: Long,
        effectiveTakenMs: Long,
        effectiveModSec: Long,
        effectiveAddedSec: Long,
    ): VaultRestoreTarget? {
        val displayName = item.originalName.ifBlank { stripVaultPrefix(vaultFile.name) }
        val resolvedMime = item.mimeType.ifBlank { mimeTypeForVaultFile(displayName, item.isVideo) }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, resolvedMime)
            put(MediaStore.MediaColumns.DATE_ADDED, effectiveAddedSec)
            put(MediaStore.MediaColumns.DATE_MODIFIED, effectiveModSec)
            put(MediaStore.MediaColumns.DATE_TAKEN, effectiveTakenMs)
            if (!item.isVideo) {
                put(MediaStore.Images.Media.ORIENTATION, item.orientation)
            }
            if (item.title.isNotBlank()) put(MediaStore.MediaColumns.TITLE, item.title)
            if (item.isVideo && item.durationMs > 0L) {
                put(MediaStore.Video.VideoColumns.DURATION, item.durationMs)
            }
            put(MediaStore.MediaColumns.RELATIVE_PATH, destination.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (item.isVideo) {
            MediaStore.Video.Media.getContentUri(destination.volume)
        } else {
            MediaStore.Images.Media.getContentUri(destination.volume)
        }
        val newUri = runCatching { context.contentResolver.insert(collection, values) }.getOrNull() ?: return null
        val expectedBytes = vaultFile.length()
        val written = runCatching {
            var copiedBytes = 0L
            context.contentResolver.openOutputStream(newUri)?.use { output ->
                vaultFile.inputStream().use { input ->
                    copiedBytes = input.copyTo(output)
                }
            } ?: error("Could not open restore output")
            copiedBytes > 0L && (expectedBytes <= 0L || copiedBytes == expectedBytes)
        }.getOrDefault(false)
        if (!written) {
            runCatching { context.contentResolver.delete(newUri, null, null) }
            return null
        }
        val pendingPath = mediaStorePath(newUri)
        if (effectiveModMs > 0L && pendingPath.isNotBlank()) {
            runCatching { File(pendingPath).setLastModified(effectiveModMs) }
        }
        val updateValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            put(MediaStore.MediaColumns.DATE_MODIFIED, effectiveModSec)
            put(MediaStore.MediaColumns.DATE_TAKEN, effectiveTakenMs)
            put(MediaStore.MediaColumns.DATE_ADDED, effectiveAddedSec)
        }
        if (runCatching { context.contentResolver.update(newUri, updateValues, null, null) }.getOrDefault(0) <= 0) {
            runCatching { context.contentResolver.delete(newUri, null, null) }
            return null
        }
        return VaultRestoreTarget(
            uri = newUri,
            path = mediaStorePath(newUri).ifBlank { pendingPath },
            needsMediaScan = false,
        )
    }

    private suspend fun awaitMediaScan(pathsWithMime: List<Pair<String, String?>>) {
        val paths = pathsWithMime.map { it.first }.toTypedArray()
        val mimes = pathsWithMime.map { it.second }.toTypedArray()
        withTimeoutOrNull(3_000L) {
            suspendCancellableCoroutine { cont ->
                var remaining = paths.size
                MediaScannerConnection.scanFile(context, paths, mimes) { _, _ ->
                    synchronized(paths) {
                        remaining--
                        if (remaining <= 0 && cont.isActive) {
                            cont.resume(Unit)
                        }
                    }
                }
            }
        }
    }

    /** Where a restore through MediaStore actually landed. */
    private fun mediaStorePath(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull().orEmpty()

    private fun resolveRestoreDestination(originalPath: String, isVideo: Boolean): VaultRestoreDestination {
        val defaultDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val fallback = VaultRestoreDestination(MediaStore.VOLUME_EXTERNAL_PRIMARY, "$defaultDir/Iris/")
        if (originalPath.isBlank()) return fallback

        val parent = File(originalPath).parentFile?.absolutePath.orEmpty()
        if (parent.isBlank()) return fallback
        var volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        var relativeParent = ""
        when {
            parent.startsWith("/storage/emulated/0/") -> {
                relativeParent = parent.removePrefix("/storage/emulated/0/")
            }
            parent.startsWith("/storage/self/primary/") -> {
                relativeParent = parent.removePrefix("/storage/self/primary/")
            }
            parent.startsWith("/storage/") -> {
                val rest = parent.removePrefix("/storage/")
                val candidateVolume = rest.substringBefore('/', "")
                val afterVolume = rest.substringAfter('/', "")
                val knownVolumes = if (Build.VERSION.SDK_INT >= 29) {
                    runCatching { MediaStore.getExternalVolumeNames(context) }.getOrDefault(emptySet())
                } else {
                    emptySet()
                }
                if (candidateVolume.isNotBlank() && afterVolume.isNotBlank() && candidateVolume in knownVolumes) {
                    volume = candidateVolume
                    relativeParent = afterVolume
                }
            }
        }
        relativeParent = relativeParent.trim('/')
        if (relativeParent.isBlank()) return fallback
        if (relativeParent.startsWith("Android/data", ignoreCase = true) ||
            relativeParent.startsWith("Android/obb", ignoreCase = true)
        ) {
            return fallback
        }
        return VaultRestoreDestination(volume, "$relativeParent/")
    }

    private data class OrphanMetadata(
        val width: Int = 0,
        val height: Int = 0,
        val orientation: Int = 0,
        val durationMs: Long = 0L,
        val dateTaken: Long = 0L,
    )

    private fun extractOrphanMetadata(file: File, isVideo: Boolean): OrphanMetadata {
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(file.absolutePath)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                OrphanMetadata(width = w, height = h, orientation = rot, durationMs = dur)
            } catch (e: Exception) {
                OrphanMetadata()
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            return try {
                val exif = ExifInterface(file)
                val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                val orientation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                val dateTaken = if (!dateStr.isNullOrBlank()) {
                    runCatching {
                        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateStr)?.time ?: 0L
                    }.getOrDefault(0L)
                } else 0L
                OrphanMetadata(width = w, height = h, orientation = orientation, dateTaken = dateTaken)
            } catch (e: Exception) {
                OrphanMetadata()
            }
        }
    }

    private fun generateUniqueId(existing: List<VaultItem>): Long {
        var id = System.currentTimeMillis()
        val ids = existing.map { it.id }.toSet()
        while (id in ids || id <= 0) {
            id++
        }
        return id
    }

    /**
     * Keeps non-ASCII (e.g. Chinese) names intact. Only path separators and control characters are
     * replaced, since those would break file access.
     */
    private fun sanitizeFileName(name: String): String {
        val cleaned = buildString(name.length) {
            for (ch in name) {
                append(if (ch.isISOControl() || ch == '/' || ch == '\\') '_' else ch)
            }
        }.trim().trimEnd('.')
        return cleaned.ifBlank { "media" }
    }

    /**
     * Trims the name so `prefix + name` fits the 255-byte file name limit used by ext4/f2fs.
     * The extension is preserved whenever possible.
     */
    private fun truncateToFileSystemLimit(prefix: String, name: String): String {
        val maxBytes = 255 - (prefix.toByteArray(Charsets.UTF_8).size + 4)
        if (name.toByteArray(Charsets.UTF_8).size <= maxBytes) return name
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extensionCandidate = if (dot > 0) name.substring(dot) else ""
        val extension = if (extensionCandidate.toByteArray(Charsets.UTF_8).size <= 24) extensionCandidate else ""
        val budget = (maxBytes - extension.toByteArray(Charsets.UTF_8).size).coerceAtLeast(1)
        val builder = StringBuilder()
        var bytes = 0
        for (ch in stem) {
            val chBytes = ch.toString().toByteArray(Charsets.UTF_8).size
            if (bytes + chBytes > budget) break
            builder.append(ch)
            bytes += chBytes
        }
        return builder.toString().ifBlank { "media" } + extension
    }

    private fun parseVaultFileId(fileName: String): Long? {
        val match = Regex("^(\\d{5,})_").find(fileName) ?: return null
        return match.groupValues[1].toLongOrNull()?.takeIf { it > 0 }
    }

    private fun stripVaultPrefix(fileName: String): String {
        val stripped = fileName.replace(Regex("^\\d{5,}_"), "")
        return stripped.ifBlank { fileName }
    }

    private fun isVideoFileName(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in VAULT_VIDEO_EXTENSIONS

    private fun mimeTypeForVaultFile(fileName: String, isVideo: Boolean): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (isVideo) "video/mp4" else "image/jpeg"
    }

    private suspend fun <T> indexLocked(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            synchronized(indexLock) {
                kotlinx.coroutines.runBlocking { block() }
            }
        }

    private fun parseVaultItem(obj: JSONObject): VaultItem? {
        val vaultFileName = obj.optString("vaultFileName")
        if (vaultFileName.isBlank()) return null
        val id = obj.optLong("id")
        if (id <= 0L) return null
        return VaultItem(
            id = id,
            vaultFileName = vaultFileName,
            originalName = obj.optString("originalName").ifBlank { stripVaultPrefix(vaultFileName) },
            originalPath = obj.optString("originalPath"),
            originalBucketName = obj.optString("originalBucketName"),
            originalBucketId = obj.optLong("originalBucketId"),
            dateTaken = obj.optLong("dateTaken"),
            dateLocked = obj.optLong("dateLocked"),
            width = obj.optInt("width"),
            height = obj.optInt("height"),
            isVideo = obj.optBoolean("isVideo"),
            durationMs = obj.optLong("durationMs"),
            mimeType = obj.optString("mimeType"),
            sizeBytes = obj.optLong("sizeBytes"),
            orientation = obj.optInt("orientation"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            dateModified = obj.optLong("dateModified"),
            dateAdded = obj.optLong("dateAdded"),
        )
    }

    /** Returns null when the index exists but cannot be parsed. */
    private fun readIndexStrict(): List<VaultItem>? {
        val file = indexFile.baseFile
        if (!file.exists()) return emptyList()
        return try {
            val json = indexFile.openRead().bufferedReader().use { it.readText() }
            if (json.isBlank()) return emptyList()
            val array = JSONArray(json)
            val list = mutableListOf<VaultItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseVaultItem(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    /**
     * When `vault_index.json` is truncated or corrupted, salvages any intact JSON objects
     * and adopts any remaining files in `vaultDir` so the vault never becomes permanently
     * empty or deadlocked.
     */
    private fun salvageCorruptIndex(): List<VaultItem> {
        val file = indexFile.baseFile
        val salvaged = mutableListOf<VaultItem>()
        val knownNames = mutableSetOf<String>()
        val usedIds = mutableSetOf<Long>()

        if (file.exists()) {
            val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
            if (raw.isNotBlank()) {
                val backupFile = File(context.filesDir, "vault_index.corrupt.bak")
                runCatching { file.copyTo(backupFile, overwrite = true) }
                val objectPattern = Regex("\\{[^{}]*\"vaultFileName\"[^{}]*\\}")
                for (match in objectPattern.findAll(raw)) {
                    val item = runCatching { parseVaultItem(JSONObject(match.value)) }.getOrNull() ?: continue
                    val diskFile = File(vaultDir, item.vaultFileName)
                    if (diskFile.exists() && diskFile.length() > 0L && knownNames.add(item.vaultFileName) && usedIds.add(item.id)) {
                        salvaged.add(item)
                    }
                }
            }
        }

        val diskFiles = (vaultDir.listFiles() ?: emptyArray())
            .filter { f ->
                f.isFile &&
                    !f.name.startsWith(".") &&
                    !f.name.endsWith(".iris_tmp") &&
                    !f.name.endsWith(".tmp") &&
                    f.name !in knownNames &&
                    f.length() > 0L
            }
            .sortedBy { it.name }

        for (diskFile in diskFiles) {
            val parsedId = parseVaultFileId(diskFile.name)
            val uniqueId = if (parsedId != null && parsedId !in usedIds) parsedId else generateUniqueId(salvaged)
            usedIds.add(uniqueId)
            knownNames.add(diskFile.name)
            val isVideo = isVideoFileName(diskFile.name)
            val lastMod = diskFile.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
            salvaged.add(
                VaultItem(
                    id = uniqueId,
                    vaultFileName = diskFile.name,
                    originalName = stripVaultPrefix(diskFile.name),
                    originalPath = "",
                    originalBucketName = VAULT_RECOVERED_BUCKET,
                    originalBucketId = 0L,
                    dateTaken = lastMod,
                    dateLocked = lastMod,
                    width = 0,
                    height = 0,
                    isVideo = isVideo,
                    durationMs = 0L,
                    mimeType = mimeTypeForVaultFile(diskFile.name, isVideo),
                    sizeBytes = diskFile.length(),
                    orientation = 0,
                    title = "",
                    description = "",
                    dateModified = lastMod,
                    dateAdded = lastMod,
                )
            )
        }

        writeIndex(salvaged)
        return salvaged
    }

    private fun writeIndex(items: List<VaultItem>): Boolean {
        var stream: FileOutputStream? = null
        return try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("vaultFileName", item.vaultFileName)
                    put("originalName", item.originalName)
                    put("originalPath", item.originalPath)
                    put("originalBucketName", item.originalBucketName)
                    put("originalBucketId", item.originalBucketId)
                    put("dateTaken", item.dateTaken)
                    put("dateLocked", item.dateLocked)
                    put("width", item.width)
                    put("height", item.height)
                    put("isVideo", item.isVideo)
                    put("durationMs", item.durationMs)
                    put("mimeType", item.mimeType)
                    put("sizeBytes", item.sizeBytes)
                    put("orientation", item.orientation)
                    put("title", item.title)
                    put("description", item.description)
                    put("dateModified", item.dateModified)
                    put("dateAdded", item.dateAdded)
                }
                array.put(obj)
            }
            stream = indexFile.startWrite()
            stream.write(array.toString(2).toByteArray(Charsets.UTF_8))
            stream.fd.sync()
            indexFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            runCatching { stream?.let { indexFile.failWrite(it) } }
            false
        }
    }

    /**
     * Writes the index and reads it back before callers are allowed to run destructive steps
     * (deleting originals). If this returns false, nothing that was copied may be moved again.
     */
    private fun persistIndexVerified(items: List<VaultItem>): Boolean {
        if (!writeIndex(items)) return false
        val readBack = readIndexStrict() ?: return false
        return readBack.size == items.size
    }
}
