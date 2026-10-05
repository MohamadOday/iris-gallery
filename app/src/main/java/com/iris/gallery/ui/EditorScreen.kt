package com.iris.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.iris.gallery.data.LibraryPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.CropRotate
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.gallery.R
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aomedia.avif.android.AvifDecoder
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

enum class EditorCategory {
    TRANSFORM,
    ADJUST,
    MARKUP,
    PRIVACY
}

enum class MarkupSubMode {
    BRUSH,
    SHAPES,
    TEXT
}

enum class PrivacySubMode {
    BLUR,
    PIXELATE
}

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val defaultQuality: Int
) {
    JPEG("jpg", "image/jpeg", 94),
    PNG("png", "image/png", 100),
    WEBP("webp", "image/webp", 92);

    val compressFormat: Bitmap.CompressFormat
        get() = when (this) {
            JPEG -> Bitmap.CompressFormat.JPEG
            PNG -> Bitmap.CompressFormat.PNG
            WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(image: MediaImage, onClose: () -> Unit, onSaved: (Boolean) -> Unit) {
    val context = LocalContext.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val rawPreview by produceState<Bitmap?>(null, image.id) { value = loadPreview(context, image) }
    var editorView by remember { mutableStateOf<EditorCanvasView?>(null) }

    var category by remember { mutableStateOf(EditorCategory.TRANSFORM) }
    var markupSubMode by remember { mutableStateOf(MarkupSubMode.BRUSH) }
    var privacySubMode by remember { mutableStateOf(PrivacySubMode.BLUR) }

    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var straightenAngle by remember { mutableFloatStateOf(0f) }
    var isStraightening by remember { mutableStateOf(false) }
    var canvasZoom by remember { mutableFloatStateOf(1f) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var brushSize by remember { mutableFloatStateOf(.07f) }
    var strength by remember { mutableFloatStateOf(18f) }
    var erasing by remember { mutableStateOf(false) }
    var drawColor by remember { mutableIntStateOf(android.graphics.Color.parseColor("#F44336")) }
    var drawBrushSize by remember { mutableFloatStateOf(0.015f) }
    var drawErasing by remember { mutableStateOf(false) }
    var shapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeFilled by remember { mutableStateOf(false) }
    var shapeStrokeSize by remember { mutableFloatStateOf(0.012f) }
    var selectedOverlay by remember { mutableStateOf<TextOverlay?>(null) }
    var cropPreset by remember { mutableStateOf("Manual") }

    val baseWidth = if (rotation == 90 || rotation == 270) image.height else image.width
    val baseHeight = if (rotation == 90 || rotation == 270) image.width else image.height
    var resizeWidth by remember(image.id, rotation) { mutableStateOf(baseWidth.toString()) }
    var resizeHeight by remember(image.id, rotation) { mutableStateOf(baseHeight.toString()) }
    var isCustomResized by remember(image.id, rotation) { mutableStateOf(false) }
    var targetMaxBytes by remember(image.id, rotation) { mutableStateOf<Long?>(null) }
    var lockAspect by remember { mutableStateOf(true) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    val originalExportFormat = remember(image.id, image.mimeType, image.path, image.name) {
        val mime = image.mimeType.lowercase()
        val name = (image.name.ifBlank { image.path }).lowercase()
        when {
            mime.contains("png") || name.endsWith(".png") -> ExportFormat.PNG
            mime.contains("webp") || name.endsWith(".webp") -> ExportFormat.WEBP
            else -> ExportFormat.JPEG
        }
    }
    var selectedExportFormat by remember(image.id) { mutableStateOf(originalExportFormat) }
    var saving by remember { mutableStateOf(false) }

    val activeTool = when (category) {
        EditorCategory.TRANSFORM -> EditorTool.CROP
        EditorCategory.ADJUST -> EditorTool.ADJUST
        EditorCategory.MARKUP -> when (markupSubMode) {
            MarkupSubMode.BRUSH -> EditorTool.DRAW
            MarkupSubMode.SHAPES -> EditorTool.SHAPE
            MarkupSubMode.TEXT -> EditorTool.TEXT
        }
        EditorCategory.PRIVACY -> if (privacySubMode == PrivacySubMode.BLUR) EditorTool.BLUR else EditorTool.PIXELATE
    }

    val transformedPreview = remember(rawPreview, rotation, flipHorizontal, flipVertical) {
        val src = rawPreview ?: return@remember null
        val matrix = Matrix()
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        if (flipHorizontal || flipVertical) matrix.postScale(if (flipHorizontal) -1f else 1f, if (flipVertical) -1f else 1f)
        if (!matrix.isIdentity) {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } else {
            src
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            editorView?.clearBitmaps()
        }
    }

    val androidFilter = remember(brightness, saturation, contrast, warmth) {
        ColorMatrix().apply {
            setSaturation(saturation)
            val shift = brightness * 255f
            val pivot = (1f - contrast) * 128f
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, shift + pivot + warmth * 36f,
                        0f, contrast, 0f, 0f, shift + pivot,
                        0f, 0f, contrast, 0f, shift + pivot - warmth * 36f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }.let(::ColorMatrixColorFilter)
    }

    BackHandler(onBack = onClose)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.editor_title),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.editor_close))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { editorView?.undoStroke() },
                            enabled = canUndo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Undo,
                                stringResource(R.string.editor_undo_stroke),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { editorView?.redoStroke() },
                            enabled = canRedo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Redo,
                                stringResource(R.string.editor_redo_stroke),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            enabled = !saving && transformedPreview != null,
                            onClick = { showSaveAsDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                if (saving) stringResource(R.string.action_saving)
                                else stringResource(R.string.action_save_as),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = category == EditorCategory.TRANSFORM,
                        onClick = {
                            category = EditorCategory.TRANSFORM
                            editorView?.selectedTextId = null
                            selectedOverlay = null
                        },
                        icon = { Icon(Icons.Outlined.CropRotate, null) },
                        label = { Text(stringResource(R.string.editor_category_transform)) }
                    )
                    NavigationBarItem(
                        selected = category == EditorCategory.ADJUST,
                        onClick = {
                            category = EditorCategory.ADJUST
                            editorView?.selectedTextId = null
                            selectedOverlay = null
                        },
                        icon = { Icon(Icons.Outlined.Tune, null) },
                        label = { Text(stringResource(R.string.editor_category_adjust)) }
                    )
                    NavigationBarItem(
                        selected = category == EditorCategory.MARKUP,
                        onClick = {
                            category = EditorCategory.MARKUP
                        },
                        icon = { Icon(Icons.Outlined.Draw, null) },
                        label = { Text(stringResource(R.string.editor_category_markup)) }
                    )
                    NavigationBarItem(
                        selected = category == EditorCategory.PRIVACY,
                        onClick = {
                            category = EditorCategory.PRIVACY
                            editorView?.selectedTextId = null
                            selectedOverlay = null
                        },
                        icon = { Icon(Icons.Outlined.BlurOn, null) },
                        label = { Text(stringResource(R.string.editor_category_effects)) }
                    )
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = {
                            EditorCanvasView(it).also { view ->
                                editorView = view
                                view.onTextSelected = { overlay ->
                                    selectedOverlay = overlay
                                }
                                view.onZoomChanged = { zoom ->
                                    canvasZoom = zoom
                                }
                                view.onHistoryChanged = { u, r ->
                                    canUndo = u
                                    canRedo = r
                                }
                            }
                        },
                        update = { view ->
                            view.setSource(transformedPreview)
                            view.straightenAngle = straightenAngle
                            view.isStraightening = isStraightening
                            view.tool = activeTool
                            view.brushRadius = when (activeTool) {
                                EditorTool.DRAW -> drawBrushSize
                                EditorTool.SHAPE -> shapeStrokeSize
                                else -> brushSize
                            }
                            view.brushColor = drawColor
                            view.currentShapeType = shapeType
                            view.shapeFilled = shapeFilled
                            view.effectStrength = strength.toInt()
                            view.erasing = if (activeTool == EditorTool.DRAW || activeTool == EditorTool.SHAPE) drawErasing else erasing
                            view.colorFilter = androidFilter
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (transformedPreview == null) {
                        Text(stringResource(R.string.editor_preparing), color = Color.White)
                    }
                    if (canvasZoom > 1.05f) {
                        Surface(
                            onClick = { editorView?.resetZoom() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${((canvasZoom * 10).roundToInt() / 10f)}×",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = stringResource(R.string.editor_reset_zoom),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (landscape) 130.dp else 225.dp)
                ) {
                    AnimatedContent(
                        targetState = category,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "EditorCategorySwitch"
                    ) { currentCategory ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (currentCategory) {
                                EditorCategory.TRANSFORM -> TransformControls(
                                    rotation = rotation,
                                    flipH = flipHorizontal,
                                    flipV = flipVertical,
                                    straightenAngle = straightenAngle,
                                    cropPreset = cropPreset,
                                    onRotateLeft = { rotation = (rotation - 90 + 360) % 360 },
                                    onRotateRight = { rotation = (rotation + 90) % 360 },
                                    onToggleFlipH = { flipHorizontal = !flipHorizontal },
                                    onToggleFlipV = { flipVertical = !flipVertical },
                                    onStraightenChange = { straightenAngle = it },
                                    onStraightenActive = { isStraightening = it },
                                    onReset = {
                                        rotation = 0
                                        flipHorizontal = false
                                        flipVertical = false
                                        straightenAngle = 0f
                                    },
                                    onSelectCropAspect = { label, aspect ->
                                        cropPreset = label
                                        editorView?.setCropAspect(aspect)
                                    },
                                    onOpenResize = { showResizeDialog = true }
                                )
                                EditorCategory.ADJUST -> AdjustControls(
                                    brightness = brightness,
                                    saturation = saturation,
                                    contrast = contrast,
                                    warmth = warmth,
                                    onBrightness = { brightness = it },
                                    onSaturation = { saturation = it },
                                    onContrast = { contrast = it },
                                    onWarmth = { warmth = it },
                                    onReset = {
                                        brightness = 0f
                                        saturation = 1f
                                        contrast = 1f
                                        warmth = 0f
                                    }
                                )
                                EditorCategory.MARKUP -> MarkupControls(
                                    subMode = markupSubMode,
                                    onSubModeChange = {
                                        markupSubMode = it
                                        if (it != MarkupSubMode.TEXT) {
                                            editorView?.selectedTextId = null
                                            selectedOverlay = null
                                        }
                                    },
                                    brushSize = drawBrushSize,
                                    brushColor = drawColor,
                                    erasing = drawErasing,
                                    onSize = { drawBrushSize = it },
                                    onColor = { drawColor = it },
                                    onErase = { drawErasing = it },
                                    onClear = { editorView?.clearStrokes() },
                                    shapeType = shapeType,
                                    onShapeTypeChange = { shapeType = it },
                                    shapeFilled = shapeFilled,
                                    onShapeFilledChange = { shapeFilled = it },
                                    shapeStrokeSize = shapeStrokeSize,
                                    onShapeStrokeSizeChange = { shapeStrokeSize = it },
                                    selectedOverlay = selectedOverlay,
                                    onAddOrUpdateText = { text, color, bg, size ->
                                        if (selectedOverlay != null) {
                                             editorView?.updateSelectedText(text, color, bg, size)
                                            selectedOverlay = editorView?.session?.textOverlays?.find { it.id == selectedOverlay?.id }
                                        } else {
                                            val newOverlay = editorView?.addTextOverlay(text, color, bg, size)
                                            selectedOverlay = newOverlay
                                        }
                                    },
                                    onDeleteText = {
                                        editorView?.removeSelectedText()
                                        selectedOverlay = null
                                    },
                                    onClearAllText = {
                                        editorView?.clearTextOverlays()
                                        selectedOverlay = null
                                    },
                                    onDeselect = {
                                        editorView?.selectedTextId = null
                                        selectedOverlay = null
                                    }
                                )
                                EditorCategory.PRIVACY -> PrivacyControls(
                                    subMode = privacySubMode,
                                    onSubModeChange = { privacySubMode = it },
                                    size = brushSize,
                                    strength = strength,
                                    erasing = erasing,
                                    onSize = { brushSize = it },
                                    onStrength = { strength = it },
                                    onErase = { erasing = it },
                                    onClear = { editorView?.clearStrokes() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResizeDialog) {
        ResizeDialog(
            baseWidth = baseWidth,
            baseHeight = baseHeight,
            currentWidth = resizeWidth,
            currentHeight = resizeHeight,
            lockAspect = lockAspect,
            onDismiss = { showResizeDialog = false },
            onApply = { w, h, maxBytes ->
                isCustomResized = true
                resizeWidth = w
                resizeHeight = h
                targetMaxBytes = maxBytes
                showResizeDialog = false
            },
            onReset = {
                isCustomResized = false
                resizeWidth = baseWidth.toString()
                resizeHeight = baseHeight.toString()
                targetMaxBytes = null
                showResizeDialog = false
            }
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            selectedFormat = selectedExportFormat,
            originalFormat = originalExportFormat,
            onSelectFormat = { selectedExportFormat = it },
            onDismiss = { if (!saving) showSaveAsDialog = false },
            onConfirm = {
                showSaveAsDialog = false
                val session = editorView?.session ?: return@SaveAsDialog
                val crop = RectF(session.crop)
                val strokes = session.strokes.map { it.copy(points = it.points.toMutableList()) }
                val textOverlays = session.textOverlays.map { it.copy() }
                val format = selectedExportFormat
                saving = true
                scope.launch {
                    val saved = runCatching {
                        saveEditedCopy(
                            context, image, rotation, flipHorizontal, flipVertical,
                            straightenAngle = straightenAngle,
                            brightness = brightness, saturation = saturation, contrast = contrast, warmth = warmth,
                            crop = crop,
                            requestedWidth = if (isCustomResized) resizeWidth.toIntOrNull() else null,
                            requestedHeight = if (isCustomResized) resizeHeight.toIntOrNull() else null,
                            strokes = strokes,
                            textOverlays = textOverlays,
                            targetMaxBytes = if (isCustomResized) targetMaxBytes else null,
                            format = format
                        )
                    }.isSuccess
                    saving = false
                    onSaved(saved)
                }
            }
        )
    }
}

@Composable
private fun SaveAsDialog(
    selectedFormat: ExportFormat,
    originalFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_format_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.format_original_subtitle, originalFormat.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExportFormat.values().forEach { format ->
                    val isSelected = selectedFormat == format
                    val desc = when (format) {
                        ExportFormat.JPEG -> stringResource(R.string.format_jpeg_desc)
                        ExportFormat.PNG -> stringResource(R.string.format_png_desc)
                        ExportFormat.WEBP -> stringResource(R.string.format_webp_desc)
                    }
                    Surface(
                        selected = isSelected,
                        onClick = { onSelectFormat(format) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectFormat(format) }
                            )
                            Column(Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = format.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (format == originalFormat) {
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.format_original_tag),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun TransformControls(
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    straightenAngle: Float,
    cropPreset: String,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onStraightenChange: (Float) -> Unit,
    onStraightenActive: (Boolean) -> Unit,
    onReset: () -> Unit,
    onSelectCropAspect: (String, Float?) -> Unit,
    onOpenResize: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onRotateLeft,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.RotateLeft, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.editor_rotate_left), maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = onRotateRight,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.RotateRight, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.editor_rotate_right), maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        if (rotation != 0 || flipH || flipV) {
            IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.RestartAlt, stringResource(R.string.editor_reset_orientation), modifier = Modifier.size(20.dp))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = flipH,
            onClick = onToggleFlipH,
            label = { Text(stringResource(R.string.editor_flip_horizontal), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = flipV,
            onClick = onToggleFlipV,
            label = { Text(stringResource(R.string.editor_flip_vertical), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onOpenResize,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Outlined.AspectRatio, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.editor_custom_resize), style = MaterialTheme.typography.bodySmall)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.editor_straighten),
                    style = MaterialTheme.typography.titleSmall
                )
                val angleDisplay = (straightenAngle * 10).roundToInt() / 10f
                Text(
                    text = "${if (angleDisplay > 0) "+" else ""}${angleDisplay}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (straightenAngle != 0f) {
                TextButton(
                    onClick = { onStraightenChange(0f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.editor_reset_adjustments), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Slider(
            value = straightenAngle,
            onValueChange = { angle ->
                onStraightenActive(true)
                val snapped = if (angle in -0.75f..0.75f) 0f else angle
                onStraightenChange(snapped)
            },
            onValueChangeFinished = {
                onStraightenActive(false)
            },
            valueRange = -45f..45f,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val presets = listOf(
            stringResource(R.string.editor_crop_manual) to null,
            "1:1" to 1f,
            "4:3" to 4f / 3f,
            "3:4" to 3f / 4f,
            "16:9" to 16f / 9f,
            "9:16" to 9f / 16f
        )
        presets.forEach { (name, ratio) ->
            FilterChip(
                selected = cropPreset == name,
                onClick = { onSelectCropAspect(name, ratio) },
                label = { Text(name, style = MaterialTheme.typography.bodySmall) }
            )
        }
    }
}

@Composable
private fun AdjustControls(
    brightness: Float,
    saturation: Float,
    contrast: Float,
    warmth: Float,
    onBrightness: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onWarmth: (Float) -> Unit,
    onReset: () -> Unit
) {
    val isModified = brightness != 0f || saturation != 1f || contrast != 1f || warmth != 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.editor_category_adjust), style = MaterialTheme.typography.titleSmall)
        if (isModified) {
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.editor_reset_adjustments), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    AdjustSliderRow(
        icon = Icons.Outlined.BrightnessMedium,
        label = stringResource(R.string.editor_brightness),
        value = brightness,
        displayValue = "${(brightness * 200).toInt()}%",
        valueRange = -0.5f..0.5f,
        onValueChange = onBrightness
    )

    AdjustSliderRow(
        icon = Icons.Outlined.Contrast,
        label = stringResource(R.string.editor_contrast),
        value = contrast,
        displayValue = "${((contrast - 1f) * 100).toInt()}%",
        valueRange = 0.5f..1.5f,
        onValueChange = onContrast
    )

    AdjustSliderRow(
        icon = Icons.Outlined.Palette,
        label = stringResource(R.string.editor_saturation),
        value = saturation,
        displayValue = "${((saturation - 1f) * 100).toInt()}%",
        valueRange = 0f..2f,
        onValueChange = onSaturation
    )

    AdjustSliderRow(
        icon = Icons.Outlined.WbSunny,
        label = stringResource(R.string.editor_warmth),
        value = warmth,
        displayValue = "${(warmth * 100).toInt()}%",
        valueRange = -1f..1f,
        onValueChange = onWarmth
    )
}

@Composable
private fun AdjustSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Text(
            displayValue,
            modifier = Modifier.width(42.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MarkupControls(
    subMode: MarkupSubMode,
    onSubModeChange: (MarkupSubMode) -> Unit,
    brushSize: Float,
    brushColor: Int,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onColor: (Int) -> Unit,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit,
    shapeType: ShapeType,
    onShapeTypeChange: (ShapeType) -> Unit,
    shapeFilled: Boolean,
    onShapeFilledChange: (Boolean) -> Unit,
    shapeStrokeSize: Float,
    onShapeStrokeSizeChange: (Float) -> Unit,
    selectedOverlay: TextOverlay?,
    onAddOrUpdateText: (String, Int, Int, Float) -> Unit,
    onDeleteText: () -> Unit,
    onClearAllText: () -> Unit,
    onDeselect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = subMode == MarkupSubMode.BRUSH,
            onClick = { onSubModeChange(MarkupSubMode.BRUSH) },
            label = { Text(stringResource(R.string.editor_mode_draw)) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = subMode == MarkupSubMode.SHAPES,
            onClick = { onSubModeChange(MarkupSubMode.SHAPES) },
            label = { Text(stringResource(R.string.editor_mode_shape)) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = subMode == MarkupSubMode.TEXT,
            onClick = { onSubModeChange(MarkupSubMode.TEXT) },
            label = { Text(stringResource(R.string.editor_mode_text)) },
            modifier = Modifier.weight(1f)
        )
    }

    when (subMode) {
        MarkupSubMode.BRUSH -> {
            DrawControls(
                brushSize = brushSize,
                brushColor = brushColor,
                erasing = erasing,
                onSize = onSize,
                onColor = onColor,
                onErase = onErase,
                onClear = onClear
            )
        }
        MarkupSubMode.SHAPES -> {
            ShapeControls(
                shapeType = shapeType,
                onShapeTypeChange = onShapeTypeChange,
                shapeFilled = shapeFilled,
                onShapeFilledChange = onShapeFilledChange,
                strokeSize = shapeStrokeSize,
                onSize = onShapeStrokeSizeChange,
                color = brushColor,
                onColor = onColor,
                erasing = erasing,
                onErase = onErase,
                onClear = onClear
            )
        }
        MarkupSubMode.TEXT -> {
            TextControls(
                selectedOverlay = selectedOverlay,
                onAddOrUpdateText = onAddOrUpdateText,
                onDeleteText = onDeleteText,
                onClearAllText = onClearAllText,
                onDeselect = onDeselect
            )
        }
    }
}

@Composable
private fun PrivacyControls(
    subMode: PrivacySubMode,
    onSubModeChange: (PrivacySubMode) -> Unit,
    size: Float,
    strength: Float,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onStrength: (Float) -> Unit,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = subMode == PrivacySubMode.BLUR,
            onClick = { onSubModeChange(PrivacySubMode.BLUR) },
            label = { Text(stringResource(R.string.editor_mode_blur)) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = subMode == PrivacySubMode.PIXELATE,
            onClick = { onSubModeChange(PrivacySubMode.PIXELATE) },
            label = { Text(stringResource(R.string.editor_mode_pixelate)) },
            modifier = Modifier.weight(1f)
        )
    }

    val tool = if (subMode == PrivacySubMode.BLUR) EditorTool.BLUR else EditorTool.PIXELATE
    BrushControls(
        tool = tool,
        size = size,
        strength = strength,
        erasing = erasing,
        onSize = onSize,
        onStrength = onStrength,
        onErase = onErase,
        onClear = onClear
    )
}

@Composable
private fun ResizeDialog(
    baseWidth: Int,
    baseHeight: Int,
    currentWidth: String,
    currentHeight: String,
    lockAspect: Boolean,
    onDismiss: () -> Unit,
    onApply: (String, String, Long?) -> Unit,
    onReset: () -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) }
    var width by remember { mutableStateOf(currentWidth) }
    var height by remember { mutableStateOf(currentHeight) }
    var locked by remember { mutableStateOf(lockAspect) }

    var percentage by remember { mutableFloatStateOf(100f) }

    var targetSizeInput by remember { mutableStateOf("") }
    var isMb by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_resize_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        label = { Text(stringResource(R.string.editor_resize_tab_dimensions), maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        label = { Text(stringResource(R.string.editor_resize_tab_percentage), maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedMode == 2,
                        onClick = { selectedMode = 2 },
                        label = { Text(stringResource(R.string.editor_resize_tab_filesize), maxLines = 1) }
                    )
                }

                when (selectedMode) {
                    0 -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = width,
                                onValueChange = { value ->
                                    val clean = value.filter(Char::isDigit)
                                    width = clean
                                    if (locked && baseWidth > 0) {
                                        clean.toIntOrNull()?.let {
                                            height = (it * baseHeight.toFloat() / baseWidth).toInt().toString()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.editor_width_px)) },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Text("×")
                            OutlinedTextField(
                                value = height,
                                onValueChange = { value ->
                                    val clean = value.filter(Char::isDigit)
                                    height = clean
                                    if (locked && baseHeight > 0) {
                                        clean.toIntOrNull()?.let {
                                            width = (it * baseWidth.toFloat() / baseHeight).toInt().toString()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.editor_height_px)) },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(checked = locked, onCheckedChange = { locked = it })
                            Text(stringResource(R.string.editor_lock_aspect), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    1 -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(25f, 50f, 75f).forEach { pct ->
                                FilterChip(
                                    selected = percentage.toInt() == pct.toInt(),
                                    onClick = {
                                        percentage = pct
                                        val factor = pct / 100f
                                        width = (baseWidth * factor).toInt().coerceAtLeast(1).toString()
                                        height = (baseHeight * factor).toInt().coerceAtLeast(1).toString()
                                    },
                                    label = { Text("${pct.toInt()}%") }
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.editor_resize_scale, percentage.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = percentage,
                            onValueChange = { pct ->
                                percentage = pct
                                val factor = pct / 100f
                                width = (baseWidth * factor).toInt().coerceAtLeast(1).toString()
                                height = (baseHeight * factor).toInt().coerceAtLeast(1).toString()
                            },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.editor_resize_result, width.toIntOrNull() ?: baseWidth, height.toIntOrNull() ?: baseHeight),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    2 -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = targetSizeInput,
                                onValueChange = { v -> targetSizeInput = v.filter { c -> c.isDigit() || c == '.' } },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.editor_resize_target_size)) },
                                placeholder = { Text(stringResource(R.string.editor_resize_target_size_hint)) },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = !isMb,
                                    onClick = { isMb = false },
                                    label = { Text(stringResource(R.string.editor_resize_kb), maxLines = 1) }
                                )
                                FilterChip(
                                    selected = isMb,
                                    onClick = { isMb = true },
                                    label = { Text(stringResource(R.string.editor_resize_mb), maxLines = 1) }
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.editor_resize_target_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${stringResource(R.string.editor_original)}: $baseWidth × $baseHeight px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val targetBytes: Long? = if (selectedMode == 2) {
                    val num = targetSizeInput.toDoubleOrNull()
                    if (num != null && num > 0) {
                        if (isMb) (num * 1024 * 1024).toLong() else (num * 1024).toLong()
                    } else null
                } else null
                onApply(width, height, targetBytes)
            }) {
                Text(stringResource(R.string.editor_apply))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    width = baseWidth.toString()
                    height = baseHeight.toString()
                    percentage = 100f
                    targetSizeInput = ""
                    onReset()
                }) {
                    Text(stringResource(R.string.editor_original))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
private fun BrushControls(
    tool: EditorTool,
    size: Float,
    strength: Float,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onStrength: (Float) -> Unit,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    val effectLabel = if (tool == EditorTool.PIXELATE) stringResource(R.string.editor_brush_pixelation)
    else stringResource(R.string.editor_brush_blur)
    Text(
        stringResource(R.string.editor_brush_hint, effectLabel),
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        stringResource(R.string.editor_brush_size, (size * 100).toInt()),
        style = MaterialTheme.typography.bodySmall
    )
    Slider(value = size, onValueChange = onSize, valueRange = .015f.. .25f)
    Text(
        stringResource(R.string.editor_strength, strength.toInt()),
        style = MaterialTheme.typography.bodySmall
    )
    Slider(value = strength, onValueChange = onStrength, valueRange = 4f..48f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = erasing,
            onClick = { onErase(!erasing) },
            label = {
                Text(
                    if (erasing) stringResource(R.string.editor_eraser_on)
                    else stringResource(R.string.editor_erase)
                )
            }
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.editor_clear_effects))
        }
    }
}

@Composable
private fun DrawControls(
    brushSize: Float,
    brushColor: Int,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onColor: (Int) -> Unit,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    val colors = remember {
        listOf(
            android.graphics.Color.WHITE,
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#FF9800"),
            android.graphics.Color.parseColor("#FFEB3B"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#00BCD4"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#E91E63"),
        )
    }

    Text(
        stringResource(R.string.editor_brush_size, (brushSize * 1000).toInt()),
        style = MaterialTheme.typography.titleSmall
    )
    Slider(brushSize, onSize, valueRange = 0.005f..0.12f)

    Text(
        stringResource(R.string.editor_draw_color),
        style = MaterialTheme.typography.titleSmall
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { c ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(c), CircleShape)
                    .then(
                        if (brushColor == c && !erasing) {
                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                        }
                    )
                    .clickable {
                        onColor(c)
                        if (erasing) onErase(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (brushColor == c && !erasing) {
                    val checkColor = if (c == android.graphics.Color.WHITE || c == android.graphics.Color.parseColor("#FFEB3B")) Color.Black else Color.White
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        FilterChip(
            selected = erasing,
            onClick = { onErase(!erasing) },
            label = {
                Text(
                    if (erasing) stringResource(R.string.editor_eraser_on)
                    else stringResource(R.string.editor_erase)
                )
            }
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.editor_clear_effects))
        }
    }
}

@Composable
private fun ShapeControls(
    shapeType: ShapeType,
    onShapeTypeChange: (ShapeType) -> Unit,
    shapeFilled: Boolean,
    onShapeFilledChange: (Boolean) -> Unit,
    strokeSize: Float,
    onSize: (Float) -> Unit,
    color: Int,
    onColor: (Int) -> Unit,
    erasing: Boolean,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    val colors = remember {
        listOf(
            android.graphics.Color.WHITE,
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#FF9800"),
            android.graphics.Color.parseColor("#FFEB3B"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#00BCD4"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#E91E63"),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = shapeType == ShapeType.RECTANGLE,
            onClick = { onShapeTypeChange(ShapeType.RECTANGLE) },
            leadingIcon = {
                Icon(Icons.Outlined.CropSquare, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            label = { Text(stringResource(R.string.editor_shape_rect)) }
        )
        FilterChip(
            selected = shapeType == ShapeType.OVAL,
            onClick = { onShapeTypeChange(ShapeType.OVAL) },
            leadingIcon = {
                Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            label = { Text(stringResource(R.string.editor_shape_oval)) }
        )
        FilterChip(
            selected = shapeType == ShapeType.ARROW,
            onClick = { onShapeTypeChange(ShapeType.ARROW) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            label = { Text(stringResource(R.string.editor_shape_arrow)) }
        )
        FilterChip(
            selected = shapeType == ShapeType.LINE,
            onClick = { onShapeTypeChange(ShapeType.LINE) },
            leadingIcon = {
                Icon(Icons.Outlined.HorizontalRule, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            label = { Text(stringResource(R.string.editor_shape_line)) }
        )

        if (shapeType == ShapeType.RECTANGLE || shapeType == ShapeType.OVAL) {
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = !shapeFilled,
                onClick = { onShapeFilledChange(false) },
                label = { Text(stringResource(R.string.editor_shape_outline)) }
            )
            FilterChip(
                selected = shapeFilled,
                onClick = { onShapeFilledChange(true) },
                label = { Text(stringResource(R.string.editor_shape_fill)) }
            )
        }
    }

    if (!shapeFilled || (shapeType == ShapeType.ARROW || shapeType == ShapeType.LINE)) {
        Text(
            stringResource(R.string.editor_brush_size, (strokeSize * 1000).toInt()),
            style = MaterialTheme.typography.titleSmall
        )
        Slider(strokeSize, onSize, valueRange = 0.005f..0.045f)
    }

    Text(
        stringResource(R.string.editor_draw_color),
        style = MaterialTheme.typography.titleSmall
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { c ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(c), CircleShape)
                    .then(
                        if (color == c && !erasing) {
                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                        }
                    )
                    .clickable {
                        onColor(c)
                        if (erasing) onErase(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (color == c && !erasing) {
                    val checkColor = if (c == android.graphics.Color.WHITE || c == android.graphics.Color.parseColor("#FFEB3B")) Color.Black else Color.White
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        FilterChip(
            selected = erasing,
            onClick = { onErase(!erasing) },
            label = {
                Text(
                    if (erasing) stringResource(R.string.editor_eraser_on)
                    else stringResource(R.string.editor_erase)
                )
            }
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.editor_clear_effects))
        }
    }
}

@Composable
private fun TextControls(
    selectedOverlay: TextOverlay?,
    onAddOrUpdateText: (String, Int, Int, Float) -> Unit,
    onDeleteText: () -> Unit,
    onClearAllText: () -> Unit,
    onDeselect: () -> Unit
) {
    var textInput by remember(selectedOverlay?.id) { mutableStateOf(selectedOverlay?.text ?: "") }
    var textColor by remember(selectedOverlay?.id) { mutableIntStateOf(selectedOverlay?.color ?: android.graphics.Color.WHITE) }
    var bgStyle by remember(selectedOverlay?.id) {
        mutableIntStateOf(
            when (selectedOverlay?.bgColor) {
                0 -> 0
                android.graphics.Color.WHITE -> 1
                else -> 2
            }
        )
    }
    var textSizeRatio by remember(selectedOverlay?.id) { mutableFloatStateOf(selectedOverlay?.textSizeRatio ?: 0.05f) }

    val colors = remember {
        listOf(
            android.graphics.Color.WHITE,
            android.graphics.Color.BLACK,
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#FF9800"),
            android.graphics.Color.parseColor("#FFEB3B"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#00BCD4"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#E91E63"),
        )
    }

    val computedBgColor = when (bgStyle) {
        0 -> 0
        1 -> android.graphics.Color.WHITE
        else -> android.graphics.Color.argb(160, 0, 0, 0)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                if (selectedOverlay != null && it.isNotBlank()) {
                    onAddOrUpdateText(it, textColor, computedBgColor, textSizeRatio)
                }
            },
            placeholder = { Text(stringResource(R.string.editor_text_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onAddOrUpdateText(textInput, textColor, computedBgColor, textSizeRatio)
                }
            },
            enabled = textInput.isNotBlank()
        ) {
            Text(
                if (selectedOverlay != null) stringResource(R.string.editor_update_text)
                else stringResource(R.string.editor_add_text)
            )
        }
    }

    Text(
        stringResource(R.string.editor_text_size, (textSizeRatio * 1000).toInt()),
        style = MaterialTheme.typography.titleSmall
    )
    Slider(
        value = textSizeRatio,
        onValueChange = {
            textSizeRatio = it
            if (selectedOverlay != null && textInput.isNotBlank()) {
                onAddOrUpdateText(textInput, textColor, computedBgColor, it)
            }
        },
        valueRange = 0.02f..0.12f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.editor_draw_color) + ":", style = MaterialTheme.typography.bodySmall)
        colors.forEach { c ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(c), CircleShape)
                    .then(
                        if (textColor == c) {
                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                        }
                    )
                    .clickable {
                        textColor = c
                        if (selectedOverlay != null && textInput.isNotBlank()) {
                            onAddOrUpdateText(textInput, c, computedBgColor, textSizeRatio)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (textColor == c) {
                    val checkColor = if (c == android.graphics.Color.WHITE || c == android.graphics.Color.parseColor("#FFEB3B")) Color.Black else Color.White
                    Icon(Icons.Filled.Check, null, tint = checkColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = bgStyle == 2,
            onClick = {
                bgStyle = 2
                if (selectedOverlay != null && textInput.isNotBlank()) {
                    onAddOrUpdateText(textInput, textColor, android.graphics.Color.argb(160, 0, 0, 0), textSizeRatio)
                }
            },
            label = { Text(stringResource(R.string.editor_text_bg_dark)) }
        )
        FilterChip(
            selected = bgStyle == 1,
            onClick = {
                bgStyle = 1
                if (selectedOverlay != null && textInput.isNotBlank()) {
                    onAddOrUpdateText(textInput, textColor, android.graphics.Color.WHITE, textSizeRatio)
                }
            },
            label = { Text(stringResource(R.string.editor_text_bg_light)) }
        )
        FilterChip(
            selected = bgStyle == 0,
            onClick = {
                bgStyle = 0
                if (selectedOverlay != null && textInput.isNotBlank()) {
                    onAddOrUpdateText(textInput, textColor, 0, textSizeRatio)
                }
            },
            label = { Text(stringResource(R.string.editor_text_bg_none)) }
        )
        if (selectedOverlay != null) {
            IconButton(onClick = onDeleteText) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.editor_delete_text))
            }
            TextButton(onClick = onDeselect) {
                Text(stringResource(R.string.editor_text_new))
            }
        }
    }
}

private suspend fun loadPreview(context: Context, image: MediaImage): Bitmap? = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    val standardBitmap = runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = if (isFile) ImageDecoder.createSource(File(image.path))
            else ImageDecoder.createSource(context.contentResolver, image.uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            if (isFile) BitmapFactory.decodeFile(image.path)
            else context.contentResolver.openInputStream(image.uri).use(BitmapFactory::decodeStream)
        }
    }.getOrNull()

    if (standardBitmap != null) return@withContext standardBitmap

    // Fallback using AvifDecoder if standard Android decoders fail (e.g. yuv422p AVIF)
    runCatching {
        val bytes = if (isFile) {
            File(image.path).readBytes()
        } else {
            context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() }
        }
        if (bytes != null && bytes.isNotEmpty()) {
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            if (AvifDecoder.isAvifImage(buffer)) {
                val info = AvifDecoder.Info()
                if (AvifDecoder.getInfo(buffer, bytes.size, info)) {
                    val scale = minOf(1f, 1600f / maxOf(info.width, info.height))
                    val targetW = (info.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (info.height * scale).toInt().coerceAtLeast(1)
                    val fullBitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
                    if (AvifDecoder.decode(buffer, bytes.size, fullBitmap)) {
                        if (targetW != info.width || targetH != info.height) {
                            val scaled = Bitmap.createScaledBitmap(fullBitmap, targetW, targetH, true)
                            fullBitmap.recycle()
                            scaled
                        } else {
                            fullBitmap
                        }
                    } else {
                        fullBitmap.recycle()
                        null
                    }
                } else null
            } else null
        } else null
    }.getOrNull()
}

private suspend fun saveEditedCopy(
    context: Context,
    image: MediaImage,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    straightenAngle: Float = 0f,
    brightness: Float,
    saturation: Float,
    contrast: Float,
    warmth: Float,
    crop: RectF,
    requestedWidth: Int?,
    requestedHeight: Int?,
    strokes: List<BrushStroke>,
    textOverlays: List<TextOverlay>,
    targetMaxBytes: Long? = null,
    format: ExportFormat = ExportFormat.JPEG
) = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    val rawSource = runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = if (isFile) ImageDecoder.createSource(File(image.path))
            else ImageDecoder.createSource(context.contentResolver, image.uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            if (isFile) BitmapFactory.decodeFile(image.path) ?: error("Could not decode image")
            else context.contentResolver.openInputStream(image.uri).use(BitmapFactory::decodeStream) ?: error("Could not decode image")
        }
    }.getOrNull() ?: runCatching {
        val bytes = if (isFile) {
            File(image.path).readBytes()
        } else {
            context.contentResolver.openInputStream(image.uri)?.use { it.readBytes() }
        }
        if (bytes != null && bytes.isNotEmpty()) {
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            val info = AvifDecoder.Info()
            if (AvifDecoder.getInfo(buffer, bytes.size, info)) {
                val bmp = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
                if (AvifDecoder.decode(buffer, bytes.size, bmp)) {
                    bmp
                } else {
                    bmp.recycle()
                    null
                }
            } else null
        } else null
    }.getOrNull() ?: error("Could not decode image")

    val matrix = Matrix()
    if (rotation != 0) matrix.postRotate(rotation.toFloat())
    if (flipH || flipV) matrix.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
    var source = if (!matrix.isIdentity) {
        Bitmap.createBitmap(rawSource, 0, 0, rawSource.width, rawSource.height, matrix, true).also {
            if (it !== rawSource) rawSource.recycle()
        }
    } else {
        rawSource
    }

    if (straightenAngle != 0f) {
        val rad = Math.toRadians(kotlin.math.abs(straightenAngle).toDouble())
        val r = source.width.toFloat() / source.height.toFloat().coerceAtLeast(1f)
        val maxR = maxOf(r, 1f / r)
        val scale = (Math.cos(rad) + Math.sin(rad) * maxR).toFloat()
        val strMatrix = Matrix().apply {
            postRotate(straightenAngle, source.width / 2f, source.height / 2f)
            postScale(scale, scale, source.width / 2f, source.height / 2f)
        }
        val straightened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(straightened).drawBitmap(source, strMatrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (source !== rawSource) {
            source.recycle()
        }
        source = straightened
    }

    val left = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
    val top = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
    val cropWidth = (crop.width() * source.width).toInt().coerceIn(1, source.width - left)
    val cropHeight = (crop.height() * source.height).toInt().coerceIn(1, source.height - top)
    val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val colors = ColorMatrix().apply { setSaturation(saturation) }
    val shift = brightness * 255f
    val pivot = (1f - contrast) * 128f
    colors.postConcat(
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, shift + pivot + warmth * 36f,
                0f, contrast, 0f, 0f, shift + pivot,
                0f, 0f, contrast, 0f, shift + pivot - warmth * 36f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    Canvas(adjusted).drawBitmap(cropped, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colors) })
    val width = requestedWidth?.coerceIn(1, 16384) ?: adjusted.width
    val height = requestedHeight?.coerceIn(1, 16384) ?: adjusted.height
    val resized = if (width != adjusted.width || height != adjusted.height) Bitmap.createScaledBitmap(adjusted, width, height, true) else adjusted
    renderBrushes(resized, strokes, crop)
    renderTextOverlays(resized, textOverlays, crop)
    val nowMs = System.currentTimeMillis()
    val nowSec = nowMs / 1000L
    val effectiveTitle = image.title.takeIf { it.isNotBlank() && it != image.name && it != image.name.substringBeforeLast('.') }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, image.name.substringBeforeLast('.') + "_iris." + format.extension)
        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, nowSec)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
        put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
        if (effectiveTitle != null) {
            put(MediaStore.Images.Media.TITLE, effectiveTitle)
        }
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Iris")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create copy")
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        if (targetMaxBytes != null && targetMaxBytes > 0) {
            var quality = format.defaultQuality
            val bos = java.io.ByteArrayOutputStream()
            var fits = false
            if (format != ExportFormat.PNG) {
                while (!fits && quality >= 25) {
                    bos.reset()
                    resized.compress(format.compressFormat, quality, bos)
                    if (bos.size() <= targetMaxBytes) {
                        fits = true
                        break
                    }
                    quality -= 10
                }
            }
            if (!fits) {
                var scale = 0.85f
                while (!fits && scale >= 0.15f) {
                    val sw = (resized.width * scale).toInt().coerceAtLeast(64)
                    val sh = (resized.height * scale).toInt().coerceAtLeast(64)
                    val scaled = Bitmap.createScaledBitmap(resized, sw, sh, true)
                    quality = format.defaultQuality
                    if (format == ExportFormat.PNG) {
                        bos.reset()
                        scaled.compress(format.compressFormat, 100, bos)
                        if (bos.size() <= targetMaxBytes) {
                            fits = true
                        }
                    } else {
                        while (!fits && quality >= 30) {
                            bos.reset()
                            scaled.compress(format.compressFormat, quality, bos)
                            if (bos.size() <= targetMaxBytes) {
                                fits = true
                                break
                            }
                            quality -= 15
                        }
                    }
                    if (scaled !== resized) scaled.recycle()
                    if (fits) break
                    scale -= 0.15f
                }
            }
            outputStream.write(bos.toByteArray())
        } else {
            resized.compress(format.compressFormat, format.defaultQuality, outputStream)
        }
    } ?: error("Could not write copy")
    copyExifMetadata(context, image, uri)
    if (Build.VERSION.SDK_INT >= 29) {
        val updateValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
            put(MediaStore.Images.Media.DATE_ADDED, nowSec)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSec)
            put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
            if (effectiveTitle != null) {
                put(MediaStore.Images.Media.TITLE, effectiveTitle)
            }
        }
        context.contentResolver.update(uri, updateValues, null, null)
    }
    val newId = uri.lastPathSegment?.toLongOrNull()
    if (newId != null && effectiveTitle != null) {
        runCatching {
            LibraryPreferences(context).setCustomTitle(newId, effectiveTitle)
        }
    }
    if (source !== cropped) source.recycle()
    if (cropped !== adjusted) cropped.recycle()
    if (adjusted !== resized) adjusted.recycle()
    resized.recycle()
}

private fun copyExifMetadata(context: Context, sourceImage: MediaImage, destUri: Uri) {
    runCatching {
        val srcExif = if (sourceImage.path.isNotBlank() && File(sourceImage.path).exists()) {
            ExifInterface(sourceImage.path)
        } else {
            context.contentResolver.openInputStream(sourceImage.uri)?.use {
                ExifInterface(it)
            }
        } ?: return

        context.contentResolver.openFileDescriptor(destUri, "rw")?.use { pfd ->
            val dstExif = ExifInterface(pfd.fileDescriptor)
            val tagsToCopy = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_APERTURE_VALUE,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_SCENE_CAPTURE_TYPE,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                "DocumentName",
                "XPTitle",
                "XPComment",
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
            )
            for (tag in tagsToCopy) {
                val value = srcExif.getAttribute(tag)
                if (value != null) {
                    dstExif.setAttribute(tag, value)
                }
            }
            val effectiveTitle = sourceImage.title.takeIf { it.isNotBlank() && it != sourceImage.name && it != sourceImage.name.substringBeforeLast('.') }
            if (effectiveTitle != null) {
                dstExif.setAttribute("DocumentName", effectiveTitle)
                dstExif.setAttribute("XPTitle", effectiveTitle)
            }
            dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            dstExif.setAttribute(ExifInterface.TAG_SOFTWARE, "Iris Gallery")
            dstExif.saveAttributes()
        }
    }
}

private fun renderBrushes(target: Bitmap, strokes: List<BrushStroke>, crop: RectF) {
    if (strokes.isEmpty()) return
    val canvas = Canvas(target)
    strokes.forEach { stroke ->
        if (stroke.shape != ShapeType.NONE && stroke.points.size >= 2) {
            val p1 = stroke.points.first()
            val p2 = stroke.points.last()
            val x1 = (p1.x - crop.left) / crop.width() * target.width
            val y1 = (p1.y - crop.top) / crop.height() * target.height
            val x2 = (p2.x - crop.left) / crop.width() * target.width
            val y2 = (p2.y - crop.top) / crop.height() * target.height
            val strokeWidth = stroke.radius / crop.width() * target.width
            renderShape(canvas, stroke, x1, y1, x2, y2, strokeWidth)
            return@forEach
        }
        if (stroke.effect == BrushEffect.COLOR) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.radius / crop.width() * target.width
                color = stroke.color
            }
            val path = Path()
            var visible = false
            stroke.points.forEach { point ->
                val x = (point.x - crop.left) / crop.width() * target.width
                val y = (point.y - crop.top) / crop.height() * target.height
                if (!visible) {
                    path.moveTo(x, y)
                    path.lineTo(x + .1f, y + .1f)
                    visible = true
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, paint)
            return@forEach
        }
        val effect = if (stroke.effect == BrushEffect.PIXELATE) createPixelatedBitmap(target, stroke.strength)
        else createBlurredBitmap(target, stroke.strength)
        val shader = BitmapShader(effect, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.radius / crop.width() * target.width
            this.shader = shader
        }
        val path = Path()
        var visible = false
        stroke.points.forEach { point ->
            val x = (point.x - crop.left) / crop.width() * target.width
            val y = (point.y - crop.top) / crop.height() * target.height
            if (!visible) {
                path.moveTo(x, y)
                path.lineTo(x + .1f, y + .1f)
                visible = true
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paint)
        effect.recycle()
    }
}

private fun renderTextOverlays(target: Bitmap, overlays: List<TextOverlay>, crop: RectF) {
    if (overlays.isEmpty()) return
    val canvas = Canvas(target)
    overlays.forEach { overlay ->
        val x = (overlay.x - crop.left) / crop.width() * target.width
        val y = (overlay.y - crop.top) / crop.height() * target.height
        val pxSize = (overlay.textSizeRatio / crop.width()) * target.width
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.color
            textSize = pxSize.coerceAtLeast(16f)
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
        val paddingX = pxSize * 0.4f
        val paddingY = pxSize * 0.25f
        val bgRect = RectF(
            x - bounds.width() / 2f - paddingX,
            y - bounds.height() / 2f - paddingY,
            x + bounds.width() / 2f + paddingX,
            y + bounds.height() / 2f + paddingY
        )
        if (overlay.bgColor != 0) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = overlay.bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(bgRect, pxSize * 0.25f, pxSize * 0.25f, bgPaint)
        }
        val textY = y + bounds.height() / 2f - bounds.bottom
        canvas.drawText(overlay.text, x, textY, textPaint)
    }
}
