package com.iris.gallery.ui

import android.os.Build
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.ThemeMode
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

    fun clearCache() {
        runCatching {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        }
        Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Appearance & Theme
        item {
            SettingsSectionHeader(Icons.Outlined.Palette, "Theme & Colors")
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
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
                        title = "AMOLED Pure Black",
                        subtitle = "Pitch-black background for OLED displays and maximum contrast",
                        checked = settings.amoledBlack,
                        onCheckedChange = { preferences.setAmoledBlack(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Material You dynamic color selector
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitchRow(
                            title = "Material You Dynamic Colors",
                            subtitle = "Extract accent colors automatically from your device wallpaper",
                            checked = settings.accentColor == AccentColor.MATERIAL_YOU,
                            onCheckedChange = { isChecked ->
                                preferences.setAccentColor(if (isChecked) AccentColor.MATERIAL_YOU else AccentColor.IRIS)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Text("Custom Accent Palettes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            SettingsSectionHeader(Icons.Outlined.GridView, "Layout & Grid Styling")
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Thumbnail Corner Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SHARP,
                                onClick = { preferences.setCornerStyle(CornerStyle.SHARP) },
                                label = { Text("Sharp (0dp)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.CLASSIC,
                                onClick = { preferences.setCornerStyle(CornerStyle.CLASSIC) },
                                label = { Text("Classic (4dp)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.ROUNDED,
                                onClick = { preferences.setCornerStyle(CornerStyle.ROUNDED) },
                                label = { Text("Rounded (12dp)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SQUIRCLE,
                                onClick = { preferences.setCornerStyle(CornerStyle.SQUIRCLE) },
                                label = { Text("Squircle (18dp)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text("Grid Spacing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                            GridSpacing.COMPACT -> "Compact (2dp)"
                                            GridSpacing.STANDARD -> "Standard (4dp)"
                                            GridSpacing.RELAXED -> "Relaxed (8dp)"
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
            SettingsSectionHeader(Icons.Outlined.ViewDay, "Media Display & Badges")
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = "Timeline Date Headers",
                        subtitle = "Group photos by day with interactive sticky date headers",
                        checked = settings.showTimelineHeaders,
                        onCheckedChange = { preferences.setShowTimelineHeaders(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = "Video Duration Badge",
                        subtitle = "Overlay length on video tiles in grid view",
                        checked = settings.showVideoDurationBadge,
                        onCheckedChange = { preferences.setShowVideoDurationBadge(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = "Special Format Badges",
                        subtitle = "Show indicator on GIF, RAW, panorama, and motion photos",
                        checked = settings.showMediaFormatBadge,
                        onCheckedChange = { preferences.setShowMediaFormatBadge(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = "Album Item Count",
                        subtitle = "Display item numbers under album names",
                        checked = settings.showAlbumCount,
                        onCheckedChange = { preferences.setShowAlbumCount(it) }
                    )
                }
            }
        }

        // Section 4: Viewer & Playback
        item {
            SettingsSectionHeader(Icons.Outlined.PlayCircle, "Viewer & Playback")
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = "Auto-Play Videos",
                        subtitle = "Start playing immediately when opening video in viewer",
                        checked = settings.autoPlayVideo,
                        onCheckedChange = { preferences.setAutoPlayVideo(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = "Loop Videos",
                        subtitle = "Repeat video playback continuously",
                        checked = settings.loopVideo,
                        onCheckedChange = { preferences.setLoopVideo(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text("Double-Tap Zoom Scale", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            SettingsSectionHeader(Icons.Outlined.Dashboard, "Behavior & Security")
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Default Startup Tab", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.PHOTOS,
                                onClick = { preferences.setStartupTab(StartupTab.PHOTOS) },
                                label = { Text("Photos", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.ALBUMS,
                                onClick = { preferences.setStartupTab(StartupTab.ALBUMS) },
                                label = { Text("Albums", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.FAVORITES,
                                onClick = { preferences.setStartupTab(StartupTab.FAVORITES) },
                                label = { Text("Favorites", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.LIBRARY,
                                onClick = { preferences.setStartupTab(StartupTab.LIBRARY) },
                                label = { Text("Library", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text("App Lock & Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    SettingsSwitchRow(
                        title = "Gallery App Lock",
                        subtitle = "Require custom PIN to open Iris Gallery and Image Picker",
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
                            Text("Change Gallery PIN")
                        }

                        if (isBiometricHardwareAvailable(context)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = "Fingerprint / Biometric Unlock",
                                subtitle = "Allow unlocking Iris Gallery with your fingerprint",
                                checked = settings.appLockBiometricsEnabled,
                                onCheckedChange = { preferences.setAppLockBiometricsEnabled(it) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = "Biometric Lock for Private Vault",
                        subtitle = "Require device credential/PIN when viewing locked vault items",
                        checked = settings.biometricLockEnabled,
                        onCheckedChange = { preferences.setBiometricLockEnabled(it) }
                    )
                }
            }
        }

        // Section 6: Cache & Reset
        item {
            SettingsSectionHeader(Icons.Outlined.CleaningServices, "Storage & Reset")
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
                        Text("Clear Image & Video Thumbnail Cache")
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset All Settings to Defaults")
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
                Toast.makeText(context, "Gallery PIN enabled", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Gallery PIN updated", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset settings?") },
            text = { Text("All your customization, theme preferences, and grid layouts will be restored to their factory defaults.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.resetToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
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
