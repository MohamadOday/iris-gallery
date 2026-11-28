package com.iris.gallery.ui

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.iris.gallery.R
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val BUILT_IN_EDITOR_ID = "BUILT_IN"

sealed interface EditorChoiceOption {
    val id: String

    data object BuiltIn : EditorChoiceOption {
        override val id: String = BUILT_IN_EDITOR_ID
    }

    data class External(
        val component: ComponentName,
        val label: String,
        val icon: ImageBitmap?,
        val intent: Intent,
    ) : EditorChoiceOption {
        override val id: String = component.flattenToString()
    }
}

private val EDITOR_SHARE_BLACKLIST = setOf(
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

private val VIDEO_EDITOR_KEYWORDS = listOf(
    "edit", "cut", "shot", "video", "maker", "clip",
    "film", "movie", "vlog", "vn", "capcut", "inshot",
    "youcut", "kinemaster", "powerdirector", "motion",
)

fun resolveEditorLabel(context: Context, componentString: String): String? {
    if (componentString.isBlank()) return null
    val component = ComponentName.unflattenFromString(componentString) ?: return null
    val pm = context.packageManager
    return runCatching {
        pm.getActivityInfo(component, 0).loadLabel(pm).toString().takeIf { it.isNotBlank() }
    }.getOrNull() ?: runCatching {
        pm.getApplicationInfo(component.packageName, 0).loadLabel(pm).toString().takeIf { it.isNotBlank() }
    }.getOrNull()
}

private fun resolveExternalEditorUri(context: Context, image: MediaImage): android.net.Uri {
    val needsFileProvider = image.uri.scheme == "file" ||
        image.path.startsWith(context.filesDir.absolutePath)
    if (needsFileProvider && image.path.isNotBlank()) {
        val file = File(image.path)
        if (file.exists()) {
            return runCatching {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }.getOrDefault(image.uri)
        }
    }
    return image.uri
}

private fun isAllowedExternalEditorPackage(context: Context, pkg: String): Boolean {
    return pkg != context.packageName &&
        pkg !in EDITOR_SHARE_BLACKLIST &&
        !EDITOR_SHARE_BLACKLIST.any { bl -> pkg == bl || pkg.startsWith("$bl.") }
}

private fun queryExternalEditorMatches(
    context: Context,
    image: MediaImage,
): List<Pair<android.content.pm.ResolveInfo, Intent>> {
    val uri = resolveExternalEditorUri(context, image)
    val mimeType = image.mimeType.ifBlank {
        if (image.isVideo) "video/*" else "image/*"
    }
    val wildcardMime = if (image.isVideo) "video/*" else "image/*"
    val pm = context.packageManager

    val editIntent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
    }
    val genericEditIntent = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, wildcardMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
    }
    val cameraEditIntent = Intent("com.android.camera.action.EDITOR").apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "media", uri)
    }

    val seenPackages = HashSet<String>()
    val results = mutableListOf<Pair<android.content.pm.ResolveInfo, Intent>>()

    fun addMatches(baseIntent: Intent, extraFilter: ((android.content.pm.ResolveInfo) -> Boolean)? = null) {
        val matches = runCatching { pm.queryIntentActivities(baseIntent, 0) }.getOrDefault(emptyList())
        for (match in matches) {
            val pkg = match.activityInfo.packageName
            if (!isAllowedExternalEditorPackage(context, pkg)) continue
            if (extraFilter != null && !extraFilter(match)) continue
            if (!seenPackages.add(pkg)) continue
            val component = ComponentName(pkg, match.activityInfo.name)
            val targeted = Intent(baseIntent).apply {
                this.component = component
            }
            results.add(match to targeted)
        }
    }

    addMatches(editIntent)
    addMatches(genericEditIntent)
    addMatches(cameraEditIntent)

    if (image.isVideo) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        addMatches(sendIntent) { match ->
            val pkg = match.activityInfo.packageName.lowercase()
            val cls = match.activityInfo.name.lowercase()
            val label = runCatching { match.loadLabel(pm).toString().lowercase() }.getOrDefault("")
            (pkg == "com.google.android.apps.photos" && cls.contains("editvideo")) ||
                VIDEO_EDITOR_KEYWORDS.any { pkg.contains(it) || cls.contains(it) || label.contains(it) }
        }
    }

    return results
}

fun resolveExternalEditorOptions(context: Context, image: MediaImage): List<EditorChoiceOption.External> {
    val pm = context.packageManager
    return queryExternalEditorMatches(context, image).map { (match, targetedIntent) ->
        val pkg = match.activityInfo.packageName
        val component = targetedIntent.component ?: ComponentName(pkg, match.activityInfo.name)
        val label = runCatching { match.loadLabel(pm).toString() }.getOrDefault(pkg).ifBlank { pkg }
        val icon = runCatching {
            match.loadIcon(pm)?.toBitmap(96, 96)?.asImageBitmap()
        }.getOrNull()
        EditorChoiceOption.External(
            component = component,
            label = label,
            icon = icon,
            intent = targetedIntent,
        )
    }
}

fun launchTargetedEditorIntent(context: Context, targetedIntent: Intent): Boolean {
    val intent = Intent(targetedIntent).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

fun launchExternalEditorOption(context: Context, option: EditorChoiceOption.External): Boolean {
    return launchTargetedEditorIntent(context, option.intent)
}

fun findSpecificExternalEditorIntent(
    context: Context,
    image: MediaImage,
    componentString: String,
): Intent? {
    if (componentString.isBlank()) return null
    val savedComponent = ComponentName.unflattenFromString(componentString) ?: return null
    val matches = queryExternalEditorMatches(context, image)
    return matches.firstOrNull { (_, intent) ->
        intent.component?.flattenToString() == componentString
    }?.second ?: matches.firstOrNull { (_, intent) ->
        intent.component?.packageName == savedComponent.packageName
    }?.second
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditChoiceBottomSheet(
    image: MediaImage,
    lastUsedEditorId: String = "",
    includeBuiltIn: Boolean = !image.isVideo,
    onDismiss: () -> Unit,
    onChooseBuiltIn: (always: Boolean) -> Unit,
    onChooseExternal: (option: EditorChoiceOption.External, always: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val externalOptions by produceState<List<EditorChoiceOption.External>?>(
        initialValue = null,
        image.id,
        image.uri,
        image.mimeType,
        image.isVideo,
    ) {
        value = withContext(Dispatchers.IO) {
            resolveExternalEditorOptions(context, image)
        }
    }

    val orderedOptions = remember(externalOptions, includeBuiltIn, lastUsedEditorId) {
        val externals = externalOptions.orEmpty()
        val all = buildList<EditorChoiceOption> {
            if (includeBuiltIn) add(EditorChoiceOption.BuiltIn)
            addAll(externals.sortedBy { it.label.lowercase() })
        }
        if (lastUsedEditorId.isBlank()) {
            all
        } else {
            val savedPkg = ComponentName.unflattenFromString(lastUsedEditorId)?.packageName
            val top = all.firstOrNull { it.id == lastUsedEditorId }
                ?: if (savedPkg != null) {
                    all.firstOrNull { it is EditorChoiceOption.External && it.component.packageName == savedPkg }
                } else null
            if (top != null) {
                listOf(top) + all.filterNot { it.id == top.id }
            } else {
                all
            }
        }
    }

    val topIsLastUsed = remember(orderedOptions, lastUsedEditorId) {
        val first = orderedOptions.firstOrNull() ?: return@remember false
        if (lastUsedEditorId.isBlank()) return@remember false
        if (first.id == lastUsedEditorId) return@remember true
        val savedPkg = ComponentName.unflattenFromString(lastUsedEditorId)?.packageName
        savedPkg != null && first is EditorChoiceOption.External && first.component.packageName == savedPkg
    }

    var selectedId by remember(image.id) { mutableStateOf<String?>(null) }
    var userPicked by remember(image.id) { mutableStateOf(false) }
    LaunchedEffect(externalOptions, orderedOptions) {
        if (externalOptions == null) return@LaunchedEffect
        if (!userPicked || selectedId == null || orderedOptions.none { it.id == selectedId }) {
            selectedId = orderedOptions.firstOrNull()?.id
        }
    }

    val selectedOption = orderedOptions.firstOrNull { it.id == selectedId }

    fun confirmSelection(always: Boolean) {
        when (val chosen = selectedOption) {
            is EditorChoiceOption.BuiltIn -> onChooseBuiltIn(always)
            is EditorChoiceOption.External -> onChooseExternal(chosen, always)
            null -> Unit
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (image.isVideo) R.string.editor_choice_video_title else R.string.editor_choice_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (externalOptions == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else if (orderedOptions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_external_editor_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    orderedOptions.forEachIndexed { index, option ->
                        val isSelected = option.id == selectedId
                        val isLastUsed = index == 0 && topIsLastUsed
                        EditorPickerRow(
                            option = option,
                            selected = isSelected,
                            isLastUsed = isLastUsed,
                            onClick = {
                                userPicked = true
                                selectedId = option.id
                            },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { confirmSelection(always = false) },
                        enabled = selectedOption != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.editor_just_once))
                    }
                    Button(
                        onClick = { confirmSelection(always = true) },
                        enabled = selectedOption != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.editor_always))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorPickerRow(
    option: EditorChoiceOption,
    selected: Boolean,
    isLastUsed: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val border = if (selected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = border,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (option) {
                is EditorChoiceOption.BuiltIn -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF9D34F5), Color(0xFF4F16D8))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                is EditorChoiceOption.External -> {
                    val iconBitmap = option.icon
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = option.label,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option.label.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when (option) {
                            is EditorChoiceOption.BuiltIn -> stringResource(R.string.editor_builtin_title)
                            is EditorChoiceOption.External -> option.label
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isLastUsed) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.editor_last_used),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (option is EditorChoiceOption.BuiltIn) {
                    Text(
                        text = stringResource(R.string.editor_builtin_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
