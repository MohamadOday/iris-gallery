package com.iris.gallery.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropRotate
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Exposure
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vignette
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.iris.gallery.R
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.SettingsPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class EditorCategory {
    TRANSFORM,
    FILTERS,
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

enum class TransformSubPanel {
    STRAIGHTEN,
    VERTICAL,
    HORIZONTAL,
    RATIO
}

enum class AdjustKind(
    val defaultValue: Float,
    val valueRange: ClosedFloatingPointRange<Float>
) {
    BRIGHTNESS(0f, -0.5f..0.5f),
    EXPOSURE(0f, -1f..1f),
    CONTRAST(1f, 0.5f..1.5f),
    SATURATION(1f, 0f..2f),
    WARMTH(0f, -1f..1f),
    TINT(0f, -1f..1f),
    FADE(0f, 0f..1f),
    VIGNETTE(0f, 0f..1f);

    fun displayNumber(value: Float): Int = when (this) {
        BRIGHTNESS -> (value * 200f).roundToInt()
        CONTRAST, SATURATION -> ((value - 1f) * 100f).roundToInt()
        EXPOSURE, WARMTH, TINT, FADE, VIGNETTE -> (value * 100f).roundToInt()
    }

    fun isUnidirectional(): Boolean = this == FADE || this == VIGNETTE
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

private val EDITOR_SWATCH_COLORS = listOf(
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

private fun Modifier.fadingHorizontalEdges(
    scrollState: ScrollState,
    edgeWidth: Dp = 22.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadePx = edgeWidth.toPx().coerceAtMost(size.width * 0.25f)
        if (fadePx <= 0f) return@drawWithContent
        if (scrollState.canScrollBackward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadePx
                ),
                topLeft = Offset.Zero,
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn
            )
        }
        if (scrollState.canScrollForward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadePx,
                    endX = size.width
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    image: MediaImage,
    onClose: () -> Unit,
    onSaved: (saved: Boolean, overwritten: Boolean) -> Unit
) {
    val context = LocalContext.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val settingsPrefs = remember { SettingsPreferences(context) }
    var overwriteOriginal by remember(image.id) {
        mutableStateOf(settingsPrefs.state.value.editorOverwriteOriginal)
    }
    var saving by remember { mutableStateOf(false) }
    var pendingOverwriteBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingOverwriteFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var pendingCachedTags by remember { mutableStateOf<Map<String, String>?>(null) }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bytes = pendingOverwriteBytes
            val fmt = pendingOverwriteFormat ?: ExportFormat.JPEG
            val tags = pendingCachedTags ?: emptyMap()
            pendingOverwriteBytes = null
            pendingOverwriteFormat = null
            pendingCachedTags = null
            if (bytes != null) {
                scope.launch {
                    val success = runCatching {
                        performOverwriteWrite(context, image, bytes, fmt, tags)
                    }.getOrElse {
                        if (it is CancellationException) throw it
                        false
                    }
                    saving = false
                    onSaved(success, true)
                }
            } else {
                saving = false
            }
        } else {
            pendingOverwriteBytes = null
            pendingOverwriteFormat = null
            pendingCachedTags = null
            saving = false
            Toast.makeText(context, R.string.action_cancel, Toast.LENGTH_SHORT).show()
        }
    }

    val rawPreview by produceState<Bitmap?>(null, image.id) { value = loadEditorPreview(context, image) }
    var editorView by remember { mutableStateOf<EditorCanvasView?>(null) }

    var category by remember { mutableStateOf(EditorCategory.TRANSFORM) }
    var transformSubPanel by remember { mutableStateOf(TransformSubPanel.STRAIGHTEN) }
    var markupSubMode by remember { mutableStateOf(MarkupSubMode.BRUSH) }
    var privacySubMode by remember { mutableStateOf(PrivacySubMode.BLUR) }

    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var straightenAngle by remember { mutableFloatStateOf(0f) }
    var perspectiveV by remember { mutableFloatStateOf(0f) }
    var perspectiveH by remember { mutableFloatStateOf(0f) }
    var isStraightening by remember { mutableStateOf(false) }
    var canvasZoom by remember { mutableFloatStateOf(1f) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var comparingOriginal by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf(EditorFilter.ORIGINAL) }
    var filterIntensity by remember { mutableFloatStateOf(1f) }
    var adjustments by remember { mutableStateOf(EditorAdjustments()) }
    var selectedAdjust by remember { mutableStateOf(AdjustKind.BRIGHTNESS) }

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
    var textColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }
    var textBgStyle by remember { mutableIntStateOf(2) }
    var textSizeRatio by remember { mutableFloatStateOf(0.05f) }
    var showTextInputDialog by remember { mutableStateOf(false) }
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
    var showDiscardDialog by remember { mutableStateOf(false) }

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

    val activeTool = when (category) {
        EditorCategory.TRANSFORM -> EditorTool.CROP
        EditorCategory.FILTERS, EditorCategory.ADJUST -> EditorTool.ADJUST
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

    val filterThumbnail = remember(transformedPreview) {
        val src = transformedPreview ?: return@remember null
        val maxSide = 144f
        val scale = minOf(1f, maxSide / maxOf(src.width, src.height))
        val w = (src.width * scale).roundToInt().coerceAtLeast(1)
        val h = (src.height * scale).roundToInt().coerceAtLeast(1)
        runCatching { Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap() }.getOrNull()
    }

    DisposableEffect(Unit) {
        onDispose {
            editorView?.clearBitmaps()
        }
    }

    val androidFilter = remember(selectedFilter, filterIntensity, adjustments) {
        ColorMatrixColorFilter(buildEditorColorMatrix(selectedFilter, filterIntensity, adjustments))
    }

    val hasAnyEdits = rotation != 0 ||
        flipHorizontal ||
        flipVertical ||
        straightenAngle != 0f ||
        perspectiveH != 0f ||
        perspectiveV != 0f ||
        selectedFilter != EditorFilter.ORIGINAL ||
        adjustments.isModified ||
        isCustomResized ||
        canUndo ||
        (editorView?.session?.textOverlays?.isNotEmpty() == true) ||
        (editorView?.hasCustomCrop() == true)

    val requestClose: () -> Unit = {
        if (hasAnyEdits && !saving) {
            showDiscardDialog = true
        } else {
            onClose()
        }
    }

    BackHandler(onBack = requestClose)

    val computedTextBg = when (textBgStyle) {
        0 -> 0
        1 -> android.graphics.Color.WHITE
        else -> android.graphics.Color.argb(160, 0, 0, 0)
    }

    val canvasPane: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    EditorCanvasView(it).also { view ->
                        editorView = view
                        view.onTextSelected = { overlay ->
                            selectedOverlay = overlay?.copy()
                            if (overlay != null) {
                                textColor = overlay.color
                                textSizeRatio = overlay.textSizeRatio
                                textBgStyle = when (overlay.bgColor) {
                                    0 -> 0
                                    android.graphics.Color.WHITE -> 1
                                    else -> 2
                                }
                            }
                        }
                        view.onTextTransformed = { overlay ->
                            selectedOverlay = overlay.copy()
                            textSizeRatio = overlay.textSizeRatio
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
                    view.originalBitmap = rawPreview
                    view.setSource(transformedPreview)
                    view.comparingOriginal = comparingOriginal
                    view.straightenAngle = straightenAngle
                    view.perspectiveV = perspectiveV
                    view.perspectiveH = perspectiveH
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
                    view.vignetteStrength = adjustments.vignette
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            if (transformedPreview == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    Text(
                        stringResource(R.string.editor_preparing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = comparingOriginal,
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                ) {
                    Text(
                        text = stringResource(R.string.editor_original_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            if (canvasZoom > 1.05f) {
                Surface(
                    onClick = { editorView?.resetZoom() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${((canvasZoom * 10).roundToInt() / 10f)}×",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "•", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = stringResource(R.string.editor_reset_zoom),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    val controlsDeckContent: @Composable (EditorCategory) -> Unit = { currentCategory ->
        when (currentCategory) {
            EditorCategory.TRANSFORM -> TransformDeck(
                subPanel = transformSubPanel,
                onSubPanelChange = { transformSubPanel = it },
                rotation = rotation,
                flipH = flipHorizontal,
                flipV = flipVertical,
                straightenAngle = straightenAngle,
                perspectiveV = perspectiveV,
                perspectiveH = perspectiveH,
                cropPreset = cropPreset,
                isCustomResized = isCustomResized,
                hasCustomCrop = editorView?.hasCustomCrop() == true,
                onRotateLeft = { rotation = (rotation - 90 + 360) % 360 },
                onRotateRight = { rotation = (rotation + 90) % 360 },
                onToggleFlipH = { flipHorizontal = !flipHorizontal },
                onToggleFlipV = { flipVertical = !flipVertical },
                onStraightenChange = { straightenAngle = it },
                onPerspectiveVChange = { perspectiveV = it },
                onPerspectiveHChange = { perspectiveH = it },
                onGridActive = { isStraightening = it },
                onSelectCropAspect = { label, aspect ->
                    cropPreset = label
                    editorView?.setCropAspect(aspect)
                },
                onOpenResize = { showResizeDialog = true },
                onResetAll = {
                    rotation = 0
                    flipHorizontal = false
                    flipVertical = false
                    straightenAngle = 0f
                    perspectiveV = 0f
                    perspectiveH = 0f
                    cropPreset = "Manual"
                    editorView?.resetCrop()
                }
            )
            EditorCategory.FILTERS -> FilterDeck(
                thumbnail = filterThumbnail,
                selectedFilter = selectedFilter,
                intensity = filterIntensity,
                onSelectFilter = { filter ->
                    if (selectedFilter != filter) {
                        selectedFilter = filter
                        filterIntensity = 1f
                    }
                },
                onIntensityChange = { filterIntensity = it }
            )
            EditorCategory.ADJUST -> AdjustDeck(
                selected = selectedAdjust,
                onSelect = { selectedAdjust = it },
                adjustments = adjustments,
                onUpdateAdjustments = { adjustments = it },
                onResetAll = { adjustments = EditorAdjustments() }
            )
            EditorCategory.MARKUP -> MarkupDeck(
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
                onBrushSize = { drawBrushSize = it },
                onColor = {
                    drawColor = it
                    if (drawErasing) drawErasing = false
                },
                onErase = { drawErasing = it },
                onClearStrokes = { editorView?.clearStrokes() },
                shapeType = shapeType,
                onShapeTypeChange = { shapeType = it },
                shapeFilled = shapeFilled,
                onShapeFilledChange = { shapeFilled = it },
                shapeStrokeSize = shapeStrokeSize,
                onShapeStrokeSizeChange = { shapeStrokeSize = it },
                selectedOverlay = selectedOverlay,
                textColor = textColor,
                textBgStyle = textBgStyle,
                textSizeRatio = textSizeRatio,
                onOpenTextInput = { showTextInputDialog = true },
                onTextColorChange = { c ->
                    textColor = c
                    val overlay = selectedOverlay
                    if (overlay != null) {
                        editorView?.updateSelectedText(overlay.text, c, computedTextBg, textSizeRatio)
                        selectedOverlay = editorView?.session?.textOverlays?.find { it.id == overlay.id }?.copy()
                    }
                },
                onCycleTextBgStyle = {
                    val nextStyle = (textBgStyle + 1) % 3
                    textBgStyle = nextStyle
                    val bgColor = when (nextStyle) {
                        0 -> 0
                        1 -> android.graphics.Color.WHITE
                        else -> android.graphics.Color.argb(160, 0, 0, 0)
                    }
                    val overlay = selectedOverlay
                    if (overlay != null) {
                        editorView?.updateSelectedText(overlay.text, textColor, bgColor, textSizeRatio)
                        selectedOverlay = editorView?.session?.textOverlays?.find { it.id == overlay.id }?.copy()
                    }
                },
                onTextSizeChange = { size ->
                    textSizeRatio = size
                    val overlay = selectedOverlay
                    if (overlay != null) {
                        editorView?.updateSelectedText(overlay.text, textColor, computedTextBg, size)
                        selectedOverlay = editorView?.session?.textOverlays?.find { it.id == overlay.id }?.copy()
                    }
                },
                onResetTextRotation = {
                    editorView?.setSelectedTextRotation(0f)
                    selectedOverlay = editorView?.session?.textOverlays?.find { it.id == selectedOverlay?.id }?.copy()
                },
                onDeleteText = {
                    editorView?.removeSelectedText()
                    selectedOverlay = null
                },
                onDeselectText = {
                    editorView?.selectedTextId = null
                    selectedOverlay = null
                }
            )
            EditorCategory.PRIVACY -> PrivacyDeck(
                subMode = privacySubMode,
                onSubModeChange = {
                    privacySubMode = it
                    if (erasing) erasing = false
                },
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxSize()) {
            // Compact Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(if (landscape) 46.dp else 52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = requestClose) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.editor_close),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { editorView?.undoStroke() },
                        enabled = canUndo,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = stringResource(R.string.editor_undo_stroke),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { editorView?.redoStroke() },
                        enabled = canRedo,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = stringResource(R.string.editor_redo_stroke),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .pointerInput(hasAnyEdits) {
                                if (!hasAnyEdits) return@pointerInput
                                detectTapGestures(
                                    onPress = {
                                        comparingOriginal = true
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            comparingOriginal = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Compare,
                            contentDescription = stringResource(R.string.editor_compare_hold),
                            tint = when {
                                comparingOriginal -> MaterialTheme.colorScheme.primary
                                hasAnyEdits -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        enabled = !saving && transformedPreview != null,
                        onClick = { showSaveAsDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (saving) stringResource(R.string.action_saving)
                            else stringResource(R.string.action_save),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }

            if (landscape) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    canvasPane(Modifier.weight(1f).fillMaxHeight())
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(304.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = category,
                                    transitionSpec = {
                                        fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                                    },
                                    label = "EditorCategorySwitchLandscape"
                                ) { currentCategory ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        controlsDeckContent(currentCategory)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            EditorCategoryBar(
                                selected = category,
                                onSelect = { next ->
                                    category = next
                                    if (next != EditorCategory.MARKUP) {
                                        editorView?.selectedTextId = null
                                        selectedOverlay = null
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                canvasPane(Modifier.weight(1f).fillMaxWidth())

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                                .padding(top = 6.dp, bottom = 4.dp)
                        ) {
                            AnimatedContent(
                                targetState = category,
                                transitionSpec = {
                                    fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                                },
                                label = "EditorCategorySwitch"
                            ) { currentCategory ->
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    controlsDeckContent(currentCategory)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        EditorCategoryBar(
                            selected = category,
                            onSelect = { next ->
                                category = next
                                if (next != EditorCategory.MARKUP) {
                                    editorView?.selectedTextId = null
                                    selectedOverlay = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) {
                    Text(
                        stringResource(R.string.editor_discard_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.editor_keep_editing))
                }
            }
        )
    }

    if (showTextInputDialog) {
        TextOverlayInputDialog(
            initialText = selectedOverlay?.text.orEmpty(),
            isEditing = selectedOverlay != null,
            onDismiss = { showTextInputDialog = false },
            onConfirm = { entered ->
                val clean = entered.trim()
                if (clean.isNotEmpty()) {
                    if (selectedOverlay != null) {
                        editorView?.updateSelectedText(clean, textColor, computedTextBg, textSizeRatio)
                        selectedOverlay = editorView?.session?.textOverlays?.find { it.id == selectedOverlay?.id }?.copy()
                    } else {
                        val added = editorView?.addTextOverlay(clean, textColor, computedTextBg, textSizeRatio)
                        selectedOverlay = added?.copy()
                    }
                }
                showTextInputDialog = false
            }
        )
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
            overwriteOriginal = overwriteOriginal,
            onToggleOverwrite = {
                overwriteOriginal = it
                if (it) {
                    selectedExportFormat = originalExportFormat
                }
            },
            onSelectFormat = { selectedExportFormat = it },
            onDismiss = { if (!saving) showSaveAsDialog = false },
            onConfirm = {
                showSaveAsDialog = false
                val session = editorView?.session ?: return@SaveAsDialog
                val crop = RectF(session.crop)
                val strokes = session.strokes.map { it.copy(points = it.points.toMutableList()) }
                val textOverlays = session.textOverlays.map { it.copy() }
                val shouldOverwrite = overwriteOriginal
                val format = if (shouldOverwrite) originalExportFormat else selectedExportFormat
                saving = true
                scope.launch {
                    if (shouldOverwrite) {
                        val tags = readEditorExifTags(context, image)
                        val bytes = runCatching {
                            renderEditedBitmapToBytes(
                                context = context,
                                image = image,
                                rotation = rotation,
                                flipH = flipHorizontal,
                                flipV = flipVertical,
                                straightenAngle = straightenAngle,
                                perspectiveH = perspectiveH,
                                perspectiveV = perspectiveV,
                                filter = selectedFilter,
                                filterIntensity = filterIntensity,
                                adjustments = adjustments,
                                crop = crop,
                                requestedWidth = if (isCustomResized) resizeWidth.toIntOrNull() else null,
                                requestedHeight = if (isCustomResized) resizeHeight.toIntOrNull() else null,
                                strokes = strokes,
                                textOverlays = textOverlays,
                                targetMaxBytes = if (isCustomResized) targetMaxBytes else null,
                                format = format
                            )
                        }.getOrElse {
                            if (it is CancellationException) throw it
                            null
                        }

                        if (bytes == null) {
                            saving = false
                            onSaved(false, true)
                            return@launch
                        }

                        try {
                            val success = performOverwriteWrite(context, image, bytes, format, tags)
                            saving = false
                            onSaved(success, true)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: SecurityException) {
                            val consentSender = when {
                                Build.VERSION.SDK_INT >= 30 -> runCatching {
                                    MediaStore.createWriteRequest(context.contentResolver, listOf(canonicalMediaUri(context, image)))
                                }.getOrNull()?.intentSender
                                Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException -> e.userAction.actionIntent.intentSender
                                else -> null
                            }

                            if (consentSender != null) {
                                pendingOverwriteBytes = bytes
                                pendingOverwriteFormat = format
                                pendingCachedTags = tags
                                writePermissionLauncher.launch(
                                    IntentSenderRequest.Builder(consentSender).build()
                                )
                            } else {
                                saving = false
                                onSaved(false, true)
                            }
                        } catch (e: Exception) {
                            saving = false
                            onSaved(false, true)
                        }
                    } else {
                        val saved = runCatching {
                            saveEditedCopy(
                                context = context,
                                image = image,
                                rotation = rotation,
                                flipH = flipHorizontal,
                                flipV = flipVertical,
                                straightenAngle = straightenAngle,
                                perspectiveH = perspectiveH,
                                perspectiveV = perspectiveV,
                                filter = selectedFilter,
                                filterIntensity = filterIntensity,
                                adjustments = adjustments,
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
                        onSaved(saved, false)
                    }
                }
            }
        )
    }
}

@Composable
private fun EditorCategoryBar(
    selected: EditorCategory,
    onSelect: (EditorCategory) -> Unit,
) {
    val items = listOf(
        Triple(EditorCategory.TRANSFORM, Icons.Outlined.CropRotate, stringResource(R.string.editor_category_transform)),
        Triple(EditorCategory.FILTERS, Icons.Outlined.AutoAwesome, stringResource(R.string.editor_category_filters)),
        Triple(EditorCategory.ADJUST, Icons.Outlined.Tune, stringResource(R.string.editor_category_adjust)),
        Triple(EditorCategory.MARKUP, Icons.Outlined.Draw, stringResource(R.string.editor_category_markup)),
        Triple(EditorCategory.PRIVACY, Icons.Outlined.BlurOn, stringResource(R.string.editor_category_effects)),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (cat, icon, label) ->
            val isSelected = selected == cat
            Surface(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorActionTile(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    modified: Boolean = false,
    enabled: Boolean = true,
    iconRotation: Float = 0f,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        modified -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        destructive -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        modified -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (selected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .widthIn(min = 58.dp)
            .height(58.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (modified && !selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd)
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .size(19.dp)
                        .rotate(iconRotation)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    fontWeight = if (selected || modified) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PrecisionDialSlider(
    label: String,
    value: Float,
    defaultValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    onValueChange: (Float) -> Unit,
    onActiveChanged: (Boolean) -> Unit = {},
    onReset: () -> Unit,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
) {
    val modified = abs(value - defaultValue) > 0.002f
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    val zeroTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val normalizedValue = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val normalizedDefault = ((defaultValue - valueRange.start) / span).coerceIn(0f, 1f)
    val snapWindow = span * 0.018f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (modified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (modified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (modified) {
                    Text(
                        text = stringResource(R.string.editor_action_reset_short),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onReset)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (extraActionLabel != null && onExtraAction != null) {
                    Text(
                        text = extraActionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onExtraAction)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(valueRange, defaultValue) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val raw = valueRange.start + frac * span
                        val snapped = if (abs(raw - defaultValue) <= snapWindow) defaultValue else raw
                        onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
                    }
                }
                .pointerInput(valueRange, defaultValue) {
                    detectHorizontalDragGestures(
                        onDragStart = { onActiveChanged(true) },
                        onDragEnd = { onActiveChanged(false) },
                        onDragCancel = { onActiveChanged(false) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val raw = valueRange.start + frac * span
                            val snapped = if (abs(raw - defaultValue) <= snapWindow) defaultValue else raw
                            onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padX = 10.dp.toPx()
                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                val centerY = size.height * 0.56f
                val tickCount = 40
                val minFrac = minOf(normalizedDefault, normalizedValue)
                val maxFrac = maxOf(normalizedDefault, normalizedValue)

                for (i in 0..tickCount) {
                    val frac = i / tickCount.toFloat()
                    val x = padX + frac * usableW
                    val isMajor = i % 5 == 0
                    val isDefaultTick = abs(frac - normalizedDefault) < (0.5f / tickCount)
                    val inActiveSpan = modified && frac in (minFrac - 0.005f)..(maxFrac + 0.005f)
                    val halfH = when {
                        isDefaultTick -> 9.dp.toPx()
                        isMajor -> 7.dp.toPx()
                        else -> 4.dp.toPx()
                    }
                    drawLine(
                        color = when {
                            inActiveSpan -> primaryColor
                            isDefaultTick -> zeroTickColor
                            else -> tickColor
                        },
                        start = Offset(x, centerY - halfH),
                        end = Offset(x, centerY + halfH),
                        strokeWidth = if (isDefaultTick || isMajor) 1.8.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Active thumb needle
                val thumbX = padX + normalizedValue * usableW
                drawLine(
                    color = primaryColor,
                    start = Offset(thumbX, 3.dp.toPx()),
                    end = Offset(thumbX, size.height - 3.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun TransformDeck(
    subPanel: TransformSubPanel,
    onSubPanelChange: (TransformSubPanel) -> Unit,
    rotation: Int,
    flipH: Boolean,
    flipV: Boolean,
    straightenAngle: Float,
    perspectiveV: Float,
    perspectiveH: Float,
    cropPreset: String,
    isCustomResized: Boolean,
    hasCustomCrop: Boolean,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onStraightenChange: (Float) -> Unit,
    onPerspectiveVChange: (Float) -> Unit,
    onPerspectiveHChange: (Float) -> Unit,
    onGridActive: (Boolean) -> Unit,
    onSelectCropAspect: (String, Float?) -> Unit,
    onOpenResize: () -> Unit,
    onResetAll: () -> Unit,
) {
    val anyTransformModified = rotation != 0 || flipH || flipV ||
        straightenAngle != 0f || perspectiveV != 0f || perspectiveH != 0f || hasCustomCrop || cropPreset != "Manual"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        when (subPanel) {
            TransformSubPanel.STRAIGHTEN -> {
                val displayDeg = (straightenAngle * 10).roundToInt() / 10f
                PrecisionDialSlider(
                    label = stringResource(R.string.editor_straighten),
                    value = straightenAngle,
                    defaultValue = 0f,
                    valueRange = -45f..45f,
                    formattedValue = "${if (displayDeg > 0) "+" else ""}${displayDeg}°",
                    onValueChange = onStraightenChange,
                    onActiveChanged = onGridActive,
                    onReset = { onStraightenChange(0f) }
                )
            }
            TransformSubPanel.VERTICAL -> {
                val displayDeg = (perspectiveV * 10).roundToInt() / 10f
                PrecisionDialSlider(
                    label = stringResource(R.string.editor_perspective_vertical),
                    value = perspectiveV,
                    defaultValue = 0f,
                    valueRange = -35f..35f,
                    formattedValue = "${if (displayDeg > 0) "+" else ""}${displayDeg}°",
                    onValueChange = onPerspectiveVChange,
                    onActiveChanged = onGridActive,
                    onReset = { onPerspectiveVChange(0f) }
                )
            }
            TransformSubPanel.HORIZONTAL -> {
                val displayDeg = (perspectiveH * 10).roundToInt() / 10f
                PrecisionDialSlider(
                    label = stringResource(R.string.editor_perspective_horizontal),
                    value = perspectiveH,
                    defaultValue = 0f,
                    valueRange = -35f..35f,
                    formattedValue = "${if (displayDeg > 0) "+" else ""}${displayDeg}°",
                    onValueChange = onPerspectiveHChange,
                    onActiveChanged = onGridActive,
                    onReset = { onPerspectiveHChange(0f) }
                )
            }
            TransformSubPanel.RATIO -> {
                val ratioScroll = rememberScrollState()
                val presets = listOf(
                    stringResource(R.string.editor_crop_manual) to null,
                    "1:1" to 1f,
                    "4:3" to 4f / 3f,
                    "3:4" to 3f / 4f,
                    "16:9" to 16f / 9f,
                    "9:16" to 9f / 16f,
                    "3:2" to 3f / 2f,
                    "2:3" to 2f / 3f,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingHorizontalEdges(ratioScroll)
                        .horizontalScroll(ratioScroll)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { (name, ratio) ->
                        FilterChip(
                            selected = cropPreset == name,
                            onClick = { onSelectCropAspect(name, ratio) },
                            label = { Text(name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    }

    val toolsScroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadingHorizontalEdges(toolsScroll)
            .horizontalScroll(toolsScroll)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorActionTile(
            icon = Icons.Outlined.Straighten,
            label = stringResource(R.string.editor_straighten),
            selected = subPanel == TransformSubPanel.STRAIGHTEN,
            modified = straightenAngle != 0f,
            onClick = { onSubPanelChange(TransformSubPanel.STRAIGHTEN) }
        )
        EditorActionTile(
            icon = Icons.Outlined.SwapVert,
            label = stringResource(R.string.editor_perspective_vertical),
            selected = subPanel == TransformSubPanel.VERTICAL,
            modified = perspectiveV != 0f,
            onClick = { onSubPanelChange(TransformSubPanel.VERTICAL) }
        )
        EditorActionTile(
            icon = Icons.Outlined.SwapHoriz,
            label = stringResource(R.string.editor_perspective_horizontal),
            selected = subPanel == TransformSubPanel.HORIZONTAL,
            modified = perspectiveH != 0f,
            onClick = { onSubPanelChange(TransformSubPanel.HORIZONTAL) }
        )
        EditorActionTile(
            icon = Icons.Outlined.Crop,
            label = stringResource(R.string.editor_action_ratio),
            selected = subPanel == TransformSubPanel.RATIO,
            modified = cropPreset != "Manual" || hasCustomCrop,
            onClick = { onSubPanelChange(TransformSubPanel.RATIO) }
        )
        EditorActionTile(
            icon = Icons.AutoMirrored.Outlined.RotateLeft,
            label = stringResource(R.string.editor_action_rotate_left_short),
            modified = rotation != 0,
            onClick = onRotateLeft
        )
        EditorActionTile(
            icon = Icons.AutoMirrored.Outlined.RotateRight,
            label = stringResource(R.string.editor_action_rotate_right_short),
            modified = rotation != 0,
            onClick = onRotateRight
        )
        EditorActionTile(
            icon = Icons.Outlined.Flip,
            label = stringResource(R.string.editor_action_flip_h_short),
            selected = flipH,
            onClick = onToggleFlipH
        )
        EditorActionTile(
            icon = Icons.Outlined.Flip,
            label = stringResource(R.string.editor_action_flip_v_short),
            selected = flipV,
            iconRotation = 90f,
            onClick = onToggleFlipV
        )
        EditorActionTile(
            icon = Icons.Outlined.AspectRatio,
            label = stringResource(R.string.editor_action_resize_short),
            modified = isCustomResized,
            onClick = onOpenResize
        )
        if (anyTransformModified) {
            EditorActionTile(
                icon = Icons.Outlined.RestartAlt,
                label = stringResource(R.string.editor_action_reset_short),
                onClick = onResetAll
            )
        }
    }
}

@Composable
private fun FilterDeck(
    thumbnail: androidx.compose.ui.graphics.ImageBitmap?,
    selectedFilter: EditorFilter,
    intensity: Float,
    onSelectFilter: (EditorFilter) -> Unit,
    onIntensityChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedFilter == EditorFilter.ORIGINAL) {
            Text(
                text = stringResource(selectedFilter.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.editor_action_intensity_short),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(intensity * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(38.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadingHorizontalEdges(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val composeColorFilter = remember(filter) {
                if (filter == EditorFilter.ORIGINAL) null
                else ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(filter.baseMatrix()))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectFilter(filter) }
                    .padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = stringResource(filter.labelRes),
                            contentScale = ContentScale.Crop,
                            colorFilter = composeColorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(filter.labelRes),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdjustDeck(
    selected: AdjustKind,
    onSelect: (AdjustKind) -> Unit,
    adjustments: EditorAdjustments,
    onUpdateAdjustments: (EditorAdjustments) -> Unit,
    onResetAll: () -> Unit,
) {
    val activeValue = adjustValueOf(adjustments, selected)
    val updateActive: (Float) -> Unit = { v ->
        onUpdateAdjustments(withAdjustValue(adjustments, selected, v))
    }
    val displayInt = selected.displayNumber(activeValue)
    val formatted = if (!selected.isUnidirectional() && displayInt > 0) "+$displayInt" else "$displayInt"

    PrecisionDialSlider(
        label = adjustKindLabel(selected),
        value = activeValue,
        defaultValue = selected.defaultValue,
        valueRange = selected.valueRange,
        formattedValue = formatted,
        onValueChange = updateActive,
        onReset = { updateActive(selected.defaultValue) },
        extraActionLabel = if (adjustments.isModified) stringResource(R.string.editor_action_reset_all) else null,
        onExtraAction = if (adjustments.isModified) onResetAll else null
    )

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadingHorizontalEdges(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdjustKind.entries.forEach { kind ->
            val v = adjustValueOf(adjustments, kind)
            val isModified = abs(v - kind.defaultValue) > 0.002f
            EditorActionTile(
                icon = adjustKindIcon(kind),
                label = adjustKindLabel(kind),
                selected = kind == selected,
                modified = isModified,
                onClick = { onSelect(kind) }
            )
        }
    }
}

private fun adjustValueOf(adj: EditorAdjustments, kind: AdjustKind): Float = when (kind) {
    AdjustKind.BRIGHTNESS -> adj.brightness
    AdjustKind.EXPOSURE -> adj.exposure
    AdjustKind.CONTRAST -> adj.contrast
    AdjustKind.SATURATION -> adj.saturation
    AdjustKind.WARMTH -> adj.warmth
    AdjustKind.TINT -> adj.tint
    AdjustKind.FADE -> adj.fade
    AdjustKind.VIGNETTE -> adj.vignette
}

private fun withAdjustValue(adj: EditorAdjustments, kind: AdjustKind, value: Float): EditorAdjustments = when (kind) {
    AdjustKind.BRIGHTNESS -> adj.copy(brightness = value)
    AdjustKind.EXPOSURE -> adj.copy(exposure = value)
    AdjustKind.CONTRAST -> adj.copy(contrast = value)
    AdjustKind.SATURATION -> adj.copy(saturation = value)
    AdjustKind.WARMTH -> adj.copy(warmth = value)
    AdjustKind.TINT -> adj.copy(tint = value)
    AdjustKind.FADE -> adj.copy(fade = value)
    AdjustKind.VIGNETTE -> adj.copy(vignette = value)
}

@Composable
private fun adjustKindLabel(kind: AdjustKind): String = when (kind) {
    AdjustKind.BRIGHTNESS -> stringResource(R.string.editor_brightness)
    AdjustKind.EXPOSURE -> stringResource(R.string.editor_exposure)
    AdjustKind.CONTRAST -> stringResource(R.string.editor_contrast)
    AdjustKind.SATURATION -> stringResource(R.string.editor_saturation)
    AdjustKind.WARMTH -> stringResource(R.string.editor_warmth)
    AdjustKind.TINT -> stringResource(R.string.editor_tint)
    AdjustKind.FADE -> stringResource(R.string.editor_fade)
    AdjustKind.VIGNETTE -> stringResource(R.string.editor_vignette)
}

private fun adjustKindIcon(kind: AdjustKind): ImageVector = when (kind) {
    AdjustKind.BRIGHTNESS -> Icons.Outlined.Brightness6
    AdjustKind.EXPOSURE -> Icons.Outlined.Exposure
    AdjustKind.CONTRAST -> Icons.Outlined.Contrast
    AdjustKind.SATURATION -> Icons.Outlined.ColorLens
    AdjustKind.WARMTH -> Icons.Outlined.WbSunny
    AdjustKind.TINT -> Icons.Outlined.Tonality
    AdjustKind.FADE -> Icons.Outlined.Opacity
    AdjustKind.VIGNETTE -> Icons.Outlined.Vignette
}

@Composable
private fun CompactColorSwatches(
    selectedColor: Int,
    active: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fadingHorizontalEdges(scrollState, edgeWidth = 18.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EDITOR_SWATCH_COLORS.forEach { c ->
            val picked = active && selectedColor == c
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .then(
                        if (picked) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                    )
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center
            ) {
                if (picked) {
                    val checkColor = if (c == android.graphics.Color.WHITE || c == android.graphics.Color.parseColor("#FFEB3B")) {
                        Color.Black
                    } else {
                        Color.White
                    }
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkupDeck(
    subMode: MarkupSubMode,
    onSubModeChange: (MarkupSubMode) -> Unit,
    brushSize: Float,
    brushColor: Int,
    erasing: Boolean,
    onBrushSize: (Float) -> Unit,
    onColor: (Int) -> Unit,
    onErase: (Boolean) -> Unit,
    onClearStrokes: () -> Unit,
    shapeType: ShapeType,
    onShapeTypeChange: (ShapeType) -> Unit,
    shapeFilled: Boolean,
    onShapeFilledChange: (Boolean) -> Unit,
    shapeStrokeSize: Float,
    onShapeStrokeSizeChange: (Float) -> Unit,
    selectedOverlay: TextOverlay?,
    textColor: Int,
    textBgStyle: Int,
    textSizeRatio: Float,
    onOpenTextInput: () -> Unit,
    onTextColorChange: (Int) -> Unit,
    onCycleTextBgStyle: () -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onResetTextRotation: () -> Unit,
    onDeleteText: () -> Unit,
    onDeselectText: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        when (subMode) {
            MarkupSubMode.BRUSH -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.editor_action_size_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSize,
                        valueRange = 0.005f..0.12f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(brushSize * 1000).roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactColorSwatches(
                        selectedColor = brushColor,
                        active = !erasing,
                        onSelect = onColor,
                        modifier = Modifier.weight(1f)
                    )
                    CompactPillButton(
                        icon = Icons.Outlined.CleaningServices,
                        label = stringResource(R.string.editor_action_eraser_short),
                        selected = erasing,
                        onClick = { onErase(!erasing) }
                    )
                    CompactPillButton(
                        icon = Icons.Outlined.DeleteSweep,
                        label = stringResource(R.string.editor_action_clear_short),
                        onClick = onClearStrokes
                    )
                }
            }
            MarkupSubMode.SHAPES -> {
                val shapesScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingHorizontalEdges(shapesScroll, edgeWidth = 18.dp)
                        .horizontalScroll(shapesScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPillButton(
                        icon = Icons.Outlined.CropSquare,
                        label = stringResource(R.string.editor_shape_rect),
                        selected = shapeType == ShapeType.RECTANGLE,
                        onClick = { onShapeTypeChange(ShapeType.RECTANGLE) }
                    )
                    CompactPillButton(
                        icon = Icons.Outlined.Circle,
                        label = stringResource(R.string.editor_shape_oval),
                        selected = shapeType == ShapeType.OVAL,
                        onClick = { onShapeTypeChange(ShapeType.OVAL) }
                    )
                    CompactPillButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        label = stringResource(R.string.editor_shape_arrow),
                        selected = shapeType == ShapeType.ARROW,
                        onClick = { onShapeTypeChange(ShapeType.ARROW) }
                    )
                    CompactPillButton(
                        icon = Icons.Outlined.HorizontalRule,
                        label = stringResource(R.string.editor_shape_line),
                        selected = shapeType == ShapeType.LINE,
                        onClick = { onShapeTypeChange(ShapeType.LINE) }
                    )
                    if (shapeType == ShapeType.RECTANGLE || shapeType == ShapeType.OVAL) {
                        CompactPillButton(
                            icon = null,
                            label = if (shapeFilled) stringResource(R.string.editor_shape_fill) else stringResource(R.string.editor_shape_outline),
                            selected = shapeFilled,
                            onClick = { onShapeFilledChange(!shapeFilled) }
                        )
                    }
                    CompactPillButton(
                        icon = Icons.Outlined.CleaningServices,
                        label = stringResource(R.string.editor_action_eraser_short),
                        selected = erasing,
                        onClick = { onErase(!erasing) }
                    )
                    CompactPillButton(
                        icon = Icons.Outlined.DeleteSweep,
                        label = stringResource(R.string.editor_action_clear_short),
                        onClick = onClearStrokes
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!shapeFilled || shapeType == ShapeType.ARROW || shapeType == ShapeType.LINE) {
                        Text(
                            text = stringResource(R.string.editor_action_size_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = shapeStrokeSize,
                            onValueChange = onShapeStrokeSizeChange,
                            valueRange = 0.005f..0.045f,
                            modifier = Modifier.weight(0.45f)
                        )
                    }
                    CompactColorSwatches(
                        selectedColor = brushColor,
                        active = !erasing,
                        onSelect = onColor,
                        modifier = Modifier.weight(0.55f)
                    )
                }
            }
            MarkupSubMode.TEXT -> {
                val actionsScroll = rememberScrollState()
                val bgLabel = when (textBgStyle) {
                    0 -> stringResource(R.string.editor_text_bg_none)
                    1 -> stringResource(R.string.editor_text_bg_light)
                    else -> stringResource(R.string.editor_text_bg_dark)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingHorizontalEdges(actionsScroll, edgeWidth = 18.dp)
                        .horizontalScroll(actionsScroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactPillButton(
                        icon = Icons.Outlined.Edit,
                        label = if (selectedOverlay != null) stringResource(R.string.editor_text_edit) else stringResource(R.string.editor_add_text),
                        selected = true,
                        onClick = onOpenTextInput
                    )
                    CompactPillButton(
                        icon = null,
                        label = "${stringResource(R.string.editor_text_style_short)}: $bgLabel",
                        onClick = onCycleTextBgStyle
                    )
                    if (selectedOverlay != null) {
                        if (abs(selectedOverlay.rotationDeg) > 0.5f) {
                            CompactPillButton(
                                icon = Icons.Outlined.RestartAlt,
                                label = "${selectedOverlay.rotationDeg.roundToInt()}°",
                                onClick = onResetTextRotation
                            )
                        }
                        CompactPillButton(
                            icon = Icons.Outlined.TextFields,
                            label = stringResource(R.string.editor_text_new),
                            onClick = {
                                onDeselectText()
                                onOpenTextInput()
                            }
                        )
                        CompactPillButton(
                            icon = Icons.Outlined.Delete,
                            label = stringResource(R.string.action_delete),
                            destructive = true,
                            onClick = onDeleteText
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.editor_action_size_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = textSizeRatio,
                        onValueChange = onTextSizeChange,
                        valueRange = 0.02f..0.24f,
                        modifier = Modifier.weight(0.45f)
                    )
                    CompactColorSwatches(
                        selectedColor = textColor,
                        active = true,
                        onSelect = onTextColorChange,
                        modifier = Modifier.weight(0.55f)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPillButton(
            icon = Icons.Outlined.Brush,
            label = stringResource(R.string.editor_mode_draw),
            selected = subMode == MarkupSubMode.BRUSH,
            onClick = { onSubModeChange(MarkupSubMode.BRUSH) },
            modifier = Modifier.weight(1f)
        )
        CompactPillButton(
            icon = Icons.Outlined.Category,
            label = stringResource(R.string.editor_mode_shape),
            selected = subMode == MarkupSubMode.SHAPES,
            onClick = { onSubModeChange(MarkupSubMode.SHAPES) },
            modifier = Modifier.weight(1f)
        )
        CompactPillButton(
            icon = Icons.Outlined.TextFields,
            label = stringResource(R.string.editor_mode_text),
            selected = subMode == MarkupSubMode.TEXT,
            onClick = { onSubModeChange(MarkupSubMode.TEXT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrivacyDeck(
    subMode: PrivacySubMode,
    onSubModeChange: (PrivacySubMode) -> Unit,
    size: Float,
    strength: Float,
    erasing: Boolean,
    onSize: (Float) -> Unit,
    onStrength: (Float) -> Unit,
    onErase: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_action_size_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(58.dp)
            )
            Slider(
                value = size,
                onValueChange = onSize,
                valueRange = 0.015f..0.25f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(size * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_action_strength_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(58.dp)
            )
            Slider(
                value = strength,
                onValueChange = onStrength,
                valueRange = 4f..48f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${strength.roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End
            )
        }
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadingHorizontalEdges(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPillButton(
            icon = Icons.Outlined.BlurOn,
            label = stringResource(R.string.editor_mode_blur),
            selected = !erasing && subMode == PrivacySubMode.BLUR,
            onClick = { onSubModeChange(PrivacySubMode.BLUR) }
        )
        CompactPillButton(
            icon = Icons.Outlined.GridOn,
            label = stringResource(R.string.editor_mode_pixelate),
            selected = !erasing && subMode == PrivacySubMode.PIXELATE,
            onClick = { onSubModeChange(PrivacySubMode.PIXELATE) }
        )
        CompactPillButton(
            icon = Icons.Outlined.CleaningServices,
            label = stringResource(R.string.editor_action_eraser_short),
            selected = erasing,
            onClick = { onErase(!erasing) }
        )
        CompactPillButton(
            icon = Icons.Outlined.DeleteSweep,
            label = stringResource(R.string.editor_action_clear_short),
            onClick = onClear
        )
    }
}

@Composable
private fun CompactPillButton(
    icon: ImageVector?,
    label: String,
    selected: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        destructive -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        contentColor = fg,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextOverlayInputDialog(
    initialText: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_text_dialog_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(stringResource(R.string.editor_text_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) {
                Text(
                    if (isEditing) stringResource(R.string.editor_update_text)
                    else stringResource(R.string.editor_add_text)
                )
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
private fun SaveAsDialog(
    selectedFormat: ExportFormat,
    originalFormat: ExportFormat,
    overwriteOriginal: Boolean,
    onToggleOverwrite: (Boolean) -> Unit,
    onSelectFormat: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_format_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (overwriteOriginal)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOverwrite(!overwriteOriginal) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dialog_overwrite_original_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.dialog_overwrite_original_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = overwriteOriginal,
                            onCheckedChange = onToggleOverwrite
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = stringResource(R.string.format_original_subtitle, originalFormat.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExportFormat.entries.forEach { format ->
                    val isSelected = if (overwriteOriginal) format == originalFormat else selectedFormat == format
                    val desc = when (format) {
                        ExportFormat.JPEG -> stringResource(R.string.format_jpeg_desc)
                        ExportFormat.PNG -> stringResource(R.string.format_png_desc)
                        ExportFormat.WEBP -> stringResource(R.string.format_webp_desc)
                    }
                    Surface(
                        selected = isSelected,
                        onClick = { if (!overwriteOriginal) onSelectFormat(format) },
                        enabled = !overwriteOriginal,
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
                                onClick = { if (!overwriteOriginal) onSelectFormat(format) },
                                enabled = !overwriteOriginal
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
                Text(
                    if (overwriteOriginal) stringResource(R.string.action_overwrite)
                    else stringResource(R.string.action_save_copy)
                )
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
                val tabsScroll = rememberScrollState()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingHorizontalEdges(tabsScroll, edgeWidth = 16.dp)
                        .horizontalScroll(tabsScroll)
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
                        if (isMb) (num * 1_000_000).toLong() else (num * 1_000).toLong()
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
