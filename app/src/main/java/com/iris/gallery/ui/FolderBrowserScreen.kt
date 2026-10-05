package com.iris.gallery.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iris.gallery.R
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "dng")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm", "3gp", "avi", "flv", "ts")

data class FolderItem(
    val file: File,
    val isDirectory: Boolean,
    val mediaCount: Int = 0,
    val mediaImage: MediaImage? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    gridSpacing: GridSpacing = GridSpacing.STANDARD,
    timelineDateFormat: com.iris.gallery.data.TimelineDateFormat = com.iris.gallery.data.TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    favorites: Set<Long> = emptySet(),
    onOpenMedia: (MediaImage, List<MediaImage>) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val storageRoots = remember { getStorageRoots(context) }
    var currentDirectory by remember {
        mutableStateOf(
            storageRoots.firstOrNull() ?: Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        )
    }
    var isListView by rememberSaveable { mutableStateOf(false) }
    var folderItems by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isAtRoot = remember(currentDirectory, storageRoots) {
        storageRoots.any { it.absolutePath == currentDirectory.absolutePath } || currentDirectory.parentFile == null
    }

    BackHandler(enabled = true) {
        if (!isAtRoot && currentDirectory.parentFile != null) {
            currentDirectory = currentDirectory.parentFile!!
        } else {
            onBack()
        }
    }

    LaunchedEffect(currentDirectory) {
        isLoading = true
        folderItems = withContext(Dispatchers.IO) {
            loadDirectoryContents(currentDirectory)
        }
        isLoading = false
    }

    val directories = remember(folderItems) { folderItems.filter { it.isDirectory } }
    val mediaFiles = remember(folderItems) { folderItems.mapNotNull { it.mediaImage } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAtRoot) stringResource(R.string.section_folder_view) else currentDirectory.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentDirectory.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isAtRoot && currentDirectory.parentFile != null) {
                            currentDirectory = currentDirectory.parentFile!!
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(
                            if (isListView) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ViewList,
                            contentDescription = stringResource(if (isListView) R.string.view_grid else R.string.view_list)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Roots Selector if multiple roots available (e.g. Internal + SD card)
            if (storageRoots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(storageRoots) { root ->
                        val isSelected = currentDirectory.absolutePath.startsWith(root.absolutePath)
                        val isSd = root.absolutePath != "/storage/emulated/0" && !root.absolutePath.startsWith("/data")
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentDirectory = root },
                            leadingIcon = {
                                Icon(
                                    if (isSd) Icons.Outlined.SdCard else Icons.Outlined.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (isSd) stringResource(R.string.storage_sd_card) else stringResource(R.string.storage_internal))
                            }
                        )
                    }
                }
            }

            // Path Breadcrumb Navigation
            val pathSegments = remember(currentDirectory, storageRoots) {
                computeBreadcrumbs(currentDirectory, storageRoots)
            }
            if (pathSegments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(pathSegments) { segment ->
                        Text(
                            text = segment.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (segment.file == currentDirectory) FontWeight.Bold else FontWeight.Normal,
                            color = if (segment.file == currentDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { currentDirectory = segment.file }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        if (segment != pathSegments.last()) {
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (!isLoading && directories.isEmpty() && mediaFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            stringResource(R.string.folder_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isListView) {
                val currentLocale = rememberAppLocale()
                val formatItemDate: (Long) -> String = remember(timelineDateFormat, customTimelineDateFormat, currentLocale) {
                    { dateTakenMillis ->
                        val localDate = java.time.Instant.ofEpochMilli(dateTakenMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val formatter = getTimelineFormatter(
                            format = timelineDateFormat,
                            isSameYear = false,
                            showDayOfWeek = false,
                            locale = currentLocale,
                            customPattern = customTimelineDateFormat,
                            smartYearHiding = false
                        )
                        localDate.format(formatter)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Directories Section
                    if (directories.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.folder_section_folders, directories.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(directories, key = { it.file.absolutePath }) { item ->
                            Card(
                                shape = RoundedCornerShape(cornerStyle.dp.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentDirectory = item.file }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = item.file.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.folder_items_count, item.mediaCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // Media Files Section
                    if (mediaFiles.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.folder_section_media, mediaFiles.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(mediaFiles, key = { it.id }) { image ->
                            Card(
                                shape = RoundedCornerShape(cornerStyle.dp.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMedia(image, mediaFiles) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape((cornerStyle.dp.dp / 2).coerceAtLeast(4.dp)),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        MediaThumbnail(
                                            image = image,
                                            modifier = Modifier.fillMaxSize(),
                                            targetSizePx = 160,
                                            isFavorite = image.id in favorites
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = image.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (image.sizeBytes > 0) {
                                                Text(
                                                    text = formatBytes(image.sizeBytes),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            Text(
                                                text = formatItemDate(image.dateTaken),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (image.isVideo && image.durationMs > 0) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    text = formatDuration(image.durationMs),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Directories Section
                    if (directories.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.folder_section_folders, directories.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(directories, key = { it.file.absolutePath }) { item ->
                            Card(
                                shape = RoundedCornerShape(cornerStyle.dp.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentDirectory = item.file }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = item.file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.folder_items_count, item.mediaCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Media Files Section
                    if (mediaFiles.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.folder_section_media, mediaFiles.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(mediaFiles, key = { it.id }) { image ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clickable { onOpenMedia(image, mediaFiles) }
                            ) {
                                Card(
                                    shape = RoundedCornerShape(cornerStyle.dp.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    MediaThumbnail(
                                        image = image,
                                        modifier = Modifier.fillMaxSize(),
                                        targetSizePx = 256,
                                        isFavorite = image.id in favorites
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Breadcrumb(val name: String, val file: File)

private fun computeBreadcrumbs(current: File, roots: List<File>): List<Breadcrumb> {
    val matchingRoot = roots.firstOrNull { current.absolutePath.startsWith(it.absolutePath) }
    val list = mutableListOf<Breadcrumb>()
    var curr: File? = current
    while (curr != null) {
        val name = when {
            matchingRoot != null && curr.absolutePath == matchingRoot.absolutePath -> {
                if (curr.absolutePath.startsWith("/storage/emulated/0")) "Internal" else "SD Card"
            }
            curr.name.isNotBlank() -> curr.name
            else -> curr.path
        }
        list.add(0, Breadcrumb(name, curr))
        if (matchingRoot != null && curr.absolutePath == matchingRoot.absolutePath) {
            break
        }
        curr = curr.parentFile
    }
    return list
}

private fun getStorageRoots(context: Context): List<File> {
    val roots = mutableListOf<File>()
    val emulated = Environment.getExternalStorageDirectory()
    if (emulated != null && emulated.exists()) {
        roots.add(emulated)
    } else {
        roots.add(File("/storage/emulated/0"))
    }
    runCatching {
        ContextCompat.getExternalFilesDirs(context, null).forEach { dir ->
            if (dir != null) {
                val path = dir.absolutePath
                val rootPath = path.substringBefore("/Android/data")
                if (rootPath.isNotBlank() && rootPath != path) {
                    val root = File(rootPath)
                    if (root.exists() && root !in roots) {
                        roots.add(root)
                    }
                }
            }
        }
    }
    return roots
}

private fun loadDirectoryContents(directory: File): List<FolderItem> {
    if (!directory.exists() || !directory.canRead()) return emptyList()
    val files = directory.listFiles() ?: return emptyList()
    val items = mutableListOf<FolderItem>()

    for (file in files) {
        if (file.name.startsWith(".")) continue
        if (file.isDirectory) {
            val count = runCatching {
                file.listFiles()?.count { child ->
                    !child.name.startsWith(".") && (child.isDirectory || isMediaFile(child))
                } ?: 0
            }.getOrDefault(0)
            items.add(FolderItem(file = file, isDirectory = true, mediaCount = count))
        } else if (isMediaFile(file)) {
            val isVid = file.extension.lowercase(Locale.ROOT) in VIDEO_EXTENSIONS
            val media = MediaImage(
                id = file.hashCode().toLong(),
                uri = Uri.fromFile(file),
                name = file.name,
                dateTaken = file.lastModified(),
                width = 0,
                height = 0,
                path = file.absolutePath,
                bucketId = directory.hashCode().toLong(),
                bucketName = directory.name,
                isVideo = isVid,
                durationMs = 0L,
                mimeType = if (isVid) "video/*" else "image/*",
                sizeBytes = file.length(),
            )
            items.add(FolderItem(file = file, isDirectory = false, mediaImage = media))
        }
    }

    return items.sortedWith(compareByDescending<FolderItem> { it.isDirectory }.thenBy { it.file.name.lowercase(Locale.ROOT) })
}

private fun isMediaFile(file: File): Boolean {
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in IMAGE_EXTENSIONS || ext in VIDEO_EXTENSIONS
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, index.toDouble())
    return if (index == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", value, units[index])
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

