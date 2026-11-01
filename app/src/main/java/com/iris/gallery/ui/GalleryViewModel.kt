package com.iris.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.LibraryPreferences
import com.iris.gallery.data.LibraryPreferencesState
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.VaultRepository
import com.iris.gallery.data.TrashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.iris.gallery.data.SettingsPreferences
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import com.iris.gallery.data.DuplicateDetector
import com.iris.gallery.data.DuplicateGroup
import com.iris.gallery.data.ExifEditRequest

import com.iris.gallery.data.AlbumRepository
import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

data class GalleryUiState(
    val loading: Boolean = false,
    val images: List<MediaImage> = emptyList(),
    val trashed: List<MediaImage> = emptyList(),
    val error: String? = null,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val libraryPreferences = LibraryPreferences(application)
    private val vaultRepository = VaultRepository(application)
    private val trashRepository = TrashRepository(application)
    private val albumRepository = AlbumRepository(application)
    private val settingsPreferences = SettingsPreferences(application)
    private val preferences = application.getSharedPreferences("gallery", 0)
    private val _uiState = MutableStateFlow(GalleryUiState(images = repository.loadSnapshot(), trashed = trashRepository.trashedMedia.value))
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private val _favorites = MutableStateFlow(
        preferences.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
    )
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()
    val libraryState: StateFlow<LibraryPreferencesState> = libraryPreferences.state
    val vaultMedia: StateFlow<List<MediaImage>> = vaultRepository.vaultMedia
    val trashedMedia: StateFlow<List<MediaImage>> = trashRepository.trashedMedia
    private val duplicateDetector = DuplicateDetector(application)
    private var duplicateJob: Job? = null
    private val _duplicateState = MutableStateFlow(DuplicateScanState())
    val duplicateState: StateFlow<DuplicateScanState> = _duplicateState.asStateFlow()

    init {
        viewModelScope.launch {
            trashRepository.trashedMedia.collect { trashedList ->
                val systemTrash = if (android.os.Build.VERSION.SDK_INT >= 30) {
                    runCatching { repository.loadImages(trashed = true) }.getOrDefault(emptyList())
                } else emptyList()
                val combined = (trashedList + systemTrash).distinctBy { it.id }
                _uiState.value = _uiState.value.copy(trashed = combined)
            }
        }
        viewModelScope.launch {
            settingsPreferences.state.map { it.useSystemTrash }.distinctUntilChanged().drop(1).collect {
                refresh(showLoading = false)
            }
        }
        viewModelScope.launch {
            // Adopt vault files that were copied but never made it into the index.
            vaultRepository.recoverOrphanedItems()
        }
    }

    fun scanDuplicates() {
        if (duplicateJob?.isActive == true) return
        duplicateJob = viewModelScope.launch {
            _duplicateState.value = DuplicateScanState(scanning = true)
            runCatching {
                duplicateDetector.scan(_uiState.value.images) { done, total ->
                    _duplicateState.value = _duplicateState.value.copy(done = done, total = total)
                }
            }.onSuccess { groups ->
                _duplicateState.value = DuplicateScanState(groups = groups, hasScanned = true)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    _duplicateState.value = DuplicateScanState()
                } else _duplicateState.value = DuplicateScanState(hasScanned = true, error = error.message ?: "Scan failed")
            }
        }
    }

    fun cancelDuplicateScan() { duplicateJob?.cancel() }

    fun setLocked(ids: Collection<Long>, locked: Boolean) = libraryPreferences.setLocked(ids, locked)
    fun hasAllFilesAccess(): Boolean = vaultRepository.hasAllFilesAccess()
    suspend fun moveToVault(mediaList: List<MediaImage>): com.iris.gallery.data.VaultMoveResult = vaultRepository.moveToVault(mediaList)
    suspend fun rollbackVaultMove(vaultedMedia: List<MediaImage>) = vaultRepository.rollbackVault(vaultedMedia)
    suspend fun restoreFromVault(mediaList: List<MediaImage>): List<MediaImage> = vaultRepository.restoreFromVault(mediaList)
    suspend fun deletePermanentlyFromVault(mediaList: List<MediaImage>) = vaultRepository.deletePermanently(mediaList)

    suspend fun moveToTrash(mediaList: List<MediaImage>): com.iris.gallery.data.TrashMoveResult = trashRepository.moveToTrash(mediaList)
    suspend fun rollbackTrashMove(trashedMedia: List<MediaImage>) = trashRepository.rollbackTrash(trashedMedia)

    suspend fun restoreFromTrash(mediaList: List<MediaImage>): List<MediaImage> {
        val result = trashRepository.restoreFromTrash(mediaList)
        val restoredIds = result.map { it.id }.toSet()
        val restoredPaths = result.map { it.path }.filter { it.isNotBlank() }.toSet()
        repository.clearRecentMovedOrDeleted(restoredIds, restoredPaths)
        refresh()
        return result
    }

    suspend fun deletePermanently(mediaList: List<MediaImage>) {
        trashRepository.deletePermanently(mediaList)
        refresh()
    }

    suspend fun emptyTrash() {
        trashRepository.emptyTrash()
        refresh()
    }

    suspend fun moveMediaToAlbum(mediaList: List<MediaImage>, targetDir: File, targetAlbumName: String): AlbumOperationResult {
        val result = albumRepository.moveMedia(mediaList, targetDir, targetAlbumName)
        if (result.successCount > 0) {
            val movedOldIds = result.successfulSourceMedia.map { it.id }.toSet()
            val movedOldPaths = result.successfulSourceMedia.map { it.path }.toSet()
            val newIds = result.movedMedia.map { it.id }.toSet()
            result.successfulSourceMedia.forEach { item ->
                ThumbnailCache.remove(item.id)
            }
            // Only mark IDs in recentMovedOrDeletedIds if the row was truly deleted/recreated with a new ID.
            // In-place MediaStore RELATIVE_PATH moves preserve the item's row ID, so suppressing it would hide the moved photo!
            val trulyDeletedOldIds = movedOldIds.filterNot { it in newIds }.toSet()
            repository.markMovedOrDeleted(trulyDeletedOldIds, movedOldPaths)
            val currentImages = _uiState.value.images
            val remainingImages = currentImages.filterNot { it.id in movedOldIds || it.path in movedOldPaths }
            val updatedImages = (remainingImages + result.movedMedia).sortedWith(
                compareByDescending<MediaImage> { it.dateTaken }
                    .thenByDescending { it.dateModified }
                    .thenByDescending { it.dateAdded }
                    .thenByDescending { it.id }
            )
            _uiState.value = _uiState.value.copy(images = updatedImages)
        }
        refresh(showLoading = false)
        return result
    }

    suspend fun copyMediaToAlbum(mediaList: List<MediaImage>, targetDir: File, targetAlbumName: String): AlbumOperationResult {
        val result = albumRepository.copyMedia(mediaList, targetDir, targetAlbumName)
        if (result.successCount > 0) {
            val currentImages = _uiState.value.images
            val updatedImages = (currentImages + result.movedMedia).distinctBy { it.path }.sortedWith(
                compareByDescending<MediaImage> { it.dateTaken }
                    .thenByDescending { it.dateModified }
                    .thenByDescending { it.dateAdded }
                    .thenByDescending { it.id }
            )
            _uiState.value = _uiState.value.copy(images = updatedImages)
        }
        refresh(showLoading = false)
        return result
    }

    fun getAlbumDirectory(album: MediaAlbum): File = albumRepository.getAlbumDirectory(album)
    fun createNewAlbumDirectory(albumName: String): File = albumRepository.createNewAlbumDirectory(albumName)

    fun togglePinnedAlbum(id: Long) = libraryPreferences.togglePinnedAlbum(id)
    fun setAlbumCover(albumId: Long, mediaId: Long) = libraryPreferences.setAlbumCover(albumId, mediaId)
    fun setAlbumSort(sort: AlbumSort) = libraryPreferences.setAlbumSort(sort)
    fun setAlbumOrder(order: List<Long>) = libraryPreferences.setAlbumOrder(order)
    fun setAlbumMediaSort(sort: com.iris.gallery.data.MediaSort) = libraryPreferences.setAlbumMediaSort(sort)
    fun setAlbumMediaSortOverride(albumId: Long, sort: com.iris.gallery.data.MediaSort?) = libraryPreferences.setAlbumMediaSortOverride(albumId, sort)
    fun addExcludedFolder(path: String) = libraryPreferences.addExcludedFolder(path)
    fun removeExcludedFolder(path: String) = libraryPreferences.removeExcludedFolder(path)
    fun setExcludedFolders(folders: Set<String>) = libraryPreferences.setExcludedFolders(folders)

    fun toggleFavorite(id: Long) {
        val updated = _favorites.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        _favorites.value = updated
        preferences.edit().putStringSet("favorites", updated.mapTo(mutableSetOf()) { it.toString() }).apply()
    }

    private var contentObserver: ContentObserver? = null
    private var liveReloadJob: Job? = null
    private var lastGenerationByVolume: Map<String, Long>? = null
    private var hasCheckedObsolete = false
    private val pendingChangedUris = mutableSetOf<Uri>()

    fun registerObserver() {
        if (contentObserver != null) return
        runCatching {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (uri != null) {
                        synchronized(pendingChangedUris) { pendingChangedUris.add(uri) }
                    }
                    liveReloadJob?.cancel()
                    liveReloadJob = viewModelScope.launch {
                        delay(250)
                        val batch = synchronized(pendingChangedUris) {
                            pendingChangedUris.toSet().also { pendingChangedUris.clear() }
                        }
                        val itemUris = batch.filter { it.lastPathSegment?.toLongOrNull() != null }
                        if (batch.isNotEmpty() && itemUris.size == batch.size && itemUris.size <= 200) {
                            applyIncrementalMediaChanges(itemUris)
                        } else {
                            checkForChanges()
                        }
                    }
                }
            }
            val resolver = getApplication<Application>().contentResolver
            val volumes = if (android.os.Build.VERSION.SDK_INT >= 29) {
                runCatching { MediaStore.getExternalVolumeNames(getApplication()) }
                    .getOrNull()
                    ?.filter { it.isNotBlank() }
                    ?.toMutableSet()
                    ?: mutableSetOf(MediaStore.VOLUME_EXTERNAL)
            } else {
                mutableSetOf(MediaStore.VOLUME_EXTERNAL)
            }
            volumes.add(MediaStore.VOLUME_EXTERNAL)
            for (volume in volumes) {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    resolver.registerContentObserver(MediaStore.Images.Media.getContentUri(volume), true, observer)
                    resolver.registerContentObserver(MediaStore.Video.Media.getContentUri(volume), true, observer)
                } else {
                    resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
                    resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
                }
                // The files table carries the per-volume row notifications on newer
                // Android versions, so watch it too.
                resolver.registerContentObserver(MediaStore.Files.getContentUri(volume), true, observer)
            }
            contentObserver = observer
        }
    }

    override fun onCleared() {
        super.onCleared()
        contentObserver?.let {
            runCatching {
                getApplication<Application>().contentResolver.unregisterContentObserver(it)
            }
        }
        contentObserver = null
    }

    fun renameMedia(item: MediaImage, newName: String, onResult: (MediaImage?) -> Unit = {}) {
        viewModelScope.launch {
            val updated = repository.renameMedia(item, newName)
            if (updated != null) {
                _uiState.update { state ->
                    state.copy(
                        images = state.images.map { if (it.id == item.id) updated else it }
                    )
                }
                ThumbnailCache.remove(item.id)
            }
            onResult(updated)
        }
    }

    fun updateMediaMetadata(media: MediaImage, request: ExifEditRequest): MediaImage {
        val newTitle = request.title
        val newDescription = when {
            request.imageDescription != null -> request.imageDescription
            request.stripAllExif -> ""
            else -> media.description
        }
        libraryPreferences.setCustomTitle(media.id, request.title.ifBlank { null })
        val updated = media.copy(
            title = newTitle,
            orientation = request.orientation,
            dateTaken = request.dateTakenMillis,
            description = newDescription,
        )
        if (media.orientation != request.orientation) {
            ThumbnailCache.remove(media.id)
        }
        _uiState.update { state ->
            state.copy(
                images = state.images.map { if (it.id == media.id) updated else it },
                trashed = state.trashed.map { if (it.id == media.id) updated else it },
            )
        }
        return updated
    }

    fun markMediaDeleted(ids: Collection<Long>, paths: Collection<String> = emptyList()) {
        if (ids.isEmpty() && paths.isEmpty()) return
        val idSet = ids.toSet()
        val pathSet = paths.toSet()
        idSet.forEach { libraryPreferences.setCustomTitle(it, null) }
        repository.markMovedOrDeleted(idSet, pathSet)
        idSet.forEach { ThumbnailCache.remove(it) }
        _uiState.update { state ->
            state.copy(
                images = state.images.filterNot { it.id in idSet || it.path in pathSet }
            )
        }
        if (_duplicateState.value.groups.isNotEmpty()) {
            val updatedGroups = _duplicateState.value.groups.mapNotNull { group ->
                val remainingItems = group.items.filterNot { it.id in idSet || it.path in pathSet }
                if (remainingItems.size > 1) group.copy(items = remainingItems) else null
            }
            _duplicateState.update { it.copy(groups = updatedGroups) }
        }
    }

    fun rescanMedia(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val count = repository.rescanStorage()
            refresh(showLoading = false)
            onComplete(count)
        }
    }

    fun restoreSystemTrash(ids: Set<Long>, paths: Set<String> = emptySet()) {
        repository.clearRecentMovedOrDeleted(ids, paths)
        refresh(showLoading = false)
    }

    fun purgeGhostMedia(image: MediaImage) {
        repository.purgeGhostMedia(image.id, image.path, image.uri)
        com.iris.gallery.ui.ThumbnailCache.remove(image.id)
    }

    private fun mergeChangedMedia(idSet: Set<Long>, changed: List<MediaImage>) {
        var merged: List<MediaImage> = emptyList()
        _uiState.update { state ->
            merged = (state.images.filterNot { it.id in idSet } + changed)
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<MediaImage> { it.dateTaken }
                        .thenByDescending { it.dateModified }
                        .thenByDescending { it.dateAdded }
                        .thenByDescending { it.id }
                )
            state.copy(images = merged)
        }
        if (merged.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) { repository.persistSnapshot(merged) }
        }
    }

    private fun isOnKnownStoragePath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.startsWith("/storage/")) return true
        return path.startsWith(getApplication<Application>().filesDir.absolutePath)
    }

    private suspend fun refreshTrashOnly() {
        val trashList = withContext(Dispatchers.IO) {
            (vaultRepository.loadVaultItems() + trashRepository.loadTrashItems()).distinctBy { it.id }
        }
        _uiState.update { it.copy(trashed = trashList) }
    }

    private suspend fun applyIncrementalMediaChanges(uris: List<Uri>) {
        if (uris.size > 200) {
            refresh(showLoading = false)
            return
        }
        val ids = uris.mapNotNull { it.lastPathSegment?.toLongOrNull() }.toSet()
        val loaded = mutableListOf<MediaImage>()
        val pending = mutableListOf<Uri>()
        for (uri in uris) {
            val item = runCatching { repository.loadImageByUri(uri) }.getOrNull()
            if (item == null) {
                // Could be pending, still being written, or deleted. Retry it,
                // a deleted row simply keeps returning null and is ignored.
                pending.add(uri)
                continue
            }
            val ready = item.path.isNotBlank() &&
                isOnKnownStoragePath(item.path) &&
                java.io.File(item.path).exists()
            if (ready) loaded.add(item) else pending.add(uri)
        }
        mergeChangedMedia(ids, loaded)
        if (loaded.size < ids.size) refreshTrashOnly()
        if (pending.isNotEmpty()) retryPendingUris(pending)
    }

    private fun retryPendingUris(uris: List<Uri>) {
        viewModelScope.launch {
            val pending = uris.toMutableList()
            var delayMs = 1000L
            repeat(10) {
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(8000L)
                val loaded = mutableListOf<MediaImage>()
                val stillPending = mutableListOf<Uri>()
                for (uri in pending) {
                    val item = runCatching { repository.loadImageByUri(uri) }.getOrNull()
                    val ready = item != null &&
                        item.path.isNotBlank() &&
                        isOnKnownStoragePath(item.path) &&
                        java.io.File(item.path).exists()
                    if (ready) loaded.add(item!!) else stillPending.add(uri)
                }
                if (loaded.isNotEmpty()) {
                    mergeChangedMedia(loaded.mapTo(HashSet()) { it.id }, loaded)
                }
                pending.clear()
                pending.addAll(stillPending)
                if (pending.isEmpty()) return@launch
            }
        }
    }

    fun checkForChanges() {
        viewModelScope.launch {
            if (refreshJob?.isActive == true) return@launch
            if (_uiState.value.images.isEmpty()) {
                refresh(showLoading = false)
                return@launch
            }
            val previous = lastGenerationByVolume ?: repository.loadGenerationByVolume()
            val current = runCatching { repository.getGenerationByVolume() }.getOrNull()
            if (previous != null && current != null && repository.isSnapshotComplete()) {
                val changed = runCatching { repository.getChangedUrisSince(previous) }.getOrDefault(emptyList())
                lastGenerationByVolume = current
                repository.saveGenerationByVolume(current)

                if (!hasCheckedObsolete) {
                    val knownIds = _uiState.value.images.mapTo(HashSet()) { it.id }
                    val obsolete = runCatching { repository.checkObsoleteContentIds(knownIds) }.getOrDefault(emptySet())
                    hasCheckedObsolete = true
                    if (obsolete.isNotEmpty()) {
                        _uiState.update { state -> state.copy(images = state.images.filterNot { it.id in obsolete }) }
                    }
                }

                if (changed.isNotEmpty()) {
                    applyIncrementalMediaChanges(changed)
                } else {
                    refreshTrashOnly()
                }
            } else {
                refresh(showLoading = false)
            }
        }
    }

    private var refreshJob: Job? = null

    fun refresh(showLoading: Boolean = true, forceClearCache: Boolean = false) {
        val current = refreshJob
        if (current?.isActive == true) {
            current.cancel()
        }
        if (forceClearCache) {
            repository.clearVerifiedPathsCache()
        }
        refreshJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeout(120_000) {
                    // Index loads touch storage and take the repo index lock, so keep them off the main thread.
                    val internalTrash = withContext(Dispatchers.IO) {
                        vaultRepository.loadVaultItems()
                        trashRepository.loadTrashItems()
                    }
                    val systemTrash = if (android.os.Build.VERSION.SDK_INT >= 30 && settingsPreferences.state.value.useSystemTrash) {
                        try {
                            repository.loadImages(trashed = true)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList()
                        }
                    } else emptyList()
                    val trashList = (internalTrash + systemTrash).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            loading = if (showLoading) true else it.loading,
                            trashed = trashList,
                            error = null
                        )
                    }
                    try {
                        repository.setSnapshotComplete(false)
                        val loaded = repository.loadImages()
                        val loadedPaths = loaded.mapTo(HashSet(loaded.size)) { it.path }
                        val loadedIds = loaded.mapTo(HashSet(loaded.size)) { it.id }
                        val currentPending = _uiState.value.images.filter { item ->
                            item.path.isNotBlank() &&
                            item.path !in loadedPaths &&
                            item.id !in loadedIds &&
                            !repository.isMarkedMovedOrDeleted(item.id, item.path) &&
                            java.io.File(item.path).exists()
                        }
                        val combined = if (currentPending.isNotEmpty()) {
                            (loaded + currentPending).distinctBy { it.id }.sortedWith(
                                compareByDescending<MediaImage> { it.dateTaken }
                                    .thenByDescending { it.dateModified }
                                    .thenByDescending { it.dateAdded }
                                    .thenByDescending { it.id }
                            )
                        } else {
                            loaded
                        }
                        _uiState.update { it.copy(images = combined, loading = false, trashed = trashList, error = null) }
                        repository.setSnapshotComplete(true)
                        val generations = withContext(Dispatchers.IO) { repository.getGenerationByVolume() }
                        if (generations != null) {
                            lastGenerationByVolume = generations
                            repository.saveGenerationByVolume(generations)
                        }
                        if (_duplicateState.value.hasScanned) _duplicateState.value = DuplicateScanState()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        android.util.Log.e("GalleryViewModel", "refresh loadImages failed", e)
                        if (_uiState.value.images.isEmpty()) {
                            _uiState.update { state -> state.copy(loading = false, error = e.message ?: "Could not load photos") }
                        } else {
                            _uiState.update { it.copy(loading = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GalleryViewModel", "refresh overall failed", e)
                _uiState.update { it.copy(loading = false) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}

data class DuplicateScanState(
    val scanning: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val groups: List<DuplicateGroup> = emptyList(),
    val hasScanned: Boolean = false,
    val error: String? = null,
)
