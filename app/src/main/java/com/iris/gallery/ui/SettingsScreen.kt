package com.iris.gallery.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.iris.gallery.R
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.SUPPORTED_LANGUAGES
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.ui.LanguageSelectionBottomSheet
import com.iris.gallery.ui.setAppLanguage
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settings: SettingsState,
    preferences: SettingsPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun clearCache() {
        runCatching {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        }
        Toast.makeText(context, context.getString(R.string.toast_cache_cleared), Toast.LENGTH_SHORT).show()
    }

    val currentLang = remember(settings.language) {
        SUPPORTED_LANGUAGES.firstOrNull { it.code == settings.language }
            ?: SUPPORTED_LANGUAGES.first()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Language & Region
        item {
            SettingsSectionHeader(Icons.Outlined.Language, stringResource(R.string.settings_section_language))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${currentLang.flag} ${currentLang.nativeName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Section 1: Appearance & Theme
        item {
            SettingsSectionHeader(Icons.Outlined.Palette, stringResource(R.string.settings_section_appearance))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.themeMode == mode,
                                onClick = { preferences.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_amoled_black_title),
                        subtitle = stringResource(R.string.settings_amoled_black_desc),
                        checked = settings.amoledBlack,
                        onCheckedChange = { preferences.setAmoledBlack(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Material You dynamic color selector
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_material_you_title),
                            subtitle = stringResource(R.string.settings_material_you_desc),
                            checked = settings.accentColor == AccentColor.MATERIAL_YOU,
                            onCheckedChange = { isChecked ->
                                preferences.setAccentColor(if (isChecked) AccentColor.MATERIAL_YOU else AccentColor.IRIS)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Text(stringResource(R.string.settings_custom_accents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            AccentColor.IRIS to Color(0xFF8A4AF3),
                            AccentColor.LAPIS_MESOPOTAMIA to Color(0xFF1976D2),
                            AccentColor.EMERALD to Color(0xFF00897B),
                            AccentColor.ISHTAR_AMBER to Color(0xFFF57C00),
                            AccentColor.ROSE to Color(0xFFD81B60),
                        ).forEach { (accent, color) ->
                            val isSelected = settings.accentColor == accent
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { preferences.setAccentColor(accent) }
                                    .then(
                                        if (isSelected) Modifier.border(3.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Layout & Grid Customization
        item {
            SettingsSectionHeader(Icons.Outlined.GridView, stringResource(R.string.settings_section_layout))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_corner_style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SHARP,
                                onClick = { preferences.setCornerStyle(CornerStyle.SHARP) },
                                label = { Text(stringResource(R.string.settings_corner_sharp), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.CLASSIC,
                                onClick = { preferences.setCornerStyle(CornerStyle.CLASSIC) },
                                label = { Text(stringResource(R.string.settings_corner_subtle), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.ROUNDED,
                                onClick = { preferences.setCornerStyle(CornerStyle.ROUNDED) },
                                label = { Text(stringResource(R.string.settings_corner_rounded), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SQUIRCLE,
                                onClick = { preferences.setCornerStyle(CornerStyle.SQUIRCLE) },
                                label = { Text(stringResource(R.string.settings_corner_pill), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(stringResource(R.string.settings_grid_spacing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridSpacing.values().forEach { spacing ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.gridSpacing == spacing,
                                onClick = { preferences.setGridSpacing(spacing) },
                                label = {
                                    Text(
                                        when (spacing) {
                                            GridSpacing.COMPACT -> stringResource(R.string.settings_spacing_compact)
                                            GridSpacing.STANDARD -> stringResource(R.string.settings_spacing_standard)
                                            GridSpacing.RELAXED -> stringResource(R.string.settings_spacing_relaxed)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Photos Default Tile Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${settings.photoGridSize.roundToInt()} dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.photoGridSize,
                            onValueChange = { preferences.setPhotoGridSize(it) },
                            valueRange = 60f..220f
                        )
                    }

                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Albums Default Tile Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${settings.albumGridSize.roundToInt()} dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.albumGridSize,
                            onValueChange = { preferences.setAlbumGridSize(it) },
                            valueRange = 70f..340f
                        )
                    }
                }
            }
        }

        // Section 3: Media Badges & Timeline
        item {
            SettingsSectionHeader(Icons.Outlined.ViewDay, stringResource(R.string.settings_section_badges))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_timeline_title),
                        subtitle = stringResource(R.string.settings_show_timeline_desc),
                        checked = settings.showTimelineHeaders,
                        onCheckedChange = { preferences.setShowTimelineHeaders(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_badge_duration_title),
                        subtitle = stringResource(R.string.settings_badge_duration_desc),
                        checked = settings.showVideoDurationBadge,
                        onCheckedChange = { preferences.setShowVideoDurationBadge(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_badge_formats_title),
                        subtitle = stringResource(R.string.settings_badge_formats_desc),
                        checked = settings.showMediaFormatBadge,
                        onCheckedChange = { preferences.setShowMediaFormatBadge(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_album_count_title),
                        subtitle = stringResource(R.string.settings_album_count_desc),
                        checked = settings.showAlbumCount,
                        onCheckedChange = { preferences.setShowAlbumCount(it) }
                    )
                }
            }
        }

        // Section 4: Viewer & Playback
        item {
            SettingsSectionHeader(Icons.Outlined.PlayCircle, stringResource(R.string.settings_section_viewer))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_autoplay_video_title),
                        subtitle = stringResource(R.string.settings_autoplay_video_desc),
                        checked = settings.autoPlayVideo,
                        onCheckedChange = { preferences.setAutoPlayVideo(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_loop_video_title),
                        subtitle = stringResource(R.string.settings_loop_video_desc),
                        checked = settings.loopVideo,
                        onCheckedChange = { preferences.setLoopVideo(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(stringResource(R.string.settings_zoom_scale_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2.0f, 2.5f, 3.0f, 4.0f).forEach { zoom ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.doubleTapZoomLevel == zoom,
                                onClick = { preferences.setDoubleTapZoomLevel(zoom) },
                                label = { Text("${zoom}×", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Startup & Security
        item {
            SettingsSectionHeader(Icons.Outlined.Dashboard, stringResource(R.string.settings_section_behavior))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.settings_startup_tab), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.PHOTOS,
                                onClick = { preferences.setStartupTab(StartupTab.PHOTOS) },
                                label = { Text(stringResource(R.string.tab_photos), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.ALBUMS,
                                onClick = { preferences.setStartupTab(StartupTab.ALBUMS) },
                                label = { Text(stringResource(R.string.tab_albums), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.FAVORITES,
                                onClick = { preferences.setStartupTab(StartupTab.FAVORITES) },
                                label = { Text(stringResource(R.string.tab_favorites), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.LIBRARY,
                                onClick = { preferences.setStartupTab(StartupTab.LIBRARY) },
                                label = { Text(stringResource(R.string.tab_library), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(stringResource(R.string.settings_app_lock_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_app_lock_title),
                        subtitle = stringResource(R.string.settings_app_lock_desc),
                        checked = settings.appLockEnabled && settings.hasPin,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (settings.hasPin) {
                                    preferences.setAppLockEnabled(true)
                                } else {
                                    showSetPinDialog = true
                                }
                            } else {
                                if (settings.hasPin) {
                                    showDisablePinDialog = true
                                } else {
                                    preferences.setAppLockEnabled(false)
                                }
                            }
                        }
                    )

                    if (settings.hasPin) {
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.settings_change_pin))
                        }

                        if (isBiometricHardwareAvailable(context)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_biometric_unlock_title),
                                subtitle = stringResource(R.string.settings_biometric_unlock_desc),
                                checked = settings.appLockBiometricsEnabled,
                                onCheckedChange = { preferences.setAppLockBiometricsEnabled(it) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_hide_vault_title),
                        subtitle = stringResource(R.string.settings_hide_vault_desc),
                        checked = settings.vaultHideFromStorage,
                        onCheckedChange = { preferences.setVaultHideFromStorage(it) }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && settings.vaultHideFromStorage) {
                        val hasAllFiles = remember(context) { Environment.isExternalStorageManager() }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    stringResource(R.string.settings_silent_vault_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (hasAllFiles) stringResource(R.string.settings_silent_vault_desc_granted)
                                    else stringResource(R.string.settings_silent_vault_desc_prompt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (hasAllFiles) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        runCatching { context.startActivity(intent) }
                                            .onFailure {
                                                runCatching {
                                                    context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                                }
                                            }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(stringResource(R.string.action_grant), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_vault_biometric_title),
                        subtitle = stringResource(R.string.settings_vault_biometric_desc),
                        checked = settings.biometricLockEnabled,
                        onCheckedChange = { preferences.setBiometricLockEnabled(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_confirm_delete_title),
                        subtitle = stringResource(R.string.settings_confirm_delete_desc),
                        checked = settings.confirmDelete,
                        onCheckedChange = { preferences.setConfirmDelete(it) }
                    )
                }
            }
        }

        // Section 6: Cache & Reset
        item {
            SettingsSectionHeader(Icons.Outlined.CleaningServices, stringResource(R.string.settings_section_storage))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = ::clearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_cache))
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_reset_all))
                    }
                }
            }
        }
    }

    if (showSetPinDialog) {
        PinSetupBottomSheet(
            onDismiss = { showSetPinDialog = false },
            onPinConfirmed = { pin ->
                preferences.setPin(pin)
                showSetPinDialog = false
                Toast.makeText(context, context.getString(R.string.toast_pin_enabled), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showChangePinDialog) {
        PinChangeBottomSheet(
            onDismiss = { showChangePinDialog = false },
            onVerifyOldPin = { pin -> preferences.verifyPin(pin) },
            onNewPinConfirmed = { pin ->
                preferences.setPin(pin)
                showChangePinDialog = false
                Toast.makeText(context, context.getString(R.string.toast_pin_changed), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDisablePinDialog) {
        PinDisableBottomSheet(
            onDismiss = { showDisablePinDialog = false },
            onVerifyPin = { pin -> preferences.verifyPin(pin) },
            onSuccess = {
                preferences.removePin()
                showDisablePinDialog = false
                Toast.makeText(context, context.getString(R.string.toast_pin_disabled), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.resetToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, context.getString(R.string.toast_settings_reset), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionBottomSheet(
            currentLanguageCode = settings.language,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { code ->
                preferences.setLanguage(code)
                setAppLanguage(context, code)
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
