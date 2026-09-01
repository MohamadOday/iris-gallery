package com.iris.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iris.gallery.R
import com.iris.gallery.data.TimelineDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFormatBottomSheet(
    currentFormat: TimelineDateFormat,
    customPattern: String,
    showDayOfWeek: Boolean,
    onDismiss: () -> Unit,
    onFormatSelected: (TimelineDateFormat) -> Unit,
    onCustomPatternChange: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = rememberAppLocale()
    val sampleDatePast = remember { LocalDate.of(2024, 8, 31) }
    var showCustomPatternDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.settings_date_format_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_date_format_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimelineDateFormat.entries.toTypedArray()) { format ->
                    val isSelected = format == currentFormat
                    val isCustom = format == TimelineDateFormat.CUSTOM
                    val formatterOtherYear = remember(format, showDayOfWeek, locale, customPattern) {
                        getTimelineFormatter(
                            format = format,
                            isSameYear = false,
                            showDayOfWeek = showDayOfWeek,
                            locale = locale,
                            customPattern = customPattern
                        )
                    }
                    val previewStr = remember(sampleDatePast, formatterOtherYear) {
                        runCatching { sampleDatePast.format(formatterOtherYear) }.getOrDefault(customPattern)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isCustom) {
                                    onFormatSelected(TimelineDateFormat.CUSTOM)
                                    showCustomPatternDialog = true
                                } else {
                                    onFormatSelected(format)
                                    onDismiss()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(format.getDisplayNameRes()),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCustom) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = customPattern,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = previewStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isCustom) {
                                    IconButton(
                                        onClick = {
                                            onFormatSelected(TimelineDateFormat.CUSTOM)
                                            showCustomPatternDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (isCustom) {
                                            onFormatSelected(TimelineDateFormat.CUSTOM)
                                            showCustomPatternDialog = true
                                        } else {
                                            onFormatSelected(format)
                                            onDismiss()
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomPatternDialog) {
        CustomDateFormatDialog(
            initialPattern = customPattern,
            locale = locale,
            onDismiss = { showCustomPatternDialog = false },
            onConfirm = { pattern ->
                onCustomPatternChange(pattern)
                onFormatSelected(TimelineDateFormat.CUSTOM)
                showCustomPatternDialog = false
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomDateFormatDialog(
    initialPattern: String,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var patternText by remember { mutableStateOf(initialPattern) }
    val today = remember { LocalDate.now() }
    val sampleDate = remember { LocalDate.of(2024, 8, 31) }

    val isValid = remember(patternText, locale) {
        if (patternText.isBlank()) false
        else {
            runCatching {
                DateTimeFormatter.ofPattern(patternText, locale)
                true
            }.getOrDefault(false)
        }
    }

    val previewToday = remember(patternText, locale, isValid, today) {
        if (!isValid) ""
        else runCatching {
            today.format(DateTimeFormatter.ofPattern(patternText, locale))
        }.getOrDefault("")
    }

    val previewSample = remember(patternText, locale, isValid, sampleDate) {
        if (!isValid) ""
        else runCatching {
            sampleDate.format(DateTimeFormatter.ofPattern(patternText, locale))
        }.getOrDefault("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.settings_date_format_custom_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_date_format_custom_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = patternText,
                    onValueChange = { patternText = it },
                    label = { Text(stringResource(R.string.settings_date_format_custom_hint)) },
                    singleLine = true,
                    isError = !isValid && patternText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isValid && patternText.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.settings_date_format_custom_error),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Live Preview Card
                if (isValid && previewSample.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_date_format_custom_preview),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = previewSample,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (previewToday != previewSample) {
                                Text(
                                    text = previewToday,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Token Insert Chips
                Text(
                    text = stringResource(R.string.settings_date_format_custom_tokens),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tokens = listOf("d", "dd", "MM", "MMM", "MMMM", "yyyy", "yy", "EEEE", "EEE", ". ", " / ", " - ", ", ")
                    tokens.forEach { token ->
                        SuggestionChip(
                            onClick = { patternText += token },
                            label = { Text(token.trim().ifEmpty { token }, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Preset Suggestion Chips
                Text(
                    text = stringResource(R.string.settings_date_format_custom_presets),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "d. MMMM yyyy",
                        "d MMMM yyyy",
                        "dd.MM.yyyy",
                        "yyyy-MM-dd",
                        "dd/MM/yyyy",
                        "MMMM d, yyyy",
                    )
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { patternText = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (patternText == preset) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(patternText.trim()) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
