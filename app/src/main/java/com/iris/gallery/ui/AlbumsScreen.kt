package com.iris.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.MediaImage

data class MediaAlbum(val id: Long, val name: String, val cover: MediaImage, val images: List<MediaImage>)

@Composable
fun AlbumsGrid(
    images: List<MediaImage>,
    padding: PaddingValues,
    state: LazyGridState,
    pinned: Set<Long> = emptySet(),
    covers: Map<Long, Long> = emptyMap(),
    sort: AlbumSort = AlbumSort.NEWEST,
    customOrder: List<Long> = emptyList(),
    onTogglePinned: (Long) -> Unit = {},
    onSortChanged: (AlbumSort) -> Unit = {},
    onOrderChanged: (List<Long>) -> Unit = {},
    onOpen: (MediaAlbum) -> Unit,
) {
    val albums = remember(images, pinned, covers, sort, customOrder) {
        val base = images.groupBy { it.bucketId }.map { (id, media) ->
            MediaAlbum(id, media.first().bucketName,
                media.firstOrNull { it.id == covers[id] } ?: media.first(), media)
        }
        val orderIndex = customOrder.withIndex().associate { it.value to it.index }
        base.sortedWith(compareByDescending<MediaAlbum> { it.id in pinned }.thenComparator { a, b ->
            when (sort) {
                AlbumSort.NEWEST -> b.cover.dateTaken.compareTo(a.cover.dateTaken)
                AlbumSort.OLDEST -> a.cover.dateTaken.compareTo(b.cover.dateTaken)
                AlbumSort.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                AlbumSort.ITEM_COUNT -> b.images.size.compareTo(a.images.size)
                AlbumSort.CUSTOM -> (orderIndex[a.id] ?: Int.MAX_VALUE).compareTo(orderIndex[b.id] ?: Int.MAX_VALUE)
            }
        })
    }
    val effectiveOrder = remember(albums, customOrder) {
        customOrder.filter { id -> albums.any { it.id == id } } + albums.map { it.id }.filterNot(customOrder::contains)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp), state = state,
        modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "album-sort", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort albums", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AlbumSort.NEWEST to "Recent", AlbumSort.NAME to "Name",
                        AlbumSort.ITEM_COUNT to "Size", AlbumSort.CUSTOM to "Custom").forEach { (value, label) ->
                        FilterChip(selected = sort == value, onClick = {
                            if (value == AlbumSort.CUSTOM && customOrder.isEmpty()) onOrderChanged(albums.map { it.id })
                            onSortChanged(value)
                        }, label = { Text(label) })
                    }
                }
            }
        }
        items(albums, key = { it.id }) { album ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(album) }) {
                Card(Modifier.fillMaxWidth().aspectRatio(1f)) { MediaThumbnail(album.cover, Modifier.fillMaxSize()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(album.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${album.images.size} items", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (sort == AlbumSort.CUSTOM) {
                        val index = effectiveOrder.indexOf(album.id)
                        IconButton(enabled = index > 0, onClick = {
                            val next = effectiveOrder.toMutableList(); next.add(index - 1, next.removeAt(index)); onOrderChanged(next)
                        }) { Icon(Icons.Outlined.ArrowUpward, "Move up") }
                        IconButton(enabled = index in 0 until effectiveOrder.lastIndex, onClick = {
                            val next = effectiveOrder.toMutableList(); next.add(index + 1, next.removeAt(index)); onOrderChanged(next)
                        }) { Icon(Icons.Outlined.ArrowDownward, "Move down") }
                    } else IconButton(onClick = { onTogglePinned(album.id) }) {
                        Icon(if (album.id in pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            if (album.id in pinned) "Unpin album" else "Pin album")
                    }
                }
            }
        }
    }
}
