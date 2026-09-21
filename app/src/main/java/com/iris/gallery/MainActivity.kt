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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
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
import com.iris.gallery.data.ViewerHeaderStyle
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
            com.iris.gallery.data.ThemeMode.SYSTEM -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
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
    var lastBackgroundTimestamp by remember { mutableStateOf(0L) }
    var isAppUnlocked by remember {
        mutableStateOf(!settings.appLockEnabled || !settings.hasPin || isSessionAppUnlocked)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                lastBackgroundTimestamp = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (permitted) {
                    viewModel.refresh(showLoading = false)
                }
                if (lastBackgroundTimestamp > 0L && System.currentTimeMillis() - lastBackgroundTimestamp > 300_000L) {
                    lockedAuthorized = false
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
                        albumMediaSortOverrides = libraryState.albumMediaSortOverrides,
                        lockedAuthorized = lockedAuthorized,
                        onLockVault = { lockedAuthorized = false },
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
                        onSetAlbumMediaSortOverride = viewModel::setAlbumMediaSortOverride,
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
                            }.getOrDefault(false)

                            val mediaStoreUpdated = runCatching {
                                if (media.id > 0 && uri.toString().startsWith("content://media/")) {
                                    context.contentResolver.update(uri, values, null, null) > 0
                                } else false
                            }.getOrDefault(false)

                            if (directSaved || mediaStoreUpdated) {
                                if (media.path.isNotBlank()) {
                                    android.media.MediaScannerConnection.scanFile(context, arrayOf(media.path), null, null)
                                }
                                val updated = viewModel.updateMediaMetadata(media, request)
                                callback(updated)
                                Toast.makeText(context, R.string.toast_metadata_updated, Toast.LENGTH_SHORT).show()
                            } else if (Build.VERSION.SDK_INT >= 30) {
                                runCatching {
                                    if (!uri.toString().startsWith("content://media/")) {
                                        error("Non-MediaStore URI cannot request write permission: $uri")
                                    }
                                    pendingMetadata = Triple(media, values, Pair(request, callback))
                                    val writeReq = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                                    metadataWriteLauncher.launch(IntentSenderRequest.Builder(writeReq.intentSender).build())
                                }.onFailure { e ->
                                    android.util.Log.e("IrisGallery", "Failed to create write request for metadata", e)
                                    pendingMetadata = null
                                    callback(null)
                                    Toast.makeText(context, context.getString(R.string.toast_could_not_request_metadata), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                callback(null)
                                Toast.makeText(context, context.getString(R.string.toast_could_not_request_metadata), Toast.LENGTH_LONG).show()
                            }
                        },
                        duplicateState = duplicateState,
                        onScanDuplicates = viewModel::scanDuplicates,
                        onCancelDuplicateScan = viewModel::cancelDuplicateScan,
                        onMoveToAlbum = { media, dir, name ->
                            coroutineScope.launch {
                                viewModel.moveMediaToAlbum(media, dir, name)
                            }
                        },
                        onCopyToAlbum = { media, dir, name ->
                            coroutineScope.launch {
                                viewModel.copyMediaToAlbum(media, dir, name)
                            }
                        },
                        getAlbumDir = viewModel::getAlbumDirectory,
                        createAlbumDir = viewModel::createNewAlbumDirectory,
                        onRefresh = { viewModel.refresh() },
                        onRenameMedia = { media, newName, callback ->
                            viewModel.renameMedia(media, newName) { updated ->
                                if (updated != null) {
                                    callback(updated)
                                } else if (Build.VERSION.SDK_INT >= 30) {
                                    runCatching {
                                        pendingRename = Triple(media, newName, callback)
                                        val writeReq = MediaStore.createWriteRequest(
                                            context.contentResolver,
                                            listOf(canonicalMediaUri(media))
                                        )
                                        renameLauncher.launch(IntentSenderRequest.Builder(writeReq.intentSender).build())
                                    }.onFailure {
                                        pendingRename = null
                                        callback(null)
                                    }
                                } else {
                                    callback(null)
                                }
                            }
                        },
                        initialMemories = initialMemories,
                        initialViewUri = initialViewUri,
                        initialEditMode = initialEditMode,
                    )
                }

                if (showAllFilesAccessPromptDialog) {
                    val pending = pendingVaultItems
                    AlertDialog(
                        onDismissRequest = { showAllFilesAccessPromptDialog = false; pendingVaultItems = null },
                        icon = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text(stringResource(R.string.prompt_free_vault_title)) },
                        text = {
                            Text(stringResource(R.string.prompt_free_vault_desc))
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAllFilesAccessPromptDialog = false
                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                runCatching { context.startActivity(intent) }
                                    .onFailure {
                                        runCatching {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                        }
                                    }
                            }) {
                                Text(stringResource(R.string.action_grant_access))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAllFilesAccessPromptDialog = false
                                if (pending != null) {
                                    val lockedIds = pending.map { it.id }.toSet()
                                    viewModel.setLocked(lockedIds, true)
                                    viewModel.refresh()
                                    Toast.makeText(context, "${pending.size} item(s) hidden in Iris Gallery", Toast.LENGTH_SHORT).show()
                                }
                                pendingVaultItems = null
                            }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun canonicalMediaUri(context: android.content.Context, item: MediaImage): Uri {
    if (item.uri.toString().startsWith("content://media/")) {
        return item.uri
    }
    if (item.uri.scheme != "file" && item.id > 0) {
        return if (item.isVideo) ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
    }
    if (item.path.isNotBlank()) {
        val table = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        runCatching {
            context.contentResolver.query(
                table,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(item.path),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    if (id > 0) {
                        return ContentUris.withAppendedId(table, id)
                    }
                }
            }
        }
    }
    return item.uri
}

private fun canonicalMediaUri(item: MediaImage): Uri {
    return if (item.uri.toString().startsWith("content://media/")) {
        item.uri
    } else if (item.uri.scheme != "file" && item.id > 0) {
        if (item.isVideo) ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
    } else {
        item.uri
    }
}

private fun getShareUri(context: android.content.Context, item: MediaImage): Uri {
    return if (item.path.startsWith(context.filesDir.absolutePath)) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(item.path)
        )
    } else {
        item.uri
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.permission_desc), modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.permission_button)) }
    }
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
            kotlinx.coroutines.coroutineScope {
                launch {
                    dismissAlpha.animateTo(0f, tween(220, easing = LinearOutSlowInEasing))
                }
                launch {
                    dismissScale.animateTo(0.65f, tween(220, easing = LinearOutSlowInEasing))
                }
            }
            isDismissingInPlace = false
            dismissAlpha.snapTo(1f)
            dismissScale.snapTo(1f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScaffold(
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
    onDeleteFromLocked: (List<MediaImage>) -> Unit,
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
    onMoveToAlbum: (List<MediaImage>, java.io.File, String) -> Unit,
    onCopyToAlbum: (List<MediaImage>, java.io.File, String) -> Unit,
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
) {
    val context = LocalContext.current
    val tabPagerState = rememberPagerState(
        initialPage = if (initialMemories) 3 else settings.startupTab.pageIndex,
        pageCount = { 4 }
    )
    val tabScope = rememberCoroutineScope()
    val destination = tabPagerState.targetPage
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

    var initialUriHandled by remember { mutableStateOf(false) }
    LaunchedEffect(initialViewUri, initialEditMode) {
        if (initialViewUri != null && !initialUriHandled) {
            initialUriHandled = true
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
        }
    }
    var customCellSize by remember { mutableStateOf(settings.photoGridSize.dp) }
    var customAlbumCellSize by remember { mutableStateOf(settings.albumGridSize.dp) }
    var compactGrid by remember { mutableStateOf(settings.photoGridSize < 95f) }

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
    val photoGridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()
    val albumPhotoGridState = rememberLazyGridState()
    val favoriteGridState = rememberLazyGridState()
    val libraryGridState = rememberLazyGridState()
    val photoTabLabel = stringResource(R.string.tab_photos)
    val albumsTabLabel = stringResource(R.string.tab_albums)
    val favoritesTabLabel = stringResource(R.string.tab_favorites)
    val libraryTabLabel = stringResource(R.string.tab_library)
    val labels = listOf(photoTabLabel, albumsTabLabel, favoritesTabLabel, libraryTabLabel)
    val icons = remember { listOf(Icons.Outlined.PhotoLibrary, Icons.Outlined.PhotoAlbum, Icons.Outlined.FavoriteBorder, Icons.Outlined.Dashboard) }
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

    val displayedPhotos = remember(images, fileSearchQuery) { filterMediaList(images, fileSearchQuery) }
    val displayedFavoritePhotos = remember(favoriteImages, fileSearchQuery) { filterMediaList(favoriteImages, fileSearchQuery) }
    val effectiveAlbumMediaSort = selectedAlbum?.id?.let { albumMediaSortOverrides[it] } ?: albumMediaSort
    val displayedAlbumPhotos = remember(selectedAlbum, fileSearchQuery, effectiveAlbumMediaSort) {
        selectedAlbum?.let { album ->
            val filtered = filterMediaList(album.images, fileSearchQuery)
            when (effectiveAlbumMediaSort) {
                MediaSort.DATE_DESC -> filtered.sortedWith(compareByDescending<MediaImage> { it.dateTaken }.thenByDescending { it.id })
                MediaSort.DATE_ASC -> filtered.sortedWith(compareBy<MediaImage> { it.dateTaken }.thenBy { it.id })
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
        else -> displayedPhotos
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
        val uris = ArrayList(selected.map { getShareUri(context, it) })
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (uris.size == 1) selected.first().mimeType.ifBlank { "*/*" } else "*/*"
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share_media)))
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
    var trashFeedback by remember { mutableStateOf<TrashFeedback?>(null) }
    LaunchedEffect(trashFeedback) {
        if (trashFeedback != null) {
            kotlinx.coroutines.delay(1800)
            trashFeedback = null
        }
    }

    fun handleTrash(items: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, items.size)
            onTrash(items, onConfirmed)
        }
    }
    fun handleRestore(items: List<MediaImage>) {
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, items.size)
            onRestore(items)
        }
    }
    fun handleDeletePermanently(items: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, items.size)
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
            if (current == index) {
                if (index == 1) selectedAlbumId = null
                if (index == 3) librarySection = null
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
            TopAppBar(
                title = {
                    if (isFileSearching) {
                        OutlinedTextField(
                            value = fileSearchQuery,
                            onValueChange = { fileSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = { Text(stringResource(R.string.search_files_placeholder), style = MaterialTheme.typography.bodyLarge) },
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
                                else -> photoTabLabel
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
                    if (isFileSearching) {
                        IconButton(onClick = { isFileSearching = false; fileSearchQuery = "" }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.clear_search))
                        }
                    } else if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = ::clearSelection) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear_selection)) }
                    } else if (destination == 1 && selectedAlbum != null) {
                        IconButton(onClick = { selectedAlbumId = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, albumsTabLabel) }
                    } else if (destination == 3 && librarySection == "locked" && selectedLockedAlbum != null) {
                        IconButton(onClick = { selectedLockedAlbum = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.section_locked)) }
                    } else if (destination == 3 && librarySection != null) {
                        IconButton(onClick = { librarySection = null; selectedLockedAlbum = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, libraryTabLabel) }
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
                                } else if (settings.confirmDelete) {
                                    pendingDeleteItems = selected
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
                                DropdownMenuItem(text = { Text(stringResource(R.string.action_favorite)) }, leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null) },
                                    onClick = {
                                        selectionMenuExpanded = false
                                        val makeFavorite = selectedIds.any { it !in favorites }
                                        selectedIds.forEach { id -> if ((id in favorites) != makeFavorite) onToggleFavorite(id) }
                                        clearSelection()
                                    })
                            }
                        }
                    } else if (!isFileSearching) {
                        val canSearch = destination == 0 || destination == 2 || (destination == 1 && selectedAlbum != null)
                        if (canSearch) {
                            IconButton(onClick = { isFileSearching = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.action_search))
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
                                            onLockMedia(selectedAlbum!!.images)
                                            selectedAlbumId = null
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
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = destination == index,
                                onClick = { handleTabSelected(index) },
                                icon = { AnimatedNavigationIcon(icons[index], destination == index, label) },
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
        error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(error) }
        else -> HorizontalPager(
          state = tabPagerState,
          beyondViewportPageCount = 3,
          userScrollEnabled = selectedIds.isEmpty(),
          modifier = Modifier.fillMaxSize(),
        ) { page ->
          when (page) {
            0 -> {
              val pullRefreshState0 = rememberPullToRefreshState()
              PullToRefreshBox(
                state = pullRefreshState0,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                  IrisPullToRefreshIndicator(
                    state = pullRefreshState0,
                    isRefreshing = loading,
                  )
                }
              ) {
                if (displayedPhotos.isNotEmpty()) PhotoGrid(
                  displayedPhotos, padding, photoGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
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
                  onToggleSelection = if (onPick == null) ::toggleSelection else null,
                  onSetSelection = if (onPick == null) ::setSelection else null,
                  onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = displayedPhotos; selectedId = it.id } }
                else if (fileSearchQuery.isNotBlank()) EmptyState(stringResource(R.string.empty_search_files, fileSearchQuery), padding)
                else EmptyState(stringResource(R.string.empty_photos), padding)
              }
            }
            1 -> {
              val pullRefreshState1 = rememberPullToRefreshState()
              PullToRefreshBox(
                state = pullRefreshState1,
                isRefreshing = loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                  IrisPullToRefreshIndicator(
                    state = pullRefreshState1,
                    isRefreshing = loading,
                  )
                }
              ) {
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
                  selectedIds = selectedIds, onToggleSelection = if (onPick == null) ::toggleSelection else null,
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
            else -> {
                if (page == 3) {
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
                            val allRaw = remember(images) { images.filter { it.isRaw } }
                            val allGifs = remember(images) { images.filter { it.isGif } }
                            val allPanos = remember(images) { images.filter { it.isPanorama } }
                            val allMotion = remember(images) { images.filter { it.isMotionPhoto } }
                            val allSpecial = remember(allRaw, allGifs, allPanos, allMotion) {
                                (allRaw + allGifs + allPanos + allMotion).distinctBy { it.id }.sortedByDescending { it.dateTaken }
                            }
                            val currentFiltered = when (formatFilter) {
                                MediaFormatFilter.ALL -> allSpecial
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
                    return@HorizontalPager
                }
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
                    onToggleSelection = if (onPick == null) ::toggleSelection else null,
                    onSetSelection = if (onPick == null) ::setSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
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
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = destination == index,
                        onClick = { handleTabSelected(index) },
                        icon = { AnimatedNavigationIcon(icons[index], destination == index, label) },
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
            onRemoveExcludedFolder = onRemoveExcludedFolder,
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
        val isPermanentMode = isInTrash || isLockedSection
        var deletePermanently by remember { mutableStateOf(isPermanentMode) }
        AlertDialog(
            onDismissRequest = { pendingDeleteItems = null },
            title = {
                Text(
                    if (isPermanentMode) {
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
                        else if (isPermanentMode) stringResource(R.string.delete_permanent_desc)
                        else stringResource(R.string.delete_trash_desc)
                    )
                    if (!isPermanentMode) {
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
                        trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, toDelete.size)
                        onDeleteFromLocked(toDelete)
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
                clearSelection()
                if (action == AlbumAction.MOVE) {
                    trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, toProcess.size, album.name)
                    onMoveToAlbum(toProcess, targetDir, album.name)
                } else {
                    trashFeedback = TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, toProcess.size, album.name)
                    onCopyToAlbum(toProcess, targetDir, album.name)
                }
            },
            onCreateAlbum = { newName ->
                val action = albumPickerAction!!
                val toProcess = pendingAlbumMedia!!
                val targetDir = createAlbumDir(newName)
                albumPickerAction = null
                pendingAlbumMedia = null
                clearSelection()
                if (action == AlbumAction.MOVE) {
                    trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, toProcess.size, newName)
                    onMoveToAlbum(toProcess, targetDir, newName)
                } else {
                    trashFeedback = TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, toProcess.size, newName)
                    onCopyToAlbum(toProcess, targetDir, newName)
                }
            }
        )
    }

    BackHandler(enabled = isFileSearching) { isFileSearching = false; fileSearchQuery = "" }
    BackHandler(enabled = !isFileSearching && selectedIds.isNotEmpty()) { clearSelection() }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum != null) { selectedAlbumId = null }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection == "locked" && selectedLockedAlbum != null) { selectedLockedAlbum = null }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection != null && (librarySection != "locked" || selectedLockedAlbum == null)) { librarySection = null; selectedLockedAlbum = null }
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
            timelineDateFormat = settings.timelineDateFormat,
            customTimelineDateFormat = settings.customTimelineDateFormat,
            smartYearHiding = settings.smartYearHiding,
            isLocked = false,
            isInTrash = false,
            confirmDeleteSetting = settings.confirmDelete,
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
            onMoveToAlbum = { mediaList, dir, name ->
                trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, mediaList.size, name)
                onMoveToAlbum(mediaList, dir, name)
                externalMedia = null
            },
            onCopyToAlbum = { mediaList, dir, name ->
                trashFeedback = TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, mediaList.size, name)
                onCopyToAlbum(mediaList, dir, name)
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
            timelineDateFormat = settings.timelineDateFormat,
            customTimelineDateFormat = settings.customTimelineDateFormat,
            smartYearHiding = settings.smartYearHiding,
            isLocked = isViewingLocked,
            isInTrash = isViewingTrash,
            confirmDeleteSetting = settings.confirmDelete,
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
                    trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, 1)
                    onDeleteFromLocked(listOf(media))
                    onConfirmedAction()
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
            onMoveToAlbum = { mediaList, dir, name ->
                trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_ALBUM, mediaList.size, name)
                onMoveToAlbum(mediaList, dir, name)
            },
            onCopyToAlbum = { mediaList, dir, name ->
                trashFeedback = TrashFeedback(TrashFeedbackType.COPIED_TO_ALBUM, mediaList.size, name)
                onCopyToAlbum(mediaList, dir, name)
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
            EditorScreen(image, onClose = { editorImage = null }, onSaved = { saved ->
                if (saved) editorImage = null
                Toast.makeText(context, if (saved) context.getString(R.string.toast_edited_saved) else context.getString(R.string.toast_edited_failed),
                    Toast.LENGTH_SHORT).show()
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
                onRemoveExcludedFolder = onRemoveExcludedFolder,
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
private fun DuplicateReviewScreen(
    state: DuplicateScanState,
    padding: PaddingValues,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onOpen: (DuplicateGroup, MediaImage) -> Unit,
    onTrash: (List<MediaImage>) -> Unit,
) {
    var selectedIds by remember(state.groups) {
        mutableStateOf(state.groups.flatMap { it.items.drop(1) }.mapTo(mutableSetOf()) { it.id }.toSet())
    }
    val layoutDir = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDir)
    val endPadding = padding.calculateEndPadding(layoutDir)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.duplicates_hero_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.duplicates_hero_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    when {
                        state.scanning -> {
                            val progress = if (state.total == 0) 0f else state.done.toFloat() / state.total
                            LinearProgressIndicator({ progress }, Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.duplicates_comparing_progress, state.done, state.total), style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                            }
                        }
                        else -> Button(onClick = onScan) { Text(if (state.hasScanned) stringResource(R.string.duplicates_scan_again) else stringResource(R.string.duplicates_scan_library)) }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (state.hasScanned && state.groups.isEmpty() && state.error == null) {
            item { Text(stringResource(R.string.duplicates_empty_state),
                modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium) }
        }
        if (state.groups.isNotEmpty()) {
            item {
                val selected = state.groups.flatMap { it.items }.filter { it.id in selectedIds }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.duplicates_groups_count, state.groups.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.duplicates_suggested_removal, selected.size, formatBytes(selected.sumOf { it.sizeBytes })),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(enabled = selected.isNotEmpty(), onClick = { onTrash(selected) }) { Text(stringResource(R.string.duplicates_review_delete)) }
                }
            }
            items(state.groups, key = { group -> group.items.joinToString { it.id.toString() } }) { group ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (group.exact) stringResource(R.string.duplicates_exact_copies) else stringResource(R.string.duplicates_similar_photos), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.duplicates_save_up_to, formatBytes(group.reclaimableBytes)),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(group.items, key = { it.id }) { media ->
                                val selected = media.id in selectedIds
                                Box(Modifier.size(126.dp).clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedIds = selectedIds.toMutableSet().apply {
                                        if (!add(media.id)) remove(media.id)
                                    } }) {
                                    MediaThumbnail(media, Modifier.fillMaxSize())
                                    Surface(Modifier.align(Alignment.TopEnd).padding(6.dp),
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = .86f)) {
                                        Icon(if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Close,
                                            if (selected) stringResource(R.string.duplicates_remove) else stringResource(R.string.duplicates_keep), Modifier.padding(5.dp).size(18.dp),
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                    }
                                    TextButton(onClick = { onOpen(group, media) },
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .82f))) { Text(stringResource(R.string.duplicates_preview)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    images: List<MediaImage>,
    padding: PaddingValues,
    gridState: LazyGridState,
    cellSize: androidx.compose.ui.unit.Dp,
    onCellSizeChange: ((androidx.compose.ui.unit.Dp) -> Unit)? = null,
    showTimeline: Boolean = false,
    timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    useRelativeDates: Boolean = true,
    showDayOfWeek: Boolean = false,
    abbreviateDayOfWeek: Boolean = false,
    smartYearHiding: Boolean = true,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    gridSpacing: GridSpacing = GridSpacing.STANDARD,
    showVideoDuration: Boolean = true,
    showFormatBadge: Boolean = true,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelection: ((Long) -> Unit)? = null,
    onSetSelection: ((Long, Boolean) -> Unit)? = null,
    onSetDateSelection: ((List<Long>, Boolean) -> Unit)? = null,
    onOpen: (MediaImage) -> Unit,
) {
    val currentLocale = rememberAppLocale()
    val todayString = stringResource(R.string.timeline_today)
    val yesterdayString = stringResource(R.string.timeline_yesterday)

    val sameYearFormatter = remember(currentLocale, timelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, customTimelineDateFormat) {
        getTimelineFormatter(timelineDateFormat, isSameYear = true, showDayOfWeek = showDayOfWeek, locale = currentLocale, customPattern = customTimelineDateFormat, abbreviateDayOfWeek = abbreviateDayOfWeek, smartYearHiding = smartYearHiding)
    }
    val otherYearFormatter = remember(currentLocale, timelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, customTimelineDateFormat) {
        getTimelineFormatter(timelineDateFormat, isSameYear = false, showDayOfWeek = showDayOfWeek, locale = currentLocale, customPattern = customTimelineDateFormat, abbreviateDayOfWeek = abbreviateDayOfWeek, smartYearHiding = smartYearHiding)
    }

    val groups = remember(images, showTimeline, timelineDateFormat, customTimelineDateFormat, useRelativeDates, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, currentLocale, todayString, yesterdayString, sameYearFormatter, otherYearFormatter) {
        if (showTimeline) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val dayCache = HashMap<LocalDate, String>()
            images.groupBy { image ->
                val date = Instant.ofEpochMilli(image.dateTaken).atZone(zone).toLocalDate()
                dayCache.getOrPut(date) {
                    formatTimelineDate(
                        date = date,
                        today = today,
                        sameYearFormatter = sameYearFormatter,
                        otherYearFormatter = otherYearFormatter,
                        useRelativeDates = useRelativeDates,
                        todayString = todayString,
                        yesterdayString = yesterdayString,
                        smartYearHiding = smartYearHiding,
                    )
                }
            }.entries.toList()
        } else listOf("" to images).map { object : Map.Entry<String, List<MediaImage>> {
            override val key = it.first
            override val value = it.second
        } }
    }
    val timelineItems = remember(groups, showTimeline) {
        buildList<MediaImage?> {
            groups.forEach { group ->
                if (showTimeline) add(null)
                addAll(group.value)
            }
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actualColumns = remember(screenWidthDp, cellSize, gridSpacing, density, startPadding, endPadding) {
        with(density) {
            val availableWidthPx = (screenWidthDp.dp - startPadding - endPadding - (gridSpacing.dp * 2).dp).roundToPx()
            val minSizePx = cellSize.roundToPx()
            val spacingPx = (gridSpacing.dp).dp.roundToPx()
            maxOf(1, (availableWidthPx + spacingPx) / (minSizePx + spacingPx))
        }
    }

    val spacingPx = remember(gridSpacing, density) { with(density) { (gridSpacing.dp).dp.roundToPx() } }
    val topPaddingPx = remember(gridSpacing, density) { with(density) { (gridSpacing.dp + 3).dp.roundToPx() } }
    val bottomPaddingPx = remember(gridSpacing, bottomPadding, density) { with(density) { ((gridSpacing.dp + 3).dp + bottomPadding).roundToPx() } }

    val estimatedPhotoHeight = remember(actualColumns, screenWidthDp, cellSize, gridSpacing, density, startPadding, endPadding) {
        with(density) {
            val availableWidthPx = (screenWidthDp.dp - startPadding - endPadding - (gridSpacing.dp * 2).dp).roundToPx()
            val cols = actualColumns.coerceAtLeast(1)
            val spacing = (gridSpacing.dp).dp.roundToPx()
            maxOf(1, (availableWidthPx - spacing * (cols - 1)) / cols)
        }
    }
    val estimatedHeaderHeight = remember(density) { with(density) { 50.dp.roundToPx() } }

    val photoHeight = estimatedPhotoHeight
    val headerHeight = estimatedHeaderHeight

    val rowLayout = remember(groups, showTimeline, actualColumns, timelineItems.size) {
        val cols = actualColumns.coerceAtLeast(1)
        val totalCount = timelineItems.size
        val itemToRow = IntArray(totalCount)
        val rowToItem = ArrayList<Int>()
        val isRowHeaderList = ArrayList<Boolean>()
        var itemIdx = 0
        var rowIdx = 0

        if (showTimeline) {
            for (group in groups) {
                if (itemIdx < totalCount) {
                    itemToRow[itemIdx] = rowIdx
                    rowToItem.add(itemIdx)
                    isRowHeaderList.add(true)
                    itemIdx++
                    rowIdx++
                }
                val photoCount = group.value.size
                var photoInGroup = 0
                while (photoInGroup < photoCount && itemIdx < totalCount) {
                    rowToItem.add(itemIdx)
                    isRowHeaderList.add(false)
                    val inThisRow = minOf(cols, photoCount - photoInGroup)
                    for (c in 0 until inThisRow) {
                        if (itemIdx < totalCount) {
                            itemToRow[itemIdx] = rowIdx
                            itemIdx++
                            photoInGroup++
                        }
                    }
                    rowIdx++
                }
            }
        } else {
            while (itemIdx < totalCount) {
                rowToItem.add(itemIdx)
                isRowHeaderList.add(false)
                val inThisRow = minOf(cols, totalCount - itemIdx)
                for (c in 0 until inThisRow) {
                    itemToRow[itemIdx] = rowIdx
                    itemIdx++
                }
                rowIdx++
            }
        }
        val totalRows = maxOf(rowIdx, 1)
        val isRowHeader = BooleanArray(totalRows) { if (it < isRowHeaderList.size) isRowHeaderList[it] else false }
        Triple(totalRows, itemToRow, Pair(rowToItem.toIntArray(), isRowHeader))
    }
    val totalRows = rowLayout.first
    val itemToRow = rowLayout.second
    val rowToItem = rowLayout.third.first
    val isRowHeader = rowLayout.third.second

    val rowOffsets = remember(rowLayout, photoHeight, headerHeight, spacingPx, topPaddingPx) {
        val offsets = IntArray(totalRows)
        var acc = topPaddingPx
        for (r in 0 until totalRows) {
            offsets[r] = acc
            val h = if (isRowHeader[r]) headerHeight else photoHeight
            acc += h + spacingPx
        }
        offsets
    }

    val totalContentHeight = remember(rowLayout, photoHeight, headerHeight, spacingPx, topPaddingPx, bottomPaddingPx) {
        var acc = topPaddingPx
        for (r in 0 until totalRows) {
            val h = if (isRowHeader[r]) headerHeight else photoHeight
            acc += h + spacingPx
        }
        acc += bottomPaddingPx
        maxOf(acc, 1)
    }

    var scrubberDragging by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var scrubTargetIndex by remember(timelineItems) { mutableIntStateOf(0) }
    var suppressReleaseClickId by remember { mutableStateOf<Long?>(null) }
    val scrubberScope = rememberCoroutineScope()

    val scrollFraction by remember(totalRows, rowOffsets, totalContentHeight) {
        derivedStateOf {
            if (scrubberDragging) {
                scrubFraction
            } else {
                val layoutInfo = gridState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (totalRows <= 1 || visibleItems.isEmpty()) {
                    0f
                } else if (!gridState.canScrollForward) {
                    1f
                } else if (!gridState.canScrollBackward && gridState.firstVisibleItemScrollOffset == 0) {
                    0f
                } else {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val maxScrollPx = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
                    val firstItem = visibleItems.first()
                    val row = itemToRow.getOrElse(firstItem.index) { 0 }
                    val rowStartPx = rowOffsets.getOrElse(row) { 0 }
                    val currentScrollPx = rowStartPx - firstItem.offset.y
                    (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)
                }
            }
        }
    }
    val visibleDate by remember(timelineItems) { derivedStateOf {
        if (timelineItems.isEmpty()) return@derivedStateOf null
        val visibleIndex = if (scrubberDragging) scrubTargetIndex else gridState.firstVisibleItemIndex
        val index = visibleIndex.coerceIn(0, timelineItems.lastIndex)
        var found: MediaImage? = null
        for (i in index..timelineItems.lastIndex) {
            val item = timelineItems[i]
            if (item != null) {
                found = item
                break
            }
        }
        if (found == null) {
            for (i in index downTo 0) {
                val item = timelineItems[i]
                if (item != null) {
                    found = item
                    break
                }
            }
        }
        found
    } }
    val currentImages by rememberUpdatedState(images)
    val currentSelection by rememberUpdatedState(selectedIds)
    val currentSetSelection by rememberUpdatedState(onSetSelection)
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    val formatTimelineLabel: (Long) -> String = remember(currentLocale, timelineDateFormat, customTimelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, useRelativeDates, todayString, yesterdayString, sameYearFormatter, otherYearFormatter) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val labelFn: (Long) -> String = { timestamp ->
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatTimelineDate(
                date = date,
                today = today,
                sameYearFormatter = sameYearFormatter,
                otherYearFormatter = otherYearFormatter,
                useRelativeDates = useRelativeDates,
                todayString = todayString,
                yesterdayString = yesterdayString,
                smartYearHiding = smartYearHiding,
            )
        }
        labelFn
    }

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
                                val nextSize = (currentCellSize.value * zoom).coerceIn(36f, 320f)
                                currentOnCellSizeChange?.invoke(nextSize.dp)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val targetThumbnailPx = remember(cellSize) { com.iris.gallery.ui.getThumbnailTargetSizePx(cellSize.value) }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(cellSize),
            modifier = Modifier.fillMaxSize().pointerInput(gridState, onSetSelection) {
                if (currentSetSelection == null) return@pointerInput
                var selecting = true
                var startId: Long? = null
                var startPosition = Offset.Zero
                var initialSelection = emptySet<Long>()
                var previousDragIds = emptySet<Long>()
                var hasDragged = false
                var autoScrollJob: kotlinx.coroutines.Job? = null
                val edge = 72.dp.toPx()
                val dragSlop = 16.dp.toPx()
                fun mediaAt(position: Offset) = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    item.key is Long &&
                        position.x >= item.offset.x && position.x <= item.offset.x + item.size.width &&
                        position.y >= item.offset.y && position.y <= item.offset.y + item.size.height
                }?.key as? Long
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        val id = mediaAt(position) ?: return@detectDragGesturesAfterLongPress
                        startId = id
                        startPosition = position
                        initialSelection = currentSelection
                        selecting = id !in initialSelection
                        suppressReleaseClickId = id
                        hasDragged = false
                        previousDragIds = setOf(id)
                        currentSetSelection?.invoke(id, selecting)
                    },
                    onDrag = { change, _ ->
                        val sId = startId
                        if (sId != null) {
                            val dist = (change.position - startPosition).getDistance()
                            if (!hasDragged && dist < dragSlop) {
                                change.consume()
                                return@detectDragGesturesAfterLongPress
                            }
                            hasDragged = true
                            val currentId = mediaAt(change.position)
                            if (currentId != null) {
                                val allImages = currentImages
                                val startIndex = allImages.indexOfFirst { it.id == sId }
                                val currIndex = allImages.indexOfFirst { it.id == currentId }
                                if (startIndex >= 0 && currIndex >= 0) {
                                    val minIdx = minOf(startIndex, currIndex)
                                    val maxIdx = maxOf(startIndex, currIndex)
                                    val currentRangeIds = allImages.subList(minIdx, maxIdx + 1).map { it.id }.toSet()
                                    currentRangeIds.forEach { id ->
                                        currentSetSelection?.invoke(id, selecting)
                                    }
                                    previousDragIds.filter { it !in currentRangeIds }.forEach { id ->
                                        currentSetSelection?.invoke(id, id in initialSelection)
                                    }
                                    previousDragIds = currentRangeIds
                                }
                            }
                            val scrollBy = when {
                                change.position.y < edge -> -38f
                                change.position.y > size.height - edge -> 38f
                                else -> 0f
                            }
                            if (scrollBy != 0f && autoScrollJob?.isActive != true) {
                                autoScrollJob = scrubberScope.launch { gridState.scrollBy(scrollBy) }
                            }
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        autoScrollJob?.cancel()
                        startId = null
                        previousDragIds = emptySet()
                        scrubberScope.launch {
                            kotlinx.coroutines.delay(250)
                            suppressReleaseClickId = null
                        }
                    },
                    onDragCancel = {
                        autoScrollJob?.cancel()
                        startId = null
                        previousDragIds = emptySet()
                        suppressReleaseClickId = null
                    },
                )
            },
            contentPadding = PaddingValues(
                start = gridSpacing.dp.dp,
                top = (gridSpacing.dp + 3).dp,
                end = gridSpacing.dp.dp,
                bottom = (gridSpacing.dp + 3).dp + bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
            verticalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
        ) {
            groups.forEach { group ->
              if (showTimeline) {
                item(key = "header:${group.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    val ids = remember(group.value) { group.value.map { it.id } }
                    val wholeDateSelected = ids.isNotEmpty() && ids.all { it in selectedIds }
                    val headerClickModifier = if (onSetDateSelection != null) {
                        Modifier.combinedClickable(
                            enabled = true,
                            indication = if (selectedIds.isNotEmpty()) androidx.compose.foundation.LocalIndication.current else null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = {
                                if (selectedIds.isNotEmpty()) onSetDateSelection.invoke(ids, !wholeDateSelected)
                            },
                            onLongClick = { onSetDateSelection.invoke(ids, !wholeDateSelected) },
                        )
                    } else Modifier
                    Row(
                        Modifier
                            .animateItem(fadeInSpec = null)
                            .fillMaxWidth()
                            .then(headerClickModifier)
                            .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.key, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        AnimatedVisibility(selectedIds.isNotEmpty()) {
                            Text(if (wholeDateSelected) stringResource(R.string.action_deselect_date) else stringResource(R.string.action_select_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
              }
              items(group.value, key = { it.id }, contentType = { "photo" }) { image ->
                val selected = image.id in selectedIds
                Box(Modifier.animateItem(fadeInSpec = null).aspectRatio(1f).clip(RoundedCornerShape(if (selected) 14.dp else cornerStyle.dp.dp))
                    .combinedClickable(onClick = {
                        if (suppressReleaseClickId == image.id) suppressReleaseClickId = null
                        else onOpen(image)
                    },
                        onLongClick = null)) {
                    MediaThumbnail(
                        image = image,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val selectedScale = if (selected) .91f else 1f
                            scaleX = selectedScale; scaleY = selectedScale
                        },
                        targetSizePx = targetThumbnailPx,
                        showVideoDuration = showVideoDuration,
                        showFormatBadge = showFormatBadge,
                    )
                    AnimatedVisibility(selected, enter = fadeIn(tween(120)) + scaleIn(tween(160)),
                        exit = fadeOut(tween(100)) + scaleOut(tween(120)),
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                        Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(27.dp).background(MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(50)))
                    }
                }
              }
            }
        }
        if (timelineItems.size > 15) {
            val isScrollerActive = gridState.isScrollInProgress || scrubberDragging
            val scrollerAlpha by animateFloatAsState(
                targetValue = if (isScrollerActive) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isScrollerActive) 150 else 500,
                    delayMillis = if (isScrollerActive) 0 else 1200
                ),
                label = "scrubberAlpha"
            )

            val thumbColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)

            if (scrollerAlpha > 0f) {
                Canvas(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(bottom = bottomPadding)
                        .width(40.dp)
                        .graphicsLayer { alpha = scrollerAlpha }
                        .pointerInput(timelineItems.size, totalRows, totalContentHeight) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                scrubberDragging = true
                                val inset = 16.dp.toPx()
                                val thumbHeight = 48.dp.toPx()
                                val maxTravel = (size.height - inset * 2 - thumbHeight).coerceAtLeast(1f)

                                fun calculateFraction(y: Float): Float {
                                    val topTarget = y - inset - (thumbHeight / 2f)
                                    return (topTarget / maxTravel).coerceIn(0f, 1f)
                                }

                                val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                                val maxScrollPx = (totalContentHeight - viewportHeight).coerceAtLeast(1f)

                                var lastTargetIndex = -1
                                var lastOffset = -1
                                var scrubScrollJob: kotlinx.coroutines.Job? = null
                                fun updateTarget(y: Float) {
                                    val frac = calculateFraction(y)
                                    scrubFraction = frac
                                    val targetScrollPx = (frac * maxScrollPx).toInt()
                                    var targetRow = rowOffsets.binarySearch(targetScrollPx)
                                    if (targetRow < 0) {
                                        targetRow = (-targetRow - 2).coerceIn(0, totalRows - 1)
                                    }
                                    val rowStart = rowOffsets[targetRow]
                                    val remainder = (targetScrollPx - rowStart).coerceAtLeast(0)
                                    val targetIndex = rowToItem[targetRow]
                                    val offset = remainder
                                    scrubTargetIndex = targetIndex
                                    if (targetIndex != lastTargetIndex || kotlin.math.abs(offset - lastOffset) > 8) {
                                        lastTargetIndex = targetIndex
                                        lastOffset = offset
                                        scrubScrollJob?.cancel()
                                        scrubScrollJob = scrubberScope.launch {
                                            gridState.scrollToItem(targetIndex, offset)
                                        }
                                    }
                                }

                                updateTarget(down.position.y)

                                try {
                                    var change = down
                                    do {
                                        updateTarget(change.position.y)
                                        change.consume()
                                        change = awaitPointerEvent().changes.first()
                                    } while (change.pressed)
                                } finally {
                                    scrubberDragging = false
                                    scrubScrollJob?.cancel()
                                    val targetScrollPx = (scrubFraction * maxScrollPx).toInt()
                                    var targetRow = rowOffsets.binarySearch(targetScrollPx)
                                    if (targetRow < 0) {
                                        targetRow = (-targetRow - 2).coerceIn(0, totalRows - 1)
                                    }
                                    val rowStart = rowOffsets[targetRow]
                                    val remainder = (targetScrollPx - rowStart).coerceAtLeast(0)
                                    val targetIndex = rowToItem[targetRow]
                                    val offset = remainder
                                    scrubberScope.launch {
                                        gridState.scrollToItem(targetIndex, offset)
                                    }
                                }
                            }
                        }
                ) {
                    val inset = 16.dp.toPx()
                    val thumbWidth = 5.dp.toPx()
                    val thumbHeight = 48.dp.toPx()
                    val maxTravel = (size.height - inset * 2 - thumbHeight).coerceAtLeast(0f)
                    val thumbTop = inset + scrollFraction.coerceIn(0f, 1f) * maxTravel
                    val thumbLeft = size.width - thumbWidth - 4.dp.toPx()

                    if (scrubberDragging) {
                        val trackWidth = 3.dp.toPx()
                        val trackLeft = size.width - trackWidth - 5.dp.toPx()
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(trackLeft, inset),
                            size = Size(trackWidth, size.height - inset * 2),
                            cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                        )
                    }

                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(thumbLeft, thumbTop),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                    )
                }
            }

            AnimatedVisibility(
                visible = scrubberDragging,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 42.dp),
                enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = .88f),
                exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = .9f)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = visibleDate?.let { formatTimelineLabel(it.dateTaken) } ?: "",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoViewer(
    images: List<MediaImage>,
    initialPage: Int,
    favorites: Set<Long>,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    videoDoubleTapToZoom: Boolean = false,
    showViewerUserComments: Boolean = true,
    viewerHeaderStyle: ViewerHeaderStyle = ViewerHeaderStyle.DATE,
    showViewerPageCount: Boolean = true,
    showViewerTime: Boolean = true,
    showFilmstrip: Boolean = true,
    dismissedFilmstripTip: Boolean = false,
    onDismissFilmstripTip: () -> Unit = {},
    pinchToRotate: Boolean = true,
    dismissedRotateTip: Boolean = false,
    onDismissRotateTip: () -> Unit = {},
    doubleTapZoomLevel: Float = 2.5f,
    timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    smartYearHiding: Boolean = true,
    isLocked: Boolean = false,
    isInTrash: Boolean = false,
    confirmDeleteSetting: Boolean = false,
    preferredEditor: PreferredEditor = PreferredEditor.ALWAYS_ASK,
    onSetPreferredEditor: (PreferredEditor) -> Unit = {},
    availableAlbums: List<MediaAlbum> = emptyList(),
    onToggleFavorite: (Long) -> Unit,
    onClose: () -> Unit,
    onPageChanged: (Long) -> Unit = {},
    onRename: (MediaImage, String) -> Unit = { _, _ -> },
    onDelete: (MediaImage, Boolean) -> Unit,
    onRestore: (MediaImage) -> Unit = {},
    onEditMetadata: (MediaImage, ExifEditRequest) -> Unit,
    onEdit: (MediaImage) -> Unit,
    onLock: (MediaImage) -> Unit,
    onUnlock: (MediaImage) -> Unit = {},
    onMoveToAlbum: (List<MediaImage>, File, String) -> Unit = { _, _, _ -> },
    onCopyToAlbum: (List<MediaImage>, File, String) -> Unit = { _, _, _ -> },
    getAlbumDir: (MediaAlbum) -> File = { File("") },
    createAlbumDir: (String) -> File = { File("") },
) {
    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    DisposableEffect(insetsController, activity) {
        insetsController?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val handleClose = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            show(WindowInsetsCompat.Type.systemBars())
        }
        onClose()
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    var showInfo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showEditChoiceSheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var previewPopupImage by remember { mutableStateOf<MediaImage?>(null) }
    var showRotateTipBanner by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible, insetsController) {
        insetsController?.let { controller ->
            if (controlsVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var zoomedImageId by remember { mutableStateOf<Long?>(null) }
    var viewerMenuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var viewerAlbumAction by remember { mutableStateOf<AlbumAction?>(null) }
    val current = images[pagerState.currentPage]
    val currentExif by produceState<ExifMetadata?>(initialValue = null, current.id, current.uri, current.dateTaken, current.description, current.title) {
        value = withContext(Dispatchers.IO) {
            loadExifMetadata(context, current.uri)
        }
    }
    val viewerComment = remember(current.id, currentExif) {
        currentExif?.userComment?.ifBlank { null }
            ?: currentExif?.xpComment?.ifBlank { null }
            ?: currentExif?.jpegComments?.firstOrNull()?.ifBlank { null }
    }

    fun handleEditClick(image: MediaImage) {
        when (preferredEditor) {
            PreferredEditor.BUILT_IN -> onEdit(image)
            PreferredEditor.EXTERNAL -> launchExternalEditor(context, image)
            PreferredEditor.ALWAYS_ASK -> showEditChoiceSheet = true
        }
    }
    val videoEngine = remember { Media3VideoEngine(context) }
    DisposableEffect(videoEngine) { onDispose { videoEngine.release() } }
    LaunchedEffect(current.id) {
        if (current.isVideo) videoEngine.load(current.uri) else videoEngine.player.pause()
    }

    val dismissOffsetY = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var viewerHeightPx by remember { mutableFloatStateOf(context.resources.displayMetrics.heightPixels.toFloat()) }
    val dismissThresholdPx = remember(context) { 120f * context.resources.displayMetrics.density }
    val flingThresholdPx = remember(context) { 30f * context.resources.displayMetrics.density }

    fun triggerAnimatedDismiss(velocity: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        videoEngine.player.pause()
        coroutineScope.launch {
            dismissOffsetY.animateTo(
                targetValue = viewerHeightPx,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            handleClose()
        }
    }

    BackHandler(enabled = !isDismissing) { triggerAnimatedDismiss() }

    // Preload adjacent images into memory for instant zero-delay swiping
    val imageLoader = remember(context) { coil3.SingletonImageLoader.get(context) }
    LaunchedEffect(pagerState.currentPage, images) {
        val currentIdx = pagerState.currentPage
        if (currentIdx in images.indices) {
            onPageChanged(images[currentIdx].id)
        }
        listOf(currentIdx - 1, currentIdx + 1, currentIdx + 2).forEach { idx ->
            if (idx in images.indices) {
                val media = images[idx]
                if (!media.isVideo) {
                    val req = ImageRequest.Builder(context)
                        .data(media.uri)
                        .size(coil3.size.Size.ORIGINAL)
                        .precision(Precision.EXACT)
                        .build()
                    imageLoader.enqueue(req)
                }
            }
        }
    }

    val dismissProgress = (dismissOffsetY.value / 350f).coerceIn(0f, 1f)
    val contentScale = 1f - (dismissProgress * 0.12f)
    val bgAlpha = (1f - dismissProgress).coerceIn(0f, 1f)
    val controlsDismissAlpha = (1f - (dismissOffsetY.value / 100f)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { if (it.height > 0) viewerHeightPx = it.height.toFloat() },
        color = Color(0xFF080808).copy(alpha = bgAlpha)
    ) {
      Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissOffsetY.value
                    scaleX = contentScale
                    scaleY = contentScale
                }
        ) {
          HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = zoomedImageId != current.id && !isDismissing,
            pageSpacing = 16.dp,
          ) { page ->
            val media = images[page]
            if (media.isVideo) {
                VideoPage(
                    media = media,
                    engine = videoEngine,
                    active = page == pagerState.currentPage,
                    controlsVisible = controlsVisible,
                    autoPlay = autoPlay,
                    loop = loop,
                    doubleTapToZoom = videoDoubleTapToZoom,
                    onTap = { controlsVisible = !controlsVisible },
                    onSwipeUp = { showInfo = true },
                    onSwipeDown = { triggerAnimatedDismiss() },
                    onDismissDrag = { dy ->
                        if (!isDismissing) {
                            coroutineScope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dy).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDismissRelease = { velocityY ->
                        if (!isDismissing) {
                            val shouldDismiss = dismissOffsetY.value > dismissThresholdPx ||
                                (dismissOffsetY.value > flingThresholdPx && velocityY > 800f)
                            if (shouldDismiss) {
                                triggerAnimatedDismiss(velocityY)
                            } else {
                                coroutineScope.launch {
                                    dismissOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    },
                    onZoomChanged = { zoomed -> zoomedImageId = if (zoomed) media.id else null },
                )
            } else {
                ZoomablePhoto(
                    image = media,
                    doubleTapZoomLevel = doubleTapZoomLevel,
                    pinchToRotate = pinchToRotate,
                    onTap = { controlsVisible = !controlsVisible },
                    onSwipeUp = { showInfo = true },
                    onSwipeDown = { triggerAnimatedDismiss() },
                    onDismissDrag = { dy ->
                        if (!isDismissing) {
                            coroutineScope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dy).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDismissRelease = { velocityY ->
                        if (!isDismissing) {
                            val shouldDismiss = dismissOffsetY.value > dismissThresholdPx ||
                                (dismissOffsetY.value > flingThresholdPx && velocityY > 800f)
                            if (shouldDismiss) {
                                triggerAnimatedDismiss(velocityY)
                            } else {
                                coroutineScope.launch {
                                    dismissOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    },
                    onRotateGestureTriggered = {
                        if (!dismissedRotateTip) showRotateTipBanner = true
                    },
                    onZoomChanged = { zoomed ->
                        zoomedImageId = if (zoomed) media.id else null
                    },
                )
            }
          }
        }
        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier
              .align(Alignment.TopCenter)
              .graphicsLayer { alpha = controlsDismissAlpha },
          enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 5 },
          exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 5 },
        ) {
          Box(Modifier.fillMaxWidth().height(156.dp)) {
          Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .82f), Color.Transparent))),
          )
          Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { triggerAnimatedDismiss() }) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
            val currentLocale = rememberAppLocale()
            val headerDate = remember(current.dateTaken, currentLocale, timelineDateFormat, smartYearHiding, customTimelineDateFormat, showViewerTime) {
                val localDate = Instant.ofEpochMilli(current.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
                val isSameYear = localDate.year == LocalDate.now().year
                val formatter = getTimelineFormatter(
                    format = timelineDateFormat,
                    isSameYear = isSameYear,
                    showDayOfWeek = false,
                    locale = currentLocale,
                    customPattern = customTimelineDateFormat,
                    smartYearHiding = smartYearHiding,
                )
                val dateStr = localDate.format(formatter)
                if (showViewerTime) {
                    val tf = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, currentLocale)
                    val timeStr = tf.format(java.util.Date(current.dateTaken))
                    "$dateStr · $timeStr"
                } else {
                    dateStr
                }
            }
            val pageCountText = stringResource(R.string.viewer_page_count, pagerState.currentPage + 1, images.size)
            val pageCountPrefix = if (showViewerPageCount) "$pageCountText · " else ""
            val customTitle = current.title.takeIf { it.isNotBlank() && it != current.name && it != current.name.substringBeforeLast('.') }
            val titleOrName = customTitle ?: current.name

            val (primaryHeaderText, secondaryHeaderText) = when (viewerHeaderStyle) {
                ViewerHeaderStyle.DATE -> {
                    headerDate to if (titleOrName.isNotBlank()) "$pageCountPrefix$titleOrName" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.TITLE_OR_FILENAME -> {
                    titleOrName to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.FILENAME -> {
                    current.name to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                }
                ViewerHeaderStyle.ADAPTIVE -> {
                    if (customTitle != null) {
                        customTitle to if (headerDate.isNotBlank()) "$pageCountPrefix$headerDate" else if (showViewerPageCount) pageCountText else ""
                    } else {
                        headerDate to if (current.name.isNotBlank()) "$pageCountPrefix${current.name}" else if (showViewerPageCount) pageCountText else ""
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (primaryHeaderText.isNotBlank()) {
                    Text(primaryHeaderText, color = Color.White, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (secondaryHeaderText.isNotBlank()) {
                    Text(secondaryHeaderText, color = Color.White.copy(alpha = .75f),
                        style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = { showInfo = true }) { Icon(Icons.Outlined.Info, stringResource(R.string.details_title), tint = Color.White) }
            if (!isLocked && !isInTrash && current.id > 0) {
                Box {
                    IconButton(onClick = { viewerMenuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more), tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = viewerMenuExpanded,
                        onDismissRequest = { viewerMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_move_to_album)) },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                viewerAlbumAction = AlbumAction.MOVE
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_to_album)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                viewerAlbumAction = AlbumAction.COPY
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                viewerMenuExpanded = false
                                showRenameDialog = true
                            }
                        )
                        if (!current.isVideo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_set_as_wallpaper)) },
                                leadingIcon = { Icon(Icons.Outlined.Wallpaper, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    setAsWallpaper(context, current)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_builtin)) },
                                leadingIcon = { Icon(Icons.Outlined.AutoFixHigh, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    onEdit(current)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_external)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    launchExternalEditor(context, current)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit_external)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                                onClick = {
                                    viewerMenuExpanded = false
                                    launchExternalEditor(context, current)
                                }
                            )
                        }
                    }
                }
            }
          }
          }
        }
        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
              .graphicsLayer { alpha = controlsDismissAlpha },
          enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                  slideInVertically(
                      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                      initialOffsetY = { it }
                  ) +
                  scaleIn(
                      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                      initialScale = 0.90f
                  ),
          exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                 slideOutVertically(
                     animationSpec = tween(180, easing = FastOutSlowInEasing),
                     targetOffsetY = { it / 2 }
                  ) +
                 scaleOut(
                     animationSpec = tween(160, easing = FastOutSlowInEasing),
                     targetScale = 0.92f
                  ),
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth(),
        ) {
            // Floating User Comment Overlay positioned directly above filmstrip/bar
            if (showViewerUserComments && !viewerComment.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .clickable { showInfo = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Comment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = viewerComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Dismissible Pinch-to-Rotate Tip Banner
            if (showRotateTipBanner && !dismissedRotateTip) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(R.string.viewer_rotate_tip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        IconButton(
                            onClick = {
                                showRotateTipBanner = false
                                onDismissRotateTip()
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // Mini Filmstrip Thumbnail Carousel for Fast Visual Scrubbing (#40)
            if (showFilmstrip && images.size > 1) {
                if (!dismissedFilmstripTip) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = stringResource(R.string.viewer_filmstrip_tip),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            IconButton(
                                onClick = onDismissFilmstripTip,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                val filmstripListState = rememberLazyListState()
                LaunchedEffect(pagerState.currentPage) {
                    filmstripListState.animateScrollToItem(
                        (pagerState.currentPage - 2).coerceAtLeast(0)
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.height(48.dp),
                ) {
                    LazyRow(
                        state = filmstripListState,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        itemsIndexed(images, key = { _, it -> it.id }) { index, item ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    )
                                    .pointerInput(item.id) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var popupShown = false
                                            val holdJob = coroutineScope.launch {
                                                delay(160)
                                                popupShown = true
                                                previewPopupImage = item
                                            }
                                            val up = waitForUpOrCancellation()
                                            holdJob.cancel()
                                            if (popupShown) {
                                                previewPopupImage = null
                                                up?.consume()
                                            } else if (up != null) {
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(index)
                                                }
                                            }
                                        }
                                    },
                            ) {
                                MediaThumbnail(
                                    image = item,
                                    targetSizePx = 180,
                                    showVideoDuration = false,
                                    showFormatBadge = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 8.dp,
            ) {
              val isFav = current.id in favorites
              val favScale by animateFloatAsState(
                  targetValue = if (isFav) 1.25f else 1.0f,
                  animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                  label = "fav_scale"
              )
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (isInTrash) {
                    ViewerIconButton(
                        icon = Icons.Outlined.RestoreFromTrash,
                        label = stringResource(R.string.action_restore),
                        modifier = Modifier.weight(1f),
                    ) {
                        onRestore(current)
                        handleClose()
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.DeleteForever,
                        label = stringResource(R.string.action_delete_permanently),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    ) {
                        confirmDelete = true
                    }
                } else {
                    ViewerIconButton(
                        icon = Icons.Outlined.Share,
                        label = stringResource(R.string.action_share),
                        modifier = Modifier.weight(1f),
                    ) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (current.isVideo) "video/*" else "image/*"
                            val shareUri = getShareUri(context, current)
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share_media)))
                    }
                    if (current.id > 0) {
                        ViewerIconButton(
                            icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = if (isFav) stringResource(R.string.action_favorited) else stringResource(R.string.action_favorite),
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            scaleEffect = favScale,
                            modifier = Modifier.weight(1f),
                        ) { onToggleFavorite(current.id) }
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.Edit,
                        label = stringResource(R.string.action_edit),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (current.isVideo) {
                            launchExternalEditor(context, current)
                        } else {
                            handleEditClick(current)
                        }
                    }
                    if (current.id > 0) {
                        if (isLocked) {
                            ViewerIconButton(
                                icon = Icons.Outlined.LockOpen,
                                label = stringResource(R.string.action_unlock),
                                modifier = Modifier.weight(1f),
                            ) { onUnlock(current) }
                        } else {
                            ViewerIconButton(
                                icon = Icons.Outlined.Lock,
                                label = stringResource(R.string.action_lock),
                                modifier = Modifier.weight(1f),
                            ) { onLock(current) }
                        }
                    }
                    ViewerIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        label = stringResource(R.string.action_delete),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isLocked || confirmDeleteSetting) {
                            confirmDelete = true
                        } else {
                            onDelete(current, false)
                        }
                    }
                }
              }
            }
        }
        }

        // Floating popup preview card when holding an item in the filmstrip
        AnimatedVisibility(
            visible = previewPopupImage != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.88f),
        ) {
            previewPopupImage?.let { popupItem ->
                Surface(
                    modifier = Modifier
                        .sizeIn(minWidth = 220.dp, maxWidth = 320.dp, minHeight = 220.dp, maxHeight = 340.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    shadowElevation = 24.dp,
                    tonalElevation = 8.dp,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (!popupItem.isVideo) {
                            val popupImageReq = remember(popupItem.id, popupItem.uri) {
                                coil3.request.ImageRequest.Builder(context)
                                    .data(popupItem.uri)
                                    .size(coil3.size.Size.ORIGINAL)
                                    .crossfade(120)
                                    .build()
                            }
                            val popupPainter = rememberAsyncImagePainter(model = popupImageReq)
                            val popupPainterState by popupPainter.state.collectAsState()

                            Image(
                                painter = popupPainter,
                                contentDescription = popupItem.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (popupPainterState !is AsyncImagePainter.State.Success) {
                                MediaThumbnail(
                                    image = popupItem,
                                    targetSizePx = 512,
                                    showVideoDuration = false,
                                    showFormatBadge = false,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            MediaThumbnail(
                                image = popupItem,
                                targetSizePx = 1080,
                                showVideoDuration = false,
                                showFormatBadge = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = popupItem.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val currentLocale = rememberAppLocale()
                                val popupDate = remember(popupItem.dateTaken, currentLocale) {
                                    DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale).format(Date(popupItem.dateTaken))
                                }
                                val subtitle = if (popupItem.isVideo && popupItem.durationMs > 0) {
                                    val totalSecs = popupItem.durationMs / 1000
                                    val durStr = if (totalSecs >= 3600) {
                                        "%d:%02d:%02d".format(totalSecs / 3600, (totalSecs % 3600) / 60, totalSecs % 60)
                                    } else {
                                        "%d:%02d".format(totalSecs / 60, totalSecs % 60)
                                    }
                                    "$durStr · $popupDate"
                                } else {
                                    popupDate
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        }
                        if (popupItem.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .padding(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
      }
    }

    val canNavigatePrev = pagerState.currentPage > 0
    val canNavigateNext = pagerState.currentPage < images.size - 1
    if (showInfo) PhotoDetailsSheet(
        image = current,
        timelineDateFormat = timelineDateFormat,
        customTimelineDateFormat = customTimelineDateFormat,
        canNavigatePrevious = canNavigatePrev,
        canNavigateNext = canNavigateNext,
        onNavigatePrevious = {
            if (canNavigatePrev) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        },
        onNavigateNext = {
            if (canNavigateNext) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        },
        onDismiss = { showInfo = false },
        onSave = { request ->
            onEditMetadata(current, request)
        },
        onRename = onRename,
    )
    if (showRenameDialog) {
        RenameFileDialog(
            currentName = current.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(current, newName)
                showRenameDialog = false
            }
        )
    }
    if (confirmDelete) {
        val isPermanentlyDeleting = isLocked || isInTrash
        var deletePermanently by remember { mutableStateOf(isPermanentlyDeleting) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    if (isPermanentlyDeleting) {
                        if (current.isVideo) stringResource(R.string.delete_permanent_video_title)
                        else stringResource(R.string.delete_permanent_photo_title)
                    } else {
                        if (current.isVideo) stringResource(R.string.delete_trash_video_title)
                        else stringResource(R.string.delete_trash_photo_title)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isLocked) stringResource(R.string.delete_vault_desc)
                        else if (isPermanentlyDeleting) stringResource(R.string.delete_permanent_desc)
                        else stringResource(R.string.delete_trash_desc)
                    )
                    if (!isPermanentlyDeleting) {
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
                    confirmDelete = false
                    onDelete(current, isPermanentlyDeleting || deletePermanently)
                }) {
                    Text(
                        if (isPermanentlyDeleting || deletePermanently) stringResource(R.string.action_delete_permanently)
                        else stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (viewerAlbumAction != null) {
        val action = viewerAlbumAction!!
        AlbumPickerSheet(
            action = action,
            selectedCount = 1,
            albums = availableAlbums,
            currentAlbumId = if (action == AlbumAction.MOVE) current.bucketId else null,
            onDismiss = { viewerAlbumAction = null },
            onSelectAlbum = { album ->
                val targetDir = getAlbumDir(album)
                viewerAlbumAction = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(listOf(current), targetDir, album.name)
                    handleClose()
                } else {
                    onCopyToAlbum(listOf(current), targetDir, album.name)
                }
            },
            onCreateAlbum = { newName ->
                val targetDir = createAlbumDir(newName)
                viewerAlbumAction = null
                if (action == AlbumAction.MOVE) {
                    onMoveToAlbum(listOf(current), targetDir, newName)
                    handleClose()
                } else {
                    onCopyToAlbum(listOf(current), targetDir, newName)
                }
            }
        )
    }

    if (showEditChoiceSheet) {
        EditChoiceBottomSheet(
            onDismiss = { showEditChoiceSheet = false },
            onChooseBuiltIn = { rememberChoice ->
                showEditChoiceSheet = false
                if (rememberChoice) onSetPreferredEditor(PreferredEditor.BUILT_IN)
                onEdit(current)
            },
            onChooseExternal = { rememberChoice ->
                showEditChoiceSheet = false
                if (rememberChoice) onSetPreferredEditor(PreferredEditor.EXTERNAL)
                launchExternalEditor(context, current)
            }
        )
    }
}

@Composable
private fun ZoomablePhoto(
    image: MediaImage,
    doubleTapZoomLevel: Float = 2.5f,
    pinchToRotate: Boolean = true,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onDismissDrag: (Float) -> Unit = {},
    onDismissRelease: (Float) -> Unit = {},
    onRotateGestureTriggered: () -> Unit = {},
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scaleAnim = remember(image.id) { Animatable(1f) }
    val offsetAnim = remember(image.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val rotationAnim = remember(image.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var containerSize by remember(image.id) { mutableStateOf(IntSize.Zero) }

    // Instant zero-delay preview from memory cache while full image decodes
    var cachedThumb by remember(image.id) { mutableStateOf(com.iris.gallery.ui.ThumbnailCache.findForImage(image.id)) }
    if (cachedThumb == null) {
        LaunchedEffect(image.id, image.uri) {
            val thumb = withContext(Dispatchers.IO) { com.iris.gallery.ui.loadThumbnailSync(context, image, 512) }
            if (thumb != null) cachedThumb = thumb
        }
    }
    val thumbPainter = remember(cachedThumb) {
        cachedThumb?.let { BitmapPainter(it.asImageBitmap()) }
    }

    val imageRequest: ImageRequest = remember(image.id, image.uri) {
        ImageRequest.Builder(context)
            .data(image.uri)
            .size(coil3.size.Size.ORIGINAL)
            .precision(Precision.EXACT)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val painterState by painter.state.collectAsState()
    val fullImageLoaded = painterState is AsyncImagePainter.State.Success

    fun clampOffset(candidate: Offset, atScale: Float): Offset {
        if (atScale <= 1f || containerSize == IntSize.Zero || candidate.x.isNaN() || candidate.y.isNaN()) return Offset.Zero
        val intrinsic = painter.intrinsicSize
        val hasIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
        val thumbIntrinsic = thumbPainter?.intrinsicSize
        val hasThumbIntrinsic = thumbIntrinsic != null && thumbIntrinsic.isSpecified && thumbIntrinsic.width > 0f && thumbIntrinsic.height > 0f
        val imgWidth = when {
            hasIntrinsic -> intrinsic.width
            hasThumbIntrinsic -> thumbIntrinsic!!.width
            image.width > 0 -> image.width.toFloat()
            else -> 1080f
        }
        val imgHeight = when {
            hasIntrinsic -> intrinsic.height
            hasThumbIntrinsic -> thumbIntrinsic!!.height
            image.height > 0 -> image.height.toFloat()
            else -> 1080f
        }
        val imageAspect = imgWidth / imgHeight
        val containerAspect = containerSize.width.toFloat() / containerSize.height.coerceAtLeast(1)
        val displayedWidth: Float
        val displayedHeight: Float
        if (imageAspect > containerAspect) {
            displayedWidth = containerSize.width.toFloat()
            displayedHeight = displayedWidth / imageAspect
        } else {
            displayedHeight = containerSize.height.toFloat()
            displayedWidth = displayedHeight * imageAspect
        }
        val maxX = (displayedWidth * atScale - containerSize.width).coerceAtLeast(0f) / 2f
        val maxY = (displayedHeight * atScale - containerSize.height).coerceAtLeast(0f) / 2f
        val clampedX = candidate.x.coerceIn(-maxX, maxX)
        val clampedY = candidate.y.coerceIn(-maxY, maxY)
        if (clampedX.isNaN() || clampedY.isNaN()) return Offset.Zero
        return Offset(clampedX, clampedY)
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(image.id, doubleTapZoomLevel) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tapPos ->
                            coroutineScope.launch {
                                val currentScale = scaleAnim.value
                                val currentOffset = offsetAnim.value
                                if (currentScale > 1.05f || kotlin.math.abs(rotationAnim.value) > 1f) {
                                    launch { scaleAnim.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                    launch { offsetAnim.animateTo(Offset.Zero, tween(260, easing = FastOutSlowInEasing)) }
                                    launch { rotationAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                                    onZoomChanged(false)
                                } else {
                                    val targetScale = doubleTapZoomLevel
                                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                    val z = targetScale / currentScale
                                    val targetOffset = clampOffset(currentOffset + (tapPos - center - currentOffset) * (1f - z), targetScale)
                                    launch { scaleAnim.animateTo(targetScale, tween(260, easing = FastOutSlowInEasing)) }
                                    launch { offsetAnim.animateTo(targetOffset, tween(260, easing = FastOutSlowInEasing)) }
                                    onZoomChanged(true)
                                }
                            }
                        },
                    )
                }
                .pointerInput(image.id, pinchToRotate) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        var totalDragX = 0f
                        var totalRotation = rotationAnim.value
                        var accumulatedAngleDelta = 0f
                        var isRotating = false
                        var isSwipeUpDetected = false
                        var isDismissDragging = false
                        var lastDragTime = SystemClock.uptimeMillis()
                        var lastDragY = 0f
                        var releaseVelocityY = 0f
                        do {
                            val event = awaitPointerEvent()
                            val pointersDown = event.changes.count { it.pressed }
                            val scale = scaleAnim.value
                            val offset = offsetAnim.value
                            if (pointersDown >= 2 || (pointersDown == 1 && scale > 1f)) {
                                if (isDismissDragging) {
                                    isDismissDragging = false
                                    onDismissRelease(0f)
                                }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val rotationChange = if (pointersDown >= 2 && pinchToRotate) event.calculateRotation() else 0f
                                val isTransforming = pointersDown >= 2 || panChange.getDistance() > 0.5f

                                val validZoom = if (!zoomChange.isNaN() && zoomChange > 0f) zoomChange else 1f
                                val validPan = if (panChange.isSpecified && !panChange.x.isNaN() && !panChange.y.isNaN()) panChange else Offset.Zero

                                val calculatedScale = (scale * validZoom).coerceIn(1f, 7f)
                                val nextScale = if (calculatedScale < 1.02f) 1f else calculatedScale
                                val nextOffset = if (pointersDown >= 2 && containerSize != IntSize.Zero) {
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid.isSpecified && !centroid.x.isNaN() && !centroid.y.isNaN()) {
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val effectiveZoom = nextScale / scale
                                        val focalOffset = (offset + validPan) + (centroid - center - offset) * (1f - effectiveZoom)
                                        clampOffset(focalOffset, nextScale)
                                    } else {
                                        clampOffset(offset + validPan, nextScale)
                                    }
                                } else {
                                    clampOffset(offset + validPan, nextScale)
                                }

                                if (pinchToRotate && pointersDown >= 2 && !rotationChange.isNaN()) {
                                    accumulatedAngleDelta += rotationChange
                                    if (!isRotating && kotlin.math.abs(accumulatedAngleDelta) >= 15f) {
                                        isRotating = true
                                        onRotateGestureTriggered()
                                    }
                                    if (isRotating) {
                                        totalRotation += rotationChange
                                        coroutineScope.launch {
                                            rotationAnim.snapTo(totalRotation)
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    scaleAnim.snapTo(nextScale)
                                    offsetAnim.snapTo(nextOffset)
                                }
                                onZoomChanged(nextScale > 1.01f || (isRotating && kotlin.math.abs(rotationAnim.value % 360f) > 5f))
                                if (isTransforming) event.changes.forEach { it.consume() }
                            } else if (pointersDown == 1 && scale <= 1.02f && !isRotating) {
                                val panChange = event.calculatePan()
                                totalDragY += panChange.y
                                totalDragX += panChange.x
                                val now = SystemClock.uptimeMillis()
                                val dt = (now - lastDragTime).coerceAtLeast(1)
                                releaseVelocityY = (totalDragY - lastDragY) / (dt / 1000f)
                                lastDragTime = now
                                lastDragY = totalDragY

                                if (!isSwipeUpDetected && !isDismissDragging && totalDragY < -75f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 1.5f) {
                                    isSwipeUpDetected = true
                                    event.changes.forEach { it.consume() }
                                    onSwipeUp()
                                } else if (!isSwipeUpDetected) {
                                    if (!isDismissDragging && totalDragY > 15f && totalDragY > kotlin.math.abs(totalDragX) * 1.3f) {
                                        isDismissDragging = true
                                    }
                                    if (isDismissDragging) {
                                        event.changes.forEach { it.consume() }
                                        onDismissDrag(panChange.y)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (isDismissDragging) {
                            onDismissRelease(releaseVelocityY)
                        }

                        // When gesture ends, snap rotation to nearest 90° angle if rotated
                        if (isRotating) {
                            val normalized = (totalRotation % 360f + 360f) % 360f
                            val targetAngle = when {
                                normalized in 45f..135f -> 90f
                                normalized in 135f..225f -> 180f
                                normalized in 225f..315f -> 270f
                                else -> 0f
                            }
                            val diff = (targetAngle - normalized)
                            val snapTarget = totalRotation + diff
                            coroutineScope.launch {
                                rotationAnim.animateTo(
                                    targetValue = snapTarget,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                )
                                onZoomChanged(scaleAnim.value > 1.01f || kotlin.math.abs(snapTarget % 360f) > 2f)
                            }
                        }
                    }
                },
        ) {
            val scale = scaleAnim.value
            val offset = offsetAnim.value
            val intrinsic = painter.intrinsicSize
            val hasValidIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
            val thumbIntrinsic = thumbPainter?.intrinsicSize
            val hasThumbIntrinsic = thumbIntrinsic != null && thumbIntrinsic.isSpecified && thumbIntrinsic.width > 0f && thumbIntrinsic.height > 0f
            val imgWidth = when {
                hasValidIntrinsic -> intrinsic.width
                hasThumbIntrinsic -> thumbIntrinsic!!.width
                image.width > 0 -> image.width.toFloat()
                else -> 1080f
            }
            val imgHeight = when {
                hasValidIntrinsic -> intrinsic.height
                hasThumbIntrinsic -> thumbIntrinsic!!.height
                image.height > 0 -> image.height.toFloat()
                else -> 1080f
            }

            if (imgWidth > 0f && imgHeight > 0f && size.width > 0f && size.height > 0f) {
                val imageAspect = imgWidth / imgHeight
                val canvasAspect = size.width / size.height
                val fitWidth: Float
                val fitHeight: Float
                if (imageAspect > canvasAspect) {
                    fitWidth = size.width
                    fitHeight = fitWidth / imageAspect
                } else {
                    fitHeight = size.height
                    fitWidth = fitHeight * imageAspect
                }
                val left = (size.width - fitWidth) / 2f
                val top = (size.height - fitHeight) / 2f

                withTransform({
                    translate(offset.x, offset.y)
                    scale(scale, scale, pivot = center)
                    rotate(rotationAnim.value, pivot = center)
                    translate(left, top)
                }) {
                    val drawSize = androidx.compose.ui.geometry.Size(fitWidth, fitHeight)
                    if (fullImageLoaded) {
                        with(painter) {
                            draw(size = drawSize)
                        }
                    } else if (thumbPainter != null) {
                        with(thumbPainter) {
                            draw(size = drawSize)
                        }
                    } else {
                        with(painter) {
                            draw(size = drawSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    scaleEffect: Float = 1f,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .scale(scaleEffect)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PhotoDetailsSheet(
    image: MediaImage,
    timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    canNavigatePrevious: Boolean = false,
    canNavigateNext: Boolean = false,
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (ExifEditRequest) -> Unit,
    onRename: (MediaImage, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var exifRevision by remember { mutableIntStateOf(0) }
    val exif by produceState<ExifMetadata?>(initialValue = null, image.id, image.uri, exifRevision) {
        value = withContext(Dispatchers.IO) {
            loadExifMetadata(context, image.uri)
        }
    }
    val currentExif = exif
    var editing by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        dragOffset += delta
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if ((dragOffset > 80f || velocity > 300f) && canNavigatePrevious) {
                            onNavigatePrevious()
                        } else if ((dragOffset < -80f || velocity < -300f) && canNavigateNext) {
                            onNavigateNext()
                        }
                        dragOffset = 0f
                    }
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val sheetTitle = image.title.takeIf { it.isNotBlank() && it != image.name && it != image.name.substringBeforeLast('.') }
                    ?: image.name.ifBlank { stringResource(R.string.details_photo_details) }
                Text(
                    sheetTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (canNavigatePrevious || canNavigateNext) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigatePrevious,
                            enabled = canNavigatePrevious,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_previous),
                                tint = if (canNavigatePrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = onNavigateNext,
                            enabled = canNavigateNext,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_next),
                                tint = if (canNavigateNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(20.dp).graphicsLayer(scaleX = -1f),
                            )
                        }
                    }
                }
            }

            val currentLocale = rememberAppLocale()
            val resolvedTitle = (currentExif?.title?.takeIf { it.isNotBlank() } ?: image.title).takeIf {
                it.isNotBlank() && it != image.name && it != image.name.substringBeforeLast('.')
            }
            val resolvedDesc = (currentExif?.imageDescription?.takeIf { it.isNotBlank() } ?: image.description.takeIf { it.isNotBlank() })?.takeIf {
                it != resolvedTitle && it != image.name && it != image.name.substringBeforeLast('.')
            }
            val commentText = currentExif?.userComment?.ifBlank { null }
            val hasNotes = commentText != null || resolvedDesc != null || !currentExif?.xpComment.isNullOrBlank() || !currentExif?.jpegComments.isNullOrEmpty() || resolvedTitle != null

            // 1. Description Card
            if (hasNotes) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Comment, stringResource(R.string.details_section_description), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.details_section_description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (resolvedTitle != null) {
                            DetailBlock(stringResource(R.string.details_title_field), resolvedTitle)
                        }
                        if (commentText != null) {
                            DetailBlock(stringResource(R.string.details_exif_user_comment), commentText)
                        }
                        if (!currentExif?.xpComment.isNullOrBlank() && currentExif?.xpComment != commentText) {
                            DetailBlock(stringResource(R.string.details_xp_comment), currentExif!!.xpComment!!)
                        }
                        currentExif?.jpegComments?.filter { it != commentText && it != currentExif?.xpComment }?.let { comments ->
                            comments.forEachIndexed { idx, jc ->
                                val label = if (comments.size > 1) {
                                    stringResource(R.string.details_jpeg_comment_numbered, idx + 1)
                                } else {
                                    stringResource(R.string.details_jpeg_comment)
                                }
                                DetailBlock(label, jc)
                            }
                        }
                        if (resolvedDesc != null) {
                            DetailBlock(stringResource(R.string.details_desc_field), resolvedDesc)
                        }
                    }
                }
            }

            // 2. Origin Card (Captured Date & Time, Location, Artist, Copyright, Software)
            val parsedCapturedDate = remember(currentExif?.dateTimeOriginal, currentExif?.offsetTimeOriginal, image.dateTaken, currentLocale, timelineDateFormat, customTimelineDateFormat) {
                val offset = currentExif?.offsetTimeOriginal?.trim()
                val tz = if (!offset.isNullOrBlank()) {
                    val prefix = if (offset.startsWith("+") || offset.startsWith("-")) "GMT" else "GMT+"
                    java.util.TimeZone.getTimeZone(prefix + offset)
                } else {
                    java.util.TimeZone.getDefault()
                }
                val dateMillis = runCatching {
                    currentExif?.dateTimeOriginal?.let { raw ->
                        val parser = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).apply {
                            timeZone = tz
                        }
                        parser.parse(raw)?.time
                    }
                }.getOrNull() ?: image.dateTaken
                val tf = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale).apply {
                    timeZone = tz
                }
                val timeStr = tf.format(Date(dateMillis))
                val localDate = Instant.ofEpochMilli(dateMillis).atZone(tz.toZoneId()).toLocalDate()
                val formatter = getTimelineFormatter(
                    format = timelineDateFormat,
                    isSameYear = false,
                    showDayOfWeek = false,
                    locale = currentLocale,
                    customPattern = customTimelineDateFormat,
                    smartYearHiding = false,
                )
                val tzSuffix = if (!offset.isNullOrBlank()) " ($offset)" else ""
                "${localDate.format(formatter)} · $timeStr$tzSuffix"
            }
            val hasOrigin = parsedCapturedDate.isNotBlank() ||
                (currentExif?.latitude != null && currentExif.longitude != null) ||
                !currentExif?.artist.isNullOrBlank() ||
                !currentExif?.copyright.isNullOrBlank() ||
                !currentExif?.software.isNullOrBlank()

            if (hasOrigin) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.LocationOn, stringResource(R.string.details_section_origin), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.details_section_origin), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        DetailItem(stringResource(R.string.details_captured), parsedCapturedDate)

                        if (currentExif?.latitude != null && currentExif.longitude != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.details_location), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val locStr = "%.4f, %.4f".format(Locale.US, currentExif.latitude, currentExif.longitude) +
                                        (currentExif.altitude?.let { " (%.0f m)".format(Locale.US, it) } ?: "")
                                    Text(locStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                                TextButton(onClick = {
                                    val uri = Uri.parse("geo:${currentExif.latitude},${currentExif.longitude}?q=${currentExif.latitude},${currentExif.longitude}(Photo+Location)")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    runCatching { context.startActivity(intent) }
                                }) {
                                    Icon(Icons.Outlined.Map, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.details_map))
                                }
                            }
                        }

                        if (!currentExif?.artist.isNullOrBlank()) {
                            DetailItem(stringResource(R.string.details_artist), currentExif.artist!!)
                        }
                        if (!currentExif?.copyright.isNullOrBlank()) {
                            DetailItem(stringResource(R.string.details_copyright), currentExif.copyright!!)
                        }
                        if (!currentExif?.software.isNullOrBlank()) {
                            DetailItem(stringResource(R.string.details_software), currentExif.software!!)
                        }
                    }
                }
            }

            // 3. Camera & Lens Card
            currentExif?.let { data ->
                val hasCamera = data.cameraDisplayName != null || data.aperture != null || data.shutterSpeed != null || data.iso != null || data.focalLength != null
                if (hasCamera) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CameraAlt, stringResource(R.string.details_camera), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(stringResource(R.string.details_camera), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            if (data.cameraDisplayName != null) {
                                DetailItem(stringResource(R.string.details_camera), data.cameraDisplayName!!)
                            }
                            if (!data.lensModel.isNullOrBlank()) {
                                DetailItem(stringResource(R.string.details_lens), data.lensModel)
                            }
                            val specs = listOfNotNull(data.aperture, data.shutterSpeed, data.focalLength, data.iso).joinToString(" · ")
                            if (specs.isNotBlank()) {
                                DetailItem(stringResource(R.string.details_camera_capture), specs)
                            }
                            val extraSpecs = listOfNotNull(data.flash, data.whiteBalance?.let { "${stringResource(R.string.details_white_balance)}: $it" }).joinToString(" · ")
                            if (extraSpecs.isNotBlank()) {
                                Text(extraSpecs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 4. Image Properties Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Image, stringResource(R.string.details_image_properties), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.details_image_properties), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    val mp = if (image.width > 0 && image.height > 0) (image.width * image.height) / 1_000_000.0 else 0.0
                    val resText = if (mp > 0) "${image.width} × ${image.height} (%.1f MP)".format(Locale.US, mp) else "${image.width} × ${image.height}"
                    DetailItem(stringResource(R.string.details_resolution), resText)
                    if (!currentExif?.imageUniqueId.isNullOrBlank()) {
                        DetailBlock(stringResource(R.string.details_image_unique_id), currentExif.imageUniqueId!!)
                    }
                    if (image.orientation != 0) DetailItem(stringResource(R.string.details_orientation), "${image.orientation}°")
                    if (image.isVideo && image.durationMs > 0) DetailItem(stringResource(R.string.details_duration), formatMediaDuration(image.durationMs))
                }
            }

            // 5. File Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Description, stringResource(R.string.details_file_info), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.details_file_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(stringResource(R.string.details_file_name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SelectionContainer(Modifier.weight(1f)) {
                                Text(image.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(
                                onClick = { showRenameDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.action_rename),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    val modifiedMillis = remember(image.path, image.dateTaken) {
                        val f = File(image.path)
                        if (f.exists() && f.lastModified() > 0) f.lastModified() else image.dateTaken
                    }
                    val formattedDate = remember(modifiedMillis, currentLocale, timelineDateFormat, customTimelineDateFormat) {
                        val tf = DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale)
                        val timeStr = tf.format(Date(modifiedMillis))
                        val localDate = Instant.ofEpochMilli(modifiedMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val formatter = getTimelineFormatter(
                            format = timelineDateFormat,
                            isSameYear = false,
                            showDayOfWeek = false,
                            locale = currentLocale,
                            customPattern = customTimelineDateFormat,
                            smartYearHiding = false,
                        )
                        val dateStr = localDate.format(formatter)
                        "$dateStr · $timeStr"
                    }
                    DetailItem(stringResource(R.string.details_modified), formattedDate)
                    DetailItem(stringResource(R.string.details_type), image.mimeType.ifBlank { if (image.isVideo) stringResource(R.string.format_video) else stringResource(R.string.format_image) })
                    DetailItem(stringResource(R.string.details_size), formatFileSize(image.sizeBytes))
                    DetailBlock(stringResource(R.string.details_path), image.path)
                }
            }

            // Edit Metadata Action Button
            if (!image.isVideo) {
                Button(
                    onClick = { editing = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.details_edit_metadata))
                }
            }
        }
    }
    if (editing) {
        ExifEditorSheet(
            image = image,
            exif = currentExif,
            onDismiss = { editing = false },
            onSave = { request ->
                editing = false
                onSave(request)
                exifRevision++
            },
        )
    }
    if (showRenameDialog) {
        RenameFileDialog(
            currentName = image.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(image, newName)
                showRenameDialog = false
            }
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

private fun formatMediaDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val nameWithoutExt = currentName.substringBeforeLast('.')
    val ext = currentName.substringAfterLast('.', "")
    var newName by remember { mutableStateOf(nameWithoutExt) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_file_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.rename_file_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = if (ext.isNotEmpty()) { { Text(".$ext", color = MaterialTheme.colorScheme.outline) } } else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalName = if (ext.isNotEmpty()) "$newName.$ext" else newName
                    onConfirm(finalName)
                    onDismiss()
                },
                enabled = newName.isNotBlank() && newName.trim() != nameWithoutExt
            ) {
                Text(stringResource(R.string.action_rename))
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
private fun EmptyState(message: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
