@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iris.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.sp
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.NaturalOrderComparator

data class MediaAlbum(
    val id: Long,
    val name: String,
    val cover: MediaImage,
    val images: List<MediaImage>,
    val isSdCard: Boolean = false,
    val storageLabel: String = "",
)

enum class AlbumCategory {
    ALL,
    CAMERA_SYSTEM,
    APPS,
    VIDEOS,
}

private val SYSTEM_FOLDER_NAMES = setOf(
    "camera",
    "dcim",
    "100andro",
    "screenshots",
    "screen recordings",
    "screen recorder",
    "screencapture",
    "download",
    "downloads",
    "bluetooth",
    "raw"
)

private val KNOWN_APP_KEYWORDS = listOf(
    "whatsapp", "telegram", "instagram", "reddit", "twitter", " x ",
    "snapchat", "facebook", "messenger", "discord", "tiktok", "signal",
    "viber", "wechat", "line", "pinterest", "tumblr", "vsco", "snapseed",
    "lightroom", "canva", "capcut", "inshot", "youcut", "picsart",
    "threads", "bluesky", "mastodon", "slack", "skype", "zoom"
)

private fun isKnownApp(samplePath: String, name: String): Boolean {
    if (samplePath.contains("/android/media/") || samplePath.contains("/android/data/")) return true
    return KNOWN_APP_KEYWORDS.any { name.contains(it) || samplePath.contains("/$it") }
}

fun isCameraOrSystemAlbum(album: MediaAlbum): Boolean {
    val samplePath = album.images.firstOrNull { it.path.isNotBlank() }?.path.orEmpty().lowercase()
    val name = album.name.lowercase().trim()

    if (isKnownApp(samplePath, name)) {
        return false
    }

    if (name in SYSTEM_FOLDER_NAMES) {
        return true
    }

    if (samplePath.contains("/dcim/camera") ||
        samplePath.contains("/dcim/100andro") ||
        samplePath.contains("/pictures/screenshots") ||
        samplePath.contains("/dcim/screenshots") ||
        samplePath.contains("/movies/screen recordings") ||
        samplePath.contains("/movies/screen recorder") ||
        samplePath.contains("/download/") ||
        samplePath.contains("/downloads/") ||
        samplePath.contains("/bluetooth/")
    ) {
        return true
    }

    if (samplePath.endsWith("/dcim") || samplePath.endsWith("/pictures") || samplePath.endsWith("/movies")) {
        return true
    }

    return false
}

fun isAppAlbum(album: MediaAlbum): Boolean {
    return !isCameraOrSystemAlbum(album)
}

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
    isEditingOrder: Boolean = false,
    onTogglePinned: (Long) -> Unit = {},
    onSortChanged: (AlbumSort) -> Unit = {},
    onOrderChanged: (List<Long>) -> Unit = {},
    onLockAlbum: ((MediaAlbum) -> Unit)? = null,
    onExcludeFolder: ((MediaAlbum) -> Unit)? = null,
    onOpen: (MediaAlbum) -> Unit,
) {
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AlbumCategory.ALL) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedAlbumForOptions by remember { mutableStateOf<MediaAlbum?>(null) }

    val albums = remember(images, pinned, covers, sort, customOrder) {
        val base = images.groupBy { it.bucketId }.map { (id, media) ->
            val samplePath = media.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
            val isSd = samplePath.isNotBlank() && !samplePath.startsWith("/storage/emulated/0") && !samplePath.startsWith("/data/")
            MediaAlbum(
                id = id,
                name = media.first().bucketName,
                cover = media.firstOrNull { it.id == covers[id] } ?: media.first(),
                images = media,
                isSdCard = isSd,
                storageLabel = if (isSd) "SD Card" else ""
            )
        }
        val orderIndex = customOrder.withIndex().associate { it.value to it.index }
        base.sortedWith(compareByDescending<MediaAlbum> { it.id in pinned }.thenComparator { a, b ->
            when (sort) {
                AlbumSort.NEWEST -> b.cover.dateTaken.compareTo(a.cover.dateTaken)
                AlbumSort.OLDEST -> a.cover.dateTaken.compareTo(b.cover.dateTaken)
                AlbumSort.NAME -> NaturalOrderComparator.compare(a.name, b.name)
                AlbumSort.ITEM_COUNT -> b.images.size.compareTo(a.images.size)
                AlbumSort.CUSTOM -> (orderIndex[a.id] ?: Int.MAX_VALUE).compareTo(orderIndex[b.id] ?: Int.MAX_VALUE)
            }
        })
    }
    val effectiveOrder = remember(albums, customOrder) {
        customOrder.filter { id -> albums.any { it.id == id } } + albums.map { it.id }.filterNot(customOrder::contains)
    }

    val categorizedAlbums = remember(albums, selectedCategory) {
        when (selectedCategory) {
            AlbumCategory.ALL -> albums
            AlbumCategory.CAMERA_SYSTEM -> albums.filter { isCameraOrSystemAlbum(it) }
            AlbumCategory.APPS -> albums.filter { isAppAlbum(it) }
            AlbumCategory.VIDEOS -> albums.filter { it.images.any { img -> img.isVideo } }
        }
    }

    val filteredAlbums = remember(categorizedAlbums, searchQuery) {
        if (searchQuery.isBlank()) categorizedAlbums
        else categorizedAlbums.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val spacingDp = gridSpacing.dp.dp
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp)
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
                                val nextSize = (currentCellSize.value * zoom).coerceIn(48f, 420f)
                                currentOnCellSizeChange?.invoke(nextSize.dp)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val actualColumns = remember(screenWidthDp, cellSize, gridSpacing, density, startPadding, endPadding) {
            GridCalculations.calculateAlbumColumns(
                screenWidthDp = screenWidthDp,
                cellSizeDp = cellSize.value,
                gridSpacingDp = gridSpacing.dp.toFloat(),
                density = density,
                horizontalPaddingDp = (startPadding + endPadding).value / 2f
            )
        }
        val targetThumbnailPx = remember(actualColumns) { getThumbnailTargetSizePx(actualColumns) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(cellSize),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp + bottomPadding),
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
                        placeholder = { Text(stringResource(com.iris.gallery.R.string.search_albums_placeholder, categorizedAlbums.size)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(com.iris.gallery.R.string.clear_search))
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedCategory == AlbumCategory.ALL,
                                onClick = { selectedCategory = AlbumCategory.ALL },
                                label = { Text(stringResource(com.iris.gallery.R.string.filter_all_albums)) }
                            )
                            FilterChip(
                                selected = selectedCategory == AlbumCategory.CAMERA_SYSTEM,
                                onClick = { selectedCategory = AlbumCategory.CAMERA_SYSTEM },
                                label = { Text(stringResource(com.iris.gallery.R.string.filter_camera_system)) }
                            )
                            FilterChip(
                                selected = selectedCategory == AlbumCategory.APPS,
                                onClick = { selectedCategory = AlbumCategory.APPS },
                                label = { Text(stringResource(com.iris.gallery.R.string.filter_apps)) }
                            )
                            FilterChip(
                                selected = selectedCategory == AlbumCategory.VIDEOS,
                                onClick = { selectedCategory = AlbumCategory.VIDEOS },
                                label = { Text(stringResource(com.iris.gallery.R.string.filter_videos)) }
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        val currentSortLabelRes = when (sort) {
                            AlbumSort.NEWEST -> com.iris.gallery.R.string.sort_recent
                            AlbumSort.NAME -> com.iris.gallery.R.string.sort_name
                            AlbumSort.ITEM_COUNT -> com.iris.gallery.R.string.sort_size
                            AlbumSort.CUSTOM -> com.iris.gallery.R.string.sort_custom
                            AlbumSort.OLDEST -> com.iris.gallery.R.string.sort_recent
                        }

                        Box {
                            AssistChip(
                                modifier = Modifier.widthIn(max = 145.dp),
                                onClick = { sortMenuExpanded = true },
                                label = {
                                    Text(
                                        text = stringResource(currentSortLabelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                listOf(
                                    AlbumSort.NEWEST to com.iris.gallery.R.string.sort_recent,
                                    AlbumSort.NAME to com.iris.gallery.R.string.sort_name,
                                    AlbumSort.ITEM_COUNT to com.iris.gallery.R.string.sort_size,
                                    AlbumSort.CUSTOM to com.iris.gallery.R.string.sort_custom
                                ).forEach { (value, strRes) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(strRes)) },
                                        trailingIcon = if (sort == value) {
                                            {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            sortMenuExpanded = false
                                            if (value == AlbumSort.CUSTOM && customOrder.isEmpty()) {
                                                onOrderChanged(albums.map { it.id })
                                            }
                                            onSortChanged(value)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredAlbums.isEmpty()) {
                item(key = "empty-search", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (searchQuery.isNotBlank()) {
                                stringResource(com.iris.gallery.R.string.empty_search_albums, searchQuery)
                            } else {
                                stringResource(com.iris.gallery.R.string.empty_photos)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank()) {
                            TextButton(onClick = { searchQuery = "" }) {
                                Text(stringResource(com.iris.gallery.R.string.clear_search))
                            }
                        }
                    }
                }
            }

            items(filteredAlbums, key = { it.id }) { album ->
                val isPinned = album.id in pinned
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .combinedClickable(
                            onClick = { onOpen(album) },
                            onLongClick = { selectedAlbumForOptions = album }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        Card(
                            shape = RoundedCornerShape(cornerStyle.dp.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            MediaThumbnail(
                                album.cover,
                                Modifier.fillMaxSize(),
                                targetSizePx = targetThumbnailPx,
                            )
                        }

                        // Floating Pin Badge on top-right of pinned album image
                        if (sort != AlbumSort.CUSTOM && isPinned) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clickable { onTogglePinned(album.id) },
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.album_unpin),
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        } else if (sort == AlbumSort.CUSTOM && isEditingOrder) {
                            val index = effectiveOrder.indexOf(album.id)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val buttonColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                                val buttonContentColor = MaterialTheme.colorScheme.onSurface
                                if (index > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = buttonColor,
                                        contentColor = buttonContentColor,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(28.dp).clickable {
                                            val next = effectiveOrder.toMutableList()
                                            next.add(0, next.removeAt(index))
                                            onOrderChanged(next)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.VerticalAlignTop, contentDescription = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_move_to_top), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = buttonColor,
                                        contentColor = buttonContentColor,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(28.dp).clickable {
                                            val next = effectiveOrder.toMutableList()
                                            next.add(index - 1, next.removeAt(index))
                                            onOrderChanged(next)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                                if (index in 0 until effectiveOrder.lastIndex) {
                                    Surface(
                                        shape = CircleShape,
                                        color = buttonColor,
                                        contentColor = buttonContentColor,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(28.dp).clickable {
                                            val next = effectiveOrder.toMutableList()
                                            next.add(index + 1, next.removeAt(index))
                                            onOrderChanged(next)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.ArrowDownward, null, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = buttonColor,
                                        contentColor = buttonContentColor,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(28.dp).clickable {
                                            val next = effectiveOrder.toMutableList()
                                            next.add(next.removeAt(index))
                                            onOrderChanged(next)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_move_to_bottom), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Full-width album text container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            album.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (album.isSdCard) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.storage_sd_card),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (showCount) {
                                Text(
                                    androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.album_items_count, album.images.size),
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

        if (selectedAlbumForOptions != null) {
            val album = selectedAlbumForOptions!!
            val isPinned = album.id in pinned
            ModalBottomSheet(
                onDismissRequest = { selectedAlbumForOptions = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with album thumbnail & info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            MediaThumbnail(
                                album.cover,
                                modifier = Modifier.fillMaxSize(),
                                targetSizePx = 128
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.album_items_count, album.images.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Lock Album Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAlbumForOptions = null
                                onLockAlbum?.invoke(album)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_lock_album),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.lock_album_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Exclude Folder Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAlbumForOptions = null
                                onExcludeFolder?.invoke(album)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.FolderOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_exclude_album),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.exclude_album_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Pin / Unpin Album Option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAlbumForOptions = null
                                onTogglePinned(album.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(
                                        if (isPinned) com.iris.gallery.R.string.album_unpin else com.iris.gallery.R.string.album_pin
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
