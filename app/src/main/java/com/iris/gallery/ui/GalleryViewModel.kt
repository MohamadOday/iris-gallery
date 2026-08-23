package com.iris.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.LibraryPreferences
import com.iris.gallery.data.LibraryPreferencesState
import com.iris.gallery.data.AlbumSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.iris.gallery.data.DuplicateDetector
import com.iris.gallery.data.DuplicateGroup

data class GalleryUiState(
    val loading: Boolean = false,
    val images: List<MediaImage> = emptyList(),
    val trashed: List<MediaImage> = emptyList(),
    val error: String? = null,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val libraryPreferences = LibraryPreferences(application)
    private val preferences = application.getSharedPreferences("gallery", 0)
    private val _uiState = MutableStateFlow(GalleryUiState(images = repository.loadSnapshot()))
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private val _favorites = MutableStateFlow(
        preferences.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
    )
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()
    val libraryState: StateFlow<LibraryPreferencesState> = libraryPreferences.state
    private val duplicateDetector = DuplicateDetector(application)
    private var duplicateJob: Job? = null
    private val _duplicateState = MutableStateFlow(DuplicateScanState())
    val duplicateState: StateFlow<DuplicateScanState> = _duplicateState.asStateFlow()

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
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val media = runCatching { repository.loadImages() }
            media.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(images = it, loading = false, error = null)
                    if (_duplicateState.value.hasScanned) _duplicateState.value = DuplicateScanState()
                },
                onFailure = { _uiState.value = _uiState.value.copy(loading = false,
                    error = it.message ?: "Could not load photos") },
            )
            if (media.isSuccess) runCatching { repository.loadImages(trashed = true) }
                .onSuccess { trash -> _uiState.value = _uiState.value.copy(trashed = trash) }
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
