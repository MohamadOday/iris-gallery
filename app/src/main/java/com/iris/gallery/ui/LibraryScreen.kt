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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.res.stringResource
import com.iris.gallery.R

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    trashCount: Int,
    lockedCount: Int,
    onOpen: (String) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = padding.calculateStartPadding(layoutDirection)
    val endPadding = padding.calculateEndPadding(layoutDirection)
    val topPadding = padding.calculateTopPadding()
    val bottomPadding = padding.calculateBottomPadding()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(start = startPadding, top = topPadding, end = endPadding, bottom = 0.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.library_header),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item { LibraryCard(Icons.Outlined.AutoAwesome, stringResource(R.string.library_memories_title), stringResource(R.string.library_memories_subtitle)) { onOpen("memories") } }
        item { LibraryCard(Icons.Outlined.ContentCopy, stringResource(R.string.library_duplicates_title), stringResource(R.string.library_duplicates_subtitle)) { onOpen("duplicates") } }
        item { LibraryCard(Icons.Outlined.Lock, stringResource(R.string.library_locked_title), stringResource(R.string.library_locked_subtitle, lockedCount)) { onOpen("locked") } }
        item { LibraryCard(Icons.Outlined.DeleteOutline, stringResource(R.string.library_trash_title), stringResource(R.string.library_trash_subtitle, trashCount)) { onOpen("trash") } }
        item { LibraryCard(Icons.Outlined.Edit, stringResource(R.string.library_editor_title), stringResource(R.string.library_editor_subtitle)) { onOpen("editor") } }
        item { LibraryCard(Icons.Outlined.PhotoLibrary, stringResource(R.string.library_formats_title), stringResource(R.string.library_formats_subtitle)) { onOpen("formats") } }
        item { LibraryCard(Icons.Outlined.Folder, stringResource(R.string.section_folder_view), stringResource(R.string.section_folder_view_desc)) { onOpen("folder_view") } }
        item { LibraryCard(Icons.Outlined.FolderOff, stringResource(R.string.excluded_folders_title), stringResource(R.string.excluded_folders_desc)) { onOpen("excluded_folders") } }
        item { LibraryCard(Icons.Outlined.Refresh, stringResource(R.string.rescan_media_title), stringResource(R.string.rescan_media_subtitle)) { onOpen("rescan") } }
    }
}

@Composable
private fun LibraryCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}
