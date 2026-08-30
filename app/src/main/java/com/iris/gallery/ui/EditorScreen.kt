package com.iris.gallery.ui

import android.content.ContentValues
import android.content.Context
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AdjustTarget { BRIGHTNESS, CONTRAST, SATURATION, WARMTH }
private enum class BrushParam { SIZE, STRENGTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(image: MediaImage, onClose: () -> Unit, onSaved: (Boolean) -> Unit) {
    BackHandler(onBack = onClose)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val rawPreview by produceState<Bitmap?>(null, image.id) { value = loadPreview(context, image) }
    var editorView by remember { mutableStateOf<EditorCanvasView?>(null) }
    var tool by remember { mutableStateOf(EditorTool.ADJUST) }
    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var brushSize by remember { mutableFloatStateOf(.07f) }
    var strength by remember { mutableFloatStateOf(18f) }
    var erasing by remember { mutableStateOf(false) }
    var cropPreset by remember { mutableStateOf("Manual") }

    val baseWidth = if (rotation == 90 || rotation == 270) image.height else image.width
    val baseHeight = if (rotation == 90 || rotation == 270) image.width else image.height
    var resizeWidth by remember(image.id, rotation) { mutableStateOf(baseWidth.toString()) }
    var resizeHeight by remember(image.id, rotation) { mutableStateOf(baseHeight.toString()) }
    var lockAspect by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

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
            val shift = brightness * 255f; val pivot = (1f - contrast) * 128f
            postConcat(ColorMatrix(floatArrayOf(contrast,0f,0f,0f,shift + pivot + warmth * 36f,
                0f,contrast,0f,0f,shift + pivot, 0f,0f,contrast,0f,shift + pivot - warmth * 36f,
                0f,0f,0f,1f,0f)))
        }.let(::ColorMatrixColorFilter)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_title)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_close))
                        }
                    },
                    actions = {
                        Button(
                            enabled = !saving && transformedPreview != null,
                            onClick = {
                                val session = editorView?.session ?: return@Button
                                val crop = RectF(session.crop)
                                val strokes = session.strokes.map { it.copy(points = it.points.toMutableList()) }
                                saving = true
                                scope.launch {
                                    val saved = runCatching {
                                        saveEditedCopy(
                                            context, image, rotation, flipHorizontal, flipVertical,
                                            brightness, saturation, contrast, warmth,
                                            crop, resizeWidth.toIntOrNull(), resizeHeight.toIntOrNull(), strokes
                                        )
                                    }.isSuccess
                                    saving = false
                                    onSaved(saved)
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (saving) androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_saving) else androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_save_copy))
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tool Category Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                EditorTool.ADJUST to com.iris.gallery.R.string.editor_tool_adjust,
                                EditorTool.CROP to com.iris.gallery.R.string.editor_tool_crop,
                                EditorTool.TRANSFORM to com.iris.gallery.R.string.editor_tool_transform,
                                EditorTool.RESIZE to com.iris.gallery.R.string.editor_tool_resize,
                                EditorTool.PIXELATE to com.iris.gallery.R.string.editor_tool_pixelate,
                                EditorTool.BLUR to com.iris.gallery.R.string.editor_tool_blur
                            ).forEach { (value, strRes) ->
                                FilterChip(
                                    selected = tool == value,
                                    onClick = { tool = value },
                                    label = { Text(androidx.compose.ui.res.stringResource(strRes)) }
                                )
                            }
                        }

                        // Active Tool Control Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            when (tool) {
                                EditorTool.ADJUST -> AdjustPanel(
                                    brightness = brightness,
                                    saturation = saturation,
                                    contrast = contrast,
                                    warmth = warmth,
                                    onBrightness = { brightness = it },
                                    onSaturation = { saturation = it },
                                    onContrast = { contrast = it },
                                    onWarmth = { warmth = it }
                                )
                                EditorTool.CROP -> CropControls(cropPreset) { label, aspect ->
                                    cropPreset = label
                                    if (aspect != null) editorView?.setCropAspect(aspect)
                                }
                                EditorTool.TRANSFORM -> TransformControls(
                                    rotation = rotation,
                                    flipH = flipHorizontal,
                                    flipV = flipVertical,
                                    onRotateLeft = { rotation = (rotation - 90 + 360) % 360 },
                                    onRotateRight = { rotation = (rotation + 90) % 360 },
                                    onToggleFlipH = { flipHorizontal = !flipHorizontal },
                                    onToggleFlipV = { flipVertical = !flipVertical },
                                    onReset = {
                                        rotation = 0
                                        flipHorizontal = false
                                        flipVertical = false
                                    }
                                )
                                EditorTool.RESIZE -> ResizeControls(
                                    baseWidth = baseWidth,
                                    baseHeight = baseHeight,
                                    width = resizeWidth,
                                    height = resizeHeight,
                                    locked = lockAspect,
                                    onWidth = { value ->
                                        resizeWidth = value.filter(Char::isDigit)
                                        if (lockAspect) value.toIntOrNull()?.let { resizeHeight = (it * baseHeight.toFloat() / baseWidth).toInt().toString() }
                                    },
                                    onHeight = { value ->
                                        resizeHeight = value.filter(Char::isDigit)
                                        if (lockAspect) value.toIntOrNull()?.let { resizeWidth = (it * baseWidth.toFloat() / baseHeight).toInt().toString() }
                                    },
                                    onLock = { lockAspect = it }
                                )
                                EditorTool.PIXELATE, EditorTool.BLUR -> BrushPanel(
                                    tool = tool,
                                    size = brushSize,
                                    strength = strength,
                                    erasing = erasing,
                                    onSize = { brushSize = it },
                                    onStrength = { strength = it },
                                    onErase = { erasing = it },
                                    onUndo = { editorView?.undoStroke() },
                                    onRedo = { editorView?.redoStroke() },
                                    onClear = { editorView?.clearStrokes() }
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { EditorCanvasView(it).also { view -> editorView = view } },
                    update = { view ->
                        view.setSource(transformedPreview)
                        view.tool = tool
                        view.brushRadius = brushSize
                        view.effectStrength = strength.toInt()
                        view.erasing = erasing
                        view.colorFilter = androidFilter
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (transformedPreview == null) {
                    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_preparing), color = Color.White)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun AdjustPanel(
    brightness: Float,
    saturation: Float,
    contrast: Float,
    warmth: Float,
    onBrightness: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onWarmth: (Float) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(AdjustTarget.BRIGHTNESS) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTarget == AdjustTarget.BRIGHTNESS,
                onClick = { selectedTarget = AdjustTarget.BRIGHTNESS },
                label = {
                    val formatted = if (brightness != 0f) " (${if (brightness > 0) "+" else ""}${(brightness * 200).toInt()}%)" else ""
                    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brightness) + formatted)
                }
            )
            FilterChip(
                selected = selectedTarget == AdjustTarget.CONTRAST,
                onClick = { selectedTarget = AdjustTarget.CONTRAST },
                label = {
                    val formatted = if (contrast != 1f) " (${(contrast * 100).toInt()}%)" else ""
                    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_contrast) + formatted)
                }
            )
            FilterChip(
                selected = selectedTarget == AdjustTarget.SATURATION,
                onClick = { selectedTarget = AdjustTarget.SATURATION },
                label = {
                    val formatted = if (saturation != 1f) " (${(saturation * 100).toInt()}%)" else ""
                    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_saturation) + formatted)
                }
            )
            FilterChip(
                selected = selectedTarget == AdjustTarget.WARMTH,
                onClick = { selectedTarget = AdjustTarget.WARMTH },
                label = {
                    val formatted = if (warmth != 0f) " (${if (warmth > 0) "+" else ""}${(warmth * 100).toInt()}%)" else ""
                    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_warmth) + formatted)
                }
            )
        }

        val (currentVal, range, onValChange, resetVal) = when (selectedTarget) {
            AdjustTarget.BRIGHTNESS -> Quad(brightness, -0.5f..0.5f, onBrightness, 0f)
            AdjustTarget.CONTRAST -> Quad(contrast, 0.5f..1.5f, onContrast, 1f)
            AdjustTarget.SATURATION -> Quad(saturation, 0f..2f, onSaturation, 1f)
            AdjustTarget.WARMTH -> Quad(warmth, -1f..1f, onWarmth, 0f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = currentVal,
                onValueChange = onValChange,
                valueRange = range,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            )
            if (currentVal != resetVal) {
                IconButton(
                    onClick = { onValChange(resetVal) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.RestartAlt,
                        contentDescription = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_reset_orientation),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrushPanel(
    tool: EditorTool,
    size: Float,
    strength: Float,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onStrength: (Float) -> Unit,
    onErase: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit
) {
    var selectedParam by remember { mutableStateOf(BrushParam.SIZE) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedParam == BrushParam.SIZE,
                onClick = { selectedParam = BrushParam.SIZE },
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brush_size, (size * 100).toInt())) }
            )
            FilterChip(
                selected = selectedParam == BrushParam.STRENGTH,
                onClick = { selectedParam = BrushParam.STRENGTH },
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_strength, strength.toInt())) }
            )
            Slider(
                value = if (selectedParam == BrushParam.SIZE) size else strength,
                onValueChange = if (selectedParam == BrushParam.SIZE) onSize else onStrength,
                valueRange = if (selectedParam == BrushParam.SIZE) 0.015f..0.25f else 4f..48f,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                selected = erasing,
                onClick = { onErase(!erasing) },
                label = { Text(if (erasing) androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_eraser_on) else androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_erase)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onUndo) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_undo_stroke))
                }
                IconButton(onClick = onRedo) {
                    Icon(Icons.AutoMirrored.Outlined.Redo, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_redo_stroke))
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.DeleteSweep, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_clear_effects))
                }
            }
        }
    }
}

@Composable
private fun TransformControls(
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onRotateLeft, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.RotateLeft, null, modifier = Modifier.size(18.dp))
                Text(" " + androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_rotate_left), maxLines = 1)
            }
            OutlinedButton(onClick = onRotateRight, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.RotateRight, null, modifier = Modifier.size(18.dp))
                Text(" " + androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_rotate_right), maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = flipH,
                onClick = onToggleFlipH,
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_flip_h)) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = flipV,
                onClick = onToggleFlipV,
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_flip_v)) },
                modifier = Modifier.weight(1f),
            )
            if (rotation != 0 || flipH || flipV) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Outlined.RestartAlt, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_reset_orientation))
                }
            }
        }
    }
}

@Composable
private fun CropControls(selected: String, onSelect: (String, Float?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Manual" to null, "Square" to 1f, "4:3" to 4f/3f, "3:4" to 3f/4f, "16:9" to 16f/9f, "9:16" to 9f/16f).forEach { (name, ratio) ->
            FilterChip(
                selected = selected == name,
                onClick = { onSelect(name, ratio) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun ResizeControls(
    baseWidth: Int,
    baseHeight: Int,
    width: String,
    height: String,
    locked: Boolean,
    onWidth: (String) -> Unit,
    onHeight: (String) -> Unit,
    onLock: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = width,
                onValueChange = onWidth,
                modifier = Modifier.weight(1f),
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_width_px)) },
                singleLine = true
            )
            Text("×")
            OutlinedTextField(
                value = height,
                onValueChange = onHeight,
                modifier = Modifier.weight(1f),
                label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_height_px)) },
                singleLine = true
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(checked = locked, onCheckedChange = onLock)
            Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_lock_aspect))
            TextButton(onClick = { onWidth(baseWidth.toString()); onHeight(baseHeight.toString()) }) {
                Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_original))
            }
        }
    }
}

private suspend fun loadPreview(context: Context, image: MediaImage): Bitmap? = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    if (Build.VERSION.SDK_INT >= 28) {
        val source = if (isFile) ImageDecoder.createSource(java.io.File(image.path))
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
}

private suspend fun saveEditedCopy(
    context: Context,
    image: MediaImage,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    brightness: Float,
    saturation: Float,
    contrast: Float,
    warmth: Float,
    crop: RectF,
    requestedWidth: Int?,
    requestedHeight: Int?,
    strokes: List<BrushStroke>
) = withContext(Dispatchers.IO) {
    val isFile = image.uri.scheme == "file" || image.path.startsWith(context.filesDir.absolutePath)
    val rawSource = if (Build.VERSION.SDK_INT >= 28) {
        val source = if (isFile) ImageDecoder.createSource(java.io.File(image.path))
                     else ImageDecoder.createSource(context.contentResolver, image.uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        if (isFile) BitmapFactory.decodeFile(image.path) ?: error("Could not decode image")
        else context.contentResolver.openInputStream(image.uri).use(BitmapFactory::decodeStream) ?: error("Could not decode image")
    }

    val matrix = Matrix()
    if (rotation != 0) matrix.postRotate(rotation.toFloat())
    if (flipH || flipV) matrix.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
    val source = if (!matrix.isIdentity) {
        Bitmap.createBitmap(rawSource, 0, 0, rawSource.width, rawSource.height, matrix, true).also {
            if (it !== rawSource) rawSource.recycle()
        }
    } else {
        rawSource
    }

    val left = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
    val top = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
    val cropWidth = (crop.width() * source.width).toInt().coerceIn(1, source.width - left)
    val cropHeight = (crop.height() * source.height).toInt().coerceIn(1, source.height - top)
    val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val colors = ColorMatrix().apply { setSaturation(saturation) }
    val shift = brightness * 255f; val pivot = (1f - contrast) * 128f
    colors.postConcat(ColorMatrix(floatArrayOf(contrast,0f,0f,0f,shift + pivot + warmth * 36f,
        0f,contrast,0f,0f,shift + pivot, 0f,0f,contrast,0f,shift + pivot - warmth * 36f, 0f,0f,0f,1f,0f)))
    Canvas(adjusted).drawBitmap(cropped, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colors) })
    val width = requestedWidth?.coerceIn(1, 16384) ?: adjusted.width
    val height = requestedHeight?.coerceIn(1, 16384) ?: adjusted.height
    val resized = if (width != adjusted.width || height != adjusted.height) Bitmap.createScaledBitmap(adjusted, width, height, true) else adjusted
    renderBrushes(resized, strokes, crop)
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, image.name.substringBeforeLast('.') + "_iris.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) { put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Iris"); put(MediaStore.Images.Media.IS_PENDING, 1) }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create copy")
    context.contentResolver.openOutputStream(uri)?.use { resized.compress(Bitmap.CompressFormat.JPEG, 94, it) } ?: error("Could not write copy")
    if (Build.VERSION.SDK_INT >= 29) context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    if (source !== cropped) source.recycle(); if (cropped !== adjusted) cropped.recycle()
    if (adjusted !== resized) adjusted.recycle(); resized.recycle()
}

private fun renderBrushes(target: Bitmap, strokes: List<BrushStroke>, crop: RectF) {
    if (strokes.isEmpty()) return
    val canvas = Canvas(target)
    strokes.forEach { stroke ->
        val effect = if (stroke.effect == BrushEffect.PIXELATE) createPixelatedBitmap(target, stroke.strength)
            else createBlurredBitmap(target, stroke.strength)
        val shader = BitmapShader(effect, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND; strokeWidth = stroke.radius / crop.width() * target.width; this.shader = shader }
        val path = Path(); var visible = false
        stroke.points.forEach { point ->
            val x = (point.x - crop.left) / crop.width() * target.width
            val y = (point.y - crop.top) / crop.height() * target.height
            if (!visible) { path.moveTo(x, y); path.lineTo(x + .1f, y + .1f); visible = true } else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint); effect.recycle()
    }
}
