@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.iris.gallery.ui

import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.res.stringResource
import com.iris.gallery.data.MediaImage
import com.iris.gallery.ui.MediaThumbnail
import com.iris.gallery.data.CornerStyle
import com.iris.gallery.data.GridSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.iris.gallery.data.TimelineDateFormat
import com.iris.gallery.ui.rememberAppLocale
import com.iris.gallery.ui.getTimelineFormatter
import com.iris.gallery.ui.formatTimelineDate
import com.iris.gallery.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhotoGrid(
    images: List<MediaImage>,
    padding: PaddingValues,
    gridState: LazyGridState,
    cellSize: androidx.compose.ui.unit.Dp,
    onCellSizeChange: ((androidx.compose.ui.unit.Dp) -> Unit)? = null,
    showTimeline: Boolean = false,
    showDateBubble: Boolean = showTimeline,
    timelineDateFormat: TimelineDateFormat = TimelineDateFormat.SYSTEM_DEFAULT,
    customTimelineDateFormat: String = "d. MMMM yyyy",
    useRelativeDates: Boolean = true,
    showDayOfWeek: Boolean = false,
    abbreviateDayOfWeek: Boolean = false,
    smartYearHiding: Boolean = true,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    gridSpacing: GridSpacing = GridSpacing.STANDARD,
    showVideoDuration: Boolean = true,
    showFormatBadge: Boolean = true,
    selectedIds: Set<Long> = emptySet(),
    favorites: Set<Long> = emptySet(),
    showFavoriteBadge: Boolean = true,
    onToggleSelection: ((Long) -> Unit)? = null,
    onSetSelection: ((Long, Boolean) -> Unit)? = null,
    onSetBatchSelection: ((Collection<Long>, Boolean) -> Unit)? = null,
    onSetDateSelection: ((List<Long>, Boolean) -> Unit)? = null,
    onInvalidMedia: ((MediaImage) -> Unit)? = null,
    onOpen: (MediaImage) -> Unit,
) {
    val currentLocale = rememberAppLocale()
    val todayString = stringResource(R.string.timeline_today)
    val yesterdayString = stringResource(R.string.timeline_yesterday)

    val sameYearFormatter = remember(currentLocale, timelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, customTimelineDateFormat) {
        getTimelineFormatter(timelineDateFormat, isSameYear = true, showDayOfWeek = showDayOfWeek, locale = currentLocale, customPattern = customTimelineDateFormat, abbreviateDayOfWeek = abbreviateDayOfWeek, smartYearHiding = smartYearHiding)
    }
    val otherYearFormatter = remember(currentLocale, timelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, customTimelineDateFormat) {
        getTimelineFormatter(timelineDateFormat, isSameYear = false, showDayOfWeek = showDayOfWeek, locale = currentLocale, customPattern = customTimelineDateFormat, abbreviateDayOfWeek = abbreviateDayOfWeek, smartYearHiding = smartYearHiding)
    }

    val groups = remember(images, showTimeline, timelineDateFormat, customTimelineDateFormat, useRelativeDates, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, currentLocale, todayString, yesterdayString, sameYearFormatter, otherYearFormatter) {
        if (showTimeline) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val dayCache = HashMap<LocalDate, String>()
            images.groupBy { image ->
                val date = Instant.ofEpochMilli(image.dateTaken).atZone(zone).toLocalDate()
                dayCache.getOrPut(date) {
                    formatTimelineDate(
                        date = date,
                        today = today,
                        sameYearFormatter = sameYearFormatter,
                        otherYearFormatter = otherYearFormatter,
                        useRelativeDates = useRelativeDates,
                        todayString = todayString,
                        yesterdayString = yesterdayString,
                        smartYearHiding = smartYearHiding,
                    )
                }
            }.entries.toList()
        } else listOf("" to images).map { object : Map.Entry<String, List<MediaImage>> {
            override val key = it.first
            override val value = it.second
        } }
    }
    val timelineItems = remember(groups, showTimeline) {
        buildList<MediaImage?> {
            groups.forEach { group ->
                if (showTimeline) add(null)
                addAll(group.value)
            }
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actualColumns = remember(screenWidthDp, cellSize, gridSpacing, density, startPadding, endPadding) {
        com.iris.gallery.ui.GridCalculations.calculatePhotoColumns(
            screenWidthDp = screenWidthDp,
            cellSizeDp = cellSize.value,
            gridSpacingDp = gridSpacing.dp.toFloat(),
            density = density,
            horizontalPaddingDp = (startPadding + endPadding).value / 2f
        )
    }

    val spacingPx = remember(gridSpacing, density) { with(density) { (gridSpacing.dp).dp.roundToPx() } }
    val topPaddingPx = remember(gridSpacing, density) { with(density) { (gridSpacing.dp + 3).dp.roundToPx() } }
    val bottomPaddingPx = remember(gridSpacing, bottomPadding, density) { with(density) { ((gridSpacing.dp + 3).dp + bottomPadding).roundToPx() } }

    val estimatedPhotoHeight = remember(actualColumns, screenWidthDp, cellSize, gridSpacing, density, startPadding, endPadding) {
        with(density) {
            val availableWidthPx = (screenWidthDp.dp - startPadding - endPadding - (gridSpacing.dp * 2).dp).roundToPx()
            val cols = actualColumns.coerceAtLeast(1)
            val spacing = (gridSpacing.dp).dp.roundToPx()
            maxOf(1, (availableWidthPx - spacing * (cols - 1)) / cols)
        }
    }
    val estimatedHeaderHeight = remember(density) { with(density) { 50.dp.roundToPx() } }

    val photoHeight = estimatedPhotoHeight
    val headerHeight = estimatedHeaderHeight

    val rowLayout = remember(groups, showTimeline, actualColumns, timelineItems.size) {
        val cols = actualColumns.coerceAtLeast(1)
        val totalCount = timelineItems.size
        val itemToRow = IntArray(totalCount)
        val rowToItem = ArrayList<Int>()
        val isRowHeaderList = ArrayList<Boolean>()
        var itemIdx = 0
        var rowIdx = 0

        if (showTimeline) {
            for (group in groups) {
                if (itemIdx < totalCount) {
                    itemToRow[itemIdx] = rowIdx
                    rowToItem.add(itemIdx)
                    isRowHeaderList.add(true)
                    itemIdx++
                    rowIdx++
                }
                val photoCount = group.value.size
                var photoInGroup = 0
                while (photoInGroup < photoCount && itemIdx < totalCount) {
                    rowToItem.add(itemIdx)
                    isRowHeaderList.add(false)
                    val inThisRow = minOf(cols, photoCount - photoInGroup)
                    for (c in 0 until inThisRow) {
                        if (itemIdx < totalCount) {
                            itemToRow[itemIdx] = rowIdx
                            itemIdx++
                            photoInGroup++
                        }
                    }
                    rowIdx++
                }
            }
        } else {
            while (itemIdx < totalCount) {
                rowToItem.add(itemIdx)
                isRowHeaderList.add(false)
                val inThisRow = minOf(cols, totalCount - itemIdx)
                for (c in 0 until inThisRow) {
                    itemToRow[itemIdx] = rowIdx
                    itemIdx++
                }
                rowIdx++
            }
        }
        val totalRows = maxOf(rowIdx, 1)
        val isRowHeader = BooleanArray(totalRows) { if (it < isRowHeaderList.size) isRowHeaderList[it] else false }
        Triple(totalRows, itemToRow, Pair(rowToItem.toIntArray(), isRowHeader))
    }
    val totalRows = rowLayout.first
    val itemToRow = rowLayout.second
    val rowToItem = rowLayout.third.first
    val isRowHeader = rowLayout.third.second

    val rowOffsets = remember(rowLayout, photoHeight, headerHeight, spacingPx, topPaddingPx) {
        val offsets = IntArray(totalRows)
        var acc = topPaddingPx
        for (r in 0 until totalRows) {
            offsets[r] = acc
            val h = if (isRowHeader[r]) headerHeight else photoHeight
            acc += h + spacingPx
        }
        offsets
    }

    val totalContentHeight = remember(rowLayout, photoHeight, headerHeight, spacingPx, topPaddingPx, bottomPaddingPx) {
        var acc = topPaddingPx
        for (r in 0 until totalRows) {
            val h = if (isRowHeader[r]) headerHeight else photoHeight
            acc += h + spacingPx
        }
        acc += bottomPaddingPx
        maxOf(acc, 1)
    }

    var scrubberDragging by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var scrubTargetIndex by remember(timelineItems) { mutableIntStateOf(0) }
    var scrubThumbIndex by remember(timelineItems) { mutableIntStateOf(0) }
    var suppressReleaseClickId by remember { mutableStateOf<Long?>(null) }
    val scrubberScope = rememberCoroutineScope()

    val scrollFraction by remember(totalRows, rowOffsets, totalContentHeight) {
        derivedStateOf {
            if (scrubberDragging) {
                scrubFraction
            } else {
                val layoutInfo = gridState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (totalRows <= 1 || visibleItems.isEmpty()) {
                    0f
                } else if (!gridState.canScrollForward) {
                    1f
                } else if (!gridState.canScrollBackward && gridState.firstVisibleItemScrollOffset == 0) {
                    0f
                } else {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val maxScrollPx = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
                    val firstItem = visibleItems.first()
                    val row = itemToRow.getOrElse(firstItem.index) { 0 }
                    val rowStartPx = rowOffsets.getOrElse(row) { 0 }
                    val currentScrollPx = rowStartPx - firstItem.offset.y
                    (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)
                }
            }
        }
    }
    val visibleDate by remember(timelineItems) { derivedStateOf {
        if (timelineItems.isEmpty()) return@derivedStateOf null
        val visibleIndex = if (scrubberDragging) scrubThumbIndex else gridState.firstVisibleItemIndex
        val index = visibleIndex.coerceIn(0, timelineItems.lastIndex)
        var found: MediaImage? = null
        for (i in index..timelineItems.lastIndex) {
            val item = timelineItems[i]
            if (item != null) {
                found = item
                break
            }
        }
        if (found == null) {
            for (i in index downTo 0) {
                val item = timelineItems[i]
                if (item != null) {
                    found = item
                    break
                }
            }
        }
        found
    } }
    val currentImages by rememberUpdatedState(images)
    val currentSelection by rememberUpdatedState(selectedIds)
    val currentSetSelection by rememberUpdatedState(onSetSelection)
    val currentSetBatchSelection by rememberUpdatedState(onSetBatchSelection)
    var lastSelectedAnchorId by remember { mutableStateOf<Long?>(null) }
    var previousObservedSelection by remember { mutableStateOf(selectedIds) }
    LaunchedEffect(selectedIds) {
        if (selectedIds.isEmpty()) {
            lastSelectedAnchorId = null
        } else {
            val added = selectedIds - previousObservedSelection
            if (added.size == 1) {
                lastSelectedAnchorId = added.first()
            } else if (lastSelectedAnchorId !in selectedIds) {
                lastSelectedAnchorId = selectedIds.lastOrNull()
            }
        }
        previousObservedSelection = selectedIds
    }
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    val formatTimelineLabel: (Long) -> String = remember(currentLocale, timelineDateFormat, customTimelineDateFormat, showDayOfWeek, abbreviateDayOfWeek, smartYearHiding, useRelativeDates, todayString, yesterdayString, sameYearFormatter, otherYearFormatter) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val labelFn: (Long) -> String = { timestamp ->
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatTimelineDate(
                date = date,
                today = today,
                sameYearFormatter = sameYearFormatter,
                otherYearFormatter = otherYearFormatter,
                useRelativeDates = useRelativeDates,
                todayString = todayString,
                yesterdayString = yesterdayString,
                smartYearHiding = smartYearHiding,
            )
        }
        labelFn
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp)
            .pointerInput(Unit) {
                if (currentOnCellSizeChange == null) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val downChanges = event.changes.filter { it.pressed }
                        if (downChanges.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (kotlin.math.abs(zoom - 1f) > 0.001f) {
                                val nextSize = (currentCellSize.value * zoom).coerceIn(36f, 320f)
                                currentOnCellSizeChange?.invoke(nextSize.dp)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val targetThumbnailPx = remember(actualColumns) { com.iris.gallery.ui.getThumbnailTargetSizePx(actualColumns) }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(cellSize),
            modifier = Modifier.fillMaxSize().pointerInput(gridState, onSetSelection, onSetBatchSelection) {
                if (currentSetSelection == null && currentSetBatchSelection == null) return@pointerInput
                var selecting = true
                var startId: Long? = null
                var startPosition = Offset.Zero
                var initialSelection = emptySet<Long>()
                var previousDragIds = emptySet<Long>()
                var hasDragged = false
                var autoScrollJob: kotlinx.coroutines.Job? = null
                val edge = 72.dp.toPx()
                val dragSlop = 16.dp.toPx()
                fun applyBatchSelection(ids: Collection<Long>, selected: Boolean) {
                    if (ids.isEmpty()) return
                    val batch = currentSetBatchSelection
                    if (batch != null) {
                        batch(ids, selected)
                    } else {
                        ids.forEach { currentSetSelection?.invoke(it, selected) }
                    }
                }
                fun mediaAt(position: Offset) = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    item.key is Long &&
                        position.x >= item.offset.x && position.x <= item.offset.x + item.size.width &&
                        position.y >= item.offset.y && position.y <= item.offset.y + item.size.height
                }?.key as? Long
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        val id = mediaAt(position) ?: return@detectDragGesturesAfterLongPress
                        val anchorId = lastSelectedAnchorId
                        initialSelection = currentSelection
                        startPosition = position
                        suppressReleaseClickId = id
                        hasDragged = false

                        val allImages = currentImages
                        val anchorIdx = if (
                            anchorId != null &&
                            initialSelection.isNotEmpty() &&
                            anchorId in initialSelection &&
                            id !in initialSelection &&
                            id != anchorId
                        ) {
                            allImages.indexOfFirst { it.id == anchorId }
                        } else -1
                        val targetIdx = if (anchorIdx >= 0) allImages.indexOfFirst { it.id == id } else -1

                        if (anchorIdx >= 0 && targetIdx >= 0) {
                            val minIdx = minOf(anchorIdx, targetIdx)
                            val maxIdx = maxOf(anchorIdx, targetIdx)
                            val rangeIds = allImages.subList(minIdx, maxIdx + 1).map { it.id }
                            selecting = true
                            startId = anchorId
                            previousDragIds = rangeIds.toSet()
                            lastSelectedAnchorId = id
                            applyBatchSelection(rangeIds, true)
                        } else {
                            startId = id
                            selecting = id !in initialSelection
                            previousDragIds = setOf(id)
                            applyBatchSelection(listOf(id), selecting)
                        }
                    },
                    onDrag = { change, _ ->
                        val sId = startId
                        if (sId != null) {
                            val dist = (change.position - startPosition).getDistance()
                            if (!hasDragged && dist < dragSlop) {
                                change.consume()
                                return@detectDragGesturesAfterLongPress
                            }
                            hasDragged = true
                            val currentId = mediaAt(change.position)
                            if (currentId != null) {
                                val allImages = currentImages
                                val startIndex = allImages.indexOfFirst { it.id == sId }
                                val currIndex = allImages.indexOfFirst { it.id == currentId }
                                if (startIndex >= 0 && currIndex >= 0) {
                                    val minIdx = minOf(startIndex, currIndex)
                                    val maxIdx = maxOf(startIndex, currIndex)
                                    val currentRangeIds = allImages.subList(minIdx, maxIdx + 1).map { it.id }.toSet()
                                    if (currentRangeIds != previousDragIds) {
                                        val entered = currentRangeIds - previousDragIds
                                        val exited = previousDragIds - currentRangeIds
                                        applyBatchSelection(entered, selecting)
                                        if (exited.isNotEmpty()) {
                                            applyBatchSelection(exited.filter { it in initialSelection }, true)
                                            applyBatchSelection(exited.filter { it !in initialSelection }, false)
                                        }
                                        previousDragIds = currentRangeIds
                                    }
                                }
                            }
                            val scrollBy = when {
                                change.position.y < edge -> -38f
                                change.position.y > size.height - edge -> 38f
                                else -> 0f
                            }
                            if (scrollBy != 0f && autoScrollJob?.isActive != true) {
                                autoScrollJob = scrubberScope.launch { gridState.scrollBy(scrollBy) }
                            }
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        autoScrollJob?.cancel()
                        startId = null
                        previousDragIds = emptySet()
                        scrubberScope.launch {
                            kotlinx.coroutines.delay(250)
                            suppressReleaseClickId = null
                        }
                    },
                    onDragCancel = {
                        autoScrollJob?.cancel()
                        startId = null
                        previousDragIds = emptySet()
                        suppressReleaseClickId = null
                    },
                )
            },
            contentPadding = PaddingValues(
                start = gridSpacing.dp.dp,
                top = (gridSpacing.dp + 3).dp,
                end = gridSpacing.dp.dp,
                bottom = (gridSpacing.dp + 3).dp + bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
            verticalArrangement = Arrangement.spacedBy(gridSpacing.dp.dp),
        ) {
            groups.forEach { group ->
              if (showTimeline) {
                item(key = "header:${group.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    val ids = remember(group.value) { group.value.map { it.id } }
                    val wholeDateSelected = ids.isNotEmpty() && ids.all { it in selectedIds }
                    val headerClickModifier = if (onSetDateSelection != null) {
                        Modifier.combinedClickable(
                            enabled = true,
                            indication = if (selectedIds.isNotEmpty()) androidx.compose.foundation.LocalIndication.current else null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = {
                                if (selectedIds.isNotEmpty()) onSetDateSelection.invoke(ids, !wholeDateSelected)
                            },
                            onLongClick = { onSetDateSelection.invoke(ids, !wholeDateSelected) },
                        )
                    } else Modifier
                    Row(
                        Modifier
                            .animateItem(fadeInSpec = null)
                            .fillMaxWidth()
                            .then(headerClickModifier)
                            .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.key, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        AnimatedVisibility(selectedIds.isNotEmpty()) {
                            Text(if (wholeDateSelected) stringResource(R.string.action_deselect_date) else stringResource(R.string.action_select_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
              }
              items(group.value, key = { it.id }, contentType = { "photo" }) { image ->
                val selected = image.id in selectedIds
                Box(Modifier.animateItem(fadeInSpec = null).aspectRatio(1f).clip(RoundedCornerShape(if (selected) 14.dp else cornerStyle.dp.dp))
                    .combinedClickable(onClick = {
                        if (suppressReleaseClickId == image.id) suppressReleaseClickId = null
                        else onOpen(image)
                    },
                        onLongClick = null)) {
                    MediaThumbnail(
                        image = image,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val selectedScale = if (selected) .91f else 1f
                            scaleX = selectedScale; scaleY = selectedScale
                        },
                        targetSizePx = targetThumbnailPx,
                        showVideoDuration = showVideoDuration,
                        showFormatBadge = showFormatBadge,
                        isFavorite = showFavoriteBadge && (image.id in favorites),
                        onInvalidMedia = onInvalidMedia,
                    )
                    AnimatedVisibility(selected, enter = fadeIn(tween(120)) + scaleIn(tween(160)),
                        exit = fadeOut(tween(100)) + scaleOut(tween(120)),
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                        Icon(Icons.Filled.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(27.dp).background(MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(50)))
                    }
                }
              }
            }
        }
        if (timelineItems.size > 15) {
            val isScrollerActive = gridState.isScrollInProgress || scrubberDragging
            val scrollerAlpha by animateFloatAsState(
                targetValue = if (isScrollerActive) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isScrollerActive) 150 else 500,
                    delayMillis = if (isScrollerActive) 0 else 1200
                ),
                label = "scrubberAlpha"
            )

            val thumbColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)

            if (scrollerAlpha > 0f) {
                Canvas(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(bottom = bottomPadding)
                        .width(40.dp)
                        .graphicsLayer { alpha = scrollerAlpha }
                        .pointerInput(timelineItems.size, totalRows, totalContentHeight) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                scrubberDragging = true
                                val inset = 16.dp.toPx()
                                val thumbHeight = 48.dp.toPx()
                                val maxTravel = (size.height - inset * 2 - thumbHeight).coerceAtLeast(1f)

                                fun calculateFraction(y: Float): Float {
                                    val topTarget = y - inset - (thumbHeight / 2f)
                                    return (topTarget / maxTravel).coerceIn(0f, 1f)
                                }

                                val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                                val maxScrollPx = (totalContentHeight - viewportHeight).coerceAtLeast(1f)

                                var lastTargetIndex = -1
                                var lastOffset = -1
                                var scrubScrollJob: kotlinx.coroutines.Job? = null
                                fun updateTarget(y: Float) {
                                    val frac = calculateFraction(y)
                                    scrubFraction = frac
                                    val targetScrollPx = (frac * maxScrollPx).toInt()
                                    var targetRow = rowOffsets.binarySearch(targetScrollPx)
                                    if (targetRow < 0) {
                                        targetRow = (-targetRow - 2).coerceIn(0, totalRows - 1)
                                    }
                                    val rowStart = rowOffsets[targetRow]
                                    val remainder = (targetScrollPx - rowStart).coerceAtLeast(0)
                                    val targetIndex = rowToItem[targetRow]
                                    val offset = remainder
                                    scrubTargetIndex = targetIndex

                                    val thumbContentPx = (targetScrollPx + y.coerceIn(0f, size.height.toFloat())).toInt()
                                    var thumbRow = rowOffsets.binarySearch(thumbContentPx)
                                    if (thumbRow < 0) {
                                        thumbRow = (-thumbRow - 2).coerceIn(0, totalRows - 1)
                                    }
                                    scrubThumbIndex = rowToItem[thumbRow]
                                    if (targetIndex != lastTargetIndex || kotlin.math.abs(offset - lastOffset) > 8) {
                                        lastTargetIndex = targetIndex
                                        lastOffset = offset
                                        scrubScrollJob?.cancel()
                                        scrubScrollJob = scrubberScope.launch {
                                            gridState.scrollToItem(targetIndex, offset)
                                        }
                                    }
                                }

                                updateTarget(down.position.y)

                                try {
                                    var change = down
                                    do {
                                        updateTarget(change.position.y)
                                        change.consume()
                                        change = awaitPointerEvent().changes.first()
                                    } while (change.pressed)
                                } finally {
                                    scrubberDragging = false
                                    scrubScrollJob?.cancel()
                                    val targetScrollPx = (scrubFraction * maxScrollPx).toInt()
                                    var targetRow = rowOffsets.binarySearch(targetScrollPx)
                                    if (targetRow < 0) {
                                        targetRow = (-targetRow - 2).coerceIn(0, totalRows - 1)
                                    }
                                    val rowStart = rowOffsets[targetRow]
                                    val remainder = (targetScrollPx - rowStart).coerceAtLeast(0)
                                    val targetIndex = rowToItem[targetRow]
                                    val offset = remainder
                                    scrubberScope.launch {
                                        gridState.scrollToItem(targetIndex, offset)
                                    }
                                }
                            }
                        }
                ) {
                    val inset = 16.dp.toPx()
                    val thumbWidth = 5.dp.toPx()
                    val thumbHeight = 48.dp.toPx()
                    val maxTravel = (size.height - inset * 2 - thumbHeight).coerceAtLeast(0f)
                    val thumbTop = inset + scrollFraction.coerceIn(0f, 1f) * maxTravel
                    val thumbLeft = size.width - thumbWidth - 4.dp.toPx()

                    if (scrubberDragging) {
                        val trackWidth = 3.dp.toPx()
                        val trackLeft = size.width - trackWidth - 5.dp.toPx()
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(trackLeft, inset),
                            size = Size(trackWidth, size.height - inset * 2),
                            cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                        )
                    }

                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(thumbLeft, thumbTop),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                    )
                }
            }

            AnimatedVisibility(
                visible = scrubberDragging && showDateBubble && visibleDate != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 42.dp, bottom = bottomPadding)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val insetPx = 16.dp.toPx()
                        val thumbHeightPx = 48.dp.toPx()
                        val maxTravel = (constraints.maxHeight - insetPx * 2 - thumbHeightPx).coerceAtLeast(1f)
                        val thumbCenterY = insetPx + (thumbHeightPx / 2f) + (scrollFraction.coerceIn(0f, 1f) * maxTravel)
                        val targetY = (thumbCenterY - placeable.height / 2f).coerceIn(
                            insetPx,
                            (constraints.maxHeight - insetPx - placeable.height).coerceAtLeast(insetPx)
                        ).roundToInt()
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, targetY)
                        }
                    },
                enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = .88f),
                exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = .9f)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = visibleDate?.let { formatTimelineLabel(it.dateTaken) } ?: "",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
