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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iris.gallery.R
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.PreferredEditor
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.SUPPORTED_LANGUAGES
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.ui.LanguageSelectionBottomSheet
import com.iris.gallery.ui.setAppLanguage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    settings: SettingsState,
    preferences: SettingsPreferences,
    onOpenAbout: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.section_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
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
                onClick = { showLanguageDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
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
                        val langLabel = if (currentLang.code.isEmpty()) {
                            val sysLocale = java.util.Locale.getDefault()
                            "${currentLang.flag} ${stringResource(R.string.settings_language_system_default)} (${sysLocale.displayLanguage.replaceFirstChar { it.uppercase() }})"
                        } else {
                            "${currentLang.flag} ${currentLang.nativeName}"
                        }
                        Text(
                            text = langLabel,
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
                            AccentColor.IRIS to Brush.linearGradient(listOf(Color(0xFF8A4AF3), Color(0xFF6C5CE7))),
                            AccentColor.LAPIS_MESOPOTAMIA to Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF0288D1))),
                            AccentColor.EMERALD to Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF26A69A))),
                            AccentColor.ISHTAR_AMBER to Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFFFA726))),
                            AccentColor.ROSE to Brush.linearGradient(listOf(Color(0xFFD81B60), Color(0xFFEC407A))),
                            AccentColor.MONOCHROME to Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFFA1A1AA))),
                        ).forEach { (accent, brush) ->
                            val isSelected = settings.accentColor == accent
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .clickable { preferences.setAccentColor(accent) }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
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

                    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
                    val photoSpacingDp = settings.gridSpacing.dp.toFloat()
                    val photoAvailableWidth = (screenWidthDp - 2 * photoSpacingDp).coerceAtLeast(100f)

                    // Dynamic slider boundaries for Photos
                    val minPhotoDp = maxOf(36f, ((photoAvailableWidth + photoSpacingDp) / 6.8f - photoSpacingDp).coerceAtLeast(36f))
                    val maxPhotoDp = (photoAvailableWidth * 0.72f).coerceIn(160f, 320f)
                    val currentPhotoSize = settings.photoGridSize.coerceIn(minPhotoDp, maxPhotoDp)
                    val photoColumns = maxOf(1, ((photoAvailableWidth + photoSpacingDp) / (currentPhotoSize + photoSpacingDp + 0.001f)).toInt())

                    // Dynamic slider boundaries for Albums
                    val albumSpacingDp = 12f
                    val albumPaddingDp = 16f
                    val albumAvailableWidth = (screenWidthDp - 2 * albumPaddingDp).coerceAtLeast(100f)
                    val minAlbumDp = maxOf(48f, ((albumAvailableWidth + albumSpacingDp) / 4.8f - albumSpacingDp).coerceAtLeast(48f))
                    val maxAlbumDp = (albumAvailableWidth * 0.85f).coerceIn(200f, 420f)
                    val currentAlbumSize = settings.albumGridSize.coerceIn(minAlbumDp, maxAlbumDp)
                    val albumColumns = maxOf(1, ((albumAvailableWidth + albumSpacingDp) / (currentAlbumSize + albumSpacingDp + 0.001f)).toInt())

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    val colorScheme = MaterialTheme.colorScheme
                    val materialYouTiles = listOf(
                        Triple(
                            listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.35f)),
                            colorScheme.onPrimaryContainer,
                            Icons.Outlined.PhotoCamera
                        ),
                        Triple(
                            listOf(colorScheme.secondaryContainer, colorScheme.secondary.copy(alpha = 0.3f)),
                            colorScheme.onSecondaryContainer,
                            Icons.Outlined.Landscape
                        ),
                        Triple(
                            listOf(colorScheme.tertiaryContainer, colorScheme.tertiary.copy(alpha = 0.35f)),
                            colorScheme.onTertiaryContainer,
                            Icons.Outlined.Videocam
                        ),
                        Triple(
                            listOf(colorScheme.surfaceVariant, colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            colorScheme.onSurfaceVariant,
                            Icons.Outlined.AutoAwesome
                        ),
                        Triple(
                            listOf(colorScheme.inversePrimary.copy(alpha = 0.45f), colorScheme.primaryContainer),
                            colorScheme.primary,
                            Icons.Outlined.WbSunny
                        ),
                        Triple(
                            listOf(colorScheme.secondary.copy(alpha = 0.25f), colorScheme.tertiaryContainer),
                            colorScheme.onSecondaryContainer,
                            Icons.Outlined.Folder
                        ),
                        Triple(
                            listOf(colorScheme.tertiary.copy(alpha = 0.25f), colorScheme.primaryContainer),
                            colorScheme.onTertiaryContainer,
                            Icons.Outlined.Image
                        ),
                        Triple(
                            listOf(colorScheme.primary.copy(alpha = 0.2f), colorScheme.secondaryContainer),
                            colorScheme.onPrimaryContainer,
                            Icons.Outlined.PhotoAlbum
                        ),
                    )

                    // Photos Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_photos_tile_size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        // Scaled Live Photos Grid Mockup (Dynamic Height, Capped Max Height, Full Width Fill)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .animateContentSize(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            val spacing = settings.gridSpacing.dp.dp
                            val numRows = if (photoColumns <= 3) 1 else 2

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(spacing),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                for (row in 0 until numRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        for (col in 0 until photoColumns) {
                                            val index = (row * photoColumns + col) % materialYouTiles.size
                                            val (gradient, tint, icon) = materialYouTiles[index]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(settings.cornerStyle.dp.dp))
                                                    .background(Brush.linearGradient(gradient)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    icon,
                                                    contentDescription = null,
                                                    tint = tint.copy(alpha = 0.45f),
                                                    modifier = Modifier.size(
                                                        when {
                                                            photoColumns >= 6 -> 11.dp
                                                            photoColumns >= 5 -> 13.dp
                                                            photoColumns >= 4 -> 15.dp
                                                            photoColumns >= 3 -> 18.dp
                                                            else -> 26.dp
                                                        }
                                                    ),
                                                )
                                                if (index == 0 && settings.showVideoDurationBadge) {
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.65f),
                                                        shape = RoundedCornerShape(3.dp),
                                                        modifier = Modifier.align(Alignment.BottomStart).padding(if (photoColumns <= 2) 4.dp else 2.dp),
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                                        ) {
                                                            Icon(
                                                                Icons.Outlined.PlayArrow,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(if (photoColumns <= 2) 10.dp else 6.5.dp),
                                                            )
                                                            Text("0:24", color = Color.White, fontSize = if (photoColumns <= 2) 9.sp else 6.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                } else if (index == 1 && settings.showMediaFormatBadge) {
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.65f),
                                                        shape = RoundedCornerShape(3.dp),
                                                        modifier = Modifier.align(Alignment.BottomStart).padding(if (photoColumns <= 2) 4.dp else 2.dp),
                                                    ) {
                                                        Text("RAW", color = Color.White, fontSize = if (photoColumns <= 2) 9.sp else 6.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                    }
                                                } else if (index == 2) {
                                                    Icon(
                                                        Icons.Filled.Favorite,
                                                        contentDescription = null,
                                                        tint = tint.copy(alpha = 0.9f),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(if (photoColumns <= 2) 5.dp else 2.5.dp)
                                                            .size(if (photoColumns <= 2) 14.dp else 8.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Column Presets for Photos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(2, 3, 4, 5, 6).forEach { targetCols ->
                                val isCurrentCol = photoColumns == targetCols
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = isCurrentCol,
                                    onClick = {
                                        val idealDp = ((photoAvailableWidth + photoSpacingDp) / (targetCols + 0.45f) - photoSpacingDp).coerceIn(minPhotoDp, maxPhotoDp)
                                        preferences.setPhotoGridSize(idealDp)
                                    },
                                    label = {
                                        Text(
                                            "$targetCols",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentCol) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${currentPhotoSize.roundToInt()} dp · $photoColumns ${if (photoColumns == 1) "col" else "cols"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Slider(
                            value = currentPhotoSize,
                            onValueChange = { preferences.setPhotoGridSize(it) },
                            valueRange = minPhotoDp..maxPhotoDp,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Albums Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_albums_tile_size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        // Compact Scaled Live Albums Grid Mockup
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val sampleAlbums = listOf(
                                    Triple("Camera", listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.35f)), colorScheme.onPrimaryContainer),
                                    Triple("Screenshots", listOf(colorScheme.secondaryContainer, colorScheme.secondary.copy(alpha = 0.3f)), colorScheme.onSecondaryContainer),
                                    Triple("Downloads", listOf(colorScheme.tertiaryContainer, colorScheme.tertiary.copy(alpha = 0.35f)), colorScheme.onTertiaryContainer),
                                    Triple("Wallpapers", listOf(colorScheme.surfaceVariant, colorScheme.surfaceVariant.copy(alpha = 0.6f)), colorScheme.onSurfaceVariant),
                                )
                                val countShown = minOf(albumColumns, sampleAlbums.size)
                                for (i in 0 until countShown) {
                                    val (albumName, albumGrad, albumTint) = sampleAlbums[i]
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                                .clip(RoundedCornerShape(settings.cornerStyle.dp.dp))
                                                .background(Brush.linearGradient(albumGrad)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = albumTint.copy(alpha = 0.5f),
                                                modifier = Modifier.size(if (albumColumns >= 3) 14.dp else 20.dp),
                                            )
                                        }
                                        Text(
                                            albumName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = if (albumColumns >= 3) 8.sp else 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        // Column Presets for Albums
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(1, 2, 3, 4).forEach { targetCols ->
                                val isCurrentCol = albumColumns == targetCols
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = isCurrentCol,
                                    onClick = {
                                        val idealDp = ((albumAvailableWidth + albumSpacingDp) / (targetCols + 0.45f) - albumSpacingDp).coerceIn(minAlbumDp, maxAlbumDp)
                                        preferences.setAlbumGridSize(idealDp)
                                    },
                                    label = {
                                        Text(
                                            "$targetCols",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentCol) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    shape = CircleShape,
                                )
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${currentAlbumSize.roundToInt()} dp · $albumColumns ${if (albumColumns == 1) "col" else "cols"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Slider(
                            value = currentAlbumSize,
                            onValueChange = { preferences.setAlbumGridSize(it) },
                            valueRange = minAlbumDp..maxAlbumDp,
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

                    if (settings.showTimelineHeaders) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDateFormatDialog = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    stringResource(R.string.settings_date_format_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (settings.timelineDateFormat == TimelineDateFormat.CUSTOM) {
                                        "${stringResource(R.string.settings_date_format_custom)} (${settings.customTimelineDateFormat})"
                                    } else {
                                        stringResource(settings.timelineDateFormat.getDisplayNameRes())
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_relative_dates_title),
                            subtitle = stringResource(R.string.settings_relative_dates_desc),
                            checked = settings.useRelativeDates,
                            onCheckedChange = { preferences.setUseRelativeDates(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_show_day_of_week_title),
                            subtitle = stringResource(R.string.settings_show_day_of_week_desc),
                            checked = settings.showDayOfWeek,
                            onCheckedChange = { preferences.setShowDayOfWeek(it) }
                        )

                        if (settings.showDayOfWeek) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_abbreviate_day_title),
                                subtitle = stringResource(R.string.settings_abbreviate_day_desc),
                                checked = settings.abbreviateDayOfWeek,
                                onCheckedChange = { preferences.setAbbreviateDayOfWeek(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_smart_year_title),
                            subtitle = stringResource(R.string.settings_smart_year_desc),
                            checked = settings.smartYearHiding,
                            onCheckedChange = { preferences.setSmartYearHiding(it) }
                        )
                    }

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

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_user_comments_title),
                        subtitle = stringResource(R.string.settings_show_user_comments_desc),
                        checked = settings.showViewerUserComments,
                        onCheckedChange = { preferences.setShowViewerUserComments(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_show_filmstrip_title),
                        subtitle = stringResource(R.string.settings_show_filmstrip_desc),
                        checked = settings.showFilmstrip,
                        onCheckedChange = { preferences.setShowFilmstrip(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_pinch_to_rotate_title),
                        subtitle = stringResource(R.string.settings_pinch_to_rotate_desc),
                        checked = settings.pinchToRotate,
                        onCheckedChange = { preferences.setPinchToRotate(it) }
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

        // Section: Memories & Notifications
        item {
            SettingsSectionHeader(Icons.Outlined.Notifications, stringResource(R.string.settings_section_memories))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_memories_notifications),
                        subtitle = stringResource(R.string.setting_memories_notifications_desc),
                        checked = settings.memoriesNotificationEnabled,
                        onCheckedChange = { enabled ->
                            preferences.setMemoriesNotificationEnabled(enabled)
                            com.iris.gallery.MemoriesNotifications.schedule(
                                context = context,
                                enabled = enabled,
                                hour = settings.memoriesNotificationHour,
                                minute = settings.memoriesNotificationMinute
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = settings.memoriesNotificationEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))

                            val formattedTime = remember(settings.memoriesNotificationHour, settings.memoriesNotificationMinute) {
                                val cal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, settings.memoriesNotificationHour)
                                    set(java.util.Calendar.MINUTE, settings.memoriesNotificationMinute)
                                }
                                java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(cal.time)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(R.string.setting_memories_time),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.setting_memories_time_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { showTimePickerDialog = true }
                                ) {
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.settings_preferred_editor_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.settings_preferred_editor_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.ALWAYS_ASK,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.ALWAYS_ASK) },
                                label = { Text(stringResource(R.string.settings_editor_always_ask), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.BUILT_IN,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.BUILT_IN) },
                                label = { Text(stringResource(R.string.settings_editor_builtin), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.EXTERNAL,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.EXTERNAL) },
                                label = { Text(stringResource(R.string.settings_editor_external), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
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

        // Section 7: About Iris Gallery
        item {
            SettingsSectionHeader(Icons.Outlined.Info, stringResource(R.string.library_about_title))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                onClick = onOpenAbout,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            stringResource(R.string.library_about_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.library_about_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showDateFormatDialog) {
        DateFormatBottomSheet(
            currentFormat = settings.timelineDateFormat,
            customPattern = settings.customTimelineDateFormat,
            showDayOfWeek = settings.showDayOfWeek,
            onDismiss = { showDateFormatDialog = false },
            onFormatSelected = { format ->
                preferences.setTimelineDateFormat(format)
            },
            onCustomPatternChange = { pattern ->
                preferences.setCustomTimelineDateFormat(pattern)
            }
        )
    }

    if (showTimePickerDialog) {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val timePickerState = rememberTimePickerState(
            initialHour = settings.memoriesNotificationHour,
            initialMinute = settings.memoriesNotificationMinute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    preferences.setMemoriesNotificationTime(timePickerState.hour, timePickerState.minute)
                    com.iris.gallery.MemoriesNotifications.schedule(
                        context = context,
                        enabled = settings.memoriesNotificationEnabled,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    showTimePickerDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = { Text(stringResource(R.string.setting_memories_time)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
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
