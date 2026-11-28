package com.iris.gallery.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.iris.gallery.R

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentColor { MATERIAL_YOU, IRIS, LAPIS_MESOPOTAMIA, EMERALD, ISHTAR_AMBER, ROSE, MONOCHROME }
enum class CornerStyle(val dp: Int) { SHARP(0), CLASSIC(4), ROUNDED(12), SQUIRCLE(18) }
enum class GridSpacing(val dp: Int) { COMPACT(2), STANDARD(4), RELAXED(8) }
enum class PhotoGridLayout { MOSAIC, GRID }
enum class AlbumsViewMode { GRID, LIST }
enum class NavBarStyle { DOCKED, FLOATING }
enum class NavLabelMode { ALWAYS, SELECTED_ONLY, HIDDEN }
enum class StartupTab(val pageIndex: Int) { MEDIA(0), ALBUMS(1), FAVORITES(2), LIBRARY(3), VAULT(4) }

val DEFAULT_SELECTION_PILL_ACTIONS = listOf("share", "move", "favorite", "lock", "delete")
val ALL_SELECTION_PILL_ACTIONS = listOf("share", "move", "copy", "favorite", "lock", "delete", "delete_permanent", "select_all")
enum class PreferredEditor { ALWAYS_ASK, BUILT_IN, EXTERNAL }
enum class DeleteMode { TRASH, PERMANENT, ALWAYS_ASK }
enum class AlbumPickerSort { FOLLOW_ALBUMS, RECENT, NAME }
enum class AutoBackupFrequency { ON_CHANGE, DAILY, WEEKLY }

enum class TimelineDateSource {
    DATE_TAKEN,
    DATE_MODIFIED,
    DATE_ADDED;

    fun getTitleRes(): Int = when (this) {
        DATE_TAKEN -> R.string.settings_timeline_date_source_taken
        DATE_MODIFIED -> R.string.settings_timeline_date_source_modified
        DATE_ADDED -> R.string.settings_timeline_date_source_added
    }

    fun getDescriptionRes(): Int = when (this) {
        DATE_TAKEN -> R.string.settings_timeline_date_source_taken_desc
        DATE_MODIFIED -> R.string.settings_timeline_date_source_modified_desc
        DATE_ADDED -> R.string.settings_timeline_date_source_added_desc
    }
}

enum class TimelineDateFormat {
    SYSTEM_DEFAULT,
    DAY_MONTH_YEAR,
    MONTH_DAY_YEAR,
    YEAR_MONTH_DAY,
    NUMERIC_DMY,
    NUMERIC_MDY,
    NUMERIC_YMD,
    CUSTOM;

    fun getDisplayNameRes(): Int = when (this) {
        SYSTEM_DEFAULT -> R.string.settings_date_format_system
        DAY_MONTH_YEAR -> R.string.settings_date_format_dmy
        MONTH_DAY_YEAR -> R.string.settings_date_format_mdy
        YEAR_MONTH_DAY -> R.string.settings_date_format_ymd
        NUMERIC_DMY -> R.string.settings_date_format_num_dmy
        NUMERIC_MDY -> R.string.settings_date_format_num_mdy
        NUMERIC_YMD -> R.string.settings_date_format_num_ymd
        CUSTOM -> R.string.settings_date_format_custom
    }
}

enum class ViewerHeaderStyle {
    DATE,
    TITLE_OR_FILENAME,
    FILENAME,
    ADAPTIVE;

    fun getDisplayNameRes(): Int = when (this) {
        DATE -> R.string.settings_viewer_header_style_date
        TITLE_OR_FILENAME -> R.string.settings_viewer_header_style_title
        FILENAME -> R.string.settings_viewer_header_style_filename
        ADAPTIVE -> R.string.settings_viewer_header_style_adaptive
    }

    fun getDescriptionRes(): Int = when (this) {
        DATE -> R.string.settings_viewer_header_style_date_desc
        TITLE_OR_FILENAME -> R.string.settings_viewer_header_style_title_desc
        FILENAME -> R.string.settings_viewer_header_style_filename_desc
        ADAPTIVE -> R.string.settings_viewer_header_style_adaptive_desc
    }
}

enum class SecureSharingMode {
    OFF,
    STRIP_LOCATION,
    STRIP_ALL;

    fun getTitleRes(): Int = when (this) {
        OFF -> R.string.settings_secure_sharing_off
        STRIP_LOCATION -> R.string.settings_secure_sharing_strip_location
        STRIP_ALL -> R.string.settings_secure_sharing_strip_all
    }

    fun getDescriptionRes(): Int = when (this) {
        OFF -> R.string.settings_secure_sharing_off_desc
        STRIP_LOCATION -> R.string.settings_secure_sharing_strip_location_desc
        STRIP_ALL -> R.string.settings_secure_sharing_strip_all_desc
    }
}

data class AppLanguage(
    val code: String, // "" for system, or "en", "ar", "es", etc.
    val displayName: String,
    val nativeName: String,
    val flag: String,
)

val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("", "System Default", "System Default", "🌐"),
    AppLanguage("en", "English", "English", "🇺🇸"),
    AppLanguage("ar", "Arabic", "العربية", "🇮🇶"),
    AppLanguage("es", "Spanish", "Español", "🇪🇸"),
    AppLanguage("fr", "French", "Français", "🇫🇷"),
    AppLanguage("de", "German", "Deutsch", "🇩🇪"),
    AppLanguage("zh", "Chinese (Simplified)", "简体中文", "🇨🇳"),
    AppLanguage("pt", "Portuguese", "Português", "🇧🇷"),
    AppLanguage("it", "Italian", "Italiano", "🇮🇹"),
    AppLanguage("ja", "Japanese", "日本語", "🇯🇵"),
    AppLanguage("pl", "Polish", "Polski", "🇵🇱"),
    AppLanguage("ru", "Russian", "Русский", "🇷🇺"),
    AppLanguage("tr", "Turkish", "Türkçe", "🇹🇷"),
)

fun getSystemDefaultLocale(context: Context? = null): java.util.Locale {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val appCtx = context ?: runCatching { com.iris.gallery.IrisApplication.instance }.getOrNull()
        val lm = appCtx?.getSystemService(android.app.LocaleManager::class.java)
        val sysLocales = lm?.systemLocales
        if (sysLocales != null && !sysLocales.isEmpty) {
            return sysLocales[0]
        }
    }
    val config = android.content.res.Resources.getSystem().configuration
    return if (Build.VERSION.SDK_INT >= 24) {
        if (!config.locales.isEmpty) config.locales[0] else java.util.Locale.getDefault()
    } else {
        @Suppress("DEPRECATION")
        config.locale ?: java.util.Locale.getDefault()
    }
}

fun detectBestLanguage(): String {
    val systemLang = getSystemDefaultLocale().language.lowercase()
    val match = SUPPORTED_LANGUAGES.firstOrNull { it.code.isNotEmpty() && it.code == systemLang }
    return match?.code ?: "en"
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val accentColor: AccentColor = AccentColor.MATERIAL_YOU,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val gridSpacing: GridSpacing = GridSpacing.STANDARD,
    val photoGridLayout: PhotoGridLayout = PhotoGridLayout.MOSAIC,
    val animateGifThumbnails: Boolean = false,
    val groupRawJpegPairs: Boolean = true,
    val showHiddenItems: Boolean = false,
    val photoGridSize: Float = 105f,
    val mosaicRowSize: Float = 120f,
    val albumGridSize: Float = 156f,
    val albumsViewMode: AlbumsViewMode = AlbumsViewMode.GRID,
    val groupDirectSubfolders: Boolean = false,
    val albumHierarchyDepth: Int = 0,
    val globalAlbumSearch: Boolean = true,
    val showTimelineHeaders: Boolean = true,
    val timelineDateSource: TimelineDateSource = TimelineDateSource.DATE_TAKEN,
    val timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    val customTimelineDateFormat: String = "d. MMMM yyyy",
    val useRelativeDates: Boolean = true,
    val showDayOfWeek: Boolean = false,
    val abbreviateDayOfWeek: Boolean = false,
    val smartYearHiding: Boolean = true,
    val showVideoDurationBadge: Boolean = true,
    val showMediaFormatBadge: Boolean = true,
    val showAlbumCount: Boolean = true,
    val showAlbumLabels: Boolean = true,
    val autoPlayVideo: Boolean = true,
    val loopVideo: Boolean = true,
    val videoDoubleTapToZoom: Boolean = false,
    val videoGestureControls: Boolean = true,
    val showViewerUserComments: Boolean = true,
    val viewerHeaderStyle: ViewerHeaderStyle = ViewerHeaderStyle.DATE,
    val showViewerPageCount: Boolean = true,
    val showViewerTime: Boolean = true,
    val showFilmstrip: Boolean = true,
    val dismissedFilmstripTip: Boolean = false,
    val pinchToRotate: Boolean = true,
    val showImageRotateButton: Boolean = false,
    val dismissedRotateTip: Boolean = false,
    val doubleTapZoomLevel: Float = 2.5f,
    val multiStageZoom: Boolean = true,
    val startupTab: StartupTab = StartupTab.MEDIA,
    val mediaFilter: Int = 0,
    val showMediaTab: Boolean = true,
    val showAlbumsTab: Boolean = true,
    val showFavoritesTab: Boolean = true,
    val showLibraryTab: Boolean = true,
    val showVaultTab: Boolean = false,
    val showAlbumFilterChips: Boolean = true,
    val tiltNavIcons: Boolean = true,
    val navBarStyle: NavBarStyle = NavBarStyle.DOCKED,
    val navLabelMode: NavLabelMode = NavLabelMode.ALWAYS,
    val useFloatingSelectionToolbar: Boolean = false,
    val selectionPillActions: List<String> = DEFAULT_SELECTION_PILL_ACTIONS,
    val albumPickerSort: AlbumPickerSort = AlbumPickerSort.FOLLOW_ALBUMS,
    val biometricLockEnabled: Boolean = true,
    val vaultHideFromStorage: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val appLockPinSalt: String = "",
    val appLockFailedAttempts: Int = 0,
    val appLockLockoutUntil: Long = 0L,
    val appLockBiometricsEnabled: Boolean = true,
    val confirmDelete: Boolean = false,
    val deleteMode: DeleteMode = DeleteMode.TRASH,
    val useSystemTrash: Boolean = false,
    val videoMuted: Boolean = false,
    val preferredEditor: PreferredEditor = PreferredEditor.ALWAYS_ASK,
    val preferredEditorComponent: String = "",
    val lastUsedEditorId: String = "",
    val preferredVideoEditorComponent: String = "",
    val lastUsedVideoEditorId: String = "",
    val editorOverwriteOriginal: Boolean = false,
    val language: String = "",
    val firstLaunchLanguageSetupDone: Boolean = false,
    val memoriesNotificationEnabled: Boolean = true,
    val memoriesNotificationHour: Int = 10,
    val memoriesNotificationMinute: Int = 0,
    val dismissedMemoriesTip: Boolean = false,
    val secureSharingMode: SecureSharingMode = SecureSharingMode.OFF,
    val dismissedSecureSharingTip: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoBackupTreeUri: String = "",
    val autoBackupFolderLabel: String = "",
    val autoBackupFrequency: AutoBackupFrequency = AutoBackupFrequency.ON_CHANGE,
    val autoBackupRetention: Int = 1,
    val lastAutoBackupTimestamp: Long = 0L,
    val lastAutoBackupError: String = "",
    val lastAutoBackupHash: String = "",
    val autoBackupConsecutiveFailures: Int = 0,
) {
    val hasPin: Boolean get() = appLockPinHash.isNotEmpty()
}

class SettingsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("iris_gallery_settings", Context.MODE_PRIVATE)
    private val _state: MutableStateFlow<SettingsState> = synchronized(sharedLock) {
        sharedState ?: MutableStateFlow(read()).also { sharedState = it }
    }
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    companion object {
        private val sharedLock = Any()
        @Volatile
        private var sharedState: MutableStateFlow<SettingsState>? = null
    }

    fun setThemeMode(mode: ThemeMode) = update { copy(themeMode = mode) }
    fun setLanguage(lang: String) = update { copy(language = lang) }
    fun setFirstLaunchLanguageSetupDone(done: Boolean) = update { copy(firstLaunchLanguageSetupDone = done) }
    fun setAmoledBlack(enabled: Boolean) = update { copy(amoledBlack = enabled) }
    fun setAccentColor(color: AccentColor) = update { copy(accentColor = color) }
    fun setCornerStyle(style: CornerStyle) = update { copy(cornerStyle = style) }
    fun setGridSpacing(spacing: GridSpacing) = update { copy(gridSpacing = spacing) }
    fun setPhotoGridLayout(layout: PhotoGridLayout) = update { copy(photoGridLayout = layout) }
    fun setAnimateGifThumbnails(enabled: Boolean) = update { copy(animateGifThumbnails = enabled) }
    fun setGroupRawJpegPairs(enabled: Boolean) = update { copy(groupRawJpegPairs = enabled) }
    fun setShowHiddenItems(show: Boolean) = update { copy(showHiddenItems = show) }
    fun setPhotoGridSize(size: Float) = update { copy(photoGridSize = size.coerceIn(36f, 340f)) }
    fun setMosaicRowSize(size: Float) = update { copy(mosaicRowSize = size.coerceIn(56f, 260f)) }
    fun setAlbumGridSize(size: Float) = update { copy(albumGridSize = size.coerceIn(48f, 450f)) }
    fun setAlbumsViewMode(mode: AlbumsViewMode) = update { copy(albumsViewMode = mode) }
    fun setGroupDirectSubfolders(enabled: Boolean) = update { copy(groupDirectSubfolders = enabled) }
    fun setAlbumHierarchyDepth(depth: Int) = update { copy(albumHierarchyDepth = depth.coerceIn(0, 4)) }
    fun setGlobalAlbumSearch(enabled: Boolean) = update { copy(globalAlbumSearch = enabled) }
    fun setShowTimelineHeaders(show: Boolean) = update { copy(showTimelineHeaders = show) }
    fun setTimelineDateSource(source: TimelineDateSource) = update { copy(timelineDateSource = source) }
    fun setTimelineDateFormat(format: TimelineDateFormat) = update { copy(timelineDateFormat = format) }
    fun setCustomTimelineDateFormat(pattern: String) = update {
        val trimmed = pattern.trim().ifBlank { "d. MMMM yyyy" }
        val safe = runCatching {
            val f = java.time.format.DateTimeFormatter.ofPattern(trimmed, java.util.Locale.getDefault())
            java.time.LocalDate.now().format(f)
            trimmed
        }.getOrDefault("d. MMMM yyyy")
        copy(customTimelineDateFormat = safe)
    }
    fun setUseRelativeDates(use: Boolean) = update { copy(useRelativeDates = use) }
    fun setShowDayOfWeek(show: Boolean) = update { copy(showDayOfWeek = show) }
    fun setAbbreviateDayOfWeek(abbreviate: Boolean) = update { copy(abbreviateDayOfWeek = abbreviate) }
    fun setSmartYearHiding(smart: Boolean) = update { copy(smartYearHiding = smart) }
    fun setShowVideoDurationBadge(show: Boolean) = update { copy(showVideoDurationBadge = show) }
    fun setShowMediaFormatBadge(show: Boolean) = update { copy(showMediaFormatBadge = show) }
    fun setShowAlbumCount(show: Boolean) = update { copy(showAlbumCount = show) }
    fun setShowAlbumLabels(show: Boolean) = update { copy(showAlbumLabels = show) }
    fun setAutoPlayVideo(autoPlay: Boolean) = update { copy(autoPlayVideo = autoPlay) }
    fun setLoopVideo(loop: Boolean) = update { copy(loopVideo = loop) }
    fun setVideoDoubleTapToZoom(enabled: Boolean) = update { copy(videoDoubleTapToZoom = enabled) }
    fun setVideoGestureControls(enabled: Boolean) = update { copy(videoGestureControls = enabled) }
    fun setShowViewerUserComments(show: Boolean) = update { copy(showViewerUserComments = show) }
    fun setViewerHeaderStyle(style: ViewerHeaderStyle) = update { copy(viewerHeaderStyle = style) }
    fun setShowViewerPageCount(show: Boolean) = update { copy(showViewerPageCount = show) }
    fun setShowViewerTime(show: Boolean) = update { copy(showViewerTime = show) }
    fun setShowFilmstrip(show: Boolean) = update { copy(showFilmstrip = show) }
    fun setDismissedFilmstripTip(dismissed: Boolean) = update { copy(dismissedFilmstripTip = dismissed) }
    fun setPinchToRotate(enabled: Boolean) = update { copy(pinchToRotate = enabled) }
    fun setShowImageRotateButton(show: Boolean) = update { copy(showImageRotateButton = show) }
    fun setDismissedRotateTip(dismissed: Boolean) = update { copy(dismissedRotateTip = dismissed) }
    fun setDoubleTapZoomLevel(level: Float) = update { copy(doubleTapZoomLevel = level) }
    fun setMultiStageZoom(enabled: Boolean) = update { copy(multiStageZoom = enabled) }
    fun setStartupTab(tab: StartupTab) = update { copy(startupTab = tab) }
    fun setMediaFilter(filter: Int) = update { copy(mediaFilter = filter) }
    fun setShowMediaTab(show: Boolean) = update {
        if (!show && !showAlbumsTab && !showFavoritesTab && !showLibraryTab && !showVaultTab) this
        else copy(showMediaTab = show)
    }
    fun setShowAlbumsTab(show: Boolean) = update {
        if (!show && !showMediaTab && !showFavoritesTab && !showLibraryTab && !showVaultTab) this
        else copy(showAlbumsTab = show)
    }
    fun setShowFavoritesTab(show: Boolean) = update {
        if (!show && !showMediaTab && !showAlbumsTab && !showLibraryTab && !showVaultTab) this
        else copy(showFavoritesTab = show)
    }
    fun setShowLibraryTab(show: Boolean) = update {
        if (!show && !showMediaTab && !showAlbumsTab && !showFavoritesTab && !showVaultTab) this
        else copy(showLibraryTab = show)
    }
    fun setShowVaultTab(show: Boolean) = update {
        if (!show && !showMediaTab && !showAlbumsTab && !showFavoritesTab && !showLibraryTab) this
        else copy(
            showVaultTab = show,
            startupTab = if (!show && startupTab == StartupTab.VAULT) StartupTab.MEDIA else startupTab
        )
    }
    fun setShowAlbumFilterChips(show: Boolean) = update { copy(showAlbumFilterChips = show) }
    fun setTiltNavIcons(tilt: Boolean) = update { copy(tiltNavIcons = tilt) }
    fun setNavBarStyle(style: NavBarStyle) = update { copy(navBarStyle = style) }
    fun setNavLabelMode(mode: NavLabelMode) = update { copy(navLabelMode = mode) }
    fun setUseFloatingSelectionToolbar(enabled: Boolean) = update { copy(useFloatingSelectionToolbar = enabled) }
    fun setSelectionPillActions(actions: List<String>) = update {
        val valid = actions.filter { it in ALL_SELECTION_PILL_ACTIONS }.distinct()
        copy(selectionPillActions = if (valid.isEmpty()) DEFAULT_SELECTION_PILL_ACTIONS else valid)
    }
    fun setAlbumPickerSort(sort: AlbumPickerSort) = update { copy(albumPickerSort = sort) }
    fun setBiometricLockEnabled(enabled: Boolean) = update { copy(biometricLockEnabled = enabled) }
    fun setVaultHideFromStorage(enabled: Boolean) = update { copy(vaultHideFromStorage = enabled) }
    fun setAppLockEnabled(enabled: Boolean) = update { copy(appLockEnabled = enabled) }
    fun setAppLockBiometricsEnabled(enabled: Boolean) = update { copy(appLockBiometricsEnabled = enabled) }
    fun setConfirmDelete(enabled: Boolean) = update { copy(confirmDelete = enabled) }
    fun setDeleteMode(mode: DeleteMode) = update { copy(deleteMode = mode) }
    fun setUseSystemTrash(enabled: Boolean) = update { copy(useSystemTrash = enabled) }
    fun setVideoMuted(muted: Boolean) = update { copy(videoMuted = muted) }
    fun setPreferredEditor(editor: PreferredEditor) = update {
        copy(
            preferredEditor = editor,
            preferredEditorComponent = if (editor == PreferredEditor.EXTERNAL) preferredEditorComponent else "",
            lastUsedEditorId = if (editor == PreferredEditor.BUILT_IN) "BUILT_IN" else lastUsedEditorId,
        )
    }
    fun setPreferredExternalEditor(component: String) = update {
        copy(
            preferredEditor = PreferredEditor.EXTERNAL,
            preferredEditorComponent = component,
            lastUsedEditorId = component.ifBlank { lastUsedEditorId },
        )
    }
    fun setLastUsedEditorId(id: String) = update { copy(lastUsedEditorId = id) }
    fun resetPreferredEditor() = update {
        copy(preferredEditor = PreferredEditor.ALWAYS_ASK, preferredEditorComponent = "")
    }
    fun setPreferredVideoEditorComponent(component: String) = update {
        copy(
            preferredVideoEditorComponent = component,
            lastUsedVideoEditorId = component.ifBlank { lastUsedVideoEditorId },
        )
    }
    fun setLastUsedVideoEditorId(id: String) = update { copy(lastUsedVideoEditorId = id) }
    fun resetPreferredVideoEditor() = update { copy(preferredVideoEditorComponent = "") }
    fun setEditorOverwriteOriginal(enabled: Boolean) = update { copy(editorOverwriteOriginal = enabled) }
    fun setMemoriesNotificationEnabled(enabled: Boolean) = update { copy(memoriesNotificationEnabled = enabled) }
    fun setMemoriesNotificationTime(hour: Int, minute: Int) = update { copy(memoriesNotificationHour = hour, memoriesNotificationMinute = minute) }
    fun setDismissedMemoriesTip(dismissed: Boolean) = update { copy(dismissedMemoriesTip = dismissed) }
    fun setSecureSharingMode(mode: SecureSharingMode) = update { copy(secureSharingMode = mode) }
    fun setDismissedSecureSharingTip(dismissed: Boolean) = update { copy(dismissedSecureSharingTip = dismissed) }
    fun setAutoBackupEnabled(enabled: Boolean) = update {
        copy(
            autoBackupEnabled = enabled,
            lastAutoBackupError = if (!enabled) "" else lastAutoBackupError,
            autoBackupConsecutiveFailures = if (!enabled) 0 else autoBackupConsecutiveFailures,
        )
    }
    fun setAutoBackupFolder(treeUri: String, label: String, enableIfBlank: Boolean = true) = update {
        copy(
            autoBackupTreeUri = treeUri,
            autoBackupFolderLabel = label,
            autoBackupEnabled = if (treeUri.isBlank()) false else (enableIfBlank || autoBackupEnabled),
            lastAutoBackupError = "",
            lastAutoBackupHash = "",
            autoBackupConsecutiveFailures = 0,
        )
    }
    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) = update { copy(autoBackupFrequency = frequency) }
    fun setAutoBackupRetention(count: Int) = update { copy(autoBackupRetention = count.coerceIn(1, 30)) }
    fun recordAutoBackupSuccess(timestamp: Long, contentHash: String) = update {
        copy(
            lastAutoBackupTimestamp = timestamp,
            lastAutoBackupError = "",
            lastAutoBackupHash = contentHash,
            autoBackupConsecutiveFailures = 0,
        )
    }
    fun recordAutoBackupFailure(error: String): Int {
        var failures = 0
        update {
            failures = autoBackupConsecutiveFailures + 1
            copy(
                lastAutoBackupError = error,
                autoBackupConsecutiveFailures = failures,
            )
        }
        return failures
    }

    /** PBKDF2 iteration count for new PIN hashes. */
    private val pinPbkdf2Iterations = 120_000

    fun setPin(pin: String) {
        val salt = java.util.UUID.randomUUID().toString()
        val hash = hashPin(pin, salt) ?: legacyHashPin(pin, salt)
        update {
            copy(
                appLockPinHash = hash,
                appLockPinSalt = salt,
                appLockEnabled = true,
                appLockFailedAttempts = 0,
                appLockLockoutUntil = 0L,
            )
        }
    }

    fun verifyPin(pin: String): Boolean {
        val current = _state.value
        val stored = current.appLockPinHash
        if (stored.isEmpty()) return false
        if (current.appLockLockoutUntil > System.currentTimeMillis()) return false
        val verified = if (stored.startsWith("pbkdf2:")) {
            val parts = stored.split(":")
            val iterations = parts.getOrNull(1)?.toIntOrNull() ?: return false
            hashPinWithIterations(pin, current.appLockPinSalt, iterations) == stored
        } else {
            legacyHashPin(pin, current.appLockPinSalt) == stored
        }
        if (verified) {
            if (current.appLockFailedAttempts != 0 || current.appLockLockoutUntil != 0L || !stored.startsWith("pbkdf2:")) {
                // Reset throttling and silently upgrade legacy hashes while the PIN is known.
                val upgraded = if (stored.startsWith("pbkdf2:")) stored else (hashPin(pin, current.appLockPinSalt) ?: stored)
                update {
                    copy(
                        appLockPinHash = upgraded,
                        appLockFailedAttempts = 0,
                        appLockLockoutUntil = 0L,
                    )
                }
            }
            return true
        }
        registerPinFailure(current)
        return false
    }

    fun pinLockoutRemainingMs(): Long {
        return (_state.value.appLockLockoutUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun registerPinFailure(current: SettingsState) {
        val attempts = current.appLockFailedAttempts + 1
        val lockoutMs = when {
            attempts < 5 -> 0L
            attempts < 10 -> 30_000L
            attempts < 15 -> 120_000L
            else -> 600_000L
        }
        update {
            copy(
                appLockFailedAttempts = attempts,
                appLockLockoutUntil = if (lockoutMs > 0L) System.currentTimeMillis() + lockoutMs else 0L,
            )
        }
    }

    fun removePin() {
        update {
            copy(
                appLockPinHash = "",
                appLockPinSalt = "",
                appLockEnabled = false,
                appLockFailedAttempts = 0,
                appLockLockoutUntil = 0L,
            )
        }
    }

    private fun hashPin(pin: String, salt: String): String? {
        return hashPinWithIterations(pin, salt, pinPbkdf2Iterations)
    }

    private fun hashPinWithIterations(pin: String, salt: String, iterations: Int): String? {
        return runCatching {
            val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), iterations, 256)
            try {
                val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                "pbkdf2:$iterations:" + key.joinToString("") { "%02x".format(it) }
            } finally {
                spec.clearPassword()
            }
        }.getOrNull()
    }

    private fun legacyHashPin(pin: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun resetToDefaults() {
        synchronized(sharedLock) {
            val current = _state.value
            val reset = SettingsState().copy(
                appLockPinHash = current.appLockPinHash,
                appLockPinSalt = current.appLockPinSalt,
                appLockEnabled = current.appLockEnabled,
                autoBackupEnabled = current.autoBackupEnabled,
                autoBackupTreeUri = current.autoBackupTreeUri,
                autoBackupFolderLabel = current.autoBackupFolderLabel,
                autoBackupFrequency = current.autoBackupFrequency,
                autoBackupRetention = current.autoBackupRetention,
                lastAutoBackupTimestamp = current.lastAutoBackupTimestamp,
                lastAutoBackupError = current.lastAutoBackupError,
                lastAutoBackupHash = current.lastAutoBackupHash,
                autoBackupConsecutiveFailures = current.autoBackupConsecutiveFailures,
            )
            _state.value = reset
            write(reset)
        }
    }

    fun restoreFromBackup(restored: SettingsState) {
        synchronized(sharedLock) {
            val current = _state.value
            val merged = restored.copy(
                appLockPinHash = current.appLockPinHash,
                appLockPinSalt = current.appLockPinSalt,
                appLockEnabled = current.appLockEnabled,
                appLockFailedAttempts = current.appLockFailedAttempts,
                appLockLockoutUntil = current.appLockLockoutUntil,
                firstLaunchLanguageSetupDone = current.firstLaunchLanguageSetupDone,
                autoBackupEnabled = current.autoBackupEnabled,
                autoBackupTreeUri = current.autoBackupTreeUri,
                autoBackupFolderLabel = current.autoBackupFolderLabel,
                autoBackupFrequency = current.autoBackupFrequency,
                autoBackupRetention = current.autoBackupRetention,
                lastAutoBackupTimestamp = current.lastAutoBackupTimestamp,
                lastAutoBackupError = current.lastAutoBackupError,
                lastAutoBackupHash = current.lastAutoBackupHash,
                autoBackupConsecutiveFailures = current.autoBackupConsecutiveFailures,
            )
            _state.value = merged
            write(merged)
        }
    }

    private fun update(transform: SettingsState.() -> SettingsState) {
        synchronized(sharedLock) {
            val updated = _state.value.transform()
            _state.value = updated
            write(updated)
        }
    }

    private fun read(): SettingsState {
        val themeModeStr = prefs.getString("theme_mode", null)
        val accentStr = prefs.getString("accent_color", null)
        val cornerStr = prefs.getString("corner_style", null)
        val spacingStr = prefs.getString("grid_spacing", null)
        val startupStr = prefs.getString("startup_tab", null)
        val dateFormatStr = prefs.getString("timeline_date_format", null)

        return SettingsState(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr.orEmpty()) }.getOrDefault(ThemeMode.SYSTEM),
            amoledBlack = prefs.getBoolean("amoled_black", false),
            accentColor = runCatching { AccentColor.valueOf(accentStr.orEmpty()) }.getOrDefault(AccentColor.MATERIAL_YOU),
            cornerStyle = runCatching { CornerStyle.valueOf(cornerStr.orEmpty()) }.getOrDefault(CornerStyle.ROUNDED),
            gridSpacing = runCatching { GridSpacing.valueOf(spacingStr.orEmpty()) }.getOrDefault(GridSpacing.STANDARD),
            photoGridLayout = runCatching {
                PhotoGridLayout.valueOf(prefs.getString("photo_grid_layout", null).orEmpty())
            }.getOrDefault(PhotoGridLayout.MOSAIC),
            animateGifThumbnails = prefs.getBoolean("animate_gif_thumbnails", false),
            groupRawJpegPairs = prefs.getBoolean("group_raw_jpeg_pairs", true),
            showHiddenItems = prefs.getBoolean("show_hidden_items", false),
            photoGridSize = prefs.getFloat("photo_grid_size", 105f),
            mosaicRowSize = prefs.getFloat("mosaic_row_size", 120f).coerceIn(56f, 260f),
            albumGridSize = prefs.getFloat("album_grid_size", 156f),
            albumsViewMode = runCatching {
                AlbumsViewMode.valueOf(prefs.getString("albums_view_mode", null).orEmpty())
            }.getOrDefault(AlbumsViewMode.GRID),
            groupDirectSubfolders = prefs.getBoolean("group_direct_subfolders", false),
            albumHierarchyDepth = prefs.getInt("album_hierarchy_depth", 0).coerceIn(0, 4),
            globalAlbumSearch = prefs.getBoolean("global_album_search", true),
            showTimelineHeaders = prefs.getBoolean("show_timeline_headers", true),
            timelineDateSource = runCatching {
                TimelineDateSource.valueOf(prefs.getString("timeline_date_source", null).orEmpty())
            }.getOrDefault(TimelineDateSource.DATE_TAKEN),
            timelineDateFormat = runCatching { TimelineDateFormat.valueOf(dateFormatStr.orEmpty()) }.getOrDefault(TimelineDateFormat.SYSTEM_DEFAULT),
            customTimelineDateFormat = runCatching {
                val raw = prefs.getString("custom_timeline_date_format", "d. MMMM yyyy").orEmpty().ifBlank { "d. MMMM yyyy" }
                val f = java.time.format.DateTimeFormatter.ofPattern(raw, java.util.Locale.getDefault())
                java.time.LocalDate.now().format(f)
                raw
            }.getOrDefault("d. MMMM yyyy"),
            useRelativeDates = prefs.getBoolean("use_relative_dates", true),
            showDayOfWeek = prefs.getBoolean("show_day_of_week", false),
            abbreviateDayOfWeek = prefs.getBoolean("abbreviate_day_of_week", false),
            smartYearHiding = prefs.getBoolean("smart_year_hiding", true),
            showVideoDurationBadge = prefs.getBoolean("show_video_duration_badge", true),
            showMediaFormatBadge = prefs.getBoolean("show_media_format_badge", true),
            showAlbumCount = prefs.getBoolean("show_album_count", true),
            showAlbumLabels = prefs.getBoolean("show_album_labels", true),
            autoPlayVideo = prefs.getBoolean("auto_play_video", true),
            loopVideo = prefs.getBoolean("loop_video", true),
            videoDoubleTapToZoom = prefs.getBoolean("video_double_tap_to_zoom", false),
            videoGestureControls = prefs.getBoolean("video_gesture_controls", true),
            showViewerUserComments = prefs.getBoolean("show_viewer_user_comments", true),
            viewerHeaderStyle = runCatching { ViewerHeaderStyle.valueOf(prefs.getString("viewer_header_style", null).orEmpty()) }.getOrDefault(ViewerHeaderStyle.DATE),
            showViewerPageCount = prefs.getBoolean("show_viewer_page_count", true),
            showViewerTime = prefs.getBoolean("show_viewer_time", true),
            showFilmstrip = prefs.getBoolean("show_filmstrip", true),
            dismissedFilmstripTip = prefs.getBoolean("dismissed_filmstrip_tip", false),
            pinchToRotate = prefs.getBoolean("pinch_to_rotate", true),
            showImageRotateButton = prefs.getBoolean("show_image_rotate_button", false),
            dismissedRotateTip = prefs.getBoolean("dismissed_rotate_tip", false),
            doubleTapZoomLevel = prefs.getFloat("double_tap_zoom_level", 2.5f),
            multiStageZoom = prefs.getBoolean("multi_stage_zoom_v2", true),
            startupTab = runCatching { StartupTab.valueOf(startupStr.orEmpty()) }.getOrDefault(StartupTab.MEDIA),
            mediaFilter = prefs.getInt("media_filter", if (prefs.getBoolean("photos_only", false)) 1 else 0),
            showMediaTab = prefs.getBoolean("show_media_tab", prefs.getBoolean("show_photos_tab", true)),
            showAlbumsTab = prefs.getBoolean("show_albums_tab", true),
            showFavoritesTab = prefs.getBoolean("show_favorites_tab", true),
            showLibraryTab = prefs.getBoolean("show_library_tab", true),
            showVaultTab = prefs.getBoolean("show_vault_tab", false),
            showAlbumFilterChips = prefs.getBoolean("show_album_filter_chips", true),
            tiltNavIcons = prefs.getBoolean("tilt_nav_icons", true),
            navBarStyle = runCatching {
                NavBarStyle.valueOf(prefs.getString("nav_bar_style", null).orEmpty())
            }.getOrDefault(NavBarStyle.DOCKED),
            navLabelMode = runCatching {
                NavLabelMode.valueOf(prefs.getString("nav_label_mode", null).orEmpty())
            }.getOrDefault(NavLabelMode.ALWAYS),
            useFloatingSelectionToolbar = prefs.getBoolean("use_floating_selection_toolbar", false),
            selectionPillActions = prefs.getString("selection_pill_actions", null)
                ?.split(',')
                ?.filter { it in ALL_SELECTION_PILL_ACTIONS }
                ?.distinct()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_SELECTION_PILL_ACTIONS,
            albumPickerSort = runCatching { AlbumPickerSort.valueOf(prefs.getString("album_picker_sort", null).orEmpty()) }.getOrDefault(AlbumPickerSort.FOLLOW_ALBUMS),
            biometricLockEnabled = prefs.getBoolean("biometric_lock_enabled", true),
            vaultHideFromStorage = prefs.getBoolean("vault_hide_from_storage", true),
            appLockEnabled = prefs.getBoolean("app_lock_enabled", false),
            appLockPinHash = prefs.getString("app_lock_pin_hash", "").orEmpty(),
            appLockPinSalt = prefs.getString("app_lock_pin_salt", "").orEmpty(),
            appLockFailedAttempts = prefs.getInt("app_lock_failed_attempts", 0),
            appLockLockoutUntil = prefs.getLong("app_lock_lockout_until", 0L),
            appLockBiometricsEnabled = prefs.getBoolean("app_lock_biometrics_enabled", true),
            confirmDelete = prefs.getBoolean("confirm_delete", false),
            deleteMode = runCatching { DeleteMode.valueOf(prefs.getString("delete_mode", null).orEmpty()) }.getOrDefault(DeleteMode.TRASH),
            useSystemTrash = prefs.getBoolean("use_system_trash", false),
            videoMuted = prefs.getBoolean("video_muted", false),
            preferredEditor = runCatching { PreferredEditor.valueOf(prefs.getString("preferred_editor", null).orEmpty()) }.getOrDefault(PreferredEditor.ALWAYS_ASK),
            preferredEditorComponent = prefs.getString("preferred_editor_component", "").orEmpty(),
            lastUsedEditorId = prefs.getString("last_used_editor_id", "").orEmpty(),
            preferredVideoEditorComponent = prefs.getString("preferred_video_editor_component", "").orEmpty(),
            lastUsedVideoEditorId = prefs.getString("last_used_video_editor_id", "").orEmpty(),
            editorOverwriteOriginal = prefs.getBoolean("editor_overwrite_original", false),
            language = prefs.getString("app_language", "").orEmpty(),
            firstLaunchLanguageSetupDone = prefs.getBoolean("first_launch_lang_done", false),
            memoriesNotificationEnabled = prefs.getBoolean("memories_notification_enabled", true),
            memoriesNotificationHour = prefs.getInt("memories_notification_hour", 10),
            memoriesNotificationMinute = prefs.getInt("memories_notification_minute", 0),
            dismissedMemoriesTip = prefs.getBoolean("dismissed_memories_tip", false),
            secureSharingMode = runCatching { SecureSharingMode.valueOf(prefs.getString("secure_sharing_mode", null).orEmpty()) }.getOrDefault(SecureSharingMode.OFF),
            dismissedSecureSharingTip = prefs.getBoolean("dismissed_secure_sharing_tip", false),
            autoBackupEnabled = prefs.getBoolean("auto_backup_enabled", false),
            autoBackupTreeUri = prefs.getString("auto_backup_tree_uri", "").orEmpty(),
            autoBackupFolderLabel = prefs.getString("auto_backup_folder_label", "").orEmpty(),
            autoBackupFrequency = runCatching {
                AutoBackupFrequency.valueOf(prefs.getString("auto_backup_frequency", null).orEmpty())
            }.getOrDefault(AutoBackupFrequency.ON_CHANGE),
            autoBackupRetention = prefs.getInt("auto_backup_retention", 1).coerceIn(1, 30),
            lastAutoBackupTimestamp = prefs.getLong("last_auto_backup_timestamp", 0L),
            lastAutoBackupError = prefs.getString("last_auto_backup_error", "").orEmpty(),
            lastAutoBackupHash = prefs.getString("last_auto_backup_hash", "").orEmpty(),
            autoBackupConsecutiveFailures = prefs.getInt("auto_backup_consecutive_failures", 0).coerceAtLeast(0),
        )
    }

    private fun write(state: SettingsState) {
        prefs.edit()
            .putString("theme_mode", state.themeMode.name)
            .putBoolean("amoled_black", state.amoledBlack)
            .putString("accent_color", state.accentColor.name)
            .putString("corner_style", state.cornerStyle.name)
            .putString("grid_spacing", state.gridSpacing.name)
            .putString("photo_grid_layout", state.photoGridLayout.name)
            .putBoolean("animate_gif_thumbnails", state.animateGifThumbnails)
            .putBoolean("group_raw_jpeg_pairs", state.groupRawJpegPairs)
            .putBoolean("show_hidden_items", state.showHiddenItems)
            .putFloat("photo_grid_size", state.photoGridSize)
            .putFloat("mosaic_row_size", state.mosaicRowSize)
            .putFloat("album_grid_size", state.albumGridSize)
            .putString("albums_view_mode", state.albumsViewMode.name)
            .putBoolean("group_direct_subfolders", state.groupDirectSubfolders)
            .putInt("album_hierarchy_depth", state.albumHierarchyDepth)
            .putBoolean("global_album_search", state.globalAlbumSearch)
            .putBoolean("show_timeline_headers", state.showTimelineHeaders)
            .putString("timeline_date_source", state.timelineDateSource.name)
            .putString("timeline_date_format", state.timelineDateFormat.name)
            .putString("custom_timeline_date_format", state.customTimelineDateFormat)
            .putBoolean("use_relative_dates", state.useRelativeDates)
            .putBoolean("show_day_of_week", state.showDayOfWeek)
            .putBoolean("abbreviate_day_of_week", state.abbreviateDayOfWeek)
            .putBoolean("smart_year_hiding", state.smartYearHiding)
            .putBoolean("show_video_duration_badge", state.showVideoDurationBadge)
            .putBoolean("show_media_format_badge", state.showMediaFormatBadge)
            .putBoolean("show_album_count", state.showAlbumCount)
            .putBoolean("show_album_labels", state.showAlbumLabels)
            .putBoolean("auto_play_video", state.autoPlayVideo)
            .putBoolean("loop_video", state.loopVideo)
            .putBoolean("video_double_tap_to_zoom", state.videoDoubleTapToZoom)
            .putBoolean("video_gesture_controls", state.videoGestureControls)
            .putBoolean("show_viewer_user_comments", state.showViewerUserComments)
            .putString("viewer_header_style", state.viewerHeaderStyle.name)
            .putBoolean("show_viewer_page_count", state.showViewerPageCount)
            .putBoolean("show_viewer_time", state.showViewerTime)
            .putBoolean("show_filmstrip", state.showFilmstrip)
            .putBoolean("dismissed_filmstrip_tip", state.dismissedFilmstripTip)
            .putBoolean("pinch_to_rotate", state.pinchToRotate)
            .putBoolean("show_image_rotate_button", state.showImageRotateButton)
            .putBoolean("dismissed_rotate_tip", state.dismissedRotateTip)
            .putFloat("double_tap_zoom_level", state.doubleTapZoomLevel)
            .putBoolean("multi_stage_zoom_v2", state.multiStageZoom)
            .putString("startup_tab", state.startupTab.name)
            .putInt("media_filter", state.mediaFilter)
            .putBoolean("show_media_tab", state.showMediaTab)
            .putBoolean("show_albums_tab", state.showAlbumsTab)
            .putBoolean("show_favorites_tab", state.showFavoritesTab)
            .putBoolean("show_library_tab", state.showLibraryTab)
            .putBoolean("show_vault_tab", state.showVaultTab)
            .putBoolean("show_album_filter_chips", state.showAlbumFilterChips)
            .putBoolean("tilt_nav_icons", state.tiltNavIcons)
            .putString("nav_bar_style", state.navBarStyle.name)
            .putString("nav_label_mode", state.navLabelMode.name)
            .putBoolean("use_floating_selection_toolbar", state.useFloatingSelectionToolbar)
            .putString("selection_pill_actions", state.selectionPillActions.joinToString(","))
            .putString("album_picker_sort", state.albumPickerSort.name)
            .putBoolean("biometric_lock_enabled", state.biometricLockEnabled)
            .putBoolean("vault_hide_from_storage", state.vaultHideFromStorage)
            .putBoolean("app_lock_enabled", state.appLockEnabled)
            .putString("app_lock_pin_hash", state.appLockPinHash)
            .putString("app_lock_pin_salt", state.appLockPinSalt)
            .putInt("app_lock_failed_attempts", state.appLockFailedAttempts)
            .putLong("app_lock_lockout_until", state.appLockLockoutUntil)
            .putBoolean("app_lock_biometrics_enabled", state.appLockBiometricsEnabled)
            .putBoolean("confirm_delete", state.confirmDelete)
            .putString("delete_mode", state.deleteMode.name)
            .putBoolean("use_system_trash", state.useSystemTrash)
            .putBoolean("video_muted", state.videoMuted)
            .putString("preferred_editor", state.preferredEditor.name)
            .putString("preferred_editor_component", state.preferredEditorComponent)
            .putString("last_used_editor_id", state.lastUsedEditorId)
            .putString("preferred_video_editor_component", state.preferredVideoEditorComponent)
            .putString("last_used_video_editor_id", state.lastUsedVideoEditorId)
            .putBoolean("editor_overwrite_original", state.editorOverwriteOriginal)
            .putString("app_language", state.language)
            .putBoolean("first_launch_lang_done", state.firstLaunchLanguageSetupDone)
            .putBoolean("memories_notification_enabled", state.memoriesNotificationEnabled)
            .putInt("memories_notification_hour", state.memoriesNotificationHour)
            .putInt("memories_notification_minute", state.memoriesNotificationMinute)
            .putBoolean("dismissed_memories_tip", state.dismissedMemoriesTip)
            .putString("secure_sharing_mode", state.secureSharingMode.name)
            .putBoolean("dismissed_secure_sharing_tip", state.dismissedSecureSharingTip)
            .putBoolean("auto_backup_enabled", state.autoBackupEnabled)
            .putString("auto_backup_tree_uri", state.autoBackupTreeUri)
            .putString("auto_backup_folder_label", state.autoBackupFolderLabel)
            .putString("auto_backup_frequency", state.autoBackupFrequency.name)
            .putInt("auto_backup_retention", state.autoBackupRetention)
            .putLong("last_auto_backup_timestamp", state.lastAutoBackupTimestamp)
            .putString("last_auto_backup_error", state.lastAutoBackupError)
            .putString("last_auto_backup_hash", state.lastAutoBackupHash)
            .putInt("auto_backup_consecutive_failures", state.autoBackupConsecutiveFailures)
            .apply()
    }
}