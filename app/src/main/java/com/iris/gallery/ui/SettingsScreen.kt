package com.iris.gallery.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.DocumentsContract
import java.io.File
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.iris.gallery.R
import com.iris.gallery.data.ALL_SELECTION_PILL_ACTIONS
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.DeleteMode
import com.iris.gallery.data.GridSpacing
import com.iris.gallery.data.NavBarStyle
import com.iris.gallery.data.NavLabelMode
import com.iris.gallery.data.PreferredEditor
import com.iris.gallery.data.SecureSharingMode
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.StartupTab
import com.iris.gallery.data.SUPPORTED_LANGUAGES
import com.iris.gallery.data.ThemeMode
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.data.ViewerHeaderStyle
import com.iris.gallery.data.getSystemDefaultLocale
import com.iris.gallery.ui.LanguageSelectionBottomSheet
import com.iris.gallery.ui.setAppLanguage
import kotlin.math.roundToInt

private enum class SettingsCategory {
    APPEARANCE,
    LAYOUT,
    TIMELINE_BADGES,
    VIEWER,
    NAVIGATION_BEHAVIOR,
    PRIVACY_SECURITY,
    MEMORIES,
    STORAGE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    settings: SettingsState,
    preferences: SettingsPreferences,
    excludedFolders: Set<String> = emptySet(),
    availableAlbums: List<MediaAlbum> = emptyList(),
    onRemoveExcludedFolder: (String) -> Unit = {},
    onAddExcludedFolder: (String) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onRescanMedia: () -> Unit = {},
    onToggleShowHiddenItems: (Boolean) -> Unit = { preferences.setShowHiddenItems(it) },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showViewerHeaderDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }
    var showSecureSharingDialog by remember { mutableStateOf(false) }
    var showSelectionPillCustomizer by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearching) {
        isSearching = false
        searchQuery = ""
    }
    BackHandler(enabled = !isSearching && activeCategory != null) {
        activeCategory = null
    }

    val searchTokens = remember(searchQuery) {
        searchQuery.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    fun matches(vararg texts: String): Boolean {
        if (searchTokens.isEmpty()) return true
        return searchTokens.all { token -> texts.any { it.contains(token, ignoreCase = true) } }
    }

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
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_settings_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = stringResource(R.string.clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        val headerTitleRes = when (activeCategory) {
                            SettingsCategory.APPEARANCE -> R.string.settings_section_appearance
                            SettingsCategory.LAYOUT -> R.string.settings_section_layout
                            SettingsCategory.TIMELINE_BADGES -> R.string.settings_section_badges
                            SettingsCategory.VIEWER -> R.string.settings_section_viewer
                            SettingsCategory.NAVIGATION_BEHAVIOR -> R.string.settings_section_nav_behavior
                            SettingsCategory.PRIVACY_SECURITY -> R.string.settings_section_privacy_security
                            SettingsCategory.MEMORIES -> R.string.settings_section_memories
                            SettingsCategory.STORAGE -> R.string.settings_section_storage
                            null -> R.string.section_settings
                        }
                        Text(
                            text = stringResource(headerTitleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (activeCategory != null) {
                            activeCategory = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Outlined.Search, stringResource(R.string.action_search))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { scaffoldPadding ->
        val languageSectionTitle = stringResource(R.string.settings_section_language)
        val languageTitle = stringResource(R.string.settings_language_title)
        val languageSysDefault = stringResource(R.string.settings_language_system_default)
        val langLabel = if (currentLang.code.isEmpty()) {
            val sysLocale = getSystemDefaultLocale(context)
            "${currentLang.flag} $languageSysDefault (${sysLocale.getDisplayLanguage(sysLocale).replaceFirstChar { it.uppercase() }})"
        } else {
            "${currentLang.flag} ${currentLang.nativeName}"
        }
        val showLanguageSection = matches(languageSectionTitle) || matches(languageTitle, langLabel)

        val appearanceSectionTitle = stringResource(R.string.settings_section_appearance)
        val appearanceSectionMatch = matches(appearanceSectionTitle)
        val themeModeTitle = stringResource(R.string.settings_theme_mode)
        val themeSystemStr = stringResource(R.string.settings_theme_system)
        val themeLightStr = stringResource(R.string.settings_theme_light)
        val themeDarkStr = stringResource(R.string.settings_theme_dark)
        val showThemeMode = appearanceSectionMatch || matches(themeModeTitle, themeSystemStr, themeLightStr, themeDarkStr)
        val amoledTitle = stringResource(R.string.settings_amoled_black_title)
        val amoledDesc = stringResource(R.string.settings_amoled_black_desc)
        val showAmoled = appearanceSectionMatch || matches(amoledTitle, amoledDesc)
        val materialYouTitle = stringResource(R.string.settings_material_you_title)
        val materialYouDesc = stringResource(R.string.settings_material_you_desc)
        val showMaterialYou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (appearanceSectionMatch || matches(materialYouTitle, materialYouDesc))
        val customAccentsTitle = stringResource(R.string.settings_custom_accents)
        val showCustomAccents = appearanceSectionMatch || matches(customAccentsTitle)

        val layoutSectionTitle = stringResource(R.string.settings_section_layout)
        val layoutSectionMatch = matches(layoutSectionTitle)
        val gridLayoutTitle = stringResource(R.string.settings_grid_layout_title)
        val gridLayoutMosaic = stringResource(R.string.settings_grid_layout_mosaic)
        val gridLayoutSquare = stringResource(R.string.settings_grid_layout_square)
        val showGridLayout = layoutSectionMatch || matches(gridLayoutTitle, gridLayoutMosaic, gridLayoutSquare)
        val animateGifsTitle = stringResource(R.string.settings_animate_gif_thumbs_title)
        val animateGifsDesc = stringResource(R.string.settings_animate_gif_thumbs_desc)
        val showAnimateGifs = layoutSectionMatch || matches(animateGifsTitle, animateGifsDesc)
        val cornerStyleTitle = stringResource(R.string.settings_corner_style)
        val cornerSharp = stringResource(R.string.settings_corner_sharp)
        val cornerSubtle = stringResource(R.string.settings_corner_subtle)
        val cornerRounded = stringResource(R.string.settings_corner_rounded)
        val cornerPill = stringResource(R.string.settings_corner_pill)
        val showCornerStyle = layoutSectionMatch || matches(cornerStyleTitle, cornerSharp, cornerSubtle, cornerRounded, cornerPill)
        val gridSpacingTitle = stringResource(R.string.settings_grid_spacing)
        val spacingCompact = stringResource(R.string.settings_spacing_compact)
        val spacingStandard = stringResource(R.string.settings_spacing_standard)
        val spacingRelaxed = stringResource(R.string.settings_spacing_relaxed)
        val showGridSpacing = layoutSectionMatch || matches(gridSpacingTitle, spacingCompact, spacingStandard, spacingRelaxed)
        val photosTileTitle = stringResource(R.string.settings_photos_tile_size)
        val showPhotosTileSize = settings.photoGridLayout == com.iris.gallery.data.PhotoGridLayout.GRID &&
            (layoutSectionMatch || matches(photosTileTitle))
        val albumsTileTitle = stringResource(R.string.settings_albums_tile_size)
        val showAlbumsTileSize = layoutSectionMatch || matches(albumsTileTitle)

        val badgesSectionTitle = stringResource(R.string.settings_section_badges)
        val badgesSectionMatch = matches(badgesSectionTitle)
        val timelineTitle = stringResource(R.string.settings_show_timeline_title)
        val timelineDesc = stringResource(R.string.settings_show_timeline_desc)
        val dateFormatTitle = stringResource(R.string.settings_date_format_title)
        val relativeDatesTitle = stringResource(R.string.settings_relative_dates_title)
        val relativeDatesDesc = stringResource(R.string.settings_relative_dates_desc)
        val dayOfWeekTitle = stringResource(R.string.settings_show_day_of_week_title)
        val dayOfWeekDesc = stringResource(R.string.settings_show_day_of_week_desc)
        val abbrevDayTitle = stringResource(R.string.settings_abbreviate_day_title)
        val abbrevDayDesc = stringResource(R.string.settings_abbreviate_day_desc)
        val smartYearTitle = stringResource(R.string.settings_smart_year_title)
        val smartYearDesc = stringResource(R.string.settings_smart_year_desc)
        val showTimeline = badgesSectionMatch || matches(
            timelineTitle, timelineDesc,
            dateFormatTitle, relativeDatesTitle, relativeDatesDesc,
            dayOfWeekTitle, dayOfWeekDesc, abbrevDayTitle, abbrevDayDesc,
            smartYearTitle, smartYearDesc
        )
        val badgeDurationTitle = stringResource(R.string.settings_badge_duration_title)
        val badgeDurationDesc = stringResource(R.string.settings_badge_duration_desc)
        val showBadgeDuration = badgesSectionMatch || matches(badgeDurationTitle, badgeDurationDesc)
        val badgeFormatsTitle = stringResource(R.string.settings_badge_formats_title)
        val badgeFormatsDesc = stringResource(R.string.settings_badge_formats_desc)
        val showBadgeFormats = badgesSectionMatch || matches(badgeFormatsTitle, badgeFormatsDesc)
        val albumCountTitle = stringResource(R.string.settings_album_count_title)
        val albumCountDesc = stringResource(R.string.settings_album_count_desc)
        val showAlbumCount = badgesSectionMatch || matches(albumCountTitle, albumCountDesc)
        val albumLabelsTitle = stringResource(R.string.settings_album_labels_title)
        val albumLabelsDesc = stringResource(R.string.settings_album_labels_desc)
        val showAlbumLabels = badgesSectionMatch || matches(albumLabelsTitle, albumLabelsDesc)

        val viewerSectionTitle = stringResource(R.string.settings_section_viewer)
        val viewerSectionMatch = matches(viewerSectionTitle)
        val viewerHeaderTitle = stringResource(R.string.settings_viewer_header_title)
        val viewerHeaderStyleLabel = stringResource(settings.viewerHeaderStyle.getDisplayNameRes())
        val showViewerHeader = viewerSectionMatch || matches(viewerHeaderTitle, viewerHeaderStyleLabel)
        val autoplayTitle = stringResource(R.string.settings_autoplay_video_title)
        val autoplayDesc = stringResource(R.string.settings_autoplay_video_desc)
        val showAutoplay = viewerSectionMatch || matches(autoplayTitle, autoplayDesc)
        val loopTitle = stringResource(R.string.settings_loop_video_title)
        val loopDesc = stringResource(R.string.settings_loop_video_desc)
        val showLoop = viewerSectionMatch || matches(loopTitle, loopDesc)
        val videoDoubleTapTitle = stringResource(R.string.settings_video_double_tap_zoom_title)
        val videoDoubleTapDesc = stringResource(R.string.settings_video_double_tap_zoom_desc)
        val showVideoDoubleTap = viewerSectionMatch || matches(videoDoubleTapTitle, videoDoubleTapDesc)
        val videoGesturesTitle = stringResource(R.string.settings_video_gesture_controls_title)
        val videoGesturesDesc = stringResource(R.string.settings_video_gesture_controls_desc)
        val showVideoGestures = viewerSectionMatch || matches(videoGesturesTitle, videoGesturesDesc)
        val userCommentsTitle = stringResource(R.string.settings_show_user_comments_title)
        val userCommentsDesc = stringResource(R.string.settings_show_user_comments_desc)
        val showUserComments = viewerSectionMatch || matches(userCommentsTitle, userCommentsDesc)
        val filmstripTitle = stringResource(R.string.settings_show_filmstrip_title)
        val filmstripDesc = stringResource(R.string.settings_show_filmstrip_desc)
        val showFilmstrip = viewerSectionMatch || matches(filmstripTitle, filmstripDesc)
        val pinchRotateTitle = stringResource(R.string.settings_pinch_to_rotate_title)
        val pinchRotateDesc = stringResource(R.string.settings_pinch_to_rotate_desc)
        val showPinchRotate = viewerSectionMatch || matches(pinchRotateTitle, pinchRotateDesc)
        val imageRotateBtnTitle = stringResource(R.string.settings_image_rotate_button_title)
        val imageRotateBtnDesc = stringResource(R.string.settings_image_rotate_button_desc)
        val showImageRotateBtn = viewerSectionMatch || matches(imageRotateBtnTitle, imageRotateBtnDesc)
        val multiStageZoomTitle = stringResource(R.string.settings_multi_stage_zoom_title)
        val multiStageZoomDesc = stringResource(R.string.settings_multi_stage_zoom_desc)
        val zoomScaleTitle = stringResource(R.string.settings_zoom_scale_title)
        val showMultiStageZoom = viewerSectionMatch || matches(multiStageZoomTitle, multiStageZoomDesc, zoomScaleTitle)

        val memoriesSectionTitle = stringResource(R.string.settings_section_memories)
        val memoriesSectionMatch = matches(memoriesSectionTitle)
        val memoriesNotifTitle = stringResource(R.string.setting_memories_notifications)
        val memoriesNotifDesc = stringResource(R.string.setting_memories_notifications_desc)
        val memoriesTimeTitle = stringResource(R.string.setting_memories_time)
        val memoriesTimeDesc = stringResource(R.string.setting_memories_time_desc)
        val showMemories = memoriesSectionMatch || matches(memoriesNotifTitle, memoriesNotifDesc, memoriesTimeTitle, memoriesTimeDesc)

        val behaviorSectionTitle = stringResource(R.string.settings_section_nav_behavior)
        val privacySectionTitle = stringResource(R.string.settings_section_privacy_security)
        val legacyBehaviorSectionTitle = stringResource(R.string.settings_section_behavior)
        val behaviorSectionMatch = matches(behaviorSectionTitle, legacyBehaviorSectionTitle)
        val privacySectionMatch = matches(privacySectionTitle, legacyBehaviorSectionTitle)
        val navTabsTitle = stringResource(R.string.settings_navigation_tabs_title)
        val navTabsDesc = stringResource(R.string.settings_navigation_tabs_desc)
        val tabMediaStr = stringResource(R.string.tab_media)
        val tabAlbumsStr = stringResource(R.string.tab_albums)
        val tabFavoritesStr = stringResource(R.string.tab_favorites)
        val tabLibraryStr = stringResource(R.string.tab_library)
        val tabVaultStr = stringResource(R.string.tab_vault)
        val showNavTabs = behaviorSectionMatch || matches(navTabsTitle, navTabsDesc, tabMediaStr, tabAlbumsStr, tabFavoritesStr, tabLibraryStr, tabVaultStr)
        val tiltNavIconsTitle = stringResource(R.string.settings_tilt_nav_icons_title)
        val tiltNavIconsDesc = stringResource(R.string.settings_tilt_nav_icons_desc)
        val showTiltNavIcons = behaviorSectionMatch || matches(tiltNavIconsTitle, tiltNavIconsDesc)
        val navBarStyleTitle = stringResource(R.string.settings_nav_bar_style_title)
        val navBarDockedStr = stringResource(R.string.settings_nav_bar_docked)
        val navBarFloatingStr = stringResource(R.string.settings_nav_bar_floating)
        val showNavBarStyle = behaviorSectionMatch || matches(navBarStyleTitle, navBarDockedStr, navBarFloatingStr)
        val navLabelModeTitle = stringResource(R.string.settings_nav_label_mode_title)
        val navLabelAlwaysStr = stringResource(R.string.settings_nav_label_always)
        val navLabelSelectedStr = stringResource(R.string.settings_nav_label_selected)
        val navLabelHiddenStr = stringResource(R.string.settings_nav_label_hidden)
        val showNavLabelMode = behaviorSectionMatch || matches(navLabelModeTitle, navLabelAlwaysStr, navLabelSelectedStr, navLabelHiddenStr)
        val floatingSelectionTitle = stringResource(R.string.settings_floating_selection_toolbar_title)
        val floatingSelectionDesc = stringResource(R.string.settings_floating_selection_toolbar_desc)
        val selectionPillTitle = stringResource(R.string.settings_selection_pill_title)
        val selectionPillDesc = stringResource(R.string.settings_selection_pill_desc)
        val showSelectionPillSetting = behaviorSectionMatch || matches(floatingSelectionTitle, floatingSelectionDesc, selectionPillTitle, selectionPillDesc)
        val albumTopTitle = stringResource(R.string.settings_album_top_section_title)
        val albumFiltersTitle = stringResource(R.string.settings_album_filters_title)
        val albumFiltersDesc = stringResource(R.string.settings_album_filters_desc)
        val showAlbumFilters = behaviorSectionMatch || matches(albumTopTitle, albumFiltersTitle, albumFiltersDesc)
        val globalAlbumSearchTitle = stringResource(R.string.settings_global_album_search_title)
        val globalAlbumSearchDesc = stringResource(R.string.settings_global_album_search_desc)
        val showGlobalAlbumSearch = behaviorSectionMatch || matches(globalAlbumSearchTitle, globalAlbumSearchDesc)
        val showHiddenItemsTitle = stringResource(R.string.settings_show_hidden_items_title)
        val showHiddenItemsDesc = stringResource(R.string.settings_show_hidden_items_desc)
        val showHiddenItemsToggle = behaviorSectionMatch || matches(showHiddenItemsTitle, showHiddenItemsDesc)
        val startupTabTitle = stringResource(R.string.settings_startup_tab)
        val showStartupTab = behaviorSectionMatch || matches(startupTabTitle, tabMediaStr, tabAlbumsStr, tabFavoritesStr, tabLibraryStr, tabVaultStr)
        val appLockTitle = stringResource(R.string.settings_app_lock_title)
        val appLockDesc = stringResource(R.string.settings_app_lock_desc)
        val changePinStr = stringResource(R.string.settings_change_pin)
        val bioUnlockTitle = stringResource(R.string.settings_biometric_unlock_title)
        val bioUnlockDesc = stringResource(R.string.settings_biometric_unlock_desc)
        val showAppLock = privacySectionMatch || matches(appLockTitle, appLockDesc, changePinStr, bioUnlockTitle, bioUnlockDesc)
        val hideVaultTitle = stringResource(R.string.settings_hide_vault_title)
        val hideVaultDesc = stringResource(R.string.settings_hide_vault_desc)
        val showHideVault = privacySectionMatch || matches(hideVaultTitle, hideVaultDesc)
        val storageAccessTitle = stringResource(R.string.settings_storage_access_title)
        val storageAccessDescGranted = stringResource(R.string.settings_storage_access_desc_granted)
        val storageAccessDescPrompt = stringResource(R.string.settings_storage_access_desc_prompt)
        val showStorageAccess = Build.VERSION.SDK_INT <= 29 &&
            (privacySectionMatch || matches(storageAccessTitle, storageAccessDescGranted, storageAccessDescPrompt))
        val silentVaultTitle = stringResource(R.string.settings_silent_vault_title)
        val silentVaultDescGranted = stringResource(R.string.settings_silent_vault_desc_granted)
        val silentVaultDescPrompt = stringResource(R.string.settings_silent_vault_desc_prompt)
        val showSilentVault = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && settings.vaultHideFromStorage &&
            (privacySectionMatch || matches(silentVaultTitle, silentVaultDescGranted, silentVaultDescPrompt))
        val vaultBioTitle = stringResource(R.string.settings_vault_biometric_title)
        val vaultBioDesc = stringResource(R.string.settings_vault_biometric_desc)
        val showVaultBio = privacySectionMatch || matches(vaultBioTitle, vaultBioDesc)
        val confirmDeleteTitle = stringResource(R.string.settings_confirm_delete_title)
        val confirmDeleteDesc = stringResource(R.string.settings_confirm_delete_desc)
        val showConfirmDelete = privacySectionMatch || matches(confirmDeleteTitle, confirmDeleteDesc)
        val deleteModeTitle = stringResource(R.string.settings_delete_mode_title)
        val deleteModeDesc = stringResource(R.string.settings_delete_mode_desc)
        val deleteModeTrash = stringResource(R.string.settings_delete_mode_trash)
        val deleteModePerm = stringResource(R.string.settings_delete_mode_permanent)
        val deleteModeAsk = stringResource(R.string.settings_delete_mode_always_ask)
        val showDeleteMode = privacySectionMatch || matches(deleteModeTitle, deleteModeDesc, deleteModeTrash, deleteModePerm, deleteModeAsk)
        val systemTrashTitle = stringResource(R.string.settings_system_trash_title)
        val systemTrashDesc = stringResource(R.string.settings_system_trash_desc)
        val showSystemTrash = Build.VERSION.SDK_INT >= 30 &&
            (privacySectionMatch || matches(systemTrashTitle, systemTrashDesc))
        val prefEditorTitle = stringResource(R.string.settings_preferred_editor_title)
        val prefEditorDesc = stringResource(R.string.settings_preferred_editor_desc)
        val editorAsk = stringResource(R.string.settings_editor_always_ask)
        val editorBuiltin = stringResource(R.string.settings_editor_builtin)
        val editorExternal = stringResource(R.string.settings_editor_external)
        val showPreferredEditor = behaviorSectionMatch || matches(prefEditorTitle, prefEditorDesc, editorAsk, editorBuiltin, editorExternal)
        val overwriteTitle = stringResource(R.string.settings_editor_overwrite_title)
        val overwriteDesc = stringResource(R.string.settings_editor_overwrite_desc)
        val showOverwrite = behaviorSectionMatch || matches(overwriteTitle, overwriteDesc)
        val secureSharingTitle = stringResource(R.string.settings_secure_sharing_title)
        val secureSharingModeTitle = stringResource(settings.secureSharingMode.getTitleRes())
        val secureSharingModeDesc = stringResource(settings.secureSharingMode.getDescriptionRes())
        val showSecureSharing = privacySectionMatch || matches(secureSharingTitle, secureSharingModeTitle, secureSharingModeDesc)

        val storageSectionTitle = stringResource(R.string.settings_section_storage)
        val storageSectionMatch = matches(storageSectionTitle)
        val rescanMediaLabel = stringResource(R.string.settings_rescan_media)
        val excludedFoldersLabel = stringResource(R.string.excluded_folders_title)
        val clearCacheLabel = stringResource(R.string.settings_clear_cache)
        val resetAllLabel = stringResource(R.string.settings_reset_all)
        val showRescanMedia = storageSectionMatch || matches(rescanMediaLabel)
        val showExcludedFoldersBtn = storageSectionMatch || matches(excludedFoldersLabel)
        val showClearCache = storageSectionMatch || matches(clearCacheLabel)
        val showResetAll = storageSectionMatch || matches(resetAllLabel)

        val aboutTitle = stringResource(R.string.library_about_title)
        val aboutSubtitle = stringResource(R.string.library_about_subtitle)
        val showAboutSection = matches(aboutTitle, aboutSubtitle)

        val hasAnyAppearance = showThemeMode || showAmoled || showMaterialYou || showCustomAccents
        val hasAnyLayout = showGridLayout || showAnimateGifs || showCornerStyle || showGridSpacing || showPhotosTileSize || showAlbumsTileSize
        val hasAnyBadges = showTimeline || showBadgeDuration || showBadgeFormats || showAlbumCount || showAlbumLabels
        val hasAnyViewer = showViewerHeader || showAutoplay || showLoop || showVideoDoubleTap ||
            showVideoGestures || showUserComments || showFilmstrip || showPinchRotate || showImageRotateBtn || showMultiStageZoom
        val hasAnyNavBehavior = showNavTabs || showTiltNavIcons || showNavBarStyle || showNavLabelMode ||
            showSelectionPillSetting || showAlbumFilters || showGlobalAlbumSearch || showHiddenItemsToggle ||
            showStartupTab || showPreferredEditor || showOverwrite
        val hasAnyPrivacySecurity = showAppLock || showHideVault || showStorageAccess || showSilentVault ||
            showVaultBio || showConfirmDelete || showDeleteMode || showSystemTrash || showSecureSharing
        val hasAnyStorage = showRescanMedia || showExcludedFoldersBtn || showClearCache || showResetAll
        val hasAnyMatches = showLanguageSection || hasAnyAppearance || hasAnyLayout ||
            hasAnyBadges || hasAnyViewer || showMemories || hasAnyNavBehavior ||
            hasAnyPrivacySecurity || hasAnyStorage || showAboutSection

        val hubAppearanceDesc = stringResource(R.string.settings_hub_appearance_desc)
        val hubLayoutDesc = stringResource(R.string.settings_hub_layout_desc)
        val hubBadgesDesc = stringResource(R.string.settings_hub_badges_desc)
        val hubViewerDesc = stringResource(R.string.settings_hub_viewer_desc)
        val hubNavBehaviorDesc = stringResource(R.string.settings_hub_nav_behavior_desc)
        val hubPrivacyDesc = stringResource(R.string.settings_hub_privacy_desc)
        val hubMemoriesDesc = stringResource(R.string.settings_hub_memories_desc)
        val hubStorageDesc = stringResource(R.string.settings_hub_storage_desc)

        val showAllForSearch = searchTokens.isNotEmpty()
        val currentViewKey = if (showAllForSearch) "search" else activeCategory?.name ?: "hub"

        AnimatedContent(
            targetState = currentViewKey,
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            transitionSpec = {
                when {
                    targetState == "search" || initialState == "search" -> {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                    }
                    targetState == "hub" -> {
                        (fadeIn(tween(180)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 8 }) togetherWith
                            (fadeOut(tween(140)) + slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 8 })
                    }
                    else -> {
                        (fadeIn(tween(180)) + slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 8 }) togetherWith
                            (fadeOut(tween(140)) + slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 8 })
                    }
                }
            },
            label = "settings_navigation"
        ) { viewKey ->
            if (viewKey == "hub") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "hub_visual") {
                        SettingsHubGroupCard(
                            listOf(
                                SettingsHubEntry(Icons.Outlined.Palette, appearanceSectionTitle, hubAppearanceDesc) {
                                    activeCategory = SettingsCategory.APPEARANCE
                                },
                                SettingsHubEntry(Icons.Outlined.GridView, layoutSectionTitle, hubLayoutDesc) {
                                    activeCategory = SettingsCategory.LAYOUT
                                },
                                SettingsHubEntry(Icons.Outlined.ViewDay, badgesSectionTitle, hubBadgesDesc) {
                                    activeCategory = SettingsCategory.TIMELINE_BADGES
                                },
                                SettingsHubEntry(Icons.Outlined.PlayCircle, viewerSectionTitle, hubViewerDesc) {
                                    activeCategory = SettingsCategory.VIEWER
                                },
                            )
                        )
                    }
                    item(key = "hub_behavior") {
                        SettingsHubGroupCard(
                            listOf(
                                SettingsHubEntry(Icons.Outlined.Dashboard, behaviorSectionTitle, hubNavBehaviorDesc) {
                                    activeCategory = SettingsCategory.NAVIGATION_BEHAVIOR
                                },
                                SettingsHubEntry(Icons.Outlined.Security, privacySectionTitle, hubPrivacyDesc) {
                                    activeCategory = SettingsCategory.PRIVACY_SECURITY
                                },
                                SettingsHubEntry(Icons.Outlined.Notifications, memoriesSectionTitle, hubMemoriesDesc) {
                                    activeCategory = SettingsCategory.MEMORIES
                                },
                            )
                        )
                    }
                    item(key = "hub_system") {
                        SettingsHubGroupCard(
                            listOf(
                                SettingsHubEntry(Icons.Outlined.CleaningServices, storageSectionTitle, hubStorageDesc) {
                                    activeCategory = SettingsCategory.STORAGE
                                },
                                SettingsHubEntry(Icons.Outlined.Language, languageTitle, langLabel, accentSubtitle = true) {
                                    showLanguageDialog = true
                                },
                                SettingsHubEntry(Icons.Outlined.Info, aboutTitle, aboutSubtitle) {
                                    onOpenAbout()
                                },
                            )
                        )
                    }
                }
            } else {
                val isSearchView = viewKey == "search"
                val selectedCategory = if (isSearchView) null else runCatching { SettingsCategory.valueOf(viewKey) }.getOrNull()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
        if (isSearchView && !hasAnyMatches) {
            item(key = "settings_empty_search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = stringResource(R.string.empty_search_settings, searchQuery.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Section: Language & Region
        if (isSearchView && showLanguageSection) {
        item {
            SettingsSectionHeader(Icons.Outlined.Language, languageSectionTitle)
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
                            text = languageTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
        }

        // Section 1: Appearance & Theme
        if ((isSearchView && hasAnyAppearance) || selectedCategory == SettingsCategory.APPEARANCE) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.Palette, appearanceSectionTitle)
            }
        }

        item {
            val appearanceBlocks = buildList<@Composable () -> Unit> {
                if (showThemeMode) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(themeModeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                            ThemeMode.SYSTEM -> themeSystemStr
                                            ThemeMode.LIGHT -> themeLightStr
                                            ThemeMode.DARK -> themeDarkStr
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }
                        }
                    }
                }
                if (showAmoled) {
                    add {
                        SettingsSwitchRow(
                            title = amoledTitle,
                            subtitle = amoledDesc,
                            checked = settings.amoledBlack,
                            onCheckedChange = { preferences.setAmoledBlack(it) }
                        )
                    }
                }
                if (showMaterialYou) {
                    add {
                        SettingsSwitchRow(
                            title = materialYouTitle,
                            subtitle = materialYouDesc,
                            checked = settings.accentColor == AccentColor.MATERIAL_YOU,
                            onCheckedChange = { isChecked ->
                                preferences.setAccentColor(if (isChecked) AccentColor.MATERIAL_YOU else AccentColor.IRIS)
                            }
                        )
                    }
                }
                if (showCustomAccents) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(customAccentsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            }
            SettingsSectionCard(appearanceBlocks)
        }
        }

        // Section 2: Layout & Grid Customization
        if ((isSearchView && hasAnyLayout) || selectedCategory == SettingsCategory.LAYOUT) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.GridView, layoutSectionTitle)
            }
        }

        item {
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
            val density = LocalDensity.current
            val photoSpacingDp = settings.gridSpacing.dp.toFloat()
            val photoAvailableWidth = (screenWidthDp - 2 * photoSpacingDp).coerceAtLeast(100f)

            // Dynamic slider boundaries for Photos
            val minPhotoDp = maxOf(36f, ((photoAvailableWidth + photoSpacingDp) / 6.8f - photoSpacingDp).coerceAtLeast(36f))
            val maxPhotoDp = (photoAvailableWidth * 0.72f).coerceIn(160f, 320f)
            val currentPhotoSize = settings.photoGridSize.coerceIn(minPhotoDp, maxPhotoDp)
            val photoColumns = GridCalculations.calculatePhotoColumns(
                screenWidthDp = screenWidthDp,
                cellSizeDp = currentPhotoSize,
                gridSpacingDp = photoSpacingDp,
                density = density
            )

            // Dynamic slider boundaries for Albums
            val albumSpacingDp = (settings.gridSpacing.dp + 8).toFloat()
            val albumPaddingDp = 12f
            val albumAvailableWidth = (screenWidthDp - 2 * albumPaddingDp).coerceAtLeast(100f)
            val minAlbumDp = maxOf(48f, ((albumAvailableWidth + albumSpacingDp) / 4.8f - albumSpacingDp).coerceAtLeast(48f))
            val maxAlbumDp = (albumAvailableWidth * 0.85f).coerceIn(200f, 420f)
            val currentAlbumSize = settings.albumGridSize.coerceIn(minAlbumDp, maxAlbumDp)
            val albumColumns = GridCalculations.calculateAlbumColumns(
                screenWidthDp = screenWidthDp,
                cellSizeDp = currentAlbumSize,
                gridSpacingDp = settings.gridSpacing.dp.toFloat(),
                density = density
            )

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

            val layoutBlocks = buildList<@Composable () -> Unit> {
                if (showGridLayout) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(gridLayoutTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.photoGridLayout == com.iris.gallery.data.PhotoGridLayout.MOSAIC,
                                    onClick = { preferences.setPhotoGridLayout(com.iris.gallery.data.PhotoGridLayout.MOSAIC) },
                                    label = { Text(gridLayoutMosaic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.photoGridLayout == com.iris.gallery.data.PhotoGridLayout.GRID,
                                    onClick = { preferences.setPhotoGridLayout(com.iris.gallery.data.PhotoGridLayout.GRID) },
                                    label = { Text(gridLayoutSquare, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                            }
                        }
                    }
                }
                if (showAnimateGifs) {
                    add {
                        SettingsSwitchRow(
                            title = animateGifsTitle,
                            subtitle = animateGifsDesc,
                            checked = settings.animateGifThumbnails,
                            onCheckedChange = { preferences.setAnimateGifThumbnails(it) }
                        )
                    }
                }
                if (showCornerStyle) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(cornerStyleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    // 2x2 grid for symmetrical, perfect alignment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SHARP,
                                onClick = { preferences.setCornerStyle(CornerStyle.SHARP) },
                                label = { Text(cornerSharp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.CLASSIC,
                                onClick = { preferences.setCornerStyle(CornerStyle.CLASSIC) },
                                label = { Text(cornerSubtle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.ROUNDED,
                                onClick = { preferences.setCornerStyle(CornerStyle.ROUNDED) },
                                label = { Text(cornerRounded, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.cornerStyle == CornerStyle.SQUIRCLE,
                                onClick = { preferences.setCornerStyle(CornerStyle.SQUIRCLE) },
                                label = { Text(cornerPill, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
                        }
                    }
                }
                if (showGridSpacing) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(gridSpacingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                            GridSpacing.COMPACT -> spacingCompact
                                            GridSpacing.STANDARD -> spacingStandard
                                            GridSpacing.RELAXED -> spacingRelaxed
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }
                        }
                    }
                }
                if (showPhotosTileSize) {
                    add {
                    // Photos Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(photosTileTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        // Scaled Live Photos Grid Mockup (Dynamic Height, Full Width Fill)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
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
                                        val idealDp = GridCalculations.idealPhotoCellSizeForColumns(
                                            targetCols = targetCols,
                                            screenWidthDp = screenWidthDp,
                                            gridSpacingDp = photoSpacingDp,
                                            density = density
                                        ).coerceIn(minPhotoDp, maxPhotoDp)
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
                    }
                }
                if (showAlbumsTileSize) {
                    add {
                    // Albums Grid Tile Size & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(albumsTileTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

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
                                    Triple(stringResource(R.string.album_sample_camera), listOf(colorScheme.primaryContainer, colorScheme.primary.copy(alpha = 0.35f)), colorScheme.onPrimaryContainer),
                                    Triple(stringResource(R.string.album_sample_screenshots), listOf(colorScheme.secondaryContainer, colorScheme.secondary.copy(alpha = 0.3f)), colorScheme.onSecondaryContainer),
                                    Triple(stringResource(R.string.album_sample_downloads), listOf(colorScheme.tertiaryContainer, colorScheme.tertiary.copy(alpha = 0.35f)), colorScheme.onTertiaryContainer),
                                    Triple(stringResource(R.string.album_sample_wallpapers), listOf(colorScheme.surfaceVariant, colorScheme.surfaceVariant.copy(alpha = 0.6f)), colorScheme.onSurfaceVariant),
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
                                        val idealDp = GridCalculations.idealAlbumCellSizeForColumns(
                                            targetCols = targetCols,
                                            screenWidthDp = screenWidthDp,
                                            gridSpacingDp = settings.gridSpacing.dp.toFloat(),
                                            density = density
                                        ).coerceIn(minAlbumDp, maxAlbumDp)
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
            SettingsSectionCard(layoutBlocks)
        }
        }

        // Section 3: Media Badges & Timeline
        if ((isSearchView && hasAnyBadges) || selectedCategory == SettingsCategory.TIMELINE_BADGES) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.ViewDay, badgesSectionTitle)
            }
        }

        item {
            val badgeBlocks = buildList<@Composable () -> Unit> {
                if (showTimeline) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = timelineTitle,
                        subtitle = timelineDesc,
                        checked = settings.showTimelineHeaders,
                        onCheckedChange = { preferences.setShowTimelineHeaders(it) }
                    )

                    AnimatedVisibility(
                        visible = settings.showTimelineHeaders,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDateFormatDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        dateFormatTitle,
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
                                title = relativeDatesTitle,
                                subtitle = relativeDatesDesc,
                                checked = settings.useRelativeDates,
                                onCheckedChange = { preferences.setUseRelativeDates(it) }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = dayOfWeekTitle,
                                subtitle = dayOfWeekDesc,
                                checked = settings.showDayOfWeek,
                                onCheckedChange = { preferences.setShowDayOfWeek(it) }
                            )

                            AnimatedVisibility(
                                visible = settings.showDayOfWeek,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 14.dp))

                                    SettingsSwitchRow(
                                        title = abbrevDayTitle,
                                        subtitle = abbrevDayDesc,
                                        checked = settings.abbreviateDayOfWeek,
                                        onCheckedChange = { preferences.setAbbreviateDayOfWeek(it) }
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = smartYearTitle,
                                subtitle = smartYearDesc,
                                checked = settings.smartYearHiding,
                                onCheckedChange = { preferences.setSmartYearHiding(it) }
                            )
                        }
                    }
                        }
                    }
                }
                if (showBadgeDuration) {
                    add {
                        SettingsSwitchRow(
                            title = badgeDurationTitle,
                            subtitle = badgeDurationDesc,
                            checked = settings.showVideoDurationBadge,
                            onCheckedChange = { preferences.setShowVideoDurationBadge(it) }
                        )
                    }
                }
                if (showBadgeFormats) {
                    add {
                        SettingsSwitchRow(
                            title = badgeFormatsTitle,
                            subtitle = badgeFormatsDesc,
                            checked = settings.showMediaFormatBadge,
                            onCheckedChange = { preferences.setShowMediaFormatBadge(it) }
                        )
                    }
                }
                if (showAlbumCount) {
                    add {
                        SettingsSwitchRow(
                            title = albumCountTitle,
                            subtitle = albumCountDesc,
                            checked = settings.showAlbumCount,
                            onCheckedChange = { preferences.setShowAlbumCount(it) }
                        )
                    }
                }
                if (showAlbumLabels) {
                    add {
                        SettingsSwitchRow(
                            title = albumLabelsTitle,
                            subtitle = albumLabelsDesc,
                            checked = settings.showAlbumLabels,
                            onCheckedChange = { preferences.setShowAlbumLabels(it) }
                        )
                    }
                }
            }
            SettingsSectionCard(badgeBlocks)
        }
        }

        // Section 4: Viewer & Playback
        if ((isSearchView && hasAnyViewer) || selectedCategory == SettingsCategory.VIEWER) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.PlayCircle, viewerSectionTitle)
            }
        }

        item {
            val viewerBlocks = buildList<@Composable () -> Unit> {
                if (showViewerHeader) {
                    add {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showViewerHeaderDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    viewerHeaderTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    viewerHeaderStyleLabel,
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
                    }
                }
                if (showAutoplay) {
                    add {
                        SettingsSwitchRow(
                            title = autoplayTitle,
                            subtitle = autoplayDesc,
                            checked = settings.autoPlayVideo,
                            onCheckedChange = { preferences.setAutoPlayVideo(it) }
                        )
                    }
                }
                if (showLoop) {
                    add {
                        SettingsSwitchRow(
                            title = loopTitle,
                            subtitle = loopDesc,
                            checked = settings.loopVideo,
                            onCheckedChange = { preferences.setLoopVideo(it) }
                        )
                    }
                }
                if (showVideoDoubleTap) {
                    add {
                        SettingsSwitchRow(
                            title = videoDoubleTapTitle,
                            subtitle = videoDoubleTapDesc,
                            checked = settings.videoDoubleTapToZoom,
                            onCheckedChange = { preferences.setVideoDoubleTapToZoom(it) }
                        )
                    }
                }
                if (showVideoGestures) {
                    add {
                        SettingsSwitchRow(
                            title = videoGesturesTitle,
                            subtitle = videoGesturesDesc,
                            checked = settings.videoGestureControls,
                            onCheckedChange = { preferences.setVideoGestureControls(it) }
                        )
                    }
                }
                if (showUserComments) {
                    add {
                        SettingsSwitchRow(
                            title = userCommentsTitle,
                            subtitle = userCommentsDesc,
                            checked = settings.showViewerUserComments,
                            onCheckedChange = { preferences.setShowViewerUserComments(it) }
                        )
                    }
                }
                if (showFilmstrip) {
                    add {
                        SettingsSwitchRow(
                            title = filmstripTitle,
                            subtitle = filmstripDesc,
                            checked = settings.showFilmstrip,
                            onCheckedChange = { preferences.setShowFilmstrip(it) }
                        )
                    }
                }
                if (showPinchRotate) {
                    add {
                        SettingsSwitchRow(
                            title = pinchRotateTitle,
                            subtitle = pinchRotateDesc,
                            checked = settings.pinchToRotate,
                            onCheckedChange = { preferences.setPinchToRotate(it) }
                        )
                    }
                }
                if (showImageRotateBtn) {
                    add {
                        SettingsSwitchRow(
                            title = imageRotateBtnTitle,
                            subtitle = imageRotateBtnDesc,
                            checked = settings.showImageRotateButton,
                            onCheckedChange = { preferences.setShowImageRotateButton(it) }
                        )
                    }
                }
                if (showMultiStageZoom) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = multiStageZoomTitle,
                        subtitle = multiStageZoomDesc,
                        checked = settings.multiStageZoom,
                        onCheckedChange = { preferences.setMultiStageZoom(it) }
                    )

                    AnimatedVisibility(
                        visible = !settings.multiStageZoom,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text(zoomScaleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.doubleTapZoomLevel == -1.0f,
                                    onClick = { preferences.setDoubleTapZoomLevel(-1.0f) },
                                    label = { Text("1:1", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                            }
                        }
                    }
                        }
                    }
                }
            }
            SettingsSectionCard(viewerBlocks)
        }
        }

        // Section: Memories & Notifications
        if ((isSearchView && showMemories) || selectedCategory == SettingsCategory.MEMORIES) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.Notifications, memoriesSectionTitle)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = memoriesNotifTitle,
                        subtitle = memoriesNotifDesc,
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
                                        text = memoriesTimeTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = memoriesTimeDesc,
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
        }

        // Section 5: Navigation & Behavior
        if ((isSearchView && hasAnyNavBehavior) || selectedCategory == SettingsCategory.NAVIGATION_BEHAVIOR) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.Dashboard, behaviorSectionTitle)
            }
        }

        item {
            val behaviorBlocks = buildList<@Composable () -> Unit> {
                if (showNavTabs) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(navTabsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(navTabsDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showMediaTab,
                                onClick = {
                                    val next = !settings.showMediaTab
                                    preferences.setShowMediaTab(next)
                                    if (!next && settings.startupTab == StartupTab.MEDIA) {
                                        val fallback = when {
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(tabMediaStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showAlbumsTab,
                                onClick = {
                                    val next = !settings.showAlbumsTab
                                    preferences.setShowAlbumsTab(next)
                                    if (!next && settings.startupTab == StartupTab.ALBUMS) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(tabAlbumsStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showFavoritesTab,
                                onClick = {
                                    val next = !settings.showFavoritesTab
                                    preferences.setShowFavoritesTab(next)
                                    if (!next && settings.startupTab == StartupTab.FAVORITES) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showLibraryTab -> StartupTab.LIBRARY
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(tabFavoritesStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.showLibraryTab,
                                onClick = {
                                    val next = !settings.showLibraryTab
                                    preferences.setShowLibraryTab(next)
                                    if (!next && settings.startupTab == StartupTab.LIBRARY) {
                                        val fallback = when {
                                            settings.showMediaTab -> StartupTab.MEDIA
                                            settings.showAlbumsTab -> StartupTab.ALBUMS
                                            settings.showFavoritesTab -> StartupTab.FAVORITES
                                            settings.showVaultTab -> StartupTab.VAULT
                                            else -> StartupTab.MEDIA
                                        }
                                        preferences.setStartupTab(fallback)
                                    }
                                },
                                label = { Text(tabLibraryStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.fillMaxWidth(),
                                selected = settings.showVaultTab,
                                onClick = {
                                    preferences.setShowVaultTab(!settings.showVaultTab)
                                },
                                label = { Text(tabVaultStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
                        }
                    }
                }
                if (showTiltNavIcons) {
                    add {
                        SettingsSwitchRow(
                            title = tiltNavIconsTitle,
                            subtitle = tiltNavIconsDesc,
                            checked = settings.tiltNavIcons,
                            onCheckedChange = { preferences.setTiltNavIcons(it) }
                        )
                    }
                }
                if (showNavBarStyle) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(navBarStyleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.navBarStyle == NavBarStyle.DOCKED,
                                    onClick = { preferences.setNavBarStyle(NavBarStyle.DOCKED) },
                                    label = { Text(navBarDockedStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.navBarStyle == NavBarStyle.FLOATING,
                                    onClick = { preferences.setNavBarStyle(NavBarStyle.FLOATING) },
                                    label = { Text(navBarFloatingStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                            }
                        }
                    }
                }
                if (showNavLabelMode) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(navLabelModeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.navLabelMode == NavLabelMode.ALWAYS,
                                    onClick = { preferences.setNavLabelMode(NavLabelMode.ALWAYS) },
                                    label = { Text(navLabelAlwaysStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.navLabelMode == NavLabelMode.SELECTED_ONLY,
                                    onClick = { preferences.setNavLabelMode(NavLabelMode.SELECTED_ONLY) },
                                    label = { Text(navLabelSelectedStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = settings.navLabelMode == NavLabelMode.HIDDEN,
                                    onClick = { preferences.setNavLabelMode(NavLabelMode.HIDDEN) },
                                    label = { Text(navLabelHiddenStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
                if (showSelectionPillSetting) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsSwitchRow(
                                title = floatingSelectionTitle,
                                subtitle = floatingSelectionDesc,
                                checked = settings.useFloatingSelectionToolbar,
                                onCheckedChange = { preferences.setUseFloatingSelectionToolbar(it) }
                            )
                            AnimatedVisibility(
                                visible = settings.useFloatingSelectionToolbar,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showSelectionPillCustomizer = true }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Tune,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    selectionPillTitle,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                selectionPillDesc,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "${settings.selectionPillActions.size}/${ALL_SELECTION_PILL_ACTIONS.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
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
                }
                if (showAlbumFilters) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(albumTopTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    SettingsSwitchRow(
                        title = albumFiltersTitle,
                        subtitle = albumFiltersDesc,
                        checked = settings.showAlbumFilterChips,
                        onCheckedChange = { preferences.setShowAlbumFilterChips(it) }
                    )
                        }
                    }
                }
                if (showGlobalAlbumSearch) {
                    add {
                        SettingsSwitchRow(
                            title = globalAlbumSearchTitle,
                            subtitle = globalAlbumSearchDesc,
                            checked = settings.globalAlbumSearch,
                            onCheckedChange = { preferences.setGlobalAlbumSearch(it) }
                        )
                    }
                }
                if (showHiddenItemsToggle) {
                    add {
                        SettingsSwitchRow(
                            title = showHiddenItemsTitle,
                            subtitle = showHiddenItemsDesc,
                            checked = settings.showHiddenItems,
                            onCheckedChange = onToggleShowHiddenItems
                        )
                    }
                }
                if (showStartupTab) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(startupTabTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.MEDIA,
                                enabled = settings.showMediaTab,
                                onClick = { preferences.setStartupTab(StartupTab.MEDIA) },
                                label = { Text(tabMediaStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.ALBUMS,
                                enabled = settings.showAlbumsTab,
                                onClick = { preferences.setStartupTab(StartupTab.ALBUMS) },
                                label = { Text(tabAlbumsStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.FAVORITES,
                                enabled = settings.showFavoritesTab,
                                onClick = { preferences.setStartupTab(StartupTab.FAVORITES) },
                                label = { Text(tabFavoritesStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.startupTab == StartupTab.LIBRARY,
                                enabled = settings.showLibraryTab,
                                onClick = { preferences.setStartupTab(StartupTab.LIBRARY) },
                                label = { Text(tabLibraryStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                        if (settings.showVaultTab) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    modifier = Modifier.fillMaxWidth(),
                                    selected = settings.startupTab == StartupTab.VAULT,
                                    enabled = settings.showVaultTab,
                                    onClick = { preferences.setStartupTab(StartupTab.VAULT) },
                                    label = { Text(tabVaultStr, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                                )
                            }
                        }
                    }
                        }
                    }
                }
                if (showPreferredEditor) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            prefEditorTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            prefEditorDesc,
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
                                label = { Text(editorAsk, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.BUILT_IN,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.BUILT_IN) },
                                label = { Text(editorBuiltin, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.preferredEditor == PreferredEditor.EXTERNAL,
                                onClick = { preferences.setPreferredEditor(PreferredEditor.EXTERNAL) },
                                label = { Text(editorExternal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
                    }
                }
                if (showOverwrite) {
                    add {
                        SettingsSwitchRow(
                            title = overwriteTitle,
                            subtitle = overwriteDesc,
                            checked = settings.editorOverwriteOriginal,
                            onCheckedChange = { preferences.setEditorOverwriteOriginal(it) }
                        )
                    }
                }
            }
            SettingsSectionCard(behaviorBlocks)
        }
        }

        // Section 5B: Privacy & Security
        if ((isSearchView && hasAnyPrivacySecurity) || selectedCategory == SettingsCategory.PRIVACY_SECURITY) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.Security, privacySectionTitle)
            }
        }

        item {
            val privacyBlocks = buildList<@Composable () -> Unit> {
                if (showAppLock) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsSwitchRow(
                        title = appLockTitle,
                        subtitle = appLockDesc,
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
                            Text(changePinStr)
                        }

                        if (isBiometricHardwareAvailable(context)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                title = bioUnlockTitle,
                                subtitle = bioUnlockDesc,
                                checked = settings.appLockBiometricsEnabled,
                                onCheckedChange = { preferences.setAppLockBiometricsEnabled(it) }
                            )
                        }
                    }
                        }
                    }
                }
                if (showHideVault) {
                    add {
                        SettingsSwitchRow(
                            title = hideVaultTitle,
                            subtitle = hideVaultDesc,
                            checked = settings.vaultHideFromStorage,
                            onCheckedChange = { preferences.setVaultHideFromStorage(it) }
                        )
                    }
                }
                if (showStorageAccess) {
                    add {
                        val hasStorage = remember(context) { com.iris.gallery.data.StorageAccess.hasWriteAccess(context) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    storageAccessTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (hasStorage) storageAccessDescGranted
                                    else storageAccessDescPrompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (hasStorage) {
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
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                ) {
                                    Text(stringResource(R.string.settings_storage_access_button))
                                }
                            }
                        }
                    }
                }
                if (showSilentVault) {
                    add {
                        val hasAllFiles = remember(context) { Environment.isExternalStorageManager() }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    silentVaultTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (hasAllFiles) silentVaultDescGranted
                                    else silentVaultDescPrompt,
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
                }
                if (showVaultBio) {
                    add {
                        SettingsSwitchRow(
                            title = vaultBioTitle,
                            subtitle = vaultBioDesc,
                            checked = settings.biometricLockEnabled,
                            onCheckedChange = { preferences.setBiometricLockEnabled(it) }
                        )
                    }
                }
                if (showConfirmDelete) {
                    add {
                        SettingsSwitchRow(
                            title = confirmDeleteTitle,
                            subtitle = confirmDeleteDesc,
                            checked = settings.confirmDelete,
                            onCheckedChange = { preferences.setConfirmDelete(it) }
                        )
                    }
                }
                if (showDeleteMode) {
                    add {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            deleteModeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            deleteModeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.TRASH,
                                onClick = { preferences.setDeleteMode(DeleteMode.TRASH) },
                                label = { Text(deleteModeTrash, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.PERMANENT,
                                onClick = { preferences.setDeleteMode(DeleteMode.PERMANENT) },
                                label = { Text(deleteModePerm, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.deleteMode == DeleteMode.ALWAYS_ASK,
                                onClick = { preferences.setDeleteMode(DeleteMode.ALWAYS_ASK) },
                                label = { Text(deleteModeAsk, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                    }
                }
                if (showSystemTrash) {
                    add {
                        SettingsSwitchRow(
                            title = systemTrashTitle,
                            subtitle = systemTrashDesc,
                            checked = settings.useSystemTrash,
                            onCheckedChange = { preferences.setUseSystemTrash(it) }
                        )
                    }
                }
                if (showSecureSharing) {
                    add {
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSecureSharingDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = if (settings.secureSharingMode != SecureSharingMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    secureSharingTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                secureSharingModeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (settings.secureSharingMode != SecureSharingMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                secureSharingModeDesc,
                                style = MaterialTheme.typography.bodySmall,
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
            SettingsSectionCard(privacyBlocks)
        }
        }

        // Section 6: Cache & Reset
        if ((isSearchView && hasAnyStorage) || selectedCategory == SettingsCategory.STORAGE) {
        if (isSearchView) {
            item {
                SettingsSectionHeader(Icons.Outlined.CleaningServices, storageSectionTitle)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showRescanMedia) {
                        OutlinedButton(
                            onClick = onRescanMedia,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(rescanMediaLabel)
                        }
                    }

                    if (showExcludedFoldersBtn) {
                        OutlinedButton(
                            onClick = { showExcludedFoldersDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FolderOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(excludedFoldersLabel)
                        }
                    }

                    if (showClearCache) {
                        OutlinedButton(
                            onClick = ::clearCache,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(clearCacheLabel)
                        }
                    }

                    if (showResetAll) {
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(resetAllLabel)
                        }
                    }
                }
            }
        }
        }

        // Section 7: About Iris Gallery
        if (isSearchView && showAboutSection) {
        item {
            SettingsSectionHeader(Icons.Outlined.Info, aboutTitle)
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
                            aboutTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            aboutSubtitle,
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
            }
        }
    }

    if (showSelectionPillCustomizer) {
        SelectionPillCustomizerSheet(
            selectedActions = settings.selectionPillActions,
            onSave = { preferences.setSelectionPillActions(it) },
            onDismiss = { showSelectionPillCustomizer = false }
        )
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
            abbreviateDayOfWeek = settings.abbreviateDayOfWeek,
            onDismiss = { showDateFormatDialog = false },
            onFormatSelected = { format ->
                preferences.setTimelineDateFormat(format)
            },
            onCustomPatternChange = { pattern ->
                preferences.setCustomTimelineDateFormat(pattern)
            }
        )
    }

    if (showViewerHeaderDialog) {
        ViewerHeaderBottomSheet(
            currentStyle = settings.viewerHeaderStyle,
            showPageCount = settings.showViewerPageCount,
            showTime = settings.showViewerTime,
            onDismiss = { showViewerHeaderDialog = false },
            onStyleSelected = { style ->
                preferences.setViewerHeaderStyle(style)
            },
            onShowPageCountChange = { show ->
                preferences.setShowViewerPageCount(show)
            },
            onShowTimeChange = { show ->
                preferences.setShowViewerTime(show)
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

    if (showSecureSharingDialog) {
        AlertDialog(
            onDismissRequest = { showSecureSharingDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(stringResource(R.string.settings_secure_sharing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_secure_sharing_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SecureSharingMode.values().forEach { mode ->
                        val isSelected = settings.secureSharingMode == mode
                        val modeIcon = when (mode) {
                            SecureSharingMode.OFF -> Icons.Outlined.Share
                            SecureSharingMode.STRIP_LOCATION -> Icons.Outlined.LocationOff
                            SecureSharingMode.STRIP_ALL -> Icons.Outlined.Security
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    preferences.setSecureSharingMode(mode)
                                    showSecureSharingDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(mode.getTitleRes()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(mode.getDescriptionRes()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        preferences.setSecureSharingMode(mode)
                                        showSecureSharingDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecureSharingDialog = false }) {
                    Text(stringResource(R.string.action_done_editing))
                }
            }
        )
    }

    if (showExcludedFoldersDialog) {
        ExcludedFoldersDialog(
            excludedFolders = excludedFolders,
            availableAlbums = availableAlbums,
            onRemoveExcludedFolder = onRemoveExcludedFolder,
            onAddExcludedFolder = onAddExcludedFolder,
            onDismissRequest = { showExcludedFoldersDialog = false }
        )
    }
}

private fun getPathFromTreeUri(uri: Uri): String? {
    val authority = uri.authority.orEmpty()
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (_: Exception) {
        try {
            DocumentsContract.getDocumentId(uri)
        } catch (_: Exception) {
            uri.lastPathSegment.orEmpty()
        }
    }
    if (docId.isBlank()) return null
    if (authority == "com.android.externalstorage.documents" || authority.endsWith(".documents")) {
        val split = docId.split(":")
        val type = split.getOrNull(0).orEmpty()
        val relPath = if (split.size > 1) split[1].removePrefix("/") else ""
        return if (type.equals("primary", ignoreCase = true)) {
            if (relPath.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relPath"
        } else if (type.isNotBlank()) {
            if (relPath.isBlank()) "/storage/$type" else "/storage/$type/$relPath"
        } else {
            relPath
        }
    }
    if (docId.startsWith("raw:")) {
        return docId.removePrefix("raw:")
    }
    if (docId.startsWith("/")) {
        return docId
    }
    return null
}

@Composable
fun ExcludedFoldersDialog(
    excludedFolders: Set<String>,
    availableAlbums: List<MediaAlbum> = emptyList(),
    onRemoveExcludedFolder: (String) -> Unit,
    onAddExcludedFolder: (String) -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var showChooserDialog by remember { mutableStateOf(false) }
    var showAlbumPickerDialog by remember { mutableStateOf(false) }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = getPathFromTreeUri(uri)
            if (resolved != null && resolved.isNotBlank()) {
                val clean = resolved.trimEnd('/')
                if (clean in excludedFolders) {
                    Toast.makeText(context, R.string.dialog_folder_already_excluded, Toast.LENGTH_SHORT).show()
                } else {
                    onAddExcludedFolder(clean)
                    Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.excluded_folders_title)) },
        text = {
            if (excludedFolders.isEmpty()) {
                Text(stringResource(R.string.no_excluded_folders), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                ) {
                    items(excludedFolders.toList()) { folderPath ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(folderPath.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(folderPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                onRemoveExcludedFolder(folderPath)
                                Toast.makeText(context, R.string.toast_folder_unexcluded, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { showChooserDialog = true }) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_add_excluded_folder))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_done_editing))
            }
        }
    )

    if (showChooserDialog) {
        AlertDialog(
            onDismissRequest = { showChooserDialog = false },
            title = { Text(stringResource(R.string.dialog_add_excluded_folder_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChooserDialog = false
                                showAlbumPickerDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PhotoAlbum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.exclude_option_choose_album),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.exclude_option_choose_album_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChooserDialog = false
                                openDocumentTreeLauncher.launch(null)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.exclude_option_system_picker),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.exclude_option_system_picker_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChooserDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAlbumPickerDialog) {
        AlertDialog(
            onDismissRequest = { showAlbumPickerDialog = false },
            title = { Text(stringResource(R.string.dialog_choose_album_to_exclude_title)) },
            text = {
                if (availableAlbums.isEmpty()) {
                    Text(
                        stringResource(R.string.no_albums_to_exclude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                    ) {
                        items(availableAlbums, key = { it.id }) { album ->
                            val folderPath = remember(album) {
                                album.images.firstOrNull { it.path.isNotBlank() }?.let { File(it.path).parent }
                                    ?: File(album.cover.path).parent
                                    ?: ""
                            }
                            val isAlreadyExcluded = remember(folderPath, excludedFolders) {
                                folderPath.isNotBlank() && folderPath in excludedFolders
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlreadyExcluded)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyExcluded) {
                                        if (folderPath.isNotBlank()) {
                                            val clean = folderPath.trimEnd('/')
                                            onAddExcludedFolder(clean)
                                            Toast.makeText(context, R.string.toast_folder_excluded, Toast.LENGTH_SHORT).show()
                                            showAlbumPickerDialog = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        MediaThumbnail(album.cover, Modifier.fillMaxSize())
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isAlreadyExcluded) {
                                                stringResource(R.string.dialog_folder_already_excluded)
                                            } else {
                                                "${album.images.size} items • $folderPath"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isAlreadyExcluded)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlbumPickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(blocks: List<@Composable () -> Unit>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            blocks.forEachIndexed { index, block ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                block()
            }
        }
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

private data class SettingsHubEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentSubtitle: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsHubGroupCard(entries: List<SettingsHubEntry>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = entry.onClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entry.accentSubtitle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
