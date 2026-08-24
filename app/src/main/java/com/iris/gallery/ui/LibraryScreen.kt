package com.iris.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
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

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    trashCount: Int,
    lockedCount: Int,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Organize and rediscover",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item { LibraryCard(Icons.Outlined.AutoAwesome, "Memories", "Highlights from this day and past years") { onOpen("memories") } }
        item { LibraryCard(Icons.Outlined.ContentCopy, "Duplicates", "Find exact copies and visually similar photos") { onOpen("duplicates") } }
        item { LibraryCard(Icons.Outlined.Lock, "Locked", "$lockedCount private items") { onOpen("locked") } }
        item { LibraryCard(Icons.Outlined.DeleteOutline, "Trash", "$trashCount items · Android removes them after 30 days") { onOpen("trash") } }
        item { LibraryCard(Icons.Outlined.Edit, "Editor", "Create a polished copy without touching the original") { onOpen("editor") } }
        item { LibraryCard(Icons.Outlined.PhotoLibrary, "Media formats", "RAW, GIF, motion photos and panoramas") { onOpen("formats") } }
        item { LibraryCard(Icons.Outlined.Settings, "Settings & Customization", "Themes, palettes, grid styles, and gestures") { onOpen("settings") } }
        item { LibraryCard(Icons.Outlined.Info, "About", "Developer, open-source project, and app details") { onOpen("about") } }
    }
}

@Composable
private fun LibraryCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
