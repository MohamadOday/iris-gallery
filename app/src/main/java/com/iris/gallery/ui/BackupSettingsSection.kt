package com.iris.gallery.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iris.gallery.IrisPhotoWidget
import com.iris.gallery.MemoriesNotifications
import com.iris.gallery.R
import com.iris.gallery.data.AutoBackupFrequency
import com.iris.gallery.data.AutoBackupManager
import com.iris.gallery.data.AutoBackupScheduler
import com.iris.gallery.data.BackupManager
import com.iris.gallery.data.BackupPreview
import com.iris.gallery.data.BackupRestoreSummary
import com.iris.gallery.data.SettingsPreferences
import com.iris.gallery.data.SettingsState
import com.iris.gallery.data.VaultBackupCrypto
import com.iris.gallery.data.VaultBackupExportResult
import com.iris.gallery.data.VaultBackupImportResult
import com.iris.gallery.data.VaultWrongPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BackupSettingsActions(
    val onExportBackupJson: () -> String = { "" },
    val onParseBackupPreview: (String) -> Result<BackupPreview> = { Result.failure(IllegalStateException("Backup unavailable")) },
    val onImportBackupJson: suspend (String) -> Result<BackupRestoreSummary> = { Result.failure(IllegalStateException("Backup unavailable")) },
    val onRunAutoBackupNow: suspend () -> Result<String> = { Result.failure(IllegalStateException("Backup unavailable")) },
    val vaultFileCount: Int = 0,
    val vaultTotalBytes: Long = 0L,
    val lockedAuthorized: Boolean = false,
    val onRequestUnlock: () -> Unit = {},
    val onExportVaultBackup: suspend (Uri, CharArray, (Int, Int) -> Unit) -> Result<VaultBackupExportResult> = { _, _, _ ->
        Result.failure(IllegalStateException("Vault backup unavailable"))
    },
    val onImportVaultBackup: suspend (Uri, CharArray, (Int, Int) -> Unit) -> Result<VaultBackupImportResult> = { _, _, _ ->
        Result.failure(IllegalStateException("Vault backup unavailable"))
    },
)

private data class PendingExistingFolderBackup(
    val treeUri: String,
    val label: String,
    val previousUri: String,
    val fileName: String,
    val preview: BackupPreview,
)

private data class VaultBackupUiProgress(
    val isImport: Boolean,
    val done: Int,
    val total: Int,
)

internal class BackupSettingsController(
    val actions: BackupSettingsActions,
    val isBusy: () -> Boolean,
    val launchExportJson: () -> Unit,
    val launchImportJson: () -> Unit,
    val launchPickFolder: () -> Unit,
    val runBackupNow: () -> Unit,
    val restoreLatestFromFolder: () -> Unit,
    val requestExportVault: () -> Unit,
    val launchImportVault: () -> Unit,
)

@Composable
internal fun rememberBackupSettingsController(
    settings: SettingsState,
    preferences: SettingsPreferences,
    actions: BackupSettingsActions,
): BackupSettingsController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backupBusy by remember { mutableStateOf(false) }
    var pendingBackupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var pendingExistingFolderBackup by remember { mutableStateOf<PendingExistingFolderBackup?>(null) }
    var showVaultExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingVaultExportAfterAuth by remember { mutableStateOf(false) }
    var pendingVaultExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingVaultImportUri by remember { mutableStateOf<Uri?>(null) }
    var vaultImportPasswordError by remember { mutableStateOf<String?>(null) }
    var vaultBackupProgress by remember { mutableStateOf<VaultBackupUiProgress?>(null) }

    LaunchedEffect(actions.lockedAuthorized) {
        if (actions.lockedAuthorized && pendingVaultExportAfterAuth) {
            pendingVaultExportAfterAuth = false
            showVaultExportPasswordDialog = true
        }
    }

    val exportVaultBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pw = pendingVaultExportPassword
        pendingVaultExportPassword = null
        if (uri == null || pw == null) {
            pw?.fill('\u0000')
            return@rememberLauncherForActivityResult
        }
        backupBusy = true
        vaultBackupProgress = VaultBackupUiProgress(isImport = false, done = 0, total = actions.vaultFileCount)
        scope.launch {
            val result = try {
                actions.onExportVaultBackup(uri, pw) { done, total ->
                    vaultBackupProgress = VaultBackupUiProgress(isImport = false, done = done, total = total)
                }
            } finally {
                pw.fill('\u0000')
            }
            vaultBackupProgress = null
            backupBusy = false
            result
                .onSuccess { res ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_vault_backup_exported, res.exportedCount),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_vault_backup_export_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    val importVaultBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupBusy = true
        scope.launch {
            val isValidHeader = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        VaultBackupCrypto.isVaultBackupHeader(stream)
                    } ?: false
                }.getOrDefault(false)
            }
            backupBusy = false
            if (isValidHeader) {
                vaultImportPasswordError = null
                pendingVaultImportUri = uri
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_vault_backup_invalid),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val pickAutoBackupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) return@rememberLauncherForActivityResult
        val previousUri = settings.autoBackupTreeUri
        val persisted = AutoBackupManager.takeTreePermission(
            context = context,
            treeUri = treeUri,
            previousUriString = "",
        )
        if (!persisted) {
            Toast.makeText(context, context.getString(R.string.toast_auto_backup_folder_failed), Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val uriStr = treeUri.toString()
        val label = AutoBackupManager.formatTreeDisplayName(context, treeUri)
        backupBusy = true
        scope.launch {
            val existingResult = withContext(Dispatchers.IO) {
                AutoBackupManager.readLatestBackupFromFolder(context, uriStr)
                    .fold(
                        onSuccess = { folderFile ->
                            actions.onParseBackupPreview(folderFile.rawJson).map { preview -> folderFile.fileName to preview }
                        },
                        onFailure = { Result.failure(it) },
                    )
            }
            val existingPair = existingResult.getOrNull()
            if (existingPair != null) {
                backupBusy = false
                pendingExistingFolderBackup = PendingExistingFolderBackup(
                    treeUri = uriStr,
                    label = label,
                    previousUri = previousUri,
                    fileName = existingPair.first,
                    preview = existingPair.second,
                )
            } else {
                if (previousUri.isNotBlank() && previousUri != uriStr) {
                    AutoBackupManager.releaseTreePermission(context, previousUri)
                }
                preferences.setAutoBackupFolder(uriStr, label, enableIfBlank = true)
                AutoBackupScheduler.scheduleFromSettings(context)
                val writeResult = actions.onRunAutoBackupNow()
                backupBusy = false
                val toastRes = if (writeResult.isSuccess) {
                    R.string.toast_auto_backup_folder_ready
                } else {
                    R.string.toast_backup_export_failed
                }
                Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupBusy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = actions.onExportBackupJson()
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: context.contentResolver.openOutputStream(uri)
                        ?: error("Could not open destination file")
                    stream.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
            }
            backupBusy = false
            val toastRes = if (result.isSuccess) R.string.toast_backup_exported else R.string.toast_backup_export_failed
            Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupBusy = true
        scope.launch {
            val previewResult = withContext(Dispatchers.IO) {
                runCatching {
                    val maxBytes = 10 * 1024 * 1024
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val read = input.read(chunk)
                            if (read <= 0) break
                            total += read
                            require(total <= maxBytes) { "Backup file is too large" }
                            buffer.write(chunk, 0, read)
                        }
                        buffer.toByteArray()
                    } ?: error("Could not open backup file")
                    String(bytes, Charsets.UTF_8)
                }.fold(
                    onSuccess = { raw -> actions.onParseBackupPreview(raw) },
                    onFailure = { Result.failure(it) },
                )
            }
            backupBusy = false
            previewResult
                .onSuccess { preview -> pendingBackupPreview = preview }
                .onFailure {
                    Toast.makeText(context, context.getString(R.string.toast_backup_invalid), Toast.LENGTH_LONG).show()
                }
        }
    }

    // Dialogs stay anchored to SettingsScreen so scrolling LazyColumn never drops them.
    if (showVaultExportPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }
        val tooShort = password.isNotEmpty() && password.length < 6
        val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword
        val canSubmit = password.length >= 6 && password == confirmPassword
        AlertDialog(
            onDismissRequest = { showVaultExportPasswordDialog = false },
            title = { Text(stringResource(R.string.settings_vault_backup_password_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_vault_backup_password_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_vault_backup_password_label)) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        isError = tooShort,
                        supportingText = if (tooShort) {
                            { Text(stringResource(R.string.settings_vault_backup_password_too_short)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_vault_backup_password_confirm_label)) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text(stringResource(R.string.settings_vault_backup_password_mismatch)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = canSubmit,
                    onClick = {
                        pendingVaultExportPassword?.fill('\u0000')
                        pendingVaultExportPassword = password.toCharArray()
                        showVaultExportPasswordDialog = false
                        val stamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        exportVaultBackupLauncher.launch("iris-vault-backup-$stamp.irisvault")
                    },
                ) {
                    Text(stringResource(R.string.settings_vault_backup_export))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultExportPasswordDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (vaultBackupProgress == null) {
        pendingVaultImportUri?.let { importUri ->
            var password by remember(importUri) { mutableStateOf("") }
            var showPassword by remember(importUri) { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = {
                    if (!backupBusy) {
                        pendingVaultImportUri = null
                        vaultImportPasswordError = null
                    }
                },
                title = { Text(stringResource(R.string.settings_vault_backup_unlock_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.settings_vault_backup_unlock_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (vaultImportPasswordError != null) vaultImportPasswordError = null
                            },
                            singleLine = true,
                            label = { Text(stringResource(R.string.settings_vault_backup_password_label)) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                    )
                                }
                            },
                            isError = vaultImportPasswordError != null,
                            supportingText = vaultImportPasswordError?.let { err -> { Text(err) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = password.isNotEmpty() && !backupBusy,
                        onClick = {
                            val pwChars = password.toCharArray()
                            backupBusy = true
                            vaultBackupProgress = VaultBackupUiProgress(isImport = true, done = 0, total = 0)
                            scope.launch {
                                val result = try {
                                    actions.onImportVaultBackup(importUri, pwChars) { done, total ->
                                        vaultBackupProgress = VaultBackupUiProgress(isImport = true, done = done, total = total)
                                    }
                                } finally {
                                    pwChars.fill('\u0000')
                                }
                                vaultBackupProgress = null
                                backupBusy = false
                                result
                                    .onSuccess { res ->
                                        pendingVaultImportUri = null
                                        vaultImportPasswordError = null
                                        val msg = when {
                                            res.importedCount == 0 && res.skippedDuplicates > 0 ->
                                                context.getString(R.string.toast_vault_backup_all_duplicates, res.skippedDuplicates)
                                            res.skippedDuplicates > 0 ->
                                                context.getString(R.string.toast_vault_backup_imported_with_skipped, res.importedCount, res.skippedDuplicates)
                                            else ->
                                                context.getString(R.string.toast_vault_backup_imported, res.importedCount)
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                    .onFailure { err ->
                                        if (err is VaultWrongPasswordException) {
                                            vaultImportPasswordError = context.getString(R.string.settings_vault_backup_wrong_password)
                                        } else {
                                            pendingVaultImportUri = null
                                            vaultImportPasswordError = null
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_vault_backup_import_failed),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.settings_backup_restore_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !backupBusy,
                        onClick = {
                            pendingVaultImportUri = null
                            vaultImportPasswordError = null
                        },
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }

    vaultBackupProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    stringResource(
                        if (progress.isImport) R.string.settings_vault_backup_progress_import_title
                        else R.string.settings_vault_backup_progress_export_title
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { (progress.done.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.settings_vault_backup_progress_count, progress.done, progress.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
        )
    }

    pendingExistingFolderBackup?.let { pending ->
        val dateText = remember(pending.preview.exportedAt) {
            formatBackupTimestamp(pending.preview.exportedAt)
        }
        AlertDialog(
            onDismissRequest = {
                if (pending.treeUri != pending.previousUri) {
                    AutoBackupManager.releaseTreePermission(context, pending.treeUri)
                }
                pendingExistingFolderBackup = null
            },
            title = { Text(stringResource(R.string.settings_auto_backup_existing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dateText.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_backup_exported_at, dateText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.settings_auto_backup_existing_desc,
                            pending.fileName,
                            pending.preview.albumGroupsCount,
                            pending.preview.pinnedAlbumsCount,
                            pending.preview.excludedFoldersCount,
                            pending.preview.favoritesCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val previousLanguage = settings.language
                            pendingExistingFolderBackup = null
                            backupBusy = true
                            scope.launch {
                                val importResult = actions.onImportBackupJson(pending.preview.rawJson)
                                backupBusy = false
                                importResult
                                    .onSuccess { summary ->
                                        if (pending.previousUri.isNotBlank() && pending.previousUri != pending.treeUri) {
                                            AutoBackupManager.releaseTreePermission(context, pending.previousUri)
                                        }
                                        preferences.setAutoBackupFolder(pending.treeUri, pending.label, enableIfBlank = true)
                                        AutoBackupScheduler.scheduleFromSettings(context)
                                        MemoriesNotifications.scheduleFromSettings(context)
                                        IrisPhotoWidget.refreshAll(context)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_backup_restored),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (summary.restoredLanguage != previousLanguage) {
                                            setAppLanguage(context, summary.restoredLanguage)
                                        }
                                    }
                                    .onFailure {
                                        if (pending.treeUri != pending.previousUri) {
                                            AutoBackupManager.releaseTreePermission(context, pending.treeUri)
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_backup_import_failed),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_auto_backup_existing_restore))
                    }
                    OutlinedButton(
                        onClick = {
                            pendingExistingFolderBackup = null
                            if (pending.previousUri.isNotBlank() && pending.previousUri != pending.treeUri) {
                                AutoBackupManager.releaseTreePermission(context, pending.previousUri)
                            }
                            preferences.setAutoBackupFolder(pending.treeUri, pending.label, enableIfBlank = true)
                            AutoBackupScheduler.scheduleFromSettings(context)
                            backupBusy = true
                            scope.launch {
                                val writeResult = actions.onRunAutoBackupNow()
                                backupBusy = false
                                val toastRes = if (writeResult.isSuccess) {
                                    R.string.toast_auto_backup_folder_ready
                                } else {
                                    R.string.toast_backup_export_failed
                                }
                                Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_auto_backup_existing_replace))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        if (pending.treeUri != pending.previousUri) {
                            AutoBackupManager.releaseTreePermission(context, pending.treeUri)
                        }
                        pendingExistingFolderBackup = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    pendingBackupPreview?.let { preview ->
        val dateText = remember(preview.exportedAt) {
            formatBackupTimestamp(preview.exportedAt)
        }
        AlertDialog(
            onDismissRequest = { pendingBackupPreview = null },
            title = { Text(stringResource(R.string.settings_backup_restore_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dateText.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_backup_exported_at, dateText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.settings_backup_restore_dialog_desc,
                            preview.albumGroupsCount,
                            preview.pinnedAlbumsCount,
                            preview.excludedFoldersCount,
                            preview.favoritesCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = preview.rawJson
                        val previousLanguage = settings.language
                        pendingBackupPreview = null
                        backupBusy = true
                        scope.launch {
                            val importResult = actions.onImportBackupJson(raw)
                            backupBusy = false
                            importResult
                                .onSuccess { summary ->
                                    MemoriesNotifications.scheduleFromSettings(context)
                                    IrisPhotoWidget.refreshAll(context)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_backup_restored),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    if (summary.restoredLanguage != previousLanguage) {
                                        setAppLanguage(context, summary.restoredLanguage)
                                    }
                                }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_backup_import_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_backup_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackupPreview = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    return BackupSettingsController(
        actions = actions,
        isBusy = { backupBusy },
        launchExportJson = { exportBackupLauncher.launch(BackupManager.defaultBackupFileName()) },
        launchImportJson = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
        launchPickFolder = { pickAutoBackupFolderLauncher.launch(null) },
        runBackupNow = {
            backupBusy = true
            scope.launch {
                val result = actions.onRunAutoBackupNow()
                backupBusy = false
                val toastRes = if (result.isSuccess) R.string.toast_backup_exported else R.string.toast_backup_export_failed
                Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
            }
        },
        restoreLatestFromFolder = {
            backupBusy = true
            scope.launch {
                val previewResult = withContext(Dispatchers.IO) {
                    AutoBackupManager
                        .readLatestBackupFromFolder(context, settings.autoBackupTreeUri)
                        .fold(
                            onSuccess = { file -> actions.onParseBackupPreview(file.rawJson) },
                            onFailure = { Result.failure(it) },
                        )
                }
                backupBusy = false
                previewResult
                    .onSuccess { preview -> pendingBackupPreview = preview }
                    .onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_auto_backup_none_in_folder),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        },
        requestExportVault = {
            if (settings.biometricLockEnabled && !actions.lockedAuthorized) {
                pendingVaultExportAfterAuth = true
                actions.onRequestUnlock()
            } else {
                showVaultExportPasswordDialog = true
            }
        },
        launchImportVault = { importVaultBackupLauncher.launch(arrayOf("*/*")) },
    )
}

@Composable
internal fun BackupAndRestoreCard(
    settings: SettingsState,
    preferences: SettingsPreferences,
    controller: BackupSettingsController,
) {
    val context = LocalContext.current
    val busy = controller.isBusy()
    val hasFolderUri = settings.autoBackupTreeUri.isNotBlank()
    val hasFolderPermission = remember(settings.autoBackupTreeUri, settings.lastAutoBackupTimestamp, settings.lastAutoBackupError) {
        hasFolderUri && AutoBackupManager.hasPersistedTreePermission(context, settings.autoBackupTreeUri)
    }
    val lastBackupFormatted = remember(settings.lastAutoBackupTimestamp) {
        formatBackupTimestamp(settings.lastAutoBackupTimestamp)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_backup_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_backup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = controller.launchExportJson,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_backup_export))
                }
                OutlinedButton(
                    onClick = controller.launchImportJson,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_backup_import))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            SettingsSwitchRow(
                title = stringResource(R.string.settings_auto_backup_title),
                subtitle = stringResource(R.string.settings_auto_backup_desc),
                checked = settings.autoBackupEnabled && hasFolderPermission,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (!hasFolderPermission) {
                            controller.launchPickFolder()
                        } else {
                            preferences.setAutoBackupEnabled(true)
                            AutoBackupScheduler.scheduleFromSettings(context)
                            controller.runBackupNow()
                        }
                    } else {
                        preferences.setAutoBackupEnabled(false)
                        AutoBackupScheduler.cancel(context)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !busy) { controller.launchPickFolder() }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = if (hasFolderUri && !hasFolderPermission) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.settings_auto_backup_folder),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = when {
                                !hasFolderUri -> stringResource(R.string.settings_auto_backup_folder_none)
                                !hasFolderPermission -> stringResource(R.string.settings_auto_backup_folder_expired)
                                else -> settings.autoBackupFolderLabel.ifBlank { settings.autoBackupTreeUri }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasFolderUri && !hasFolderPermission) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }

            if (hasFolderPermission) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_auto_backup_frequency),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            AutoBackupFrequency.ON_CHANGE to R.string.settings_auto_backup_freq_on_change,
                            AutoBackupFrequency.DAILY to R.string.settings_auto_backup_freq_daily,
                            AutoBackupFrequency.WEEKLY to R.string.settings_auto_backup_freq_weekly,
                        ).forEach { (freq, labelRes) ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.autoBackupFrequency == freq,
                                onClick = {
                                    preferences.setAutoBackupFrequency(freq)
                                    AutoBackupScheduler.scheduleFromSettings(context)
                                },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            when (settings.autoBackupFrequency) {
                                AutoBackupFrequency.ON_CHANGE -> R.string.settings_auto_backup_freq_on_change_desc
                                AutoBackupFrequency.DAILY -> R.string.settings_auto_backup_freq_daily_desc
                                AutoBackupFrequency.WEEKLY -> R.string.settings_auto_backup_freq_weekly_desc
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_auto_backup_retention),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1, 3, 5, 10).forEach { count ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.autoBackupRetention == count,
                                onClick = { preferences.setAutoBackupRetention(count) },
                                label = {
                                    Text(
                                        text = if (count == 1) {
                                            stringResource(R.string.settings_auto_backup_keep_latest)
                                        } else {
                                            "$count"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        text = if (settings.autoBackupRetention <= 1) {
                            stringResource(R.string.settings_auto_backup_keep_latest_desc)
                        } else {
                            stringResource(R.string.settings_auto_backup_keep_copies_desc, settings.autoBackupRetention)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (lastBackupFormatted.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_auto_backup_last, lastBackupFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (settings.lastAutoBackupError.isNotBlank()) {
                    Text(
                        text = settings.lastAutoBackupError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = controller.runBackupNow,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_auto_backup_now))
                    }
                    OutlinedButton(
                        onClick = controller.restoreLatestFromFolder,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_auto_backup_restore_latest))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EncryptedVaultBackupCard(
    controller: BackupSettingsController,
) {
    val context = LocalContext.current
    val busy = controller.isBusy()
    val vaultCount = controller.actions.vaultFileCount
    val formattedVaultSize = remember(controller.actions.vaultTotalBytes) {
        android.text.format.Formatter.formatShortFileSize(context, controller.actions.vaultTotalBytes)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.settings_vault_backup_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.settings_vault_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (vaultCount > 0) {
                    stringResource(R.string.settings_vault_backup_status, vaultCount, formattedVaultSize)
                } else {
                    stringResource(R.string.settings_vault_backup_empty)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = controller.requestExportVault,
                    enabled = !busy && vaultCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_vault_backup_export))
                }
                OutlinedButton(
                    onClick = controller.launchImportVault,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_vault_backup_import))
                }
            }
        }
    }
}

private fun formatBackupTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return runCatching {
        val zdt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.getDefault()))
    }.getOrDefault("")
}
