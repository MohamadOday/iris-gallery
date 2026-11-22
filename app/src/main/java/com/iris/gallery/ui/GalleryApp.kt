@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.app.KeyguardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.saveExifToMedia
import com.iris.gallery.data.resolveMediaUri
import com.iris.gallery.ui.GalleryViewModel
import com.iris.gallery.ui.EditorScreen
import com.iris.gallery.ui.AppLockScreen
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import androidx.compose.material.icons.outlined.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import com.iris.gallery.data.AlbumAction
import com.iris.gallery.data.AlbumOperationResult
import com.iris.gallery.MemoriesNotifications
import com.iris.gallery.R
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

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        Manifest.permission.ACCESS_MEDIA_LOCATION,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.ACCESS_MEDIA_LOCATION,
    )
    Build.VERSION.SDK_INT >= 29 -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION,
    )
    else -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )
}

private fun requiredPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT <= 29 -> mediaPermissions() + Manifest.permission.WRITE_EXTERNAL_STORAGE
    else -> mediaPermissions()
}

private fun hasMediaPermission(context: android.content.Context): Boolean {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PermissionChecker.PERMISSION_GRANTED
    return when {
        Build.VERSION.SDK_INT >= 34 -> {
            // On Android 14+, user may choose "Select photos" which grants
            // READ_MEDIA_VISUAL_USER_SELECTED without the full media permissions.
            (granted(Manifest.permission.READ_MEDIA_IMAGES) && granted(Manifest.permission.READ_MEDIA_VIDEO)) ||
            granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
        else -> mediaPermissions().all { granted(it) }
    }
}

private fun hasStorageWritePermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= 30) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PermissionChecker.PERMISSION_GRANTED
}

/**
 * When only some originals could be deleted, keep the trash copies for the ones
 * that are already gone. Rolling all of them back would delete the only copy.
 */
private fun trashRollbackItems(moveResult: com.iris.gallery.data.TrashMoveResult): List<MediaImage> {
    val alreadyDeleted = moveResult.deletedOriginalIds
    return moveResult.trashedMedia.filterIndexed { index, _ ->
        moveResult.originalMedia.getOrNull(index)?.id !in alreadyDeleted
    }
}

/**
 * When only some originals could be deleted during a vault move, keep the vault copies
 * for the items whose originals are already gone from external storage.
 */
private fun vaultRollbackItems(moveResult: com.iris.gallery.data.VaultMoveResult): List<MediaImage> {
    val alreadyDeleted = moveResult.deletedOriginalIds
    return moveResult.vaultedMedia.filterIndexed { index, _ ->
        moveResult.originalMedia.getOrNull(index)?.id !in alreadyDeleted
    }
}

private data class PendingMoveData(
    val mediaList: List<MediaImage>,
    val targetDir: java.io.File,
    val targetAlbumName: String,
    val alreadyMoved: List<MediaImage>,
    val callback: (AlbumOperationResult) -> Unit
)

@Composable
internal fun GalleryApp(
    settings: SettingsState,
    settingsPreferences: SettingsPreferences,
    requestedType: String? = null,
    initialViewUri: Uri? = null,
    initialEditMode: Boolean = false,
    initialMemories: Boolean = false,
    initialOpenLatest: Boolean = false,
    initialIntentGeneration: Long = 0L,
    onPick: ((MediaImage) -> Unit)? = null,
    viewModel: GalleryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val permissions = remember { requiredPermissions() }
    var permitted by remember {
        mutableStateOf(hasMediaPermission(context))
    }
    var pendingWriteRequest by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = hasMediaPermission(context)
        if (pendingWriteRequest) {
            pendingWriteRequest = false
            val message = if (hasStorageWritePermission(context)) {
                R.string.toast_storage_permission_granted
            } else {
                R.string.toast_storage_permission_needed
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun ensureStorageWrite(): Boolean {
        if (hasStorageWritePermission(context)) return true
        pendingWriteRequest = true
        permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        return false
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val coroutineScope = rememberCoroutineScope()
    var trashFeedback by remember { mutableStateOf<TrashFeedback?>(null) }
    var pendingPermanentDeleteMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingPermanentDeleteInternalMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingPermanentDeleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingPermanentDeleteMedia
        val pendingInternal = pendingPermanentDeleteInternalMedia
        val cb = pendingPermanentDeleteCallback
        pendingPermanentDeleteMedia = null
        pendingPermanentDeleteInternalMedia = null
        pendingPermanentDeleteCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (!pendingInternal.isNullOrEmpty()) {
                coroutineScope.launch {
                    val delIds = ((pending ?: emptyList()) + pendingInternal).map { it.id }.toSet()
                    val delPaths = ((pending ?: emptyList()) + pendingInternal).map { it.path }.toSet()
                    viewModel.markMediaDeleted(delIds, delPaths)
                    viewModel.deletePermanently(pendingInternal)
                    trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, (pending?.size ?: 0) + pendingInternal.size)
                    viewModel.checkForChanges()
                    cb?.invoke()
                }
            } else {
                if (pending != null) {
                    val delIds = pending.map { it.id }.toSet()
                    val delPaths = pending.map { it.path }.toSet()
                    viewModel.markMediaDeleted(delIds, delPaths)
                    trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, pending.size)
                }
                viewModel.checkForChanges()
                cb?.invoke()
            }
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
            coroutineScope.launch {
                val uri = canonicalMediaUri(context, media)
                withContext(Dispatchers.IO) {
                    saveExifToMedia(context, uri, media.path, request)
                    if (media.id > 0 && uri.toString().startsWith("content://media/")) {
                        runCatching { context.contentResolver.update(uri, values, null, null) }
                    }
                }
                if (media.path.isNotBlank() && !media.path.startsWith(context.filesDir.absolutePath)) {
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(media.path), null, null)
                }
                val updated = viewModel.updateMediaMetadata(media, request)
                callback?.invoke(updated)
                Toast.makeText(context, R.string.toast_metadata_updated, Toast.LENGTH_SHORT).show()
            }
        } else {
            pending?.third?.second?.invoke(null)
        }
    }
    var lockedAuthorized by remember { mutableStateOf(false) }
    var isVaultAuthInProgress by remember { mutableStateOf(false) }
    var isAppUnlocked by remember {
        mutableStateOf(!settings.appLockEnabled || !settings.hasPin || isSessionAppUnlocked)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentShowHiddenItems by androidx.compose.runtime.rememberUpdatedState(settings.showHiddenItems)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!isVaultAuthInProgress) {
                    lockedAuthorized = false
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (permitted) {
                    viewModel.checkForChanges()
                    if (currentShowHiddenItems) {
                        viewModel.refreshHiddenMedia()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isVaultAuthInProgress = false
        lockedAuthorized = it.resultCode == Activity.RESULT_OK
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val vaultMedia by viewModel.vaultMedia.collectAsStateWithLifecycle()
    val hiddenMedia by viewModel.hiddenMedia.collectAsStateWithLifecycle()
    val vaultProgress by viewModel.vaultProgress.collectAsStateWithLifecycle()
    val duplicateState by viewModel.duplicateState.collectAsStateWithLifecycle()

    var pendingMove by remember { mutableStateOf<PendingMoveData?>(null) }
    val moveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingMove
        pendingMove = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            coroutineScope.launch {
                val secondResult = viewModel.moveMediaToAlbum(pending.mediaList, pending.targetDir, pending.targetAlbumName)
                val totalMoved = pending.alreadyMoved + secondResult.movedMedia
                val totalSuccess = totalMoved.size
                val totalFailed = secondResult.failedCount
                val combinedResult = AlbumOperationResult(
                    successCount = totalSuccess,
                    failedCount = totalFailed,
                    targetAlbumName = pending.targetAlbumName,
                    action = AlbumAction.MOVE,
                    movedMedia = totalMoved,
                    successfulSourceMedia = secondResult.successfulSourceMedia,
                    failedMedia = secondResult.failedMedia
                )
                pending.callback(combinedResult)
            }
        } else {
            pending?.let { p ->
                val fallbackResult = AlbumOperationResult(
                    successCount = p.alreadyMoved.size,
                    failedCount = p.mediaList.size,
                    targetAlbumName = p.targetAlbumName,
                    action = AlbumAction.MOVE,
                    movedMedia = p.alreadyMoved,
                    successfulSourceMedia = emptyList(),
                    failedMedia = p.mediaList
                )
                p.callback(fallbackResult)
            }
        }
    }

    var pendingVaultMove by remember { mutableStateOf<com.iris.gallery.data.VaultMoveResult?>(null) }
    var pendingVaultCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val vaultDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingVaultMove
        val cb = pendingVaultCallback
        pendingVaultMove = null
        pendingVaultCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            if (pending != null) {
                val delIds = pending.originalMedia.map { it.id }.toSet()
                val delPaths = pending.originalMedia.map { it.path }.toSet()
                viewModel.markMediaDeleted(delIds, delPaths)
                viewModel.setLocked(delIds, false)
            }
            viewModel.checkForChanges()
            cb?.invoke()
            val count = pending?.vaultedMedia?.size ?: 0
            Toast.makeText(context, context.getString(R.string.toast_items_vaulted, count), Toast.LENGTH_SHORT).show()
        } else {
            pending?.let { moveResult ->
                coroutineScope.launch {
                    viewModel.rollbackVaultMove(vaultRollbackItems(moveResult))
                    if (moveResult.deletedOriginalIds.isNotEmpty()) {
                        viewModel.checkForChanges()
                        cb?.invoke()
                    }
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
            viewModel.checkForChanges()
            val count = pending?.trashedMedia?.size ?: 0
            trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, count)
            cb?.invoke()
        } else {
            pending?.let { moveResult ->
                coroutineScope.launch {
                    viewModel.rollbackTrashMove(trashRollbackItems(moveResult))
                    viewModel.checkForChanges()
                }
            }
        }
    }

    var pendingSystemTrashMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingSystemTrashCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSystemTrashInternalMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    val systemTrashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val items = pendingSystemTrashMedia
        val cb = pendingSystemTrashCallback
        val internalItems = pendingSystemTrashInternalMedia
        pendingSystemTrashMedia = null
        pendingSystemTrashCallback = null
        pendingSystemTrashInternalMedia = null
        if (result.resultCode == Activity.RESULT_OK && items != null) {
            val delIds = items.map { it.id }.toSet()
            val delPaths = items.map { it.path }.toSet()
            viewModel.markMediaDeleted(delIds, delPaths)
            if (internalItems.isNullOrEmpty()) {
                viewModel.checkForChanges()
                trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, items.size)
                cb?.invoke()
            } else {
                coroutineScope.launch {
                    val moveResult = viewModel.moveToTrash(internalItems)
                    viewModel.checkForChanges()
                    trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, items.size + moveResult.trashedMedia.size)
                    cb?.invoke()
                }
            }
        }
    }

    var pendingSystemRestoreMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingSystemRestoreInternalMedia by remember { mutableStateOf<List<MediaImage>?>(null) }
    var pendingSystemRestoreCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val systemRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val items = pendingSystemRestoreMedia
        val internalItems = pendingSystemRestoreInternalMedia
        val cb = pendingSystemRestoreCallback
        pendingSystemRestoreMedia = null
        pendingSystemRestoreInternalMedia = null
        pendingSystemRestoreCallback = null
        if (result.resultCode == Activity.RESULT_OK && items != null) {
            val ids = items.map { it.id }.toSet()
            val paths = items.map { it.path }.toSet()
            viewModel.restoreSystemTrash(ids, paths)
            if (internalItems.isNullOrEmpty()) {
                trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, items.size)
                cb?.invoke()
            } else {
                coroutineScope.launch {
                    val internalResult = viewModel.restoreFromTrash(internalItems)
                    val totalRestored = items.size + internalResult.restored.size
                    trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, totalRestored)
                    if (internalResult.failed > 0) {
                        Toast.makeText(context, context.getString(R.string.toast_restore_failed_count, internalResult.failed), Toast.LENGTH_LONG).show()
                    }
                    if (totalRestored > 0) {
                        cb?.invoke()
                    }
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
            viewModel.checkForChanges()
            viewModel.registerObserver()
            viewModel.rescanNewFilesInBackground()
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
            EditorScreen(standaloneEditorMedia!!, onClose = { standaloneEditorMedia = null }, onSaved = { saved, overwritten ->
                if (saved) standaloneEditorMedia = null
                val msg = if (!saved) {
                    context.getString(R.string.toast_edited_failed)
                } else if (overwritten) {
                    context.getString(R.string.toast_edited_overwritten)
                } else {
                    context.getString(R.string.toast_edited_saved)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            })
        } else if (standaloneExternalMedia != null) {
            PhotoViewer(
                images = listOf(standaloneExternalMedia!!),
                initialPage = 0,
                favorites = emptySet(),
                autoPlay = settings.autoPlayVideo,
                loop = settings.loopVideo,
                videoDoubleTapToZoom = settings.videoDoubleTapToZoom,
                videoGestureControls = settings.videoGestureControls,
                secureSharingMode = settings.secureSharingMode,
                onSetSecureSharingMode = { settingsPreferences.setSecureSharingMode(it) },
                dismissedSecureSharingTip = settings.dismissedSecureSharingTip,
                onDismissSecureSharingTip = { settingsPreferences.setDismissedSecureSharingTip(true) },
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
                videoMuted = settings.videoMuted,
                onSetVideoMuted = { settingsPreferences.setVideoMuted(it) },
                preferredEditor = settings.preferredEditor,
                onSetPreferredEditor = { settingsPreferences.setPreferredEditor(it) },
                availableAlbums = emptyList(),
                onToggleFavorite = { },
                onClose = { standaloneExternalMedia = null },
                onDelete = { item, _ ->
                    val deleted = runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }
                        .onFailure { android.util.Log.w("IrisGallery", "standalone delete failed", it) }
                        .getOrDefault(false)
                    if (deleted) {
                        standaloneExternalMedia = null
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                    }
                },
                onRestore = { _, _ -> },
                onEditMetadata = { _, _ -> },
                onEdit = { standaloneEditorMedia = it; standaloneExternalMedia = null },
                onLock = { },
                onUnlock = { },
                onMoveToAlbum = { _, _, _, cb -> cb(AlbumOperationResult(0, 0, "", AlbumAction.MOVE)) },
                onCopyToAlbum = { _, _, _, cb -> cb(AlbumOperationResult(0, 0, "", AlbumAction.COPY)) },
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
                    pinLockoutRemainingMs = { settingsPreferences.pinLockoutRemainingMs() },
                    onResetLock = {
                        settingsPreferences.removePin()
                        Toast.makeText(context, R.string.app_lock_reset_success, Toast.LENGTH_LONG).show()
                    },
                    onUnlocked = {
                        isSessionAppUnlocked = true
                        isAppUnlocked = true
                    }
                )
            } else {
                val visibleMedia = remember(state.images, hiddenMedia, settings.showHiddenItems, requestedType, libraryState.lockedMedia, libraryState.excludedFolders) {
                    val merged = if (settings.showHiddenItems && hiddenMedia.isNotEmpty()) {
                        val existingPaths = state.images.mapTo(HashSet(state.images.size)) { it.path }
                        val extras = hiddenMedia.filter { it.path.isNotBlank() && it.path !in existingPaths }
                        if (extras.isEmpty()) state.images
                        else (state.images + extras).sortedWith(
                            compareByDescending<MediaImage> { it.dateTaken }
                                .thenByDescending { it.dateModified }
                                .thenByDescending { it.dateAdded }
                                .thenByDescending { it.id }
                        )
                    } else {
                        state.images
                    }
                    val requested = when {
                        requestedType?.startsWith("image/") == true -> merged.filterNot { it.isVideo }
                        requestedType?.startsWith("video/") == true -> merged.filter { it.isVideo }
                        else -> merged
                    }
                    requested.filterNot { it.id in libraryState.lockedMedia }
                        .filterNot { img -> isPathExcluded(img.path, libraryState.excludedFolders) }
                }
                var showHiddenAccessDialog by remember { mutableStateOf(false) }
                val allLockedMedia = remember(vaultMedia, state.images, libraryState.lockedMedia) {
                    val galleryLocked = state.images.filter { it.id in libraryState.lockedMedia }
                    vaultMedia + galleryLocked
                }
                var showAllFilesAccessPromptDialog by remember { mutableStateOf(false) }
                var pendingVaultItems by remember { mutableStateOf<List<MediaImage>?>(null) }
                var pendingVaultItemsCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var awaitingAllFilesAccessGrant by remember { mutableStateOf(false) }

                fun executeVaultMove(mediaList: List<MediaImage>, onConfirmed: (() -> Unit)? = null) {
                    if (vaultProgress != null) return
                    val externalItems = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                    if (externalItems.isEmpty()) {
                        onConfirmed?.invoke()
                        return
                    }
                    coroutineScope.launch {
                        val moveResult = viewModel.moveToVault(externalItems)
                        if (moveResult.indexWriteFailed) {
                            viewModel.rollbackVaultMove(vaultRollbackItems(moveResult))
                            Toast.makeText(context, context.getString(R.string.toast_vault_index_failed), Toast.LENGTH_LONG).show()
                        } else if (moveResult.vaultedMedia.isNotEmpty()) {
                            val alreadyDeletedItems = moveResult.originalMedia.filter { it.id in moveResult.deletedOriginalIds }
                            if (alreadyDeletedItems.isNotEmpty()) {
                                val delIds = alreadyDeletedItems.map { it.id }.toSet()
                                val delPaths = alreadyDeletedItems.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                viewModel.setLocked(delIds, false)
                            }
                            val remainingOriginals = moveResult.originalMedia.filter { it.id !in moveResult.deletedOriginalIds }
                            if (moveResult.silentSuccess || remainingOriginals.isEmpty()) {
                                val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                viewModel.setLocked(delIds, false)
                                viewModel.refresh(showLoading = false)
                                onConfirmed?.invoke()
                                Toast.makeText(context, context.getString(R.string.toast_items_vaulted, moveResult.vaultedMedia.size), Toast.LENGTH_SHORT).show()
                            } else if (Build.VERSION.SDK_INT >= 30) {
                                runCatching {
                                    pendingVaultMove = moveResult
                                    pendingVaultCallback = onConfirmed
                                    val request = MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        remainingOriginals.map { canonicalMediaUri(context, it) }
                                    )
                                    vaultDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                }.onFailure {
                                    pendingVaultMove = null
                                    pendingVaultCallback = null
                                    val newlyDeletedIds = remainingOriginals.filter { item ->
                                        runCatching {
                                            context.contentResolver.delete(canonicalMediaUri(context, item), null, null) > 0 ||
                                                java.io.File(item.path).delete() ||
                                                (item.path.isNotBlank() && !java.io.File(item.path).exists())
                                        }.getOrDefault(false)
                                    }.map { it.id }.toSet()
                                    val totalDeletedIds = moveResult.deletedOriginalIds + newlyDeletedIds
                                    if (newlyDeletedIds.isNotEmpty()) {
                                        val newlyDeletedItems = remainingOriginals.filter { it.id in newlyDeletedIds }
                                        viewModel.markMediaDeleted(newlyDeletedIds, newlyDeletedItems.map { it.path }.toSet())
                                        viewModel.setLocked(newlyDeletedIds, false)
                                    }
                                    if (totalDeletedIds.size == moveResult.originalMedia.size) {
                                        viewModel.refresh(showLoading = false)
                                        onConfirmed?.invoke()
                                        Toast.makeText(context, context.getString(R.string.toast_items_vaulted, moveResult.vaultedMedia.size), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val toRollback = moveResult.vaultedMedia.filterIndexed { idx, _ ->
                                            moveResult.originalMedia.getOrNull(idx)?.id !in totalDeletedIds
                                        }
                                        viewModel.rollbackVaultMove(toRollback)
                                        if (totalDeletedIds.isNotEmpty()) {
                                            viewModel.refresh(showLoading = false)
                                            onConfirmed?.invoke()
                                        }
                                        Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                val newlyDeletedIds = remainingOriginals.filter { item ->
                                    runCatching {
                                        context.contentResolver.delete(canonicalMediaUri(context, item), null, null) > 0 ||
                                            java.io.File(item.path).delete() ||
                                            (item.path.isNotBlank() && !java.io.File(item.path).exists())
                                    }.getOrDefault(false)
                                }.map { it.id }.toSet()
                                val totalDeletedIds = moveResult.deletedOriginalIds + newlyDeletedIds
                                if (newlyDeletedIds.isNotEmpty()) {
                                    val newlyDeletedItems = remainingOriginals.filter { it.id in newlyDeletedIds }
                                    viewModel.markMediaDeleted(newlyDeletedIds, newlyDeletedItems.map { it.path }.toSet())
                                    viewModel.setLocked(newlyDeletedIds, false)
                                }
                                if (totalDeletedIds.size == moveResult.originalMedia.size) {
                                    viewModel.refresh(showLoading = false)
                                    onConfirmed?.invoke()
                                    Toast.makeText(context, context.getString(R.string.toast_items_vaulted, moveResult.vaultedMedia.size), Toast.LENGTH_SHORT).show()
                                } else {
                                    val toRollback = moveResult.vaultedMedia.filterIndexed { idx, _ ->
                                        moveResult.originalMedia.getOrNull(idx)?.id !in totalDeletedIds
                                    }
                                    viewModel.rollbackVaultMove(toRollback)
                                    if (totalDeletedIds.isNotEmpty()) {
                                        viewModel.refresh(showLoading = false)
                                        onConfirmed?.invoke()
                                    }
                                    Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else if (moveResult.failedCount > 0) {
                            Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val vaultResumeObserver = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME && awaitingAllFilesAccessGrant) {
                            awaitingAllFilesAccessGrant = false
                            val pending = pendingVaultItems
                            val cb = pendingVaultItemsCallback
                            if (pending != null && Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
                                pendingVaultItems = null
                                pendingVaultItemsCallback = null
                                executeVaultMove(pending, cb)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(vaultResumeObserver)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(vaultResumeObserver) }
                }

                val onLockMedia: (List<MediaImage>, (() -> Unit)?) -> Unit = { mediaList, onConfirmed ->
                    if (mediaList.isNotEmpty()) {
                        if (settings.vaultHideFromStorage) {
                            if (Build.VERSION.SDK_INT >= 30) {
                                if (Environment.isExternalStorageManager()) {
                                    executeVaultMove(mediaList, onConfirmed)
                                } else {
                                    pendingVaultItems = mediaList
                                    pendingVaultItemsCallback = onConfirmed
                                    showAllFilesAccessPromptDialog = true
                                }
                            } else if (ensureStorageWrite()) {
                                executeVaultMove(mediaList, onConfirmed)
                            }
                        } else {
                            viewModel.setLocked(mediaList.map { it.id }, true)
                            onConfirmed?.invoke()
                            Toast.makeText(context, context.getString(R.string.toast_vault_items_locked_gallery, mediaList.size), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val onUnlockMedia: (List<MediaImage>, (() -> Unit)?) -> Unit = { mediaList, onConfirmed ->
                    if (mediaList.isNotEmpty() && vaultProgress == null) {
                        val vaultItems = mediaList.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                        val galleryLockedIds = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }.map { it.id }
                        coroutineScope.launch {
                            var restoredCount = 0
                            var failedCount = 0
                            if (vaultItems.isNotEmpty()) {
                                val result = viewModel.restoreFromVault(vaultItems)
                                restoredCount += result.restored.size
                                failedCount += result.failed
                            }
                            if (galleryLockedIds.isNotEmpty()) {
                                viewModel.setLocked(galleryLockedIds, false)
                                restoredCount += galleryLockedIds.size
                            }
                            viewModel.refresh(showLoading = false)
                            if (restoredCount > 0) {
                                onConfirmed?.invoke()
                                Toast.makeText(context, context.getString(R.string.toast_vault_items_restored, restoredCount), Toast.LENGTH_SHORT).show()
                                if (failedCount > 0) {
                                    Toast.makeText(context, context.getString(R.string.toast_restore_failed_count, failedCount), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_vault_restore_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                val onRescanMedia: () -> Unit = {
                    Toast.makeText(context, R.string.toast_rescan_started, Toast.LENGTH_SHORT).show()
                    viewModel.rescanMedia {
                        Toast.makeText(context, R.string.toast_rescan_completed, Toast.LENGTH_SHORT).show()
                    }
                }
                val onDeleteFromLocked: (List<MediaImage>, () -> Unit) -> Unit = { mediaList, onDone ->
                    if (mediaList.isEmpty()) {
                        onDone()
                    } else if (mediaList.any { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) } && !ensureStorageWrite()) {
                        onDone()
                    } else {
                        val vaultItems = mediaList.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                        val galleryLockedItems = mediaList.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                        var pendingParts = 0
                        if (vaultItems.isNotEmpty()) pendingParts++
                        if (galleryLockedItems.isNotEmpty()) pendingParts++
                        val onPartConfirmed = {
                            pendingParts--
                            if (pendingParts <= 0) onDone()
                        }
                        if (vaultItems.isNotEmpty()) {
                            coroutineScope.launch {
                                val delIds = vaultItems.map { it.id }.toSet()
                                val delPaths = vaultItems.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                viewModel.deletePermanentlyFromVault(vaultItems)
                                viewModel.checkForChanges()
                                onPartConfirmed()
                            }
                        }
                        if (galleryLockedItems.isNotEmpty()) {
                            if (Build.VERSION.SDK_INT >= 30) runCatching {
                                val request = MediaStore.createDeleteRequest(context.contentResolver,
                                    galleryLockedItems.map { canonicalMediaUri(it) })
                                pendingPermanentDeleteMedia = galleryLockedItems
                                pendingPermanentDeleteCallback = onPartConfirmed
                                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                            }.onFailure {
                                pendingPermanentDeleteMedia = null
                                pendingPermanentDeleteCallback = null
                                Toast.makeText(context, "Could not request deletion", Toast.LENGTH_LONG).show()
                            }
                            else runCatching {
                                val delIds = galleryLockedItems.map { it.id }.toSet()
                                val delPaths = galleryLockedItems.map { it.path }.toSet()
                                viewModel.markMediaDeleted(delIds, delPaths)
                                galleryLockedItems.forEach { context.contentResolver.delete(canonicalMediaUri(it), null, null) }
                                viewModel.checkForChanges()
                                onPartConfirmed()
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
                        trashFeedback = trashFeedback,
                        onTrashFeedbackChange = { trashFeedback = it },
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
                        libraryOrder = libraryState.libraryOrder,
                        albumMediaSort = libraryState.albumMediaSort,
                        albumMediaSortOverrides = libraryState.albumMediaSortOverrides,
                        albumGroups = libraryState.albumGroups,
                        vaultAlbumSort = libraryState.vaultAlbumSort,
                        vaultMediaSort = libraryState.vaultMediaSort,
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
                        onSetLibraryOrder = viewModel::setLibraryOrder,
                        onSetAlbumMediaSort = viewModel::setAlbumMediaSort,
                        onSetAlbumMediaSortOverride = viewModel::setAlbumMediaSortOverride,
                        onSetVaultAlbumSort = viewModel::setVaultAlbumSort,
                        onSetVaultMediaSort = viewModel::setVaultMediaSort,
                        onCreateAlbumGroup = { name, ids -> viewModel.createAlbumGroup(name, ids) },
                        onRenameAlbumGroup = viewModel::renameAlbumGroup,
                        onSetAlbumGroupCover = viewModel::setAlbumGroupCover,
                        onDeleteAlbumGroup = viewModel::deleteAlbumGroup,
                        onSetAlbumGroupMembers = viewModel::setAlbumGroupMembers,
                        onAddAlbumToGroup = viewModel::addAlbumToGroup,
                        onRemoveAlbumFromGroup = viewModel::removeAlbumFromGroup,
                        onToggleShowHiddenItems = { show ->
                            if (show && Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                                settingsPreferences.setShowHiddenItems(true)
                                showHiddenAccessDialog = true
                            } else {
                                settingsPreferences.setShowHiddenItems(show)
                            }
                        },
                        excludedFolders = libraryState.excludedFolders,
                        onAddExcludedFolder = viewModel::addExcludedFolder,
                        onRemoveExcludedFolder = viewModel::removeExcludedFolder,
                        onRequestUnlock = {
                            if (!settings.biometricLockEnabled) {
                                lockedAuthorized = true
                            } else {
                                val keyguard = context.getSystemService(KeyguardManager::class.java)
                                if (keyguard == null || !keyguard.isDeviceSecure) {
                                    lockedAuthorized = true
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val title = context.getString(R.string.vault_unlock_title)
                                    val subtitle = context.getString(R.string.vault_unlock_subtitle)
                                    val launchedBiometric = runCatching {
                                        val builder = android.hardware.biometrics.BiometricPrompt.Builder(context)
                                            .setTitle(title)
                                            .setSubtitle(subtitle)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            builder.setAllowedAuthenticators(
                                                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                            )
                                        } else {
                                            @Suppress("DEPRECATION")
                                            builder.setDeviceCredentialAllowed(true)
                                        }
                                        val prompt = builder.build()
                                        isVaultAuthInProgress = true
                                        prompt.authenticate(
                                            android.os.CancellationSignal(),
                                            context.mainExecutor,
                                            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                                                    isVaultAuthInProgress = false
                                                    lockedAuthorized = true
                                                }
                                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                                    isVaultAuthInProgress = false
                                                }
                                            }
                                        )
                                        true
                                    }.getOrDefault(false)
                                    if (!launchedBiometric) {
                                        @Suppress("DEPRECATION")
                                        val intent = keyguard.createConfirmDeviceCredentialIntent(title, subtitle)
                                        if (intent == null) {
                                            lockedAuthorized = true
                                        } else {
                                            isVaultAuthInProgress = true
                                            unlockLauncher.launch(intent)
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val intent = keyguard.createConfirmDeviceCredentialIntent(
                                        context.getString(R.string.vault_unlock_title),
                                        context.getString(R.string.vault_unlock_subtitle)
                                    )
                                    if (intent == null) {
                                        lockedAuthorized = true
                                    } else {
                                        isVaultAuthInProgress = true
                                        unlockLauncher.launch(intent)
                                    }
                                }
                            }
                        },
                        onPick = onPick,
                        onTrash = trash@{ media, onConfirmed ->
                            val needsWrite = media.any { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                            if (needsWrite && !ensureStorageWrite()) return@trash
                            if (media.isNotEmpty()) {
                                val systemMedia = media.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                                val internalMedia = media.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                                if (settings.useSystemTrash && Build.VERSION.SDK_INT >= 30 && systemMedia.isNotEmpty()) {
                                    val uris = systemMedia.map { canonicalMediaUri(context, it) }
                                    val request = runCatching {
                                        MediaStore.createTrashRequest(context.contentResolver, uris, true)
                                    }.onFailure {
                                        android.util.Log.e("IrisTrash", "createTrashRequest failed for uris: $uris", it)
                                    }.getOrNull()

                                    if (request != null) {
                                        pendingSystemTrashMedia = systemMedia
                                        pendingSystemTrashCallback = onConfirmed
                                        pendingSystemTrashInternalMedia = internalMedia
                                        systemTrashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                    } else {
                                        // Fallback to in-app trash if platform trash request fails
                                        coroutineScope.launch {
                                            val moveResult = viewModel.moveToTrash(media)
                                            if (moveResult.trashedMedia.isNotEmpty()) {
                                                val alreadyDeletedTrash = moveResult.originalMedia.filter { it.id in moveResult.deletedOriginalIds }
                                                if (alreadyDeletedTrash.isNotEmpty()) {
                                                    viewModel.markMediaDeleted(alreadyDeletedTrash.map { it.id }.toSet(), alreadyDeletedTrash.map { it.path }.toSet())
                                                }
                                                val remainingTrashOriginals = moveResult.originalMedia.filter { it.id !in moveResult.deletedOriginalIds }
                                                if (moveResult.silentSuccess || remainingTrashOriginals.isEmpty()) {
                                                    val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                    val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                    viewModel.markMediaDeleted(delIds, delPaths)
                                                    viewModel.checkForChanges()
                                                    trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, moveResult.trashedMedia.size)
                                                    onConfirmed?.invoke()
                                                } else if (Build.VERSION.SDK_INT >= 30) {
                                                    runCatching {
                                                        pendingTrashMove = moveResult
                                                        pendingTrashCallback = onConfirmed
                                                        val delRequest = MediaStore.createDeleteRequest(
                                                            context.contentResolver,
                                                            remainingTrashOriginals.map { canonicalMediaUri(context, it) }
                                                        )
                                                        trashDeleteLauncher.launch(IntentSenderRequest.Builder(delRequest.intentSender).build())
                                                    }.onFailure {
                                                        val allDeleted = remainingTrashOriginals.all { item ->
                                                            runCatching {
                                                                context.contentResolver.delete(canonicalMediaUri(context, item), null, null) > 0 ||
                                                                java.io.File(item.path).delete() ||
                                                                (item.path.isNotBlank() && !java.io.File(item.path).exists())
                                                            }.getOrDefault(false)
                                                        }
                                                        if (allDeleted) {
                                                            val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                            val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                            viewModel.markMediaDeleted(delIds, delPaths)
                                                            viewModel.checkForChanges()
                                                            trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, moveResult.trashedMedia.size)
                                                            onConfirmed?.invoke()
                                                        } else {
                                                            pendingTrashMove = null
                                                            pendingTrashCallback = null
                                                            viewModel.rollbackTrashMove(trashRollbackItems(moveResult))
                                                            Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        val moveResult = viewModel.moveToTrash(media)
                                        if (moveResult.trashedMedia.isNotEmpty()) {
                                            val alreadyDeletedTrash = moveResult.originalMedia.filter { it.id in moveResult.deletedOriginalIds }
                                            if (alreadyDeletedTrash.isNotEmpty()) {
                                                viewModel.markMediaDeleted(alreadyDeletedTrash.map { it.id }.toSet(), alreadyDeletedTrash.map { it.path }.toSet())
                                            }
                                            val remainingTrashOriginals = moveResult.originalMedia.filter { it.id !in moveResult.deletedOriginalIds }
                                            if (moveResult.silentSuccess || remainingTrashOriginals.isEmpty()) {
                                                val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                viewModel.markMediaDeleted(delIds, delPaths)
                                                viewModel.checkForChanges()
                                                trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, moveResult.trashedMedia.size)
                                                onConfirmed?.invoke()
                                            } else if (Build.VERSION.SDK_INT >= 30) {
                                                runCatching {
                                                    pendingTrashMove = moveResult
                                                    pendingTrashCallback = onConfirmed
                                                    val request = MediaStore.createDeleteRequest(
                                                        context.contentResolver,
                                                        remainingTrashOriginals.map { canonicalMediaUri(context, it) }
                                                    )
                                                    trashDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                                }.onFailure {
                                                    val allDeleted = remainingTrashOriginals.all { item ->
                                                        runCatching {
                                                            context.contentResolver.delete(canonicalMediaUri(context, item), null, null) > 0 ||
                                                            java.io.File(item.path).delete() ||
                                                            (item.path.isNotBlank() && !java.io.File(item.path).exists())
                                                        }.getOrDefault(false)
                                                    }
                                                    if (allDeleted) {
                                                        val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                        val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                        viewModel.markMediaDeleted(delIds, delPaths)
                                                        viewModel.checkForChanges()
                                                        trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, moveResult.trashedMedia.size)
                                                        onConfirmed?.invoke()
                                                    } else {
                                                        pendingTrashMove = null
                                                        pendingTrashCallback = null
                                                        viewModel.rollbackTrashMove(trashRollbackItems(moveResult))
                                                        Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                val allDeleted = remainingTrashOriginals.all { item ->
                                                    runCatching {
                                                        context.contentResolver.delete(canonicalMediaUri(context, item), null, null) > 0 ||
                                                        java.io.File(item.path).delete() ||
                                                        (item.path.isNotBlank() && !java.io.File(item.path).exists())
                                                    }.getOrDefault(false)
                                                }
                                                if (allDeleted) {
                                                    val delIds = moveResult.originalMedia.map { it.id }.toSet()
                                                    val delPaths = moveResult.originalMedia.map { it.path }.toSet()
                                                    viewModel.markMediaDeleted(delIds, delPaths)
                                                    viewModel.checkForChanges()
                                                    trashFeedback = TrashFeedback(TrashFeedbackType.MOVED_TO_TRASH, moveResult.trashedMedia.size)
                                                    onConfirmed?.invoke()
                                                } else {
                                                    viewModel.rollbackTrashMove(trashRollbackItems(moveResult))
                                                    Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onRestore = { media, onConfirmed ->
                            if (media.isNotEmpty()) {
                                val systemMedia = media.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                                val internalMedia = media.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                                if (Build.VERSION.SDK_INT >= 30 && systemMedia.isNotEmpty()) {
                                    runCatching {
                                        pendingSystemRestoreMedia = systemMedia
                                        pendingSystemRestoreInternalMedia = internalMedia
                                        pendingSystemRestoreCallback = onConfirmed
                                        val uris = systemMedia.map { canonicalMediaUri(context, it) }
                                        val request = MediaStore.createTrashRequest(context.contentResolver, uris, false)
                                        systemRestoreLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                    }.onFailure {
                                        pendingSystemRestoreMedia = null
                                        pendingSystemRestoreInternalMedia = null
                                        pendingSystemRestoreCallback = null
                                        coroutineScope.launch {
                                            if (internalMedia.isNotEmpty()) {
                                                val internalResult = viewModel.restoreFromTrash(internalMedia)
                                                if (internalResult.restored.isNotEmpty()) {
                                                    trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, internalResult.restored.size)
                                                    onConfirmed?.invoke()
                                                }
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_restore_failed_count, systemMedia.size + internalResult.failed),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.toast_restore_failed_count, systemMedia.size),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        val result = viewModel.restoreFromTrash(media)
                                        if (result.restored.isNotEmpty()) {
                                            trashFeedback = TrashFeedback(TrashFeedbackType.RESTORED, result.restored.size)
                                            onConfirmed?.invoke()
                                        }
                                        if (result.failed > 0) {
                                            Toast.makeText(context, context.getString(R.string.toast_restore_failed_count, result.failed), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        onDeletePermanently = deletePermanently@{ media, onConfirmed ->
                            val needsWrite = media.any { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                            if (needsWrite && !ensureStorageWrite()) return@deletePermanently
                            if (media.isNotEmpty()) {
                                val internalItems = media.filter { it.id < 0 || it.path.startsWith(context.filesDir.absolutePath) }
                                val externalItems = media.filter { it.id > 0 && !it.path.startsWith(context.filesDir.absolutePath) }
                                coroutineScope.launch {
                                    if (externalItems.isEmpty()) {
                                        val delIds = internalItems.map { it.id }.toSet()
                                        val delPaths = internalItems.map { it.path }.toSet()
                                        viewModel.markMediaDeleted(delIds, delPaths)
                                        viewModel.deletePermanently(internalItems)
                                        trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, internalItems.size)
                                        onConfirmed?.invoke()
                                    } else if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                                        runCatching {
                                            val request = MediaStore.createDeleteRequest(
                                                context.contentResolver,
                                                externalItems.map { canonicalMediaUri(context, it) }
                                            )
                                            pendingPermanentDeleteMedia = externalItems
                                            pendingPermanentDeleteInternalMedia = internalItems.takeIf { it.isNotEmpty() }
                                            pendingPermanentDeleteCallback = onConfirmed
                                            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                        }.onFailure {
                                            val allDeleted = externalItems.all { item ->
                                                runCatching {
                                                    val uri = canonicalMediaUri(context, item)
                                                    val deleted = context.contentResolver.delete(
                                                        uri,
                                                        android.os.Bundle().apply {
                                                            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                                                        }
                                                    ) > 0
                                                    deleted || java.io.File(item.path).delete()
                                                }.getOrDefault(false)
                                            }
                                            if (allDeleted) {
                                                val allItems = internalItems + externalItems
                                                val delIds = allItems.map { it.id }.toSet()
                                                val delPaths = allItems.map { it.path }.toSet()
                                                viewModel.markMediaDeleted(delIds, delPaths)
                                                viewModel.deletePermanently(allItems)
                                                trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, allItems.size)
                                                onConfirmed?.invoke()
                                            } else {
                                                pendingPermanentDeleteMedia = null
                                                pendingPermanentDeleteInternalMedia = null
                                                pendingPermanentDeleteCallback = null
                                                Toast.makeText(context, context.getString(R.string.toast_could_not_request_removal), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        val allItems = internalItems + externalItems
                                        val delIds = allItems.map { it.id }.toSet()
                                        val delPaths = allItems.map { it.path }.toSet()
                                        viewModel.markMediaDeleted(delIds, delPaths)
                                        viewModel.deletePermanently(allItems)
                                        trashFeedback = TrashFeedback(TrashFeedbackType.PERMANENTLY_DELETED, allItems.size)
                                        onConfirmed?.invoke()
                                    }
                                }
                            }
                        },
                        onRescanMedia = onRescanMedia,
                        onEditMetadata = { media, request, callback ->
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, request.displayName)
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
                                put(MediaStore.Images.Media.DATE_TAKEN, request.dateTakenMillis)
                                put(MediaStore.Images.Media.ORIENTATION, request.orientation)
                            }

                            coroutineScope.launch {
                            val uri = canonicalMediaUri(context, media)
                            val directSaved = withContext(Dispatchers.IO) {
                                runCatching {
                                    saveExifToMedia(context, uri, media.path, request)
                                }.getOrDefault(false)
                            }

                            val mediaStoreUpdated = withContext(Dispatchers.IO) {
                                runCatching {
                                    if (media.id > 0 && uri.toString().startsWith("content://media/")) {
                                        context.contentResolver.update(uri, values, null, null) > 0
                                    } else false
                                }.getOrDefault(false)
                            }

                            if (directSaved || mediaStoreUpdated) {
                                if (media.path.isNotBlank() && !media.path.startsWith(context.filesDir.absolutePath)) {
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
                            }
                        },
                        duplicateState = duplicateState,
                        onScanDuplicates = viewModel::scanDuplicates,
                        onCancelDuplicateScan = viewModel::cancelDuplicateScan,
                        onMoveToAlbum = { media, dir, name, onComplete ->
                            coroutineScope.launch {
                                val firstResult = viewModel.moveMediaToAlbum(media, dir, name)
                                if (firstResult.failedMedia.isNotEmpty() && Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                                    val uris = firstResult.failedMedia.filter { it.id > 0 }.map { canonicalMediaUri(context, it) }
                                    if (uris.isNotEmpty()) {
                                        runCatching {
                                            val writeReq = MediaStore.createWriteRequest(context.contentResolver, uris)
                                            pendingMove = PendingMoveData(
                                                mediaList = firstResult.failedMedia,
                                                targetDir = dir,
                                                targetAlbumName = name,
                                                alreadyMoved = firstResult.movedMedia,
                                                callback = onComplete
                                            )
                                            moveLauncher.launch(IntentSenderRequest.Builder(writeReq.intentSender).build())
                                        }.onFailure { e ->
                                            android.util.Log.e("IrisGallery", "Failed to create write request for move", e)
                                            pendingMove = null
                                            onComplete(firstResult)
                                        }
                                    } else {
                                        onComplete(firstResult)
                                    }
                                } else {
                                    onComplete(firstResult)
                                }
                            }
                        },
                        onCopyToAlbum = { media, dir, name, onComplete ->
                            coroutineScope.launch {
                                val result = viewModel.copyMediaToAlbum(media, dir, name)
                                onComplete(result)
                            }
                        },
                        getAlbumDir = viewModel::getAlbumDirectory,
                        createAlbumDir = viewModel::createNewAlbumDirectory,
                        onRefresh = { viewModel.refresh(showLoading = true, forceClearCache = true, rescanStorage = true) },
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
                        initialOpenLatest = initialOpenLatest,
                        initialIntentGeneration = initialIntentGeneration,
                        onEnterVault = { viewModel.recoverVaultOrphans() },
                        onInvalidMedia = { ghost ->
                            viewModel.markMediaDeleted(setOf(ghost.id), setOf(ghost.path))
                            viewModel.purgeGhostMedia(ghost)
                        },
                    )
                }

                vaultProgress?.let { progress ->
                    AlertDialog(
                        onDismissRequest = { },
                        icon = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        title = {
                            Text(
                                stringResource(
                                    if (progress.isRestoring) R.string.action_remove_from_locked
                                    else R.string.action_move_to_locked
                                )
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (progress.total > 1) {
                                    LinearProgressIndicator(
                                        progress = { (progress.done.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                }
                                Text(
                                    text = stringResource(R.string.media_page_indicator, progress.done.coerceAtMost(progress.total), progress.total),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        confirmButton = { }
                    )
                }

                if (showHiddenAccessDialog) {
                    AlertDialog(
                        onDismissRequest = { showHiddenAccessDialog = false },
                        title = { Text(stringResource(R.string.hidden_items_permission_title)) },
                        text = { Text(stringResource(R.string.hidden_items_permission_desc)) },
                        confirmButton = {
                            Button(onClick = {
                                showHiddenAccessDialog = false
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
                            TextButton(onClick = { showHiddenAccessDialog = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showAllFilesAccessPromptDialog) {
                    val pending = pendingVaultItems
                    val cb = pendingVaultItemsCallback
                    AlertDialog(
                        onDismissRequest = {
                            showAllFilesAccessPromptDialog = false
                            pendingVaultItems = null
                            pendingVaultItemsCallback = null
                        },
                        icon = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text(stringResource(R.string.prompt_free_vault_title)) },
                        text = {
                            Text(stringResource(R.string.prompt_free_vault_desc))
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAllFilesAccessPromptDialog = false
                                awaitingAllFilesAccessGrant = true
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
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    showAllFilesAccessPromptDialog = false
                                    pendingVaultItems = null
                                    pendingVaultItemsCallback = null
                                    if (pending != null) {
                                        executeVaultMove(pending, cb)
                                    }
                                }) {
                                    Text(stringResource(R.string.action_use_system_popup))
                                }
                                TextButton(onClick = {
                                    showAllFilesAccessPromptDialog = false
                                    if (pending != null) {
                                        val lockedIds = pending.map { it.id }.toSet()
                                        viewModel.setLocked(lockedIds, true)
                                        viewModel.refresh(showLoading = false)
                                        cb?.invoke()
                                        Toast.makeText(context, context.getString(R.string.toast_vault_items_locked_gallery, pending.size), Toast.LENGTH_SHORT).show()
                                    }
                                    pendingVaultItems = null
                                    pendingVaultItemsCallback = null
                                }) {
                                    Text(stringResource(R.string.action_hide_in_iris))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.permission_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onGrant,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.permission_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
