@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iris.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.iris.gallery.R
import com.iris.gallery.data.AlbumGroup
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.AlbumsViewMode
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.NaturalOrderComparator
import kotlinx.coroutines.launch
import java.util.Locale

data class MediaAlbum(
    val id: Long,
    val name: String,
    val cover: MediaImage,
    val images: List<MediaImage>,
    val isSdCard: Boolean = false,
    val storageLabel: String = "",
    val folderPath: String = "",
)

data class FolderCluster(
    val id: String,
    val name: String,
    val folderPath: String,
    val directAlbum: MediaAlbum?,
    val subAlbums: List<MediaAlbum>,
    val allImages: List<MediaImage>,
    val cover: MediaImage,
    val isSdCard: Boolean,
)

sealed interface AlbumDisplayItem {
    val stableKey: String
    val displayName: String
    val latestDate: Long
    val totalItemCount: Int
    val totalSizeBytes: Long
    val isPinned: Boolean

    data class SingleAlbum(
        val album: MediaAlbum,
        override val isPinned: Boolean,
    ) : AlbumDisplayItem {
        override val stableKey: String = "album:${album.id}"
        override val displayName: String = album.name
        override val latestDate: Long = album.cover.dateTaken
        override val totalItemCount: Int = album.images.size
        override val totalSizeBytes: Long = album.images.sumOf { it.sizeBytes + (it.pairedRaw?.sizeBytes ?: 0L) }
    }

    data class SubfolderGroup(
        val cluster: FolderCluster,
        override val isPinned: Boolean,
    ) : AlbumDisplayItem {
        override val stableKey: String = "cluster:${cluster.id}"
        override val displayName: String = cluster.name
        override val latestDate: Long = cluster.cover.dateTaken
        override val totalItemCount: Int = cluster.allImages.size
        override val totalSizeBytes: Long = cluster.allImages.sumOf { it.sizeBytes + (it.pairedRaw?.sizeBytes ?: 0L) }
    }

    data class ManualGroup(
        val group: AlbumGroup,
        val albums: List<MediaAlbum>,
        val allImages: List<MediaImage>,
    ) : AlbumDisplayItem {
        override val stableKey: String = "group:${group.id}"
        override val displayName: String = group.name
        override val latestDate: Long = allImages.maxOfOrNull { it.dateTaken } ?: 0L
        override val totalItemCount: Int = allImages.size
        override val totalSizeBytes: Long = allImages.sumOf { it.sizeBytes + (it.pairedRaw?.sizeBytes ?: 0L) }
        override val isPinned: Boolean = false
    }
}

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

internal fun formatAlbumSizeBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1000.0 && unitIndex < units.lastIndex) {
        value /= 1000.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${bytes} B"
    } else if (value >= 100.0) {
        String.format(Locale.getDefault(), "%.0f %s", value, units[unitIndex])
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}

internal fun extractStorageRootAndSegments(filePath: String): Pair<String, List<String>>? {
    if (filePath.isBlank()) return null
    val dirPath = filePath.substringBeforeLast('/', "").trimEnd('/')
    if (dirPath.isBlank()) return null
    return when {
        dirPath.startsWith("/storage/emulated/") -> {
            val after = dirPath.removePrefix("/storage/emulated/")
            val userId = after.substringBefore('/')
            val root = "/storage/emulated/$userId"
            val rest = dirPath.removePrefix(root).trim('/')
            val segments = if (rest.isBlank()) emptyList() else rest.split('/').filter { it.isNotBlank() }
            root to segments
        }
        dirPath.startsWith("/storage/") -> {
            val after = dirPath.removePrefix("/storage/")
            val vol = after.substringBefore('/')
            val root = "/storage/$vol"
            val rest = dirPath.removePrefix(root).trim('/')
            val segments = if (rest.isBlank()) emptyList() else rest.split('/').filter { it.isNotBlank() }
            root to segments
        }
        else -> {
            val segments = dirPath.trim('/').split('/').filter { it.isNotBlank() }
            "" to segments
        }
    }
}

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
internal fun AlbumSortChip(
    sort: AlbumSort,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortChanged: (AlbumSort) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onStartReorder: (() -> Unit)? = null,
) {
    val currentSortLabelRes = when (sort) {
        AlbumSort.NEWEST, AlbumSort.OLDEST -> R.string.sort_recent
        AlbumSort.NAME -> R.string.sort_name
        AlbumSort.ITEM_COUNT -> R.string.sort_size
        AlbumSort.CUSTOM -> R.string.sort_custom
    }
    Box(modifier = modifier) {
        if (compact) {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = stringResource(R.string.action_sort)
                )
            }
        } else {
            AssistChip(
                modifier = Modifier.widthIn(max = 145.dp),
                onClick = { onExpandedChange(true) },
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
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            listOf(
                AlbumSort.NEWEST to R.string.sort_recent,
                AlbumSort.NAME to R.string.sort_name,
                AlbumSort.ITEM_COUNT to R.string.sort_size,
                AlbumSort.CUSTOM to R.string.sort_custom
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
                        onExpandedChange(false)
                        onSortChanged(value)
                    }
                )
            }
            if (onStartReorder != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_reorder_albums)) },
                    leadingIcon = { Icon(Icons.Outlined.DragHandle, null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onExpandedChange(false)
                        onStartReorder()
                    }
                )
            }
        }
    }
}

internal fun buildMediaAlbums(
    images: List<MediaImage>,
    covers: Map<Long, Long>,
    hierarchyDepth: Int = 0,
): List<MediaAlbum> {
    if (hierarchyDepth <= 0) {
        return images.groupBy { it.bucketId }.map { (id, media) ->
            val samplePath = media.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
            val dirPath = samplePath.substringBeforeLast('/', "")
            val isSd = samplePath.isNotBlank() && !samplePath.startsWith("/storage/emulated/0") && !samplePath.startsWith("/data/")
            MediaAlbum(
                id = id,
                name = media.first().bucketName,
                cover = media.firstOrNull { it.id == covers[id] || it.pairedRaw?.id == covers[id] } ?: media.first(),
                images = media,
                isSdCard = isSd,
                storageLabel = if (isSd) "SD Card" else "",
                folderPath = dirPath,
            )
        }
    }

    data class GroupKey(val id: Long, val name: String, val folderPath: String)
    val grouped = LinkedHashMap<GroupKey, MutableList<MediaImage>>()
    for (img in images) {
        val parsed = extractStorageRootAndSegments(img.path)
        val key = if (parsed != null && parsed.second.size > hierarchyDepth) {
            val (root, segments) = parsed
            val ancestorSegments = segments.take(hierarchyDepth)
            val ancestorPath = if (root.isNotEmpty()) "$root/${ancestorSegments.joinToString("/")}" else "/${ancestorSegments.joinToString("/")}"
            val syntheticId = ancestorPath.lowercase(Locale.ROOT).hashCode().toLong()
            GroupKey(
                id = syntheticId,
                name = ancestorSegments.lastOrNull().orEmpty().ifBlank { img.bucketName },
                folderPath = ancestorPath,
            )
        } else {
            val dirPath = img.path.substringBeforeLast('/', "")
            GroupKey(
                id = img.bucketId,
                name = img.bucketName,
                folderPath = dirPath,
            )
        }
        grouped.getOrPut(key) { mutableListOf() }.add(img)
    }

    return grouped.map { (key, media) ->
        val samplePath = media.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
        val isSd = samplePath.isNotBlank() && !samplePath.startsWith("/storage/emulated/0") && !samplePath.startsWith("/data/")
        MediaAlbum(
            id = key.id,
            name = key.name,
            cover = media.firstOrNull { it.id == covers[key.id] } ?: media.first(),
            images = media,
            isSdCard = isSd,
            storageLabel = if (isSd) "SD Card" else "",
            folderPath = key.folderPath,
        )
    }
}

internal fun sortMediaAlbums(
    albums: List<MediaAlbum>,
    pinned: Set<Long>,
    sort: AlbumSort,
    customOrder: List<Long>,
): List<MediaAlbum> {
    val orderIndex = customOrder.withIndex().associate { it.value to it.index }
    if (sort == AlbumSort.CUSTOM) {
        return albums.sortedWith(compareBy<MediaAlbum> { orderIndex[it.id] ?: Int.MAX_VALUE }
            .thenByDescending { it.id in pinned }
            .thenByDescending { it.cover.dateTaken })
    }
    return albums.sortedWith(compareByDescending<MediaAlbum> { it.id in pinned }.thenComparator { a, b ->
        when (sort) {
            AlbumSort.NEWEST -> b.cover.dateTaken.compareTo(a.cover.dateTaken)
            AlbumSort.OLDEST -> a.cover.dateTaken.compareTo(b.cover.dateTaken)
            AlbumSort.NAME -> NaturalOrderComparator.compare(a.name, b.name)
            AlbumSort.ITEM_COUNT -> b.images.size.compareTo(a.images.size)
            AlbumSort.CUSTOM -> 0
        }
    })
}

internal fun groupAlbumsIntoSubfolderClusters(
    albums: List<MediaAlbum>,
    pinned: Set<Long>,
): List<AlbumDisplayItem> {
    if (albums.size <= 1) {
        return albums.map { AlbumDisplayItem.SingleAlbum(it, it.id in pinned) }
    }
    val albumByPath = HashMap<String, MediaAlbum>()
    for (album in albums) {
        val norm = album.folderPath.trimEnd('/').lowercase(Locale.ROOT)
        if (norm.isNotBlank()) albumByPath[norm] = album
    }

    // Group albums by their immediate parent directory (excluding storage roots like /storage/emulated/0).
    val childrenByParent = LinkedHashMap<String, MutableList<MediaAlbum>>()
    val parentOriginalCase = HashMap<String, String>()
    for (album in albums) {
        val parsed = extractStorageRootAndSegments("${album.folderPath}/placeholder")
        if (parsed == null || parsed.second.size < 2) continue
        val parentSegments = parsed.second.dropLast(1)
        val parentPath = if (parsed.first.isNotEmpty()) {
            "${parsed.first}/${parentSegments.joinToString("/")}"
        } else {
            "/${parentSegments.joinToString("/")}"
        }
        val parentKey = parentPath.lowercase(Locale.ROOT)
        parentOriginalCase.putIfAbsent(parentKey, parentPath)
        childrenByParent.getOrPut(parentKey) { mutableListOf() }.add(album)
    }

    val consumedAlbumIds = HashSet<Long>()
    val result = ArrayList<AlbumDisplayItem>()

    for ((parentKey, children) in childrenByParent) {
        val parentDirectAlbum = albumByPath[parentKey]
        // Only cluster when the parent has either a direct album + >=1 child, or >=2 direct subfolders.
        val shouldCluster = (parentDirectAlbum != null && children.isNotEmpty()) || children.size >= 2
        if (!shouldCluster) continue

        val validChildren = children.filter { it.id !in consumedAlbumIds }
        if (validChildren.isEmpty()) continue
        if (parentDirectAlbum != null && parentDirectAlbum.id in consumedAlbumIds) continue

        if (parentDirectAlbum != null) consumedAlbumIds.add(parentDirectAlbum.id)
        validChildren.forEach { consumedAlbumIds.add(it.id) }

        val fullPath = parentOriginalCase[parentKey].orEmpty()
        val clusterName = parentDirectAlbum?.name
            ?: fullPath.substringAfterLast('/').ifBlank { "Folder" }
        val combinedImages = buildList {
            if (parentDirectAlbum != null) addAll(parentDirectAlbum.images)
            validChildren.forEach { addAll(it.images) }
        }.sortedByDescending { it.dateTaken }
        val cover = parentDirectAlbum?.cover ?: combinedImages.firstOrNull() ?: validChildren.first().cover
        val isSd = parentDirectAlbum?.isSdCard ?: validChildren.any { it.isSdCard }
        val clusterPinned = (parentDirectAlbum != null && parentDirectAlbum.id in pinned) ||
            validChildren.any { it.id in pinned }

        result.add(
            AlbumDisplayItem.SubfolderGroup(
                cluster = FolderCluster(
                    id = parentKey,
                    name = clusterName,
                    folderPath = fullPath,
                    directAlbum = parentDirectAlbum,
                    subAlbums = validChildren,
                    allImages = combinedImages,
                    cover = cover,
                    isSdCard = isSd,
                ),
                isPinned = clusterPinned,
            )
        )
    }

    for (album in albums) {
        if (album.id !in consumedAlbumIds) {
            result.add(AlbumDisplayItem.SingleAlbum(album, album.id in pinned))
        }
    }
    return result
}

internal fun sortDisplayItems(
    items: List<AlbumDisplayItem>,
    sort: AlbumSort,
): List<AlbumDisplayItem> {
    if (sort == AlbumSort.CUSTOM) return items
    return items.sortedWith(compareByDescending<AlbumDisplayItem> { it.isPinned }.thenComparator { a, b ->
        when (sort) {
            AlbumSort.NEWEST -> b.latestDate.compareTo(a.latestDate)
            AlbumSort.OLDEST -> a.latestDate.compareTo(b.latestDate)
            AlbumSort.NAME -> NaturalOrderComparator.compare(a.displayName, b.displayName)
            AlbumSort.ITEM_COUNT -> b.totalItemCount.compareTo(a.totalItemCount)
            AlbumSort.CUSTOM -> 0
        }
    })
}

@Composable
internal fun AlbumsViewOptionsDialog(
    viewMode: AlbumsViewMode,
    groupDirectSubfolders: Boolean,
    hierarchyDepth: Int,
    onViewModeChange: (AlbumsViewMode) -> Unit,
    onGroupDirectSubfoldersChange: (Boolean) -> Unit,
    onHierarchyDepthChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.albums_view_options_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.albums_view_type_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onViewModeChange(AlbumsViewMode.GRID) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = viewMode == AlbumsViewMode.GRID,
                            onClick = { onViewModeChange(AlbumsViewMode.GRID) }
                        )
                        Icon(Icons.Outlined.GridView, null, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.view_grid), style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onViewModeChange(AlbumsViewMode.LIST) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = viewMode == AlbumsViewMode.LIST,
                            onClick = { onViewModeChange(AlbumsViewMode.LIST) }
                        )
                        Icon(Icons.AutoMirrored.Outlined.ViewList, null, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.view_list), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onGroupDirectSubfoldersChange(!groupDirectSubfolders) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = groupDirectSubfolders,
                        onCheckedChange = onGroupDirectSubfoldersChange
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.albums_group_subfolders),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.albums_group_subfolders_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.albums_hierarchy_depth_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.albums_hierarchy_depth_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4).forEach { depth ->
                            FilterChip(
                                selected = hierarchyDepth == depth,
                                onClick = { onHierarchyDepthChange(depth) },
                                label = {
                                    Text(
                                        if (depth == 0) stringResource(R.string.albums_hierarchy_depth_off)
                                        else stringResource(R.string.albums_hierarchy_depth_level, depth)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done_editing))
            }
        }
    )
}

@Composable
internal fun AlbumGroupEditorDialog(
    title: String,
    initialName: String,
    allAlbums: List<MediaAlbum>,
    initialSelectedIds: Set<Long>,
    onConfirm: (name: String, selectedIds: List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.album_group_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.album_group_select_albums, selectedIds.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allAlbums, key = { it.id }) { album ->
                        val checked = album.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedIds = if (checked) selectedIds - album.id else selectedIds + album.id
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedIds = if (isChecked) selectedIds + album.id else selectedIds - album.id
                                }
                            )
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                MediaThumbnail(album.cover, Modifier.fillMaxSize(), targetSizePx = 96)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    album.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    stringResource(R.string.album_items_count, album.images.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedIds.toList()) },
                enabled = name.isNotBlank() && selectedIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
internal fun AlbumGroupCoverPickerSheet(
    groupName: String,
    mediaItems: List<MediaImage>,
    currentCoverMediaId: Long?,
    onSelectCover: (Long) -> Unit,
    onResetCover: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dialog_pick_group_cover_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (currentCoverMediaId != null) {
                    TextButton(onClick = onResetCover) {
                        Text(stringResource(R.string.action_reset_group_cover))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(mediaItems.take(180), key = { it.id }) { media ->
                    val isSelected = media.id == currentCoverMediaId
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectCover(media.id) }
                    ) {
                        MediaThumbnail(
                            image = media,
                            modifier = Modifier.fillMaxSize(),
                            targetSizePx = 384,
                            showVideoDuration = false,
                            showFormatBadge = false,
                        )
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCoverCollage(
    covers: List<MediaImage>,
    cornerStyle: CornerStyle,
    targetSizePx: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(cornerStyle.dp.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        val slots = covers.take(4)
        // Two side-by-side panels span the full height of the tile, so decode them at full
        // high resolution instead of dividing by 2 (which produced blurry covers).
        val twoSideTargetPx = maxOf(targetSizePx, 384)
        val quadTargetPx = maxOf(((targetSizePx * 0.75f).toInt()), 256)
        if (slots.size <= 1) {
            slots.firstOrNull()?.let { img ->
                MediaThumbnail(img, Modifier.fillMaxSize(), targetSizePx = maxOf(targetSizePx, 384))
            }
        } else if (slots.size == 2) {
            Row(
                modifier = Modifier.fillMaxSize().padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MediaThumbnail(
                    slots[0],
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                    targetSizePx = twoSideTargetPx,
                    showVideoDuration = false,
                    showFormatBadge = false,
                )
                MediaThumbnail(
                    slots[1],
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                    targetSizePx = twoSideTargetPx,
                    showVideoDuration = false,
                    showFormatBadge = false,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MediaThumbnail(
                        slots[0],
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                        targetSizePx = quadTargetPx,
                        showVideoDuration = false,
                        showFormatBadge = false,
                    )
                    MediaThumbnail(
                        slots[1],
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                        targetSizePx = quadTargetPx,
                        showVideoDuration = false,
                        showFormatBadge = false,
                    )
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MediaThumbnail(
                        slots[2],
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                        targetSizePx = quadTargetPx,
                        showVideoDuration = false,
                        showFormatBadge = false,
                    )
                    if (slots.size >= 4) {
                        MediaThumbnail(
                            slots[3],
                            Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)),
                            targetSizePx = quadTargetPx,
                            showVideoDuration = false,
                            showFormatBadge = false,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FolderSpecial,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsGrid(
    images: List<MediaImage>,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    padding: PaddingValues,
    state: LazyGridState,
    cellSize: Dp = 156.dp,
    onCellSizeChange: ((Dp) -> Unit)? = null,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    gridSpacing: GridSpacing = GridSpacing.STANDARD,
    viewMode: AlbumsViewMode = AlbumsViewMode.GRID,
    groupDirectSubfolders: Boolean = false,
    hierarchyDepth: Int = 0,
    globalAlbumSearch: Boolean = true,
    showCount: Boolean = true,
    showLabels: Boolean = true,
    showFilterChips: Boolean = true,
    pinned: Set<Long> = emptySet(),
    covers: Map<Long, Long> = emptyMap(),
    sort: AlbumSort = AlbumSort.NEWEST,
    customOrder: List<Long> = emptyList(),
    isEditingOrder: Boolean = false,
    albumGroups: List<AlbumGroup> = emptyList(),
    activeGroup: AlbumGroup? = null,
    activeCluster: FolderCluster? = null,
    onTogglePinned: (Long) -> Unit = {},
    onOrderChanged: (List<Long>) -> Unit = {},
    onLockAlbum: ((MediaAlbum) -> Unit)? = null,
    onExcludeFolder: ((MediaAlbum) -> Unit)? = null,
    onDeleteAlbum: ((MediaAlbum) -> Unit)? = null,
    onOpenFolderCluster: ((FolderCluster) -> Unit)? = null,
    onOpenAlbumGroup: ((AlbumGroup) -> Unit)? = null,
    onCreateAlbumGroup: ((String, List<Long>) -> Unit)? = null,
    onAddAlbumToGroup: ((String, Long) -> Unit)? = null,
    onRemoveAlbumFromGroup: ((String, Long) -> Unit)? = null,
    onRenameAlbumGroup: ((String, String) -> Unit)? = null,
    onSetAlbumGroupCover: ((String, Long?) -> Unit)? = null,
    onDeleteAlbumGroup: ((String) -> Unit)? = null,
    onOpenGlobalSearchMedia: ((MediaImage, List<MediaImage>) -> Unit)? = null,
    onOpen: (MediaAlbum) -> Unit,
) {
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(AlbumCategory.ALL) }
    var selectedAlbumForOptions by remember { mutableStateOf<MediaAlbum?>(null) }
    var selectedGroupForOptions by remember { mutableStateOf<AlbumDisplayItem.ManualGroup?>(null) }
    var coverPickerTargetGroup by remember { mutableStateOf<AlbumDisplayItem.ManualGroup?>(null) }
    var addToGroupTargetAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var showCreateGroupForAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var renameGroupTarget by remember { mutableStateOf<AlbumGroup?>(null) }

    LaunchedEffect(showFilterChips) {
        if (!showFilterChips) selectedCategory = AlbumCategory.ALL
    }

    val rawAlbums = remember(images, pinned, covers, sort, customOrder, hierarchyDepth) {
        sortMediaAlbums(buildMediaAlbums(images, covers, hierarchyDepth), pinned, sort, customOrder)
    }

    LaunchedEffect(sort, customOrder, rawAlbums) {
        if (sort == AlbumSort.CUSTOM && customOrder.isEmpty() && rawAlbums.isNotEmpty()) {
            onOrderChanged(rawAlbums.map { it.id })
        }
    }

    val effectiveOrder = remember(rawAlbums, customOrder) {
        customOrder.filter { id -> rawAlbums.any { it.id == id } } +
            rawAlbums.map { it.id }.filterNot(customOrder::contains)
    }
    val currentEffectiveOrder by rememberUpdatedState(effectiveOrder)
    val currentOnOrderChanged by rememberUpdatedState(onOrderChanged)

    // Scoped albums when inside a manual AlbumGroup or a FolderCluster.
    val scopedAlbums = remember(rawAlbums, activeGroup, activeCluster) {
        when {
            activeGroup != null -> {
                val idSet = activeGroup.albumIds.toSet()
                rawAlbums.filter { it.id in idSet }
            }
            activeCluster != null -> {
                buildList {
                    if (activeCluster.directAlbum != null) add(activeCluster.directAlbum)
                    addAll(activeCluster.subAlbums)
                }
            }
            else -> rawAlbums
        }
    }

    val categorizedAlbums = remember(scopedAlbums, selectedCategory) {
        when (selectedCategory) {
            AlbumCategory.ALL -> scopedAlbums
            AlbumCategory.CAMERA_SYSTEM -> scopedAlbums.filter { isCameraOrSystemAlbum(it) }
            AlbumCategory.APPS -> scopedAlbums.filter { isAppAlbum(it) }
            AlbumCategory.VIDEOS -> scopedAlbums.filter { it.images.any { img -> img.isVideo } }
        }
    }

    val displayItems: List<AlbumDisplayItem> = remember(
        categorizedAlbums,
        albumGroups,
        activeGroup,
        activeCluster,
        groupDirectSubfolders,
        pinned,
        sort,
        searchQuery,
        isEditingOrder,
    ) {
        val isRoot = activeGroup == null && activeCluster == null
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isNotBlank()) {
            // During search, show matching flat albums and matching groups cleanly.
            val matchingAlbums = categorizedAlbums
                .filter { it.name.contains(trimmedQuery, ignoreCase = true) }
                .map { AlbumDisplayItem.SingleAlbum(it, it.id in pinned) }
            val matchingGroups = if (isRoot) {
                albumGroups.mapNotNull { g ->
                    if (!g.name.contains(trimmedQuery, ignoreCase = true)) return@mapNotNull null
                    val members = categorizedAlbums.filter { it.id in g.albumIds }
                    if (members.isEmpty()) return@mapNotNull null
                    AlbumDisplayItem.ManualGroup(g, members, members.flatMap { it.images })
                }
            } else emptyList()
            matchingGroups + matchingAlbums
        } else if (isEditingOrder && sort == AlbumSort.CUSTOM) {
            // Reordering operates directly on flat albums so drag indices map 1:1 to albumOrder.
            categorizedAlbums.map { AlbumDisplayItem.SingleAlbum(it, it.id in pinned) }
        } else {
            val groupedIds = if (isRoot) {
                albumGroups.flatMapTo(HashSet()) { it.albumIds }
            } else emptySet()
            val manualGroupEntries = if (isRoot) {
                albumGroups.mapNotNull { group ->
                    val members = categorizedAlbums.filter { it.id in group.albumIds }
                    if (members.isEmpty()) null
                    else AlbumDisplayItem.ManualGroup(
                        group = group,
                        albums = members,
                        allImages = members.flatMap { it.images }.sortedByDescending { it.dateTaken }
                    )
                }
            } else emptyList()

            val ungroupedAlbums = categorizedAlbums.filterNot { it.id in groupedIds }
            val folderItems = if (isRoot && groupDirectSubfolders) {
                groupAlbumsIntoSubfolderClusters(ungroupedAlbums, pinned)
            } else {
                ungroupedAlbums.map { AlbumDisplayItem.SingleAlbum(it, it.id in pinned) }
            }
            sortDisplayItems(manualGroupEntries + folderItems, sort)
        }
    }

    val globalSearchMedia = remember(images, searchQuery, globalAlbumSearch) {
        if (searchQuery.isBlank() || !globalAlbumSearch) emptyList()
        else filterMediaList(images, searchQuery)
    }

    val spacingDp = gridSpacing.dp.dp
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()
    val listState = rememberLazyListState()

    // Drag-and-drop state for Custom Sort reorder mode (#259 Feature 3).
    var draggedAlbumId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    fun swapOrderIds(fromId: Long, toId: Long) {
        if (fromId == toId) return
        val order = currentEffectiveOrder.toMutableList()
        val fromIdx = order.indexOf(fromId)
        val toIdx = order.indexOf(toId)
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return
        order.add(toIdx, order.removeAt(fromIdx))
        currentOnOrderChanged(order)
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp)
            .pointerInput(viewMode, isEditingOrder) {
                if (currentOnCellSizeChange == null || viewMode == AlbumsViewMode.LIST || isEditingOrder) return@pointerInput
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
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val density = LocalDensity.current
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
        val showChipsNow = showFilterChips && activeGroup == null && activeCluster == null && !isEditingOrder

        if (viewMode == AlbumsViewMode.LIST && searchQuery.isBlank()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp + bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showChipsNow) {
                    item(key = "album-filter-chips-list") {
                        AlbumCategoryFilterRow(
                            selectedCategory = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                    }
                }
                if (isEditingOrder && sort == AlbumSort.CUSTOM) {
                    item(key = "reorder-hint-list") {
                        Text(
                            text = stringResource(R.string.albums_drag_reorder_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                if (displayItems.isEmpty()) {
                    item(key = "empty-albums-list") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.empty_photos),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(displayItems, key = { it.stableKey }) { entry ->
                    val singleAlbum = (entry as? AlbumDisplayItem.SingleAlbum)?.album
                    val isDraggingThis = singleAlbum != null && draggedAlbumId == singleAlbum.id
                    val dragMod = if (isEditingOrder && sort == AlbumSort.CUSTOM && singleAlbum != null) {
                        Modifier
                            .zIndex(if (isDraggingThis) 10f else 0f)
                            .graphicsLayer {
                                if (isDraggingThis) {
                                    translationY = dragOffset.y
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                }
                            }
                    } else {
                        Modifier.animateItem()
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDraggingThis) MaterialTheme.colorScheme.surfaceContainerHighest
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = if (isDraggingThis) 6.dp else 0.dp,
                        modifier = dragMod
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = {
                                    if (!isEditingOrder) {
                                        when (entry) {
                                            is AlbumDisplayItem.SingleAlbum -> onOpen(entry.album)
                                            is AlbumDisplayItem.SubfolderGroup -> onOpenFolderCluster?.invoke(entry.cluster)
                                            is AlbumDisplayItem.ManualGroup -> onOpenAlbumGroup?.invoke(entry.group)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isEditingOrder) {
                                        when (entry) {
                                            is AlbumDisplayItem.SingleAlbum -> selectedAlbumForOptions = entry.album
                                            is AlbumDisplayItem.ManualGroup -> selectedGroupForOptions = entry
                                            is AlbumDisplayItem.SubfolderGroup -> {
                                                entry.cluster.directAlbum?.let { selectedAlbumForOptions = it }
                                            }
                                        }
                                    }
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (entry) {
                                is AlbumDisplayItem.SingleAlbum -> {
                                    Card(
                                        shape = RoundedCornerShape(cornerStyle.dp.coerceAtMost(14).dp),
                                        modifier = Modifier.size(58.dp)
                                    ) {
                                        MediaThumbnail(entry.album.cover, Modifier.fillMaxSize(), targetSizePx = 256)
                                    }
                                }
                                is AlbumDisplayItem.SubfolderGroup -> {
                                    GroupCoverCollage(
                                        covers = buildList {
                                            entry.cluster.directAlbum?.cover?.let { add(it) }
                                            entry.cluster.subAlbums.forEach { add(it.cover) }
                                        },
                                        cornerStyle = cornerStyle,
                                        targetSizePx = 256,
                                        modifier = Modifier.size(58.dp)
                                    )
                                }
                                is AlbumDisplayItem.ManualGroup -> {
                                    val customCover = entry.group.coverMediaId?.let { cid ->
                                        entry.allImages.firstOrNull { it.id == cid }
                                    }
                                    if (customCover != null) {
                                        Card(
                                            shape = RoundedCornerShape(cornerStyle.dp.coerceAtMost(14).dp),
                                            modifier = Modifier.size(58.dp)
                                        ) {
                                            MediaThumbnail(customCover, Modifier.fillMaxSize(), targetSizePx = 256)
                                        }
                                    } else {
                                        GroupCoverCollage(
                                            covers = entry.albums.map { it.cover },
                                            cornerStyle = cornerStyle,
                                            targetSizePx = 256,
                                            modifier = Modifier.size(58.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = entry.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (entry is AlbumDisplayItem.SingleAlbum && entry.album.isSdCard) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.storage_sd_card),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                val subtitle = buildString {
                                    append(stringResource(R.string.album_items_count, entry.totalItemCount))
                                    when (entry) {
                                        is AlbumDisplayItem.SubfolderGroup -> {
                                            append(" · ")
                                            append(stringResource(R.string.album_subfolders_count, entry.cluster.subAlbums.size))
                                        }
                                        is AlbumDisplayItem.ManualGroup -> {
                                            append(" · ")
                                            append(stringResource(R.string.album_group_count, entry.albums.size))
                                        }
                                        is AlbumDisplayItem.SingleAlbum -> {}
                                    }
                                    if (entry.totalSizeBytes > 0L) {
                                        append(" · ")
                                        append(formatAlbumSizeBytes(entry.totalSizeBytes))
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (isEditingOrder && sort == AlbumSort.CUSTOM && singleAlbum != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .pointerInput(singleAlbum.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedAlbumId = singleAlbum.id
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragEnd = {
                                                    draggedAlbumId = null
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragCancel = {
                                                    draggedAlbumId = null
                                                    dragOffset = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += Offset(0f, dragAmount.y)
                                                    val layoutInfo = listState.layoutInfo
                                                    val currentInfo = layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == "album:${singleAlbum.id}" }
                                                    if (currentInfo != null) {
                                                        val draggedCenterY = currentInfo.offset + currentInfo.size / 2f + dragOffset.y
                                                        val target = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                            val keyStr = item.key as? String ?: return@firstOrNull false
                                                            keyStr.startsWith("album:") &&
                                                                keyStr != currentInfo.key &&
                                                                draggedCenterY >= item.offset &&
                                                                draggedCenterY <= item.offset + item.size
                                                        }
                                                        if (target != null) {
                                                            val targetId = (target.key as String).removePrefix("album:").toLongOrNull()
                                                            if (targetId != null) {
                                                                val deltaY = target.offset - currentInfo.offset
                                                                swapOrderIds(singleAlbum.id, targetId)
                                                                dragOffset = Offset(0f, dragOffset.y - deltaY)
                                                            }
                                                        }
                                                        val viewportH = layoutInfo.viewportSize.height
                                                        if (draggedCenterY < 100f) {
                                                            coroutineScope.launch { listState.scrollBy(-24f) }
                                                        } else if (draggedCenterY > viewportH - 100f) {
                                                            coroutineScope.launch { listState.scrollBy(24f) }
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.DragHandle,
                                        contentDescription = stringResource(R.string.action_reorder_albums),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (entry.isPinned && sort != AlbumSort.CUSTOM && singleAlbum != null) {
                                IconButton(onClick = { onTogglePinned(singleAlbum.id) }) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = stringResource(R.string.album_unpin),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(cellSize),
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp + bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(spacingDp + 8.dp),
                verticalArrangement = Arrangement.spacedBy(spacingDp + 12.dp),
            ) {
                if (showChipsNow) {
                    item(key = "album-filter-chips", span = { GridItemSpan(maxLineSpan) }) {
                        AlbumCategoryFilterRow(
                            selectedCategory = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                    }
                }

                if (isEditingOrder && sort == AlbumSort.CUSTOM) {
                    item(key = "reorder-hint-grid", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.albums_drag_reorder_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                if (displayItems.isEmpty() && globalSearchMedia.isEmpty()) {
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
                                    stringResource(R.string.empty_search_albums, searchQuery)
                                } else {
                                    stringResource(R.string.empty_photos)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank()) {
                                TextButton(onClick = onClearSearch) {
                                    Text(stringResource(R.string.clear_search))
                                }
                            }
                        }
                    }
                }

                items(displayItems, key = { it.stableKey }) { entry ->
                    val singleAlbum = (entry as? AlbumDisplayItem.SingleAlbum)?.album
                    val isDraggingThis = singleAlbum != null && draggedAlbumId == singleAlbum.id

                    val dragCardMod = if (isEditingOrder && sort == AlbumSort.CUSTOM && singleAlbum != null) {
                        Modifier
                            .zIndex(if (isDraggingThis) 10f else 0f)
                            .graphicsLayer {
                                if (isDraggingThis) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                }
                            }
                            .pointerInput(singleAlbum.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedAlbumId = singleAlbum.id
                                        dragOffset = Offset.Zero
                                    },
                                    onDragEnd = {
                                        draggedAlbumId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggedAlbumId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        val layoutInfo = state.layoutInfo
                                        val currentInfo = layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == "album:${singleAlbum.id}" }
                                        if (currentInfo != null) {
                                            val cx = currentInfo.offset.x + currentInfo.size.width / 2f + dragOffset.x
                                            val cy = currentInfo.offset.y + currentInfo.size.height / 2f + dragOffset.y
                                            val target = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                val keyStr = item.key as? String ?: return@firstOrNull false
                                                keyStr.startsWith("album:") &&
                                                    keyStr != currentInfo.key &&
                                                    cx >= item.offset.x && cx <= item.offset.x + item.size.width &&
                                                    cy >= item.offset.y && cy <= item.offset.y + item.size.height
                                            }
                                            if (target != null) {
                                                val targetId = (target.key as String).removePrefix("album:").toLongOrNull()
                                                if (targetId != null) {
                                                    val dx = target.offset.x - currentInfo.offset.x
                                                    val dy = target.offset.y - currentInfo.offset.y
                                                    swapOrderIds(singleAlbum.id, targetId)
                                                    dragOffset = Offset(dragOffset.x - dx, dragOffset.y - dy)
                                                }
                                            }
                                            val viewportH = layoutInfo.viewportSize.height
                                            if (cy < 120f) {
                                                coroutineScope.launch { state.scrollBy(-28f) }
                                            } else if (cy > viewportH - 120f) {
                                                coroutineScope.launch { state.scrollBy(28f) }
                                            }
                                        }
                                    }
                                )
                            }
                    } else {
                        Modifier.animateItem()
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .then(dragCardMod)
                            .combinedClickable(
                                onClick = {
                                    if (!isEditingOrder) {
                                        when (entry) {
                                            is AlbumDisplayItem.SingleAlbum -> onOpen(entry.album)
                                            is AlbumDisplayItem.SubfolderGroup -> onOpenFolderCluster?.invoke(entry.cluster)
                                            is AlbumDisplayItem.ManualGroup -> onOpenAlbumGroup?.invoke(entry.group)
                                        }
                                    }
                                },
                                onLongClick = if (isEditingOrder) null else {
                                    {
                                        when (entry) {
                                            is AlbumDisplayItem.SingleAlbum -> selectedAlbumForOptions = entry.album
                                            is AlbumDisplayItem.ManualGroup -> selectedGroupForOptions = entry
                                            is AlbumDisplayItem.SubfolderGroup -> {
                                                entry.cluster.directAlbum?.let { selectedAlbumForOptions = it }
                                            }
                                        }
                                    }
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            when (entry) {
                                is AlbumDisplayItem.SingleAlbum -> {
                                    Card(
                                        shape = RoundedCornerShape(cornerStyle.dp.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        MediaThumbnail(
                                            entry.album.cover,
                                            Modifier.fillMaxSize(),
                                            targetSizePx = targetThumbnailPx,
                                        )
                                    }
                                }
                                is AlbumDisplayItem.SubfolderGroup -> {
                                    GroupCoverCollage(
                                        covers = buildList {
                                            entry.cluster.directAlbum?.cover?.let { add(it) }
                                            entry.cluster.subAlbums.forEach { add(it.cover) }
                                        },
                                        cornerStyle = cornerStyle,
                                        targetSizePx = targetThumbnailPx,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(12.dp))
                                            Text(
                                                text = "${entry.cluster.subAlbums.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is AlbumDisplayItem.ManualGroup -> {
                                    val customCover = entry.group.coverMediaId?.let { cid ->
                                        entry.allImages.firstOrNull { it.id == cid }
                                    }
                                    if (customCover != null) {
                                        Card(
                                            shape = RoundedCornerShape(cornerStyle.dp.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            MediaThumbnail(
                                                customCover,
                                                Modifier.fillMaxSize(),
                                                targetSizePx = maxOf(targetThumbnailPx, 384),
                                            )
                                        }
                                    } else {
                                        GroupCoverCollage(
                                            covers = entry.albums.map { it.cover },
                                            cornerStyle = cornerStyle,
                                            targetSizePx = targetThumbnailPx,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Outlined.FolderSpecial, null, modifier = Modifier.size(12.dp))
                                            Text(
                                                text = "${entry.albums.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (sort != AlbumSort.CUSTOM && entry.isPinned && singleAlbum != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .clickable { onTogglePinned(singleAlbum.id) },
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Filled.PushPin,
                                            contentDescription = stringResource(R.string.album_unpin),
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                            } else if (sort == AlbumSort.CUSTOM && isEditingOrder && singleAlbum != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(32.dp)
                                        .pointerInput(singleAlbum.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedAlbumId = singleAlbum.id
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragEnd = {
                                                    draggedAlbumId = null
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragCancel = {
                                                    draggedAlbumId = null
                                                    dragOffset = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                    val layoutInfo = state.layoutInfo
                                                    val currentInfo = layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == "album:${singleAlbum.id}" }
                                                    if (currentInfo != null) {
                                                        val cx = currentInfo.offset.x + currentInfo.size.width / 2f + dragOffset.x
                                                        val cy = currentInfo.offset.y + currentInfo.size.height / 2f + dragOffset.y
                                                        val target = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                            val keyStr = item.key as? String ?: return@firstOrNull false
                                                            keyStr.startsWith("album:") &&
                                                                keyStr != currentInfo.key &&
                                                                cx >= item.offset.x && cx <= item.offset.x + item.size.width &&
                                                                cy >= item.offset.y && cy <= item.offset.y + item.size.height
                                                        }
                                                        if (target != null) {
                                                            val targetId = (target.key as String).removePrefix("album:").toLongOrNull()
                                                            if (targetId != null) {
                                                                val dx = target.offset.x - currentInfo.offset.x
                                                                val dy = target.offset.y - currentInfo.offset.y
                                                                swapOrderIds(singleAlbum.id, targetId)
                                                                dragOffset = Offset(dragOffset.x - dx, dragOffset.y - dy)
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Outlined.DragHandle,
                                            contentDescription = stringResource(R.string.action_reorder_albums),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }

                        val isSdCard = (entry as? AlbumDisplayItem.SingleAlbum)?.album?.isSdCard
                            ?: (entry as? AlbumDisplayItem.SubfolderGroup)?.cluster?.isSdCard
                            ?: false
                        if (showLabels || showCount || isSdCard) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (showLabels) {
                                    Text(
                                        entry.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSdCard || showCount) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSdCard) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.storage_sd_card),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (showCount) {
                                            val countText = when (entry) {
                                                is AlbumDisplayItem.SubfolderGroup ->
                                                    "${stringResource(R.string.album_items_count, entry.totalItemCount)} (${entry.cluster.subAlbums.size})"
                                                is AlbumDisplayItem.ManualGroup ->
                                                    "${stringResource(R.string.album_items_count, entry.totalItemCount)} · ${stringResource(R.string.album_group_count, entry.albums.size)}"
                                                is AlbumDisplayItem.SingleAlbum ->
                                                    stringResource(R.string.album_items_count, entry.totalItemCount)
                                            }
                                            Text(
                                                countText,
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

                // Global media search results across all albums (#259 Feature 2)
                if (searchQuery.isNotBlank() && globalAlbumSearch && globalSearchMedia.isNotEmpty()) {
                    item(key = "global-search-media-header", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.global_search_media_header, globalSearchMedia.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(globalSearchMedia, key = { "gmedia:${it.id}" }) { media ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenGlobalSearchMedia?.invoke(media, globalSearchMedia) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(cornerStyle.dp.dp))
                            ) {
                                MediaThumbnail(
                                    image = media,
                                    modifier = Modifier.fillMaxSize(),
                                    targetSizePx = targetThumbnailPx,
                                )
                            }
                            Text(
                                text = media.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
                            )
                            Text(
                                text = media.bucketName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Album long-press options bottom sheet (#252 Delete Album + #234 Add/Remove Group)
        if (selectedAlbumForOptions != null) {
            val album = selectedAlbumForOptions!!
            val isPinned = album.id in pinned
            val containingGroup = albumGroups.firstOrNull { album.id in it.albumIds }
            ModalBottomSheet(
                onDismissRequest = { selectedAlbumForOptions = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            val sizeLabel = formatAlbumSizeBytes(album.images.sumOf { it.sizeBytes })
                            Text(
                                text = "${stringResource(R.string.album_items_count, album.images.size)} · $sizeLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pin / Unpin Album
                    AlbumSheetActionCard(
                        icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        iconBg = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        title = stringResource(if (isPinned) R.string.album_unpin else R.string.album_pin),
                        subtitle = stringResource(if (isPinned) R.string.unpin_album_desc else R.string.pin_album_desc),
                        onClick = {
                            selectedAlbumForOptions = null
                            onTogglePinned(album.id)
                        }
                    )

                    // Add to / Remove from Album Group (#234)
                    if (onCreateAlbumGroup != null || onAddAlbumToGroup != null) {
                        if (containingGroup != null && onRemoveAlbumFromGroup != null) {
                            AlbumSheetActionCard(
                                icon = Icons.Outlined.FolderSpecial,
                                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.action_remove_from_group, containingGroup.name),
                                subtitle = stringResource(R.string.remove_from_group_desc),
                                onClick = {
                                    selectedAlbumForOptions = null
                                    onRemoveAlbumFromGroup(containingGroup.id, album.id)
                                }
                            )
                        } else {
                            AlbumSheetActionCard(
                                icon = Icons.Outlined.CreateNewFolder,
                                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = stringResource(R.string.action_add_to_group),
                                subtitle = stringResource(R.string.add_to_group_desc),
                                onClick = {
                                    selectedAlbumForOptions = null
                                    if (albumGroups.isNotEmpty()) {
                                        addToGroupTargetAlbum = album
                                    } else {
                                        showCreateGroupForAlbum = album
                                    }
                                }
                            )
                        }
                    }

                    // Lock Album
                    if (onLockAlbum != null) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.Lock,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.action_lock_album),
                            subtitle = stringResource(R.string.lock_album_desc),
                            onClick = {
                                selectedAlbumForOptions = null
                                onLockAlbum(album)
                            }
                        )
                    }

                    // Exclude Folder
                    if (onExcludeFolder != null) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.FolderOff,
                            iconBg = MaterialTheme.colorScheme.surfaceContainerHighest,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = stringResource(R.string.action_exclude_album),
                            subtitle = stringResource(R.string.exclude_album_desc),
                            onClick = {
                                selectedAlbumForOptions = null
                                onExcludeFolder(album)
                            }
                        )
                    }

                    // Delete Album (#252)
                    if (onDeleteAlbum != null) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.DeleteOutline,
                            iconBg = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.action_delete_album),
                            subtitle = stringResource(R.string.delete_album_desc),
                            onClick = {
                                selectedAlbumForOptions = null
                                onDeleteAlbum(album)
                            }
                        )
                    }
                }
            }
        }

        // Manual Album Group long-press options sheet
        if (selectedGroupForOptions != null) {
            val groupEntry = selectedGroupForOptions!!
            val group = groupEntry.group
            ModalBottomSheet(
                onDismissRequest = { selectedGroupForOptions = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (onSetAlbumGroupCover != null && groupEntry.allImages.isNotEmpty()) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.Wallpaper,
                            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = stringResource(R.string.action_set_group_cover),
                            subtitle = stringResource(R.string.set_group_cover_desc),
                            onClick = {
                                selectedGroupForOptions = null
                                coverPickerTargetGroup = groupEntry
                            }
                        )
                        if (group.coverMediaId != null) {
                            AlbumSheetActionCard(
                                icon = Icons.Outlined.RestartAlt,
                                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = stringResource(R.string.action_reset_group_cover),
                                subtitle = stringResource(R.string.reset_group_cover_desc),
                                onClick = {
                                    selectedGroupForOptions = null
                                    onSetAlbumGroupCover(group.id, null)
                                }
                            )
                        }
                    }
                    if (onRenameAlbumGroup != null) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.Edit,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.action_rename_group),
                            subtitle = stringResource(R.string.rename_group_desc),
                            onClick = {
                                selectedGroupForOptions = null
                                renameGroupTarget = group
                            }
                        )
                    }
                    if (onDeleteAlbumGroup != null) {
                        AlbumSheetActionCard(
                            icon = Icons.Outlined.DeleteOutline,
                            iconBg = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.action_ungroup_albums),
                            subtitle = stringResource(R.string.ungroup_albums_desc),
                            onClick = {
                                selectedGroupForOptions = null
                                onDeleteAlbumGroup(group.id)
                            }
                        )
                    }
                }
            }
        }

        if (coverPickerTargetGroup != null && onSetAlbumGroupCover != null) {
            val targetEntry = coverPickerTargetGroup!!
            AlbumGroupCoverPickerSheet(
                groupName = targetEntry.group.name,
                mediaItems = targetEntry.allImages,
                currentCoverMediaId = targetEntry.group.coverMediaId,
                onSelectCover = { mediaId ->
                    coverPickerTargetGroup = null
                    onSetAlbumGroupCover(targetEntry.group.id, mediaId)
                },
                onResetCover = {
                    coverPickerTargetGroup = null
                    onSetAlbumGroupCover(targetEntry.group.id, null)
                },
                onDismiss = { coverPickerTargetGroup = null },
            )
        }

        // Pick existing group or create a new group for an album
        if (addToGroupTargetAlbum != null) {
            val targetAlbum = addToGroupTargetAlbum!!
            AlertDialog(
                onDismissRequest = { addToGroupTargetAlbum = null },
                title = { Text(stringResource(R.string.action_add_to_group), fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        albumGroups.forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        addToGroupTargetAlbum = null
                                        onAddAlbumToGroup?.invoke(group.id, targetAlbum.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(group.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                addToGroupTargetAlbum = null
                                showCreateGroupForAlbum = targetAlbum
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CreateNewFolder, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_create_album_group))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { addToGroupTargetAlbum = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showCreateGroupForAlbum != null) {
            val seedAlbum = showCreateGroupForAlbum!!
            AlbumGroupEditorDialog(
                title = stringResource(R.string.action_create_album_group),
                initialName = "",
                allAlbums = rawAlbums,
                initialSelectedIds = setOf(seedAlbum.id),
                onConfirm = { groupName, selectedIds ->
                    showCreateGroupForAlbum = null
                    onCreateAlbumGroup?.invoke(groupName, selectedIds)
                },
                onDismiss = { showCreateGroupForAlbum = null }
            )
        }

        if (renameGroupTarget != null) {
            val targetGroup = renameGroupTarget!!
            var newGroupName by remember(targetGroup.id) { mutableStateOf(targetGroup.name) }
            AlertDialog(
                onDismissRequest = { renameGroupTarget = null },
                title = { Text(stringResource(R.string.action_rename_group), fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text(stringResource(R.string.album_group_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            renameGroupTarget = null
                            onRenameAlbumGroup?.invoke(targetGroup.id, newGroupName.trim())
                        },
                        enabled = newGroupName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameGroupTarget = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

internal val ConsumeHorizontalNestedScroll = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = Offset(x = available.x, y = 0f)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = Velocity(x = available.x, y = 0f)
}

@Composable
private fun AlbumCategoryFilterRow(
    selectedCategory: AlbumCategory,
    onSelect: (AlbumCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .nestedScroll(ConsumeHorizontalNestedScroll)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedCategory == AlbumCategory.ALL,
            onClick = { onSelect(AlbumCategory.ALL) },
            label = { Text(stringResource(R.string.filter_all_albums)) }
        )
        FilterChip(
            selected = selectedCategory == AlbumCategory.CAMERA_SYSTEM,
            onClick = { onSelect(AlbumCategory.CAMERA_SYSTEM) },
            label = { Text(stringResource(R.string.filter_camera_system)) }
        )
        FilterChip(
            selected = selectedCategory == AlbumCategory.APPS,
            onClick = { onSelect(AlbumCategory.APPS) },
            label = { Text(stringResource(R.string.filter_apps)) }
        )
        FilterChip(
            selected = selectedCategory == AlbumCategory.VIDEOS,
            onClick = { onSelect(AlbumCategory.VIDEOS) },
            label = { Text(stringResource(R.string.filter_videos)) }
        )
    }
}

@Composable
private fun AlbumSheetActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
