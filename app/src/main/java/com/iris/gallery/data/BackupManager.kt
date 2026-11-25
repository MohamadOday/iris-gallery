package com.iris.gallery.data

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val IRIS_BACKUP_FORMAT = "iris-gallery-backup"
const val IRIS_BACKUP_VERSION = 1

data class BackupPreview(
    val exportedAt: Long,
    val albumGroupsCount: Int,
    val pinnedAlbumsCount: Int,
    val excludedFoldersCount: Int,
    val favoritesCount: Int,
    val rawJson: String,
)

data class BackupRestoreSummary(
    val albumGroupsRestored: Int,
    val pinnedAlbumsRestored: Int,
    val excludedFoldersRestored: Int,
    val favoritesRestored: Int,
    val restoredLanguage: String = "",
)

data class RestoredBackupPayload(
    val settings: SettingsState,
    val library: LibraryPreferencesState,
    val customTitles: Map<Long, String>,
    val favorites: Set<Long>,
    val summary: BackupRestoreSummary,
)

data class BuiltBackup(
    val json: String,
    val contentHash: String,
)

private data class AlbumLookupEntry(
    val id: Long,
    val folderPath: String,
    val relativePath: String,
)

object BackupManager {

    fun defaultBackupFileName(): String {
        val stamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "iris-gallery-backup-$stamp.json"
    }

    fun buildBackupJson(
        settings: SettingsState,
        library: LibraryPreferencesState,
        customTitles: Map<Long, String>,
        favorites: Set<Long>,
        images: List<MediaImage>,
    ): String = buildBackup(settings, library, customTitles, favorites, images).json

    fun buildBackup(
        settings: SettingsState,
        library: LibraryPreferencesState,
        customTitles: Map<Long, String>,
        favorites: Set<Long>,
        images: List<MediaImage>,
    ): BuiltBackup {
        val imagesById = images.associateBy { it.id }
        val albumsById = buildAlbumLookup(images, settings.albumHierarchyDepth).associateBy { it.id }

        val settingsJson = serializeSettings(settings)
        val libraryJson = serializeLibrary(library, customTitles, imagesById, albumsById)
        val favoritesJson = serializeMediaRefs(favorites.sorted(), imagesById)

        // Hash the payload without exportedAt so identical states skip redundant disk/sync writes.
        val canonical = JSONObject().apply {
            put("format", IRIS_BACKUP_FORMAT)
            put("version", IRIS_BACKUP_VERSION)
            put("settings", settingsJson)
            put("library", libraryJson)
            put("favorites", favoritesJson)
        }
        val hash = sha256Hex(canonical.toString())

        val root = JSONObject().apply {
            put("format", IRIS_BACKUP_FORMAT)
            put("version", IRIS_BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("settings", settingsJson)
            put("library", libraryJson)
            put("favorites", favoritesJson)
        }
        return BuiltBackup(json = root.toString(2), contentHash = hash)
    }

    fun parseBackupPreview(rawJson: String): Result<BackupPreview> = runCatching {
        val root = parseValidatedRoot(rawJson)
        val libraryObj = root.optJSONObject("library")
        val albumGroupsCount = libraryObj?.optJSONArray("albumGroups")?.length() ?: 0
        val pinnedAlbumsCount = libraryObj?.optJSONArray("pinnedAlbums")?.length() ?: 0
        val excludedFoldersCount = libraryObj?.optJSONArray("excludedFolders")?.length() ?: 0
        val favoritesCount = root.optJSONArray("favorites")?.length() ?: 0
        BackupPreview(
            exportedAt = root.optLong("exportedAt", 0L),
            albumGroupsCount = albumGroupsCount,
            pinnedAlbumsCount = pinnedAlbumsCount,
            excludedFoldersCount = excludedFoldersCount,
            favoritesCount = favoritesCount,
            rawJson = rawJson,
        )
    }

    fun restoreFromJson(
        rawJson: String,
        currentSettings: SettingsState,
        currentLibrary: LibraryPreferencesState,
        images: List<MediaImage>,
        hasAllFilesAccess: Boolean,
    ): Result<RestoredBackupPayload> = runCatching {
        val root = parseValidatedRoot(rawJson)

        val settingsObj = root.optJSONObject("settings")
        val restoredSettings = if (settingsObj != null) {
            deserializeSettings(settingsObj, currentSettings, hasAllFilesAccess)
        } else {
            currentSettings
        }

        val albumLookup = buildAlbumLookup(images, restoredSettings.albumHierarchyDepth)
        val albumResolver = AlbumRefResolver(albumLookup)
        val mediaResolver = MediaRefResolver(images)

        val libraryObj = root.optJSONObject("library")
        val (restoredLibrary, restoredTitles) = if (libraryObj != null) {
            deserializeLibrary(libraryObj, currentLibrary, albumResolver, mediaResolver)
        } else {
            currentLibrary to emptyMap()
        }

        val favoritesArr = root.optJSONArray("favorites")
        val restoredFavorites = if (favoritesArr != null) {
            parseMediaRefIds(favoritesArr, mediaResolver).toSet()
        } else {
            emptySet()
        }

        RestoredBackupPayload(
            settings = restoredSettings,
            library = restoredLibrary,
            customTitles = restoredTitles,
            favorites = restoredFavorites,
            summary = BackupRestoreSummary(
                albumGroupsRestored = restoredLibrary.albumGroups.size,
                pinnedAlbumsRestored = restoredLibrary.pinnedAlbums.size,
                excludedFoldersRestored = restoredLibrary.excludedFolders.size,
                favoritesRestored = restoredFavorites.size,
                restoredLanguage = restoredSettings.language,
            ),
        )
    }

    private fun parseValidatedRoot(rawJson: String): JSONObject {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Backup file is empty" }
        val root = JSONObject(trimmed)
        val format = root.optString("format", "")
        require(format == IRIS_BACKUP_FORMAT) { "Unrecognized backup format" }
        val version = root.optInt("version", -1)
        require(version in 1..IRIS_BACKUP_VERSION) { "Unsupported backup version: $version" }
        return root
    }

    private fun serializeSettings(s: SettingsState): JSONObject = JSONObject().apply {
        put("themeMode", s.themeMode.name)
        put("amoledBlack", s.amoledBlack)
        put("accentColor", s.accentColor.name)
        put("cornerStyle", s.cornerStyle.name)
        put("gridSpacing", s.gridSpacing.name)
        put("photoGridLayout", s.photoGridLayout.name)
        put("animateGifThumbnails", s.animateGifThumbnails)
        put("showHiddenItems", s.showHiddenItems)
        put("photoGridSize", s.photoGridSize.toDouble())
        put("mosaicRowSize", s.mosaicRowSize.toDouble())
        put("albumGridSize", s.albumGridSize.toDouble())
        put("albumsViewMode", s.albumsViewMode.name)
        put("groupDirectSubfolders", s.groupDirectSubfolders)
        put("albumHierarchyDepth", s.albumHierarchyDepth)
        put("globalAlbumSearch", s.globalAlbumSearch)
        put("showTimelineHeaders", s.showTimelineHeaders)
        put("timelineDateFormat", s.timelineDateFormat.name)
        put("customTimelineDateFormat", s.customTimelineDateFormat)
        put("useRelativeDates", s.useRelativeDates)
        put("showDayOfWeek", s.showDayOfWeek)
        put("abbreviateDayOfWeek", s.abbreviateDayOfWeek)
        put("smartYearHiding", s.smartYearHiding)
        put("showVideoDurationBadge", s.showVideoDurationBadge)
        put("showMediaFormatBadge", s.showMediaFormatBadge)
        put("showAlbumCount", s.showAlbumCount)
        put("showAlbumLabels", s.showAlbumLabels)
        put("autoPlayVideo", s.autoPlayVideo)
        put("loopVideo", s.loopVideo)
        put("videoDoubleTapToZoom", s.videoDoubleTapToZoom)
        put("videoGestureControls", s.videoGestureControls)
        put("showViewerUserComments", s.showViewerUserComments)
        put("viewerHeaderStyle", s.viewerHeaderStyle.name)
        put("showViewerPageCount", s.showViewerPageCount)
        put("showViewerTime", s.showViewerTime)
        put("showFilmstrip", s.showFilmstrip)
        put("pinchToRotate", s.pinchToRotate)
        put("showImageRotateButton", s.showImageRotateButton)
        put("doubleTapZoomLevel", s.doubleTapZoomLevel.toDouble())
        put("multiStageZoom", s.multiStageZoom)
        put("startupTab", s.startupTab.name)
        put("mediaFilter", s.mediaFilter)
        put("showMediaTab", s.showMediaTab)
        put("showAlbumsTab", s.showAlbumsTab)
        put("showFavoritesTab", s.showFavoritesTab)
        put("showLibraryTab", s.showLibraryTab)
        put("showVaultTab", s.showVaultTab)
        put("showAlbumFilterChips", s.showAlbumFilterChips)
        put("tiltNavIcons", s.tiltNavIcons)
        put("navBarStyle", s.navBarStyle.name)
        put("navLabelMode", s.navLabelMode.name)
        put("useFloatingSelectionToolbar", s.useFloatingSelectionToolbar)
        put("selectionPillActions", JSONArray(s.selectionPillActions))
        put("albumPickerSort", s.albumPickerSort.name)
        put("biometricLockEnabled", s.biometricLockEnabled)
        put("vaultHideFromStorage", s.vaultHideFromStorage)
        put("appLockBiometricsEnabled", s.appLockBiometricsEnabled)
        put("confirmDelete", s.confirmDelete)
        put("deleteMode", s.deleteMode.name)
        put("useSystemTrash", s.useSystemTrash)
        put("videoMuted", s.videoMuted)
        put("preferredEditor", s.preferredEditor.name)
        put("editorOverwriteOriginal", s.editorOverwriteOriginal)
        put("language", s.language)
        put("memoriesNotificationEnabled", s.memoriesNotificationEnabled)
        put("memoriesNotificationHour", s.memoriesNotificationHour)
        put("memoriesNotificationMinute", s.memoriesNotificationMinute)
        put("secureSharingMode", s.secureSharingMode.name)
    }

    private fun deserializeSettings(
        obj: JSONObject,
        current: SettingsState,
        hasAllFilesAccess: Boolean,
    ): SettingsState {
        val defaults = SettingsState()
        val rawCustomDate = obj.optString("customTimelineDateFormat", defaults.customTimelineDateFormat)
            .trim()
            .ifBlank { defaults.customTimelineDateFormat }
        val safeCustomDate = runCatching {
            val fmt = DateTimeFormatter.ofPattern(rawCustomDate, Locale.getDefault())
            LocalDate.now().format(fmt)
            rawCustomDate
        }.getOrDefault(defaults.customTimelineDateFormat)

        val rawPillActions = obj.optJSONArray("selectionPillActions")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i)
                    if (value in ALL_SELECTION_PILL_ACTIONS) add(value)
                }
            }.distinct()
        }.orEmpty()
        val pillActions = if (rawPillActions.isEmpty()) DEFAULT_SELECTION_PILL_ACTIONS else rawPillActions

        var showMediaTab = obj.optBoolean("showMediaTab", defaults.showMediaTab)
        val showAlbumsTab = obj.optBoolean("showAlbumsTab", defaults.showAlbumsTab)
        val showFavoritesTab = obj.optBoolean("showFavoritesTab", defaults.showFavoritesTab)
        val showLibraryTab = obj.optBoolean("showLibraryTab", defaults.showLibraryTab)
        val showVaultTab = obj.optBoolean("showVaultTab", defaults.showVaultTab)
        if (!showMediaTab && !showAlbumsTab && !showFavoritesTab && !showLibraryTab && !showVaultTab) {
            showMediaTab = true
        }

        val parsedStartupTab = parseEnum(obj.optString("startupTab"), defaults.startupTab)
        val startupTab = if (!showVaultTab && parsedStartupTab == StartupTab.VAULT) {
            StartupTab.MEDIA
        } else {
            parsedStartupTab
        }

        val rawShowHidden = obj.optBoolean("showHiddenItems", defaults.showHiddenItems)
        val safeShowHidden = if (rawShowHidden && Build.VERSION.SDK_INT >= 30 && !hasAllFilesAccess) {
            false
        } else {
            rawShowHidden
        }

        val rawLang = obj.optString("language", defaults.language)
        val safeLang = if (SUPPORTED_LANGUAGES.any { it.code == rawLang }) rawLang else defaults.language

        return current.copy(
            themeMode = parseEnum(obj.optString("themeMode"), defaults.themeMode),
            amoledBlack = obj.optBoolean("amoledBlack", defaults.amoledBlack),
            accentColor = parseEnum(obj.optString("accentColor"), defaults.accentColor),
            cornerStyle = parseEnum(obj.optString("cornerStyle"), defaults.cornerStyle),
            gridSpacing = parseEnum(obj.optString("gridSpacing"), defaults.gridSpacing),
            photoGridLayout = parseEnum(obj.optString("photoGridLayout"), defaults.photoGridLayout),
            animateGifThumbnails = obj.optBoolean("animateGifThumbnails", defaults.animateGifThumbnails),
            showHiddenItems = safeShowHidden,
            photoGridSize = obj.optDouble("photoGridSize", defaults.photoGridSize.toDouble()).toFloat().coerceIn(36f, 340f),
            mosaicRowSize = obj.optDouble("mosaicRowSize", defaults.mosaicRowSize.toDouble()).toFloat().coerceIn(56f, 260f),
            albumGridSize = obj.optDouble("albumGridSize", defaults.albumGridSize.toDouble()).toFloat().coerceIn(48f, 450f),
            albumsViewMode = parseEnum(obj.optString("albumsViewMode"), defaults.albumsViewMode),
            groupDirectSubfolders = obj.optBoolean("groupDirectSubfolders", defaults.groupDirectSubfolders),
            albumHierarchyDepth = obj.optInt("albumHierarchyDepth", defaults.albumHierarchyDepth).coerceIn(0, 4),
            globalAlbumSearch = obj.optBoolean("globalAlbumSearch", defaults.globalAlbumSearch),
            showTimelineHeaders = obj.optBoolean("showTimelineHeaders", defaults.showTimelineHeaders),
            timelineDateFormat = parseEnum(obj.optString("timelineDateFormat"), defaults.timelineDateFormat),
            customTimelineDateFormat = safeCustomDate,
            useRelativeDates = obj.optBoolean("useRelativeDates", defaults.useRelativeDates),
            showDayOfWeek = obj.optBoolean("showDayOfWeek", defaults.showDayOfWeek),
            abbreviateDayOfWeek = obj.optBoolean("abbreviateDayOfWeek", defaults.abbreviateDayOfWeek),
            smartYearHiding = obj.optBoolean("smartYearHiding", defaults.smartYearHiding),
            showVideoDurationBadge = obj.optBoolean("showVideoDurationBadge", defaults.showVideoDurationBadge),
            showMediaFormatBadge = obj.optBoolean("showMediaFormatBadge", defaults.showMediaFormatBadge),
            showAlbumCount = obj.optBoolean("showAlbumCount", defaults.showAlbumCount),
            showAlbumLabels = obj.optBoolean("showAlbumLabels", defaults.showAlbumLabels),
            autoPlayVideo = obj.optBoolean("autoPlayVideo", defaults.autoPlayVideo),
            loopVideo = obj.optBoolean("loopVideo", defaults.loopVideo),
            videoDoubleTapToZoom = obj.optBoolean("videoDoubleTapToZoom", defaults.videoDoubleTapToZoom),
            videoGestureControls = obj.optBoolean("videoGestureControls", defaults.videoGestureControls),
            showViewerUserComments = obj.optBoolean("showViewerUserComments", defaults.showViewerUserComments),
            viewerHeaderStyle = parseEnum(obj.optString("viewerHeaderStyle"), defaults.viewerHeaderStyle),
            showViewerPageCount = obj.optBoolean("showViewerPageCount", defaults.showViewerPageCount),
            showViewerTime = obj.optBoolean("showViewerTime", defaults.showViewerTime),
            showFilmstrip = obj.optBoolean("showFilmstrip", defaults.showFilmstrip),
            pinchToRotate = obj.optBoolean("pinchToRotate", defaults.pinchToRotate),
            showImageRotateButton = obj.optBoolean("showImageRotateButton", defaults.showImageRotateButton),
            doubleTapZoomLevel = obj.optDouble("doubleTapZoomLevel", defaults.doubleTapZoomLevel.toDouble()).toFloat().coerceIn(1.5f, 5.0f),
            multiStageZoom = obj.optBoolean("multiStageZoom", defaults.multiStageZoom),
            startupTab = startupTab,
            mediaFilter = obj.optInt("mediaFilter", defaults.mediaFilter).coerceIn(0, 2),
            showMediaTab = showMediaTab,
            showAlbumsTab = showAlbumsTab,
            showFavoritesTab = showFavoritesTab,
            showLibraryTab = showLibraryTab,
            showVaultTab = showVaultTab,
            showAlbumFilterChips = obj.optBoolean("showAlbumFilterChips", defaults.showAlbumFilterChips),
            tiltNavIcons = obj.optBoolean("tiltNavIcons", defaults.tiltNavIcons),
            navBarStyle = parseEnum(obj.optString("navBarStyle"), defaults.navBarStyle),
            navLabelMode = parseEnum(obj.optString("navLabelMode"), defaults.navLabelMode),
            useFloatingSelectionToolbar = obj.optBoolean("useFloatingSelectionToolbar", defaults.useFloatingSelectionToolbar),
            selectionPillActions = pillActions,
            albumPickerSort = parseEnum(obj.optString("albumPickerSort"), defaults.albumPickerSort),
            biometricLockEnabled = obj.optBoolean("biometricLockEnabled", defaults.biometricLockEnabled),
            vaultHideFromStorage = obj.optBoolean("vaultHideFromStorage", defaults.vaultHideFromStorage),
            appLockBiometricsEnabled = obj.optBoolean("appLockBiometricsEnabled", defaults.appLockBiometricsEnabled),
            confirmDelete = obj.optBoolean("confirmDelete", defaults.confirmDelete),
            deleteMode = parseEnum(obj.optString("deleteMode"), defaults.deleteMode),
            useSystemTrash = obj.optBoolean("useSystemTrash", defaults.useSystemTrash),
            videoMuted = obj.optBoolean("videoMuted", defaults.videoMuted),
            preferredEditor = parseEnum(obj.optString("preferredEditor"), defaults.preferredEditor),
            editorOverwriteOriginal = obj.optBoolean("editorOverwriteOriginal", defaults.editorOverwriteOriginal),
            language = safeLang,
            memoriesNotificationEnabled = obj.optBoolean("memoriesNotificationEnabled", defaults.memoriesNotificationEnabled),
            memoriesNotificationHour = obj.optInt("memoriesNotificationHour", defaults.memoriesNotificationHour).coerceIn(0, 23),
            memoriesNotificationMinute = obj.optInt("memoriesNotificationMinute", defaults.memoriesNotificationMinute).coerceIn(0, 59),
            secureSharingMode = parseEnum(obj.optString("secureSharingMode"), defaults.secureSharingMode),
        )
    }

    private fun serializeLibrary(
        library: LibraryPreferencesState,
        customTitles: Map<Long, String>,
        imagesById: Map<Long, MediaImage>,
        albumsById: Map<Long, AlbumLookupEntry>,
    ): JSONObject = JSONObject().apply {
        put("libraryOrder", JSONArray(library.libraryOrder))
        put("albumSort", library.albumSort.name)
        put("albumMediaSort", library.albumMediaSort.name)
        put("vaultAlbumSort", library.vaultAlbumSort.name)
        put("vaultMediaSort", library.vaultMediaSort.name)
        put("excludedFolders", JSONArray(library.excludedFolders.sorted()))
        put("pinnedAlbums", serializeAlbumRefs(library.pinnedAlbums, albumsById))
        put("albumOrder", serializeAlbumRefs(library.albumOrder, albumsById))

        val coversArr = JSONArray()
        for ((albumId, mediaId) in library.albumCovers) {
            val album = albumsById[albumId]
            val media = imagesById[mediaId]
            coversArr.put(JSONObject().apply {
                put("albumId", albumId)
                if (!album?.folderPath.isNullOrBlank()) put("albumPath", album!!.folderPath)
                if (!album?.relativePath.isNullOrBlank()) put("albumRelativePath", album!!.relativePath)
                put("mediaId", mediaId)
                if (!media?.path.isNullOrBlank()) put("mediaPath", media!!.path)
                val rel = toRelativeStoragePath(media?.path.orEmpty())
                if (rel.isNotBlank()) put("mediaRelativePath", rel)
            })
        }
        put("albumCovers", coversArr)

        val overridesArr = JSONArray()
        for ((albumId, sort) in library.albumMediaSortOverrides) {
            val album = albumsById[albumId]
            overridesArr.put(JSONObject().apply {
                put("albumId", albumId)
                if (!album?.folderPath.isNullOrBlank()) put("albumPath", album!!.folderPath)
                if (!album?.relativePath.isNullOrBlank()) put("albumRelativePath", album!!.relativePath)
                put("sort", sort.name)
            })
        }
        put("albumMediaSortOverrides", overridesArr)

        val groupsArr = JSONArray()
        for (group in library.albumGroups) {
            val coverMedia = group.coverMediaId?.let { imagesById[it] }
            groupsArr.put(JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("albums", serializeAlbumRefs(group.albumIds, albumsById))
                if (group.coverMediaId != null) {
                    put("coverMediaId", group.coverMediaId)
                    if (!coverMedia?.path.isNullOrBlank()) put("coverMediaPath", coverMedia!!.path)
                    val rel = toRelativeStoragePath(coverMedia?.path.orEmpty())
                    if (rel.isNotBlank()) put("coverMediaRelativePath", rel)
                }
            })
        }
        put("albumGroups", groupsArr)

        val titlesArr = JSONArray()
        for ((mediaId, title) in customTitles) {
            val clean = title.trim()
            if (clean.isEmpty()) continue
            val media = imagesById[mediaId]
            titlesArr.put(JSONObject().apply {
                put("mediaId", mediaId)
                if (!media?.path.isNullOrBlank()) put("mediaPath", media!!.path)
                val rel = toRelativeStoragePath(media?.path.orEmpty())
                if (rel.isNotBlank()) put("mediaRelativePath", rel)
                put("title", clean)
            })
        }
        put("customTitles", titlesArr)
    }

    private fun deserializeLibrary(
        obj: JSONObject,
        current: LibraryPreferencesState,
        albumResolver: AlbumRefResolver,
        mediaResolver: MediaRefResolver,
    ): Pair<LibraryPreferencesState, Map<Long, String>> {
        val rawLibraryOrder = obj.optJSONArray("libraryOrder")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        } ?: current.libraryOrder

        val excludedFolders = obj.optJSONArray("excludedFolders")?.let { arr ->
            buildSet {
                for (i in 0 until arr.length()) {
                    val clean = arr.optString(i).trim().removeSuffix("/")
                    if (clean.isNotBlank()) add(clean)
                }
            }
        } ?: emptySet()

        val pinnedAlbums = obj.optJSONArray("pinnedAlbums")?.let { arr ->
            parseAlbumRefIds(arr, albumResolver).toSet()
        } ?: emptySet()

        val albumOrder = obj.optJSONArray("albumOrder")?.let { arr ->
            parseAlbumRefIds(arr, albumResolver).distinct()
        } ?: emptyList()

        val albumCovers = buildMap<Long, Long> {
            val arr = obj.optJSONArray("albumCovers") ?: return@buildMap
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val albumId = albumResolver.resolve(
                    id = entry.optLongOrNull("albumId"),
                    path = entry.optString("albumPath", ""),
                    relativePath = entry.optString("albumRelativePath", ""),
                ) ?: continue
                val mediaId = mediaResolver.resolve(
                    id = entry.optLongOrNull("mediaId"),
                    path = entry.optString("mediaPath", ""),
                    relativePath = entry.optString("mediaRelativePath", ""),
                ) ?: continue
                put(albumId, mediaId)
            }
        }

        val sortOverrides = buildMap<Long, MediaSort> {
            val arr = obj.optJSONArray("albumMediaSortOverrides") ?: return@buildMap
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val albumId = albumResolver.resolve(
                    id = entry.optLongOrNull("albumId"),
                    path = entry.optString("albumPath", ""),
                    relativePath = entry.optString("albumRelativePath", ""),
                ) ?: continue
                val sort = runCatching { MediaSort.valueOf(entry.optString("sort", "")) }.getOrNull() ?: continue
                put(albumId, sort)
            }
        }

        val usedGroupAlbums = mutableSetOf<Long>()
        val albumGroups = buildList {
            val arr = obj.optJSONArray("albumGroups") ?: return@buildList
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val id = entry.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() }
                val name = entry.optString("name", "").trim().ifBlank { continue }
                val albumsArr = entry.optJSONArray("albums") ?: continue
                val memberIds = parseAlbumRefIds(albumsArr, albumResolver)
                    .distinct()
                    .filter { usedGroupAlbums.add(it) }
                if (memberIds.isEmpty()) continue
                val coverId = mediaResolver.resolve(
                    id = entry.optLongOrNull("coverMediaId"),
                    path = entry.optString("coverMediaPath", ""),
                    relativePath = entry.optString("coverMediaRelativePath", ""),
                )
                add(AlbumGroup(id = id, name = name, albumIds = memberIds, coverMediaId = coverId))
            }
        }

        val customTitles = buildMap<Long, String> {
            val arr = obj.optJSONArray("customTitles") ?: return@buildMap
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val title = entry.optString("title", "").trim().ifBlank { continue }
                val mediaId = mediaResolver.resolve(
                    id = entry.optLongOrNull("mediaId"),
                    path = entry.optString("mediaPath", ""),
                    relativePath = entry.optString("mediaRelativePath", ""),
                ) ?: continue
                put(mediaId, title)
            }
        }

        val state = LibraryPreferencesState(
            lockedMedia = current.lockedMedia,
            pinnedAlbums = pinnedAlbums,
            albumCovers = albumCovers,
            albumOrder = albumOrder,
            libraryOrder = normalizeLibraryOrder(rawLibraryOrder),
            albumSort = parseEnum(obj.optString("albumSort"), AlbumSort.NEWEST),
            albumMediaSort = parseEnum(obj.optString("albumMediaSort"), MediaSort.DATE_DESC),
            albumMediaSortOverrides = sortOverrides,
            excludedFolders = excludedFolders,
            albumGroups = albumGroups,
            vaultAlbumSort = parseEnum(obj.optString("vaultAlbumSort"), AlbumSort.NEWEST),
            vaultMediaSort = parseEnum(obj.optString("vaultMediaSort"), MediaSort.DATE_DESC),
        )
        return state to customTitles
    }

    private fun serializeAlbumRefs(
        albumIds: Collection<Long>,
        albumsById: Map<Long, AlbumLookupEntry>,
    ): JSONArray = JSONArray().apply {
        for (id in albumIds) {
            val info = albumsById[id]
            put(JSONObject().apply {
                put("id", id)
                if (!info?.folderPath.isNullOrBlank()) put("path", info!!.folderPath)
                if (!info?.relativePath.isNullOrBlank()) put("relativePath", info!!.relativePath)
            })
        }
    }

    private fun serializeMediaRefs(
        mediaIds: Collection<Long>,
        imagesById: Map<Long, MediaImage>,
    ): JSONArray = JSONArray().apply {
        for (id in mediaIds) {
            val media = imagesById[id]
            put(JSONObject().apply {
                put("id", id)
                if (!media?.path.isNullOrBlank()) put("path", media!!.path)
                val rel = toRelativeStoragePath(media?.path.orEmpty())
                if (rel.isNotBlank()) put("relativePath", rel)
            })
        }
    }

    private fun parseAlbumRefIds(arr: JSONArray, resolver: AlbumRefResolver): List<Long> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            val resolved = if (obj != null) {
                resolver.resolve(
                    id = obj.optLongOrNull("id"),
                    path = obj.optString("path", ""),
                    relativePath = obj.optString("relativePath", ""),
                )
            } else {
                arr.optLong(i).takeIf { it != 0L }
            }
            if (resolved != null) add(resolved)
        }
    }

    private fun parseMediaRefIds(
        arr: JSONArray,
        resolver: MediaRefResolver,
    ): List<Long> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            val resolved = if (obj != null) {
                resolver.resolve(
                    id = obj.optLongOrNull("id"),
                    path = obj.optString("path", ""),
                    relativePath = obj.optString("relativePath", ""),
                )
            } else {
                resolver.resolve(
                    id = arr.optLong(i).takeIf { it != 0L },
                    path = "",
                    relativePath = "",
                )
            }
            if (resolved != null) add(resolved)
        }
    }

    private fun buildAlbumLookup(
        images: List<MediaImage>,
        hierarchyDepth: Int,
    ): List<AlbumLookupEntry> {
        if (images.isEmpty()) return emptyList()
        if (hierarchyDepth <= 0) {
            val seen = LinkedHashMap<Long, AlbumLookupEntry>()
            for (img in images) {
                if (img.bucketId in seen) continue
                val dirPath = img.path.substringBeforeLast('/', "")
                seen[img.bucketId] = AlbumLookupEntry(
                    id = img.bucketId,
                    folderPath = dirPath,
                    relativePath = toRelativeStoragePath(dirPath),
                )
            }
            return seen.values.toList()
        }

        val seen = LinkedHashMap<Long, AlbumLookupEntry>()
        for (img in images) {
            val parsed = extractStorageRootAndSegmentsForBackup(img.path)
            if (parsed != null && parsed.second.size > hierarchyDepth) {
                val (root, segments) = parsed
                val ancestorSegments = segments.take(hierarchyDepth)
                val ancestorPath = if (root.isNotEmpty()) {
                    "$root/${ancestorSegments.joinToString("/")}"
                } else {
                    "/${ancestorSegments.joinToString("/")}"
                }
                val syntheticId = ancestorPath.lowercase(Locale.ROOT).hashCode().toLong()
                if (syntheticId !in seen) {
                    seen[syntheticId] = AlbumLookupEntry(
                        id = syntheticId,
                        folderPath = ancestorPath,
                        relativePath = toRelativeStoragePath(ancestorPath),
                    )
                }
            } else if (img.bucketId !in seen) {
                val dirPath = img.path.substringBeforeLast('/', "")
                seen[img.bucketId] = AlbumLookupEntry(
                    id = img.bucketId,
                    folderPath = dirPath,
                    relativePath = toRelativeStoragePath(dirPath),
                )
            }
        }
        return seen.values.toList()
    }

    private class AlbumRefResolver(entries: List<AlbumLookupEntry>) {
        private val byExactPath = entries
            .filter { it.folderPath.isNotBlank() }
            .associate { it.folderPath to it.id }
        private val byRelativePath = entries
            .filter { it.relativePath.isNotBlank() }
            .associate { it.relativePath.lowercase(Locale.ROOT) to it.id }
        private val knownIds = entries.mapTo(HashSet()) { it.id }

        fun resolve(id: Long?, path: String, relativePath: String): Long? {
            if (path.isNotBlank()) {
                byExactPath[path]?.let { return it }
            }
            val rel = relativePath.ifBlank { toRelativeStoragePath(path) }
            if (rel.isNotBlank()) {
                byRelativePath[rel.lowercase(Locale.ROOT)]?.let { return it }
            }
            // Only fall back to numeric bucket ID if the backup had no path recorded at all.
            if (path.isBlank() && relativePath.isBlank() && id != null && id in knownIds) return id
            return null
        }
    }

    private class MediaRefResolver(images: List<MediaImage>) {
        private val byExactPath = images
            .filter { it.path.isNotBlank() }
            .associate { it.path to it.id }
        private val byRelativePath = images
            .filter { it.path.isNotBlank() }
            .associate { toRelativeStoragePath(it.path).lowercase(Locale.ROOT) to it.id }
        private val knownIds = images.mapTo(HashSet()) { it.id }

        fun resolve(
            id: Long?,
            path: String,
            relativePath: String,
        ): Long? {
            if (path.isNotBlank()) {
                byExactPath[path]?.let { return it }
            }
            val rel = relativePath.ifBlank { toRelativeStoragePath(path) }
            if (rel.isNotBlank()) {
                byRelativePath[rel.lowercase(Locale.ROOT)]?.let { return it }
            }
            // Never fall back to a numeric MediaStore ID when a saved path did not match;
            // IDs are device-local counters and would attach metadata to an unrelated photo.
            if (path.isBlank() && relativePath.isBlank() && id != null && id in knownIds) return id
            return null
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun extractStorageRootAndSegmentsForBackup(path: String): Pair<String, List<String>>? {
        val dir = path.substringBeforeLast('/', "").trimEnd('/')
        if (dir.isBlank()) return null
        val normalized = dir.replace('\\', '/')
        val knownPrefixes = listOf("/storage/emulated/0/", "/sdcard/")
        for (prefix in knownPrefixes) {
            if (normalized.startsWith(prefix, ignoreCase = true)) {
                val root = normalized.substring(0, prefix.length - 1)
                val rel = normalized.substring(prefix.length)
                val segs = rel.split('/').filter { it.isNotBlank() }
                if (segs.isNotEmpty()) return root to segs
            }
        }
        val storageMatch = Regex("^(/storage/[^/]+)/(.+)$").find(normalized)
        if (storageMatch != null) {
            val root = storageMatch.groupValues[1]
            val rel = storageMatch.groupValues[2]
            val segs = rel.split('/').filter { it.isNotBlank() }
            if (segs.isNotEmpty()) return root to segs
        }
        val segs = normalized.trimStart('/').split('/').filter { it.isNotBlank() }
        return if (segs.isNotEmpty()) "" to segs else null
    }

    private fun toRelativeStoragePath(path: String): String {
        if (path.isBlank()) return ""
        val normalized = path.replace('\\', '/').trimEnd('/')
        val knownPrefixes = listOf("/storage/emulated/0/", "/sdcard/")
        for (prefix in knownPrefixes) {
            if (normalized.startsWith(prefix, ignoreCase = true)) {
                return normalized.substring(prefix.length)
            }
        }
        val storageMatch = Regex("^/storage/[^/]+/(.+)$").find(normalized)
        if (storageMatch != null) {
            return storageMatch.groupValues[1]
        }
        return normalized.trimStart('/')
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getLong(key) }.getOrNull()
    }
}
