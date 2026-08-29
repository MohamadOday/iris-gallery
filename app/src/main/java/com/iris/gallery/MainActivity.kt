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
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
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
import coil3.size.Precision
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.drawscope.withTransform
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifMetadata
import com.iris.gallery.data.loadExifMetadata
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
import com.iris.gallery.ui.EditorScreen
import com.iris.gallery.ui.AppLockScreen
import com.iris.gallery.ui.video.VideoPage
import com.iris.gallery.ui.video.Media3VideoEngine
import com.iris.gallery.data.DuplicateGroup
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.data.AccentColor
import com.iris.gallery.ui.SettingsScreen
import com.iris.gallery.ui.AboutScreen
import androidx.compose.material.icons.outlined.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iris.gallery.ui.theme.IrisTheme
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IrisPhotoWidget.refreshAll(this)
        enableEdgeToEdge()
        val pickerMode = intent.action == Intent.ACTION_PICK || intent.action == Intent.ACTION_GET_CONTENT
        val isViewAction = intent.action == Intent.ACTION_VIEW || intent.action == "com.android.camera.action.REVIEW"
        val requestedType = intent.type
        val viewUri = intent.data.takeIf { isViewAction }
        val settingsPreferences = SettingsPreferences(this)
        if (settingsPreferences.state.value.language.isNotEmpty()) {
            com.iris.gallery.ui.setAppLanguage(this, settingsPreferences.state.value.language)
        }
        setContent {
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
                    initialMemories = intent.getBooleanExtra("open_memories", false),
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
}

private fun requiredPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    Build.VERSION.SDK_INT <= 28 -> arrayOf(
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
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) viewModel.refresh()
    }
    var pendingMetadata by remember { mutableStateOf<Pair<MediaImage, ContentValues>?>(null) }
    val metadataWriteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        val pending = pendingMetadata
        pendingMetadata = null
        if (it.resultCode == Activity.RESULT_OK && pending != null) {
            context.contentResolver.update(canonicalMediaUri(pending.first), pending.second, null, null)
            viewModel.refresh()
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
            viewModel.refresh()
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

    LaunchedEffect(permitted) { if (permitted) viewModel.refresh() }
    LaunchedEffect(Unit) {
        MemoriesNotifications.schedule(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!permitted) {
        PermissionScreen { permissionLauncher.launch(permissions) }
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
                val visibleMedia = remember(state.images, requestedType, libraryState.lockedMedia) {
                    val requested = when {
                        requestedType?.startsWith("image/") == true -> state.images.filterNot { it.isVideo }
                        requestedType?.startsWith("video/") == true -> state.images.filter { it.isVideo }
                        else -> state.images
                    }
                    requested.filterNot { it.id in libraryState.lockedMedia }
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
                val onDeleteFromLocked: (List<MediaImage>) -> Unit = { mediaList ->
                    if (mediaList.isNotEmpty()) {
                        val vaultItems = mediaList.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                        val galleryLockedItems = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                        if (vaultItems.isNotEmpty()) {
                            coroutineScope.launch {
                                viewModel.deletePermanentlyFromVault(vaultItems)
                                viewModel.refresh()
                                Toast.makeText(context, "${vaultItems.size} item(s) permanently deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (galleryLockedItems.isNotEmpty()) {
                            if (Build.VERSION.SDK_INT >= 30) runCatching {
                                val request = MediaStore.createDeleteRequest(context.contentResolver,
                                    galleryLockedItems.map { canonicalMediaUri(it) })
                                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                            }.onFailure { Toast.makeText(context, "Could not request deletion", Toast.LENGTH_LONG).show() }
                            else runCatching {
                                galleryLockedItems.forEach { context.contentResolver.delete(canonicalMediaUri(it), null, null) }
                                viewModel.refresh()
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
                        onTrash = { media ->
                            if (media.isNotEmpty()) {
                                coroutineScope.launch {
                                    viewModel.moveToTrash(media)
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
                        onDeletePermanently = { media ->
                            if (media.isNotEmpty()) {
                                coroutineScope.launch {
                                    viewModel.deletePermanently(media)
                                }
                            }
                        },
                        onEditMetadata = { media, name, title, captured, orientation ->
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(MediaStore.MediaColumns.TITLE, title)
                                put(MediaStore.Images.Media.DATE_TAKEN, captured)
                                put(MediaStore.Images.Media.ORIENTATION, orientation)
                            }
                            if (Build.VERSION.SDK_INT >= 30) runCatching {
                                pendingMetadata = media to values
                                val request = MediaStore.createWriteRequest(context.contentResolver, listOf(canonicalMediaUri(media)))
                                metadataWriteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                            }.onFailure { pendingMetadata = null; Toast.makeText(context, context.getString(R.string.toast_could_not_request_metadata), Toast.LENGTH_LONG).show() }
                            else runCatching { context.contentResolver.update(canonicalMediaUri(media), values, null, null); viewModel.refresh() }
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
                        initialMemories = initialMemories,
                        initialViewUri = initialViewUri,
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

private fun canonicalMediaUri(item: MediaImage): Uri {
    return if (item.uri.scheme == "file") {
        item.uri
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
    lockedAuthorized: Boolean,
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
    onRequestUnlock: () -> Unit,
    onPick: ((MediaImage) -> Unit)?,
    onTrash: (List<MediaImage>) -> Unit,
    onRestore: (List<MediaImage>) -> Unit,
    onDeletePermanently: (List<MediaImage>) -> Unit,
    onEditMetadata: (MediaImage, String, String, Long, Int) -> Unit,
    onMoveToAlbum: (List<MediaImage>, java.io.File, String) -> Unit,
    onCopyToAlbum: (List<MediaImage>, java.io.File, String) -> Unit,
    getAlbumDir: (MediaAlbum) -> java.io.File,
    createAlbumDir: (String) -> java.io.File,
    duplicateState: DuplicateScanState,
    onScanDuplicates: () -> Unit,
    onCancelDuplicateScan: () -> Unit,
    initialMemories: Boolean,
    initialViewUri: Uri? = null,
) {
    val context = LocalContext.current
    val tabPagerState = rememberPagerState(
        initialPage = if (initialMemories) 3 else settings.startupTab.pageIndex,
        pageCount = { 4 }
    )
    val tabScope = rememberCoroutineScope()
    val destination = tabPagerState.currentPage
    var selectedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(images, initialViewUri) {
        if (initialViewUri != null && selectedId == null && images.isNotEmpty()) {
            images.firstOrNull { it.uri == initialViewUri || it.uri.lastPathSegment == initialViewUri.lastPathSegment }?.let {
                selectedId = it.id
            }
        }
    }
    var viewerImages by remember { mutableStateOf<List<MediaImage>?>(null) }
    var selectedAlbumId by remember { mutableStateOf<Long?>(null) }
    var librarySection by remember { mutableStateOf<String?>(if (initialMemories) "memories" else null) }
    var editorImage by remember { mutableStateOf<MediaImage?>(null) }
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
                MediaAlbum(
                    id = albumId,
                    name = first.bucketName,
                    cover = albumImages.firstOrNull { it.id == albumCovers[albumId] } ?: first,
                    images = albumImages,
                )
            }
        }
    }
    val activeMedia = when {
        destination == 3 && librarySection == "trash" -> trashed
        destination == 3 && librarySection == "locked" -> lockedMedia
        destination == 1 && selectedAlbum != null -> selectedAlbum!!.images
        destination == 2 -> favoriteImages
        else -> images
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
            MediaAlbum(
                id = id,
                name = media.first().bucketName,
                cover = media.firstOrNull { it.id == albumCovers[id] } ?: media.first(),
                images = media,
            )
        }.sortedBy { it.name.lowercase() }
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

    val handleTrash: (List<MediaImage>) -> Unit = { items ->
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, items.size)
            onTrash(items)
        }
    }
    val handleRestore: (List<MediaImage>) -> Unit = { items ->
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, items.size)
            onRestore(items)
        }
    }
    val handleDeletePermanently: (List<MediaImage>) -> Unit = { items ->
        if (items.isNotEmpty()) {
            trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, items.size)
            onDeletePermanently(items)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val count = if (selectedIds.isNotEmpty()) stringResource(R.string.selected_count, selectedIds.size)
                        else when {
                            destination == 1 && selectedAlbum != null -> selectedAlbum!!.name
                            destination == 3 && librarySection == "settings" -> stringResource(R.string.section_settings)
                            destination == 3 && librarySection == "about" -> stringResource(R.string.section_about)
                            destination == 3 && librarySection == "trash" -> stringResource(R.string.section_trash)
                            destination == 3 && librarySection == "locked" -> stringResource(R.string.section_locked)
                            destination == 3 && librarySection == "duplicates" -> stringResource(R.string.section_duplicates)
                            destination == 3 && librarySection == "memories" -> stringResource(R.string.section_memories)
                            destination == 3 && librarySection != null -> librarySection!!.replaceFirstChar { it.uppercase() }
                            destination == 2 -> favoritesTabLabel
                            destination == 1 -> albumsTabLabel
                            destination == 3 -> libraryTabLabel
                            else -> photoTabLabel
                        }
                    AnimatedContent(count, label = "topbar title") { text -> Text(text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                navigationIcon = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = ::clearSelection) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear_selection)) }
                    } else if (destination == 1 && selectedAlbum != null) {
                        IconButton(onClick = { selectedAlbumId = null }) { Icon(Icons.Outlined.ArrowBack, albumsTabLabel) }
                    } else if (destination == 3 && librarySection != null) {
                        IconButton(onClick = { librarySection = null }) { Icon(Icons.Outlined.ArrowBack, libraryTabLabel) }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        if (destination == 3 && librarySection == "trash") {
                            IconButton(onClick = { val selected = activeMedia.filter { it.id in selectedIds }; clearSelection(); handleRestore(selected) }) {
                                Icon(Icons.Outlined.RestoreFromTrash, stringResource(R.string.action_restore))
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
                    } else {
                        if (destination == 3 && librarySection == "trash" && trashed.isNotEmpty()) {
                            TextButton(onClick = { confirmEmptyTrash = true }) {
                                Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_empty_trash), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (destination == 3 && librarySection == null) {
                            IconButton(onClick = { librarySection = "settings" }) {
                                Icon(Icons.Outlined.Settings, stringResource(R.string.section_settings))
                            }
                        }
                        if (destination != 1 || selectedAlbum != null) {
                            IconButton(onClick = {
                                compactGrid = !compactGrid
                                customCellSize = if (compactGrid) 80.dp else 112.dp
                                settingsPreferences.setPhotoGridSize(customCellSize.value)
                            }) {
                                Icon(Icons.Outlined.GridView, if (compactGrid) stringResource(R.string.action_comfortable_grid) else stringResource(R.string.action_compact_grid))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
          if (selectedIds.isEmpty()) {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = destination == index,
                        onClick = { tabScope.launch {
                            tabPagerState.animateScrollToPage(index, animationSpec = tween(240, easing = FastOutSlowInEasing))
                        } },
                        icon = { AnimatedNavigationIcon(icons[index], destination == index, label) },
                        label = { Text(label) },
                    )
                }
            }
          }
        },
    ) { padding ->
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
            0 -> if (images.isNotEmpty()) PhotoGrid(
              images, padding, photoGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
              showTimeline = settings.showTimelineHeaders,
              cornerStyle = settings.cornerStyle,
              gridSpacing = settings.gridSpacing,
              showVideoDuration = settings.showVideoDurationBadge,
              showFormatBadge = settings.showMediaFormatBadge,
              selectedIds = selectedIds,
              onToggleSelection = if (onPick == null) ::toggleSelection else null,
              onSetSelection = if (onPick == null) ::setSelection else null,
              onSetDateSelection = if (onPick == null) ::setDateSelection else null,
            ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = images; selectedId = it.id } }
            else EmptyState("No photos found", padding)
            1 -> if (selectedAlbum != null) PhotoGrid(
              selectedAlbum!!.images, padding, albumPhotoGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange,
              showTimeline = settings.showTimelineHeaders,
              cornerStyle = settings.cornerStyle,
              gridSpacing = settings.gridSpacing,
              showVideoDuration = settings.showVideoDurationBadge,
              showFormatBadge = settings.showMediaFormatBadge,
              selectedIds = selectedIds, onToggleSelection = if (onPick == null) ::toggleSelection else null,
              onSetSelection = if (onPick == null) ::setSelection else null,
              onSetDateSelection = if (onPick == null) ::setDateSelection else null,
            ) { if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = selectedAlbum!!.images; selectedId = it.id } }
            else AlbumsGrid(
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
                onTogglePinned = onTogglePinnedAlbum,
                onSortChanged = onSetAlbumSort,
                onOrderChanged = onSetAlbumOrder
            ) { selectedAlbumId = it.id }
            else -> {
                if (page == 3) {
                    when (librarySection) {
                        "settings" -> SettingsScreen(
                            padding = padding,
                            settings = settings,
                            preferences = settingsPreferences,
                            onBack = { librarySection = null }
                        )
                        "about" -> AboutScreen(
                            padding = padding,
                            onBack = { librarySection = null }
                        )
                        "trash" -> if (trashed.isEmpty()) EmptyState("Trash is empty", padding) else PhotoGrid(
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
                            EmptyState("Unlock to view private items", padding)
                        } else {
                            val locked = lockedMedia
                            if (locked.isEmpty()) EmptyState("No locked items", padding) else PhotoGrid(
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
                        "memories" -> {
                            val today = LocalDate.now()
                            val memories = remember(images, today) { images.filter {
                                val date = Instant.ofEpochMilli(it.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
                                date.year < today.year && date.month == today.month && date.dayOfMonth == today.dayOfMonth
                            } }
                            if (memories.isEmpty()) EmptyState("No memories for today yet", padding) else PhotoGrid(
                                memories, padding, libraryGridState, cellSize = photoCellSize, onCellSizeChange = onCellSizeChange, showTimeline = true,
                                cornerStyle = settings.cornerStyle,
                                gridSpacing = settings.gridSpacing,
                                showVideoDuration = settings.showVideoDurationBadge,
                                showFormatBadge = settings.showMediaFormatBadge,
                            ) {
                                viewerImages = memories; selectedId = it.id
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
                            onTrash = handleTrash,
                        )
                        else -> LibraryScreen(padding, trashed.size, lockedMedia.size) {
                            librarySection = it
                        }
                    }
                    return@HorizontalPager
                }
                if (favoriteImages.isEmpty()) EmptyState(stringResource(R.string.empty_favorites), padding)
                else PhotoGrid(
                    images = favoriteImages,
                    padding = padding,
                    gridState = favoriteGridState,
                    cellSize = photoCellSize,
                    onCellSizeChange = onCellSizeChange,
                    showTimeline = settings.showTimelineHeaders,
                    cornerStyle = settings.cornerStyle,
                    gridSpacing = settings.gridSpacing,
                    showVideoDuration = settings.showVideoDurationBadge,
                    showFormatBadge = settings.showMediaFormatBadge,
                    selectedIds = selectedIds,
                    onToggleSelection = if (onPick == null) ::toggleSelection else null,
                    onSetSelection = if (onPick == null) ::setSelection else null,
                    onSetDateSelection = if (onPick == null) ::setDateSelection else null,
                ) {
                    if (selectedIds.isNotEmpty()) toggleSelection(it.id) else if (onPick != null) onPick(it) else { viewerImages = favoriteImages; selectedId = it.id }
                }
            }
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

    BackHandler(enabled = selectedIds.isNotEmpty()) { clearSelection() }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 1 && selectedAlbum != null) { selectedAlbumId = null }
    BackHandler(enabled = selectedIds.isEmpty() && destination == 3 && librarySection != null) { librarySection = null }
    BackHandler(enabled = editorImage != null) { editorImage = null }

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
            doubleTapZoomLevel = settings.doubleTapZoomLevel,
            isLocked = isViewingLocked,
            isInTrash = isViewingTrash,
            confirmDeleteSetting = settings.confirmDelete,
            availableAlbums = availableAlbums,
            onToggleFavorite = onToggleFavorite,
            onClose = { selectedId = null },
            onDelete = { media, deletePermanently ->
                selectedId = null
                if (isViewingTrash || deletePermanently) {
                    handleDeletePermanently(listOf(media))
                } else if (isViewingLocked) {
                    trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, 1)
                    onDeleteFromLocked(listOf(media))
                } else {
                    handleTrash(listOf(media))
                }
            },
            onRestore = { media ->
                handleRestore(listOf(media))
            },
            onEditMetadata = onEditMetadata,
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
    editorImage?.let { image ->
        EditorScreen(image, onClose = { editorImage = null }, onSaved = { saved ->
            if (saved) editorImage = null
            Toast.makeText(context, if (saved) context.getString(R.string.toast_edited_saved) else context.getString(R.string.toast_edited_failed),
                Toast.LENGTH_SHORT).show()
        })
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
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
    val groups = remember(images, showTimeline) {
        if (showTimeline) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val yesterday = today.minusDays(1)
            val dayCache = HashMap<LocalDate, String>()
            images.groupBy { image ->
                val date = Instant.ofEpochMilli(image.dateTaken).atZone(zone).toLocalDate()
                dayCache.getOrPut(date) {
                    when (date) {
                        today -> "Today"
                        yesterday -> "Yesterday"
                        else -> date.format(if (date.year == today.year) timelinePatternSameYear else timelinePatternOtherYear)
                    }
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
    var scrubberDragging by remember { mutableStateOf(false) }
    var scrubTargetIndex by remember(timelineItems) { mutableIntStateOf(0) }
    var suppressReleaseClickId by remember { mutableStateOf<Long?>(null) }
    val scrubberScope = rememberCoroutineScope()
    val scrollFraction by remember(timelineItems) { derivedStateOf {
        val index = if (scrubberDragging) scrubTargetIndex else gridState.firstVisibleItemIndex
        index.toFloat() / timelineItems.lastIndex.coerceAtLeast(1)
    } }
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
                                val nextSize = (currentCellSize.value * zoom).coerceIn(60f, 220f)
                                currentOnCellSizeChange?.invoke(nextSize.dp)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
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
            contentPadding = PaddingValues(horizontal = gridSpacing.dp.dp, vertical = (gridSpacing.dp + 3).dp),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
            verticalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
        ) {
            groups.forEach { group ->
              if (showTimeline) {
                item(key = "header:${group.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    val ids = remember(group.value) { group.value.map { it.id } }
                    val wholeDateSelected = ids.isNotEmpty() && ids.all { it in selectedIds }
                    Row(Modifier.animateItem().fillMaxWidth().combinedClickable(
                        onClick = {
                            if (selectedIds.isNotEmpty()) onSetDateSelection?.invoke(ids, !wholeDateSelected)
                        },
                        onLongClick = { onSetDateSelection?.invoke(ids, !wholeDateSelected) },
                    ).padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(group.key, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        AnimatedVisibility(selectedIds.isNotEmpty()) {
                            Text(if (wholeDateSelected) "Deselect date" else "Select date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
              }
              items(group.value, key = { it.id }, contentType = { "photo" }) { image ->
                val selected = image.id in selectedIds
                Box(Modifier.animateItem().aspectRatio(1f).clip(RoundedCornerShape(if (selected) 14.dp else cornerStyle.dp.dp))
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
        if (showTimeline && timelineItems.size > 30) {
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .22f)
            val thumbColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp)
                .pointerInput(timelineItems.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        scrubberDragging = true
                        var finalTarget = gridState.firstVisibleItemIndex
                        try {
                            var change = down
                            do {
                                val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                                finalTarget = (fraction * timelineItems.lastIndex).toInt()
                                scrubTargetIndex = finalTarget
                                change.consume()
                                change = awaitPointerEvent().changes.first()
                            } while (change.pressed)
                        } finally {
                            scrubberDragging = false
                            scrubberScope.launch { gridState.scrollToItem(finalTarget) }
                        }
                    }
                }) {
                val inset = 14.dp.toPx()
                val trackWidth = 4.dp.toPx()
                val thumbWidth = 8.dp.toPx()
                val thumbHeight = 48.dp.toPx()
                val travel = (size.height - inset * 2).coerceAtLeast(1f)
                val centerY = inset + scrollFraction.coerceIn(0f, 1f) * travel
                drawRoundRect(trackColor, Offset((size.width - trackWidth) / 2f, inset),
                    Size(trackWidth, travel), CornerRadius(trackWidth))
                drawRoundRect(thumbColor, Offset((size.width - thumbWidth) / 2f,
                    (centerY - thumbHeight / 2f).coerceIn(inset, size.height - inset - thumbHeight)),
                    Size(thumbWidth, thumbHeight), CornerRadius(thumbWidth))
            }
            AnimatedVisibility(visible = scrubberDragging,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp),
                enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = .88f),
                exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = .9f)) {
                Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(visibleDate?.let { timelineLabel(it.dateTaken) } ?: "",
                        Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

private val timelinePatternSameYear = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
private val timelinePatternOtherYear = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

private fun timelineLabel(
    timestamp: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(if (date.year == today.year) timelinePatternSameYear else timelinePatternOtherYear)
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
    doubleTapZoomLevel: Float = 2.5f,
    isLocked: Boolean = false,
    isInTrash: Boolean = false,
    confirmDeleteSetting: Boolean = false,
    availableAlbums: List<MediaAlbum> = emptyList(),
    onToggleFavorite: (Long) -> Unit,
    onClose: () -> Unit,
    onDelete: (MediaImage, Boolean) -> Unit,
    onRestore: (MediaImage) -> Unit = {},
    onEditMetadata: (MediaImage, String, String, Long, Int) -> Unit,
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
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val handleClose = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClose()
    }
    BackHandler(enabled = true) { handleClose() }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    var showInfo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var zoomedImageId by remember { mutableStateOf<Long?>(null) }
    var viewerMenuExpanded by remember { mutableStateOf(false) }
    var viewerAlbumAction by remember { mutableStateOf<AlbumAction?>(null) }
    val current = images[pagerState.currentPage]
    val videoEngine = remember { Media3VideoEngine(context) }
    DisposableEffect(videoEngine) { onDispose { videoEngine.release() } }
    LaunchedEffect(current.id) {
        if (current.isVideo) videoEngine.load(current.uri) else videoEngine.player.pause()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080808)) {
      Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = zoomedImageId != current.id,
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
                    onTap = { controlsVisible = !controlsVisible },
                    onZoomChanged = { zoomed -> zoomedImageId = if (zoomed) media.id else null },
                )
            } else {
                ZoomablePhoto(
                    image = media,
                    doubleTapZoomLevel = doubleTapZoomLevel,
                    onTap = { controlsVisible = !controlsVisible },
                    onZoomChanged = { zoomed ->
                        zoomedImageId = if (zoomed) media.id else null
                    },
                )
            }
        }
        AnimatedVisibility(
          visible = controlsVisible,
          modifier = Modifier.align(Alignment.TopCenter),
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
            IconButton(onClick = handleClose) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(current.name, color = Color.White, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.viewer_page_count, pagerState.currentPage + 1, images.size), color = Color.White.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelMedium)
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
              .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
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
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
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
                ViewerIconButton(
                    icon = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = if (isFav) stringResource(R.string.action_favorited) else stringResource(R.string.action_favorite),
                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    scaleEffect = favScale,
                    modifier = Modifier.weight(1f),
                ) { onToggleFavorite(current.id) }
                if (!current.isVideo) {
                    ViewerIconButton(
                        icon = Icons.Outlined.Edit,
                        label = stringResource(R.string.action_edit),
                        modifier = Modifier.weight(1f),
                    ) { onEdit(current) }
                }
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
    }

    if (showInfo) PhotoDetailsSheet(current, onDismiss = { showInfo = false }, onSave = { name, title, captured, orientation ->
        onEditMetadata(current, name, title, captured, orientation); showInfo = false
    })
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
}

@Composable
private fun ZoomablePhoto(
    image: MediaImage,
    doubleTapZoomLevel: Float = 2.5f,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember(image.id) { mutableFloatStateOf(1f) }
    var offset by remember(image.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(image.id) { mutableStateOf(IntSize.Zero) }

    // Instant zero-delay preview from memory cache while full image decodes
    val cachedThumb = remember(image.id) { ThumbnailCache.get(image.id) }
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
        val imgWidth = if (hasIntrinsic) intrinsic.width else image.width.toFloat().coerceAtLeast(1f)
        val imgHeight = if (hasIntrinsic) intrinsic.height else image.height.toFloat().coerceAtLeast(1f)
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
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                                onZoomChanged(false)
                            } else {
                                val targetScale = doubleTapZoomLevel
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val z = targetScale / scale
                                val targetOffset = offset + (tapPos - center - offset) * (1f - z)
                                offset = clampOffset(targetOffset, targetScale)
                                scale = targetScale
                                onZoomChanged(true)
                            }
                        },
                    )
                }
                .pointerInput(image.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pointersDown = event.changes.count { it.pressed }
                            if (pointersDown >= 2 || (pointersDown == 1 && scale > 1f)) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val isTransforming = pointersDown >= 2 || panChange.getDistance() > 0.5f

                                val validZoom = if (!zoomChange.isNaN() && zoomChange > 0f) zoomChange else 1f
                                val validPan = if (panChange.isSpecified && !panChange.x.isNaN() && !panChange.y.isNaN()) panChange else Offset.Zero

                                val calculatedScale = (scale * validZoom).coerceIn(1f, 7f)
                                val nextScale = if (calculatedScale < 1.02f) 1f else calculatedScale

                                if (pointersDown >= 2 && containerSize != IntSize.Zero) {
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid.isSpecified && !centroid.x.isNaN() && !centroid.y.isNaN()) {
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val effectiveZoom = nextScale / scale
                                        val focalOffset = (offset + validPan) + (centroid - center - offset) * (1f - effectiveZoom)
                                        offset = clampOffset(focalOffset, nextScale)
                                    } else {
                                        offset = clampOffset(offset + validPan, nextScale)
                                    }
                                } else {
                                    offset = clampOffset(offset + validPan, nextScale)
                                }

                                scale = nextScale
                                onZoomChanged(scale > 1.01f)
                                if (isTransforming) event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            val intrinsic = painter.intrinsicSize
            val hasValidIntrinsic = intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f
            val imgWidth = if (hasValidIntrinsic) intrinsic.width else image.width.toFloat().coerceAtLeast(1f)
            val imgHeight = if (hasValidIntrinsic) intrinsic.height else image.height.toFloat().coerceAtLeast(1f)

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
private fun PhotoDetailsSheet(image: MediaImage, onDismiss: () -> Unit,
    onSave: (String, String, Long, Int) -> Unit) {
    val context = LocalContext.current
    val exif by produceState<ExifMetadata?>(initialValue = null, image.id, image.uri) {
        value = withContext(Dispatchers.IO) {
            loadExifMetadata(context, image.uri)
        }
    }
    var editing by remember { mutableStateOf(false) }
    var editedName by remember(image.id) { mutableStateOf(image.name) }
    var editedTitle by remember(image.id) { mutableStateOf(image.title) }
    var editedOrientation by remember(image.id) { mutableStateOf(image.orientation.toString()) }
    val metadataFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var editedDate by remember(image.id) { mutableStateOf(metadataFormat.format(Date(image.dateTaken))) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(image.name.ifBlank { stringResource(R.string.details_photo_details) }, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)

            exif?.let { data ->
                val hasCamera = data.cameraModel != null || data.aperture != null || data.shutterSpeed != null || data.iso != null || data.focalLength != null
                if (hasCamera) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CameraAlt, stringResource(R.string.details_camera), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(data.cameraModel ?: stringResource(R.string.details_camera_capture), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            val specs = listOfNotNull(data.aperture, data.shutterSpeed, data.focalLength, data.iso).joinToString(" · ")
                            if (specs.isNotBlank()) {
                                Text(specs, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (data.flash != null) {
                                Text(data.flash, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (data.latitude != null && data.longitude != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.LocationOn, stringResource(R.string.details_location), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.details_location), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                                Text("%.4f, %.4f".format(Locale.US, data.latitude, data.longitude),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                val uri = Uri.parse("geo:${data.latitude},${data.longitude}?q=${data.latitude},${data.longitude}(Photo+Location)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                runCatching { context.startActivity(intent) }
                            }) {
                                Icon(Icons.Outlined.Map, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.details_map))
                            }
                        }
                    }
                }
            }

            if (image.title.isNotBlank()) DetailBlock(stringResource(R.string.details_title_field), image.title)
            if (image.description.isNotBlank()) DetailBlock(stringResource(R.string.details_desc_field), image.description)
            DetailItem(stringResource(R.string.details_captured), DateFormat.getDateTimeInstance().format(Date(image.dateTaken)))
            DetailItem(stringResource(R.string.details_resolution), "${image.width} × ${image.height}")
            DetailItem(stringResource(R.string.details_type), image.mimeType.ifBlank { if (image.isVideo) stringResource(R.string.format_video) else stringResource(R.string.format_image) })
            DetailItem(stringResource(R.string.details_size), formatFileSize(image.sizeBytes))
            if (image.orientation != 0) DetailItem(stringResource(R.string.details_orientation), "${image.orientation}°")
            if (image.isVideo) DetailItem(stringResource(R.string.details_duration), formatMediaDuration(image.durationMs))
            DetailBlock(stringResource(R.string.details_path), image.path)
            if (!image.isVideo) Button(onClick = { editing = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.details_edit_metadata)) }
        }
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text(stringResource(R.string.details_edit_metadata)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedTextField(editedName, { editedName = it },
                label = { Text(stringResource(R.string.details_edit_filename)) }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(editedTitle, { editedTitle = it },
                label = { Text(stringResource(R.string.details_edit_title)) }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(editedDate, { editedDate = it },
                label = { Text(stringResource(R.string.details_edit_captured_hint)) }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(editedOrientation, { editedOrientation = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.details_edit_orientation_hint)) }, singleLine = true)
            Text(stringResource(R.string.details_edit_permission_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton(onClick = {
            val timestamp = runCatching { metadataFormat.parse(editedDate)?.time }.getOrNull()
            val orientation = editedOrientation.toIntOrNull()
            if (editedName.isNotBlank() && timestamp != null && orientation in listOf(0, 90, 180, 270)) {
                editing = false; onSave(editedName.trim(), editedTitle.trim(), timestamp, orientation!!)
            }
        }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
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
private fun DetailBlock(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EmptyState(message: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
