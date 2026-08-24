package com.iris.gallery.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentColor { MATERIAL_YOU, IRIS, LAPIS_MESOPOTAMIA, EMERALD, ISHTAR_AMBER, ROSE }
enum class CornerStyle(val dp: Int) { SHARP(0), CLASSIC(4), ROUNDED(12), SQUIRCLE(18) }
enum class GridSpacing(val dp: Int) { COMPACT(2), STANDARD(4), RELAXED(8) }
enum class StartupTab(val pageIndex: Int) { PHOTOS(0), ALBUMS(1), FAVORITES(2), LIBRARY(3) }

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val accentColor: AccentColor = AccentColor.MATERIAL_YOU,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val gridSpacing: GridSpacing = GridSpacing.STANDARD,
    val photoGridSize: Float = 105f,
    val albumGridSize: Float = 156f,
    val showTimelineHeaders: Boolean = true,
    val showVideoDurationBadge: Boolean = true,
    val showMediaFormatBadge: Boolean = true,
    val showAlbumCount: Boolean = true,
    val autoPlayVideo: Boolean = true,
    val loopVideo: Boolean = true,
    val doubleTapZoomLevel: Float = 2.5f,
    val startupTab: StartupTab = StartupTab.PHOTOS,
    val biometricLockEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val appLockPinSalt: String = "",
    val appLockBiometricsEnabled: Boolean = true,
) {
    val hasPin: Boolean get() = appLockPinHash.isNotEmpty()
}

class SettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("iris_gallery_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update { copy(themeMode = mode) }
    fun setAmoledBlack(enabled: Boolean) = update { copy(amoledBlack = enabled) }
    fun setAccentColor(color: AccentColor) = update { copy(accentColor = color) }
    fun setCornerStyle(style: CornerStyle) = update { copy(cornerStyle = style) }
    fun setGridSpacing(spacing: GridSpacing) = update { copy(gridSpacing = spacing) }
    fun setPhotoGridSize(size: Float) = update { copy(photoGridSize = size.coerceIn(60f, 220f)) }
    fun setAlbumGridSize(size: Float) = update { copy(albumGridSize = size.coerceIn(70f, 340f)) }
    fun setShowTimelineHeaders(show: Boolean) = update { copy(showTimelineHeaders = show) }
    fun setShowVideoDurationBadge(show: Boolean) = update { copy(showVideoDurationBadge = show) }
    fun setShowMediaFormatBadge(show: Boolean) = update { copy(showMediaFormatBadge = show) }
    fun setShowAlbumCount(show: Boolean) = update { copy(showAlbumCount = show) }
    fun setAutoPlayVideo(autoPlay: Boolean) = update { copy(autoPlayVideo = autoPlay) }
    fun setLoopVideo(loop: Boolean) = update { copy(loopVideo = loop) }
    fun setDoubleTapZoomLevel(level: Float) = update { copy(doubleTapZoomLevel = level) }
    fun setStartupTab(tab: StartupTab) = update { copy(startupTab = tab) }
    fun setBiometricLockEnabled(enabled: Boolean) = update { copy(biometricLockEnabled = enabled) }
    fun setAppLockEnabled(enabled: Boolean) = update { copy(appLockEnabled = enabled) }
    fun setAppLockBiometricsEnabled(enabled: Boolean) = update { copy(appLockBiometricsEnabled = enabled) }

    fun setPin(pin: String) {
        val salt = java.util.UUID.randomUUID().toString()
        val hash = hashPin(pin, salt)
        update { copy(appLockPinHash = hash, appLockPinSalt = salt, appLockEnabled = true) }
    }

    fun verifyPin(pin: String): Boolean {
        val current = _state.value
        if (current.appLockPinHash.isEmpty()) return false
        val computed = hashPin(pin, current.appLockPinSalt)
        return computed == current.appLockPinHash
    }

    fun removePin() {
        update { copy(appLockPinHash = "", appLockPinSalt = "", appLockEnabled = false) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun resetToDefaults() {
        _state.value = SettingsState()
        write(_state.value)
    }

    private fun update(transform: SettingsState.() -> SettingsState) {
        _state.value = _state.value.transform()
        write(_state.value)
    }

    private fun read(): SettingsState {
        val themeModeStr = prefs.getString("theme_mode", null)
        val accentStr = prefs.getString("accent_color", null)
        val cornerStr = prefs.getString("corner_style", null)
        val spacingStr = prefs.getString("grid_spacing", null)
        val startupStr = prefs.getString("startup_tab", null)

        return SettingsState(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr.orEmpty()) }.getOrDefault(ThemeMode.SYSTEM),
            amoledBlack = prefs.getBoolean("amoled_black", false),
            accentColor = runCatching { AccentColor.valueOf(accentStr.orEmpty()) }.getOrDefault(AccentColor.MATERIAL_YOU),
            cornerStyle = runCatching { CornerStyle.valueOf(cornerStr.orEmpty()) }.getOrDefault(CornerStyle.ROUNDED),
            gridSpacing = runCatching { GridSpacing.valueOf(spacingStr.orEmpty()) }.getOrDefault(GridSpacing.STANDARD),
            photoGridSize = prefs.getFloat("photo_grid_size", 105f),
            albumGridSize = prefs.getFloat("album_grid_size", 156f),
            showTimelineHeaders = prefs.getBoolean("show_timeline_headers", true),
            showVideoDurationBadge = prefs.getBoolean("show_video_duration_badge", true),
            showMediaFormatBadge = prefs.getBoolean("show_media_format_badge", true),
            showAlbumCount = prefs.getBoolean("show_album_count", true),
            autoPlayVideo = prefs.getBoolean("auto_play_video", true),
            loopVideo = prefs.getBoolean("loop_video", true),
            doubleTapZoomLevel = prefs.getFloat("double_tap_zoom_level", 2.5f),
            startupTab = runCatching { StartupTab.valueOf(startupStr.orEmpty()) }.getOrDefault(StartupTab.PHOTOS),
            biometricLockEnabled = prefs.getBoolean("biometric_lock_enabled", true),
            appLockEnabled = prefs.getBoolean("app_lock_enabled", false),
            appLockPinHash = prefs.getString("app_lock_pin_hash", "").orEmpty(),
            appLockPinSalt = prefs.getString("app_lock_pin_salt", "").orEmpty(),
            appLockBiometricsEnabled = prefs.getBoolean("app_lock_biometrics_enabled", true),
        )
    }

    private fun write(state: SettingsState) {
        prefs.edit()
            .putString("theme_mode", state.themeMode.name)
            .putBoolean("amoled_black", state.amoledBlack)
            .putString("accent_color", state.accentColor.name)
            .putString("corner_style", state.cornerStyle.name)
            .putString("grid_spacing", state.gridSpacing.name)
            .putFloat("photo_grid_size", state.photoGridSize)
            .putFloat("album_grid_size", state.albumGridSize)
            .putBoolean("show_timeline_headers", state.showTimelineHeaders)
            .putBoolean("show_video_duration_badge", state.showVideoDurationBadge)
            .putBoolean("show_media_format_badge", state.showMediaFormatBadge)
            .putBoolean("show_album_count", state.showAlbumCount)
            .putBoolean("auto_play_video", state.autoPlayVideo)
            .putBoolean("loop_video", state.loopVideo)
            .putFloat("double_tap_zoom_level", state.doubleTapZoomLevel)
            .putString("startup_tab", state.startupTab.name)
            .putBoolean("biometric_lock_enabled", state.biometricLockEnabled)
            .putBoolean("app_lock_enabled", state.appLockEnabled)
            .putString("app_lock_pin_hash", state.appLockPinHash)
            .putString("app_lock_pin_salt", state.appLockPinSalt)
            .putBoolean("app_lock_biometrics_enabled", state.appLockBiometricsEnabled)
            .apply()
    }
}
