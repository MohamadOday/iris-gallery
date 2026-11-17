package com.iris.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.res.stringResource
import com.iris.gallery.R
import com.iris.gallery.data.DEFAULT_LIBRARY_ORDER
import com.iris.gallery.data.normalizeLibraryOrder

private data class LibraryItemEntry(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    trashCount: Int,
    lockedCount: Int,
    order: List<String> = DEFAULT_LIBRARY_ORDER,
    isEditingOrder: Boolean = false,
    onOrderChanged: (List<String>) -> Unit = {},
    onOpen: (String) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()

    val entries = listOf(
        LibraryItemEntry("memories", Icons.Outlined.AutoAwesome, stringResource(R.string.library_memories_title), stringResource(R.string.library_memories_subtitle)),
        LibraryItemEntry("duplicates", Icons.Outlined.ContentCopy, stringResource(R.string.library_duplicates_title), stringResource(R.string.library_duplicates_subtitle)),
        LibraryItemEntry("locked", Icons.Outlined.Lock, stringResource(R.string.library_locked_title), stringResource(R.string.library_locked_subtitle, lockedCount)),
        LibraryItemEntry("trash", Icons.Outlined.DeleteOutline, stringResource(R.string.library_trash_title), stringResource(R.string.library_trash_subtitle, trashCount)),
        LibraryItemEntry("editor", Icons.Outlined.Edit, stringResource(R.string.library_editor_title), stringResource(R.string.library_editor_subtitle)),
        LibraryItemEntry("formats", Icons.Outlined.PhotoLibrary, stringResource(R.string.library_formats_title), stringResource(R.string.library_formats_subtitle)),
        LibraryItemEntry("folder_view", Icons.Outlined.Folder, stringResource(R.string.section_folder_view), stringResource(R.string.section_folder_view_desc)),
        LibraryItemEntry("excluded_folders", Icons.Outlined.FolderOff, stringResource(R.string.excluded_folders_title), stringResource(R.string.excluded_folders_desc)),
        LibraryItemEntry("rescan", Icons.Outlined.Refresh, stringResource(R.string.rescan_media_title), stringResource(R.string.rescan_media_subtitle)),
    )
    val entriesByKey = entries.associateBy { it.key }
    val orderedKeys = remember(order) { normalizeLibraryOrder(order) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "library_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.library_header),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (isEditingOrder && orderedKeys != DEFAULT_LIBRARY_ORDER) {
                    TextButton(onClick = { onOrderChanged(DEFAULT_LIBRARY_ORDER) }) {
                        Text(stringResource(R.string.library_reset_order))
                    }
                }
            }
        }
        itemsIndexed(orderedKeys, key = { _, key -> key }) { index, key ->
            val entry = entriesByKey[key] ?: return@itemsIndexed
            LibraryCard(
                icon = entry.icon,
                title = entry.title,
                subtitle = entry.subtitle,
                modifier = Modifier.animateItem(),
                isEditingOrder = isEditingOrder,
                canMoveUp = index > 0,
                canMoveDown = index < orderedKeys.lastIndex,
                onMoveUp = {
                    val mutable = orderedKeys.toMutableList()
                    val item = mutable.removeAt(index)
                    mutable.add(index - 1, item)
                    onOrderChanged(mutable)
                },
                onMoveDown = {
                    val mutable = orderedKeys.toMutableList()
                    val item = mutable.removeAt(index)
                    mutable.add(index + 1, item)
                    onOrderChanged(mutable)
                },
                onClick = { if (!isEditingOrder) onOpen(entry.key) },
            )
        }
    }
}

@Composable
private fun LibraryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isEditingOrder: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = !isEditingOrder,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isEditingOrder) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.action_move_up))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.action_move_down))
                    }
                }
            }
        }
    }
}
