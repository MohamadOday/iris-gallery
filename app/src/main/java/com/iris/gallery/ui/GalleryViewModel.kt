package com.iris.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.UriLoadResult
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

data class VaultOperationProgress(
    val isRestoring: Boolean,
    val done: Int,
    val total: Int,
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
    private val _hiddenMedia = MutableStateFlow<List<MediaImage>>(emptyList())
    val hiddenMedia: StateFlow<List<MediaImage>> = _hiddenMedia.asStateFlow()
    private val _vaultProgress = MutableStateFlow<VaultOperationProgress?>(null)
    val vaultProgress: StateFlow<VaultOperationProgress?> = _vaultProgress.asStateFlow()
    private val duplicateDetector = DuplicateDetector(application)
    private var duplicateJob: Job? = null
    private val _duplicateState = MutableStateFlow(DuplicateScanState())
    val duplicateState: StateFlow<DuplicateScanState> = _duplicateState.asStateFlow()
    private var autoBackupDebounceJob: Job? = null
    @Volatile
    private var autoBackupDirty = false
    @Volatile
    private var suppressAutoBackupUntilMs = 0L

    init {
        viewModelScope.launch {
            trashRepository.trashedMedia.collect { trashedList ->
                val combined = loadCombinedTrash(trashedList)
                _uiState.value = _uiState.value.copy(trashed = combined)
            }
        }
        viewModelScope.launch {
            settingsPreferences.state.map { it.useSystemTrash }.distinctUntilChanged().drop(1).collect {
                refreshTrashOnly()
            }
        }
        viewModelScope.launch {
            settingsPreferences.state.map { it.showHiddenItems }.distinctUntilChanged().collect { showHidden ->
                if (showHidden) {
                    refreshHiddenMedia()
                } else {
                    _hiddenMedia.value = emptyList()
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsPreferences.state.map {
                    it.copy(
                        lastAutoBackupTimestamp = 0L,
                        lastAutoBackupError = "",
                        lastAutoBackupHash = "",
                        autoBackupConsecutiveFailures = 0,
                        appLockFailedAttempts = 0,
                        appLockLockoutUntil = 0L,
                        dismissedFilmstripTip = false,
                        dismissedRotateTip = false,
                        dismissedMemoriesTip = false,
                        dismissedSecureSharingTip = false,
                    )
                }.distinctUntilChanged(),
                libraryPreferences.state.map { it.copy(lockedMedia = emptySet()) }.distinctUntilChanged(),
                _favorites,
            ) { settings, _, _ -> settings }
                .drop(1)
                .collect { settings ->
                    if (System.currentTimeMillis() < suppressAutoBackupUntilMs) return@collect
                    if (!settings.autoBackupEnabled || settings.autoBackupTreeUri.isBlank()) return@collect
                    if (settings.autoBackupFrequency != com.iris.gallery.data.AutoBackupFrequency.ON_CHANGE) return@collect
                    autoBackupDirty = true
                    autoBackupDebounceJob?.cancel()
                    autoBackupDebounceJob = viewModelScope.launch {
                        delay(6_000)
                        if (autoBackupDirty) {
                            runAutoBackupNow(force = false)
                        }
                    }
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

    fun recoverVaultOrphans() {
        viewModelScope.launch {
            vaultRepository.recoverOrphanedItems()
        }
    }

    suspend fun moveToVault(mediaList: List<MediaImage>): com.iris.gallery.data.VaultMoveResult {
        _vaultProgress.value = VaultOperationProgress(isRestoring = false, done = 0, total = mediaList.size)
        return try {
            vaultRepository.moveToVault(mediaList) { done, total ->
                _vaultProgress.value = VaultOperationProgress(isRestoring = false, done = done, total = total)
            }
        } finally {
            _vaultProgress.value = null
        }
    }

    suspend fun rollbackVaultMove(vaultedMedia: List<MediaImage>) = vaultRepository.rollbackVault(vaultedMedia)

    suspend fun restoreFromVault(mediaList: List<MediaImage>): com.iris.gallery.data.VaultRestoreResult {
        _vaultProgress.value = VaultOperationProgress(isRestoring = true, done = 0, total = mediaList.size)
        return try {
            val result = vaultRepository.restoreFromVault(mediaList) { done, total ->
                _vaultProgress.value = VaultOperationProgress(isRestoring = true, done = done, total = total)
            }
            val restoredIds = buildSet {
                result.restored.forEach {
                    add(it.id)
                    add(kotlin.math.abs(it.id))
                }
                mediaList.forEach {
                    add(it.id)
                    add(kotlin.math.abs(it.id))
                }
            }
            val restoredPaths = result.restored.map { it.path }.filter { it.isNotBlank() }.toSet() + result.restoredOriginalPaths
            repository.clearRecentMovedOrDeleted(restoredIds, restoredPaths)
            libraryPreferences.setLocked(restoredIds, false)
            result
        } finally {
            _vaultProgress.value = null
        }
    }

    suspend fun deletePermanentlyFromVault(mediaList: List<MediaImage>) = vaultRepository.deletePermanently(mediaList)

    suspend fun moveToTrash(mediaList: List<MediaImage>): com.iris.gallery.data.TrashMoveResult = trashRepository.moveToTrash(mediaList)
    suspend fun rollbackTrashMove(trashedMedia: List<MediaImage>) = trashRepository.rollbackTrash(trashedMedia)

    suspend fun restoreFromTrash(mediaList: List<MediaImage>): com.iris.gallery.data.TrashRestoreResult {
        val result = trashRepository.restoreFromTrash(mediaList)
        val restoredIds = result.restored.map { it.id }.toSet()
        val restoredPaths = result.restored.map { it.path }.filter { it.isNotBlank() }.toSet()
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
    fun setLibraryOrder(order: List<String>) = libraryPreferences.setLibraryOrder(order)
    fun setAlbumMediaSort(sort: com.iris.gallery.data.MediaSort) = libraryPreferences.setAlbumMediaSort(sort)
    fun setAlbumMediaSortOverride(albumId: Long, sort: com.iris.gallery.data.MediaSort?) = libraryPreferences.setAlbumMediaSortOverride(albumId, sort)
    fun setVaultAlbumSort(sort: AlbumSort) = libraryPreferences.setVaultAlbumSort(sort)
    fun setVaultMediaSort(sort: com.iris.gallery.data.MediaSort) = libraryPreferences.setVaultMediaSort(sort)
    fun createAlbumGroup(name: String, albumIds: Collection<Long>) = libraryPreferences.createAlbumGroup(name, albumIds)
    fun renameAlbumGroup(groupId: String, newName: String) = libraryPreferences.renameAlbumGroup(groupId, newName)
    fun setAlbumGroupCover(groupId: String, mediaId: Long?) = libraryPreferences.setAlbumGroupCover(groupId, mediaId)
    fun deleteAlbumGroup(groupId: String) = libraryPreferences.deleteAlbumGroup(groupId)
    fun setAlbumGroupMembers(groupId: String, albumIds: Collection<Long>) = libraryPreferences.setAlbumGroupMembers(groupId, albumIds)
    fun addAlbumToGroup(groupId: String, albumId: Long) = libraryPreferences.addAlbumToGroup(groupId, albumId)
    fun removeAlbumFromGroup(groupId: String, albumId: Long) = libraryPreferences.removeAlbumFromGroup(groupId, albumId)
    fun addExcludedFolder(path: String) = libraryPreferences.addExcludedFolder(path)
    fun removeExcludedFolder(path: String) = libraryPreferences.removeExcludedFolder(path)
    fun setExcludedFolders(folders: Set<String>) = libraryPreferences.setExcludedFolders(folders)

    fun refreshHiddenMedia() {
        viewModelScope.launch {
            if (!settingsPreferences.state.value.showHiddenItems) {
                _hiddenMedia.value = emptyList()
                return@launch
            }
            val loaded = runCatching { repository.loadHiddenImages() }.getOrDefault(emptyList())
            _hiddenMedia.value = loaded
        }
    }

    fun toggleFavorite(id: Long) {
        val updated = _favorites.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        _favorites.value = updated
        preferences.edit().putStringSet("favorites", updated.mapTo(mutableSetOf()) { it.toString() }).apply()
    }

    suspend fun exportEncryptedVaultBackup(
        outputUri: Uri,
        password: CharArray,
        onProgress: (Int, Int) -> Unit,
    ): Result<com.iris.gallery.data.VaultBackupExportResult> {
        return vaultRepository.exportEncryptedBackup(outputUri, password, onProgress)
    }

    suspend fun importEncryptedVaultBackup(
        inputUri: Uri,
        password: CharArray,
        onProgress: (Int, Int) -> Unit,
    ): Result<com.iris.gallery.data.VaultBackupImportResult> {
        return vaultRepository.importEncryptedBackup(inputUri, password, onProgress)
    }

    fun exportBackupJson(): String {
        return com.iris.gallery.data.BackupManager.buildBackupJson(
            settings = settingsPreferences.state.value,
            library = libraryPreferences.state.value,
            customTitles = libraryPreferences.getAllCustomTitles(),
            favorites = _favorites.value,
            images = _uiState.value.images,
        )
    }

    fun parseBackupPreview(rawJson: String): Result<com.iris.gallery.data.BackupPreview> {
        return com.iris.gallery.data.BackupManager.parseBackupPreview(rawJson)
    }

    suspend fun importBackupJson(rawJson: String): Result<com.iris.gallery.data.BackupRestoreSummary> = withContext(Dispatchers.Default) {
        val imagesForMatching = _uiState.value.images.ifEmpty {
            withContext(Dispatchers.IO) {
                repository.loadSnapshot().ifEmpty { runCatching { repository.loadImages() }.getOrDefault(emptyList()) }
            }
        }
        val result = com.iris.gallery.data.BackupManager.restoreFromJson(
            rawJson = rawJson,
            currentSettings = settingsPreferences.state.value,
            currentLibrary = libraryPreferences.state.value,
            images = imagesForMatching,
            hasAllFilesAccess = hasAllFilesAccess(),
        )
        result.map { payload ->
            suppressAutoBackupUntilMs = System.currentTimeMillis() + 2_000L
            autoBackupDirty = false
            autoBackupDebounceJob?.cancel()
            settingsPreferences.restoreFromBackup(payload.settings)
            libraryPreferences.restoreFromBackup(payload.library, payload.customTitles)
            _favorites.value = payload.favorites
            preferences.edit()
                .putStringSet("favorites", payload.favorites.mapTo(mutableSetOf()) { it.toString() })
                .apply()
            payload.summary
        }
    }

    suspend fun runAutoBackupNow(force: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val settings = settingsPreferences.state.value
        if (settings.autoBackupTreeUri.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No backup folder selected"))
        }
        val built = com.iris.gallery.data.BackupManager.buildBackup(
            settings = settings,
            library = libraryPreferences.state.value,
            customTitles = libraryPreferences.getAllCustomTitles(),
            favorites = _favorites.value,
            images = _uiState.value.images,
        )
        val writeResult = com.iris.gallery.data.AutoBackupManager.writeBackupToFolder(
            context = app,
            treeUriString = settings.autoBackupTreeUri,
            json = built.json,
            retentionCount = settings.autoBackupRetention,
            contentHash = built.contentHash,
            previousHash = settings.lastAutoBackupHash,
            skipIfUnchanged = !force,
        )
        writeResult
            .onSuccess { outcome ->
                autoBackupDirty = false
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
                com.iris.gallery.data.AutoBackupManager.notifyFailureIfNeeded(app, failures)
            }
        writeResult.map { it.fileName }
    }

    fun flushPendingAutoBackup() {
        if (!autoBackupDirty) return
        val settings = settingsPreferences.state.value
        if (!settings.autoBackupEnabled || settings.autoBackupTreeUri.isBlank()) {
            autoBackupDirty = false
            return
        }
        autoBackupDebounceJob?.cancel()
        autoBackupDirty = false
        val app = getApplication<Application>()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val currentSettings = settingsPreferences.state.value
            if (!currentSettings.autoBackupEnabled || currentSettings.autoBackupTreeUri.isBlank()) return@launch
            val built = com.iris.gallery.data.BackupManager.buildBackup(
                settings = currentSettings,
                library = libraryPreferences.state.value,
                customTitles = libraryPreferences.getAllCustomTitles(),
                favorites = _favorites.value,
                images = _uiState.value.images,
            )
            val writeResult = com.iris.gallery.data.AutoBackupManager.writeBackupToFolder(
                context = app,
                treeUriString = currentSettings.autoBackupTreeUri,
                json = built.json,
                retentionCount = currentSettings.autoBackupRetention,
                contentHash = built.contentHash,
                previousHash = currentSettings.lastAutoBackupHash,
                skipIfUnchanged = true,
            )
            writeResult
                .onSuccess { outcome ->
                    val timestamp = if (outcome.skippedUnchanged && currentSettings.lastAutoBackupTimestamp > 0L) {
                        currentSettings.lastAutoBackupTimestamp
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
                    com.iris.gallery.data.AutoBackupManager.notifyFailureIfNeeded(app, failures)
                }
        }
    }

    private var contentObserver: ContentObserver? = null
    private var liveReloadJob: Job? = null
    private var retryPendingJob: Job? = null
    @Volatile
    private var pendingCheckAfterRefresh = false
    private var lastGenerationByVolume: Map<String, Long>? = null
    private var hasCheckedObsolete = false
    private val pendingChangedUris = LinkedHashSet<Uri>()
    private var observerFullCheckNeeded = false
    @Volatile
    private var lastObserverEventMs = 0L
    private val pendingRetryAttempts = LinkedHashMap<Uri, Int>()

    fun registerObserver() {
        if (contentObserver != null) return
        runCatching {
            fun handleObservedUris(uris: Collection<Uri>) {
                synchronized(pendingChangedUris) {
                    if (uris.isEmpty()) {
                        observerFullCheckNeeded = true
                    } else {
                        for (u in uris) {
                            if (u.lastPathSegment?.toLongOrNull() != null) {
                                pendingChangedUris.add(u)
                            } else {
                                observerFullCheckNeeded = true
                            }
                        }
                    }
                }
                lastObserverEventMs = android.os.SystemClock.elapsedRealtime()
                // Never cancel an active drain job mid-flight: canceling after clearing
                // pendingChangedUris drops earlier URIs during rapid multi-file downloads.
                if (liveReloadJob?.isActive == true) return
                liveReloadJob = viewModelScope.launch {
                    try {
                        while (true) {
                            val burstStart = android.os.SystemClock.elapsedRealtime()
                            while (true) {
                                val now = android.os.SystemClock.elapsedRealtime()
                                val quietElapsed = now - lastObserverEventMs
                                val totalElapsed = now - burstStart
                                if (quietElapsed >= 250L || totalElapsed >= 1_000L) break
                                delay((250L - quietElapsed).coerceIn(25L, 250L))
                            }
                            val (batch, needsFullCheck) = synchronized(pendingChangedUris) {
                                val copy = pendingChangedUris.toList()
                                pendingChangedUris.clear()
                                val full = observerFullCheckNeeded
                                observerFullCheckNeeded = false
                                copy to full
                            }
                            if (batch.isEmpty() && !needsFullCheck) break
                            if (batch.isNotEmpty() && batch.size <= 200) {
                                applyIncrementalMediaChanges(batch)
                            }
                            if (needsFullCheck || batch.size > 200) {
                                checkForChanges()
                            }
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        android.util.Log.w("GalleryViewModel", "live observer drain failed", e)
                    }
                }
            }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    handleObservedUris(listOfNotNull(uri))
                }

                override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
                    handleObservedUris(uris)
                }
            }
            val resolver = getApplication<Application>().contentResolver
            val volumes = if (android.os.Build.VERSION.SDK_INT >= 29) {
                val detected = runCatching { MediaStore.getExternalVolumeNames(getApplication()) }
                    .getOrNull()
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                (detected + MediaStore.VOLUME_EXTERNAL).toSet()
            } else {
                setOf(MediaStore.VOLUME_EXTERNAL)
            }
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
        repository.updateCachedMetadata(updated)
        if (media.id < 0 || media.path.startsWith(getApplication<Application>().filesDir.absolutePath)) {
            viewModelScope.launch {
                vaultRepository.updateItemMetadata(media.id, request)
            }
        }
        var updatedSnapshotList: List<MediaImage> = emptyList()
        _uiState.update { state ->
            val mapped = state.images.map { if (it.id == media.id) updated else it }
            val finalImages = if (updated.dateTaken != media.dateTaken) {
                mapped.sortedWith(
                    compareByDescending<MediaImage> { it.dateTaken }
                        .thenByDescending { it.dateModified }
                        .thenByDescending { it.dateAdded }
                        .thenByDescending { it.id }
                )
            } else {
                mapped
            }
            updatedSnapshotList = finalImages
            state.copy(
                images = finalImages,
                trashed = state.trashed.map { if (it.id == media.id) updated else it },
            )
        }
        if (updatedSnapshotList.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) { repository.persistSnapshot(updatedSnapshotList) }
        }
        return updated
    }

    fun markMediaDeleted(ids: Collection<Long>, paths: Collection<String> = emptyList()) {
        if (ids.isEmpty() && paths.isEmpty()) return
        val idSet = ids.toSet()
        val pathSet = paths.toSet()
        idSet.forEach { libraryPreferences.setCustomTitle(it, null) }
        pathSet.forEach { com.iris.gallery.data.RestoredTimestamps.removeDateAdded(getApplication(), it) }
        repository.markMovedOrDeleted(idSet, pathSet)
        idSet.forEach { ThumbnailCache.remove(it) }
        _uiState.update { state ->
            state.copy(
                images = state.images.filterNot { it.id in idSet || it.path in pathSet },
                trashed = state.trashed.filterNot { it.id in idSet || it.path in pathSet },
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

    private fun removeGhostPathsFromState(ghostPaths: Set<String>) {
        if (ghostPaths.isEmpty()) return
        var updated: List<MediaImage>? = null
        _uiState.update { state ->
            val removed = state.images.filter { it.path in ghostPaths }
            if (removed.isEmpty()) return@update state
            removed.forEach { ThumbnailCache.remove(it.id) }
            val remaining = state.images.filterNot { it.path in ghostPaths }
            updated = remaining
            state.copy(images = remaining)
        }
        updated?.let { remaining ->
            if (remaining.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) { repository.persistSnapshot(remaining) }
            }
        }
    }

    fun rescanMedia(onComplete: (Int) -> Unit = {}) {
        backgroundRescanJob?.cancel()
        backgroundRescanJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.recoverOrphanedItems()
                    vaultRepository.loadVaultItems()
                }
                refreshTrashOnly()
                if (settingsPreferences.state.value.showHiddenItems) {
                    refreshHiddenMedia()
                }
                val result = repository.rescanStoragePrioritized(
                    recentOnly = false,
                    forceReverifyExif = true,
                    onBatchScanned = { uris -> applyIncrementalMediaChanges(uris) },
                    onGhostsFound = { ghosts -> removeGhostPathsFromState(ghosts) },
                )
                val latestGen = withContext(Dispatchers.IO) { repository.getGenerationByVolume() }
                if (latestGen != null) {
                    lastGenerationByVolume = latestGen
                    repository.saveGenerationByVolume(latestGen)
                }
                scheduleExifDateEnrichment(_uiState.value.images)
                onComplete(result.scannedCount)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("GalleryViewModel", "rescanMedia failed", e)
                onComplete(0)
            }
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
        if (idSet.isEmpty() && changed.isEmpty()) return
        if (refreshJob?.isActive == true) {
            pendingCheckAfterRefresh = true
        }
        val changedPaths = changed.mapNotNullTo(HashSet(changed.size)) { it.path.takeIf(String::isNotBlank) }
        var merged: List<MediaImage> = emptyList()
        _uiState.update { state ->
            merged = (state.images.filterNot { it.id in idSet || (it.path.isNotBlank() && it.path in changedPaths) } + changed)
                .distinctBy { it.id }
                .distinctBy { it.path.ifBlank { it.id.toString() } }
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

    private fun deduplicateMediaUris(uris: List<Uri>): List<Uri> {
        val byId = LinkedHashMap<Long, Uri>(uris.size)
        val withoutId = mutableListOf<Uri>()
        for (uri in uris) {
            val id = uri.lastPathSegment?.toLongOrNull()
            if (id == null) {
                withoutId.add(uri)
                continue
            }
            val existing = byId[id]
            val isTyped = uri.pathSegments.let { it.contains("images") || it.contains("video") }
            val existingTyped = existing?.pathSegments?.let { it.contains("images") || it.contains("video") } ?: false
            if (existing == null || (isTyped && !existingTyped)) {
                byId[id] = uri
            }
        }
        return byId.values.toList() + withoutId
    }

    private fun isOnKnownStoragePath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.startsWith("/storage/") || path.startsWith("/sdcard/")) return true
        return path.startsWith(getApplication<Application>().filesDir.absolutePath)
    }

    private suspend fun loadCombinedTrash(internalTrash: List<MediaImage>): List<MediaImage> {
        val systemTrash = if (android.os.Build.VERSION.SDK_INT >= 30 &&
            settingsPreferences.state.value.useSystemTrash
        ) {
            withContext(Dispatchers.IO) {
                try {
                    repository.loadImages(trashed = true).sortedByDescending { it.dateModified }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    emptyList()
                }
            }
        } else {
            emptyList()
        }
        return (internalTrash + systemTrash).distinctBy { it.id }
    }

    private suspend fun refreshTrashOnly() {
        val internalTrash = withContext(Dispatchers.IO) { trashRepository.loadTrashItems() }
        val trashList = loadCombinedTrash(internalTrash)
        _uiState.update { it.copy(trashed = trashList) }
    }

    private suspend fun applyIncrementalMediaChanges(uris: List<Uri>) {
        val uniqueUris = deduplicateMediaUris(uris)
        if (uniqueUris.size > 200) {
            refresh(showLoading = false)
            return
        }
        val loaded = mutableListOf<MediaImage>()
        val removedIds = mutableSetOf<Long>()
        val pending = mutableListOf<Uri>()
        for (uri in uniqueUris) {
            when (val result = repository.loadImageByUriResult(uri)) {
                is UriLoadResult.Ready -> {
                    if (isOnKnownStoragePath(result.item.path)) {
                        loaded.add(result.item)
                    } else {
                        pending.add(uri)
                    }
                }
                is UriLoadResult.Pending -> pending.add(uri)
                is UriLoadResult.MissingOrDeleted -> {
                    uri.lastPathSegment?.toLongOrNull()?.let { removedIds.add(it) }
                }
            }
        }
        val changedIds = HashSet<Long>(removedIds.size + loaded.size).apply {
            addAll(removedIds)
            loaded.forEach { add(it.id) }
        }
        mergeChangedMedia(changedIds, loaded)
        if (
            removedIds.isNotEmpty() ||
            (settingsPreferences.state.value.useSystemTrash && android.os.Build.VERSION.SDK_INT >= 30 && loaded.isNotEmpty())
        ) {
            refreshTrashOnly()
        }
        if (pending.isNotEmpty()) scheduleRetryPendingUris(pending)
    }

    private fun scheduleRetryPendingUris(uris: List<Uri>) {
        synchronized(pendingRetryAttempts) {
            for (u in deduplicateMediaUris(uris)) {
                pendingRetryAttempts.putIfAbsent(u, 0)
            }
        }
        if (retryPendingJob?.isActive == true) return
        retryPendingJob = viewModelScope.launch {
            val maxAttemptsPerUri = 15
            var consecutiveEmptyWaves = 0
            while (true) {
                val delayMs = when (consecutiveEmptyWaves) {
                    0 -> 800L
                    1 -> 1_500L
                    2 -> 2_500L
                    else -> 4_000L
                }
                delay(delayMs)
                val snapshot = synchronized(pendingRetryAttempts) {
                    if (pendingRetryAttempts.isEmpty()) return@launch
                    pendingRetryAttempts.entries.map { it.key to it.value }
                }
                val batch = deduplicateMediaUris(snapshot.map { it.first })
                val loaded = mutableListOf<MediaImage>()
                val doneUris = HashSet<Uri>()
                val stillPendingUris = HashSet<Uri>()
                for (uri in batch) {
                    when (val result = repository.loadImageByUriResult(uri)) {
                        is UriLoadResult.Ready -> {
                            if (isOnKnownStoragePath(result.item.path)) {
                                loaded.add(result.item)
                                doneUris.add(uri)
                            } else {
                                stillPendingUris.add(uri)
                            }
                        }
                        is UriLoadResult.Pending -> stillPendingUris.add(uri)
                        is UriLoadResult.MissingOrDeleted -> doneUris.add(uri)
                    }
                }
                if (loaded.isNotEmpty()) {
                    mergeChangedMedia(loaded.mapTo(HashSet()) { it.id }, loaded)
                    consecutiveEmptyWaves = 0
                } else {
                    consecutiveEmptyWaves++
                }
                val hasMore = synchronized(pendingRetryAttempts) {
                    for (done in doneUris) {
                        pendingRetryAttempts.remove(done)
                    }
                    for (pendingUri in stillPendingUris) {
                        val nextAttempt = (pendingRetryAttempts[pendingUri] ?: 0) + 1
                        if (nextAttempt >= maxAttemptsPerUri) {
                            pendingRetryAttempts.remove(pendingUri)
                        } else {
                            pendingRetryAttempts[pendingUri] = nextAttempt
                        }
                    }
                    pendingRetryAttempts.isNotEmpty()
                }
                if (!hasMore) return@launch
            }
        }
    }

    fun checkForChanges() {
        viewModelScope.launch {
            if (refreshJob?.isActive == true) {
                pendingCheckAfterRefresh = true
                return@launch
            }
            if (_uiState.value.images.isEmpty()) {
                refresh(showLoading = false)
                return@launch
            }
            val previous = lastGenerationByVolume ?: repository.loadGenerationByVolume()
            val current = runCatching { repository.getGenerationByVolume() }.getOrNull()
            val prevVersion = repository.loadMediaStoreVersion()
            val currVersion = withContext(Dispatchers.IO) { repository.getCurrentMediaStoreVersion() }
            val versionMismatch = prevVersion != null && currVersion != null && prevVersion != currVersion
            val generationInvalid = previous != null && current != null && current.any { (vol, gen) ->
                val prevGen = previous[vol]
                prevGen == null || gen < prevGen
            }
            if (previous != null && current != null && !versionMismatch && !generationInvalid && repository.isSnapshotComplete()) {
                val changed = if (current != previous) {
                    runCatching { repository.getChangedUrisSince(previous) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                lastGenerationByVolume = current
                repository.saveGenerationByVolume(current)
                if (currVersion != null) repository.saveMediaStoreVersion(currVersion)

                if (!hasCheckedObsolete) {
                    val knownIds = _uiState.value.images.mapNotNullTo(HashSet()) { it.id.takeIf { id -> id > 0 } }
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
                scheduleExifDateEnrichment(_uiState.value.images)
            } else {
                refresh(showLoading = false)
            }
        }
    }

    private var refreshJob: Job? = null
    private var backgroundRescanJob: Job? = null
    private var exifEnrichJob: Job? = null

    private fun scheduleExifDateEnrichment(items: List<MediaImage>) {
        if (items.isEmpty() || !repository.hasUnverifiedExifDates(items)) return
        if (exifEnrichJob?.isActive == true) return
        exifEnrichJob = viewModelScope.launch {
            try {
                var checkedAnyOverall = false
                while (true) {
                    val current = _uiState.value.images
                    if (current.isEmpty() || !repository.hasUnverifiedExifDates(current)) break
                    val checkedThisPass = repository.enrichExifDatesInBackground(
                        items = current,
                        onBatchUpdated = { updatedDates ->
                            _uiState.update { state ->
                                var changed = false
                                val updatedList = state.images.map { img ->
                                    val newDate = updatedDates[img.id]
                                    if (newDate != null && newDate != img.dateTaken) {
                                        changed = true
                                        img.copy(dateTaken = newDate)
                                    } else {
                                        img
                                    }
                                }
                                if (!changed) state else state.copy(
                                    images = updatedList.sortedWith(
                                        compareByDescending<MediaImage> { it.dateTaken }
                                            .thenByDescending { it.dateModified }
                                            .thenByDescending { it.dateAdded }
                                            .thenByDescending { it.id }
                                    )
                                )
                            }
                        },
                        onCheckpoint = {
                            val latest = _uiState.value.images
                            if (latest.isNotEmpty()) {
                                withContext(Dispatchers.IO) { repository.persistSnapshot(latest) }
                            }
                        },
                    )
                    if (!checkedThisPass) break
                    checkedAnyOverall = true
                }
                if (checkedAnyOverall) {
                    val latest = _uiState.value.images
                    if (latest.isNotEmpty()) {
                        withContext(Dispatchers.IO) { repository.persistSnapshot(latest) }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("GalleryViewModel", "background EXIF enrichment failed", e)
            }
        }
    }

    fun rescanNewFilesInBackground(skipRecentPhase: Boolean = false) {
        if (backgroundRescanJob?.isActive == true) return
        backgroundRescanJob = viewModelScope.launch {
            runCatching {
                repository.rescanStoragePrioritized(
                    recentOnly = false,
                    skipRecentPhase = skipRecentPhase,
                    forceReverifyExif = false,
                    onBatchScanned = { uris -> applyIncrementalMediaChanges(uris) },
                    onGhostsFound = { ghosts -> removeGhostPathsFromState(ghosts) },
                )
                val latestGen = withContext(Dispatchers.IO) { repository.getGenerationByVolume() }
                if (latestGen != null) {
                    lastGenerationByVolume = latestGen
                    repository.saveGenerationByVolume(latestGen)
                }
            }
        }
    }

    /**
     * Pull-to-refresh entry point: syncs recent MediaStore deltas and the most recently modified
     * storage folders first (streaming tiles live as they index), stops the pull-to-refresh
     * spinner as soon as Phase 1 finishes, and lets older folders finish scanning silently in the
     * background.
     */
    fun quickRefresh() {
        if (_uiState.value.images.isEmpty() || !repository.isSnapshotComplete()) {
            refresh(showLoading = true, forceClearCache = false, rescanStorage = false)
            rescanNewFilesInBackground()
            return
        }
        val current = refreshJob
        if (current?.isActive == true) {
            current.cancel()
        }
        backgroundRescanJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                kotlinx.coroutines.withTimeout(30_000) {
                    val internalTrash = withContext(Dispatchers.IO) {
                        vaultRepository.recoverOrphanedItems()
                        vaultRepository.loadVaultItems()
                        trashRepository.loadTrashItems()
                    }
                    val trashList = loadCombinedTrash(internalTrash)
                    _uiState.update { it.copy(trashed = trashList, error = null) }

                    // Fast MediaStore generation delta check + obsolete ID prune (or cached loadImages on API < 30)
                    val previous = lastGenerationByVolume ?: repository.loadGenerationByVolume()
                    val currentGen = runCatching { repository.getGenerationByVolume() }.getOrNull()
                    val prevVersion = repository.loadMediaStoreVersion()
                    val currVersion = withContext(Dispatchers.IO) { repository.getCurrentMediaStoreVersion() }
                    val versionMismatch = prevVersion != null && currVersion != null && prevVersion != currVersion
                    val generationInvalid = previous != null && currentGen != null && currentGen.any { (vol, gen) ->
                        val prevGen = previous[vol]
                        prevGen == null || gen < prevGen
                    }
                    if (previous != null && currentGen != null && !versionMismatch && !generationInvalid) {
                        if (currentGen != previous) {
                            val changed = runCatching { repository.getChangedUrisSince(previous) }.getOrDefault(emptyList())
                            if (changed.isNotEmpty()) {
                                applyIncrementalMediaChanges(changed)
                            }
                        }
                        val knownIds = _uiState.value.images.mapNotNullTo(HashSet()) { it.id.takeIf { id -> id > 0 } }
                        val obsolete = runCatching { repository.checkObsoleteContentIds(knownIds) }.getOrDefault(emptySet())
                        if (obsolete.isNotEmpty()) {
                            obsolete.forEach { ThumbnailCache.remove(it) }
                            var remainingAfterPrune: List<MediaImage> = emptyList()
                            _uiState.update { state ->
                                remainingAfterPrune = state.images.filterNot { it.id in obsolete }
                                state.copy(images = remainingAfterPrune)
                            }
                            if (remainingAfterPrune.isNotEmpty()) {
                                withContext(Dispatchers.IO) { repository.persistSnapshot(remainingAfterPrune) }
                            }
                        }
                        lastGenerationByVolume = currentGen
                        repository.saveGenerationByVolume(currentGen)
                        if (currVersion != null) repository.saveMediaStoreVersion(currVersion)
                    } else {
                        val loaded = repository.loadImages()
                        _uiState.update { it.copy(images = loaded, error = null) }
                        if (currentGen != null) {
                            lastGenerationByVolume = currentGen
                            repository.saveGenerationByVolume(currentGen)
                        }
                        if (currVersion != null) repository.saveMediaStoreVersion(currVersion)
                    }

                    // Phase 1: scan most recently modified directories first and stream new tiles live
                    runCatching {
                        repository.rescanStoragePrioritized(
                            recentOnly = true,
                            forceReverifyExif = false,
                            onBatchScanned = { uris -> applyIncrementalMediaChanges(uris) },
                            onGhostsFound = { ghosts -> removeGhostPathsFromState(ghosts) },
                        )
                    }

                    val afterPhase1Gen = runCatching { repository.getGenerationByVolume() }.getOrNull()
                    if (afterPhase1Gen != null) {
                        lastGenerationByVolume = afterPhase1Gen
                        repository.saveGenerationByVolume(afterPhase1Gen)
                    }
                    if (settingsPreferences.state.value.showHiddenItems) {
                        refreshHiddenMedia()
                    }
                    scheduleExifDateEnrichment(_uiState.value.images)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("GalleryViewModel", "quickRefresh failed", e)
            } finally {
                val thisJob = kotlinx.coroutines.currentCoroutineContext()[Job]
                if (refreshJob === thisJob) {
                    _uiState.update { it.copy(loading = false) }
                    refreshJob = null
                    // Continue scanning older directories silently in the background without redoing Phase 1
                    rescanNewFilesInBackground(skipRecentPhase = true)
                }
            }
        }
    }

    fun refresh(showLoading: Boolean = true, forceClearCache: Boolean = false, rescanStorage: Boolean = false) {
        val current = refreshJob
        if (current?.isActive == true) {
            current.cancel()
        }
        if (forceClearCache) {
            repository.clearVerifiedPathsCache()
        }
        refreshJob = viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(loading = true, error = null) }
            }
            try {
                kotlinx.coroutines.withTimeout(120_000) {
                    // Index loads touch storage and take the repo index lock, so keep them off the main thread.
                    val internalTrash = withContext(Dispatchers.IO) {
                        vaultRepository.recoverOrphanedItems()
                        vaultRepository.loadVaultItems()
                        trashRepository.loadTrashItems()
                    }
                    val trashList = loadCombinedTrash(internalTrash)
                    _uiState.update {
                        it.copy(
                            loading = if (showLoading) true else it.loading,
                            trashed = trashList,
                            error = null
                        )
                    }
                    if (rescanStorage) {
                        runCatching { repository.rescanStorage() }
                    }
                    try {
                        repository.setSnapshotComplete(false)
                        // Capture generation and MediaStore version before scanning so any rows committed mid-scan are picked up on the next check.
                        val scanStartGenerations = withContext(Dispatchers.IO) { repository.getGenerationByVolume() }
                        val scanStartVersion = withContext(Dispatchers.IO) { repository.getCurrentMediaStoreVersion() }
                        val loaded = repository.loadImages()
                        val loadedPaths = loaded.mapTo(HashSet(loaded.size)) { it.path }
                        val loadedIds = loaded.mapTo(HashSet(loaded.size)) { it.id }
                        val currentPending = withContext(Dispatchers.IO) {
                            _uiState.value.images.filter { item ->
                                item.path.isNotBlank() &&
                                item.path !in loadedPaths &&
                                item.id !in loadedIds &&
                                !repository.isMarkedMovedOrDeleted(item.id, item.path) &&
                                java.io.File(item.path).exists()
                            }
                        }
                        val combined = if (currentPending.isNotEmpty()) {
                            (loaded + currentPending)
                                .distinctBy { it.id }
                                .distinctBy { it.path.ifBlank { it.id.toString() } }
                                .sortedWith(
                                compareByDescending<MediaImage> { it.dateTaken }
                                    .thenByDescending { it.dateModified }
                                    .thenByDescending { it.dateAdded }
                                    .thenByDescending { it.id }
                            )
                        } else {
                            loaded
                        }
                        // Don't write the trash list captured before loadImages() back here.
                        // The image scan can take a while and a newer trash refresh may have
                        // landed in the meantime; this stale write made the trash look short.
                        _uiState.update { it.copy(images = combined, loading = false, error = null) }
                        repository.setSnapshotComplete(true)
                        if (scanStartGenerations != null) {
                            lastGenerationByVolume = scanStartGenerations
                            repository.saveGenerationByVolume(scanStartGenerations)
                        }
                        if (scanStartVersion != null) {
                            repository.saveMediaStoreVersion(scanStartVersion)
                        }
                        if (settingsPreferences.state.value.showHiddenItems) {
                            refreshHiddenMedia()
                        }
                        scheduleExifDateEnrichment(combined)
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
                val thisJob = kotlinx.coroutines.currentCoroutineContext()[Job]
                if (refreshJob === thisJob) {
                    _uiState.update { it.copy(loading = false) }
                    refreshJob = null
                    if (pendingCheckAfterRefresh) {
                        pendingCheckAfterRefresh = false
                        checkForChanges()
                    }
                }
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
