@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iris.gallery.R
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.AlbumsViewMode
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaSort
import com.iris.gallery.data.NaturalOrderComparator
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.TimelineDateSource
import com.iris.gallery.data.timelineComparator
import com.iris.gallery.data.timelineTimestamp

internal fun sortVaultMediaList(
    list: List<MediaImage>,
    sort: MediaSort,
    dateSource: TimelineDateSource = TimelineDateSource.DATE_TAKEN,
): List<MediaImage> = when (sort) {
    MediaSort.DATE_DESC -> list.sortedWith(timelineComparator(dateSource))
    MediaSort.DATE_ASC -> list.sortedWith(
        compareBy<MediaImage> { it.timelineTimestamp(dateSource) }
            .thenBy { it.dateModified }
            .thenBy { it.dateAdded }
            .thenBy { it.id }
    )
    MediaSort.NAME_ASC -> list.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
    MediaSort.NAME_DESC -> list.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
    MediaSort.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
    MediaSort.SIZE_ASC -> list.sortedBy { it.sizeBytes }
}

internal fun buildVaultAlbums(
    lockedMedia: List<MediaImage>,
    albumSort: AlbumSort,
    dateSource: TimelineDateSource = TimelineDateSource.DATE_TAKEN,
): List<MediaAlbum> {
    val albums = lockedMedia.groupBy { it.bucketName.ifBlank { "Locked" } }.map { (bucketName, list) ->
        val sorted = sortVaultMediaList(list, MediaSort.DATE_DESC, dateSource)
        MediaAlbum(
            id = bucketName.hashCode().toLong(),
            name = bucketName,
            cover = sorted.first(),
            images = sorted,
        )
    }
    return when (albumSort) {
        AlbumSort.NEWEST, AlbumSort.CUSTOM -> albums.sortedByDescending { it.cover.timelineTimestamp(dateSource) }
        AlbumSort.OLDEST -> albums.sortedBy { it.cover.timelineTimestamp(dateSource) }
        AlbumSort.NAME -> albums.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
        AlbumSort.ITEM_COUNT -> albums.sortedWith(compareByDescending<MediaAlbum> { it.images.size }.thenBy { it.name.lowercase() })
    }
}

@Composable
internal fun VaultContent(
    lockedAuthorized: Boolean,
    lockedMedia: List<MediaImage>,
    vaultTab: Int,
    onVaultTabChange: (Int) -> Unit,
    selectedLockedAlbum: String?,
    onSelectLockedAlbum: (String?) -> Unit,
    searchQuery: String,
    onClearSearch: () -> Unit,
    vaultAlbumSort: AlbumSort,
    vaultMediaSort: MediaSort,
    settings: SettingsState,
    padding: PaddingValues,
    gridState: LazyGridState,
    cellSize: Dp,
    onCellSizeChange: (Dp) -> Unit,
    selectedIds: Set<Long>,
    favorites: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onSetSelection: (Long, Boolean) -> Unit,
    onSetBatchSelection: (Collection<Long>, Boolean) -> Unit,
    onSetDateSelection: (List<Long>, Boolean) -> Unit,
    onRequestUnlock: () -> Unit,
    onRegisterBoundsResolver: (((Long) -> Rect?) -> Unit)? = null,
    onOpenMedia: (MediaImage, List<MediaImage>) -> Unit,
) {
    if (!lockedAuthorized) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.vault_locked_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRequestUnlock) {
                    Text(stringResource(R.string.action_unlock_folder))
                }
            }
        }
        return
    }

    if (lockedMedia.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.empty_locked),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val vaultAlbums = remember(lockedMedia, vaultAlbumSort, settings.timelineDateSource) {
        buildVaultAlbums(lockedMedia, vaultAlbumSort, settings.timelineDateSource)
    }
    val filteredVaultAlbums = remember(vaultAlbums, searchQuery) {
        if (searchQuery.isBlank()) vaultAlbums
        else vaultAlbums.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val allSortedMedia = remember(lockedMedia, vaultMediaSort, searchQuery, settings.timelineDateSource) {
        val sorted = sortVaultMediaList(lockedMedia, vaultMediaSort, settings.timelineDateSource)
        filterMediaList(sorted, searchQuery)
    }
    val currentAlbumMedia = remember(selectedLockedAlbum, lockedMedia, vaultMediaSort, searchQuery, settings.timelineDateSource) {
        if (selectedLockedAlbum == null) emptyList()
        else {
            val matching = lockedMedia.filter { it.bucketName.ifBlank { "Locked" } == selectedLockedAlbum }
            filterMediaList(sortVaultMediaList(matching, vaultMediaSort, settings.timelineDateSource), searchQuery)
        }
    }

    val showTimelineForSort = settings.showTimelineHeaders &&
        (vaultMediaSort == MediaSort.DATE_DESC || vaultMediaSort == MediaSort.DATE_ASC)

    if (selectedLockedAlbum != null) {
        if (currentAlbumMedia.isEmpty() && searchQuery.isNotBlank()) {
            VaultEmptySearchState(query = searchQuery, padding = padding, onClearSearch = onClearSearch)
        } else {
            PhotoGrid(
                images = currentAlbumMedia,
                padding = padding,
                gridState = gridState,
                cellSize = cellSize,
                onCellSizeChange = onCellSizeChange,
                layoutMode = settings.photoGridLayout,
                animateGifThumbnails = settings.animateGifThumbnails,
                showTimeline = showTimelineForSort,
                timelineDateSource = settings.timelineDateSource,
                timelineDateFormat = settings.timelineDateFormat,
                customTimelineDateFormat = settings.customTimelineDateFormat,
                useRelativeDates = settings.useRelativeDates,
                showDayOfWeek = settings.showDayOfWeek,
                abbreviateDayOfWeek = settings.abbreviateDayOfWeek,
                smartYearHiding = settings.smartYearHiding,
                cornerStyle = settings.cornerStyle,
                gridSpacing = settings.gridSpacing,
                showVideoDuration = settings.showVideoDurationBadge,
                showFormatBadge = settings.showMediaFormatBadge,
                selectedIds = selectedIds,
                favorites = favorites,
                onToggleSelection = onToggleSelection,
                onSetSelection = onSetSelection,
                onSetBatchSelection = onSetBatchSelection,
                onSetDateSelection = onSetDateSelection,
                onRegisterBoundsResolver = onRegisterBoundsResolver,
                onOpen = { media ->
                    if (selectedIds.isNotEmpty()) onToggleSelection(media.id)
                    else onOpenMedia(media, currentAlbumMedia)
                },
            )
        }
        return
    }

    val layoutDirection = LocalLayoutDirection.current
    val startPad = padding.calculateStartPadding(layoutDirection)
    val endPad = padding.calculateEndPadding(layoutDirection)
    val topPad = padding.calculateTopPadding()
    val bottomPad = padding.calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad)
    ) {
        TabRow(selectedTabIndex = vaultTab) {
            Tab(
                selected = vaultTab == 0,
                onClick = { onVaultTabChange(0) },
                text = { Text(stringResource(R.string.vault_tab_albums) + " (${vaultAlbums.size})") }
            )
            Tab(
                selected = vaultTab == 1,
                onClick = { onVaultTabChange(1) },
                text = { Text(stringResource(R.string.vault_tab_all_photos) + " (${lockedMedia.size})") }
            )
        }

        if (vaultTab == 0) {
            if (filteredVaultAlbums.isEmpty() && searchQuery.isNotBlank()) {
                VaultEmptySearchState(
                    query = searchQuery,
                    padding = PaddingValues(bottom = bottomPad),
                    onClearSearch = onClearSearch
                )
            } else if (settings.albumsViewMode == AlbumsViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp + startPad,
                        top = 10.dp,
                        end = 12.dp + endPad,
                        bottom = 16.dp + bottomPad
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVaultAlbums, key = { it.name }) { album ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = { onSelectLockedAlbum(album.name) })
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(settings.cornerStyle.dp.coerceAtMost(14).dp),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    MediaThumbnail(album.cover, Modifier.fillMaxSize(), targetSizePx = 144)
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        album.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = formatAlbumSizeBytes(album.images.sumOf { it.sizeBytes })
                                    Text(
                                        "${stringResource(R.string.album_items_count, album.images.size)} · $sizeLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(settings.albumGridSize.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp + startPad,
                        top = 12.dp,
                        end = 12.dp + endPad,
                        bottom = 16.dp + bottomPad
                    ),
                    horizontalArrangement = Arrangement.spacedBy((settings.gridSpacing.dp + 8).dp),
                    verticalArrangement = Arrangement.spacedBy((settings.gridSpacing.dp + 12).dp),
                ) {
                    items(filteredVaultAlbums, key = { it.name }) { album ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { onSelectLockedAlbum(album.name) })
                        ) {
                            Card(
                                shape = RoundedCornerShape(settings.cornerStyle.dp.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                MediaThumbnail(album.cover, Modifier.fillMaxSize(), targetSizePx = 256)
                            }
                            if (settings.showAlbumLabels || settings.showAlbumCount) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (settings.showAlbumLabels) {
                                        Text(
                                            album.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (settings.showAlbumCount) {
                                        Text(
                                            stringResource(R.string.album_items_count, album.images.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (allSortedMedia.isEmpty() && searchQuery.isNotBlank()) {
                VaultEmptySearchState(
                    query = searchQuery,
                    padding = PaddingValues(bottom = bottomPad),
                    onClearSearch = onClearSearch
                )
            } else {
                PhotoGrid(
                    images = allSortedMedia,
                    padding = PaddingValues(start = startPad, end = endPad, bottom = bottomPad),
                    gridState = gridState,
                    cellSize = cellSize,
                    onCellSizeChange = onCellSizeChange,
                    layoutMode = settings.photoGridLayout,
                    animateGifThumbnails = settings.animateGifThumbnails,
                    showTimeline = showTimelineForSort,
                    timelineDateSource = settings.timelineDateSource,
                    timelineDateFormat = settings.timelineDateFormat,
                    customTimelineDateFormat = settings.customTimelineDateFormat,
                    useRelativeDates = settings.useRelativeDates,
                    showDayOfWeek = settings.showDayOfWeek,
                    abbreviateDayOfWeek = settings.abbreviateDayOfWeek,
                    smartYearHiding = settings.smartYearHiding,
                    cornerStyle = settings.cornerStyle,
                    gridSpacing = settings.gridSpacing,
                    showVideoDuration = settings.showVideoDurationBadge,
                    showFormatBadge = settings.showMediaFormatBadge,
                    selectedIds = selectedIds,
                    favorites = favorites,
                    onToggleSelection = onToggleSelection,
                    onSetSelection = onSetSelection,
                    onSetBatchSelection = onSetBatchSelection,
                    onSetDateSelection = onSetDateSelection,
                    onRegisterBoundsResolver = onRegisterBoundsResolver,
                    onOpen = { media ->
                        if (selectedIds.isNotEmpty()) onToggleSelection(media.id)
                        else onOpenMedia(media, allSortedMedia)
                    },
                )
            }
        }
    }
}

@Composable
private fun VaultEmptySearchState(
    query: String,
    padding: PaddingValues,
    onClearSearch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_search_files, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onClearSearch) {
                Text(stringResource(R.string.clear_search))
            }
        }
    }
}
