package com.iris.gallery.ui

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
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "media", uri)
    }

    // 2. Generic EDIT intent with wildcard MIME (image/* or video/*)
    val genericEditIntent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, wildcardMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "media", uri)
    }

    // 3. SEND intent (vital for video editors & Google Photos video editing which don't register ACTION_EDIT)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "media", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // 4. Custom camera editor action (com.android.camera.action.EDITOR)
    val cameraEditIntent = Intent("com.android.camera.action.EDITOR").apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "media", uri)
    }

    // Query available activities (excluding ourselves)
    val editMatches = runCatching { pm.queryIntentActivities(editIntent, 0) }.getOrDefault(emptyList())
        .filter { it.activityInfo.packageName != context.packageName }
    val genericEditMatches = runCatching { pm.queryIntentActivities(genericEditIntent, 0) }.getOrDefault(emptyList())
        .filter { it.activityInfo.packageName != context.packageName }
    val cameraMatches = runCatching { pm.queryIntentActivities(cameraEditIntent, 0) }.getOrDefault(emptyList())
        .filter { it.activityInfo.packageName != context.packageName }
    val sendMatches = runCatching { pm.queryIntentActivities(sendIntent, 0) }.getOrDefault(emptyList())
        .filter { it.activityInfo.packageName != context.packageName }

    val chooserTitle = if (image.isVideo) {
        context.getString(R.string.edit_video_with_external_title)
    } else {
        context.getString(R.string.edit_with_external_title)
    }

    // Determine the base intent for chooser
    val baseIntent = when {
        editMatches.isNotEmpty() -> editIntent
        genericEditMatches.isNotEmpty() -> genericEditIntent
        cameraMatches.isNotEmpty() -> cameraEditIntent
        sendMatches.isNotEmpty() -> sendIntent
        else -> editIntent
    }

    // Extra initial intents to present other editing apps
    val extraIntents = mutableListOf<Intent>()
    if (baseIntent != editIntent && editMatches.isNotEmpty()) extraIntents.add(editIntent)
    if (baseIntent != cameraEditIntent && cameraMatches.isNotEmpty()) extraIntents.add(cameraEditIntent)
    if (image.isVideo && baseIntent != sendIntent && sendMatches.isNotEmpty()) extraIntents.add(sendIntent)

    val chooserIntent = Intent.createChooser(baseIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (extraIntents.isNotEmpty()) {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
        }
    }

    val hasAnyApp = editMatches.isNotEmpty() || genericEditMatches.isNotEmpty() || cameraMatches.isNotEmpty() || sendMatches.isNotEmpty()
    if (hasAnyApp) {
        val launched = runCatching {
            context.startActivity(chooserIntent)
            true
        }.getOrDefault(false)
        if (!launched) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.no_external_editor_found),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    } else {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.no_external_editor_found),
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
