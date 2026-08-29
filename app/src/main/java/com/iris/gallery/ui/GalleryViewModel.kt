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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.iris.gallery.data.DuplicateDetector
import com.iris.gallery.data.DuplicateGroup

import com.iris.gallery.data.AlbumRepository
import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
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
                _uiState.value = _uiState.value.copy(trashed = trashedList)
            }
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

    suspend fun moveToTrash(mediaList: List<MediaImage>): List<MediaImage> {
        val result = trashRepository.moveToTrash(mediaList)
        refresh()
        return result
    }

    suspend fun restoreFromTrash(mediaList: List<MediaImage>): List<MediaImage> {
        val result = trashRepository.restoreFromTrash(mediaList)
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
        refresh()
        return result
    }

    suspend fun copyMediaToAlbum(mediaList: List<MediaImage>, targetDir: File, targetAlbumName: String): AlbumOperationResult {
        val result = albumRepository.copyMedia(mediaList, targetDir, targetAlbumName)
        refresh()
        return result
    }

    fun getAlbumDirectory(album: MediaAlbum): File = albumRepository.getAlbumDirectory(album)
    fun createNewAlbumDirectory(albumName: String): File = albumRepository.createNewAlbumDirectory(albumName)

    fun togglePinnedAlbum(id: Long) = libraryPreferences.togglePinnedAlbum(id)
    fun setAlbumCover(albumId: Long, mediaId: Long) = libraryPreferences.setAlbumCover(albumId, mediaId)
    fun setAlbumSort(sort: AlbumSort) = libraryPreferences.setAlbumSort(sort)
    fun setAlbumOrder(order: List<Long>) = libraryPreferences.setAlbumOrder(order)

    fun toggleFavorite(id: Long) {
        val updated = _favorites.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        _favorites.value = updated
        preferences.edit().putStringSet("favorites", updated.mapTo(mutableSetOf()) { it.toString() }).apply()
    }

    fun refresh() {
        viewModelScope.launch {
            vaultRepository.loadVaultItems()
            val trashList = trashRepository.loadTrashItems()
            _uiState.value = _uiState.value.copy(loading = true, trashed = trashList, error = null)
            val media = runCatching { repository.loadImages() }
            media.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(images = it, loading = false, error = null)
                    if (_duplicateState.value.hasScanned) _duplicateState.value = DuplicateScanState()
                },
                onFailure = { _uiState.value = _uiState.value.copy(loading = false,
                    error = it.message ?: "Could not load photos") },
            )
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
