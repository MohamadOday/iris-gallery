@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.produceState
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.ExifMetadata
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.loadExifMetadata
import com.iris.gallery.ui.ExifEditorSheet
import com.iris.gallery.data.isRaw
import com.iris.gallery.data.isSvg
import com.iris.gallery.ui.ThumbnailCache
import androidx.compose.material.icons.outlined.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.ui.rememberAppLocale
import com.iris.gallery.ui.getTimelineFormatter
import com.iris.gallery.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PhotoDetailsSheet(
    image: MediaImage,
    pairedMedia: MediaImage? = null,
    onSwitchPaired: (() -> Unit)? = null,
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
    val exif by produceState<ExifMetadata?>(initialValue = null, image.id, image.uri, image.dateTaken, image.title, image.description, exifRevision) {
        value = withContext(Dispatchers.IO) {
            loadExifMetadata(context, image.uri, image.path)
        }
    }
    val currentExif = exif
    val videoHdrTransfer by produceState<String?>(initialValue = null, image.id, image.isVideo) {
        if (image.isVideo) {
            value = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, image.uri)
                    val transfer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
                    } else null
                    when (transfer) {
                        6 -> "HDR (HDR10)"
                        7 -> "HDR (HLG)"
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    retriever.release()
                }
            }
        } else {
            value = null
        }
    }
    var editing by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        dragOffset += delta
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (screenHeightDp * 0.75f).dp)
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
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
            val parsedCapturedDate = remember(currentExif?.dateTimeOriginal, currentExif?.offsetTimeOriginal, currentLocale, timelineDateFormat, customTimelineDateFormat) {
                val raw = currentExif?.dateTimeOriginal?.trim()
                if (raw.isNullOrBlank()) return@remember null
                val offset = currentExif?.offsetTimeOriginal?.trim()
                val tz = if (!offset.isNullOrBlank()) {
                    val prefix = if (offset.startsWith("+") || offset.startsWith("-")) "GMT" else "GMT+"
                    java.util.TimeZone.getTimeZone(prefix + offset)
                } else {
                    java.util.TimeZone.getDefault()
                }
                val dateMillis = runCatching {
                    val parser = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = tz
                    }
                    parser.parse(raw)?.time
                }.getOrNull() ?: return@remember null
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
            val hasOrigin = !parsedCapturedDate.isNullOrBlank() ||
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
                        if (!parsedCapturedDate.isNullOrBlank()) {
                            DetailItem(stringResource(R.string.details_captured), parsedCapturedDate)
                        }

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

            // 4. Video or Image Properties Card
            val isVideo = image.isVideo
            val formattedVideoCreatedDate = remember(image.dateTaken, currentLocale, timelineDateFormat, customTimelineDateFormat) {
                if (!isVideo || image.dateTaken <= 0) null else {
                    val tf = DateFormat.getTimeInstance(DateFormat.MEDIUM, currentLocale)
                    val timeStr = tf.format(Date(image.dateTaken))
                    val localDate = Instant.ofEpochMilli(image.dateTaken).atZone(ZoneId.systemDefault()).toLocalDate()
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
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                            if (isVideo) stringResource(R.string.details_video_properties) else stringResource(R.string.details_image_properties),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (isVideo) stringResource(R.string.details_video_properties) else stringResource(R.string.details_image_properties),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (formattedVideoCreatedDate != null) {
                        DetailItem(stringResource(R.string.details_captured), formattedVideoCreatedDate)
                    }
                    val mp = if (image.width > 0 && image.height > 0) (image.width * image.height) / 1_000_000.0 else 0.0
                    val resText = if (image.isSvg) {
                        if (image.width > 0 && image.height > 0) "${image.width} × ${image.height} (Vector)" else "Scalable Vector"
                    } else if (mp > 0) {
                        "${image.width} × ${image.height} (%.1f MP)".format(Locale.US, mp)
                    } else {
                        "${image.width} × ${image.height}"
                    }
                    DetailItem(stringResource(R.string.details_resolution), resText)
                    if (!currentExif?.imageUniqueId.isNullOrBlank()) {
                        DetailBlock(stringResource(R.string.details_image_unique_id), currentExif.imageUniqueId!!)
                    }
                    if (image.orientation != 0) DetailItem(stringResource(R.string.details_orientation), "${image.orientation}°")
                    if (isVideo && image.durationMs > 0) DetailItem(stringResource(R.string.details_duration), formatMediaDuration(image.durationMs))
                    if (!isVideo && (currentExif?.isUltraHdr == true || com.iris.gallery.ui.ThumbnailCache.isHdr(image.id) == true)) {
                        DetailItem(stringResource(R.string.details_dynamic_range), stringResource(R.string.details_ultra_hdr))
                    } else if (isVideo && videoHdrTransfer != null) {
                        DetailItem(stringResource(R.string.details_dynamic_range), videoHdrTransfer!!)
                    }
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
                    val addedMillis = remember(image.dateAdded) {
                        image.dateAdded.takeIf { it > 0 }
                    }
                    val formattedAddedDate = remember(addedMillis, currentLocale, timelineDateFormat, customTimelineDateFormat) {
                        if (addedMillis == null) null else {
                            val tf = DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale)
                            val timeStr = tf.format(Date(addedMillis))
                            val localDate = Instant.ofEpochMilli(addedMillis).atZone(ZoneId.systemDefault()).toLocalDate()
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
                    }
                    val modifiedMillis = remember(image.path, image.dateModified, image.dateTaken) {
                        if (image.dateModified > 0) {
                            image.dateModified
                        } else {
                            val f = File(image.path)
                            if (f.exists() && f.lastModified() > 0) f.lastModified() else image.dateTaken
                        }
                    }
                    val formattedModifiedDate = remember(modifiedMillis, currentLocale, timelineDateFormat, customTimelineDateFormat) {
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
                    if (formattedAddedDate != null) {
                        DetailItem(stringResource(R.string.details_added), formattedAddedDate)
                    }
                    DetailItem(stringResource(R.string.details_modified), formattedModifiedDate)
                    DetailItem(stringResource(R.string.details_type), image.mimeType.ifBlank { if (image.isVideo) stringResource(R.string.format_video) else stringResource(R.string.format_image) })
                    DetailItem(stringResource(R.string.details_size), formatFileSize(image.sizeBytes))
                    DetailBlock(stringResource(R.string.details_url), image.uri.toString())
                    DetailBlock(stringResource(R.string.details_path), image.path)
                }
            }

            if (pairedMedia != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Image,
                                    stringResource(R.string.details_paired_file),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(R.string.details_paired_file),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (onSwitchPaired != null) {
                                TextButton(onClick = onSwitchPaired) {
                                    val coverLabel = when {
                                        pairedMedia.name.endsWith(".heic", ignoreCase = true) ||
                                            pairedMedia.name.endsWith(".heif", ignoreCase = true) -> "HEIC"
                                        else -> null
                                    }
                                    Text(
                                        when {
                                            pairedMedia.isRaw -> stringResource(R.string.details_switch_to_raw)
                                            coverLabel != null -> stringResource(R.string.details_switch_to_format, coverLabel)
                                            else -> stringResource(R.string.details_switch_to_jpeg)
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        DetailItem(stringResource(R.string.details_file_name), pairedMedia.name)
                        if (pairedMedia.width > 0 && pairedMedia.height > 0) {
                            val pairedMp = (pairedMedia.width * pairedMedia.height) / 1_000_000.0
                            DetailItem(
                                stringResource(R.string.details_resolution),
                                "${pairedMedia.width} × ${pairedMedia.height} (%.1f MP)".format(Locale.US, pairedMp),
                            )
                        }
                        DetailItem(
                            stringResource(R.string.details_type),
                            pairedMedia.mimeType.ifBlank { if (pairedMedia.isRaw) "RAW" else stringResource(R.string.format_image) },
                        )
                        DetailItem(stringResource(R.string.details_size), formatFileSize(pairedMedia.sizeBytes))
                        DetailBlock(stringResource(R.string.details_path), pairedMedia.path)
                    }
                }
            }

            // Edit Metadata Action Button (AndroidX ExifInterface cannot write back into RAW/DNG files)
            if (!image.isVideo && !image.isRaw) {
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
    bytes >= 1_000_000_000 -> "%.1f GB".format(java.util.Locale.US, bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(java.util.Locale.US, bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(java.util.Locale.US, bytes / 1_000.0)
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
