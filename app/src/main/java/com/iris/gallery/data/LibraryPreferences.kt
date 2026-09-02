package com.iris.gallery.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlbumSort { NEWEST, OLDEST, NAME, ITEM_COUNT, CUSTOM }
enum class MediaSort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC }

data class LibraryPreferencesState(
    val lockedMedia: Set<Long> = emptySet(),
    val pinnedAlbums: Set<Long> = emptySet(),
    val albumCovers: Map<Long, Long> = emptyMap(),
    val albumOrder: List<Long> = emptyList(),
    val albumSort: AlbumSort = AlbumSort.NEWEST,
    val albumMediaSort: MediaSort = MediaSort.DATE_DESC,
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

    fun setAlbumMediaSort(sort: MediaSort) = update { copy(albumMediaSort = sort) }

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
        albumSort = runCatching { AlbumSort.valueOf(prefs.getString("album_sort", null).orEmpty()) }
            .getOrDefault(AlbumSort.NEWEST),
        albumMediaSort = runCatching { MediaSort.valueOf(prefs.getString("album_media_sort", null).orEmpty()) }
            .getOrDefault(MediaSort.DATE_DESC),
    )

    private fun write(state: LibraryPreferencesState) {
        prefs.edit()
            .putStringSet("locked", state.lockedMedia.mapTo(mutableSetOf(), Long::toString))
            .putStringSet("pinned_albums", state.pinnedAlbums.mapTo(mutableSetOf(), Long::toString))
            .putStringSet("album_covers", state.albumCovers.mapTo(mutableSetOf()) { "${it.key}:${it.value}" })
            .putString("album_order", state.albumOrder.joinToString(","))
            .putString("album_sort", state.albumSort.name)
            .putString("album_media_sort", state.albumMediaSort.name)
            .apply()
    }
}

private fun Set<String>?.toLongSet() = orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)
