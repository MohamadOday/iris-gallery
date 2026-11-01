@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.app.Activity
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.PushPin
import com.iris.gallery.data.MediaSort
import com.iris.gallery.data.NaturalOrderComparator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.iris.gallery.data.isGif
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isMotionPhoto
import com.iris.gallery.data.isPanorama
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Checkbox
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.res.stringResource
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.resolveMediaUri
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isGif
import com.iris.gallery.data.isPanorama
import com.iris.gallery.data.isMotionPhoto
import com.iris.gallery.ui.DuplicateScanState
import com.iris.gallery.ui.MediaThumbnail
import com.iris.gallery.ui.AlbumsGrid
import com.iris.gallery.ui.MediaAlbum
import com.iris.gallery.ui.LibraryScreen
import com.iris.gallery.ui.FolderBrowserScreen
import com.iris.gallery.ui.EditorScreen
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.DeleteMode
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.ui.SettingsScreen
import com.iris.gallery.ui.AboutScreen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
import com.iris.gallery.ui.AlbumPickerSheet
import com.iris.gallery.R
import java.io.File

private enum class MediaFormatFilter(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    RAW("RAW"),
    GIF("GIFs"),
    PANORAMA("Panoramas"),
    MOTION("Motion Photos"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.IrisPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    var wasRefreshing by remember { mutableStateOf(false) }
    var isDismissingInPlace by remember { mutableStateOf(false) }
    val dismissAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val dismissScale = remember { androidx.compose.animation.core.Animatable(1f) }

    val restingState = remember {
        object : PullToRefreshState {
            override val distanceFraction: Float get() = 1f
            override val isAnimating: Boolean get() = false
            override suspend fun animateToThreshold() {}
            override suspend fun animateToHidden() {}
            override suspend fun snapTo(targetValue: Float) {}
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            wasRefreshing = true
            isDismissingInPlace = false
            dismissAlpha.snapTo(1f)
            dismissScale.snapTo(1f)
        } else if (wasRefreshing) {
            wasRefreshing = false
            isDismissingInPlace = true
            try {
                kotlinx.coroutines.coroutineScope {
                    launch {
                        dismissAlpha.animateTo(0f, tween(220, easing = LinearOutSlowInEasing))
                    }
                    launch {
                        dismissScale.animateTo(0.65f, tween(220, easing = LinearOutSlowInEasing))
                    }
                }
            } finally {
                isDismissingInPlace = false
                dismissAlpha.snapTo(1f)
                dismissScale.snapTo(1f)
            }
        }
    }

    if (isDismissingInPlace) {
        PullToRefreshDefaults.Indicator(
            state = restingState,
            isRefreshing = true,
            modifier = modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = dismissAlpha.value
                    scaleX = dismissScale.value
                    scaleY = dismissScale.value
                }
        )
    } else {
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = modifier.align(Alignment.TopCenter)
        )
    }
}

private enum class GalleryTab(
    val destination: Int,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    MEDIA(0, R.string.tab_media, Icons.Outlined.PermMedia),
    ALBUMS(1, R.string.tab_albums, Icons.Outlined.PhotoAlbum),
    FAVORITES(2, R.string.tab_favorites, Icons.Outlined.FavoriteBorder),
    LIBRARY(3, R.string.tab_library, Icons.Outlined.Dashboard),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryScaffold(
    trashFeedback: TrashFeedback? = null,
    onTrashFeedbackChange: (TrashFeedback?) -> Unit = {},
    settings: SettingsState,
    settingsPreferences: SettingsPreferences,
    images: List<MediaImage>,
    trashed: List<MediaImage>,
    lockedIds: Set<Long>,
    lockedMedia: List<MediaImage>,
    pinnedAlbums: Set<Long>,
    albumCovers: Map<Long, Long>,
    albumSort: com.iris.gallery.data.AlbumSort,
    albumOrder: List<Long>,
    albumMediaSort: com.iris.gallery.data.MediaSort = com.iris.gallery.data.MediaSort.DATE_DESC,
    albumMediaSortOverrides: Map<Long, com.iris.gallery.data.MediaSort> = emptyMap(),
    lockedAuthorized: Boolean,
    onLockVault: () -> Unit = {},
    loading: Boolean,
    error: String?,
    favorites: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onLockMedia: (List<MediaImage>) -> Unit,
    onUnlockMedia: (List<MediaImage>) -> Unit,
    onDeleteFromLocked: (List<MediaImage>, () -> Unit) -> Unit,
    onTogglePinnedAlbum: (Long) -> Unit,
    onSetAlbumCover: (Long, Long) -> Unit,
    onSetAlbumSort: (com.iris.gallery.data.AlbumSort) -> Unit,
    onSetAlbumOrder: (List<Long>) -> Unit,
    onSetAlbumMediaSort: (com.iris.gallery.data.MediaSort) -> Unit = {},
    onSetAlbumMediaSortOverride: (Long, com.iris.gallery.data.MediaSort?) -> Unit = { _, _ -> },
    onRequestUnlock: () -> Unit,
    onPick: ((MediaImage) -> Unit)?,
    onTrash: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
    onRestore: (List<MediaImage>) -> Unit,
    onDeletePermanently: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
    onEditMetadata: (MediaImage, ExifEditRequest, (MediaImage?) -> Unit) -> Unit = { _, _, _ -> },
    onMoveToAlbum: (List<MediaImage>, java.io.File, String, (AlbumOperationResult) -> Unit) -> Unit,
    onCopyToAlbum: (List<MediaImage>, java.io.File, String, (AlbumOperationResult) -> Unit) -> Unit,
    getAlbumDir: (MediaAlbum) -> java.io.File,
    createAlbumDir: (String) -> java.io.File,
    duplicateState: DuplicateScanState,
    onScanDuplicates: () -> Unit,
    onCancelDuplicateScan: () -> Unit,
    onRefresh: () -> Unit = {},
    onRescanMedia: () -> Unit = {},
    onRenameMedia: (MediaImage, String, (MediaImage?) -> Unit) -> Unit = { _, _, _ -> },
    excludedFolders: Set<String> = emptySet(),
    onAddExcludedFolder: (String) -> Unit = {},
    onRemoveExcludedFolder: (String) -> Unit = {},
    initialMemories: Boolean,
    initialViewUri: Uri? = null,
    initialEditMode: Boolean = false,
    initialOpenLatest: Boolean = false,
    initialIntentGeneration: Long = 0L,
    onInvalidMedia: ((MediaImage) -> Unit)? = null,
) {
    val context = LocalContext.current
    val visibleTabs = remember(settings.showMediaTab, settings.showAlbumsTab, settings.showFavoritesTab, settings.showLibraryTab) {
        val list = mutableListOf<GalleryTab>()
        if (settings.showMediaTab) list.add(GalleryTab.MEDIA)
        if (settings.showAlbumsTab) list.add(GalleryTab.ALBUMS)
        if (settings.showFavoritesTab) list.add(GalleryTab.FAVORITES)
        if (settings.showLibraryTab) list.add(GalleryTab.LIBRARY)
        if (list.isEmpty()) listOf(GalleryTab.MEDIA) else list
    }
    val initialTargetTab = if (initialMemories) {
        GalleryTab.LIBRARY
    } else {
        when (settings.startupTab) {
            StartupTab.MEDIA -> GalleryTab.MEDIA
            StartupTab.ALBUMS -> GalleryTab.ALBUMS
            StartupTab.FAVORITES -> GalleryTab.FAVORITES
            StartupTab.LIBRARY -> GalleryTab.LIBRARY
        }
    }
    val initialPageIndex = visibleTabs.indexOf(initialTargetTab).let { if (it >= 0) it else 0 }
    val tabPagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { visibleTabs.size }
    )
    val tabScope = rememberCoroutineScope()
    LaunchedEffect(visibleTabs) {
        if (tabPagerState.currentPage >= visibleTabs.size) {
            tabPagerState.scrollToPage(visibleTabs.lastIndex.coerceAtLeast(0))
        }
    }
    // Use the settled page so the top bar and its actions don't flip to the next
    // tab while the user is still dragging and might release back on the current one.
    val currentVisibleTab = visibleTabs.getOrElse(tabPagerState.settledPage.coerceIn(0, visibleTabs.lastIndex)) { visibleTabs.first() }
    // The bottom bar should track the pager right away, not wait for the fling to settle.
    val navVisibleTab = visibleTabs.getOrElse(tabPagerState.targetPage.coerceIn(0, visibleTabs.lastIndex)) { visibleTabs.first() }
    val destination = currentVisibleTab.destination
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var externalMedia by remember { mutableStateOf<MediaImage?>(null) }
    var viewerImages by remember { mutableStateOf<List<MediaImage>?>(null) }
    var selectedAlbumId by remember { mutableStateOf<Long?>(null) }
    var selectedLockedAlbum by remember { mutableStateOf<String?>(null) }
    var librarySection by remember { mutableStateOf<String?>(if (initialMemories) "memories" else null) }
    var editorImage by remember { mutableStateOf<MediaImage?>(null) }
    var isEditingAlbumOrder by remember { mutableStateOf(false) }

    LaunchedEffect(destination, librarySection) {
        if (destination != 3 || librarySection != "locked") {
            onLockVault()
        }
    }

    var handledIntentGeneration by remember { mutableStateOf(-1L) }
    LaunchedEffect(initialViewUri, initialEditMode, initialOpenLatest, images, initialIntentGeneration) {
        if (handledIntentGeneration != initialIntentGeneration) {
            if (initialViewUri != null) {
                handledIntentGeneration = initialIntentGeneration
                val resolved = withContext(Dispatchers.IO) { resolveMediaUri(context, initialViewUri) }
                val matched = images.firstOrNull {
                    it.uri == initialViewUri ||
                    (resolved.path.isNotBlank() && it.path == resolved.path) ||
                    it.uri.lastPathSegment == initialViewUri.lastPathSegment
                }
                if (matched != null) {
                    if (initialEditMode) {
                        editorImage = matched
                        selectedId = null
                        externalMedia = null
                    } else {
                        viewerImages = null
                        selectedId = matched.id
                        externalMedia = null
                    }
                } else {
                    if (initialEditMode) {
                        editorImage = resolved
                        selectedId = null
                        externalMedia = null
                    } else {
                        externalMedia = resolved
                        selectedId = null
                    }
                }
            } else if (initialOpenLatest && images.isNotEmpty()) {
                handledIntentGeneration = initialIntentGeneration
                viewerImages = null
                selectedId = images.first().id
                externalMedia = null
            }
        }
    }
    var customCellSize by remember { mutableStateOf(settings.photoGridSize.dp) }
    var customAlbumCellSize by remember { mutableStateOf(settings.albumGridSize.dp) }
    var compactGrid by remember { mutableStateOf(settings.photoGridSize < 95f) }
    var mediaFilter by remember { mutableStateOf(0) }

    LaunchedEffect(settings.photoGridSize) {
        customCellSize = settings.photoGridSize.dp
        compactGrid = settings.photoGridSize < 95f
    }
    LaunchedEffect(settings.albumGridSize) {
        customAlbumCellSize = settings.albumGridSize.dp
    }

    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }
    var pendingRestoreAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectionMenuExpanded by remember { mutableStateOf(false) }
    val photoGridState = androidx.compose.runtime.key(mediaFilter) { rememberLazyGridState() }
    val albumGridState = rememberLazyGridState()
    val albumPhotoGridState = rememberLazyGridState()
    val favoriteGridState = rememberLazyGridState()
    val libraryGridState = rememberLazyGridState()
    val mediaTabLabel = stringResource(
        when (mediaFilter) {
            0 -> R.string.tab_media
            1 -> R.string.tab_photos
            2 -> R.string.tab_videos
            else -> R.string.tab_media
        }
    )
    val albumsTabLabel = stringResource(R.string.tab_albums)
    val favoritesTabLabel = stringResource(R.string.tab_favorites)
    val libraryTabLabel = stringResource(R.string.tab_library)
    val photoCellSize = customCellSize
    val onCellSizeChange: (androidx.compose.ui.unit.Dp) -> Unit = { newSize ->
        customCellSize = newSize
        compactGrid = newSize < 95.dp
        settingsPreferences.setPhotoGridSize(newSize.value)
    }
    val onAlbumCellSizeChange: (androidx.compose.ui.unit.Dp) -> Unit = { newSize ->
        customAlbumCellSize = newSize
        settingsPreferences.setAlbumGridSize(newSize.value)
    }
    val favoriteImages = remember(images, favorites) { images.filter { it.id in favorites } }
    // Keep only the album identity as state. Its contents are derived atomically
    // from the current library, so deletion/locking cannot expose a stale list.
    val selectedAlbum = remember(images, selectedAlbumId, albumCovers) {
        selectedAlbumId?.let { albumId ->
            val albumImages = images.filter { it.bucketId == albumId }
            albumImages.firstOrNull()?.let { first ->
                val samplePath = albumImages.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
                val isSd = samplePath.isNotBlank() && !samplePath.startsWith("/storage/emulated/0") && !samplePath.startsWith("/data/")
                MediaAlbum(
                    id = albumId,
                    name = first.bucketName,
                    cover = albumImages.firstOrNull { it.id == albumCovers[albumId] } ?: first,
                    images = albumImages,
                    isSdCard = isSd,
                    storageLabel = if (isSd) "SD Card" else ""
                )
            }
        }
    }
    var activeOverlayScreen by remember { mutableStateOf<String?>(null) }
    var fileSearchQuery by remember { mutableStateOf("") }
    var isFileSearching by remember { mutableStateOf(false) }

    LaunchedEffect(destination, librarySection, selectedAlbumId) {
        val canSearch = destination == 0 || destination == 2 || (destination == 1 && selectedAlbumId != null)
        if (!canSearch && isFileSearching) {
            isFileSearching = false
            fileSearchQuery = ""
        }
    }

    fun filterMediaList(list: List<MediaImage>, query: String): List<MediaImage> {
        if (query.isBlank()) return list
        val q = query.trim()
        val qLower = q.lowercase()
        return list.filter { item ->
            item.name.contains(q, ignoreCase = true) ||
            item.title.contains(q, ignoreCase = true) ||
            item.bucketName.contains(q, ignoreCase = true) ||
            item.path.contains(q, ignoreCase = true) ||
            ((qLower == "video" || qLower == "videos" || qLower == ".mp4" || qLower == ".mkv" || qLower == ".mov" || qLower == ".webm" || qLower == ".3gp") && item.isVideo) ||
            ((qLower == "photo" || qLower == "photos" || qLower == "image" || qLower == "images" || qLower == ".jpg" || qLower == ".jpeg" || qLower == ".png" || qLower == ".webp" || qLower == ".heic") && !item.isVideo) ||
            ((qLower == "gif" || qLower == "gifs" || qLower == ".gif") && item.isGif) ||
            ((qLower == "raw" || qLower == ".dng" || qLower == ".cr2" || qLower == ".nef" || qLower == ".arw") && item.isRaw) ||
            ((qLower == "motion" || qLower == "live") && item.isMotionPhoto) ||
            ((qLower == "pano" || qLower == "panorama") && item.isPanorama)
        }
    }

    val displayedFavoritePhotos = remember(favoriteImages, fileSearchQuery) { filterMediaList(favoriteImages, fileSearchQuery) }
    val displayedMedia = remember(images, fileSearchQuery) { filterMediaList(images, fileSearchQuery) }
    val displayedPhotos = remember(images, fileSearchQuery) {
        val allPhotos = images.filterNot { it.isVideo }
        filterMediaList(allPhotos, fileSearchQuery)
    }
    val displayedVideos = remember(images, fileSearchQuery) {
        val allVideos = images.filter { it.isVideo }
        filterMediaList(allVideos, fileSearchQuery)
    }
    val displayedItems = remember(mediaFilter, displayedMedia, displayedPhotos, displayedVideos) {
        when (mediaFilter) {
            0 -> displayedMedia
            1 -> displayedPhotos
            2 -> displayedVideos
            else -> displayedMedia
        }
    }
    val effectiveAlbumMediaSort = selectedAlbum?.id?.let { albumMediaSortOverrides[it] } ?: albumMediaSort
    val displayedAlbumPhotos = remember(selectedAlbum, fileSearchQuery, effectiveAlbumMediaSort) {
        selectedAlbum?.let { album ->
            val filtered = filterMediaList(album.images, fileSearchQuery)
            when (effectiveAlbumMediaSort) {
                MediaSort.DATE_DESC -> filtered.sortedWith(compareByDescending<MediaImage> { it.dateTaken }.thenByDescending { it.dateModified }.thenByDescending { it.dateAdded }.thenByDescending { it.id })
                MediaSort.DATE_ASC -> filtered.sortedWith(compareBy<MediaImage> { it.dateTaken }.thenBy { it.dateModified }.thenBy { it.dateAdded }.thenBy { it.id })
                MediaSort.NAME_ASC -> filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                MediaSort.NAME_DESC -> filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
                MediaSort.SIZE_DESC -> filtered.sortedWith(compareByDescending<MediaImage> { it.sizeBytes }.thenByDescending { it.id })
                MediaSort.SIZE_ASC -> filtered.sortedWith(compareBy<MediaImage> { it.sizeBytes }.thenBy { it.id })
            }
        } ?: emptyList()
    }
    val activeMedia = when {
        destination == 3 && librarySection == "trash" -> trashed
        destination == 3 && librarySection == "locked" -> lockedMedia
        destination == 1 && selectedAlbum != null -> displayedAlbumPhotos
        destination == 2 -> displayedFavoritePhotos
        else -> displayedItems
    }
    val isLockedActive = destination == 3 && librarySection == "locked"
    DisposableEffect(isLockedActive) {
        val window = (context as? android.app.Activity)?.window
        if (isLockedActive) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }
    fun setSelection(id: Long, selected: Boolean) {
        selectedIds = if (selected) selectedIds + id else selectedIds - id
    }
    fun setDateSelection(ids: List<Long>, selected: Boolean) {
        selectedIds = if (selected) selectedIds + ids else selectedIds - ids.toSet()
    }
    fun clearSelection() { selectedIds = emptySet() }
    fun shareSelection() {
        val selected = activeMedia.filter { it.id in selectedIds }
        if (selected.isEmpty()) return
        tabScope.launch {
            val uris = prepareShareUris(context, selected, settings.secureSharingMode)
            if (uris.isEmpty()) {
                android.widget.Toast.makeText(context, context.getString(R.string.toast_share_failed), android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val mimeType = selected.map { it.mimeType.ifBlank { if (it.isVideo) "video/*" else "image/*" } }
                .distinct()
                .let { mimeTypes ->
                    when {
                        mimeTypes.size == 1 -> mimeTypes.first()
                        mimeTypes.all { it.startsWith("image/") } -> "image/*"
                        mimeTypes.all { it.startsWith("video/") } -> "video/*"
                        else -> "*/*"
                    }
                }
            val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    putExtra(Intent.EXTRA_TITLE, selected.first().name)
                } else {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
                clipData = ClipData.newUri(context.contentResolver, mimeType, uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share_media)))
        }
    }
    val availableAlbums = remember(images, albumCovers) {
        images.groupBy { it.bucketId }.map { (id, media) ->
            val samplePath = media.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
            val isSd = samplePath.isNotBlank() && !samplePath.startsWith("/storage/emulated/0") && !samplePath.startsWith("/data/")
            MediaAlbum(
                id = id,
                name = media.first().bucketName,
                cover = media.firstOrNull { it.id == albumCovers[id] } ?: media.first(),
                images = media,
                isSdCard = isSd,
                storageLabel = if (isSd) "SD Card" else ""
            )
        }.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
    }
    var albumPickerAction by remember { mutableStateOf<AlbumAction?>(null) }
    var pendingAlbumMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingDeleteItems by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingPermanentDeleteItems by remember { mutableStateOf<List<MediaImage>?>(null) }
    LaunchedEffect(trashFeedback) {
        if (trashFeedback != null) {
            kotlinx.coroutines.delay(1800)
            onTrashFeedbackChange(null)
        }
    }

    fun handleTrash(items: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
        if (items.isNotEmpty()) {
            onTrash(items, onConfirmed)
        }
    }
    fun handleRestore(items: List<MediaImage>) {
        if (items.isNotEmpty()) {
            onRestore(items)
        }
    }
    fun handleDeletePermanently(items: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
        if (items.isNotEmpty()) {
            onDeletePermanently(items, onConfirmed)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600
    val isDarkTheme = when (settings.themeMode) {
        com.iris.gallery.data.ThemeMode.LIGHT -> false
        com.iris.gallery.data.ThemeMode.DARK -> true
        com.iris.gallery.data.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val isAmoled = isDarkTheme && settings.amoledBlack

    val handleTabSelected: (Int) -> Unit = { index ->
        tabScope.launch {
            val current = tabPagerState.currentPage
            val targetTab = visibleTabs.getOrNull(index)
            if (isFileSearching) {
                isFileSearching = false
                fileSearchQuery = ""
            }
            if (current == index) {
                if (targetTab == GalleryTab.ALBUMS) selectedAlbumId = null
                if (targetTab == GalleryTab.LIBRARY) {
                    librarySection = null
                    selectedLockedAlbum = null
                }
            } else if (kotlin.math.abs(current - index) > 1) {
                tabPagerState.scrollToPage(index)
            } else {
                tabPagerState.animateScrollToPage(index, animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        }
    }

    val showFloatingBar = isLandscape && selectedIds.isEmpty() && selectedId == null && editorImage == null && activeOverlayScreen == null && externalMedia == null

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
          if (!(destination == 3 && librarySection == "folder_view")) {
            val canSearchOnScreen = destination == 0 || destination == 2 || (destination == 1 && selectedAlbum != null)
            TopAppBar(
                title = {
                    if (isFileSearching && canSearchOnScreen) {
                        OutlinedTextField(
                            value = fileSearchQuery,
                            onValueChange = { fileSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        when (mediaFilter) {
                                            0 -> R.string.search_files_placeholder
                                            1 -> R.string.search_photos_placeholder
                                            2 -> R.string.search_videos_placeholder
                                            else -> R.string.search_files_placeholder
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (fileSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { fileSearchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        val count = if (selectedIds.isNotEmpty()) stringResource(R.string.selected_count, selectedIds.size)
                            else when {
                                destination == 1 && selectedAlbum != null -> selectedAlbum!!.name
                                destination == 3 && librarySection == "trash" -> stringResource(R.string.section_trash)
                                destination == 3 && librarySection == "locked" -> if (selectedLockedAlbum != null) selectedLockedAlbum!! else stringResource(R.string.section_locked)
                                destination == 3 && librarySection == "duplicates" -> stringResource(R.string.section_duplicates)
                                destination == 3 && librarySection == "memories" -> stringResource(R.string.section_memories)
                                destination == 3 && librarySection == "formats" -> stringResource(R.string.library_formats_title)
                                destination == 3 && librarySection == "editor" -> stringResource(R.string.library_editor_title)
                                destination == 3 && librarySection == "folder_view" -> stringResource(R.string.section_folder_view)
                                destination == 3 && librarySection != null -> librarySection!!.replaceFirstChar { it.uppercase() }
                                destination == 2 -> favoritesTabLabel
                                destination == 1 -> albumsTabLabel
                                destination == 3 -> libraryTabLabel
                                else -> mediaTabLabel
                            }
                        if (selectedIds.isNotEmpty()) {
                            AnimatedContent(count, label = "selection count") { text ->
                                Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Text(count, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    if (isFileSearching && canSearchOnScreen) {
                        IconButton(onClick = { isFileSearching = false; fileSearchQuery = "" }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clear_search))
                        }
                    } else if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = ::clearSelection) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear_selection)) }
                    } else if (destination == 1 && selectedAlbum != null) {
                        IconButton(onClick = {
                            selectedAlbumId = null
                            if (isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
                        }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, albumsTabLabel) }
                    } else if (destination == 3 && librarySection == "locked" && selectedLockedAlbum != null) {
                        IconButton(onClick = { selectedLockedAlbum = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.section_locked)) }
                    } else if (destination == 3 && librarySection != null) {
                        IconButton(onClick = {
                            librarySection = null
                            selectedLockedAlbum = null
                            if (isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
                        }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, libraryTabLabel) }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        if (destination == 3 && librarySection == "trash") {
                            IconButton(onClick = { val selected = activeMedia.filter { it.id in selectedIds }; clearSelection(); handleRestore(selected) }) {
                                Icon(Icons.Outlined.RestoreFromTrash, stringResource(R.string.action_restore))
                            }
                        }
                        if (destination == 3 && librarySection == "locked") {
                            IconButton(onClick = { val selected = activeMedia.filter { it.id in selectedIds }; clearSelection(); onUnlockMedia(selected) }) {
                                Icon(Icons.Outlined.LockOpen, stringResource(R.string.action_remove_from_locked))
                            }
                        }
                        IconButton(onClick = ::shareSelection) { Icon(Icons.Outlined.Share, stringResource(R.string.action_share)) }
                        IconButton(onClick = {
                            val selected = activeMedia.filter { it.id in selectedIds }
                            if (selected.isNotEmpty()) {
                                if (destination == 3 && (librarySection == "trash" || librarySection == "locked")) {
                                    pendingDeleteItems = selected
                                } else if (settings.deleteMode == DeleteMode.ALWAYS_ASK || settings.confirmDelete) {
                                    pendingDeleteItems = selected
                                } else if (settings.deleteMode == DeleteMode.PERMANENT) {
                                    clearSelection()
                                    handleDeletePermanently(selected)
                                } else {
                                    clearSelection()
                                    handleTrash(selected)
                                }
                            }
                        }) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.action_delete)) }
                        Box {
                            IconButton(onClick = { selectionMenuExpanded = true }) {
                                Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more))
                            }
                            DropdownMenu(selectionMenuExpanded, onDismissRequest = { selectionMenuExpanded = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.action_select_all)) }, leadingIcon = { Icon(Icons.Outlined.SelectAll, null) },
                                    onClick = { selectionMenuExpanded = false; selectedIds = activeMedia.mapTo(mutableSetOf()) { it.id } })
                                if (destination == 1 && selectedAlbum != null && selectedIds.size == 1) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.action_set_album_cover)) }, leadingIcon = { Icon(Icons.Outlined.Wallpaper, null) },
                                        onClick = { selectionMenuExpanded = false; onSetAlbumCover(selectedAlbum!!.id, selectedIds.first()); clearSelection() })
                                }
                                if (!(destination == 3 && (librarySection == "trash" || librarySection == "locked"))) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_move_to_album)) },
                                        leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            val selected = activeMedia.filter { it.id in selectedIds }
                                            pendingAlbumMedia = selected
                                            albumPickerAction = AlbumAction.MOVE
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_copy_to_album)) },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            val selected = activeMedia.filter { it.id in selectedIds }
                                            pendingAlbumMedia = selected
                                            albumPickerAction = AlbumAction.COPY
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_delete_permanently), color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            val selected = activeMedia.filter { it.id in selectedIds }
                                            if (selected.isNotEmpty()) {
                                                pendingPermanentDeleteItems = selected
                                            }
                                        }
                                    )
                                }
                                if (destination == 3 && librarySection == "locked") {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.action_remove_from_locked)) }, leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            val selected = activeMedia.filter { it.id in selectedIds }
                                            clearSelection()
                                            onUnlockMedia(selected)
                                        })
                                } else if (!(destination == 3 && librarySection == "trash")) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.action_move_to_locked)) }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            val selected = activeMedia.filter { it.id in selectedIds }
                                            clearSelection()
                                            onLockMedia(selected)
                                        })
                                }
                                 val allSelectedAreFavorites = selectedIds.isNotEmpty() && selectedIds.all { it in favorites }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (allSelectedAreFavorites) R.string.action_unfavorite
                                                else R.string.action_favorite
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        if (allSelectedAreFavorites) {
                                            Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Icon(Icons.Outlined.FavoriteBorder, null)
                                        }
                                    },
                                    onClick = {
                                        selectionMenuExpanded = false
                                        val makeFavorite = !allSelectedAreFavorites
                                        selectedIds.forEach { id -> if ((id in favorites) != makeFavorite) onToggleFavorite(id) }
                                        clearSelection()
                                    }
                                )
                            }
                        }
                    } else if (!isFileSearching || !canSearchOnScreen) {
                        val canSearch = destination == 0 || destination == 2 || (destination == 1 && selectedAlbum != null)
                        if (canSearch && !isFileSearching) {
                            IconButton(onClick = { isFileSearching = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.action_search))
                            }
                        }
                        if (destination == 3 && librarySection == "trash" && trashed.isNotEmpty()) {
                            TextButton(onClick = { confirmEmptyTrash = true }) {
                                Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_empty_trash), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        val canToggleGrid = destination == 0 || destination == 2 || (destination == 1 && selectedAlbum != null) || (destination == 3 && (librarySection == "trash" || librarySection == "locked" || librarySection == "memories" || librarySection == "formats" || librarySection == "editor"))
                        if (canToggleGrid) {
                            IconButton(onClick = {
                                compactGrid = !compactGrid
                                customCellSize = if (compactGrid) 80.dp else 112.dp
                                settingsPreferences.setPhotoGridSize(customCellSize.value)
                            }) {
                                Icon(
                                    if (compactGrid) Icons.Outlined.GridView else Icons.Outlined.Dashboard,
                                    if (compactGrid) stringResource(R.string.action_comfortable_grid) else stringResource(R.string.action_compact_grid)
                                )
                            }
                        }
                        if (destination == 0) {
                            var mediaFilterMenuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { mediaFilterMenuExpanded = true }) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = stringResource(R.string.action_filter),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        modifier = Modifier.offset(y = (-4).dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = when (mediaFilter) {
                                                0 -> stringResource(R.string.tab_media)
                                                1 -> stringResource(R.string.tab_photos)
                                                2 -> stringResource(R.string.tab_videos)
                                                else -> stringResource(R.string.tab_media)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 6.sp,
                                                lineHeight = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = mediaFilterMenuExpanded,
                                onDismissRequest = { mediaFilterMenuExpanded = false }
                            ) {
                                listOf(
                                    stringResource(R.string.tab_media) to images.size,
                                    stringResource(R.string.tab_photos) to images.count { !it.isVideo },
                                    stringResource(R.string.tab_videos) to images.count { it.isVideo }
                                ).forEachIndexed { index, (title, count) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.filters, title, count)) },
                                        trailingIcon = if (mediaFilter == index) {
                                            { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                        } else null,
                                        onClick = {
                                            mediaFilterMenuExpanded = false
                                            mediaFilter = index
                                        }
                                    )
                                }
                            }
                        }
                        if (destination == 1 && selectedAlbum != null) {
                            val activeAlbum = selectedAlbum!!
                            val albumOverride = albumMediaSortOverrides[activeAlbum.id]
                            val effectiveSort = albumOverride ?: albumMediaSort
                            var albumSortMenuExpanded by remember { mutableStateOf(false) }
                            var applyToThisAlbumOnly by remember(albumSortMenuExpanded, activeAlbum.id, albumOverride != null) {
                                mutableStateOf(albumOverride != null)
                            }
                            Box {
                                IconButton(onClick = { albumSortMenuExpanded = true }) {
                                    Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.action_sort))
                                }
                                DropdownMenu(
                                    expanded = albumSortMenuExpanded,
                                    onDismissRequest = { albumSortMenuExpanded = false }
                                ) {
                                    val toggleApplyOnly = {
                                        val next = !applyToThisAlbumOnly
                                        applyToThisAlbumOnly = next
                                        if (next) {
                                            onSetAlbumMediaSortOverride(activeAlbum.id, effectiveSort)
                                        } else {
                                            onSetAlbumMediaSortOverride(activeAlbum.id, null)
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.sort_apply_this_album_only),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        trailingIcon = {
                                            Switch(
                                                checked = applyToThisAlbumOnly,
                                                onCheckedChange = null,
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        },
                                        onClick = toggleApplyOnly
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    listOf(
                                        MediaSort.DATE_DESC to R.string.sort_date_desc,
                                        MediaSort.DATE_ASC to R.string.sort_date_asc,
                                        MediaSort.NAME_ASC to R.string.sort_name_asc,
                                        MediaSort.NAME_DESC to R.string.sort_name_desc,
                                        MediaSort.SIZE_DESC to R.string.sort_size_desc,
                                        MediaSort.SIZE_ASC to R.string.sort_size_asc,
                                    ).forEach { (sortOption, labelRes) ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(labelRes)) },
                                            trailingIcon = if (effectiveSort == sortOption) {
                                                { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                            } else null,
                                            onClick = {
                                                albumSortMenuExpanded = false
                                                if (applyToThisAlbumOnly) {
                                                    onSetAlbumMediaSortOverride(activeAlbum.id, sortOption)
                                                } else {
                                                    onSetAlbumMediaSortOverride(activeAlbum.id, null)
                                                    onSetAlbumMediaSort(sortOption)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            var albumOptionsMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { albumOptionsMenuExpanded = true }) {
                                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more_options))
                                }
                                DropdownMenu(
                                    expanded = albumOptionsMenuExpanded,
                                    onDismissRequest = { albumOptionsMenuExpanded = false }
                                ) {
                                    val isPinned = selectedAlbum!!.id in pinnedAlbums
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_lock_album)) },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                        onClick = {
                                            albumOptionsMenuExpanded = false
                                            // Stay in the album until the lock actually happens. The
                                            // vault prompt and the system delete dialog can still be
                                            // cancelled, and the album drops out below by itself once
                                            // its images are filtered out.
                                            onLockMedia(selectedAlbum!!.images)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_exclude_folder)) },
                                        leadingIcon = { Icon(Icons.Outlined.FolderOff, null) },
                                        onClick = {
                                            albumOptionsMenuExpanded = false
                                            val samplePath = selectedAlbum!!.images.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
                                            val folderPath = if (samplePath.contains('/')) samplePath.substringBeforeLast('/') else ""
                                            if (folderPath.isNotBlank()) {
                                                onAddExcludedFolder(folderPath)
                                                Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                                                selectedAlbumId = null
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(if (isPinned) R.string.album_unpin else R.string.album_pin))
                                        },
                                        leadingIcon = {
                                            Icon(if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                                        },
                                        onClick = {
                                            albumOptionsMenuExpanded = false
                                            onTogglePinnedAlbum(selectedAlbum!!.id)
                                        }
                                    )
                                }
                            }
                        }
                        val inSubpage = (destination == 1 && selectedAlbum != null) || (destination == 3 && librarySection != null)
                        if (destination == 1 && selectedAlbum == null && albumSort == com.iris.gallery.data.AlbumSort.CUSTOM) {
                            IconButton(onClick = { isEditingAlbumOrder = !isEditingAlbumOrder }) {
                                Icon(
                                    if (isEditingAlbumOrder) Icons.Filled.Check else Icons.Outlined.Edit,
                                    contentDescription = stringResource(
                                        if (isEditingAlbumOrder) R.string.action_done_editing else R.string.action_edit_order
                                    ),
                                    tint = if (isEditingAlbumOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!inSubpage) {
                            IconButton(onClick = {
                                activeOverlayScreen = "settings"
                            }) {
                                Icon(Icons.Outlined.Settings, stringResource(R.string.section_settings))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
          }
        },
        bottomBar = {
          if (!isLandscape && selectedIds.isEmpty()) {
            Surface(
                color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        visibleTabs.forEachIndexed { index, tab ->
                            val isSelected = navVisibleTab == tab
                            val icon = if (tab == GalleryTab.MEDIA) {
                                when (mediaFilter) {
                                    0 -> Icons.Outlined.PermMedia
                                    1 -> Icons.Outlined.PhotoLibrary
                                    2 -> Icons.Outlined.VideoLibrary
                                    else -> Icons.Outlined.PermMedia
                                }
                            } else tab.icon
                            val label = if (tab == GalleryTab.MEDIA) mediaTabLabel else stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { handleTabSelected(index) },
                                icon = { AnimatedNavigationIcon(icon, isSelected, label) },
                                label = { Text(label) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                }
            }
          }
        },
    ) { scaffoldPadding ->
      val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      val floatingBarBottomPadding = 74.dp + navBarsBottom
      val layoutDirection = LocalLayoutDirection.current
      val padding = if (isLandscape && selectedIds.isEmpty()) {
          PaddingValues(
              start = scaffoldPadding.calculateStartPadding(layoutDirection),
              top = scaffoldPadding.calculateTopPadding(),
              end = scaffoldPadding.calculateEndPadding(layoutDirection),
              bottom = floatingBarBottomPadding
          )
      } else {
          scaffoldPadding
      }
      when {
        loading && images.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null && images.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(error) }
        else -> HorizontalPager(
          state = tabPagerState,
          beyondViewportPageCount = (visibleTabs.size - 1).coerceIn(0, 3),
          userScrollEnabled = selectedIds.isEmpty() && selectedAlbum == null && (destination != 3 || librarySection == null),
          modifier = Modifier.fillMaxSize(),
        ) { page ->
          val activeTab = visibleTabs.getOrElse(page) { GalleryTab.MEDIA }
          when (activeTab) {
            GalleryTab.MEDIA -> {
              val pullRefreshState0 = rememberPullToRefreshState()
              val fastRefreshConnection0 = remember(pullRefreshState0, loading, onRefresh) {
                object : NestedScrollConnection {
                  override suspend fun onPreFling(available: Velocity): Velocity {
                    if (available.y > 400f && pullRefreshState0.distanceFraction in 0.01f..0.999f && !loading) {
                      pullRefreshState0.animateToThreshold()
                      onRefresh()
                      return Velocity(0f, available.y)
                    }
                    return Velocity.Zero
                  }
                }
              }
              LaunchedEffect(loading) {
                if (!loading) {
                  pullRefreshState0.animateToHidden()
                }
              }
              PullToRefreshBox(
                state = pullRefreshState0,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().nestedScroll(fastRefreshConnection0),
                indicator = {
                  IrisPullToRefreshIndicator(
                    state = pullRefreshState0,
                    isRefreshing = loading,
                  )
                }
              ) {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                  if (displayedItems.isNotEmpty()) PhotoGrid(
                    displayedItems, padding, photoGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
                    showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
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
                    onToggleSelection = if (onPick == null) ::toggleSelection else null,
                    onSetSelection = if (onPick == null) ::setSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                    onInvalidMedia = onInvalidMedia,
                  ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = displayedItems; selectedId = it.id } }
                  else if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), padding)
                  else EmptyState(stringResource(R.string.empty_photos), padding)
                }
              }
            }
            GalleryTab.ALBUMS -> {
              val pullRefreshState1 = rememberPullToRefreshState()
              val fastRefreshConnection1 = remember(pullRefreshState1, loading, onRefresh) {
                object : NestedScrollConnection {
                  override suspend fun onPreFling(available: Velocity): Velocity {
                    if (available.y > 400f && pullRefreshState1.distanceFraction in 0.01f..0.999f && !loading) {
                      pullRefreshState1.animateToThreshold()
                      onRefresh()
                      return Velocity(0f, available.y)
                    }
                    return Velocity.Zero
                  }
                }
              }
              LaunchedEffect(loading) {
                if (!loading) {
                  pullRefreshState1.animateToHidden()
                }
              }
              PullToRefreshBox(
                state = pullRefreshState1,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().nestedScroll(fastRefreshConnection1),
                indicator = {
                  IrisPullToRefreshIndicator(
                    state = pullRefreshState1,
                    isRefreshing = loading,
                  )
                }
              ) {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                  if (selectedAlbum != null) {
                    if (displayedAlbumPhotos.isNotEmpty()) PhotoGrid(
                      displayedAlbumPhotos, padding, albumPhotoGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
                      showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
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
                      onToggleSelection = if (onPick == null) ::toggleSelection else null,
                      onSetSelection = if (onPick == null) ::setSelection else null,
                      onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                    ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = displayedAlbumPhotos; selectedId = it.id } }
                    else if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), padding)
                    else EmptyState(stringResource(R.string.empty_photos), padding)
                  } else AlbumsGrid(
                    images = images,
                    padding = padding,
                    state = albumGridState,
                    cellSize = customAlbumCellSize,
                    onCellSizeChange = onAlbumCellSizeChange,
                    cornerStyle = settings.cornerStyle,
                    gridSpacing = settings.gridSpacing,
                    showCount = settings.showAlbumCount,
                    pinned = pinnedAlbums,
                    covers = albumCovers,
                    sort = albumSort,
                    customOrder = albumOrder,
                    isEditingOrder = isEditingAlbumOrder && destination == 1 && selectedAlbumId == null && albumSort == com.iris.gallery.data.AlbumSort.CUSTOM,
                    onTogglePinned = onTogglePinnedAlbum,
                    onSortChanged = onSetAlbumSort,
                    onOrderChanged = onSetAlbumOrder,
                    onLockAlbum = { album -> onLockMedia(album.images) },
                    onExcludeFolder = { album ->
                        val samplePath = album.images.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
                        val folderPath = if (samplePath.contains('/')) samplePath.substringBeforeLast('/') else ""
                        if (folderPath.isNotBlank()) {
                            onAddExcludedFolder(folderPath)
                            Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                        }
                    },
                  ) { selectedAlbumId = it.id }
                }
              }
            }
            GalleryTab.LIBRARY -> {
                when (librarySection) {
                        "trash" -> if (trashed.isEmpty()) EmptyState(stringResource(R.string.empty_trash), padding) else PhotoGrid(
                            images = trashed,
                            padding = padding,
                            gridState = libraryGridState,
                            cellSize = photoCellSize,
                            onCellSizeChange = onCellSizeChange,
                            cornerStyle = settings.cornerStyle,
                            gridSpacing = settings.gridSpacing,
                            showVideoDuration = settings.showVideoDurationBadge,
                            showFormatBadge = settings.showMediaFormatBadge,
                            selectedIds = selectedIds,
                            favorites = favorites,
                            showFavoriteBadge = false,
                            onToggleSelection = ::toggleSelection,
                            onSetSelection = ::setSelection,
                        ) {
                            if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                            else {
                                viewerImages = trashed
                                selectedId = it.id
                            }
                        }
                        "locked" -> if (!lockedAuthorized) {
                            LaunchedEffect(Unit) { onRequestUnlock() }
                            EmptyState(stringResource(R.string.empty_locked_locked_state), padding)
                        } else {
                            val locked = lockedMedia
                            val lockedAlbumList = remember(locked) {
                                locked.groupBy { it.bucketName.ifBlank { "Locked" } }
                                    .map { (name, items) ->
                                        MediaAlbum(
                                            id = items.firstOrNull()?.bucketId ?: name.hashCode().toLong(),
                                            name = name,
                                            images = items,
                                            cover = items.first(),
                                            isSdCard = false
                                        )
                                    }.sortedWith(compareByDescending<MediaAlbum> { it.images.size }.thenBy { it.name.lowercase() })
                            }
                            if (locked.isEmpty()) {
                                EmptyState(stringResource(R.string.empty_locked), padding)
                            } else if (selectedLockedAlbum != null) {
                                val albumImages = remember(locked, selectedLockedAlbum) {
                                    locked.filter { it.bucketName.ifBlank { "Locked" } == selectedLockedAlbum }
                                }
                                PhotoGrid(
                                    images = albumImages,
                                    padding = padding,
                                    gridState = libraryGridState,
                                    cellSize = photoCellSize,
                                    onCellSizeChange = onCellSizeChange,
                                    cornerStyle = settings.cornerStyle,
                                    gridSpacing = settings.gridSpacing,
                                    showVideoDuration = settings.showVideoDurationBadge,
                                    showFormatBadge = settings.showMediaFormatBadge,
                                    selectedIds = selectedIds,
                                    favorites = favorites,
                                    onToggleSelection = ::toggleSelection,
                                    onSetSelection = ::setSelection,
                                ) {
                                    if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                    else {
                                        viewerImages = albumImages
                                        selectedId = it.id
                                    }
                                }
                            } else if (lockedAlbumList.size > 1) {
                                var vaultTab by remember { mutableIntStateOf(0) }
                                Column(Modifier.fillMaxSize().padding(padding)) {
                                    TabRow(
                                        selectedTabIndex = vaultTab,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Tab(
                                            selected = vaultTab == 0,
                                            onClick = { vaultTab = 0 },
                                            text = { Text(stringResource(R.string.vault_tab_albums) + " (${lockedAlbumList.size})") }
                                        )
                                        Tab(
                                            selected = vaultTab == 1,
                                            onClick = { vaultTab = 1 },
                                            text = { Text(stringResource(R.string.vault_tab_all_photos) + " (${locked.size})") }
                                        )
                                    }
                                    if (vaultTab == 0) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(140.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(settings.gridSpacing.dp.dp),
                                            verticalArrangement = Arrangement.spacedBy(settings.gridSpacing.dp.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(lockedAlbumList, key = { it.name }) { album ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = { selectedLockedAlbum = album.name },
                                                            onLongClick = { pendingRestoreAlbum = album }
                                                        )
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f)
                                                    ) {
                                                        Card(
                                                            shape = RoundedCornerShape(settings.cornerStyle.dp.dp),
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            MediaThumbnail(
                                                                album.cover,
                                                                modifier = Modifier.fillMaxSize(),
                                                                targetSizePx = 256
                                                            )
                                                        }
                                                    }
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = album.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.album_items_count, album.images.size),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        PhotoGrid(
                                            images = locked,
                                            padding = PaddingValues(0.dp),
                                            gridState = libraryGridState,
                                            cellSize = photoCellSize,
                                            onCellSizeChange = onCellSizeChange,
                                            cornerStyle = settings.cornerStyle,
                                            gridSpacing = settings.gridSpacing,
                                            showVideoDuration = settings.showVideoDurationBadge,
                                            showFormatBadge = settings.showMediaFormatBadge,
                                             selectedIds = selectedIds,
                                            favorites = favorites,
                                            onToggleSelection = ::toggleSelection,
                                            onSetSelection = ::setSelection,
                                        ) {
                                            if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                            else {
                                                viewerImages = locked
                                                selectedId = it.id
                                            }
                                        }
                                    }
                                }
                            } else {
                                PhotoGrid(
                                    images = locked,
                                    padding = padding,
                                    gridState = libraryGridState,
                                    cellSize = photoCellSize,
                                    onCellSizeChange = onCellSizeChange,
                                    cornerStyle = settings.cornerStyle,
                                    gridSpacing = settings.gridSpacing,
                                    showVideoDuration = settings.showVideoDurationBadge,
                                    showFormatBadge = settings.showMediaFormatBadge,
                                    selectedIds = selectedIds,
                                    favorites = favorites,
                                    onToggleSelection = ::toggleSelection,
                                    onSetSelection = ::setSelection,
                                ) {
                                    if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                    else {
                                        viewerImages = locked
                                        selectedId = it.id
                                    }
                                }
                            }
                        }
                        "memories" -> {
                            val today = LocalDate.now()
                            val memories = remember(images, today) { images.filter {
                                val date = Instant.ofEpochMilli(it.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
                                date.year < today.year && date.month == today.month && date.dayOfMonth == today.dayOfMonth
                            } }
                            Column(Modifier.fillMaxSize().padding(padding)) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !settings.dismissedMemoriesTip,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.memories_tip_title),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = stringResource(R.string.memories_tip_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                            IconButton(
                                                onClick = { settingsPreferences.setDismissedMemoriesTip(true) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = stringResource(R.string.action_close),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (memories.isEmpty()) EmptyState(stringResource(R.string.empty_memories), PaddingValues(0.dp)) else PhotoGrid(
                                    memories, PaddingValues(0.dp), libraryGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange, showTimeline = true,
                                    timelineDateFormat = settings.timelineDateFormat,
                                    customTimelineDateFormat = settings.customTimelineDateFormat,
                                    useRelativeDates = settings.useRelativeDates,
                                    showDayOfWeek = settings.showDayOfWeek,
                                    cornerStyle = settings.cornerStyle,
                                    gridSpacing = settings.gridSpacing,
                                    showVideoDuration = settings.showVideoDurationBadge,
                                    showFormatBadge = settings.showMediaFormatBadge,
                                    selectedIds = selectedIds,
                                    favorites = favorites,
                                    onToggleSelection = ::toggleSelection,
                                    onSetSelection = ::setSelection,
                                ) {
                                    if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                    else { viewerImages = memories; selectedId = it.id }
                                }
                            }
                        }
                        "formats" -> {
                            var formatFilter by remember { mutableStateOf(MediaFormatFilter.ALL) }
                            val allVideos = remember(images) { images.filter { it.isVideo } }
                            val allRaw = remember(images) { images.filter { it.isRaw } }
                            val allGifs = remember(images) { images.filter { it.isGif } }
                            val allPanos = remember(images) { images.filter { it.isPanorama } }
                            val allMotion = remember(images) { images.filter { it.isMotionPhoto } }
                            val allSpecial = remember(allVideos, allRaw, allGifs, allPanos, allMotion) {
                                (allVideos + allRaw + allGifs + allPanos + allMotion).distinctBy { it.id }.sortedByDescending { it.dateTaken }
                            }
                            val currentFiltered = when (formatFilter) {
                                MediaFormatFilter.ALL -> allSpecial
                                MediaFormatFilter.VIDEOS -> allVideos
                                MediaFormatFilter.RAW -> allRaw
                                MediaFormatFilter.GIF -> allGifs
                                MediaFormatFilter.PANORAMA -> allPanos
                                MediaFormatFilter.MOTION -> allMotion
                            }

                            if (allSpecial.isEmpty()) {
                                EmptyState(stringResource(R.string.empty_special_formats), padding)
                            } else {
                                Column(Modifier.fillMaxSize().padding(padding)) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.ALL,
                                                onClick = { formatFilter = MediaFormatFilter.ALL },
                                                label = { Text(stringResource(R.string.filter_all_count, allSpecial.size)) },
                                            )
                                        }
                                        if (allVideos.isNotEmpty()) item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.VIDEOS,
                                                onClick = { formatFilter = MediaFormatFilter.VIDEOS },
                                                label = { Text(stringResource(R.string.filter_videos_count, allVideos.size)) },
                                            )
                                        }
                                        if (allRaw.isNotEmpty()) item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.RAW,
                                                onClick = { formatFilter = MediaFormatFilter.RAW },
                                                label = { Text(stringResource(R.string.filter_raw_count, allRaw.size)) },
                                            )
                                        }
                                        if (allGifs.isNotEmpty()) item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.GIF,
                                                onClick = { formatFilter = MediaFormatFilter.GIF },
                                                label = { Text(stringResource(R.string.filter_gifs_count, allGifs.size)) },
                                            )
                                        }
                                        if (allPanos.isNotEmpty()) item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.PANORAMA,
                                                onClick = { formatFilter = MediaFormatFilter.PANORAMA },
                                                label = { Text(stringResource(R.string.filter_panoramas_count, allPanos.size)) },
                                            )
                                        }
                                        if (allMotion.isNotEmpty()) item {
                                            FilterChip(
                                                selected = formatFilter == MediaFormatFilter.MOTION,
                                                onClick = { formatFilter = MediaFormatFilter.MOTION },
                                                label = { Text(stringResource(R.string.filter_motion_count, allMotion.size)) },
                                            )
                                        }
                                    }
                                    if (currentFiltered.isEmpty()) {
                                        EmptyState(stringResource(R.string.empty_format_type, formatFilter.label), PaddingValues(0.dp))
                                    } else {
                                        PhotoGrid(
                                            images = currentFiltered,
                                            padding = PaddingValues(0.dp),
                                            gridState = libraryGridState,
                                            cellSize = photoCellSize,
                                            onCellSizeChange = onCellSizeChange,
                                            cornerStyle = settings.cornerStyle,
                                            gridSpacing = settings.gridSpacing,
                                            showVideoDuration = settings.showVideoDurationBadge,
                                             showFormatBadge = settings.showMediaFormatBadge,
                                            selectedIds = selectedIds,
                                            favorites = favorites,
                                            onToggleSelection = if (onPick == null) ::toggleSelection else null,
                                            onSetSelection = if (onPick == null) ::setSelection else null,
                                        ) {
                                            if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                            else if (onPick != null) onPick(it)
                                            else { viewerImages = currentFiltered; selectedId = it.id }
                                        }
                                    }
                                }
                            }
                        }
                        "editor" -> {
                            val editable = remember(images) { images.filterNot { it.isVideo } }
                            if (editable.isEmpty()) EmptyState(stringResource(R.string.empty_editable), padding)
                            else PhotoGrid(
                                editable, padding, libraryGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
                                cornerStyle = settings.cornerStyle,
                                gridSpacing = settings.gridSpacing,
                                showVideoDuration = settings.showVideoDurationBadge,
                                showFormatBadge = settings.showMediaFormatBadge,
                                favorites = favorites,
                            ) { editorImage = it }
                        }
                        "duplicates" -> DuplicateReviewScreen(
                            state = duplicateState,
                            padding = padding,
                            onScan = onScanDuplicates,
                            onCancel = onCancelDuplicateScan,
                            onOpen = { group, media -> viewerImages = group.items; selectedId = media.id },
                            onTrash = { pendingDeleteItems = it },
                        )
                        "folder_view" -> FolderBrowserScreen(
                            padding = padding,
                            cornerStyle = settings.cornerStyle,
                            gridSpacing = settings.gridSpacing,
                            timelineDateFormat = settings.timelineDateFormat,
                            customTimelineDateFormat = settings.customTimelineDateFormat,
                            favorites = favorites,
                            onOpenMedia = { media, folderMediaList ->
                                viewerImages = folderMediaList
                                selectedId = media.id
                            },
                            onBack = { librarySection = null }
                        )
                        else -> LibraryScreen(padding, trashed.size, lockedMedia.size) {
                            if (it == "rescan") {
                                onRescanMedia()
                            } else if (it == "excluded_folders") {
                                showExcludedFoldersDialog = true
                            } else {
                                librarySection = it
                            }
                        }
                    }
            }
            GalleryTab.FAVORITES -> {
                if (displayedFavoritePhotos.isEmpty()) {
                    if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), padding)
                    else EmptyState(stringResource(R.string.empty_favorites), padding)
                } else PhotoGrid(
                    images = displayedFavoritePhotos,
                    padding = padding,
                    gridState = favoriteGridState,
                    cellSize = photoCellSize,
                    onCellSizeChange = onCellSizeChange,
                    showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
                    timelineDateFormat = settings.timelineDateFormat,
                    customTimelineDateFormat = settings.customTimelineDateFormat,
                    useRelativeDates = settings.useRelativeDates,
                    showDayOfWeek = settings.showDayOfWeek,
                    cornerStyle = settings.cornerStyle,
                    gridSpacing = settings.gridSpacing,
                    showVideoDuration = settings.showVideoDurationBadge,
                    showFormatBadge = settings.showMediaFormatBadge,
                    selectedIds = selectedIds,
                    favorites = favorites,
                    showFavoriteBadge = false,
                    onToggleSelection = if (onPick == null) ::toggleSelection else null,
                    onSetSelection = if (onPick == null) ::setSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                    onInvalidMedia = onInvalidMedia,
                ) {
                    if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = displayedFavoritePhotos; selectedId = it.id }
                }
            }
          }
        }
      }
    }

    AnimatedVisibility(
        visible = showFloatingBar,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetY = { it }
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            targetOffsetY = { it }
        )
    ) {
        Surface(
            color = if (isAmoled) Color.Black.copy(alpha = 0.90f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isAmoled) 0.45f else 0.25f)
            ),
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.height(60.dp)
            ) {
                visibleTabs.forEachIndexed { index, tab ->
                    val isSelected = navVisibleTab == tab
                    val icon = if (tab == GalleryTab.MEDIA) {
                        when (mediaFilter) {
                            0 -> Icons.Outlined.PermMedia
                            1 -> Icons.Outlined.PhotoLibrary
                            2 -> Icons.Outlined.VideoLibrary
                            else -> Icons.Outlined.PermMedia
                        }
                    } else tab.icon
                    val label = if (tab == GalleryTab.MEDIA) mediaTabLabel else stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { handleTabSelected(index) },
                        icon = { AnimatedNavigationIcon(icon, isSelected, label) },
                        label = null,
                        alwaysShowLabel = false,
                    )
                }
            }
        }
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text(stringResource(R.string.empty_trash_dialog_title)) },
            text = { Text(stringResource(R.string.empty_trash_dialog_desc, trashed.size, if (trashed.size != 1) "s" else "")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmptyTrash = false
                    handleDeletePermanently(trashed)
                }) {
                    Text(stringResource(R.string.action_empty_trash), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyTrash = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showExcludedFoldersDialog) {
        com.iris.gallery.ui.ExcludedFoldersDialog(
            excludedFolders = excludedFolders,
            availableAlbums = availableAlbums,
            onRemoveExcludedFolder = onRemoveExcludedFolder,
            onAddExcludedFolder = onAddExcludedFolder,
            onDismissRequest = { showExcludedFoldersDialog = false }
        )
    }

    pendingRestoreAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { pendingRestoreAlbum = null },
            title = { Text(stringResource(R.string.dialog_restore_album_title)) },
            text = { Text(stringResource(R.string.dialog_restore_album_desc, album.images.size, album.name)) },
            confirmButton = {
                Button(onClick = {
                    val imagesToRestore = album.images
                    pendingRestoreAlbum = null
                    onUnlockMedia(imagesToRestore)
                }) {
                    Text(stringResource(R.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreAlbum = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingDeleteItems?.let { items ->
        val isInTrash = destination == 3 && librarySection == "trash"
        val isLockedSection = destination == 3 && librarySection == "locked"
        val isPermanentMode = isInTrash || isLockedSection || settings.deleteMode == DeleteMode.PERMANENT
        var deletePermanently by remember { mutableStateOf(isPermanentMode) }
        AlertDialog(
            onDismissRequest = { pendingDeleteItems = null },
            title = {
                Text(
                    if (isPermanentMode || deletePermanently) {
                        stringResource(R.string.delete_permanent_dialog_title, items.size)
                    } else if (items.size == 1) {
                        if (items[0].isVideo) stringResource(R.string.delete_trash_video_title)
                        else stringResource(R.string.delete_trash_photo_title)
                    } else {
                        stringResource(R.string.delete_multiple_trash_title, items.size)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isLockedSection) stringResource(R.string.delete_vault_desc)
                        else if (isPermanentMode || deletePermanently) stringResource(R.string.delete_permanent_desc)
                        else stringResource(R.string.delete_trash_desc)
                    )
                    if (!isInTrash && !isLockedSection && settings.deleteMode != DeleteMode.PERMANENT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deletePermanently = !deletePermanently }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = deletePermanently,
                                onCheckedChange = { deletePermanently = it }
                            )
                            Text(
                                text = stringResource(R.string.delete_permanently_checkbox),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = items
                    pendingDeleteItems = null
                    clearSelection()
                    if (isLockedSection) {
                        onDeleteFromLocked(toDelete) {
                            onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, toDelete.size))
                        }
                    } else if (isInTrash || deletePermanently) {
                        handleDeletePermanently(toDelete)
                    } else {
                        handleTrash(toDelete)
                    }
                }) {
                    Text(
                        if (isPermanentMode || deletePermanently) stringResource(R.string.action_delete_permanently)
                        else stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItems = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    pendingPermanentDeleteItems?.let { items ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDeleteItems = null },
            title = {
                Text(stringResource(R.string.delete_permanent_dialog_title, items.size))
            },
            text = {
                Text(stringResource(R.string.delete_permanent_desc))
            },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = items
                    pendingPermanentDeleteItems = null
                    clearSelection()
                    handleDeletePermanently(toDelete)
                }) {
                    Text(
                        stringResource(R.string.action_delete_permanently),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDeleteItems = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (albumPickerAction != null && pendingAlbumMedia != null) {
        val currentAlbumIdForMove = if (destination == 1 && selectedAlbum != null) selectedAlbum.id else null
        AlbumPickerSheet(
            action = albumPickerAction!!,
            selectedCount = pendingAlbumMedia!!.size,
            albums = availableAlbums,
            currentAlbumId = currentAlbumIdForMove,
            onDismiss = {
                albumPickerAction = null
                pendingAlbumMedia = null
            },
            onSelectAlbum = { album ->
                val action = albumPickerAction!!
                val toProcess = pendingAlbumMedia!!
                val targetDir = getAlbumDir(album)
                albumPickerAction = null
                pendingAlbumMedia = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(toProcess, targetDir, album.name) { result ->
                        if (result.successCount > 0) {
                            clearSelection()
                            onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, result.successCount, album.name))
                        } else {
                            Toast.makeText(context, R.string.toast_could_not_move_media, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    onCopyToAlbum(toProcess, targetDir, album.name) { result ->
                        if (result.successCount > 0) {
                            clearSelection()
                            onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, result.successCount, album.name))
                        } else {
                            Toast.makeText(context, R.string.toast_could_not_copy_media, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCreateAlbum = { newName ->
                val action = albumPickerAction!!
                val toProcess = pendingAlbumMedia!!
                val targetDir = createAlbumDir(newName)
                albumPickerAction = null
                pendingAlbumMedia = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(toProcess, targetDir, newName) { result ->
                        if (result.successCount > 0) {
                            clearSelection()
                            onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, result.successCount, newName))
                        } else {
                            Toast.makeText(context, R.string.toast_could_not_move_media, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    onCopyToAlbum(toProcess, targetDir, newName) { result ->
                        if (result.successCount > 0) {
                            clearSelection()
                            onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, result.successCount, newName))
                        } else {
                            Toast.makeText(context, R.string.toast_could_not_copy_media, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    BackHandler(enabled = !isFileSearching && selectedIds.isNotEmpty()) { clearSelection() }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum != null && !isFileSearching) {
        selectedAlbumId = null
        if (isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection == "locked" && selectedLockedAlbum != null && !isFileSearching) {
        selectedLockedAlbum = null
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection != null && (librarySection != "locked" || selectedLockedAlbum == null) && !isFileSearching) {
        librarySection = null
        selectedLockedAlbum = null
        if (isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
    }
    BackHandler(enabled = isFileSearching) {
        isFileSearching = false
        fileSearchQuery = ""
    }
    BackHandler(enabled = editorImage != null) { editorImage = null }
    BackHandler(enabled = externalMedia != null) { externalMedia = null }

    externalMedia?.let { media ->
        PhotoViewer(
            images = listOf(media),
            initialPage = 0,
            favorites = favorites,
            autoPlay = settings.autoPlayVideo,
            loop = settings.loopVideo,
            videoDoubleTapToZoom = settings.videoDoubleTapToZoom,
            videoGestureControls = settings.videoGestureControls,
            showViewerUserComments = settings.showViewerUserComments,
            viewerHeaderStyle = settings.viewerHeaderStyle,
            showViewerPageCount = settings.showViewerPageCount,
            showViewerTime = settings.showViewerTime,
            showFilmstrip = settings.showFilmstrip,
            dismissedFilmstripTip = settings.dismissedFilmstripTip,
            onDismissFilmstripTip = { settingsPreferences.setDismissedFilmstripTip(true) },
            pinchToRotate = settings.pinchToRotate,
            dismissedRotateTip = settings.dismissedRotateTip,
            onDismissRotateTip = { settingsPreferences.setDismissedRotateTip(true) },
            doubleTapZoomLevel = settings.doubleTapZoomLevel,
            multiStageZoom = settings.multiStageZoom,
            timelineDateFormat = settings.timelineDateFormat,
            customTimelineDateFormat = settings.customTimelineDateFormat,
            smartYearHiding = settings.smartYearHiding,
            isLocked = false,
            isInTrash = false,
            confirmDeleteSetting = settings.confirmDelete,
            deleteMode = settings.deleteMode,
            secureSharingMode = settings.secureSharingMode,
            onSetSecureSharingMode = { settingsPreferences.setSecureSharingMode(it) },
            dismissedSecureSharingTip = settings.dismissedSecureSharingTip,
            onDismissSecureSharingTip = { settingsPreferences.setDismissedSecureSharingTip(true) },
            videoMuted = settings.videoMuted,
            onSetVideoMuted = { settingsPreferences.setVideoMuted(it) },
            preferredEditor = settings.preferredEditor,
            onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
            availableAlbums = availableAlbums,
            onToggleFavorite = { },
            onClose = { externalMedia = null },
            onRename = { media, newName ->
                onRenameMedia(media, newName) { updated ->
                    if (updated != null) {
                        externalMedia = updated
                        Toast.makeText(context, context.getString(R.string.toast_rename_success, updated.name), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { item, _ ->
                externalMedia = null
                runCatching { context.contentResolver.delete(item.uri, null, null) }
            },
            onRestore = { },
            onEditMetadata = { media, request ->
                onEditMetadata(media, request) { updated ->
                    if (updated != null) {
                        externalMedia = updated
                    }
                }
            },
            onEdit = { editorImage = it; externalMedia = null },
            onLock = { },
            onUnlock = { },
            onMoveToAlbum = { mediaList, dir, name, onDone ->
                onMoveToAlbum(mediaList, dir, name) { result ->
                    if (result.successCount > 0) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, result.successCount, name))
                        externalMedia = null
                    } else {
                        Toast.makeText(context, R.string.toast_could_not_move_media, Toast.LENGTH_SHORT).show()
                    }
                    onDone(result)
                }
            },
            onCopyToAlbum = { mediaList, dir, name, onDone ->
                onCopyToAlbum(mediaList, dir, name) { result ->
                    if (result.successCount > 0) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, result.successCount, name))
                    } else {
                        Toast.makeText(context, R.string.toast_could_not_copy_media, Toast.LENGTH_SHORT).show()
                    }
                    onDone(result)
                }
            },
            getAlbumDir = getAlbumDir,
            createAlbumDir = createAlbumDir,
        )
    }

    selectedId?.let { id ->
        val activeImages = viewerImages ?: images
        val index = activeImages.indexOfFirst { it.id == id }
        val isViewingLocked = destination == 3 && librarySection == "locked"
        val isViewingTrash = destination == 3 && librarySection == "trash"
        if (index >= 0 && activeImages.isNotEmpty()) PhotoViewer(
            images = activeImages,
            initialPage = index,
            favorites = favorites,
            autoPlay = settings.autoPlayVideo,
            loop = settings.loopVideo,
            videoDoubleTapToZoom = settings.videoDoubleTapToZoom,
            videoGestureControls = settings.videoGestureControls,
            showViewerUserComments = settings.showViewerUserComments,
            viewerHeaderStyle = settings.viewerHeaderStyle,
            showViewerPageCount = settings.showViewerPageCount,
            showViewerTime = settings.showViewerTime,
            showFilmstrip = settings.showFilmstrip,
            dismissedFilmstripTip = settings.dismissedFilmstripTip,
            onDismissFilmstripTip = { settingsPreferences.setDismissedFilmstripTip(true) },
            pinchToRotate = settings.pinchToRotate,
            dismissedRotateTip = settings.dismissedRotateTip,
            onDismissRotateTip = { settingsPreferences.setDismissedRotateTip(true) },
            doubleTapZoomLevel = settings.doubleTapZoomLevel,
            multiStageZoom = settings.multiStageZoom,
            timelineDateFormat = settings.timelineDateFormat,
            customTimelineDateFormat = settings.customTimelineDateFormat,
            smartYearHiding = settings.smartYearHiding,
            isLocked = isViewingLocked,
            isInTrash = isViewingTrash,
            confirmDeleteSetting = settings.confirmDelete,
            deleteMode = settings.deleteMode,
            secureSharingMode = settings.secureSharingMode,
            onSetSecureSharingMode = { settingsPreferences.setSecureSharingMode(it) },
            dismissedSecureSharingTip = settings.dismissedSecureSharingTip,
            onDismissSecureSharingTip = { settingsPreferences.setDismissedSecureSharingTip(true) },
            videoMuted = settings.videoMuted,
            onSetVideoMuted = { settingsPreferences.setVideoMuted(it) },
            preferredEditor = settings.preferredEditor,
            onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
            availableAlbums = availableAlbums,
            onToggleFavorite = onToggleFavorite,
            onClose = { selectedId = null; viewerImages = null },
            onPageChanged = { selectedId = it },
            onRename = { media, newName ->
                onRenameMedia(media, newName) { updated ->
                    if (updated != null) {
                        viewerImages = viewerImages?.map { if (it.id == media.id) updated else it }
                        Toast.makeText(context, context.getString(R.string.toast_rename_success, updated.name), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { media, deletePermanently ->
                val onConfirmedAction = {
                    val activeList = viewerImages ?: images
                    val currentIndex = activeList.indexOfFirst { it.id == media.id }
                    val nextImage = if (activeList.size > 1 && currentIndex >= 0) {
                        if (currentIndex < activeList.size - 1) activeList[currentIndex + 1]
                        else activeList[currentIndex - 1]
                    } else null

                    selectedId = nextImage?.id
                    viewerImages = viewerImages?.filterNot { it.id == media.id }
                }

                if (isViewingTrash || deletePermanently) {
                    handleDeletePermanently(listOf(media), onConfirmedAction)
                } else if (isViewingLocked) {
                    onDeleteFromLocked(listOf(media)) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, 1))
                        onConfirmedAction()
                    }
                } else {
                    handleTrash(listOf(media), onConfirmedAction)
                }
            },
            onRestore = { media ->
                handleRestore(listOf(media))
            },
            onEditMetadata = { media, request ->
                onEditMetadata(media, request) { updated ->
                    if (updated != null) {
                        viewerImages = viewerImages?.map { if (it.id == media.id) updated else it }
                    }
                }
            },
            onEdit = { editorImage = it; selectedId = null },
            onLock = { onLockMedia(listOf(it)); selectedId = null },
            onUnlock = { onUnlockMedia(listOf(it)); selectedId = null },
            onMoveToAlbum = { mediaList, dir, name, onDone ->
                onMoveToAlbum(mediaList, dir, name) { result ->
                    if (result.successCount > 0) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, result.successCount, name))
                    } else {
                        Toast.makeText(context, R.string.toast_could_not_move_media, Toast.LENGTH_SHORT).show()
                    }
                    onDone(result)
                }
            },
            onCopyToAlbum = { mediaList, dir, name, onDone ->
                onCopyToAlbum(mediaList, dir, name) { result ->
                    if (result.successCount > 0) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, result.successCount, name))
                    } else {
                        Toast.makeText(context, R.string.toast_could_not_copy_media, Toast.LENGTH_SHORT).show()
                    }
                    onDone(result)
                }
            },
            getAlbumDir = getAlbumDir,
            createAlbumDir = createAlbumDir,
        )
    }
    AnimatedVisibility(
        visible = editorImage != null,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it / 10 },
            animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    ) {
        editorImage?.let { image ->
            EditorScreen(image, onClose = { editorImage = null }, onSaved = { saved, overwritten ->
                if (saved) editorImage = null
                val msg = if (!saved) {
                    context.getString(R.string.toast_edited_failed)
                } else if (overwritten) {
                    context.getString(R.string.toast_edited_overwritten)
                } else {
                    context.getString(R.string.toast_edited_saved)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            })
        }
    }

        AnimatedVisibility(
            visible = trashFeedback != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (selectedId != null) 96.dp else if (selectedIds.isEmpty()) 88.dp else 24.dp),
            enter = fadeIn(tween(160)) + slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it / 2 }
            ) + scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.82f
            ),
            exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 2 }
            ) + scaleOut(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetScale = 0.85f
            ),
        ) {
            trashFeedback?.let { feedback ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = when (feedback.type) {
                            TrashFeedbackType.MOVED_TO_TRASH -> Icons.Outlined.DeleteOutline
                            TrashFeedbackType.RESTORED -> Icons.Outlined.RestoreFromTrash
                            TrashFeedbackType.PERMANENTLY_DELETED -> Icons.Outlined.DeleteForever
                            TrashFeedbackType.MOVED_TO_ALBUM -> Icons.Outlined.DriveFileMove
                            TrashFeedbackType.COPIED_TO_ALBUM -> Icons.Outlined.ContentCopy
                        }
                        val tint = when (feedback.type) {
                            TrashFeedbackType.MOVED_TO_TRASH -> MaterialTheme.colorScheme.primary
                            TrashFeedbackType.RESTORED -> MaterialTheme.colorScheme.primary
                            TrashFeedbackType.PERMANENTLY_DELETED -> MaterialTheme.colorScheme.error
                            TrashFeedbackType.MOVED_TO_ALBUM -> MaterialTheme.colorScheme.primary
                            TrashFeedbackType.COPIED_TO_ALBUM -> MaterialTheme.colorScheme.primary
                        }
                        val message = when (feedback.type) {
                            TrashFeedbackType.MOVED_TO_TRASH -> {
                                if (feedback.count == 1) stringResource(R.string.toast_item_moved_to_trash)
                                else stringResource(R.string.toast_items_moved_to_trash, feedback.count)
                            }
                            TrashFeedbackType.RESTORED -> {
                                if (feedback.count == 1) stringResource(R.string.toast_item_restored_from_trash)
                                else stringResource(R.string.toast_items_restored_from_trash, feedback.count)
                            }
                            TrashFeedbackType.PERMANENTLY_DELETED -> {
                                if (feedback.count == 1) stringResource(R.string.toast_item_permanently_deleted)
                                else stringResource(R.string.toast_items_permanently_deleted, feedback.count)
                            }
                            TrashFeedbackType.MOVED_TO_ALBUM -> {
                                if (feedback.count == 1) stringResource(R.string.toast_moved_to_album_single, feedback.albumName)
                                else stringResource(R.string.toast_moved_to_album_multiple, feedback.count, feedback.albumName)
                            }
                            TrashFeedbackType.COPIED_TO_ALBUM -> {
                                if (feedback.count == 1) stringResource(R.string.toast_copied_to_album_single, feedback.albumName)
                                else stringResource(R.string.toast_copied_to_album_multiple, feedback.count, feedback.albumName)
                            }
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeOverlayScreen == "settings",
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) + slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it / 10 },
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        ) {
            BackHandler(enabled = true) { activeOverlayScreen = null }
            SettingsScreen(
                settings = settings,
                preferences = settingsPreferences,
                excludedFolders = excludedFolders,
                availableAlbums = availableAlbums,
                onRemoveExcludedFolder = onRemoveExcludedFolder,
                onAddExcludedFolder = onAddExcludedFolder,
                onOpenAbout = { activeOverlayScreen = "about" },
                onRescanMedia = onRescanMedia,
                onBack = { activeOverlayScreen = null }
            )
        }

        AnimatedVisibility(
            visible = activeOverlayScreen == "about",
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) + slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it / 10 },
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        ) {
            BackHandler(enabled = true) { activeOverlayScreen = "settings" }
            AboutScreen(
                onBack = { activeOverlayScreen = "settings" }
            )
        }
    }
}

@Composable
private fun AnimatedNavigationIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    label: String,
) {
    val scale by animateFloatAsState(
        if (selected) 1.16f else 1f,
        tween(260, easing = FastOutSlowInEasing),
        label = "navigation icon scale",
    )
    val rotation by animateFloatAsState(
        if (selected) 0f else -7f,
        tween(300, easing = FastOutSlowInEasing),
        label = "navigation icon rotation",
    )
    Icon(icon, label, Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        rotationZ = rotation
    })
}

@Composable
private fun EmptyState(message: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
