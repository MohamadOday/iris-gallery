package com.iris.gallery.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlbumSort { NEWEST, OLDEST, NAME, ITEM_COUNT, CUSTOM }
enum class MediaSort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC }

internal val DEFAULT_LIBRARY_ORDER = listOf(
    "memories",
    "duplicates",
    "locked",
    "trash",
    "editor",
    "formats",
    "folder_view",
    "excluded_folders",
    "rescan"
)

internal fun normalizeLibraryOrder(saved: List<String>): List<String> {
    val validSaved = saved.filter { it in DEFAULT_LIBRARY_ORDER }.distinct()
    val missing = DEFAULT_LIBRARY_ORDER.filterNot { it in validSaved }
    return validSaved + missing
}

data class LibraryPreferencesState(
    val lockedMedia: Set<Long> = emptySet(),
    val pinnedAlbums: Set<Long> = emptySet(),
    val albumCovers: Map<Long, Long> = emptyMap(),
    val albumOrder: List<Long> = emptyList(),
    val libraryOrder: List<String> = DEFAULT_LIBRARY_ORDER,
    val albumSort: AlbumSort = AlbumSort.NEWEST,
    val albumMediaSort: MediaSort = MediaSort.DATE_DESC,
    val albumMediaSortOverrides: Map<Long, MediaSort> = emptyMap(),
    val excludedFolders: Set<String> = emptySet(),
)

/** Small, synchronous preference state. Media bytes and private metadata never live here. */
class LibraryPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("library_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<LibraryPreferencesState> = _state.asStateFlow()

    fun setLocked(ids: Collection<Long>, locked: Boolean) = update {
        copy(lockedMedia = lockedMedia.toMutableSet().apply {
            if (locked) addAll(ids) else removeAll(ids.toSet())
        })
    }

    fun togglePinnedAlbum(id: Long) = update {
        copy(pinnedAlbums = pinnedAlbums.toMutableSet().apply { if (!add(id)) remove(id) })
    }

    fun setAlbumCover(albumId: Long, mediaId: Long) = update {
        copy(albumCovers = albumCovers + (albumId to mediaId))
    }

    fun setAlbumSort(sort: AlbumSort) = update { copy(albumSort = sort) }

    fun setAlbumOrder(order: List<Long>) = update { copy(albumOrder = order.distinct()) }

    fun setLibraryOrder(order: List<String>) = update {
        copy(libraryOrder = normalizeLibraryOrder(order))
    }

    fun setAlbumMediaSort(sort: MediaSort) = update { copy(albumMediaSort = sort) }

    fun setAlbumMediaSortOverride(albumId: Long, sort: MediaSort?) = update {
        if (sort == null) {
            copy(albumMediaSortOverrides = albumMediaSortOverrides - albumId)
        } else {
            copy(albumMediaSortOverrides = albumMediaSortOverrides + (albumId to sort))
        }
    }

    fun addExcludedFolder(folderPath: String) = update {
        val clean = folderPath.trim().removeSuffix("/")
        if (clean.isNotBlank()) copy(excludedFolders = excludedFolders + clean) else this
    }

    fun removeExcludedFolder(folderPath: String) = update {
        val clean = folderPath.trim().removeSuffix("/")
        copy(excludedFolders = excludedFolders - clean)
    }

    fun setExcludedFolders(folders: Set<String>) = update {
        copy(excludedFolders = folders.map { it.trim().removeSuffix("/") }.filter { it.isNotBlank() }.toSet())
    }

    fun getCustomTitle(mediaId: Long): String? = prefs.getString("title_$mediaId", null)?.ifBlank { null }

    fun setCustomTitle(mediaId: Long, title: String?) {
        if (title.isNullOrBlank()) {
            prefs.edit().remove("title_$mediaId").apply()
        } else {
            prefs.edit().putString("title_$mediaId", title.trim()).apply()
        }
    }

    private fun update(transform: LibraryPreferencesState.() -> LibraryPreferencesState) {
        _state.value = _state.value.transform()
        write(_state.value)
    }

    private fun read() = LibraryPreferencesState(
        lockedMedia = prefs.getStringSet("locked", emptySet()).toLongSet(),
        pinnedAlbums = prefs.getStringSet("pinned_albums", emptySet()).toLongSet(),
        albumCovers = prefs.getStringSet("album_covers", emptySet()).orEmpty().mapNotNull { value ->
            val parts = value.split(':', limit = 2)
            val album = parts.getOrNull(0)?.toLongOrNull()
            val media = parts.getOrNull(1)?.toLongOrNull()
            if (album != null && media != null) album to media else null
        }.toMap(),
        albumOrder = prefs.getString("album_order", "").orEmpty().split(',').mapNotNull(String::toLongOrNull),
        libraryOrder = normalizeLibraryOrder(
            prefs.getString("library_order", "").orEmpty().split(',').filter(String::isNotBlank)
        ),
        albumSort = runCatching { AlbumSort.valueOf(prefs.getString("album_sort", null).orEmpty()) }
            .getOrDefault(AlbumSort.NEWEST),
        albumMediaSort = runCatching { MediaSort.valueOf(prefs.getString("album_media_sort", null).orEmpty()) }
            .getOrDefault(MediaSort.DATE_DESC),
        albumMediaSortOverrides = prefs.getStringSet("album_media_sort_overrides", emptySet()).orEmpty().mapNotNull { value ->
            val parts = value.split(':', limit = 2)
            val album = parts.getOrNull(0)?.toLongOrNull()
            val sort = parts.getOrNull(1)?.let { runCatching { MediaSort.valueOf(it) }.getOrNull() }
            if (album != null && sort != null) album to sort else null
        }.toMap(),
        excludedFolders = prefs.getStringSet("excluded_folders", emptySet()).orEmpty(),
    )

    private fun write(state: LibraryPreferencesState) {
        prefs.edit()
            .putStringSet("locked", state.lockedMedia.mapTo(mutableSetOf(), Long::toString))
            .putStringSet("pinned_albums", state.pinnedAlbums.mapTo(mutableSetOf(), Long::toString))
            .putStringSet("album_covers", state.albumCovers.mapTo(mutableSetOf()) { "${it.key}:${it.value}" })
            .putString("album_order", state.albumOrder.joinToString(","))
            .putString("library_order", state.libraryOrder.joinToString(","))
            .putString("album_sort", state.albumSort.name)
            .putString("album_media_sort", state.albumMediaSort.name)
            .putStringSet("album_media_sort_overrides", state.albumMediaSortOverrides.mapTo(mutableSetOf()) { "${it.key}:${it.value.name}" })
            .putStringSet("excluded_folders", state.excludedFolders)
            .apply()
    }
}

private fun Set<String>?.toLongSet() = orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)
