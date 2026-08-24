package com.iris.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.MediaImage

data class MediaAlbum(val id: Long, val name: String, val cover: MediaImage, val images: List<MediaImage>)

@Composable
fun AlbumsGrid(
    images: List<MediaImage>,
    padding: PaddingValues,
    state: LazyGridState,
    cellSize: Dp = 156.dp,
    onCellSizeChange: ((Dp) -> Unit)? = null,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    gridSpacing: GridSpacing = GridSpacing.STANDARD,
    showCount: Boolean = true,
    pinned: Set<Long> = emptySet(),
    covers: Map<Long, Long> = emptyMap(),
    sort: AlbumSort = AlbumSort.NEWEST,
    customOrder: List<Long> = emptyList(),
    onTogglePinned: (Long) -> Unit = {},
    onSortChanged: (AlbumSort) -> Unit = {},
    onOrderChanged: (List<Long>) -> Unit = {},
    onOpen: (MediaAlbum) -> Unit,
) {
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    var searchQuery by remember { mutableStateOf("") }

    val albums = remember(images, pinned, covers, sort, customOrder) {
        val base = images.groupBy { it.bucketId }.map { (id, media) ->
            MediaAlbum(
                id,
                media.first().bucketName,
                media.firstOrNull { it.id == covers[id] } ?: media.first(),
                media
            )
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

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums
        else albums.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val spacingDp = gridSpacing.dp.dp

    Box(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .pointerInput(Unit) {
                if (currentOnCellSizeChange == null) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val downChanges = event.changes.filter { it.pressed }
                        if (downChanges.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (kotlin.math.abs(zoom - 1f) > 0.001f) {
                                val nextSize = (currentCellSize.value * zoom).coerceIn(70f, 340f)
                                currentOnCellSizeChange?.invoke(nextSize.dp)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(cellSize),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(spacingDp + 8.dp),
            verticalArrangement = Arrangement.spacedBy(spacingDp + 12.dp),
        ) {
            item(key = "album-search-and-sort", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search albums input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search ${albums.size} albums…") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            AlbumSort.NEWEST to "Recent",
                            AlbumSort.NAME to "Name",
                            AlbumSort.ITEM_COUNT to "Size",
                            AlbumSort.CUSTOM to "Custom"
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = sort == value,
                                onClick = {
                                    if (value == AlbumSort.CUSTOM && customOrder.isEmpty()) onOrderChanged(albums.map { it.id })
                                    onSortChanged(value)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            if (filteredAlbums.isEmpty() && searchQuery.isNotBlank()) {
                item(key = "empty-search", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "No albums matching \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("Clear search")
                        }
                    }
                }
            }

            items(filteredAlbums, key = { it.id }) { album ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable { onOpen(album) }
                ) {
                    Card(
                        shape = RoundedCornerShape(cornerStyle.dp.dp),
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    ) {
                        MediaThumbnail(album.cover, Modifier.fillMaxSize())
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                album.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showCount) {
                                Text(
                                    "${album.images.size} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (sort == AlbumSort.CUSTOM) {
                            val index = effectiveOrder.indexOf(album.id)
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    val next = effectiveOrder.toMutableList()
                                    next.add(index - 1, next.removeAt(index))
                                    onOrderChanged(next)
                                }
                            ) {
                                Icon(Icons.Outlined.ArrowUpward, "Move up")
                            }
                            IconButton(
                                enabled = index in 0 until effectiveOrder.lastIndex,
                                onClick = {
                                    val next = effectiveOrder.toMutableList()
                                    next.add(index + 1, next.removeAt(index))
                                    onOrderChanged(next)
                                }
                            ) {
                                Icon(Icons.Outlined.ArrowDownward, "Move down")
                            }
                        } else {
                            IconButton(onClick = { onTogglePinned(album.id) }) {
                                Icon(
                                    if (album.id in pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    if (album.id in pinned) "Unpin album" else "Pin album"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
