package com.iris.gallery.ui

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.gallery.R
import com.iris.gallery.data.MediaImage

fun launchExternalEditor(context: Context, image: MediaImage) {
    val uri = if (image.path.startsWith(context.filesDir.absolutePath)) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(image.path)
        )
    } else {
        image.uri
    }
    val mimeType = image.mimeType.ifBlank {
        if (image.isVideo) "video/*" else "image/*"
    }
    val wildcardMime = if (image.isVideo) "video/*" else "image/*"

    val pm = context.packageManager

    // 1. Primary EDIT intent with specific MIME
    val editIntent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, mimeType)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    // 2. Generic EDIT intent with wildcard MIME (image/* or video/*)
    val genericEditIntent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, wildcardMime)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    // 3. Custom camera editor action (com.android.camera.action.EDITOR)
    val cameraEditIntent = Intent("com.android.camera.action.EDITOR").apply {
        setDataAndType(uri, mimeType)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    // Known generic sharing handlers that should never be shown in an editor chooser
    val shareBlacklist = setOf(
        "com.google.android.googlequicksearchbox", // Google Image Search / Lens
        "com.google.android.apps.lens",
        "com.google.android.apps.turbo",
        "com.google.android.apps.messaging",
        "com.google.android.gm",
        "com.google.android.apps.docs",
        "com.google.android.keep",
        "com.google.android.gms",
        "com.samsung.android.app.sharelive", // Quick Share
        "com.google.android.apps.sharing",
        "android",
        "com.android.bluetooth",
        "com.android.nfc",
    )

    val editorKeywords = listOf(
        "edit", "cut", "shot", "video", "maker", "clip",
        "film", "movie", "vlog", "vn", "capcut", "inshot",
        "youcut", "kinemaster", "powerdirector", "motion"
    )

    fun isAllowedEditor(pkg: String): Boolean {
        return pkg != context.packageName &&
            pkg !in shareBlacklist &&
            !shareBlacklist.any { bl -> pkg == bl || pkg.startsWith("$bl.") }
    }

    // Query all available activities
    val editAllMatches = runCatching { pm.queryIntentActivities(editIntent, 0) }.getOrDefault(emptyList())
    val genericEditAllMatches = runCatching { pm.queryIntentActivities(genericEditIntent, 0) }.getOrDefault(emptyList())
    val cameraEditAllMatches = runCatching { pm.queryIntentActivities(cameraEditIntent, 0) }.getOrDefault(emptyList())

    // For videos, external editors (CapCut, InShot, VN, YouCut, Google Photos EditVideoActivity, etc.)
    // rarely register ACTION_EDIT and instead register ACTION_SEND or dedicated editor activities.
    val specificVideoEditorSendIntent = if (image.isVideo) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
    } else {
        null
    }

    val specificVideoEditorSendAllMatches = if (specificVideoEditorSendIntent != null) {
        runCatching { pm.queryIntentActivities(specificVideoEditorSendIntent, 0) }.getOrDefault(emptyList())
    } else {
        emptyList()
    }

    val baseIntent = editIntent

    /*if (baseIntent == null) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.no_external_editor_found),
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }*/

    val alternateIntents = listOf(
        genericEditIntent,
        cameraEditIntent,
        specificVideoEditorSendIntent
    )

    val chooserTitle = if (image.isVideo) {
        context.getString(R.string.edit_video_with_external_title)
    } else {
        context.getString(R.string.edit_with_external_title)
    }

    val chooserIntent = Intent.createChooser(baseIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_ALTERNATE_INTENTS, alternateIntents.toTypedArray())
        putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, buildList {
            addAll(editAllMatches
                    .filterNot { isAllowedEditor(it.activityInfo.packageName) }
                    .map {
                        ComponentName(
                            it.activityInfo.packageName,
                            it.activityInfo.name
                        )
                    })
            addAll(genericEditAllMatches
                    .filterNot { isAllowedEditor(it.activityInfo.packageName) }
                    .map {
                        ComponentName(
                            it.activityInfo.packageName,
                            it.activityInfo.name
                        )
                    })
            addAll(cameraEditAllMatches
                    .filterNot { isAllowedEditor(it.activityInfo.packageName) }
                    .map {
                        ComponentName(
                            it.activityInfo.packageName,
                            it.activityInfo.name
                        )
                    })
            addAll(specificVideoEditorSendAllMatches
                    .filterNot { match ->
                        val pkg = match.activityInfo.packageName.lowercase()
                        val cls = match.activityInfo.name.lowercase()
                        val label = runCatching { match.loadLabel(pm).toString().lowercase() }.getOrDefault("")
                        isAllowedEditor(match.activityInfo.packageName) &&
                            ((pkg == "com.google.android.apps.photos"/* && match.activityInfo.name == "com.google.android.apps.photos.editor.intents.EditVideoActivity" && cls.contains("editvideo")*/) ||
                                    editorKeywords.any { pkg.contains(it) || cls.contains(it) || label.contains(it) })
                    }
                    .map {
                        ComponentName(
                            it.activityInfo.packageName,
                            it.activityInfo.name
                        )
                    })
        }.distinct().toTypedArray())
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    val launched = runCatching {
        context.startActivity(chooserIntent)
        true
    }.getOrDefault(false)

    if (!launched) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.external_editor_launch_failed),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

fun setAsWallpaper(context: Context, image: MediaImage) {
    val uri = if (image.path.startsWith(context.filesDir.absolutePath)) {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(image.path)
        )
    } else {
        image.uri
    }
    val mimeType = image.mimeType.ifBlank { "image/*" }

    val wallpaperManager = android.app.WallpaperManager.getInstance(context)
    val cropIntent = runCatching {
        wallpaperManager.getCropAndSetWallpaperIntent(uri).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", uri)
        }
    }.getOrNull()

    val attachIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("mimeType", mimeType)
        clipData = android.content.ClipData.newUri(context.contentResolver, "wallpaper", uri)
    }

    val launched = runCatching {
        if (cropIntent != null && cropIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(cropIntent)
            true
        } else {
            val chooser = Intent.createChooser(attachIntent, context.getString(R.string.action_set_as_wallpaper))
            context.startActivity(chooser)
            true
        }
    }.getOrElse {
        runCatching {
            val chooser = Intent.createChooser(attachIntent, context.getString(R.string.action_set_as_wallpaper))
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    if (!launched) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.action_set_as_wallpaper),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditChoiceBottomSheet(
    onDismiss: () -> Unit,
    onChooseBuiltIn: (rememberChoice: Boolean) -> Unit,
    onChooseExternal: (rememberChoice: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rememberChoice by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.editor_choice_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            // Built-in Iris Editor Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onChooseBuiltIn(rememberChoice) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF9D34F5), Color(0xFF4F16D8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.editor_builtin_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.editor_builtin_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // External Editor Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onChooseExternal(rememberChoice) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.editor_external_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.editor_external_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Remember choice checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberChoice = !rememberChoice }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = rememberChoice,
                    onCheckedChange = { rememberChoice = it }
                )
                Text(
                    text = stringResource(R.string.editor_remember_choice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
