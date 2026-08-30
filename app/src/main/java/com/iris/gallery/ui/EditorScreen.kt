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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Redo
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import com.iris.gallery.data.MediaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(image: MediaImage, onClose: () -> Unit, onSaved: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(topBar = { TopAppBar(title = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_title)) }, navigationIcon = {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_close)) }
            }, actions = {
                Button(enabled = !saving && transformedPreview != null, onClick = {
                    val session = editorView?.session ?: return@Button
                    val crop = RectF(session.crop)
                    val strokes = session.strokes.map { it.copy(points = it.points.toMutableList()) }
                    saving = true
                    scope.launch {
                        val saved = runCatching {
                            saveEditedCopy(context, image, rotation, flipHorizontal, flipVertical,
                                brightness, saturation, contrast, warmth,
                                crop, resizeWidth.toIntOrNull(), resizeHeight.toIntOrNull(), strokes)
                        }.isSuccess
                        saving = false; onSaved(saved)
                    }
                }) { Text(if (saving) androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_saving) else androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.action_save_copy)) }
            }) }) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AndroidView(factory = { EditorCanvasView(it).also { view -> editorView = view } },
                            update = { view ->
                                view.setSource(transformedPreview); view.tool = tool; view.brushRadius = brushSize
                                view.effectStrength = strength.toInt(); view.erasing = erasing; view.colorFilter = androidFilter
                            }, modifier = Modifier.fillMaxSize())
                        if (transformedPreview == null) Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_preparing), color = Color.White)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(
                            EditorTool.ADJUST to com.iris.gallery.R.string.editor_tool_adjust,
                            EditorTool.CROP to com.iris.gallery.R.string.editor_tool_crop,
                            EditorTool.TRANSFORM to com.iris.gallery.R.string.editor_tool_transform,
                            EditorTool.RESIZE to com.iris.gallery.R.string.editor_tool_resize,
                            EditorTool.PIXELATE to com.iris.gallery.R.string.editor_tool_pixelate,
                            EditorTool.BLUR to com.iris.gallery.R.string.editor_tool_blur
                        ).forEach { (value, strRes) ->
                            FilterChip(tool == value, onClick = { tool = value }, label = { Text(androidx.compose.ui.res.stringResource(strRes)) })
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = if (landscape) 150.dp else 250.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (tool) {
                            EditorTool.CROP -> CropControls(cropPreset, { label, aspect ->
                                cropPreset = label; if (aspect != null) editorView?.setCropAspect(aspect)
                            })
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
                            EditorTool.RESIZE -> ResizeControls(baseWidth, baseHeight, resizeWidth, resizeHeight, lockAspect,
                                onWidth = { value ->
                                    resizeWidth = value.filter(Char::isDigit)
                                    if (lockAspect) value.toIntOrNull()?.let { resizeHeight = (it * baseHeight.toFloat() / baseWidth).toInt().toString() }
                                }, onHeight = { value ->
                                    resizeHeight = value.filter(Char::isDigit)
                                    if (lockAspect) value.toIntOrNull()?.let { resizeWidth = (it * baseWidth.toFloat() / baseHeight).toInt().toString() }
                                }, onLock = { lockAspect = it })
                            EditorTool.PIXELATE, EditorTool.BLUR -> BrushControls(tool, brushSize, strength, erasing,
                                onSize = { brushSize = it }, onStrength = { strength = it }, onErase = { erasing = it },
                                onUndo = { editorView?.undoStroke() }, onRedo = { editorView?.redoStroke() },
                                onClear = { editorView?.clearStrokes() })
                            else -> AdjustControls(brightness, saturation, contrast, warmth,
                                { brightness = it }, { saturation = it }, { contrast = it }, { warmth = it })
                        }
                    }
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
    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_rotate_flip), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onRotateLeft, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.RotateLeft, null, modifier = Modifier.size(18.dp))
            Text(" " + androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_rotate_left), maxLines = 1)
        }
        OutlinedButton(onClick = onRotateRight, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.RotateRight, null, modifier = Modifier.size(18.dp))
            Text(" " + androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_rotate_right), maxLines = 1)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

@Composable private fun CropControls(selected: String, onSelect: (String, Float?) -> Unit) {
    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_crop_manual_hint), style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Manual" to null, "Square" to 1f, "4:3" to 4f/3f, "3:4" to 3f/4f, "16:9" to 16f/9f).forEach { (name, ratio) ->
            FilterChip(selected == name, onClick = { onSelect(name, ratio) }, label = { Text(name) })
        }
    }
    Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_crop_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun ResizeControls(baseWidth: Int, baseHeight: Int, width: String, height: String, locked: Boolean,
    onWidth: (String) -> Unit, onHeight: (String) -> Unit, onLock: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(width, onWidth, Modifier.weight(1f), label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_width_px)) }, singleLine = true)
        Text("×")
        OutlinedTextField(height, onHeight, Modifier.weight(1f), label = { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_height_px)) }, singleLine = true)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(locked, onLock); Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_lock_aspect))
        TextButton(onClick = { onWidth(baseWidth.toString()); onHeight(baseHeight.toString()) }) { Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_original)) }
    }
}

@Composable private fun BrushControls(tool: EditorTool, size: Float, strength: Float, erasing: Boolean,
    onSize: (Float) -> Unit, onStrength: (Float) -> Unit, onErase: (Boolean) -> Unit,
    onUndo: () -> Unit, onRedo: () -> Unit, onClear: () -> Unit) {
    val effectLabel = if (tool == EditorTool.PIXELATE) androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brush_pixelation)
                      else androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brush_blur)
    Text(
        androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brush_hint, effectLabel),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brush_size, (size * 100).toInt()), style = MaterialTheme.typography.bodyMedium)
    }
    Slider(
        value = size,
        onValueChange = onSize,
        valueRange = .015f.. .25f,
        modifier = Modifier.fillMaxWidth().height(34.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_strength, strength.toInt()), style = MaterialTheme.typography.bodyMedium)
    }
    Slider(
        value = strength,
        onValueChange = onStrength,
        valueRange = 4f..48f,
        modifier = Modifier.fillMaxWidth().height(34.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(erasing, onClick = { onErase(!erasing) }, label = { Text(if (erasing) androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_eraser_on) else androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_erase)) })
        IconButton(onClick = onUndo) { Icon(Icons.Outlined.Undo, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_undo_stroke)) }
        IconButton(onClick = onRedo) { Icon(Icons.Outlined.Redo, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_redo_stroke)) }
        IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteSweep, androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_clear_effects)) }
    }
}

@Composable private fun AdjustControls(
    brightness: Float, saturation: Float, contrast: Float, warmth: Float,
    onBrightness: (Float) -> Unit, onSaturation: (Float) -> Unit, onContrast: (Float) -> Unit, onWarmth: (Float) -> Unit
) {
    AdjustSlider(
        label = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_brightness),
        value = brightness,
        onValueChange = onBrightness,
        valueRange = -.5f.. .5f,
        valueText = "${if (brightness > 0) "+" else ""}${(brightness * 200).toInt()}%"
    )
    AdjustSlider(
        label = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_saturation),
        value = saturation,
        onValueChange = onSaturation,
        valueRange = 0f..2f,
        valueText = "${(saturation * 100).toInt()}%"
    )
    AdjustSlider(
        label = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_contrast),
        value = contrast,
        onValueChange = onContrast,
        valueRange = .5f..1.5f,
        valueText = "${(contrast * 100).toInt()}%"
    )
    AdjustSlider(
        label = androidx.compose.ui.res.stringResource(com.iris.gallery.R.string.editor_warmth),
        value = warmth,
        onValueChange = onWarmth,
        valueRange = -1f..1f,
        valueText = "${if (warmth > 0) "+" else ""}${(warmth * 100).toInt()}%"
    )
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth().height(34.dp)
    )
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
