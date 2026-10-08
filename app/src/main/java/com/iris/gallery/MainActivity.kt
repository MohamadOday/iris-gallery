@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.Intent
import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.app.KeyguardManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Comment
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.produceState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.Checkbox
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.drawscope.withTransform
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifMetadata
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.loadExifMetadata
import com.iris.gallery.data.saveExifToMedia
import com.iris.gallery.data.resolveMediaUri
import com.iris.gallery.ui.ExifEditorSheet
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isGif
import com.iris.gallery.data.isPanorama
import com.iris.gallery.data.isMotionPhoto
import com.iris.gallery.data.isScreenshot
import com.iris.gallery.ui.GalleryViewModel
import com.iris.gallery.ui.DuplicateScanState
import com.iris.gallery.ui.MediaThumbnail
import com.iris.gallery.ui.ThumbnailCache
import com.iris.gallery.ui.AlbumsGrid
import com.iris.gallery.ui.MediaAlbum
import com.iris.gallery.ui.LibraryScreen
import com.iris.gallery.ui.FolderBrowserScreen
import com.iris.gallery.ui.EditorScreen
import com.iris.gallery.ui.EditChoiceBottomSheet
import com.iris.gallery.ui.launchExternalEditor
import com.iris.gallery.ui.setAsWallpaper
import com.iris.gallery.ui.AppLockScreen
import com.iris.gallery.ui.video.VideoPage
import com.iris.gallery.ui.video.Media3VideoEngine
import com.iris.gallery.data.DuplicateGroup
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.PreferredEditor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.data.AccentColor
import com.iris.gallery.ui.SettingsScreen
import com.iris.gallery.ui.AboutScreen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iris.gallery.ui.theme.IrisTheme
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.ui.rememberAppLocale
import com.iris.gallery.ui.getTimelineFormatter
import com.iris.gallery.ui.formatTimelineDate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing

import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
import com.iris.gallery.ui.AlbumPickerSheet
import java.io.File

private var isSessionAppUnlocked = false

enum class TrashFeedbackType {
    MOVED_TO_TRASH,
    RESTORED,
    PERMANENTLY_DELETED,
    MOVED_TO_ALBUM,
    COPIED_TO_ALBUM,
}

data class TrashFeedback(
    val type: TrashFeedbackType,
    val count: Int,
    val albumName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private val currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntentState.value = intent
        IrisPhotoWidget.refreshAll(this)
        enableEdgeToEdge()
        val settingsPreferences = SettingsPreferences(this)
        val initialSettings = settingsPreferences.state.value
        val isDark = when (initialSettings.themeMode) {
            com.iris.gallery.data.ThemeMode.LIGHT -> false
            com.iris.gallery.data.ThemeMode.DARK -> true
            com.iris.gallery.data.ThemeMode.SYSTEM -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_[...]
        }
        val bgColor = when {
            isDark && initialSettings.amoledBlack -> android.graphics.Color.BLACK
            isDark -> 0xFF141218.toInt()
            else -> 0xFFFFF8FF.toInt()
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (initialSettings.language.isNotEmpty()) {
            com.iris.gallery.ui.setAppLanguage(this, initialSettings.language)
        }
        setContent {
            val activeIntent = currentIntentState.value ?: intent
            val pickerMode = activeIntent.action == Intent.ACTION_PICK || activeIntent.action == Intent.ACTION_GET_CONTENT
            val isViewAction = activeIntent.action == Intent.ACTION_VIEW ||
                activeIntent.action == Intent.ACTION_EDIT ||
                activeIntent.action == "com.android.camera.action.REVIEW"
            val requestedType = activeIntent.type
            val viewUri = (activeIntent.data ?: activeIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri)
                .takeIf { isViewAction }
            val isEditAction = activeIntent.action == Intent.ACTION_EDIT

            val settings by settingsPreferences.state.collectAsStateWithLifecycle()
            IrisTheme(
                themeMode = settings.themeMode,
                amoledBlack = settings.amoledBlack,
                accentColor = settings.accentColor,
            ) {
                GalleryApp(
                    settings = settings,
                    settingsPreferences = settingsPreferences,
                    requestedType = requestedType.takeIf { pickerMode },
                    initialViewUri = viewUri,
                    initialEditMode = isEditAction,
                    initialMemories = activeIntent.getBooleanExtra("open_memories", false),
                    onPick = if (pickerMode) {{ media ->
                        val result = Intent().apply {
                            data = media.uri
                            clipData = ClipData.newUri(contentResolver, media.name, media.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    }} else null,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
    }
}

private fun requiredPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    Build.VERSION.SDK_INT <= 29 -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private enum class MediaFormatFilter(val label: String) {
    ALL("All"),
    RAW("RAW"),
    GIF("GIFs"),
    PANORAMA("Panoramas"),
    MOTION("Motion Photos"),
}

@Composable
private fun GalleryApp(
    settings: SettingsState,
    settingsPreferences: SettingsPreferences,
    requestedType: String? = null,
    initialViewUri: Uri? = null,
    initialEditMode: Boolean = false,
    initialMemories: Boolean = false,
    onPick: ((MediaImage) -> Unit)? = null,
    viewModel: GalleryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val permissions = remember { requiredPermissions() }
    var permitted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = permissions.all { permission -> it[permission] == true }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var pendingPermanentDeleteMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingPermanentDeleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingPermanentDeleteMedia
        val cb = pendingPermanentDeleteCallback
        pendingPermanentDeleteMedia = null
        pendingPermanentDeleteCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (pending != null) {
                val delIds = pending.map { it.id }.toSet()
                val delPaths = pending.map { it.path }.toSet()
                viewModel.markMediaDeleted(delIds, delPaths)
            }
            viewModel.refresh(showLoading = false)
            cb?.invoke()
        }
    }
    var pendingRename by remember { mutableStateOf<Triple<MediaImage, String, (MediaImage?) -> Unit>?>(null) }
    val renameLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingRename
        pendingRename = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            val (media, newName, callback) = pending
            viewModel.renameMedia(media, newName) { updated ->
                callback(updated)
            }
        } else {
            pending?.third?.invoke(null)
        }
    }
    var pendingMetadata by remember { mutableStateOf<Triple<MediaImage, ContentValues, Pair<ExifEditRequest, ((MediaImage?) -> Unit)?>>?>(null) }
    val metadataWriteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        val pending = pendingMetadata
        pendingMetadata = null
        if (it.resultCode == Activity.RESULT_OK && pending != null) {
            val (media, values, reqAndCb) = pending
            val (request, callback) = reqAndCb
            val uri = canonicalMediaUri(context, media)
            saveExifToMedia(context, uri, media.path, request)
            if (media.id > 0 && uri.toString().startsWith("content://media/")) {
                runCatching { context.contentResolver.update(uri, values, null, null) }
            }
            if (media.path.isNotBlank()) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(media.path), null, null)
            }
            val updated = viewModel.updateMediaMetadata(media, request)
            callback?.invoke(updated)
            Toast.makeText(context, R.string.toast_metadata_updated, Toast.LENGTH_SHORT).show()
        } else {
            pending?.third?.second?.invoke(null)
        }
    }
    var lockedAuthorized by remember { mutableStateOf(false) }
    var isAppUnlocked by remember {
        mutableStateOf(!settings.appLockEnabled || !settings.hasPin || isSessionAppUnlocked)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                lockedAuthorized = false // Only lock private vault albums
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (permitted) {
                    viewModel.refresh(showLoading = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lockedAuthorized = it.resultCode == Activity.RESULT_OK
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val vaultMedia by viewModel.vaultMedia.collectAsStateWithLifecycle()
    val duplicateState by viewModel.duplicateState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var pendingVaultMove by remember { mutableStateOf<com.iris.gallery.data.VaultMoveResult?>(null) }
    val vaultDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingVaultMove
        pendingVaultMove = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (pending != null) {
                val delIds = pending.originalMedia.map { it.id }.toSet()
                val delPaths = pending.originalMedia.map { it.path }.toSet()
                viewModel.markMediaDeleted(delIds, delPaths)
            }
            viewModel.refresh(showLoading = false)
            val count = pending?.vaultedMedia?.size ?: 0
            Toast.makeText(context, context.getString(R.string.toast_items_vaulted, count), Toast.LENGTH_SHORT).show()
        } else {
            pending?.let { moveResult ->
                coroutineScope.launch {
                    viewModel.rollbackVaultMove(moveResult.vaultedMedia)
                }
            }
            Toast.makeText(context, context.getString(R.string.toast_lock_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    var pendingTrashMove by remember { mutableStateOf<com.iris.gallery.data.TrashMoveResult?>(null) }
    var pendingTrashCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val trashDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingTrashMove
        val cb = pendingTrashCallback
        pendingTrashMove = null
        pendingTrashCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (pending != null) {
                val delIds = pending.originalMedia.map { it.id }.toSet()
                val delPaths = pending.originalMedia.map { it.path }.toSet()
                viewModel.markMediaDeleted(delIds, delPaths)
            }
            viewModel.refresh(showLoading = false)
            val count = pending?.trashedMedia?.size ?: 0
            Toast.makeText(context, context.getString(R.string.toast_items_moved_to_trash, count), Toast.LENGTH_SHORT).show()
            cb?.invoke()
        } else {
            pending?.let { moveResult ->
                coroutineScope.launch {
                    viewModel.rollbackTrashMove(moveResult.trashedMedia)
                    viewModel.refresh(showLoading = false)
                }
            }
        }
    }

    var standaloneExternalMedia by remember { mutableStateOf<MediaImage?>(null) }
    var standaloneEditorMedia by remember { mutableStateOf<MediaImage?>(null) }

    LaunchedEffect(initialViewUri, initialEditMode, permitted) {
        if (!permitted && initialViewUri != null) {
            val resolved = withContext(Dispatchers.IO) { resolveMediaUri(context, initialViewUri) }
            if (initialEditMode) {
                standaloneEditorMedia = resolved
            } else {
                standaloneExternalMedia = resolved
            }
        }
    }

    LaunchedEffect(permitted) {
        if (permitted) {
            viewModel.refresh()
            viewModel.registerObserver()
        }
    }
    LaunchedEffect(Unit) {
        MemoriesNotifications.scheduleFromSettings(context)
        if (settings.memoriesNotificationEnabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!permitted) {
        if (standaloneEditorMedia != null) {
            EditorScreen(standaloneEditorMedia!!, onClose = { standaloneEditorMedia = null }, onSaved = { saved ->
                if (saved) standaloneEditorMedia = null
                Toast.makeText(context, if (saved) context.getString(R.string.toast_edited_saved) else context.getString(R.string.toast_edited_failed), Toast.LENGTH_SHORT).show()
            })
        } else if (standaloneExternalMedia != null) {
            PhotoViewer(
                images = listOf(standaloneExternalMedia!!),
                initialPage = 0,
                favorites = emptySet(),
                autoPlay = settings.autoPlayVideo,
                loop = settings.loopVideo,
                videoDoubleTapToZoom = settings.videoDoubleTapToZoom,
                showViewerUserComments = settings.showViewerUserComments,
                showFilmstrip = settings.showFilmstrip,
                dismissedFilmstripTip = settings.dismissedFilmstripTip,
                onDismissFilmstripTip = { settingsPreferences.setDismissedFilmstripTip(true) },
                pinchToRotate = settings.pinchToRotate,
                dismissedRotateTip = settings.dismissedRotateTip,
                onDismissRotateTip = { settingsPreferences.setDismissedRotateTip(true) },
                doubleTapZoomLevel = settings.doubleTapZoomLevel,
                timelineDateFormat = settings.timelineDateFormat,
                customTimelineDateFormat = settings.customTimelineDateFormat,
                smartYearHiding = settings.smartYearHiding,
                isLocked = false,
                isInTrash = false,
                confirmDeleteSetting = settings.confirmDelete,
                preferredEditor = settings.preferredEditor,
                onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
                availableAlbums = emptyList(),
                onToggleFavorite = { },
                onClose = { standaloneExternalMedia = null },
                onDelete = { item, _ ->
                    standaloneExternalMedia = null
                    runCatching { context.contentResolver.delete(item.uri, null, null) }
                },
                onRestore = { },
                onEditMetadata = { _, _ -> },
                onEdit = { standaloneEditorMedia = it; standaloneExternalMedia = null },
                onLock = { },
                onUnlock = { },
                onMoveToAlbum = { _, _, _ -> },
                onCopyToAlbum = { _, _, _ -> },
            )
        } else {
            PermissionScreen { permissionLauncher.launch(permissions) }
        }
    } else {
        AnimatedContent(
            targetState = (!settings.appLockEnabled || !settings.hasPin || isAppUnlocked),
            transitionSpec = {
                (fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                 scaleIn(initialScale = 0.94f, animationSpec = tween(320, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 1.06f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    )
            },
            label = "AppLockTransition"
        ) { unlocked ->
            if (!unlocked) {
                AppLockScreen(
                    isPicker = onPick != null,
                    biometricsEnabled = settings.appLockBiometricsEnabled,
                    onVerifyPin = { pin -> settingsPreferences.verifyPin(pin) },
                    onUnlocked = {
                        isSessionAppUnlocked = true
                        isAppUnlocked = true
                    }
                )
            } else {
                val visibleMedia = remember(state.images, requestedType, libraryState.lockedMedia, libraryState.excludedFolders) {
                    val requested = when {
                        requestedType?.startsWith("image/") == true -> state.images.filterNot { it.isVideo }
                        requestedType?.startsWith("video/") == true -> state.images.filter { it.isVideo }
                        else -> state.images
                    }
                    requested.filterNot { it.id in libraryState.lockedMedia }
                        .filterNot { img ->
                            libraryState.excludedFolders.any { excluded ->
                                val clean = excluded.trimEnd('/')
                                img.path == clean || img.path.startsWith("$clean/")
                            }
                        }
                }
                val allLockedMedia = remember(vaultMedia, state.images, libraryState.lockedMedia) {
                    val galleryLocked = state.images.filter { it.id in libraryState.lockedMedia }
                    vaultMedia + galleryLocked
                }
                var showAllFilesAccessPromptDialog by remember { mutableStateOf(false) }
                var pendingVaultItems by remember { mutableStateOf<List<MediaImage>?>(null) }

                fun executeVaultMove(mediaList: List<MediaImage>) {
                    coroutineScope.launch {
                        val moveResult = viewModel.moveToVault(mediaList)
                        if (moveResult.vaultedMedia.isNotEmpty()) {
                            if (moveResult.silentSuccess) {
                                viewModel.refresh()
                                Toast.makeText(context, context.getString(R.string.toast_items_vaulted, moveResult.vaultedMedia.size), Toast.LENGTH_SHORT).show()
                            } else if (Build.VERSION.SDK_INT >= 30) {
                                runCatching {
                                    pendingVaultMove = moveResult
                                    val request = MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        moveResult.originalMedia.map { canonicalMediaUri(it) }
                                    )
                                    vaultDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                }.onFailure {
                                    pendingVaultMove = null
                                    viewModel.rollbackVaultMove(moveResult.vaultedMedia)
                                    Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val allDeleted = moveResult.originalMedia.all { item ->
                                    runCatching {
                                        context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0 ||
                                        java.io.File(item.path).delete()
                                    }.getOrDefault(false)
                                }
                                if (allDeleted) {
                                    viewModel.refresh()
                                    Toast.makeText(context, context.getString(R.string.toast_items_vaulted, moveResult.vaultedMedia.size), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.rollbackVaultMove(moveResult.vaultedMedia)
                                    Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                val onLockMedia: (List<MediaImage>) -> Unit = { mediaList ->
                    if (mediaList.isNotEmpty()) {
                        if (settings.vaultHideFromStorage) {
                            val hasAccess = if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true
                            if (hasAccess) {
                                executeVaultMove(mediaList)
                            } else {
                                pendingVaultItems = mediaList
                                showAllFilesAccessPromptDialog = true
                            }
                        } else {
                            viewModel.setLocked(mediaList.map { it.id }, true)
                            Toast.makeText(context, "${mediaList.size} item(s) hidden in Iris Gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val onUnlockMedia: (List<MediaImage>) -> Unit = { mediaList ->
                    if (mediaList.isNotEmpty()) {
                        val vaultItems = mediaList.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                        val galleryLockedIds = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }.map { it.id }
                        coroutineScope.launch {
                            if (vaultItems.isNotEmpty()) {
                                viewModel.restoreFromVault(vaultItems)
                            }
                            if (galleryLockedIds.isNotEmpty()) {
                                viewModel.setLocked(galleryLockedIds, false)
                            }
                            viewModel.refresh()
                            Toast.makeText(context, "${mediaList.size} item(s) restored from vault", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val onRescanMedia: () -> Unit = {
                    Toast.makeText(context, R.string.toast_rescan_started, Toast.LENGTH_SHORT).show()
                    viewModel.rescanMedia {
                        Toast.makeText(context, R.string.toast_rescan_completed, Toast.LENGTH_SHORT).show()
                    }
                }
                val onDeleteFromLocked: (List<MediaImage>) -> Unit = { mediaList ->
                    if (mediaList.isNotEmpty()) {
                        val vaultItems = mediaList.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                        val galleryLockedItems = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                        if (vaultItems.isNotEmpty()) {
                            coroutineScope.launch {
                                val delIds = vaultItems.map { it.id }.toSet()
                                val delPaths = vaultItems.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                viewModel.deletePermanentlyFromVault(vaultItems)
                                viewModel.refresh(showLoading = false)
                                Toast.makeText(context, "${vaultItems.size} item(s) permanently deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (galleryLockedItems.isNotEmpty()) {
                            if (Build.VERSION.SDK_INT >= 30) runCatching {
                                val request = MediaStore.createDeleteRequest(context.contentResolver,
                                    galleryLockedItems.map { canonicalMediaUri(it) })
                                pendingPermanentDeleteMedia = galleryLockedItems
                                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                            }.onFailure {
                                pendingPermanentDeleteMedia = null
                                Toast.makeText(context, "Could not request deletion", Toast.LENGTH_LONG).show()
                            }
                            else runCatching {
                                val delIds = galleryLockedItems.map { it.id }.toSet()
                                val delPaths = galleryLockedItems.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                galleryLockedItems.forEach { context.contentResolver.delete(canonicalMediaUri(it), null, null) }
                                viewModel.refresh(showLoading = false)
                            }
                        }
                    }
                }
                AnimatedContent(
                    targetState = settings.language,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                         scaleIn(initialScale = 0.98f, animationSpec = tween(280, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.01f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "language_transition"
                ) { _ ->
                    GalleryScaffold(
                        settings = settings,
                        settingsPreferences = settingsPreferences,
                        images = visibleMedia,
                        trashed = state.trashed,
                        lockedIds = libraryState.lockedMedia,
                        lockedMedia = allLockedMedia,
                        pinnedAlbums = libraryState.pinnedAlbums,
                        albumCovers = libraryState.albumCovers,
                        albumSort = libraryState.albumSort,
                        albumOrder = libraryState.albumOrder,
                        albumMediaSort = libraryState.albumMediaSort,
                        lockedAuthorized = lockedAuthorized,
                        loading = state.loading,
                        error = state.error,
                        favorites = favorites,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onLockMedia = onLockMedia,
                        onUnlockMedia = onUnlockMedia,
                        onDeleteFromLocked = onDeleteFromLocked,
                        onTogglePinnedAlbum = viewModel::togglePinnedAlbum,
                        onSetAlbumCover = viewModel::setAlbumCover,
                        onSetAlbumSort = viewModel::setAlbumSort,
                        onSetAlbumOrder = viewModel::setAlbumOrder,
                        onSetAlbumMediaSort = viewModel::setAlbumMediaSort,
                        excludedFolders = libraryState.excludedFolders,
                        onAddExcludedFolder = viewModel::addExcludedFolder,
                        onRemoveExcludedFolder = viewModel::removeExcludedFolder,
                        onRequestUnlock = {
                            if (!settings.biometricLockEnabled) {
                                lockedAuthorized = true
                            } else {
                                val keyguard = context.getSystemService(KeyguardManager::class.java)
                                val intent = keyguard?.createConfirmDeviceCredentialIntent("Unlock Iris", "View your locked media")
                                if (intent == null) lockedAuthorized = true else unlockLauncher.launch(intent)
                            }
                        },
                        onPick = onPick,
                        onTrash = { media, onConfirmed ->
                            if (media.isNotEmpty()) {
                                coroutineScope.launch {
                                    val moveResult = viewModel.moveToTrash(media)
                                    if (moveResult.trashedMedia.isNotEmpty()) {
                                        if (moveResult.silentSuccess) {
                                            val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                            val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                            viewModel.markMediaDeleted(delIds, delPaths)
                                            viewModel.refresh(showLoading = false)
                                            onConfirmed?.invoke()
                                            Toast.makeText(context, context.getString(R.string.toast_items_moved_to_trash, moveResult.trashedMedia.size), Toast.LENGTH_SHORT).show()
                                        } else if (Build.VERSION.SDK_INT >= 30) {
                                            runCatching {
                                                pendingTrashMove = moveResult
                                                pendingTrashCallback = onConfirmed
                                                val request = MediaStore.createDeleteRequest(
                                                    context.contentResolver,
                                                    moveResult.originalMedia.map { canonicalMediaUri(it) }
                                                )
                                                trashDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                            }.onFailure {
                                                pendingTrashMove = null
                                                pendingTrashCallback = null
                                                viewModel.rollbackTrashMove(moveResult.trashedMedia)
                                                Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val allDeleted = moveResult.originalMedia.all { item ->
                                                runCatching {
                                                    context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0 ||
                                                    java.io.File(item.path).delete()
                                                }.getOrDefault(false)
                                            }
                                            if (allDeleted) {
                                                val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                viewModel.markMediaDeleted(delIds, delPaths)
                                                viewModel.refresh(showLoading = false)
                                                onConfirmed?.invoke()
                                                Toast.makeText(context, context.getString(R.string.toast_items_moved_to_trash, moveResult.trashedMedia.size), Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.rollbackTrashMove(moveResult.trashedMedia)
                                                Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onRestore = { media ->
                            if (media.isNotEmpty()) {
                                coroutineScope.launch {
                                    viewModel.restoreFromTrash(media)
                                }
                            }
                        },
                        onDeletePermanently = { media, onConfirmed ->
                            if (media.isNotEmpty()) {
                                val internalItems = media.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                                val externalItems = media.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                                coroutineScope.launch {
                                    if (internalItems.isNotEmpty()) {
                                        val delIds = internalItems.map { it.id }.toSet()
                                        val delPaths = internalItems.map { it.path }.toSet()
                                        viewModel.markMediaDeleted(delIds, delPaths)
                                        viewModel.deletePermanently(internalItems)
                                        if (externalItems.isEmpty()) {
                                            onConfirmed?.invoke()
                                        }
                                    }
                                    if (externalItems.isNotEmpty()) {
                                        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                                            runCatching {
                                                val request = MediaStore.createDeleteRequest(
                                                    context.contentResolver,
                                                    externalItems.map { canonicalMediaUri(it) }
                                                )
                                                pendingPermanentDeleteMedia = externalItems
                                                pendingPermanentDeleteCallback = onConfirmed
                                                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                            }.onFailure {
                                                pendingPermanentDeleteMedia = null
                                                pendingPermanentDeleteCallback = null
                                                Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val delIds = externalItems.map { it.id }.toSet()
                                            val delPaths = externalItems.map { it.path }.toSet()
                                            viewModel.markMediaDeleted(delIds, delPaths)
                                            viewModel.deletePermanently(externalItems)
                                            onConfirmed?.invoke()
                                        }
                                    }
                                }
                            }
                        },
                        onRescanMedia = onRescanMedia,
                        onEditMetadata = { media, request, callback ->
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, request.displayName)
                                if (request.stripAllExif) {
                                    putNull(MediaStore.MediaColumns.TITLE)
                                    putNull(MediaStore.Images.Media.DESCRIPTION)
                                } else {
                                    if (request.title.isNotBlank()) {
                                        put(MediaStore.MediaColumns.TITLE, request.title)
                                    } else {
                                        putNull(MediaStore.MediaColumns.TITLE)
                                    }
                                    if (request.imageDescription != null) {
                                        put(MediaStore.Images.Media.DESCRIPTION, request.imageDescription)
                                    } else {
                                        putNull(MediaStore.Images.Media.DESCRIPTION)
                                    }
                                }
                                put(MediaStore.Images.Media.DATE_TAKEN, request.dateTakenMillis)
                                put(MediaStore.Images.Media.ORIENTATION, request.orientation)
                            }

                            val uri = canonicalMediaUri(context, media)
                            val directSaved = runCatching {
                                saveExifToMedia(context, uri, media.path, request)
                            }
                            if (directSaved.isFailure) {
                                callback?.invoke(null)
                                return@onEditMetadata
                            }
                            if (media.id > 0 && uri.toString().startsWith("content://media/")) {
                                runCatching { context.contentResolver.update(uri, values, null, null) }
                            }
                            if (media.path.isNotBlank()) {
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(media.path), null, null)
                            }
                            val updated = viewModel.updateMediaMetadata(media, request)
                            callback?.invoke(updated)
                        },
                        onShare = { mediaList, shareTo, isLocked, callback ->
                            if (mediaList.isEmpty()) return@GalleryScaffold
                            val shareUris = mediaList.map { canonicalMediaUri(context, it) }
                            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "image/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            callback?.invoke(shareIntent)
                        },
                        onSetAsWallpaper = { media ->
                            setAsWallpaper(context, media)
                        },
                        onOpenExternal = { media ->
                            launchExternalEditor(context, media)
                        },
                        onLaunchViewer = { list, idx ->
                            // handled elsewhere
                        },
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
