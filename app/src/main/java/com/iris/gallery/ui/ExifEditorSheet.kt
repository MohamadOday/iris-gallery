package com.iris.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iris.gallery.R
import com.iris.gallery.data.ExifEditRequest
import com.iris.gallery.data.ExifMetadata
import com.iris.gallery.data.MediaImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifEditorSheet(
    image: MediaImage,
    exif: ExifMetadata?,
    onDismiss: () -> Unit,
    onSave: (ExifEditRequest) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showStripDialog by remember { mutableStateOf(false) }

    // File & MediaStore State
    var fileName by remember(image.id) { mutableStateOf(image.name) }
    val initialTitle = remember(image.id, image.title, image.name, exif) {
        val t = exif?.title?.takeIf { it.isNotBlank() } ?: image.title
        if (t.isBlank() || t == image.name || t == image.name.substringBeforeLast('.')) "" else t
    }
    var mediaTitle by remember(image.id, exif) { mutableStateOf(initialTitle) }
    var dateTakenStr by remember(image.id) { mutableStateOf(dateFormat.format(Date(image.dateTaken))) }
    var orientation by remember(image.id) { mutableIntStateOf((image.orientation % 360 + 360) % 360) }

    // Notes & Info State
    var userComment by remember(image.id, exif) { mutableStateOf(exif?.userComment ?: "") }
    var imageDescription by remember(image.id, exif) { mutableStateOf(exif?.imageDescription ?: image.description) }
    var artist by remember(image.id, exif) { mutableStateOf(exif?.artist ?: "") }
    var copyright by remember(image.id, exif) { mutableStateOf(exif?.copyright ?: "") }
    var software by remember(image.id, exif) { mutableStateOf(exif?.software ?: "") }

    // Camera & Lens State
    var cameraMake by remember(image.id, exif) { mutableStateOf(exif?.cameraMake ?: "") }
    var cameraModel by remember(image.id, exif) { mutableStateOf(exif?.cameraModel ?: "") }
    var lensModel by remember(image.id, exif) { mutableStateOf(exif?.lensModel ?: "") }
    var isoStr by remember(image.id, exif) { mutableStateOf(exif?.isoValue?.toString() ?: "") }
    var apertureStr by remember(image.id, exif) { mutableStateOf(exif?.apertureValue?.let { "%.1f".format(Locale.US, it).replace(".0", "") } ?: "") }
    var shutterSpeedStr by remember(image.id, exif) { mutableStateOf(exif?.shutterSpeed?.removeSuffix(" s") ?: "") }
    var focalLengthStr by remember(image.id, exif) { mutableStateOf(exif?.focalLengthValue?.let { "%.1f".format(Locale.US, it).replace(".0", "") } ?: "") }
    var whiteBalance by remember(image.id, exif) { mutableStateOf(exif?.whiteBalanceValue) }

    // Location State
    var latitudeStr by remember(image.id, exif) { mutableStateOf(exif?.latitude?.let { "%.6f".format(Locale.US, it) } ?: "") }
    var longitudeStr by remember(image.id, exif) { mutableStateOf(exif?.longitude?.let { "%.6f".format(Locale.US, it) } ?: "") }
    var removeGps by remember { mutableStateOf(false) }
    var stripAllExif by remember { mutableStateOf(false) }

    fun revertAll() {
        fileName = image.name
        val t = exif?.title?.takeIf { it.isNotBlank() } ?: image.title
        mediaTitle = if (t.isBlank() || t == image.name || t == image.name.substringBeforeLast('.')) "" else t
        dateTakenStr = dateFormat.format(Date(image.dateTaken))
        orientation = (image.orientation % 360 + 360) % 360

        userComment = exif?.userComment ?: ""
        imageDescription = exif?.imageDescription ?: image.description
        artist = exif?.artist ?: ""
        copyright = exif?.copyright ?: ""
        software = exif?.software ?: ""

        cameraMake = exif?.cameraMake ?: ""
        cameraModel = exif?.cameraModel ?: ""
        lensModel = exif?.lensModel ?: ""
        isoStr = exif?.isoValue?.toString() ?: ""
        apertureStr = exif?.apertureValue?.let { "%.1f".format(Locale.US, it).replace(".0", "") } ?: ""
        shutterSpeedStr = exif?.shutterSpeed?.removeSuffix(" s") ?: ""
        focalLengthStr = exif?.focalLengthValue?.let { "%.1f".format(Locale.US, it).replace(".0", "") } ?: ""
        whiteBalance = exif?.whiteBalanceValue

        latitudeStr = exif?.latitude?.let { "%.6f".format(Locale.US, it) } ?: ""
        longitudeStr = exif?.longitude?.let { "%.6f".format(Locale.US, it) } ?: ""
        removeGps = false
        stripAllExif = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.exif_editor_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        fileName.ifBlank { image.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Quick Action Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { showStripDialog = true },
                    label = { Text(stringResource(R.string.exif_strip_all)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Security,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (stripAllExif) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                )

                if (latitudeStr.isNotBlank() || longitudeStr.isNotBlank() || exif?.latitude != null) {
                    AssistChip(
                        onClick = {
                            removeGps = !removeGps
                            if (removeGps) {
                                latitudeStr = ""
                                longitudeStr = ""
                            }
                        },
                        label = { Text(stringResource(R.string.exif_remove_gps)) },
                        leadingIcon = {
                            Icon(
                                if (removeGps) Icons.Outlined.LocationOff else Icons.Outlined.LocationOn,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (removeGps) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (removeGps) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    )
                }

                AssistChip(
                    onClick = { revertAll() },
                    label = { Text(stringResource(R.string.exif_revert_changes)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Refresh,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            // Tabs Row
            val tabs = listOf(
                Pair(stringResource(R.string.exif_tab_notes), Icons.Outlined.Comment),
                Pair(stringResource(R.string.exif_tab_camera), Icons.Outlined.CameraAlt),
                Pair(stringResource(R.string.exif_tab_location), Icons.Outlined.LocationOn),
                Pair(stringResource(R.string.exif_tab_file), Icons.Outlined.Folder),
            )

            // Material You Tab Selector (Pill Filter Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    val isSelected = selectedTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                        ),
                    )
                }
            }

            // Scrollable Tab Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (selectedTab) {
                0 -> {
                    // Notes & Information
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = mediaTitle,
                            onValueChange = {
                                mediaTitle = it
                                val ext = image.name.substringAfterLast('.', "jpg")
                                fileName = if (it.isNotBlank()) "$it.$ext" else image.name
                            },
                            label = { Text(stringResource(R.string.details_edit_title)) },
                            placeholder = { Text(stringResource(R.string.details_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = userComment,
                            onValueChange = { userComment = it },
                            label = { Text(stringResource(R.string.exif_user_comment_label)) },
                            placeholder = { Text(stringResource(R.string.exif_user_comment_hint)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Comment, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = imageDescription,
                            onValueChange = { imageDescription = it },
                            label = { Text(stringResource(R.string.exif_image_desc_label)) },
                            placeholder = { Text(stringResource(R.string.exif_image_desc_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = artist,
                            onValueChange = { artist = it },
                            label = { Text(stringResource(R.string.exif_artist_label)) },
                            placeholder = { Text(stringResource(R.string.exif_artist_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = copyright,
                            onValueChange = { copyright = it },
                            label = { Text(stringResource(R.string.exif_copyright_label)) },
                            placeholder = { Text(stringResource(R.string.exif_copyright_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = software,
                            onValueChange = { software = it },
                            label = { Text(stringResource(R.string.exif_software_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
                1 -> {
                    // Camera & Lens
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cameraMake,
                                onValueChange = { cameraMake = it },
                                label = { Text(stringResource(R.string.exif_camera_make_label)) },
                                placeholder = { Text(stringResource(R.string.exif_camera_make_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                            OutlinedTextField(
                                value = cameraModel,
                                onValueChange = { cameraModel = it },
                                label = { Text(stringResource(R.string.exif_camera_model_label)) },
                                placeholder = { Text(stringResource(R.string.exif_camera_model_hint)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                        }

                        OutlinedTextField(
                            value = lensModel,
                            onValueChange = { lensModel = it },
                            label = { Text(stringResource(R.string.exif_lens_model_label)) },
                            placeholder = { Text(stringResource(R.string.exif_lens_model_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = isoStr,
                                onValueChange = { isoStr = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.exif_iso_label)) },
                                placeholder = { Text("100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                            OutlinedTextField(
                                value = apertureStr,
                                onValueChange = { apertureStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.exif_aperture_label)) },
                                placeholder = { Text("1.8") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = shutterSpeedStr,
                                onValueChange = { shutterSpeedStr = it },
                                label = { Text(stringResource(R.string.exif_shutter_label)) },
                                placeholder = { Text("1/250") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                            OutlinedTextField(
                                value = focalLengthStr,
                                onValueChange = { focalLengthStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.exif_focal_length_label)) },
                                placeholder = { Text("50.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.details_white_balance),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = whiteBalance == 0,
                                    onClick = { whiteBalance = if (whiteBalance == 0) null else 0 },
                                    label = { Text(stringResource(R.string.details_white_balance_auto)) },
                                )
                                FilterChip(
                                    selected = whiteBalance == 1,
                                    onClick = { whiteBalance = if (whiteBalance == 1) null else 1 },
                                    label = { Text(stringResource(R.string.details_white_balance_manual)) },
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Location (GPS)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.details_location), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    stringResource(R.string.exif_remove_gps_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = latitudeStr,
                            onValueChange = {
                                latitudeStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                                if (latitudeStr.isNotBlank()) removeGps = false
                            },
                            label = { Text(stringResource(R.string.exif_latitude_label)) },
                            placeholder = { Text("37.774929") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = longitudeStr,
                            onValueChange = {
                                longitudeStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                                if (longitudeStr.isNotBlank()) removeGps = false
                            },
                            label = { Text(stringResource(R.string.exif_longitude_label)) },
                            placeholder = { Text("-122.419416") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        if (latitudeStr.isNotBlank() || longitudeStr.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    latitudeStr = ""
                                    longitudeStr = ""
                                    removeGps = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.LocationOff, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.exif_remove_gps))
                            }
                        }
                    }
                }
                3 -> {
                    // File & Date
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = {
                                fileName = it
                                mediaTitle = it.substringBeforeLast('.')
                            },
                            label = { Text(stringResource(R.string.details_edit_filename)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = mediaTitle,
                            onValueChange = {
                                mediaTitle = it
                                val ext = image.name.substringAfterLast('.', "jpg")
                                fileName = if (it.isNotBlank()) "$it.$ext" else image.name
                            },
                            label = { Text(stringResource(R.string.details_edit_title)) },
                            placeholder = { Text(stringResource(R.string.details_title_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        OutlinedTextField(
                            value = dateTakenStr,
                            onValueChange = { dateTakenStr = it },
                            label = { Text(stringResource(R.string.details_edit_captured_hint)) },
                            trailingIcon = {
                                TextButton(onClick = { dateTakenStr = dateFormat.format(Date()) }) {
                                    Text(stringResource(R.string.exif_set_current_time), fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.details_orientation),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 90, 180, 270).forEach { deg ->
                                    FilterChip(
                                        selected = orientation == deg,
                                        onClick = { orientation = deg },
                                        label = { Text("$deg°") },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.details_edit_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

            // Fixed Sticky Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        val parsedTime = runCatching { dateFormat.parse(dateTakenStr)?.time }.getOrNull() ?: image.dateTaken
                        val parsedIso = isoStr.toIntOrNull()
                        val parsedAperture = apertureStr.toDoubleOrNull()
                        val parsedFocal = focalLengthStr.toDoubleOrNull()
                        val parsedLat = latitudeStr.toDoubleOrNull()
                        val parsedLng = longitudeStr.toDoubleOrNull()

                        val parsedExposure = if (shutterSpeedStr.contains("/")) {
                            val parts = shutterSpeedStr.split("/")
                            val num = parts.getOrNull(0)?.toDoubleOrNull()
                            val den = parts.getOrNull(1)?.toDoubleOrNull()
                            if (num != null && den != null && den > 0) num / den else null
                        } else {
                            shutterSpeedStr.toDoubleOrNull()
                        }

                        val customTitle = mediaTitle.trim()
                        val ext = (fileName.substringAfterLast('.', "").takeIf { it.isNotBlank() } ?: image.name.substringAfterLast('.', "jpg")).trim()
                        val effectiveDisplayName = if (fileName.trim().isNotBlank() && fileName.trim() != image.name) {
                            fileName.trim()
                        } else if (customTitle.isNotBlank()) {
                            if (customTitle.contains('.')) customTitle else "$customTitle.$ext"
                        } else {
                            image.name
                        }
                        val effectiveTitle = if (customTitle.isNotBlank()) {
                            customTitle.substringBeforeLast('.')
                        } else {
                            effectiveDisplayName.substringBeforeLast('.')
                        }
                        val effectiveDesc = imageDescription.trim().ifBlank { null }

                        val request = ExifEditRequest(
                            displayName = effectiveDisplayName,
                            title = effectiveTitle,
                            dateTakenMillis = parsedTime,
                            orientation = orientation,
                            userComment = userComment.trim().ifBlank { null },
                            imageDescription = effectiveDesc,
                            artist = artist.trim().ifBlank { null },
                            copyright = copyright.trim().ifBlank { null },
                            software = software.trim().ifBlank { null },
                            cameraMake = cameraMake.trim().ifBlank { null },
                            cameraModel = cameraModel.trim().ifBlank { null },
                            lensModel = lensModel.trim().ifBlank { null },
                            iso = parsedIso,
                            fNumber = parsedAperture,
                            exposureTime = parsedExposure,
                            focalLength = parsedFocal,
                            whiteBalance = whiteBalance,
                            latitude = parsedLat,
                            longitude = parsedLng,
                            removeGps = removeGps || (parsedLat == null && parsedLng == null && exif?.latitude != null),
                            stripAllExif = stripAllExif,
                        )
                        onSave(request)
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showStripDialog) {
        AlertDialog(
            onDismissRequest = { showStripDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.exif_strip_all_dialog_title)) },
            text = { Text(stringResource(R.string.exif_strip_all_dialog_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        stripAllExif = true
                        userComment = ""
                        imageDescription = ""
                        artist = ""
                        copyright = ""
                        software = ""
                        cameraMake = ""
                        cameraModel = ""
                        lensModel = ""
                        isoStr = ""
                        apertureStr = ""
                        shutterSpeedStr = ""
                        focalLengthStr = ""
                        whiteBalance = null
                        latitudeStr = ""
                        longitudeStr = ""
                        removeGps = true
                        showStripDialog = false
                    },
                ) {
                    Text(stringResource(R.string.exif_strip_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStripDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
