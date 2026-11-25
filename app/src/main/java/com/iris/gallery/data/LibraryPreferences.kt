package com.iris.gallery.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AlbumSort { NEWEST, OLDEST, NAME, ITEM_COUNT, CUSTOM }
enum class MediaSort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC }

data class AlbumGroup(
    val id: String,
    val name: String,
    val albumIds: List<Long>,
    val coverMediaId: Long? = null,
)

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
    val albumGroups: List<AlbumGroup> = emptyList(),
    val vaultAlbumSort: AlbumSort = AlbumSort.NEWEST,
    val vaultMediaSort: MediaSort = MediaSort.DATE_DESC,
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

    fun setVaultAlbumSort(sort: AlbumSort) = update { copy(vaultAlbumSort = sort) }

    fun setVaultMediaSort(sort: MediaSort) = update { copy(vaultMediaSort = sort) }

    fun createAlbumGroup(name: String, albumIds: Collection<Long>): AlbumGroup {
        val cleanName = name.trim().ifBlank { "Group" }
        val distinctIds = albumIds.distinct()
        val group = AlbumGroup(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            albumIds = distinctIds,
        )
        update {
            // An album belongs to at most one manual group so it doesn't duplicate on screen.
            val cleanedExisting = albumGroups.mapNotNull { existing ->
                val remaining = existing.albumIds.filterNot { it in distinctIds }
                if (remaining.isEmpty()) null else existing.copy(albumIds = remaining)
            }
            copy(albumGroups = cleanedExisting + group)
        }
        return group
    }

    fun renameAlbumGroup(groupId: String, newName: String) = update {
        val clean = newName.trim()
        if (clean.isBlank()) this
        else copy(albumGroups = albumGroups.map { if (it.id == groupId) it.copy(name = clean) else it })
    }

    fun setAlbumGroupCover(groupId: String, mediaId: Long?) = update {
        copy(albumGroups = albumGroups.map { if (it.id == groupId) it.copy(coverMediaId = mediaId) else it })
    }

    fun deleteAlbumGroup(groupId: String) = update {
        copy(albumGroups = albumGroups.filterNot { it.id == groupId })
    }

    fun setAlbumGroupMembers(groupId: String, albumIds: Collection<Long>) = update {
        val distinctIds = albumIds.distinct()
        if (distinctIds.isEmpty()) {
            copy(albumGroups = albumGroups.filterNot { it.id == groupId })
        } else {
            val updated = albumGroups.mapNotNull { existing ->
                if (existing.id == groupId) {
                    existing.copy(albumIds = distinctIds)
                } else {
                    val remaining = existing.albumIds.filterNot { it in distinctIds }
                    if (remaining.isEmpty()) null else existing.copy(albumIds = remaining)
                }
            }
            copy(albumGroups = updated)
        }
    }

    fun addAlbumToGroup(groupId: String, albumId: Long) = update {
        val updated = albumGroups.mapNotNull { existing ->
            if (existing.id == groupId) {
                existing.copy(albumIds = (existing.albumIds + albumId).distinct())
            } else {
                val remaining = existing.albumIds.filterNot { it == albumId }
                if (remaining.isEmpty()) null else existing.copy(albumIds = remaining)
            }
        }
        copy(albumGroups = updated)
    }

    fun removeAlbumFromGroup(groupId: String, albumId: Long) = update {
        val updated = albumGroups.mapNotNull { existing ->
            if (existing.id == groupId) {
                val remaining = existing.albumIds.filterNot { it == albumId }
                if (remaining.isEmpty()) null else existing.copy(albumIds = remaining)
            } else {
                existing
            }
        }
        copy(albumGroups = updated)
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

    fun getAllCustomTitles(): Map<Long, String> = buildMap {
        for ((key, value) in prefs.all) {
            if (!key.startsWith("title_")) continue
            val mediaId = key.removePrefix("title_").toLongOrNull() ?: continue
            val text = (value as? String)?.trim().orEmpty()
            if (text.isNotEmpty()) put(mediaId, text)
        }
    }

    fun restoreFromBackup(restored: LibraryPreferencesState, customTitles: Map<Long, String>) {
        val merged = restored.copy(lockedMedia = _state.value.lockedMedia)
        _state.value = merged
        val editor = prefs.edit()
        for (key in prefs.all.keys) {
            if (key.startsWith("title_")) {
                editor.remove(key)
            }
        }
        for ((mediaId, title) in customTitles) {
            val clean = title.trim()
            if (clean.isNotEmpty()) {
                editor.putString("title_$mediaId", clean)
            }
        }
        editor.apply()
        write(merged)
    }

    private fun update(transform: LibraryPreferencesState.() -> LibraryPreferencesState) {
        _state.value = _state.value.transform()
        write(_state.value)
    }

    private fun readAlbumGroups(raw: String?): List<AlbumGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { continue }
                    val name = obj.optString("name").ifBlank { continue }
                    val idsArr = obj.optJSONArray("albumIds") ?: continue
                    val ids = buildList {
                        for (j in 0 until idsArr.length()) {
                            add(idsArr.optLong(j))
                        }
                    }.distinct()
                    if (ids.isNotEmpty()) {
                        val coverId = if (obj.has("coverMediaId") && !obj.isNull("coverMediaId")) {
                            obj.optLong("coverMediaId").takeIf { it != 0L }
                        } else null
                        add(AlbumGroup(id = id, name = name, albumIds = ids, coverMediaId = coverId))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAlbumGroups(groups: List<AlbumGroup>): String {
        val arr = JSONArray()
        for (group in groups) {
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("name", group.name)
            if (group.coverMediaId != null) {
                obj.put("coverMediaId", group.coverMediaId)
            }
            val idsArr = JSONArray()
            for (id in group.albumIds) {
                idsArr.put(id)
            }
            obj.put("albumIds", idsArr)
            arr.put(obj)
        }
        return arr.toString()
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
        albumGroups = readAlbumGroups(prefs.getString("album_groups_json", null)),
        vaultAlbumSort = runCatching { AlbumSort.valueOf(prefs.getString("vault_album_sort", null).orEmpty()) }
            .getOrDefault(AlbumSort.NEWEST),
        vaultMediaSort = runCatching { MediaSort.valueOf(prefs.getString("vault_media_sort", null).orEmpty()) }
            .getOrDefault(MediaSort.DATE_DESC),
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
            .putString("album_groups_json", writeAlbumGroups(state.albumGroups))
            .putString("vault_album_sort", state.vaultAlbumSort.name)
            .putString("vault_media_sort", state.vaultMediaSort.name)
            .apply()
    }
}

private fun Set<String>?.toLongSet() = orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)
