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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.CreateNewFolder
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.iris.gallery.data.NavBarStyle
import com.iris.gallery.data.NavLabelMode
import com.iris.gallery.data.ALL_SELECTION_PILL_ACTIONS
import com.iris.gallery.data.DEFAULT_SELECTION_PILL_ACTIONS
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
import com.iris.gallery.data.AlbumGroup
import com.iris.gallery.data.AlbumOperationResult
import com.iris.gallery.data.AlbumPickerSort
import com.iris.gallery.data.AlbumSort
import com.iris.gallery.data.AlbumsViewMode
import com.iris.gallery.data.PhotoGridLayout
import com.iris.gallery.data.expandPairedRaw
import com.iris.gallery.data.timelineTimestamp
import com.iris.gallery.ui.AlbumPickerSheet
import com.iris.gallery.R
import java.io.File

internal fun filterMediaList(list: List<MediaImage>, query: String): List<MediaImage> {
    if (query.isBlank()) return list
    val q = query.trim()
    val qLower = q.lowercase()
    fun matchesSingle(item: MediaImage): Boolean {
        return item.name.contains(q, ignoreCase = true) ||
            item.title.contains(q, ignoreCase = true) ||
            item.bucketName.contains(q, ignoreCase = true) ||
            item.path.contains(q, ignoreCase = true) ||
            ((qLower == "video" || qLower == "videos" || qLower == ".mp4" || qLower == ".mkv" || qLower == ".mov" || qLower == ".webm" || qLower == ".3gp") && item.isVideo) ||
            ((qLower == "photo" || qLower == "photos" || qLower == "image" || qLower == "images" || qLower == ".jpg" || qLower == ".jpeg" || qLower == ".png" || qLower == ".webp" || qLower == ".heic") && !item.isVideo) ||
            ((qLower == "gif" || qLower == "gifs" || qLower == ".gif") && item.isGif) ||
            ((qLower == "raw" || qLower == ".dng" || qLower == ".cr2" || qLower == ".cr3" || qLower == ".nef" || qLower == ".arw") && item.isRaw) ||
            ((qLower == "motion" || qLower == "live") && item.isMotionPhoto) ||
            ((qLower == "pano" || qLower == "panorama") && item.isPanorama)
    }
    return list.filter { item ->
        matchesSingle(item) || (item.pairedRaw != null && matchesSingle(item.pairedRaw))
    }
}

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
    var isDismissing by remember { mutableStateOf(false) }
    val dismissScale = remember { Animatable(1f) }
    val dismissAlpha = remember { Animatable(1f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { PullToRefreshDefaults.PositionalThreshold.toPx() }

    // Synchronous bridge on the exact frame isRefreshing flips true -> false so there is
    // zero 1-frame flash of the pull arrow or upward jump before LaunchedEffect runs.
    val dismissingNow = isDismissing || (wasRefreshing && !isRefreshing)
    val currentDismissingNow by androidx.compose.runtime.rememberUpdatedState(dismissingNow)

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            wasRefreshing = true
            isDismissing = false
            dismissScale.snapTo(1f)
            dismissAlpha.snapTo(1f)
        } else if (wasRefreshing) {
            isDismissing = true
            wasRefreshing = false
            try {
                // Shrink smoothly to 0 around the circle's center while holding opacity crisp
                // early in the curve so the indicator scales away cleanly instead of vanishing early.
                kotlinx.coroutines.coroutineScope {
                    launch {
                        dismissScale.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        )
                    }
                    launch {
                        dismissAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 280,
                                easing = androidx.compose.animation.core.FastOutLinearInEasing
                            )
                        )
                    }
                }
                // Cancel any lingering Material3 spring so the arrow never flashes back at the top edge.
                state.snapTo(0f)
            } finally {
                isDismissing = false
                dismissScale.snapTo(1f)
                dismissAlpha.snapTo(1f)
            }
        }
    }

    val effectiveState = remember(state) {
        object : PullToRefreshState {
            override val distanceFraction: Float
                get() = if (currentDismissingNow) 1f else state.distanceFraction
            override val isAnimating: Boolean
                get() = if (currentDismissingNow) false else state.isAnimating
            override suspend fun animateToThreshold() = state.animateToThreshold()
            override suspend fun animateToHidden() = state.animateToHidden()
            override suspend fun snapTo(targetValue: Float) = state.snapTo(targetValue)
        }
    }

    PullToRefreshDefaults.Indicator(
        state = effectiveState,
        isRefreshing = isRefreshing || dismissingNow,
        modifier = modifier
            .align(Alignment.TopCenter)
            .graphicsLayer {
                if (dismissingNow) {
                    val pivotY = if (size.height > 0f) {
                        (thresholdPx - size.height / 2f) / size.height
                    } else {
                        1.5f
                    }
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, pivotY)
                    scaleX = dismissScale.value
                    scaleY = dismissScale.value
                    alpha = dismissAlpha.value
                }
            }
    )
}

private enum class GalleryTab(
    val destination: Int,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    MEDIA(0, R.string.tab_media, Icons.Outlined.PhotoLibrary),
    ALBUMS(1, R.string.tab_albums, Icons.Outlined.PhotoAlbum),
    FAVORITES(2, R.string.tab_favorites, Icons.Outlined.FavoriteBorder),
    LIBRARY(3, R.string.tab_library, Icons.Outlined.Dashboard),
    VAULT(4, R.string.tab_vault, Icons.Outlined.Lock),
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
    libraryOrder: List<String> = com.iris.gallery.data.DEFAULT_LIBRARY_ORDER,
    albumMediaSort: com.iris.gallery.data.MediaSort = com.iris.gallery.data.MediaSort.DATE_DESC,
    albumMediaSortOverrides: Map<Long, com.iris.gallery.data.MediaSort> = emptyMap(),
    albumGroups: List<AlbumGroup> = emptyList(),
    vaultAlbumSort: AlbumSort = AlbumSort.NEWEST,
    vaultMediaSort: MediaSort = MediaSort.DATE_DESC,
    lockedAuthorized: Boolean,
    onLockVault: () -> Unit = {},
    loading: Boolean,
    error: String?,
    favorites: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onLockMedia: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
    onUnlockMedia: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
    onDeleteFromLocked: (List<MediaImage>, () -> Unit) -> Unit,
    onTogglePinnedAlbum: (Long) -> Unit,
    onSetAlbumCover: (Long, Long) -> Unit,
    onSetAlbumSort: (com.iris.gallery.data.AlbumSort) -> Unit,
    onSetAlbumOrder: (List<Long>) -> Unit,
    onSetLibraryOrder: (List<String>) -> Unit = {},
    onSetAlbumMediaSort: (com.iris.gallery.data.MediaSort) -> Unit = {},
    onSetAlbumMediaSortOverride: (Long, com.iris.gallery.data.MediaSort?) -> Unit = { _, _ -> },
    onSetVaultAlbumSort: (AlbumSort) -> Unit = {},
    onSetVaultMediaSort: (MediaSort) -> Unit = {},
    onCreateAlbumGroup: (String, List<Long>) -> Unit = { _, _ -> },
    onRenameAlbumGroup: (String, String) -> Unit = { _, _ -> },
    onSetAlbumGroupCover: (String, Long?) -> Unit = { _, _ -> },
    onDeleteAlbumGroup: (String) -> Unit = {},
    onSetAlbumGroupMembers: (String, List<Long>) -> Unit = { _, _ -> },
    onAddAlbumToGroup: (String, Long) -> Unit = { _, _ -> },
    onRemoveAlbumFromGroup: (String, Long) -> Unit = { _, _ -> },
    onToggleShowHiddenItems: (Boolean) -> Unit = {},
    onRequestUnlock: () -> Unit,
    onPick: ((MediaImage) -> Unit)?,
    onTrash: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
    onRestore: (List<MediaImage>, onConfirmed: (() -> Unit)?) -> Unit,
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
    onExportBackupJson: () -> String = { "" },
    onParseBackupPreview: (String) -> Result<com.iris.gallery.data.BackupPreview> = { Result.failure(IllegalStateException("Backup unavailable")) },
    onImportBackupJson: suspend (String) -> Result<com.iris.gallery.data.BackupRestoreSummary> = { Result.failure(IllegalStateException("Backup unavailable")) },
    onRunAutoBackupNow: suspend () -> Result<String> = { Result.failure(IllegalStateException("Backup unavailable")) },
    vaultFileCount: Int = 0,
    vaultTotalBytes: Long = 0L,
    onExportVaultBackup: suspend (Uri, CharArray, (Int, Int) -> Unit) -> Result<com.iris.gallery.data.VaultBackupExportResult> = { _, _, _ -> Result.failure(IllegalStateException("Vault backup unavailable")) },
    onImportVaultBackup: suspend (Uri, CharArray, (Int, Int) -> Unit) -> Result<com.iris.gallery.data.VaultBackupImportResult> = { _, _, _ -> Result.failure(IllegalStateException("Vault backup unavailable")) },
    onRenameMedia: (MediaImage, String, (MediaImage?) -> Unit) -> Unit = { _, _, _ -> },
    excludedFolders: Set<String> = emptySet(),
    onAddExcludedFolder: (String) -> Unit = {},
    onRemoveExcludedFolder: (String) -> Unit = {},
    initialMemories: Boolean,
    initialViewUri: Uri? = null,
    initialEditMode: Boolean = false,
    initialOpenLatest: Boolean = false,
    initialIntentGeneration: Long = 0L,
    onEnterVault: () -> Unit = {},
    onInvalidMedia: ((MediaImage) -> Unit)? = null,
) {
    val context = LocalContext.current
    val visibleTabs = remember(settings.showMediaTab, settings.showAlbumsTab, settings.showFavoritesTab, settings.showLibraryTab, settings.showVaultTab) {
        val list = mutableListOf<GalleryTab>()
        if (settings.showMediaTab) list.add(GalleryTab.MEDIA)
        if (settings.showAlbumsTab) list.add(GalleryTab.ALBUMS)
        if (settings.showFavoritesTab) list.add(GalleryTab.FAVORITES)
        if (settings.showLibraryTab) list.add(GalleryTab.LIBRARY)
        if (settings.showVaultTab) list.add(GalleryTab.VAULT)
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
            StartupTab.VAULT -> if (settings.showVaultTab) GalleryTab.VAULT else GalleryTab.MEDIA
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
    var selectedAlbumGroupId by remember { mutableStateOf<String?>(null) }
    var selectedFolderClusterPath by remember { mutableStateOf<String?>(null) }
    var showAlbumsViewOptionsDialog by remember { mutableStateOf(false) }
    var showCreateAlbumGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupMembersDialog by remember { mutableStateOf(false) }
    var showGroupCoverPickerSheet by remember { mutableStateOf(false) }
    var pendingDeleteAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var selectedLockedAlbum by remember { mutableStateOf<String?>(null) }
    var vaultTab by remember { mutableIntStateOf(0) }
    var vaultSearchQuery by remember { mutableStateOf("") }
    var isVaultSearching by remember { mutableStateOf(false) }
    val vaultSearchFocus = remember { FocusRequester() }
    var vaultSortMenuExpanded by remember { mutableStateOf(false) }
    var librarySection by remember { mutableStateOf<String?>(if (initialMemories) "memories" else null) }
    var editorImage by remember { mutableStateOf<MediaImage?>(null) }
    var activeOverlayScreen by remember { mutableStateOf<String?>(null) }
    var viewerSessionId by remember { mutableStateOf(0L) }
    var isEditingAlbumOrder by remember { mutableStateOf(false) }
    var isEditingLibraryOrder by remember { mutableStateOf(false) }
    val boundsResolvers = remember { mutableMapOf<String, (Long) -> androidx.compose.ui.geometry.Rect?>() }
    var activeViewerSourceKey by remember { mutableStateOf<String?>(null) }

    val isVaultActive = destination == 4 || (destination == 3 && librarySection == "locked")

    LaunchedEffect(isVaultActive) {
        if (isVaultActive) {
            onEnterVault()
            if (!lockedAuthorized) {
                onRequestUnlock()
            }
        } else {
            onLockVault()
            selectedLockedAlbum = null
            if (isVaultSearching) {
                isVaultSearching = false
                vaultSearchQuery = ""
            }
            vaultSortMenuExpanded = false
        }
    }

    LaunchedEffect(destination, librarySection, selectedAlbumId) {
        if (destination != 3 || librarySection != null) {
            isEditingLibraryOrder = false
        }
        if (destination != 1 || selectedAlbumId != null) {
            isEditingAlbumOrder = false
        }
    }

    var handledIntentGeneration by remember { mutableStateOf(-1L) }
    // A camera app opens its newest shot before the library knows about it: the cached
    // snapshot shown at startup and the MediaStore observer both lag behind. Keep the request
    // so the standalone viewer can be swapped for the regular one (swipe through the library,
    // delete to trash) as soon as the item shows up in [images].
    var pendingExternalUri by remember { mutableStateOf<Uri?>(null) }
    var initialShowRawVariant by remember { mutableStateOf(false) }
    fun matchesUriOrPath(item: MediaImage, uri: Uri, path: String, isMediaStoreUri: Boolean): Boolean {
        return item.uri == uri ||
            (path.isNotBlank() && item.path == path) ||
            (isMediaStoreUri && !uri.lastPathSegment.isNullOrBlank() && item.uri.lastPathSegment == uri.lastPathSegment)
    }
    fun findInLibrary(uri: Uri, path: String): Pair<MediaImage, Boolean>? {
        val isMediaStoreUri = uri.authority == android.provider.MediaStore.AUTHORITY
        for (item in images) {
            if (matchesUriOrPath(item, uri, path, isMediaStoreUri)) return item to false
            val raw = item.pairedRaw
            if (raw != null && matchesUriOrPath(raw, uri, path, isMediaStoreUri)) return item to true
        }
        return null
    }
    LaunchedEffect(initialViewUri, initialEditMode, initialOpenLatest, images, initialIntentGeneration) {
        if (handledIntentGeneration != initialIntentGeneration) {
            pendingExternalUri = null
            initialShowRawVariant = false
            if (initialViewUri != null) {
                val resolved = withContext(Dispatchers.IO) { resolveMediaUri(context, initialViewUri) }
                // Mark as handled only after resolving: if [images] changes meanwhile, this effect
                // is restarted and must not skip the intent.
                handledIntentGeneration = initialIntentGeneration
                activeOverlayScreen = null
                val found = findInLibrary(initialViewUri, resolved.path)
                val matched = found?.first
                val matchedRawSide = found?.second == true
                if (matched != null) {
                    if (initialEditMode) {
                        editorImage = if (matchedRawSide && matched.pairedRaw != null) matched.pairedRaw else matched
                        selectedId = null
                        externalMedia = null
                        viewerImages = null
                        activeViewerSourceKey = null
                    } else {
                        editorImage = null
                        viewerImages = null
                        activeViewerSourceKey = "external_intent"
                        initialShowRawVariant = matchedRawSide
                        selectedId = matched.id
                        externalMedia = null
                        viewerSessionId += 1
                    }
                } else {
                    if (initialEditMode) {
                        editorImage = resolved
                        selectedId = null
                        externalMedia = null
                        viewerImages = null
                        activeViewerSourceKey = null
                    } else {
                        editorImage = null
                        externalMedia = resolved
                        selectedId = null
                        viewerImages = null
                        activeViewerSourceKey = "external_intent"
                        pendingExternalUri = initialViewUri
                        viewerSessionId += 1
                    }
                }
            } else if (initialOpenLatest) {
                if (images.isNotEmpty()) {
                    handledIntentGeneration = initialIntentGeneration
                    activeOverlayScreen = null
                    editorImage = null
                    viewerImages = null
                    activeViewerSourceKey = "external_intent"
                    selectedId = images.first().id
                    externalMedia = null
                    viewerSessionId += 1
                }
            } else {
                handledIntentGeneration = initialIntentGeneration
            }
        } else {
            val pendingUri = pendingExternalUri
            if (pendingUri != null) {
                val shown = externalMedia
                if (shown == null) {
                    // The standalone viewer was closed (or left for the editor): nothing to swap.
                    pendingExternalUri = null
                } else {
                    val found = findInLibrary(pendingUri, shown.path)
                    if (found != null) {
                        pendingExternalUri = null
                        viewerImages = null
                        activeViewerSourceKey = "external_intent"
                        initialShowRawVariant = found.second
                        selectedId = found.first.id
                        externalMedia = null
                        viewerSessionId += 1
                    }
                }
            }
        }
    }
    var customCellSize by remember { mutableStateOf(settings.photoGridSize.dp) }
    var customMosaicRowSize by remember { mutableStateOf(settings.mosaicRowSize.dp) }
    var customAlbumCellSize by remember { mutableStateOf(settings.albumGridSize.dp) }
    val mediaFilter = settings.mediaFilter

    LaunchedEffect(settings.photoGridSize) {
        customCellSize = settings.photoGridSize.dp
    }
    LaunchedEffect(settings.mosaicRowSize) {
        customMosaicRowSize = settings.mosaicRowSize.dp
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
        settingsPreferences.setPhotoGridSize(newSize.value)
    }
    val mosaicRowSize = customMosaicRowSize
    val onMosaicRowSizeChange: (androidx.compose.ui.unit.Dp) -> Unit = { newSize ->
        customMosaicRowSize = newSize
        settingsPreferences.setMosaicRowSize(newSize.value)
    }
    val isMosaicLayout = settings.photoGridLayout == com.iris.gallery.data.PhotoGridLayout.MOSAIC
    val activeGridCellSize = if (isMosaicLayout) mosaicRowSize else photoCellSize
    val activeOnCellSizeChange = if (isMosaicLayout) onMosaicRowSizeChange else onCellSizeChange
    val onAlbumCellSizeChange: (androidx.compose.ui.unit.Dp) -> Unit = { newSize ->
        customAlbumCellSize = newSize
        settingsPreferences.setAlbumGridSize(newSize.value)
    }
    val favoriteImages = remember(images, favorites) {
        images.filter { it.id in favorites || (it.pairedRaw != null && it.pairedRaw.id in favorites) }
    }
    val allHierarchyAlbums = remember(images, albumCovers, settings.albumHierarchyDepth) {
        buildMediaAlbums(images, albumCovers, settings.albumHierarchyDepth)
    }
    val selectedAlbum = remember(allHierarchyAlbums, selectedAlbumId) {
        selectedAlbumId?.let { albumId -> allHierarchyAlbums.firstOrNull { it.id == albumId } }
    }
    val activeAlbumGroup = remember(albumGroups, selectedAlbumGroupId) {
        selectedAlbumGroupId?.let { id -> albumGroups.firstOrNull { it.id == id } }
    }
    val activeFolderCluster = remember(allHierarchyAlbums, pinnedAlbums, selectedFolderClusterPath) {
        selectedFolderClusterPath?.let { clusterId ->
            groupAlbumsIntoSubfolderClusters(allHierarchyAlbums, pinnedAlbums)
                .filterIsInstance<AlbumDisplayItem.SubfolderGroup>()
                .firstOrNull { it.cluster.id == clusterId }
                ?.cluster
        }
    }
    LaunchedEffect(activeAlbumGroup, selectedAlbumGroupId) {
        if (selectedAlbumGroupId != null && activeAlbumGroup == null) {
            selectedAlbumGroupId = null
        }
    }
    LaunchedEffect(activeFolderCluster, selectedFolderClusterPath) {
        if (selectedFolderClusterPath != null && activeFolderCluster == null) {
            selectedFolderClusterPath = null
        }
    }
    var fileSearchQuery by remember { mutableStateOf("") }
    var isFileSearching by remember { mutableStateOf(false) }
    var albumSearchQuery by remember { mutableStateOf("") }
    var isAlbumSearching by remember { mutableStateOf(false) }
    var albumSortMenuExpanded by remember { mutableStateOf(false) }
    val fileSearchFocus = remember { FocusRequester() }
    val albumSearchFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val albumCount = remember(allHierarchyAlbums) { allHierarchyAlbums.size }
    val isAlbumsRoot = destination == 1 && selectedAlbumId == null

    LaunchedEffect(isVaultSearching) {
        if (isVaultSearching) {
            vaultSortMenuExpanded = false
            withFrameNanos { }
            kotlinx.coroutines.delay(80)
            runCatching { vaultSearchFocus.requestFocus() }
            keyboardController?.show()
        }
    }

    LaunchedEffect(isFileSearching) {
        if (isFileSearching) {
            // The field is added to the top bar in the same recomposition. Give the
            // focus node a frame plus a short beat to attach before raising the keyboard.
            withFrameNanos { }
            kotlinx.coroutines.delay(80)
            runCatching { fileSearchFocus.requestFocus() }
            keyboardController?.show()
        }
    }

    LaunchedEffect(isAlbumSearching) {
        if (isAlbumSearching) {
            albumSortMenuExpanded = false
            withFrameNanos { }
            kotlinx.coroutines.delay(80)
            runCatching { albumSearchFocus.requestFocus() }
            keyboardController?.show()
        }
    }

    LaunchedEffect(destination, librarySection, selectedAlbumId) {
        val canSearch = destination == 0 || destination == 2 || (destination == 1 && selectedAlbumId != null)
        if (!canSearch && isFileSearching) {
            isFileSearching = false
            fileSearchQuery = ""
        }
        if (!(destination == 1 && selectedAlbumId == null) && isAlbumSearching) {
            isAlbumSearching = false
            albumSearchQuery = ""
        }
        if (!(destination == 1 && selectedAlbumId == null)) {
            albumSortMenuExpanded = false
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
    val displayedAlbumPhotos = remember(selectedAlbum, fileSearchQuery, effectiveAlbumMediaSort, settings.timelineDateSource) {
        selectedAlbum?.let { album ->
            val filtered = filterMediaList(album.images, fileSearchQuery)
            when (effectiveAlbumMediaSort) {
                MediaSort.DATE_DESC -> filtered.sortedWith(com.iris.gallery.data.timelineComparator(settings.timelineDateSource))
                MediaSort.DATE_ASC -> filtered.sortedWith(
                    compareBy<MediaImage> { it.timelineTimestamp(settings.timelineDateSource) }
                        .thenBy { it.dateModified }
                        .thenBy { it.dateAdded }
                        .thenBy { it.id }
                )
                MediaSort.NAME_ASC -> filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                MediaSort.NAME_DESC -> filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
                MediaSort.SIZE_DESC -> filtered.sortedWith(compareByDescending<MediaImage> { it.sizeBytes }.thenByDescending { it.id })
                MediaSort.SIZE_ASC -> filtered.sortedWith(compareBy<MediaImage> { it.sizeBytes }.thenBy { it.id })
            }
        } ?: emptyList()
    }
    val activeVaultMedia = remember(lockedMedia, selectedLockedAlbum, vaultSearchQuery, vaultMediaSort) {
        val scoped = if (selectedLockedAlbum != null) {
            lockedMedia.filter { it.bucketName.ifBlank { "Locked" } == selectedLockedAlbum }
        } else {
            lockedMedia
        }
        filterMediaList(sortVaultMediaList(scoped, vaultMediaSort), vaultSearchQuery)
    }
    val activeMedia = when {
        destination == 3 && librarySection == "trash" -> trashed
        isVaultActive -> activeVaultMedia
        destination == 1 && selectedAlbum != null -> displayedAlbumPhotos
        destination == 2 -> displayedFavoritePhotos
        else -> displayedItems
    }
    val isLockedActive = isVaultActive
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
    fun setBatchSelection(ids: Collection<Long>, selected: Boolean) {
        if (ids.isEmpty()) return
        selectedIds = if (selected) selectedIds + ids else selectedIds - ids.toSet()
    }
    fun setDateSelection(ids: List<Long>, selected: Boolean) {
        setBatchSelection(ids, selected)
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
    val baseAlbums = remember(images, albumCovers) {
        buildMediaAlbums(images, albumCovers)
    }
    val availableAlbums = remember(baseAlbums, pinnedAlbums, albumSort, albumOrder, settings.albumPickerSort) {
        when (settings.albumPickerSort) {
            AlbumPickerSort.FOLLOW_ALBUMS -> sortMediaAlbums(baseAlbums, pinnedAlbums, albumSort, albumOrder)
            AlbumPickerSort.RECENT -> sortMediaAlbums(baseAlbums, pinnedAlbums, AlbumSort.NEWEST, emptyList())
            AlbumPickerSort.NAME -> sortMediaAlbums(baseAlbums, pinnedAlbums, AlbumSort.NAME, emptyList())
        }
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
    fun handleRestore(items: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
        if (items.isNotEmpty()) {
            onRestore(items, onConfirmed)
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
                if (targetTab == GalleryTab.ALBUMS) {
                    selectedAlbumId = null
                    selectedAlbumGroupId = null
                    selectedFolderClusterPath = null
                }
                if (targetTab == GalleryTab.LIBRARY) {
                    librarySection = null
                    selectedLockedAlbum = null
                }
                if (targetTab == GalleryTab.VAULT) {
                    selectedLockedAlbum = null
                }
            } else if (kotlin.math.abs(current - index) > 1) {
                tabPagerState.scrollToPage(index)
            } else {
                tabPagerState.animateScrollToPage(index, animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        }
    }

    var showSelectionPillCustomizer by remember { mutableStateOf(false) }
    val useFloatingNavBar = isLandscape || settings.navBarStyle == NavBarStyle.FLOATING
    val showFloatingBar = useFloatingNavBar && selectedIds.isEmpty() && selectedId == null && editorImage == null && activeOverlayScreen == null && externalMedia == null
    val showSelectionPill = settings.useFloatingSelectionToolbar && selectedIds.isNotEmpty() && selectedId == null && editorImage == null && activeOverlayScreen == null && externalMedia == null

    val isTrashSelection = destination == 3 && librarySection == "trash"
    val selectedMediaForFav = remember(activeMedia, selectedIds) {
        if (selectedIds.isEmpty()) emptyList() else activeMedia.filter { it.id in selectedIds }
    }
    val allSelectedAreFavorites = selectedMediaForFav.isNotEmpty() && selectedMediaForFav.all { item ->
        item.id in favorites || (item.pairedRaw != null && item.pairedRaw.id in favorites)
    }
    val executeSelectionAction: (String) -> Unit = { actionKey ->
        when (actionKey) {
            "share" -> shareSelection()
            "move" -> {
                val selected = activeMedia.filter { it.id in selectedIds }.expandPairedRaw()
                if (selected.isNotEmpty()) {
                    pendingAlbumMedia = selected
                    albumPickerAction = AlbumAction.MOVE
                }
            }
            "copy" -> {
                val selected = activeMedia.filter { it.id in selectedIds }.expandPairedRaw()
                if (selected.isNotEmpty()) {
                    pendingAlbumMedia = selected
                    albumPickerAction = AlbumAction.COPY
                }
            }
            "favorite" -> {
                val makeFavorite = !allSelectedAreFavorites
                selectedMediaForFav.forEach { item ->
                    val rawId = item.pairedRaw?.id
                    if (makeFavorite) {
                        if (item.id !in favorites && (rawId == null || rawId !in favorites)) {
                            onToggleFavorite(item.id)
                        }
                    } else {
                        if (item.id in favorites) onToggleFavorite(item.id)
                        if (rawId != null && rawId in favorites) onToggleFavorite(rawId)
                    }
                }
                clearSelection()
            }
            "lock" -> {
                val selected = activeMedia.filter { it.id in selectedIds }.expandPairedRaw()
                if (selected.isNotEmpty()) {
                    clearSelection()
                    onLockMedia(selected, null)
                }
            }
            "delete" -> {
                val selected = activeMedia.filter { it.id in selectedIds }.expandPairedRaw()
                if (selected.isNotEmpty()) {
                    if (isTrashSelection || isVaultActive) {
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
            }
            "delete_permanent" -> {
                val selected = activeMedia.filter { it.id in selectedIds }.expandPairedRaw()
                if (selected.isNotEmpty()) {
                    pendingPermanentDeleteItems = selected
                }
            }
            "select_all" -> {
                selectedIds = activeMedia.mapTo(mutableSetOf()) { it.id }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
          if (!(destination == 3 && librarySection == "folder_view")) {
            val canSearchOnScreen = destination == 0 || destination == 2 || (destination == 1 && selectedAlbum != null)
            TopAppBar(
                title = {
                    if (isVaultSearching && isVaultActive && lockedAuthorized) {
                        OutlinedTextField(
                            value = vaultSearchQuery,
                            onValueChange = { vaultSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(vaultSearchFocus),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_vault_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (vaultSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vaultSearchQuery = "" }) {
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
                    } else if (isAlbumSearching && isAlbumsRoot) {
                        OutlinedTextField(
                            value = albumSearchQuery,
                            onValueChange = { albumSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(albumSearchFocus),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        if (settings.globalAlbumSearch) R.string.search_albums_and_media_placeholder
                                        else R.string.search_albums_placeholder,
                                        albumCount
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (albumSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { albumSearchQuery = "" }) {
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
                    } else if (isFileSearching && canSearchOnScreen) {
                        OutlinedTextField(
                            value = fileSearchQuery,
                            onValueChange = { fileSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(fileSearchFocus),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        if (destination == 0) {
                                            when (mediaFilter) {
                                                1 -> R.string.search_photos_placeholder
                                                2 -> R.string.search_videos_placeholder
                                                else -> R.string.search_files_placeholder
                                            }
                                        } else {
                                            R.string.search_files_placeholder
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
                                destination == 1 && activeAlbumGroup != null -> activeAlbumGroup.name
                                destination == 1 && activeFolderCluster != null -> activeFolderCluster.name
                                destination == 3 && librarySection == "trash" -> stringResource(R.string.section_trash)
                                isVaultActive -> if (selectedLockedAlbum != null) selectedLockedAlbum!! else stringResource(R.string.section_locked)
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
                    if (isVaultSearching && isVaultActive) {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            isVaultSearching = false
                            vaultSearchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clear_search))
                        }
                    } else if (isAlbumSearching && isAlbumsRoot) {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            isAlbumSearching = false
                            albumSearchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clear_search))
                        }
                    } else if (isFileSearching && canSearchOnScreen) {
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
                    } else if (destination == 1 && (activeAlbumGroup != null || activeFolderCluster != null)) {
                        IconButton(onClick = {
                            selectedAlbumGroupId = null
                            selectedFolderClusterPath = null
                        }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, albumsTabLabel) }
                    } else if (isVaultActive && selectedLockedAlbum != null) {
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
                        if (settings.useFloatingSelectionToolbar) {
                            val allSelected = activeMedia.isNotEmpty() && selectedIds.size >= activeMedia.size
                            IconButton(onClick = {
                                if (allSelected) clearSelection()
                                else selectedIds = activeMedia.mapTo(mutableSetOf()) { it.id }
                            }) {
                                Icon(
                                    Icons.Outlined.SelectAll,
                                    contentDescription = stringResource(
                                        if (allSelected) R.string.action_clear_selection else R.string.action_select_all
                                    ),
                                    tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            if (isTrashSelection) {
                                IconButton(onClick = {
                                    val selected = activeMedia.filter { it.id in selectedIds }
                                    handleRestore(selected) { clearSelection() }
                                }) {
                                    Icon(Icons.Outlined.RestoreFromTrash, stringResource(R.string.action_restore))
                                }
                            }
                            if (isVaultActive) {
                                IconButton(onClick = {
                                    val selected = activeMedia.filter { it.id in selectedIds }
                                    clearSelection()
                                    onUnlockMedia(selected, null)
                                }) {
                                    Icon(Icons.Outlined.LockOpen, stringResource(R.string.action_remove_from_locked))
                                }
                            }
                            IconButton(onClick = { executeSelectionAction("share") }) {
                                Icon(Icons.Outlined.Share, stringResource(R.string.action_share))
                            }
                            IconButton(onClick = { executeSelectionAction("delete") }) {
                                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.action_delete))
                            }
                            Box {
                                IconButton(onClick = { selectionMenuExpanded = true }) {
                                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more))
                                }
                                DropdownMenu(selectionMenuExpanded, onDismissRequest = { selectionMenuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_select_all)) },
                                        leadingIcon = { Icon(Icons.Outlined.SelectAll, null) },
                                        onClick = { selectionMenuExpanded = false; executeSelectionAction("select_all") }
                                    )
                                    if (destination == 1 && selectedAlbum != null && selectedIds.size == 1) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_set_album_cover)) },
                                            leadingIcon = { Icon(Icons.Outlined.Wallpaper, null) },
                                            onClick = {
                                                selectionMenuExpanded = false
                                                onSetAlbumCover(selectedAlbum!!.id, selectedIds.first())
                                                clearSelection()
                                            }
                                        )
                                    }
                                    if (!isTrashSelection && !isVaultActive) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_move_to_album)) },
                                            leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                                            onClick = { selectionMenuExpanded = false; executeSelectionAction("move") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_copy_to_album)) },
                                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                            onClick = { selectionMenuExpanded = false; executeSelectionAction("copy") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete_permanently), color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = { selectionMenuExpanded = false; executeSelectionAction("delete_permanent") }
                                        )
                                    }
                                    if (isVaultActive) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_remove_from_locked)) },
                                            leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                                            onClick = {
                                                selectionMenuExpanded = false
                                                val selected = activeMedia.filter { it.id in selectedIds }
                                                clearSelection()
                                                onUnlockMedia(selected, null)
                                            }
                                        )
                                    } else if (!isTrashSelection) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_move_to_locked)) },
                                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                            onClick = { selectionMenuExpanded = false; executeSelectionAction("lock") }
                                        )
                                    }
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
                                        onClick = { selectionMenuExpanded = false; executeSelectionAction("favorite") }
                                    )
                                }
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
                                            settingsPreferences.setMediaFilter(index)
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_show_hidden_items)) },
                                    trailingIcon = {
                                        Switch(
                                            checked = settings.showHiddenItems,
                                            onCheckedChange = null,
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    },
                                    onClick = {
                                        mediaFilterMenuExpanded = false
                                        onToggleShowHiddenItems(!settings.showHiddenItems)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.layout_mosaic_mode)) },
                                    trailingIcon = {
                                        Switch(
                                            checked = settings.photoGridLayout == PhotoGridLayout.MOSAIC,
                                            onCheckedChange = null,
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    },
                                    onClick = {
                                        mediaFilterMenuExpanded = false
                                        settingsPreferences.setPhotoGridLayout(
                                            if (settings.photoGridLayout == PhotoGridLayout.MOSAIC) PhotoGridLayout.GRID
                                            else PhotoGridLayout.MOSAIC
                                        )
                                    }
                                )
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
                                            onLockMedia(selectedAlbum!!.images.expandPairedRaw()) {
                                                selectedAlbumId = null
                                            }
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
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.action_delete_album),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            albumOptionsMenuExpanded = false
                                            pendingDeleteAlbum = selectedAlbum
                                        }
                                    )
                                }
                            }
                        }
                        val inSubpage = (destination == 1 && (selectedAlbum != null || activeAlbumGroup != null || activeFolderCluster != null)) ||
                            (destination == 3 && librarySection != null) ||
                            (destination == 4 && selectedLockedAlbum != null)
                        if (isVaultActive && lockedAuthorized && !isVaultSearching) {
                            IconButton(onClick = { isVaultSearching = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.action_search))
                            }
                            Box {
                                IconButton(onClick = { vaultSortMenuExpanded = true }) {
                                    Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.action_sort))
                                }
                                DropdownMenu(
                                    expanded = vaultSortMenuExpanded,
                                    onDismissRequest = { vaultSortMenuExpanded = false }
                                ) {
                                    if (selectedLockedAlbum == null && vaultTab == 0) {
                                        listOf(
                                            AlbumSort.NEWEST to R.string.sort_recent,
                                            AlbumSort.OLDEST to R.string.sort_date_asc,
                                            AlbumSort.NAME to R.string.sort_name,
                                            AlbumSort.ITEM_COUNT to R.string.sort_size,
                                        ).forEach { (sortOpt, labelRes) ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(labelRes)) },
                                                trailingIcon = if (vaultAlbumSort == sortOpt) {
                                                    { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                                } else null,
                                                onClick = {
                                                    vaultSortMenuExpanded = false
                                                    onSetVaultAlbumSort(sortOpt)
                                                }
                                            )
                                        }
                                    } else {
                                        listOf(
                                            MediaSort.DATE_DESC to R.string.sort_date_desc,
                                            MediaSort.DATE_ASC to R.string.sort_date_asc,
                                            MediaSort.NAME_ASC to R.string.sort_name_asc,
                                            MediaSort.NAME_DESC to R.string.sort_name_desc,
                                            MediaSort.SIZE_DESC to R.string.sort_size_desc,
                                            MediaSort.SIZE_ASC to R.string.sort_size_asc,
                                        ).forEach { (sortOpt, labelRes) ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(labelRes)) },
                                                trailingIcon = if (vaultMediaSort == sortOpt) {
                                                    { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                                } else null,
                                                onClick = {
                                                    vaultSortMenuExpanded = false
                                                    onSetVaultMediaSort(sortOpt)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (isAlbumsRoot && !isAlbumSearching) {
                            IconButton(onClick = { isAlbumSearching = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.action_search))
                            }
                            if (activeAlbumGroup == null && activeFolderCluster == null) {
                                IconButton(onClick = { showAlbumsViewOptionsDialog = true }) {
                                    Icon(
                                        if (settings.albumsViewMode == AlbumsViewMode.LIST) Icons.AutoMirrored.Outlined.ViewList
                                        else Icons.Outlined.GridView,
                                        stringResource(R.string.albums_view_options_title)
                                    )
                                }
                                IconButton(onClick = { showCreateAlbumGroupDialog = true }) {
                                    Icon(Icons.Outlined.CreateNewFolder, stringResource(R.string.action_create_album_group))
                                }
                            } else if (activeAlbumGroup != null) {
                                IconButton(onClick = { showGroupCoverPickerSheet = true }) {
                                    Icon(Icons.Outlined.Wallpaper, stringResource(R.string.action_set_group_cover))
                                }
                                IconButton(onClick = { showEditGroupMembersDialog = true }) {
                                    Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit_group))
                                }
                            }
                            AlbumSortChip(
                                sort = albumSort,
                                expanded = albumSortMenuExpanded,
                                onExpandedChange = { albumSortMenuExpanded = it },
                                onSortChanged = { value ->
                                    albumSortMenuExpanded = false
                                    onSetAlbumSort(value)
                                },
                                compact = true,
                                onStartReorder = if (activeAlbumGroup == null && activeFolderCluster == null) {
                                    {
                                        onSetAlbumSort(AlbumSort.CUSTOM)
                                        isEditingAlbumOrder = true
                                    }
                                } else null
                            )
                        }
                        if (destination == 1 && selectedAlbum == null && activeAlbumGroup == null && activeFolderCluster == null && albumSort == com.iris.gallery.data.AlbumSort.CUSTOM) {
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
                        if (destination == 3 && librarySection == null) {
                            IconButton(onClick = { isEditingLibraryOrder = !isEditingLibraryOrder }) {
                                Icon(
                                    if (isEditingLibraryOrder) Icons.Filled.Check else Icons.Outlined.Edit,
                                    contentDescription = stringResource(
                                        if (isEditingLibraryOrder) R.string.action_done_editing else R.string.library_edit_order
                                    ),
                                    tint = if (isEditingLibraryOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
          if (!useFloatingNavBar && selectedIds.isEmpty()) {
            val showDockedLabels = settings.navLabelMode != NavLabelMode.HIDDEN
            val alwaysShowDockedLabels = settings.navLabelMode == NavLabelMode.ALWAYS
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
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .then(if (!showDockedLabels) Modifier.height(64.dp) else Modifier)
                    ) {
                        visibleTabs.forEachIndexed { index, tab ->
                            val isSelected = navVisibleTab == tab
                            val icon = if (tab == GalleryTab.MEDIA) mediaTabIcon(mediaFilter) else tab.icon
                            val label = if (tab == GalleryTab.MEDIA) mediaTabLabel else stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { handleTabSelected(index) },
                                icon = { AnimatedNavigationIcon(icon, isSelected, label, tiltEnabled = settings.tiltNavIcons) },
                                label = if (showDockedLabels) {
                                    { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                } else {
                                    null
                                },
                                alwaysShowLabel = alwaysShowDockedLabels,
                            )
                        }
                    }
                }
            }
          }
        },
    ) { scaffoldPadding ->
      val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      val floatingShowsLabels = !isLandscape && settings.navLabelMode != NavLabelMode.HIDDEN
      val floatingBarBottomPadding = (if (floatingShowsLabels) 92.dp else 84.dp) + navBarsBottom
      val selectionPillBottomPadding = 86.dp + navBarsBottom
      val layoutDirection = LocalLayoutDirection.current
      val padding = when {
          useFloatingNavBar && selectedIds.isEmpty() -> PaddingValues(
              start = scaffoldPadding.calculateStartPadding(layoutDirection),
              top = scaffoldPadding.calculateTopPadding(),
              end = scaffoldPadding.calculateEndPadding(layoutDirection),
              bottom = floatingBarBottomPadding
          )
          settings.useFloatingSelectionToolbar && selectedIds.isNotEmpty() -> PaddingValues(
              start = scaffoldPadding.calculateStartPadding(layoutDirection),
              top = scaffoldPadding.calculateTopPadding(),
              end = scaffoldPadding.calculateEndPadding(layoutDirection),
              bottom = selectionPillBottomPadding
          )
          else -> scaffoldPadding
      }
      // The pager starts under the transparent top bar. Pull-to-refresh lives at
      // the top of the content, so move it down by the top bar inset and drop the
      // duplicate top inset from the grids inside it.
      val topBarInset = padding.calculateTopPadding()
      val gridPadding = PaddingValues(
          start = padding.calculateStartPadding(layoutDirection),
          top = 0.dp,
          end = padding.calculateEndPadding(layoutDirection),
          bottom = padding.calculateBottomPadding()
      )
      when {
        loading && images.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null && images.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(error) }
        else -> HorizontalPager(
          state = tabPagerState,
          beyondViewportPageCount = (visibleTabs.size - 1).coerceIn(0, 4),
          userScrollEnabled = selectedIds.isEmpty() &&
              selectedAlbum == null &&
              selectedAlbumGroupId == null &&
              selectedFolderClusterPath == null &&
              (!isVaultActive || selectedLockedAlbum == null) &&
              (destination != 3 || librarySection == null),
          modifier = Modifier.fillMaxSize(),
        ) { page ->
          val activeTab = visibleTabs.getOrElse(page) { GalleryTab.MEDIA }
          when (activeTab) {
            GalleryTab.MEDIA -> {
              val pullRefreshState0 = rememberPullToRefreshState()
              PullToRefreshBox(
                state = pullRefreshState0,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(top = topBarInset),
                indicator = {
                  IrisPullToRefreshIndicator(
                    state = pullRefreshState0,
                    isRefreshing = loading,
                  )
                }
              ) {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                  if (displayedItems.isNotEmpty()) PhotoGrid(
                    displayedItems, gridPadding, photoGridState, cellSize = activeGridCellSize, onCellSizeChange = activeOnCellSizeChange,
                    layoutMode = settings.photoGridLayout,
                    animateGifThumbnails = settings.animateGifThumbnails,
                    showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
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
                    onToggleSelection = if (onPick == null) ::toggleSelection else null,
                    onSetSelection = if (onPick == null) ::setSelection else null,
                    onSetBatchSelection = if (onPick == null) ::setBatchSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                    onInvalidMedia = onInvalidMedia,
                    onRegisterBoundsResolver = { boundsResolvers["media"] = it },
                  ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { activeViewerSourceKey = "media"; viewerImages = displayedItems; selectedId = it.id } }
                  else if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), gridPadding)
                  else EmptyState(
                      stringResource(
                          when (mediaFilter) {
                              2 -> R.string.empty_videos
                              else -> R.string.empty_photos
                          }
                      ),
                      gridPadding
                  )
                }
              }
            }
            GalleryTab.ALBUMS -> {
              val pullRefreshState1 = rememberPullToRefreshState()
              PullToRefreshBox(
                state = pullRefreshState1,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(top = topBarInset),
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
                      displayedAlbumPhotos, gridPadding, albumPhotoGridState, cellSize = activeGridCellSize, onCellSizeChange = activeOnCellSizeChange,
                      layoutMode = settings.photoGridLayout,
                      animateGifThumbnails = settings.animateGifThumbnails,
                      showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
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
                      onToggleSelection = if (onPick == null) ::toggleSelection else null,
                      onSetSelection = if (onPick == null) ::setSelection else null,
                      onSetBatchSelection = if (onPick == null) ::setBatchSelection else null,
                      onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                      onRegisterBoundsResolver = { boundsResolvers["album"] = it },
                    ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { activeViewerSourceKey = "album"; viewerImages = displayedAlbumPhotos; selectedId = it.id } }
                    else if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), gridPadding)
                    else EmptyState(stringResource(R.string.empty_photos), gridPadding)
                  } else AlbumsGrid(
                    images = images,
                    searchQuery = albumSearchQuery,
                    onClearSearch = { albumSearchQuery = "" },
                    padding = gridPadding,
                    state = albumGridState,
                    cellSize = customAlbumCellSize,
                    viewMode = settings.albumsViewMode,
                    groupDirectSubfolders = settings.groupDirectSubfolders,
                    hierarchyDepth = settings.albumHierarchyDepth,
                    globalAlbumSearch = settings.globalAlbumSearch,
                    showFilterChips = settings.showAlbumFilterChips,
                    onCellSizeChange = onAlbumCellSizeChange,
                    cornerStyle = settings.cornerStyle,
                    gridSpacing = settings.gridSpacing,
                    showCount = settings.showAlbumCount,
                    showLabels = settings.showAlbumLabels,
                    pinned = pinnedAlbums,
                    covers = albumCovers,
                    sort = albumSort,
                    customOrder = albumOrder,
                    isEditingOrder = isEditingAlbumOrder && destination == 1 && selectedAlbumId == null && activeAlbumGroup == null && activeFolderCluster == null && albumSort == com.iris.gallery.data.AlbumSort.CUSTOM,
                    albumGroups = albumGroups,
                    activeGroup = activeAlbumGroup,
                    activeCluster = activeFolderCluster,
                    onTogglePinned = onTogglePinnedAlbum,
                    onOrderChanged = onSetAlbumOrder,
                    onLockAlbum = { album -> onLockMedia(album.images.expandPairedRaw(), null) },
                    onExcludeFolder = { album ->
                        val samplePath = album.images.firstOrNull { it.path.isNotBlank() }?.path.orEmpty()
                        val folderPath = if (samplePath.contains('/')) samplePath.substringBeforeLast('/') else ""
                        if (folderPath.isNotBlank()) {
                            onAddExcludedFolder(folderPath)
                            Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteAlbum = { album -> pendingDeleteAlbum = album },
                    onOpenFolderCluster = { cluster -> selectedFolderClusterPath = cluster.id },
                    onOpenAlbumGroup = { group -> selectedAlbumGroupId = group.id },
                    onCreateAlbumGroup = onCreateAlbumGroup,
                    onAddAlbumToGroup = onAddAlbumToGroup,
                    onRemoveAlbumFromGroup = onRemoveAlbumFromGroup,
                    onRenameAlbumGroup = onRenameAlbumGroup,
                    onSetAlbumGroupCover = onSetAlbumGroupCover,
                    onDeleteAlbumGroup = { groupId ->
                        if (selectedAlbumGroupId == groupId) selectedAlbumGroupId = null
                        onDeleteAlbumGroup(groupId)
                    },
                    onOpenGlobalSearchMedia = { media, list ->
                        if (onPick != null) onPick(media)
                        else {
                            activeViewerSourceKey = null
                            viewerImages = list
                            selectedId = media.id
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
                            cellSize = activeGridCellSize,
                            onCellSizeChange = activeOnCellSizeChange,
                            layoutMode = settings.photoGridLayout,
                            animateGifThumbnails = settings.animateGifThumbnails,
                            cornerStyle = settings.cornerStyle,
                            gridSpacing = settings.gridSpacing,
                            showVideoDuration = settings.showVideoDurationBadge,
                            showFormatBadge = settings.showMediaFormatBadge,
                            selectedIds = selectedIds,
                            favorites = favorites,
                            showFavoriteBadge = false,
                            onToggleSelection = ::toggleSelection,
                            onSetSelection = ::setSelection,
                            onSetBatchSelection = ::setBatchSelection,
                            onRegisterBoundsResolver = { boundsResolvers["trash"] = it },
                        ) {
                            if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                            else {
                                activeViewerSourceKey = "trash"
                                viewerImages = trashed
                                selectedId = it.id
                            }
                        }
                        "locked" -> VaultContent(
                            lockedAuthorized = lockedAuthorized,
                            lockedMedia = lockedMedia,
                            vaultTab = vaultTab,
                            onVaultTabChange = { vaultTab = it },
                            selectedLockedAlbum = selectedLockedAlbum,
                            onSelectLockedAlbum = { selectedLockedAlbum = it },
                            searchQuery = vaultSearchQuery,
                            onClearSearch = { vaultSearchQuery = "" },
                            vaultAlbumSort = vaultAlbumSort,
                            vaultMediaSort = vaultMediaSort,
                            settings = settings,
                            padding = padding,
                            gridState = libraryGridState,
                            cellSize = activeGridCellSize,
                            onCellSizeChange = activeOnCellSizeChange,
                            selectedIds = selectedIds,
                            favorites = favorites,
                            onToggleSelection = ::toggleSelection,
                            onSetSelection = ::setSelection,
                            onSetBatchSelection = ::setBatchSelection,
                            onSetDateSelection = ::setDateSelection,
                            onRequestUnlock = onRequestUnlock,
                            onRegisterBoundsResolver = { boundsResolvers["vault"] = it },
                            onOpenMedia = { media, list ->
                                activeViewerSourceKey = "vault"
                                viewerImages = list
                                selectedId = media.id
                            },
                        )
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
                                    memories, PaddingValues(0.dp), libraryGridState, cellSize = activeGridCellSize, onCellSizeChange = activeOnCellSizeChange, showTimeline = true,
                                    layoutMode = settings.photoGridLayout,
                                    animateGifThumbnails = settings.animateGifThumbnails,
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
                                    onSetBatchSelection = ::setBatchSelection,
                                    onRegisterBoundsResolver = { boundsResolvers["memories"] = it },
                                ) {
                                    if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                    else { activeViewerSourceKey = "memories"; viewerImages = memories; selectedId = it.id }
                                }
                            }
                        }
                        "formats" -> {
                            var formatFilter by remember { mutableStateOf(MediaFormatFilter.ALL) }
                            val allVideos = remember(images) { images.filter { it.isVideo } }
                            val allRaw = remember(images) { images.mapNotNull { if (it.isRaw) it else it.pairedRaw } }
                            val allGifs = remember(images) { images.filter { it.isGif } }
                            val allPanos = remember(images) { images.filter { it.isPanorama } }
                            val allMotion = remember(images) { images.filter { it.isMotionPhoto } }
                            val allSpecial = remember(allVideos, allRaw, allGifs, allPanos, allMotion, settings.timelineDateSource) {
                                (allVideos + allRaw + allGifs + allPanos + allMotion)
                                    .distinctBy { it.id }
                                    .sortedWith(com.iris.gallery.data.timelineComparator(settings.timelineDateSource))
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .nestedScroll(ConsumeHorizontalNestedScroll)
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                            cellSize = activeGridCellSize,
                                            onCellSizeChange = activeOnCellSizeChange,
                                            layoutMode = settings.photoGridLayout,
                                            animateGifThumbnails = settings.animateGifThumbnails,
                                            timelineDateSource = settings.timelineDateSource,
                                            cornerStyle = settings.cornerStyle,
                                            gridSpacing = settings.gridSpacing,
                                            showVideoDuration = settings.showVideoDurationBadge,
                                            showFormatBadge = settings.showMediaFormatBadge,
                                            selectedIds = selectedIds,
                                            favorites = favorites,
                                            onToggleSelection = if (onPick == null) ::toggleSelection else null,
                                            onSetSelection = if (onPick == null) ::setSelection else null,
                                            onSetBatchSelection = if (onPick == null) ::setBatchSelection else null,
                                            onRegisterBoundsResolver = { boundsResolvers["formats"] = it },
                                        ) {
                                            if (selectedIds.isNotEmpty()) toggleSelection(it.id)
                                            else if (onPick != null) onPick(it)
                                            else { activeViewerSourceKey = "formats"; viewerImages = currentFiltered; selectedId = it.id }
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
                                timelineDateSource = settings.timelineDateSource,
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
                        else -> LibraryScreen(
                            padding = padding,
                            trashCount = trashed.size,
                            lockedCount = lockedMedia.size,
                            order = libraryOrder,
                            isEditingOrder = isEditingLibraryOrder && destination == 3 && librarySection == null,
                            onOrderChanged = onSetLibraryOrder,
                        ) {
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
                    cellSize = activeGridCellSize,
                    onCellSizeChange = activeOnCellSizeChange,
                    layoutMode = settings.photoGridLayout,
                    animateGifThumbnails = settings.animateGifThumbnails,
                    showTimeline = settings.showTimelineHeaders && fileSearchQuery.isBlank(),
                    timelineDateSource = settings.timelineDateSource,
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
                    onSetBatchSelection = if (onPick == null) ::setBatchSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                    onInvalidMedia = onInvalidMedia,
                    onRegisterBoundsResolver = { boundsResolvers["favorites"] = it },
                ) {
                    if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { activeViewerSourceKey = "favorites"; viewerImages = displayedFavoritePhotos; selectedId = it.id }
                }
            }
            GalleryTab.VAULT -> {
                VaultContent(
                    lockedAuthorized = lockedAuthorized,
                    lockedMedia = lockedMedia,
                    vaultTab = vaultTab,
                    onVaultTabChange = { vaultTab = it },
                    selectedLockedAlbum = selectedLockedAlbum,
                    onSelectLockedAlbum = { selectedLockedAlbum = it },
                    searchQuery = vaultSearchQuery,
                    onClearSearch = { vaultSearchQuery = "" },
                    vaultAlbumSort = vaultAlbumSort,
                    vaultMediaSort = vaultMediaSort,
                    settings = settings,
                    padding = padding,
                    gridState = libraryGridState,
                    cellSize = activeGridCellSize,
                    onCellSizeChange = activeOnCellSizeChange,
                    selectedIds = selectedIds,
                    favorites = favorites,
                    onToggleSelection = ::toggleSelection,
                    onSetSelection = ::setSelection,
                    onSetBatchSelection = ::setBatchSelection,
                    onSetDateSelection = ::setDateSelection,
                    onRequestUnlock = onRequestUnlock,
                    onRegisterBoundsResolver = { boundsResolvers["vault_tab"] = it },
                    onOpenMedia = { media, list ->
                        activeViewerSourceKey = "vault_tab"
                        viewerImages = list
                        selectedId = media.id
                    },
                )
            }
          }
        }
      }
    }

    val floatingNavShowsLabels = !isLandscape && settings.navLabelMode != NavLabelMode.HIDDEN
    val floatingNavAlwaysShowLabels = floatingNavShowsLabels && settings.navLabelMode == NavLabelMode.ALWAYS
    AnimatedVisibility(
        visible = showFloatingBar,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            color = if (isAmoled) Color.Black.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(32.dp),
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
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .height(if (floatingNavShowsLabels) 72.dp else 64.dp)
            ) {
                visibleTabs.forEachIndexed { index, tab ->
                    val isSelected = navVisibleTab == tab
                    val icon = if (tab == GalleryTab.MEDIA) mediaTabIcon(mediaFilter) else tab.icon
                    val label = if (tab == GalleryTab.MEDIA) mediaTabLabel else stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { handleTabSelected(index) },
                        icon = { AnimatedNavigationIcon(icon, isSelected, label, tiltEnabled = settings.tiltNavIcons) },
                        label = if (floatingNavShowsLabels) {
                            { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        } else {
                            null
                        },
                        alwaysShowLabel = floatingNavAlwaysShowLabels,
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showSelectionPill,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetY = { it }
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            targetOffsetY = { it }
        )
    ) {
        val pillActions = remember(settings.selectionPillActions) {
            settings.selectionPillActions
                .filter { it in ALL_SELECTION_PILL_ACTIONS }
                .distinct()
                .ifEmpty { DEFAULT_SELECTION_PILL_ACTIONS }
        }
        val overflowActions = remember(pillActions) {
            ALL_SELECTION_PILL_ACTIONS.filterNot { it in pillActions }
        }
        Surface(
            color = if (isAmoled) Color(0xFF141414).copy(alpha = 0.96f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isAmoled) 0.45f else 0.30f)
            ),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                when {
                    isTrashSelection -> {
                        IconButton(onClick = {
                            val selected = activeMedia.filter { it.id in selectedIds }
                            handleRestore(selected) { clearSelection() }
                        }) {
                            Icon(
                                Icons.Outlined.RestoreFromTrash,
                                stringResource(R.string.action_restore),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = ::shareSelection) {
                            Icon(
                                Icons.Outlined.Share,
                                stringResource(R.string.action_share),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { executeSelectionAction("delete") }) {
                            Icon(
                                Icons.Outlined.DeleteForever,
                                stringResource(R.string.action_delete_permanently),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { executeSelectionAction("select_all") }) {
                            Icon(
                                Icons.Outlined.SelectAll,
                                stringResource(R.string.action_select_all),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    isVaultActive -> {
                        IconButton(onClick = {
                            val selected = activeMedia.filter { it.id in selectedIds }
                            clearSelection()
                            onUnlockMedia(selected, null)
                        }) {
                            Icon(
                                Icons.Outlined.LockOpen,
                                stringResource(R.string.action_remove_from_locked),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = ::shareSelection) {
                            Icon(
                                Icons.Outlined.Share,
                                stringResource(R.string.action_share),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { executeSelectionAction("delete") }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { executeSelectionAction("select_all") }) {
                            Icon(
                                Icons.Outlined.SelectAll,
                                stringResource(R.string.action_select_all),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState())
                                .nestedScroll(ConsumeHorizontalNestedScroll),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            pillActions.forEach { actionKey ->
                                val icon = selectionActionIcon(actionKey, allSelectedAreFavorites)
                                val label = stringResource(selectionActionLabelRes(actionKey, allSelectedAreFavorites))
                                val tint = when {
                                    actionKey == "delete_permanent" -> MaterialTheme.colorScheme.error
                                    actionKey == "favorite" && allSelectedAreFavorites -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                IconButton(onClick = { executeSelectionAction(actionKey) }) {
                                    Icon(icon, contentDescription = label, tint = tint)
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { selectionMenuExpanded = true }) {
                                Icon(
                                    Icons.Outlined.MoreVert,
                                    stringResource(R.string.action_more),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = selectionMenuExpanded,
                                onDismissRequest = { selectionMenuExpanded = false }
                            ) {
                                if (destination == 1 && selectedAlbum != null && selectedIds.size == 1) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_set_album_cover)) },
                                        leadingIcon = { Icon(Icons.Outlined.Wallpaper, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            onSetAlbumCover(selectedAlbum!!.id, selectedIds.first())
                                            clearSelection()
                                        }
                                    )
                                }
                                overflowActions.forEach { actionKey ->
                                    val icon = selectionActionIcon(actionKey, allSelectedAreFavorites)
                                    val label = stringResource(selectionActionLabelRes(actionKey, allSelectedAreFavorites))
                                    val isDestructive = actionKey == "delete_permanent"
                                    val tint = when {
                                        isDestructive -> MaterialTheme.colorScheme.error
                                        actionKey == "favorite" && allSelectedAreFavorites -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
                                            )
                                        },
                                        leadingIcon = { Icon(icon, null, tint = tint) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            executeSelectionAction(actionKey)
                                        }
                                    )
                                }
                                if (overflowActions.isNotEmpty() || (destination == 1 && selectedAlbum != null && selectedIds.size == 1)) {
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_customize_toolbar)) },
                                    leadingIcon = { Icon(Icons.Outlined.Tune, null) },
                                    onClick = {
                                        selectionMenuExpanded = false
                                        showSelectionPillCustomizer = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSelectionPillCustomizer) {
        SelectionPillCustomizerSheet(
            selectedActions = settings.selectionPillActions,
            onSave = { settingsPreferences.setSelectionPillActions(it) },
            onDismiss = { showSelectionPillCustomizer = false }
        )
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
                    onUnlockMedia(imagesToRestore, null)
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

    if (showAlbumsViewOptionsDialog) {
        AlbumsViewOptionsDialog(
            viewMode = settings.albumsViewMode,
            groupDirectSubfolders = settings.groupDirectSubfolders,
            hierarchyDepth = settings.albumHierarchyDepth,
            onViewModeChange = { settingsPreferences.setAlbumsViewMode(it) },
            onGroupDirectSubfoldersChange = {
                settingsPreferences.setGroupDirectSubfolders(it)
                if (!it) selectedFolderClusterPath = null
            },
            onHierarchyDepthChange = {
                settingsPreferences.setAlbumHierarchyDepth(it)
                selectedAlbumId = null
                selectedFolderClusterPath = null
            },
            onDismiss = { showAlbumsViewOptionsDialog = false }
        )
    }

    if (showCreateAlbumGroupDialog) {
        AlbumGroupEditorDialog(
            title = stringResource(R.string.action_create_album_group),
            initialName = "",
            allAlbums = allHierarchyAlbums,
            initialSelectedIds = emptySet(),
            onConfirm = { name, ids ->
                showCreateAlbumGroupDialog = false
                onCreateAlbumGroup(name, ids)
            },
            onDismiss = { showCreateAlbumGroupDialog = false }
        )
    }

    if (showEditGroupMembersDialog && activeAlbumGroup != null) {
        val editingGroup = activeAlbumGroup
        AlbumGroupEditorDialog(
            title = stringResource(R.string.action_edit_group),
            initialName = editingGroup.name,
            allAlbums = allHierarchyAlbums,
            initialSelectedIds = editingGroup.albumIds.toSet(),
            onConfirm = { newName, ids ->
                showEditGroupMembersDialog = false
                onRenameAlbumGroup(editingGroup.id, newName)
                onSetAlbumGroupMembers(editingGroup.id, ids)
            },
            onDismiss = { showEditGroupMembersDialog = false }
        )
    }

    if (showGroupCoverPickerSheet && activeAlbumGroup != null) {
        val targetGroup = activeAlbumGroup
        val memberIds = remember(targetGroup.albumIds) { targetGroup.albumIds.toSet() }
        val groupImages = remember(allHierarchyAlbums, memberIds) {
            allHierarchyAlbums.filter { it.id in memberIds }.flatMap { it.images }.sortedByDescending { it.dateTaken }
        }
        AlbumGroupCoverPickerSheet(
            groupName = targetGroup.name,
            mediaItems = groupImages,
            currentCoverMediaId = targetGroup.coverMediaId,
            onSelectCover = { mediaId ->
                showGroupCoverPickerSheet = false
                onSetAlbumGroupCover(targetGroup.id, mediaId)
            },
            onResetCover = {
                showGroupCoverPickerSheet = false
                onSetAlbumGroupCover(targetGroup.id, null)
            },
            onDismiss = { showGroupCoverPickerSheet = false }
        )
    }

    pendingDeleteAlbum?.let { album ->
        val isPermanentDefault = settings.deleteMode == DeleteMode.PERMANENT
        var deleteAlbumPermanently by remember(album.id) { mutableStateOf(isPermanentDefault) }
        AlertDialog(
            onDismissRequest = { pendingDeleteAlbum = null },
            title = { Text(stringResource(R.string.delete_album_dialog_title, album.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalAlbumFileCount = album.images.expandPairedRaw().size
                    Text(
                        if (isPermanentDefault || deleteAlbumPermanently) {
                            stringResource(R.string.delete_album_permanent_desc, totalAlbumFileCount, album.name)
                        } else {
                            stringResource(R.string.delete_album_trash_desc, totalAlbumFileCount, album.name)
                        }
                    )
                    if (!isPermanentDefault) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteAlbumPermanently = !deleteAlbumPermanently }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = deleteAlbumPermanently,
                                onCheckedChange = { deleteAlbumPermanently = it }
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
                    val targetAlbum = album
                    val permanent = isPermanentDefault || deleteAlbumPermanently
                    pendingDeleteAlbum = null
                    val onConfirmed = {
                        if (selectedAlbumId == targetAlbum.id) {
                            selectedAlbumId = null
                        }
                    }
                    val allAlbumItems = targetAlbum.images.expandPairedRaw()
                    if (permanent) {
                        handleDeletePermanently(allAlbumItems, onConfirmed)
                    } else {
                        handleTrash(allAlbumItems, onConfirmed)
                    }
                }) {
                    Text(
                        if (isPermanentDefault || deleteAlbumPermanently) stringResource(R.string.action_delete_permanently)
                        else stringResource(R.string.action_delete_album),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAlbum = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingDeleteItems?.let { items ->
        val isInTrash = destination == 3 && librarySection == "trash"
        val isLockedSection = isVaultActive
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
            pickerSort = settings.albumPickerSort,
            onPickerSortChanged = { settingsPreferences.setAlbumPickerSort(it) },
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

    val activity = remember(context) {
        generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>()
            .firstOrNull()
    }
    var lastRootBackPressMs by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var exitConfirmToast by remember { mutableStateOf<Toast?>(null) }

    BackHandler(
        enabled = onPick == null &&
            activeOverlayScreen == null &&
            editorImage == null &&
            externalMedia == null &&
            selectedId == null &&
            selectedIds.isEmpty() &&
            !isFileSearching &&
            !isAlbumSearching &&
            !isVaultSearching &&
            selectedAlbum == null &&
            activeAlbumGroup == null &&
            activeFolderCluster == null &&
            selectedLockedAlbum == null &&
            librarySection == null &&
            !isEditingAlbumOrder &&
            !isEditingLibraryOrder
    ) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastRootBackPressMs <= 2000L) {
            exitConfirmToast?.cancel()
            exitConfirmToast = null
            activity?.finish()
        } else {
            lastRootBackPressMs = now
            exitConfirmToast?.cancel()
            exitConfirmToast = Toast.makeText(
                context,
                R.string.toast_press_back_again_to_exit,
                Toast.LENGTH_SHORT
            ).also { it.show() }
        }
    }
    BackHandler(enabled = !isFileSearching && !isVaultSearching && selectedIds.isNotEmpty()) { clearSelection() }
    BackHandler(enabled = selectedIds.isEmpty() && isVaultActive && isVaultSearching) {
        isVaultSearching = false
        vaultSearchQuery = ""
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum == null && isAlbumSearching) {
        isAlbumSearching = false
        albumSearchQuery = ""
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum == null && isEditingAlbumOrder) {
        isEditingAlbumOrder = false
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection == null && isEditingLibraryOrder) {
        isEditingLibraryOrder = false
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum != null && !isFileSearching) {
        selectedAlbumId = null
        if (isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum == null && (activeAlbumGroup != null || activeFolderCluster != null)) {
        selectedAlbumGroupId = null
        selectedFolderClusterPath = null
    }
    BackHandler(enabled = selectedIds.isEmpty() && isVaultActive && selectedLockedAlbum != null && !isVaultSearching) {
        selectedLockedAlbum = null
    }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection != null && (librarySection != "locked" || selectedLockedAlbum == null) && !isFileSearching && !isVaultSearching) {
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
        androidx.compose.runtime.key(viewerSessionId, media.uri) {
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
            showImageRotateButton = settings.showImageRotateButton,
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
            preferredEditorComponent = settings.preferredEditorComponent,
            lastUsedEditorId = settings.lastUsedEditorId,
            preferredVideoEditorComponent = settings.preferredVideoEditorComponent,
            lastUsedVideoEditorId = settings.lastUsedVideoEditorId,
            onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
            onSetPreferredExternalEditor = { settingsPreferences.setPreferredExternalEditor(it) },
            onSetLastUsedEditorId = { settingsPreferences.setLastUsedEditorId(it) },
            onSetPreferredVideoEditor = { settingsPreferences.setPreferredVideoEditorComponent(it) },
            onSetLastUsedVideoEditorId = { settingsPreferences.setLastUsedVideoEditorId(it) },
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
            onDelete = { items, _ ->
                externalMedia = null
                items.forEach { item ->
                    runCatching { context.contentResolver.delete(item.uri, null, null) }
                }
            },
            onRestore = { _, _ -> },
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
    }

    selectedId?.let { id ->
        val activeImages = viewerImages ?: images
        val index = activeImages.indexOfFirst { it.id == id || it.pairedRaw?.id == id }
        val isViewingLocked = activeViewerSourceKey == "vault" || activeViewerSourceKey == "vault_tab" ||
            (activeViewerSourceKey == null && isVaultActive)
        val isViewingTrash = activeViewerSourceKey == "trash" ||
            (activeViewerSourceKey == null && destination == 3 && librarySection == "trash")
        val removeViewerItemAndAdvance: (MediaImage) -> Unit = { media ->
            val activeList = viewerImages ?: images
            val currentIndex = activeList.indexOfFirst { it.id == media.id }
            val nextImage = if (activeList.size > 1 && currentIndex >= 0) {
                if (currentIndex < activeList.size - 1) activeList[currentIndex + 1]
                else activeList[currentIndex - 1]
            } else null

            selectedId = nextImage?.id
            viewerImages = viewerImages?.filterNot { it.id == media.id }
        }
        if (index >= 0 && activeImages.isNotEmpty()) androidx.compose.runtime.key(viewerSessionId) { PhotoViewer(
            images = activeImages,
            initialPage = index,
            initialShowRawVariant = initialShowRawVariant,
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
            showImageRotateButton = settings.showImageRotateButton,
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
            preferredEditorComponent = settings.preferredEditorComponent,
            lastUsedEditorId = settings.lastUsedEditorId,
            preferredVideoEditorComponent = settings.preferredVideoEditorComponent,
            lastUsedVideoEditorId = settings.lastUsedVideoEditorId,
            onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
            onSetPreferredExternalEditor = { settingsPreferences.setPreferredExternalEditor(it) },
            onSetLastUsedEditorId = { settingsPreferences.setLastUsedEditorId(it) },
            onSetPreferredVideoEditor = { settingsPreferences.setPreferredVideoEditorComponent(it) },
            onSetLastUsedVideoEditorId = { settingsPreferences.setLastUsedVideoEditorId(it) },
            availableAlbums = availableAlbums,
            albumPickerSort = settings.albumPickerSort,
            onSetAlbumPickerSort = { settingsPreferences.setAlbumPickerSort(it) },
            onToggleFavorite = onToggleFavorite,
            onClose = { selectedId = null; viewerImages = null; activeViewerSourceKey = null; initialShowRawVariant = false },
            onPageChanged = { selectedId = it },
            onRename = { media, newName ->
                onRenameMedia(media, newName) { updated ->
                    if (updated != null) {
                        viewerImages = viewerImages?.flatMap { item ->
                            when {
                                item.id == media.id -> {
                                    val companion = item.pairedRaw
                                    if (companion != null &&
                                        com.iris.gallery.data.rawPairStem(updated.name) != com.iris.gallery.data.rawPairStem(companion.name)
                                    ) {
                                        listOf(updated.copy(pairedRaw = null), companion)
                                    } else {
                                        listOf(updated.copy(pairedRaw = companion))
                                    }
                                }
                                item.pairedRaw?.id == media.id -> {
                                    if (com.iris.gallery.data.rawPairStem(item.name) != com.iris.gallery.data.rawPairStem(updated.name)) {
                                        listOf(item.copy(pairedRaw = null), updated)
                                    } else {
                                        listOf(item.copy(pairedRaw = updated))
                                    }
                                }
                                else -> listOf(item)
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.toast_rename_success, updated.name), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { mediaList, deletePermanently ->
                val deletedIds = mediaList.mapTo(HashSet()) { it.id }
                val onConfirmedAction = {
                    val activeList = viewerImages ?: images
                    val removedPrimary = activeList.firstOrNull { it.id in deletedIds }
                    if (removedPrimary != null) {
                        val survivingRaw = removedPrimary.pairedRaw?.takeIf { it.id !in deletedIds }
                        if (survivingRaw != null) {
                            selectedId = survivingRaw.id
                            viewerImages = viewerImages?.map { item ->
                                if (item.id == removedPrimary.id) survivingRaw else item
                            }
                        } else {
                            removeViewerItemAndAdvance(removedPrimary)
                        }
                    } else {
                        viewerImages = viewerImages?.map { item ->
                            if (item.pairedRaw != null && item.pairedRaw.id in deletedIds) {
                                item.copy(pairedRaw = null)
                            } else {
                                item
                            }
                        }
                    }
                }

                if (isViewingTrash || deletePermanently) {
                    handleDeletePermanently(mediaList, onConfirmedAction)
                } else if (isViewingLocked) {
                    onDeleteFromLocked(mediaList) {
                        onTrashFeedbackChange(TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, mediaList.size))
                        onConfirmedAction()
                    }
                } else {
                    handleTrash(mediaList, onConfirmedAction)
                }
            },
            onRestore = { media, onViewerConfirmed ->
                handleRestore(listOf(media)) {
                    onViewerConfirmed()
                    removeViewerItemAndAdvance(media)
                }
            },
            onEditMetadata = { media, request ->
                onEditMetadata(media, request) { updated ->
                    if (updated != null) {
                        viewerImages = viewerImages?.map { item ->
                            when {
                                item.id == media.id -> updated.copy(pairedRaw = item.pairedRaw)
                                item.pairedRaw?.id == media.id -> item.copy(pairedRaw = updated)
                                else -> item
                            }
                        }
                    }
                }
            },
            onEdit = { editorImage = it; selectedId = null },
            onLock = { mediaList ->
                val deletedIds = mediaList.mapTo(HashSet()) { it.id }
                onLockMedia(mediaList) {
                    val activeList = viewerImages ?: images
                    val removedPrimary = activeList.firstOrNull { it.id in deletedIds }
                    if (removedPrimary != null) removeViewerItemAndAdvance(removedPrimary)
                }
            },
            onUnlock = { mediaList ->
                val unlockedIds = mediaList.mapTo(HashSet()) { it.id }
                onUnlockMedia(mediaList) {
                    val activeList = viewerImages ?: images
                    val removedPrimary = activeList.firstOrNull { it.id in unlockedIds }
                    if (removedPrimary != null) removeViewerItemAndAdvance(removedPrimary)
                }
            },
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
            resolveTargetRect = { mediaId -> activeViewerSourceKey?.let { boundsResolvers[it]?.invoke(mediaId) } },
        ) }
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
                onToggleShowHiddenItems = onToggleShowHiddenItems,
                backupActions = BackupSettingsActions(
                    onExportBackupJson = onExportBackupJson,
                    onParseBackupPreview = onParseBackupPreview,
                    onImportBackupJson = onImportBackupJson,
                    onRunAutoBackupNow = onRunAutoBackupNow,
                    vaultFileCount = vaultFileCount,
                    vaultTotalBytes = vaultTotalBytes,
                    lockedAuthorized = lockedAuthorized,
                    onRequestUnlock = onRequestUnlock,
                    onExportVaultBackup = onExportVaultBackup,
                    onImportVaultBackup = onImportVaultBackup,
                ),
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

private fun mediaTabIcon(mediaFilter: Int): androidx.compose.ui.graphics.vector.ImageVector = when (mediaFilter) {
    1 -> Icons.Outlined.Image
    2 -> Icons.Outlined.VideoLibrary
    else -> Icons.Outlined.PhotoLibrary
}

@Composable
private fun AnimatedNavigationIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    label: String,
    tiltEnabled: Boolean = true,
) {
    val scale by animateFloatAsState(
        if (selected) 1.16f else 1f,
        tween(260, easing = FastOutSlowInEasing),
        label = "navigation icon scale",
    )
    val rotation by animateFloatAsState(
        if (selected || !tiltEnabled) 0f else -7f,
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

internal fun selectionActionIcon(
    actionKey: String,
    allSelectedAreFavorites: Boolean = false
): androidx.compose.ui.graphics.vector.ImageVector = when (actionKey) {
    "share" -> Icons.Outlined.Share
    "move" -> Icons.Outlined.DriveFileMove
    "copy" -> Icons.Outlined.ContentCopy
    "favorite" -> if (allSelectedAreFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    "lock" -> Icons.Outlined.Lock
    "delete" -> Icons.Outlined.DeleteOutline
    "delete_permanent" -> Icons.Outlined.DeleteForever
    "select_all" -> Icons.Outlined.SelectAll
    else -> Icons.Outlined.MoreVert
}

internal fun selectionActionLabelRes(
    actionKey: String,
    allSelectedAreFavorites: Boolean = false
): Int = when (actionKey) {
    "share" -> R.string.selection_action_share
    "move" -> R.string.selection_action_move
    "copy" -> R.string.selection_action_copy
    "favorite" -> if (allSelectedAreFavorites) R.string.action_unfavorite else R.string.selection_action_favorite
    "lock" -> R.string.selection_action_lock
    "delete" -> R.string.selection_action_delete
    "delete_permanent" -> R.string.selection_action_delete_permanent
    "select_all" -> R.string.selection_action_select_all
    else -> R.string.action_more
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionPillCustomizerSheet(
    selectedActions: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val active = remember(selectedActions) {
        selectedActions.filter { it in ALL_SELECTION_PILL_ACTIONS }.distinct().ifEmpty { DEFAULT_SELECTION_PILL_ACTIONS }
    }
    val inactive = remember(active) {
        ALL_SELECTION_PILL_ACTIONS.filterNot { it in active }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dialog_customize_toolbar_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onSave(DEFAULT_SELECTION_PILL_ACTIONS) }) {
                    Text(stringResource(R.string.action_reset_defaults))
                }
            }
            Text(
                text = stringResource(R.string.dialog_customize_toolbar_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            active.forEachIndexed { index, actionKey ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = selectionActionIcon(actionKey),
                            contentDescription = null,
                            tint = if (actionKey == "delete_permanent") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(selectionActionLabelRes(actionKey)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = active.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index - 1, item)
                                    onSave(mutable)
                                }
                            },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                if (index < active.lastIndex) {
                                    val mutable = active.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index + 1, item)
                                    onSave(mutable)
                                }
                            },
                            enabled = index < active.lastIndex
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                        }
                        Switch(
                            checked = true,
                            enabled = active.size > 1,
                            onCheckedChange = { checked ->
                                if (!checked && active.size > 1) {
                                    onSave(active.filterNot { it == actionKey })
                                }
                            }
                        )
                    }
                }
            }
            if (inactive.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                inactive.forEach { actionKey ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSave(active + actionKey) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = selectionActionIcon(actionKey),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(selectionActionLabelRes(actionKey)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = false,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onSave(active + actionKey)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
