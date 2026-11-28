package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.os.Bundle
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

sealed interface UriLoadResult {
    data class Ready(val item: MediaImage) : UriLoadResult
    data object Pending : UriLoadResult
    data object MissingOrDeleted : UriLoadResult
}

data class RescanPhaseResult(
    val scannedCount: Int,
    val purgedGhostCount: Int,
)

class MediaRepository(private val context: Context) {
    private val snapshot = AtomicFile(File(context.filesDir, "media_snapshot.bin"))
    private val libraryPreferences = LibraryPreferences(context)
    private val generationPrefs = context.getSharedPreferences("media_generation", Context.MODE_PRIVATE)

    companion object {
        private const val VERIFIED_PATH_TTL_MS = 60 * 1_000L // 1 minute
        private const val TOMBSTONE_TTL_MS = 24 * 60 * 60 * 1_000L // 24 hours
    }

    private val inMemoryCache = java.util.concurrent.ConcurrentHashMap<Long, MediaImage>()
    private val exifDateVerifiedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
    private val manuallyEditedDateIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
    private val recentMovedOrDeletedIds = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    private val recentMovedOrDeletedPaths = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val verifiedPathsCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun resolveBucketId(rawBucketId: Long, filePath: String): Long {
        if (rawBucketId != 0L) return rawBucketId
        val parentPath = filePath.substringBeforeLast('/', "")
        if (parentPath.isNotBlank()) return parentPath.lowercase(java.util.Locale.ROOT).hashCode().toLong()
        return 0L
    }

    private fun probeImageBounds(uri: Uri, filePath: String): Pair<Int, Int>? = runCatching {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val f = filePath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.canRead() }
        if (f != null) {
            android.graphics.BitmapFactory.decodeFile(f.absolutePath, opts)
        } else {
            context.contentResolver.openInputStream(uri)?.use { s ->
                android.graphics.BitmapFactory.decodeStream(s, null, opts)
            }
        }
        if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth to opts.outHeight else null
    }.getOrNull()

    fun loadSnapshot(): List<MediaImage> = runCatching {
        DataInputStream(snapshot.openRead().buffered()).use { input ->
            val version = input.readInt()
            if (version !in 3..6) return@use emptyList()
            val list = List(input.readInt().coerceIn(0, 100_000)) {
                val id = input.readLong(); val isVideo = input.readBoolean()
                val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val name = input.readUTF()
                val snapshotUri = if (version >= 4) {
                    input.readUTF().takeIf { it.isNotBlank() }?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
                } else null
                val dateTaken = input.readLong()
                val width = input.readInt(); val height = input.readInt()
                val path = input.readUTF(); val bucketId = input.readLong(); val bucketName = input.readUTF()
                val durationMs = input.readLong(); val mimeType = input.readUTF(); val sizeBytes = input.readLong()
                val orientation = input.readInt(); val savedTitle = input.readUTF()
                val dateModified = input.readLong(); val dateAdded = input.readLong()
                val exifDateVerified = if (version >= 6) input.readBoolean() else false
                if (exifDateVerified) {
                    exifDateVerifiedIds.add(id)
                }
                val title = libraryPreferences.getCustomTitle(id) ?: savedTitle
                MediaImage(id, snapshotUri ?: ContentUris.withAppendedId(collection, id), name, dateTaken,
                    width, height, path, bucketId, bucketName,
                    isVideo, durationMs, mimeType, sizeBytes, orientation, title,
                    dateModified = dateModified, dateAdded = dateAdded)
            }
            list.forEach { item ->
                // Older snapshots (v3/v4) may have orientation=0 for portrait RAW/DNG files;
                // leave them out of inMemoryCache so the background load refreshes their EXIF orientation once.
                if (version >= 5 || !item.isRaw) {
                    inMemoryCache[item.id] = item
                }
            }
            list
        }
    }.getOrDefault(emptyList())

    fun updateCachedMetadata(updated: MediaImage) {
        inMemoryCache[updated.id] = updated
        exifDateVerifiedIds.add(updated.id)
        manuallyEditedDateIds.add(updated.id)
    }

    fun hasUnverifiedExifDates(items: List<MediaImage>): Boolean {
        return items.any { !it.isVideo && it.id > 0L && it.id !in exifDateVerifiedIds }
    }

    /**
     * Reads EXIF capture timestamps in parallel on [Dispatchers.IO] for non-video photos that were
     * loaded via the fast MediaStore cursor path without an EXIF check. Results are persisted in
     * [exifDateVerifiedIds] and `media_snapshot.bin` so each unchanged file is checked only once.
     */
    suspend fun enrichExifDatesInBackground(
        items: List<MediaImage>,
        onBatchUpdated: (Map<Long, Long>) -> Unit,
        onCheckpoint: (suspend () -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        val pending = items.filter { !it.isVideo && it.id > 0L && it.id !in exifDateVerifiedIds }
        if (pending.isEmpty()) return@withContext false

        // Prioritize photos whose MediaStore DATE_TAKEN looks missing or equal to file copy/mtime
        // so migrated and copied photos snap into chronological order first.
        val prioritized = pending.sortedBy { item ->
            val missingTaken = item.dateTaken <= 0L
            val matchesFileDate = (item.dateModified > 0L && kotlin.math.abs(item.dateTaken - item.dateModified) <= 60_000L) ||
                (item.dateAdded > 0L && kotlin.math.abs(item.dateTaken - item.dateAdded) <= 60_000L)
            when {
                missingTaken -> 0
                matchesFileDate -> 1
                else -> 2
            }
        }

        val chunkSize = 200
        val workerCount = 4
        var checkedAny = false
        var chunksSinceCheckpoint = 0

        for (chunk in prioritized.chunked(chunkSize)) {
            coroutineContext.ensureActive()
            val subSize = ((chunk.size + workerCount - 1) / workerCount).coerceAtLeast(1)
            val batchUpdates = coroutineScope {
                chunk.chunked(subSize).map { slice ->
                    async(Dispatchers.IO) {
                        val localUpdates = HashMap<Long, Long>()
                        for (item in slice) {
                            coroutineContext.ensureActive()
                            if (item.id in manuallyEditedDateIds) {
                                exifDateVerifiedIds.add(item.id)
                                continue
                            }
                            val exifTaken = extractExifDateTaken(context, item.uri, item.path)
                            exifDateVerifiedIds.add(item.id)
                            if (exifTaken != null && exifTaken > 0L && kotlin.math.abs(exifTaken - item.dateTaken) >= 1_000L) {
                                localUpdates[item.id] = exifTaken
                                inMemoryCache[item.id]?.let { cached ->
                                    if (cached.id !in manuallyEditedDateIds) {
                                        inMemoryCache[item.id] = cached.copy(dateTaken = exifTaken)
                                    }
                                }
                            }
                        }
                        localUpdates
                    }
                }.awaitAll()
            }
            checkedAny = true
            chunksSinceCheckpoint++
            val mergedBatch = HashMap<Long, Long>()
            for (part in batchUpdates) {
                mergedBatch.putAll(part)
            }
            if (mergedBatch.isNotEmpty()) {
                onBatchUpdated(mergedBatch)
            }
            if (onCheckpoint != null && chunksSinceCheckpoint >= 5) {
                chunksSinceCheckpoint = 0
                onCheckpoint()
            }
        }
        checkedAny
    }

    fun markMovedOrDeleted(ids: Set<Long>, paths: Set<String>) {
        val now = System.currentTimeMillis()
        ids.forEach {
            if (it > 0) {
                recentMovedOrDeletedIds[it] = now
                inMemoryCache.remove(it)
                exifDateVerifiedIds.remove(it)
                manuallyEditedDateIds.remove(it)
            }
        }
        paths.forEach {
            if (it.isNotBlank()) {
                recentMovedOrDeletedPaths[it] = now
                verifiedPathsCache.remove(it)
            }
        }
    }

    fun isMarkedMovedOrDeleted(id: Long, path: String): Boolean {
        val now = System.currentTimeMillis()
        if (id > 0) {
            val timestamp = recentMovedOrDeletedIds[id]
            if (timestamp != null && now - timestamp < TOMBSTONE_TTL_MS) return true
        }
        if (path.isNotBlank()) {
            val timestamp = recentMovedOrDeletedPaths[path]
            if (timestamp != null && now - timestamp < TOMBSTONE_TTL_MS) return true
        }
        return false
    }

    fun clearRecentMovedOrDeleted(ids: Set<Long>, paths: Set<String> = emptySet()) {
        ids.forEach { recentMovedOrDeletedIds.remove(it) }
        paths.forEach { recentMovedOrDeletedPaths.remove(it) }
    }

    fun clearVerifiedPathsCache() {
        verifiedPathsCache.clear()
    }

    fun removeCachedPaths(paths: Set<String>) {
        if (paths.isEmpty()) return
        paths.forEach { verifiedPathsCache.remove(it) }
        val iterator = inMemoryCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.path in paths) {
                exifDateVerifiedIds.remove(entry.key)
                manuallyEditedDateIds.remove(entry.key)
                iterator.remove()
            }
        }
    }

    fun purgeGhostMedia(id: Long, path: String, uri: Uri) {
        val now = System.currentTimeMillis()
        if (id > 0) {
            recentMovedOrDeletedIds[id] = now
            inMemoryCache.remove(id)
        }
        if (path.isNotBlank()) {
            recentMovedOrDeletedPaths[path] = now
            verifiedPathsCache.remove(path)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        }
        runCatching { context.contentResolver.delete(uri, null, null) }
        if (id > 0 && uri.authority == "media") {
            val ghostVolume = uri.pathSegments.firstOrNull().orEmpty()
            val isPrimary = ghostVolume.isBlank() || ghostVolume == "external" || ghostVolume == "external_primary"
            val filesVolume = if (isPrimary) "external" else ghostVolume
            runCatching {
                context.contentResolver.delete(
                    MediaStore.Files.getContentUri(filesVolume),
                    "${MediaStore.MediaColumns._ID}=?",
                    arrayOf(id.toString())
                )
            }
            if (isPrimary && android.os.Build.VERSION.SDK_INT >= 29) {
                runCatching {
                    context.contentResolver.delete(
                        MediaStore.Files.getContentUri("external_primary"),
                        "${MediaStore.MediaColumns._ID}=?",
                        arrayOf(id.toString())
                    )
                }
            }
        }
    }

    suspend fun rescanStorage(): Int =
        rescanStoragePrioritized(forceReverifyExif = true).scannedCount

    /**
     * Scans storage directories ordered by most recently modified first and streams newly indexed
     * URIs and missing ghost paths back live as small batches complete, so the gallery updates
     * online without waiting for the entire filesystem walk to finish.
     */
    suspend fun rescanStoragePrioritized(
        recentOnly: Boolean = false,
        skipRecentPhase: Boolean = false,
        recentWindowMs: Long = 7L * 24L * 60L * 60L * 1000L,
        minRecentDirs: Int = 15,
        forceReverifyExif: Boolean = false,
        onBatchScanned: suspend (List<Uri>) -> Unit = {},
        onGhostsFound: suspend (Set<String>) -> Unit = {},
        onRecentPhaseComplete: (suspend () -> Unit)? = null,
    ): RescanPhaseResult = withContext(Dispatchers.IO) {
        verifiedPathsCache.clear()
        if (forceReverifyExif) {
            exifDateVerifiedIds.clear()
        }

        val storageRoots = mutableListOf<File>()
        runCatching {
            Environment.getExternalStorageDirectory()?.takeIf { it.exists() }?.let { storageRoots.add(it) }
        }
        runCatching {
            androidx.core.content.ContextCompat.getExternalFilesDirs(context, null).forEach { dir ->
                if (dir != null) {
                    val path = dir.absolutePath
                    val rootPath = path.substringBefore("/Android/data")
                    if (rootPath.isNotBlank() && rootPath != path) {
                        val rootFile = File(rootPath)
                        if (rootFile.exists() && rootFile !in storageRoots) {
                            storageRoots.add(rootFile)
                        }
                    }
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
                sm?.storageVolumes?.forEach { vol ->
                    val dir = vol.directory
                    if (dir != null && dir.exists() && dir !in storageRoots) {
                        storageRoots.add(dir)
                    }
                }
            }
        }

        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "dng", "raw", "svg",
            "cr2", "cr3", "nef", "arw", "rw2", "orf", "pef", "raf", "srw",
            "mp4", "mkv", "mov", "webm", "3gp", "avi", "flv", "ts", "m4v", "wmv"
        )

        class KnownMediaEntry(
            val modSec: Long,
            val size: Long,
            val id: Long,
            val isVideo: Boolean,
            val volumeName: String,
        )

        val knownMap = HashMap<String, KnownMediaEntry>()
        val knownPathsByDir = HashMap<String, MutableList<String>>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.VOLUME_NAME else MediaStore.MediaColumns.DATA,
        )
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val collectionsToQuery = if (android.os.Build.VERSION.SDK_INT >= 29) {
            val names = runCatching { MediaStore.getExternalVolumeNames(context) }.getOrNull()?.filter { it.isNotBlank() }
            if (!names.isNullOrEmpty()) {
                names.map { MediaStore.Files.getContentUri(it) }
            } else {
                listOf(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
            }
        } else {
            listOf(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
        }

        for (collection in collectionsToQuery) {
            coroutineContext.ensureActive()
            runCatching {
                context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val modCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val typeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val volCol = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME) else -1
                    if (idCol >= 0 && dataCol >= 0 && modCol >= 0 && sizeCol >= 0) {
                        while (cursor.moveToNext()) {
                            val path = cursor.getString(dataCol)
                            if (!path.isNullOrBlank()) {
                                val id = cursor.getLong(idCol)
                                val mod = cursor.getLong(modCol)
                                val sz = cursor.getLong(sizeCol)
                                val isVid = typeCol >= 0 && cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                                val vol = if (volCol >= 0) cursor.getString(volCol).orEmpty() else ""
                                knownMap[path] = KnownMediaEntry(mod, sz, id, isVid, vol)
                                val parent = path.substringBeforeLast('/', "")
                                if (parent.isNotBlank()) {
                                    knownPathsByDir.getOrPut(parent) { mutableListOf() }.add(path)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun isKnownMediaReadable(knownPath: String): Boolean {
            if (File(knownPath).exists()) return true
            val entry = knownMap[knownPath] ?: return false
            if (entry.id <= 0L) return false
            val vol = if (entry.volumeName.isNotBlank() && entry.volumeName != "external_primary") entry.volumeName else "external"
            val uri = if (entry.isVideo) {
                ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(vol), entry.id)
            } else {
                ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(vol), entry.id)
            }
            return runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd -> afd.length != 0L } ?: false
            }.getOrDefault(false)
        }

        fun isExcludedDir(dir: File): Boolean {
            val name = dir.name
            if (name.startsWith(".") || name.equals("Android", ignoreCase = true) || name.equals("lost.dir", ignoreCase = true)) {
                return true
            }
            val abs = dir.absolutePath
            if (abs.contains("/Android/data", ignoreCase = true) || abs.contains("/Android/obb", ignoreCase = true)) {
                return true
            }
            return File(dir, ".nomedia").exists()
        }

        // Collect known album folders and shallow media directories so we can sort by dir.lastModified()
        // descending; on Linux/Android adding a file updates the immediate parent directory's mtime.
        val priorityDirMap = LinkedHashMap<String, File>()
        fun addCandidateDir(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            if (isExcludedDir(dir)) return
            priorityDirMap.putIfAbsent(dir.absolutePath, dir)
        }

        for (knownParent in knownPathsByDir.keys) {
            addCandidateDir(File(knownParent))
        }
        val standardFolders = listOf("DCIM", "Pictures", "Movies", "Download", "Documents")
        for (root in storageRoots) {
            for (folder in standardFolders) {
                val base = File(root, folder)
                if (base.exists() && base.isDirectory && !isExcludedDir(base)) {
                    addCandidateDir(base)
                    runCatching { base.listFiles() }.getOrNull()?.forEach { child ->
                        if (child.isDirectory && !isExcludedDir(child)) {
                            addCandidateDir(child)
                            runCatching { child.listFiles() }.getOrNull()?.forEach { grandChild ->
                                if (grandChild.isDirectory && !isExcludedDir(grandChild)) {
                                    addCandidateDir(grandChild)
                                }
                            }
                        }
                    }
                }
            }
            runCatching { root.listFiles() }.getOrNull()?.forEach { sub ->
                if (sub.isDirectory && !isExcludedDir(sub)) {
                    addCandidateDir(sub)
                }
            }
        }

        val sortedDirs = priorityDirMap.values.sortedByDescending { runCatching { it.lastModified() }.getOrDefault(0L) }
        val recentCutoffMs = System.currentTimeMillis() - recentWindowMs
        val recentDirs = ArrayList<File>()
        val olderDirs = ArrayList<File>()
        sortedDirs.forEachIndexed { index, dir ->
            val mtime = runCatching { dir.lastModified() }.getOrDefault(0L)
            if (index < minRecentDirs || mtime >= recentCutoffMs) {
                recentDirs.add(dir)
            } else {
                olderDirs.add(dir)
            }
        }

        val visitedDirs = HashSet<String>(sortedDirs.size * 2)
        val checkedKnownDirs = HashSet<String>(sortedDirs.size)
        val pendingScanPaths = ArrayList<String>(32)
        val pendingGhostPaths = LinkedHashSet<String>(32)
        var totalScanned = 0
        var totalPurgedGhosts = 0

        suspend fun flushScanPaths() {
            if (pendingScanPaths.isEmpty()) return
            val batch = pendingScanPaths.toTypedArray()
            pendingScanPaths.clear()
            val latch = kotlinx.coroutines.CompletableDeferred<Unit>()
            val counter = java.util.concurrent.atomic.AtomicInteger(0)
            val scannedUris = java.util.Collections.synchronizedList(ArrayList<Uri>(batch.size))
            android.media.MediaScannerConnection.scanFile(
                context,
                batch,
                null
            ) { _, uri ->
                if (uri != null) {
                    scannedUris.add(uri)
                }
                if (counter.incrementAndGet() >= batch.size) {
                    latch.complete(Unit)
                }
            }
            kotlinx.coroutines.withTimeoutOrNull(6_000) { latch.await() }
            totalScanned += counter.get()
            val readyUris = synchronized(scannedUris) { scannedUris.toList() }
            if (readyUris.isNotEmpty()) {
                onBatchScanned(readyUris)
            }
        }

        suspend fun flushGhostPaths() {
            if (pendingGhostPaths.isEmpty()) return
            val ghosts = pendingGhostPaths.toSet()
            pendingGhostPaths.clear()
            totalPurgedGhosts += ghosts.size
            removeCachedPaths(ghosts)
            onGhostsFound(ghosts)
            // Ask MediaStore asynchronously to prune the stale rows without blocking tile updates.
            android.media.MediaScannerConnection.scanFile(context, ghosts.toTypedArray(), null, null)
        }

        suspend fun scanDirectory(dir: File, depth: Int) {
            coroutineContext.ensureActive()
            if (depth > 12) return
            val dirPath = dir.absolutePath
            if (!visitedDirs.add(dirPath)) return
            if (isExcludedDir(dir)) return

            var foundNewOrModifiedInDir = false
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            val unvisitedSubdirs = ArrayList<File>()
            for (child in children) {
                if (child.isDirectory) {
                    if (child.absolutePath !in priorityDirMap && !isExcludedDir(child)) {
                        unvisitedSubdirs.add(child)
                    }
                    continue
                }
                if (!child.isFile || child.name.startsWith(".")) continue
                val ext = child.extension.lowercase(java.util.Locale.ROOT)
                if (ext !in mediaExtensions) continue
                val path = child.absolutePath
                val known = knownMap[path]
                val needsScan = if (known == null) {
                    true
                } else {
                    val diskModSec = child.lastModified() / 1000L
                    val diskSize = child.length()
                    Math.abs(diskModSec - known.modSec) > 1L || (diskSize > 0L && diskSize != known.size)
                }
                if (needsScan) {
                    pendingScanPaths.add(path)
                    foundNewOrModifiedInDir = true
                    if (pendingScanPaths.size >= 24) {
                        flushScanPaths()
                    }
                }
            }

            if (checkedKnownDirs.add(dirPath)) {
                knownPathsByDir[dirPath]?.forEach { knownPath ->
                    if (!isKnownMediaReadable(knownPath)) {
                        pendingGhostPaths.add(knownPath)
                    }
                }
            }

            if (foundNewOrModifiedInDir) {
                flushScanPaths()
            }
            if (pendingGhostPaths.isNotEmpty()) {
                flushGhostPaths()
            }

            if (unvisitedSubdirs.isNotEmpty()) {
                unvisitedSubdirs.sortByDescending { runCatching { it.lastModified() }.getOrDefault(0L) }
                for (sub in unvisitedSubdirs) {
                    scanDirectory(sub, depth + 1)
                }
            }
        }

        // Phase 1: recently modified directories first
        if (!skipRecentPhase) {
            for (dir in recentDirs) {
                scanDirectory(dir, depth = 0)
            }
            flushScanPaths()
            flushGhostPaths()
            onRecentPhaseComplete?.invoke()
        } else {
            for (dir in recentDirs) {
                val p = dir.absolutePath
                visitedDirs.add(p)
                checkedKnownDirs.add(p)
            }
        }

        if (recentOnly) {
            return@withContext RescanPhaseResult(totalScanned, totalPurgedGhosts)
        }

        // Phase 2: remaining older directories + any unchecked known folders
        for (dir in olderDirs) {
            scanDirectory(dir, depth = 0)
        }
        flushScanPaths()
        flushGhostPaths()

        // Verify any remaining known MediaStore paths whose parent folders were deleted or excluded.
        for ((dirPath, paths) in knownPathsByDir) {
            coroutineContext.ensureActive()
            if (dirPath in checkedKnownDirs) continue
            for (knownPath in paths) {
                if (!isKnownMediaReadable(knownPath)) {
                    pendingGhostPaths.add(knownPath)
                    if (pendingGhostPaths.size >= 50) {
                        flushGhostPaths()
                    }
                }
            }
        }
        flushGhostPaths()

        RescanPhaseResult(totalScanned, totalPurgedGhosts)
    }

    /**
     * Discovers dot-prefixed hidden media files (e.g. `.image.jpg`) and media inside hidden
     * directories or `.nomedia` folders, while strictly skipping system trash/pending/cache dirs.
     */
    suspend fun loadHiddenImages(): List<MediaImage> = withContext(Dispatchers.IO) {
        val imageExtensions = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "dng", "raw", "svg",
            "cr2", "cr3", "nef", "arw", "rw2", "orf", "pef", "raf", "srw"
        )
        val videoExtensions = setOf(
            "mp4", "mkv", "mov", "webm", "3gp", "avi", "flv", "ts", "m4v", "wmv"
        )
        val allExtensions = imageExtensions + videoExtensions

        val storageRoots = mutableListOf<File>()
        runCatching {
            val primary = android.os.Environment.getExternalStorageDirectory()
            if (primary != null && primary.exists()) storageRoots.add(primary)
        }
        runCatching {
            context.getExternalFilesDirs(null).forEach { dir ->
                if (dir != null) {
                    val path = dir.absolutePath
                    val rootPath = path.substringBefore("/Android/data")
                    if (rootPath.isNotBlank() && rootPath != path) {
                        val rootFile = File(rootPath)
                        if (rootFile.exists() && rootFile !in storageRoots) {
                            storageRoots.add(rootFile)
                        }
                    }
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
                sm?.storageVolumes?.forEach { vol ->
                    val dir = vol.directory
                    if (dir != null && dir.exists() && dir !in storageRoots) {
                        storageRoots.add(dir)
                    }
                }
            }
        }

        val appPrivatePrefix = context.filesDir.absolutePath
        val excludedDirNames = setOf(
            "android", "lost.dir", ".thumbnails", ".trash", ".trashes", ".recycle", ".recyclebin",
            ".cache", ".face", ".estrongs", ".database_uuid"
        )
        val results = mutableListOf<MediaImage>()
        val seenPaths = HashSet<String>()

        fun scanDir(dir: File, inHiddenSubtree: Boolean, depth: Int) {
            if (depth > 10) return
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            val hasNoMedia = children.any { it.isFile && it.name.equals(".nomedia", ignoreCase = true) }
            val effectiveHidden = inHiddenSubtree || hasNoMedia
            for (child in children) {
                val childName = child.name
                val childPath = child.absolutePath
                if (childPath.startsWith(appPrivatePrefix)) continue
                if (child.isDirectory) {
                    val lower = childName.lowercase(java.util.Locale.ROOT)
                    if (lower in excludedDirNames) continue
                    if (childPath.contains("/Android/data", ignoreCase = true) ||
                        childPath.contains("/Android/obb", ignoreCase = true)
                    ) continue
                    val dirIsHidden = effectiveHidden || childName.startsWith(".")
                    scanDir(child, dirIsHidden, depth + 1)
                } else if (child.isFile) {
                    val isDotFile = childName.startsWith(".")
                    if (!effectiveHidden && !isDotFile) continue
                    if (childName.equals(".nomedia", ignoreCase = true)) continue
                    if (childName.startsWith(".trashed-", ignoreCase = true) ||
                        childName.startsWith(".pending-", ignoreCase = true) ||
                        childName.endsWith(".iris_tmp", ignoreCase = true)
                    ) continue
                    val ext = child.extension.lowercase(java.util.Locale.ROOT)
                    if (ext !in allExtensions) continue
                    val sizeBytes = child.length()
                    if (sizeBytes <= 0L) continue
                    if (!seenPaths.add(childPath)) continue

                    val isVideo = ext in videoExtensions
                    val mimeType = android.webkit.MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext)
                        ?: when (ext) {
                            "dng" -> "image/x-adobe-dng"
                            "cr2" -> "image/x-canon-cr2"
                            "cr3" -> "image/x-canon-cr3"
                            "nef" -> "image/x-nikon-nef"
                            "arw" -> "image/x-sony-arw"
                            "raf" -> "image/x-fuji-raf"
                            "orf" -> "image/x-olympus-orf"
                            "rw2" -> "image/x-panasonic-rw2"
                            "pef" -> "image/x-pentax-pef"
                            "srw" -> "image/x-samsung-srw"
                            "raw" -> "image/x-dcraw"
                            else -> if (isVideo) "video/mp4" else "image/jpeg"
                        }
                    val lastMod = child.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                    val parentFile = child.parentFile
                    val parentPath = parentFile?.absolutePath.orEmpty()
                    val bucketName = parentFile?.name?.ifBlank { "Hidden" } ?: "Hidden"
                    val bucketId = resolveBucketId(0L, parentPath)

                    var width = 0
                    var height = 0
                    var itemOrientation = 0
                    if (!isVideo && ext != "svg") {
                        runCatching {
                            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeFile(childPath, opts)
                            if (opts.outWidth > 0 && opts.outHeight > 0) {
                                width = opts.outWidth
                                height = opts.outHeight
                            }
                        }
                        itemOrientation = com.iris.gallery.decoder.ExifOrientationHelper.readRotationDegrees(child)
                    }

                    val stableHash = childPath.hashCode().toLong() and 0x7FFFFFFFL
                    val syntheticId = -2_000_000_000_000L - stableHash
                    val hiddenUri = android.net.Uri.fromFile(child)
                    val exifTaken = if (!isVideo && ext != "svg") {
                        extractExifDateTaken(context, hiddenUri, childPath)
                    } else null
                    val effectiveDateTaken = if (exifTaken != null && exifTaken > 0L) exifTaken else lastMod

                    results.add(
                        MediaImage(
                            id = syntheticId,
                            uri = hiddenUri,
                            name = childName,
                            dateTaken = effectiveDateTaken,
                            width = width,
                            height = height,
                            path = childPath,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            isVideo = isVideo,
                            durationMs = 0L,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            orientation = itemOrientation,
                            title = "",
                            description = "",
                            dateModified = lastMod,
                            dateAdded = lastMod,
                        )
                    )
                }
            }
        }

        for (root in storageRoots) {
            scanDir(root, inHiddenSubtree = false, depth = 0)
        }

        results.sortedWith(
            compareByDescending<MediaImage> { it.dateTaken }
                .thenByDescending { it.dateModified }
                .thenByDescending { it.id }
        )
    }

    suspend fun loadImages(trashed: Boolean = false, ids: Collection<Long>? = null): List<MediaImage> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        recentMovedOrDeletedIds.entries.removeIf { now - it.value > TOMBSTONE_TTL_MS }
        recentMovedOrDeletedPaths.entries.removeIf { now - it.value > TOMBSTONE_TTL_MS }

        val collectionsToQuery = if (android.os.Build.VERSION.SDK_INT >= 29) {
            val names = runCatching { MediaStore.getExternalVolumeNames(context) }.getOrNull()?.filter { it.isNotBlank() }
            if (!names.isNullOrEmpty()) {
                names.map { MediaStore.Files.getContentUri(it) }
            } else {
                listOf(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
            }
        } else {
            listOf(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA,
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                MediaStore.Images.Media.RELATIVE_PATH
            } else {
                MediaStore.Images.Media.DATA
            },
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                MediaStore.MediaColumns.VOLUME_NAME
            } else {
                MediaStore.Images.Media.DATA
            },
        )

        val result = buildList {
            val mediaSelection = buildString {
                append("(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)")
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    if (!trashed) {
                        append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 0 AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                    append(" AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
                }
                if (!ids.isNullOrEmpty()) {
                    append(" AND ${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})")
                }
            }
            val selectionArgs = buildList {
                add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                if (!ids.isNullOrEmpty()) {
                    ids.forEach { add(it.toString()) }
                }
            }.toTypedArray()
            val order = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

            for (collection in collectionsToQuery) {
                val cursorResult = if (android.os.Build.VERSION.SDK_INT >= 30) {
                    context.contentResolver.query(collection, projection, Bundle().apply {
                        putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, mediaSelection)
                        putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                        putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,
                            if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
                    }, null)
                } else {
                    context.contentResolver.query(collection, projection, mediaSelection, selectionArgs, order)
                }
                cursorResult?.use { cursor ->
                    val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val taken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val width = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                    val height = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                    val bucketId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                    val bucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val mediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
                    val mimeType = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val orientation = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                    val title = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val relPathCol = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) else -1
                    val volCol = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME) else -1

                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(id)
                        if (!trashed && recentMovedOrDeletedIds.containsKey(mediaId)) {
                            continue
                        }
                        val isVid = cursor.getInt(mediaType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        val displayName = cursor.getString(name).orEmpty()
                        val rawData = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                        val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
                        val volName = if (volCol >= 0) cursor.getString(volCol).orEmpty() else ""

                        val filePath = when {
                            rawData.isNotBlank() -> rawData
                            relPath.isNotBlank() && volName.isNotBlank() && volName != "external_primary" -> "/storage/$volName/$relPath$displayName"
                            relPath.isNotBlank() -> "/storage/emulated/0/$relPath$displayName"
                            else -> ""
                        }

                        val volumeForUri = if (volName.isNotBlank() && volName != "external_primary") volName else "external"
                        val baseMediaUri = if (isVid) {
                            ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(volumeForUri), mediaId)
                        } else {
                            ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volumeForUri), mediaId)
                        }
                        val mediaUri = if (trashed && android.os.Build.VERSION.SDK_INT >= 30) {
                            baseMediaUri.buildUpon().appendQueryParameter("include_trashed", "1").build()
                        } else {
                            baseMediaUri
                        }

                        val cursorDateTaken = cursor.getLong(taken)
                        val cursorDateModified = cursor.getLong(modified) * 1_000L
                        val cursorDateAdded = cursor.getLong(added) * 1_000L
                        val effectiveDateAdded = RestoredTimestamps.getDateAdded(context, filePath) ?: cursorDateAdded
                        val newestRowMs = maxOf(cursorDateModified, cursorDateAdded)
                        val isFreshRow = newestRowMs > 0L && now - newestRowMs < 2 * 60 * 1000L
                        val itemWidth = cursor.getInt(width)
                        val itemHeight = cursor.getInt(height)
                        val itemDuration = cursor.getLong(duration)
                        val itemSize = cursor.getLong(size)
                        val itemOrientation = cursor.getInt(orientation)
                        val itemTitle = libraryPreferences.getCustomTitle(mediaId) ?: cursor.getString(title).orEmpty()
                        val itemBucketId = if (trashed) -2L else resolveBucketId(cursor.getLong(bucketId), filePath)

                        // Snapshot entries land in the cache, so an unchanged row can
                        // skip the disk stat and the EXIF read entirely. This is what
                        // keeps a 13k-item refresh from turning into a multi-minute scan.
                        val itemMimeType = cursor.getString(mimeType).orEmpty()
                        val existing = if (!trashed) inMemoryCache[mediaId] else null
                        val orientationMatches = existing != null && (
                            existing.orientation == itemOrientation ||
                                (existing.isRaw && itemOrientation == 0)
                            )
                        val dimensionsMatch = existing != null && (
                            (existing.width == itemWidth && existing.height == itemHeight) ||
                                (
                                    existing.isRaw &&
                                        existing.width > 0 &&
                                        existing.height > 0 &&
                                        (
                                            itemWidth <= 0 ||
                                                itemHeight <= 0 ||
                                                (existing.orientation in setOf(90, 270) && itemWidth < itemHeight)
                                            )
                                    )
                            )
                        if (existing != null &&
                            existing.name == displayName &&
                            existing.path == filePath &&
                            existing.bucketId == itemBucketId &&
                            existing.dateModified == cursorDateModified &&
                            existing.dateAdded == effectiveDateAdded &&
                            existing.sizeBytes == itemSize &&
                            orientationMatches &&
                            dimensionsMatch &&
                            existing.title == itemTitle &&
                            existing.isVideo == isVid
                        ) {
                            add(existing)
                            continue
                        }

                        // Old rows with a real size can skip the per-file stat and the
                        // EXIF read. MediaStore already has the values, and this keeps
                        // a cold 13k-item refresh from taking minutes.
                        val trustCursor = !trashed &&
                            !isFreshRow &&
                            filePath.isNotBlank() &&
                            itemSize > 0L &&
                            cursorDateModified > 0L &&
                            now - cursorDateModified > 2L * 24L * 60L * 60L * 1000L

                        var diskModifiedMs = 0L
                        var diskSizeBytes = -1L
                        if (!trashed && filePath.isNotBlank() && !trustCursor) {
                            val file = File(filePath)
                            var exists = file.exists()
                            if (exists) {
                                diskModifiedMs = file.lastModified()
                                diskSizeBytes = file.length()
                            } else {
                                // Under Android 11+ Scoped Storage without MANAGE_EXTERNAL_STORAGE,
                                // File.exists() can return false for media owned by other apps even
                                // though ContentResolver can read the URI directly.
                                exists = runCatching {
                                    context.contentResolver.openAssetFileDescriptor(mediaUri, "r")?.use { afd ->
                                        if (afd.length > 0L) diskSizeBytes = afd.length
                                        afd.length != 0L
                                    } ?: false
                                }.getOrDefault(false)
                            }
                            if (!exists) {
                                if (isFreshRow) {
                                    // MediaStore already has the row but the file is still
                                    // being written. Keep the row, the next observer tick
                                    // will pick it up once the camera/download finishes.
                                    continue
                                }
                                inMemoryCache.remove(mediaId)
                                // Let the platform scanner do the pruning. Deleting the
                                // row from a read path is how freshly captured photos
                                // were disappearing before the file was written.
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
                                continue
                            }
                        }

                        var effectiveModified = cursorDateModified
                        var effectiveSize = itemSize
                        if (diskModifiedMs > 0L) {
                            val diskModSec = diskModifiedMs / 1000L
                            val cursorModSec = cursor.getLong(modified)
                            if (Math.abs(diskModSec - cursorModSec) > 1L || (diskSizeBytes >= 0L && diskSizeBytes != itemSize)) {
                                effectiveModified = diskModifiedMs
                                if (diskSizeBytes >= 0L) effectiveSize = diskSizeBytes
                            }
                        }

                        if (!trashed && existing != null &&
                            existing.name == displayName &&
                            existing.path == filePath &&
                            existing.bucketId == itemBucketId &&
                            existing.dateModified == effectiveModified &&
                            existing.dateAdded == effectiveDateAdded &&
                            existing.sizeBytes == itemSize &&
                            orientationMatches &&
                            dimensionsMatch &&
                            existing.title == itemTitle &&
                            existing.isVideo == isVid
                        ) {
                            add(existing)
                            continue
                        }

                        exifDateVerifiedIds.remove(mediaId)
                        var takenTime = cursorDateTaken
                        if (!isVid && !trustCursor) {
                            val exifTaken = extractExifDateTaken(context, mediaUri, filePath)
                            if (exifTaken != null && exifTaken > 0L) takenTime = exifTaken
                            exifDateVerifiedIds.add(mediaId)
                        }

                        if (takenTime <= 0L && cursorDateTaken > 0L) takenTime = cursorDateTaken
                        if (takenTime <= 0L && effectiveModified > 0L) takenTime = effectiveModified
                        if (takenTime <= 0L && effectiveDateAdded > 0L) takenTime = effectiveDateAdded

                        val isRawItem = !isVid && isRawMedia(displayName, itemMimeType)
                        val effectiveOrientation = if (isRawItem && itemOrientation == 0) {
                            com.iris.gallery.decoder.ExifOrientationHelper.readRotationDegrees(context, mediaUri, filePath)
                        } else {
                            itemOrientation
                        }
                        var effectiveWidth = itemWidth
                        var effectiveHeight = itemHeight
                        if (isRawItem && (effectiveWidth <= 0 || effectiveHeight <= 0 || (effectiveOrientation in setOf(90, 270) && effectiveWidth < effectiveHeight))) {
                            probeImageBounds(mediaUri, filePath)?.let { (w, h) ->
                                effectiveWidth = w
                                effectiveHeight = h
                            }
                        }

                        val itemBucketName = if (trashed) "Trash" else cursor.getString(bucketName).orEmpty().ifBlank {
                            filePath.substringBeforeLast('/', "").substringAfterLast('/', "").ifBlank { "Other" }
                        }

                        val newImage = MediaImage(
                            id = mediaId,
                            uri = mediaUri,
                            name = displayName,
                            dateTaken = takenTime,
                            width = effectiveWidth,
                            height = effectiveHeight,
                            path = filePath,
                            bucketId = itemBucketId,
                            bucketName = itemBucketName,
                            isVideo = isVid,
                            durationMs = itemDuration,
                            mimeType = itemMimeType,
                            sizeBytes = effectiveSize,
                            orientation = effectiveOrientation,
                            title = itemTitle,
                            dateModified = effectiveModified,
                            dateAdded = effectiveDateAdded,
                        )
                        if (!trashed) {
                            inMemoryCache[mediaId] = newImage
                        }
                        add(newImage)
                    }
                }
            }
        }
        val sorted = result
            .distinctBy { it.id }
            .distinctBy { it.path.ifBlank { it.id.toString() } }
            .sortedWith(
            compareByDescending<MediaImage> { it.dateTaken }
                .thenByDescending { it.dateModified }
                .thenByDescending { it.dateAdded }
                .thenByDescending { it.id }
        )
        if (!trashed) {
            val validIds = sorted.mapTo(HashSet(sorted.size)) { it.id }
            inMemoryCache.keys.retainAll(validIds)
            exifDateVerifiedIds.retainAll(validIds)
            saveSnapshot(sorted)
        }
        sorted
    }

    suspend fun loadImageByUri(uri: Uri): MediaImage? =
        (loadImageByUriResult(uri) as? UriLoadResult.Ready)?.item

    suspend fun loadImageByUriResult(uri: Uri): UriLoadResult = withContext(Dispatchers.IO) {
        val segments = uri.pathSegments
        val isFilesUri = "file" in segments
        val isVideoUri = "video" in segments
        val isImageUri = "images" in segments
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.MIME_TYPE)
            // media_type only exists on the Files view; requesting it on Images/Video throws IllegalArgumentException.
            if (isFilesUri) {
                add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            }
            add(MediaStore.MediaColumns.TITLE)
            add(MediaStore.MediaColumns.DATA)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.Images.Media.BUCKET_ID)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (!isImageUri) {
                add(MediaStore.Video.VideoColumns.DURATION)
            }
            add(MediaStore.Images.Media.ORIENTATION)
            if (Build.VERSION.SDK_INT >= 29) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
                add(MediaStore.MediaColumns.VOLUME_NAME)
                add(MediaStore.MediaColumns.IS_PENDING)
            }
            if (Build.VERSION.SDK_INT >= 30) {
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()

        runCatching {
            val cursor = if (Build.VERSION.SDK_INT >= 30) {
                context.contentResolver.query(uri, projection, Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                }, null)
            } else {
                context.contentResolver.query(uri, projection, null, null, null)
            }
            cursor?.use { cursor ->
                if (!cursor.moveToFirst()) return@use UriLoadResult.MissingOrDeleted

                fun index(column: String) = cursor.getColumnIndex(column)
                fun text(column: String): String = index(column).takeIf { it >= 0 }?.let { cursor.getString(it) }.orEmpty()
                fun number(column: String): Long = index(column).takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L
                fun integer(column: String): Int = index(column).takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0

                val id = number(MediaStore.MediaColumns._ID)
                if (id <= 0L) return@use UriLoadResult.MissingOrDeleted
                if (Build.VERSION.SDK_INT >= 29 && integer(MediaStore.MediaColumns.IS_PENDING) != 0) {
                    return@use UriLoadResult.Pending
                }
                if (Build.VERSION.SDK_INT >= 30 && integer(MediaStore.MediaColumns.IS_TRASHED) != 0) {
                    return@use UriLoadResult.MissingOrDeleted
                }
                val displayName = text(MediaStore.MediaColumns.DISPLAY_NAME)
                    .ifBlank { text(MediaStore.MediaColumns.TITLE) }
                val rawData = text(MediaStore.MediaColumns.DATA)
                val relativePath = text(MediaStore.MediaColumns.RELATIVE_PATH)
                val volumeName = text(MediaStore.MediaColumns.VOLUME_NAME)
                val filePath = when {
                    rawData.isNotBlank() -> rawData
                    relativePath.isNotBlank() && volumeName.isNotBlank() && volumeName != "external_primary" ->
                        "/storage/$volumeName/$relativePath$displayName"
                    relativePath.isNotBlank() -> "/storage/emulated/0/$relativePath$displayName"
                    else -> ""
                }
                if (isMarkedMovedOrDeleted(id, filePath)) return@use UriLoadResult.MissingOrDeleted

                val mimeType = text(MediaStore.MediaColumns.MIME_TYPE)
                val mediaType = if (isFilesUri) {
                    integer(MediaStore.Files.FileColumns.MEDIA_TYPE)
                } else {
                    0
                }
                val isVideo = isVideoUri ||
                    mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ||
                    mimeType.startsWith("video/", ignoreCase = true)
                val isImage = isImageUri ||
                    mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ||
                    mimeType.startsWith("image/", ignoreCase = true)
                if (!isImage && !isVideo) return@use UriLoadResult.MissingOrDeleted

                val volumeForUri = if (volumeName.isNotBlank() && volumeName != "external_primary") volumeName else "external"
                val canonicalUri = if (isVideo) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(volumeForUri), id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volumeForUri), id)
                }

                val dateModified = number(MediaStore.MediaColumns.DATE_MODIFIED) * 1_000L
                val cursorDateAdded = number(MediaStore.MediaColumns.DATE_ADDED) * 1_000L
                val cursorSize = number(MediaStore.MediaColumns.SIZE)
                val now = System.currentTimeMillis()
                val newestRowMs = maxOf(dateModified, cursorDateAdded)
                val isFreshRow = newestRowMs > 0L && now - newestRowMs < 2 * 60 * 1000L

                var diskSize = -1L
                var diskMod = 0L
                var fileReady = false
                if (filePath.isNotBlank()) {
                    val file = File(filePath)
                    if (file.exists()) {
                        diskSize = file.length()
                        diskMod = file.lastModified()
                        fileReady = diskSize > 0L || cursorSize > 0L
                    }
                }
                if (!fileReady) {
                    // Fall back to ContentResolver for Scoped Storage (Android 11+) and secondary volumes
                    // where File.exists() can fail on videos downloaded by other apps.
                    fileReady = runCatching {
                        context.contentResolver.openAssetFileDescriptor(canonicalUri, "r")?.use { afd ->
                            if (afd.length > 0L) diskSize = afd.length
                            afd.length > 0L || (afd.length == android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH && cursorSize > 0L)
                        } ?: false
                    }.getOrDefault(false)
                }
                if (!fileReady) {
                    return@use if (isFreshRow || filePath.isBlank()) UriLoadResult.Pending else UriLoadResult.MissingOrDeleted
                }

                val effectiveModified = if (diskMod > 0L) diskMod else dateModified
                val effectiveSize = if (diskSize > 0L) diskSize else cursorSize
                val dateAdded = RestoredTimestamps.getDateAdded(context, filePath) ?: cursorDateAdded
                var dateTaken = number(MediaStore.Images.Media.DATE_TAKEN)
                if (!isVideo) {
                    extractExifDateTaken(context, canonicalUri, filePath)?.let { if (it > 0L) dateTaken = it }
                    exifDateVerifiedIds.add(id)
                }
                if (dateTaken <= 0L) dateTaken = effectiveModified
                if (dateTaken <= 0L) dateTaken = dateAdded

                val bucketName = text(MediaStore.Images.Media.BUCKET_DISPLAY_NAME).ifBlank {
                    filePath.substringBeforeLast('/', "").substringAfterLast('/', "").ifBlank { "Other" }
                }
                val bucketId = resolveBucketId(number(MediaStore.Images.Media.BUCKET_ID), filePath)
                val title = libraryPreferences.getCustomTitle(id) ?: text(MediaStore.MediaColumns.TITLE).ifBlank { displayName }

                val cursorOrientation = integer(MediaStore.Images.Media.ORIENTATION)
                val isRawItem = !isVideo && isRawMedia(displayName, mimeType)
                val effectiveOrientation = if (isRawItem && cursorOrientation == 0) {
                    com.iris.gallery.decoder.ExifOrientationHelper.readRotationDegrees(context, canonicalUri, filePath)
                } else {
                    cursorOrientation
                }
                var effectiveWidth = integer(MediaStore.Images.Media.WIDTH)
                var effectiveHeight = integer(MediaStore.Images.Media.HEIGHT)
                if (isRawItem && (effectiveWidth <= 0 || effectiveHeight <= 0 || (effectiveOrientation in setOf(90, 270) && effectiveWidth < effectiveHeight))) {
                    probeImageBounds(canonicalUri, filePath)?.let { (w, h) ->
                        effectiveWidth = w
                        effectiveHeight = h
                    }
                }

                val item = MediaImage(
                    id = id,
                    uri = canonicalUri,
                    name = displayName,
                    dateTaken = dateTaken,
                    width = effectiveWidth,
                    height = effectiveHeight,
                    path = filePath,
                    bucketId = bucketId,
                    bucketName = bucketName,
                    isVideo = isVideo,
                    durationMs = number(MediaStore.Video.VideoColumns.DURATION),
                    mimeType = mimeType,
                    sizeBytes = effectiveSize,
                    orientation = effectiveOrientation,
                    title = title,
                    dateModified = effectiveModified,
                    dateAdded = dateAdded,
                )
                inMemoryCache[item.id] = item
                UriLoadResult.Ready(item)
            } ?: UriLoadResult.MissingOrDeleted
        }.getOrElse { UriLoadResult.Pending }
    }

    suspend fun getGenerationByVolume(): Map<String, Long>? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 30) return@withContext null
        runCatching {
            MediaStore.getExternalVolumeNames(context).mapNotNull { volume ->
                runCatching { MediaStore.getGeneration(context, volume) }
                    .getOrNull()
                    ?.let { volume to it }
            }.toMap()
        }.getOrNull()
    }

    suspend fun getChangedUrisSince(since: Map<String, Long>): List<Uri> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 30) return@withContext emptyList()
        val changed = LinkedHashSet<Uri>()
        for ((volume, generation) in since) {
            for (base in listOf(
                MediaStore.Images.Media.getContentUri(volume),
                MediaStore.Video.Media.getContentUri(volume)
            )) {
                runCatching {
                    context.contentResolver.query(
                        base,
                        arrayOf(MediaStore.MediaColumns._ID),
                        Bundle().apply {
                            putString(
                                android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
                                "${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?"
                            )
                            putStringArray(
                                android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                                arrayOf(generation.toString())
                            )
                            // Include pending items so in-flight camera shots/downloads enter pending retry tracking instead of being skipped when generation advances.
                            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                        },
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            changed.add(ContentUris.withAppendedId(base, cursor.getLong(0)))
                        }
                    }
                }
            }
        }
        changed.toList()
    }

    suspend fun checkObsoleteContentIds(knownIds: Collection<Long>): Set<Long> = withContext(Dispatchers.IO) {
        if (knownIds.isEmpty()) return@withContext emptySet()
        val found = HashSet<Long>()
        val bases = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.getExternalVolumeNames(context).flatMap { volume ->
                listOf(
                    MediaStore.Images.Media.getContentUri(volume),
                    MediaStore.Video.Media.getContentUri(volume)
                )
            }
        } else {
            listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
        for (base in bases) {
            runCatching {
                context.contentResolver.query(base, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) found.add(cursor.getLong(0))
                }
            }
        }
        knownIds.toSet() - found
    }

    fun loadGenerationByVolume(): Map<String, Long>? {
        val raw = generationPrefs.getString("generations", null) ?: return null
        return runCatching {
            raw.split('|').mapNotNull { entry ->
                val parts = entry.split(':')
                val value = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                parts[0] to value
            }.toMap()
        }.getOrNull()
    }

    fun saveGenerationByVolume(generations: Map<String, Long>) {
        generationPrefs.edit()
            .putString("generations", generations.entries.joinToString("|") { "${it.key}:${it.value}" })
            .apply()
    }

    fun getCurrentMediaStoreVersion(): String? = runCatching {
        MediaStore.getVersion(context)
    }.getOrNull()

    fun loadMediaStoreVersion(): String? = generationPrefs.getString("mediastore_version", null)

    fun saveMediaStoreVersion(version: String) {
        generationPrefs.edit().putString("mediastore_version", version).apply()
    }

    fun isSnapshotComplete(): Boolean = generationPrefs.getBoolean("snapshot_complete", false)

    fun setSnapshotComplete(complete: Boolean) {
        generationPrefs.edit().putBoolean("snapshot_complete", complete).apply()
    }

    fun persistSnapshot(media: List<MediaImage>) = saveSnapshot(media)

    suspend fun renameMedia(item: MediaImage, newName: String): MediaImage? = withContext(Dispatchers.IO) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return@withContext null

        val currentExt = item.name.substringAfterLast('.', "")
        val finalName = if (trimmedName.contains('.') || currentExt.isEmpty()) trimmedName else "$trimmedName.$currentExt"
        if (finalName == item.name) return@withContext item

        val srcFile = if (item.path.isNotBlank()) File(item.path) else null
        var renamedPath = item.path

        val canonicalUri = if (item.uri.authority == "media") {
            item.uri.buildUpon().clearQuery().build()
        } else if (item.isVideo) {
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        } else {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        }

        var updatedInMediaStore = false
        if (item.id > 0) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            }
            updatedInMediaStore = runCatching {
                context.contentResolver.update(canonicalUri, values, null, null) > 0
            }.getOrDefault(false)
        }

        if (updatedInMediaStore) {
            val (actualName, actualPath) = runCatching {
                context.contentResolver.query(
                    canonicalUri,
                    arrayOf(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATA,
                        if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val relCol = if (android.os.Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                        val resolvedName = if (nameCol >= 0) cursor.getString(nameCol) else null
                        val rawData = if (dataCol >= 0) cursor.getString(dataCol) else null
                        val relPath = if (relCol >= 0) cursor.getString(relCol) else null
                        val resolvedPath = when {
                            !rawData.isNullOrBlank() -> rawData
                            !relPath.isNullOrBlank() && !resolvedName.isNullOrBlank() -> {
                                val volName = canonicalUri.pathSegments.firstOrNull().orEmpty()
                                val root = if (volName.isBlank() || volName == "external" || volName == "external_primary") {
                                    "/storage/emulated/0/"
                                } else {
                                    "/storage/$volName/"
                                }
                                "$root$relPath$resolvedName"
                            }
                            else -> null
                        }
                        Pair(resolvedName, resolvedPath)
                    } else Pair(null, null)
                }
            }.getOrNull() ?: Pair(null, null)

            val resolvedFinalName = actualName ?: finalName
            renamedPath = actualPath ?: if (srcFile != null && srcFile.parentFile != null) {
                File(srcFile.parentFile, resolvedFinalName).absolutePath
            } else {
                item.path
            }

            verifiedPathsCache.remove(item.path)
            verifiedPathsCache[renamedPath] = System.currentTimeMillis()
            RestoredTimestamps.moveDateAdded(context, item.path, renamedPath)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(renamedPath), null, null)

            val updatedItem = item.copy(name = resolvedFinalName, path = renamedPath)
            inMemoryCache[item.id] = updatedItem
            return@withContext updatedItem
        }

        // Direct filesystem rename fallback (for Android <= 28 or full storage access)
        if (srcFile != null && srcFile.exists() && srcFile.parentFile != null) {
            val parent = srcFile.parentFile!!
            var destFile = File(parent, finalName)
            if (destFile.exists() && destFile.absolutePath != srcFile.absolutePath) {
                val base = finalName.substringBeforeLast('.')
                val ext = if (finalName.contains('.')) ".${finalName.substringAfterLast('.')}" else ""
                var index = 1
                while (destFile.exists()) {
                    destFile = File(parent, "$base ($index)$ext")
                    index++
                }
            }
            val lastModified = srcFile.lastModified()
            val renamed = runCatching { srcFile.renameTo(destFile) }.getOrDefault(false)
            if (renamed) {
                if (lastModified > 0) destFile.setLastModified(lastModified)
                renamedPath = destFile.absolutePath
                val resolvedFinalName = destFile.name
                verifiedPathsCache.remove(item.path)
                verifiedPathsCache[renamedPath] = System.currentTimeMillis()
                RestoredTimestamps.moveDateAdded(context, item.path, renamedPath)

                if (item.id > 0 && canonicalUri.authority == "media") {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, resolvedFinalName)
                        if (android.os.Build.VERSION.SDK_INT <= 28) {
                            put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                        }
                    }
                    runCatching { context.contentResolver.update(canonicalUri, values, null, null) }
                }
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath, srcFile.absolutePath),
                    null,
                    null
                )
                val updatedItem = item.copy(name = resolvedFinalName, path = renamedPath)
                inMemoryCache[item.id] = updatedItem
                return@withContext updatedItem
            }
        }

        null
    }

    private fun saveSnapshot(media: List<MediaImage>) {
        var stream: java.io.FileOutputStream? = null
        runCatching {
            stream = snapshot.startWrite()
            val output = DataOutputStream(stream!!.buffered())
            output.writeInt(6); output.writeInt(media.size)
            media.forEach { item ->
                output.writeLong(item.id); output.writeBoolean(item.isVideo); output.writeUTF(item.name.take(8_000))
                output.writeUTF(item.uri.toString().take(4_000))
                output.writeLong(item.dateTaken); output.writeInt(item.width); output.writeInt(item.height)
                output.writeUTF(item.path.take(16_000)); output.writeLong(item.bucketId); output.writeUTF(item.bucketName.take(8_000))
                output.writeLong(item.durationMs); output.writeUTF(item.mimeType.take(1_000)); output.writeLong(item.sizeBytes)
                output.writeInt(item.orientation); output.writeUTF(item.title.take(8_000))
                output.writeLong(item.dateModified); output.writeLong(item.dateAdded)
                output.writeBoolean(item.id in exifDateVerifiedIds)
            }
            output.flush()
            snapshot.finishWrite(stream)
        }.onFailure { snapshot.failWrite(stream) }
    }
}
