package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CropPreset
import com.example.data.model.FilterType
import com.example.tools.SultanPhotoCollage
import com.example.tools.SultanSmartCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorSessionState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.ADJUST,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val vignette: Float = 0f,
    val activeFilter: FilterType = FilterType.ORIGINAL,
    val rotationAngle: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val selectedCropPreset: CropPreset = CropPreset.FREE,
    val currentDrawTool: DrawTool = DrawTool.BRUSH,
    val drawColor: Int = Color.RED,
    val brushSize: Float = 14f,
    val paths: List<DrawPath> = emptyList(),
    val textStickers: List<TextSticker> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val showOriginal: Boolean = false
)

enum class EditorTab(val label: String) {
    ADJUST("Adjust"),
    FILTERS("Filters"),
    CROP("Crop"),
    TRANSFORM("Rotate/Flip"),
    DRAW("Draw & Paint"),
    TEXT("Text & Stickers"),
    COLLAGE("Collage Studio")
}

class PhotoEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorSessionState())
    val uiState: StateFlow<EditorSessionState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<EditorSessionState>()
    private val redoStack = mutableListOf<EditorSessionState>()

    fun loadBitmapFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 2560 || options.outHeight / sampleSize > 2560) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    if (bitmap != null) {
                        undoStack.clear()
                        redoStack.clear()
                        _uiState.update {
                            it.copy(
                                originalBitmap = bitmap,
                                previewBitmap = bitmap,
                                canUndo = false,
                                canRedo = false,
                                paths = emptyList(),
                                textStickers = emptyList(),
                                brightness = 0f,
                                contrast = 1f,
                                saturation = 1f,
                                temperature = 0f,
                                tint = 0f,
                                vignette = 0f,
                                activeFilter = FilterType.ORIGINAL,
                                rotationAngle = 0f,
                                flipH = false,
                                flipV = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setActiveTab(tab: EditorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setShowOriginal(show: Boolean) {
        _uiState.update { it.copy(showOriginal = show) }
    }

    fun updateAdjustment(
        brightness: Float? = null,
        contrast: Float? = null,
        saturation: Float? = null,
        temperature: Float? = null,
        tint: Float? = null,
        vignette: Float? = null
    ) {
        saveStateForUndo()
        _uiState.update { state ->
            state.copy(
                brightness = brightness ?: state.brightness,
                contrast = contrast ?: state.contrast,
                saturation = saturation ?: state.saturation,
                temperature = temperature ?: state.temperature,
                tint = tint ?: state.tint,
                vignette = vignette ?: state.vignette
            )
        }
        recomputePreview()
    }

    fun resetAdjustments() {
        saveStateForUndo()
        _uiState.update {
            it.copy(
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
                temperature = 0f,
                tint = 0f,
                vignette = 0f,
                activeFilter = FilterType.ORIGINAL
            )
        }
        recomputePreview()
    }

    fun setFilter(filter: FilterType) {
        saveStateForUndo()
        _uiState.update { it.copy(activeFilter = filter) }
        recomputePreview()
    }

    fun rotate90(clockwise: Boolean = true) {
        saveStateForUndo()
        val delta = if (clockwise) 90f else -90f
        _uiState.update { it.copy(rotationAngle = (it.rotationAngle + delta + 360f) % 360f) }
        recomputePreview()
    }

    fun toggleFlipHorizontal() {
        saveStateForUndo()
        _uiState.update { it.copy(flipH = !it.flipH) }
        recomputePreview()
    }

    fun toggleFlipVertical() {
        saveStateForUndo()
        _uiState.update { it.copy(flipV = !it.flipV) }
        recomputePreview()
    }

    fun applyCropPreset(preset: CropPreset) {
        val currentBitmap = _uiState.value.previewBitmap ?: return
        if (preset == CropPreset.FREE) {
            _uiState.update { it.copy(selectedCropPreset = preset) }
            return
        }
        saveStateForUndo()
        val rect = SultanSmartCrop.calculateCropRect(currentBitmap.width, currentBitmap.height, preset)
        val cropped = SultanSmartCrop.cropBitmap(currentBitmap, rect)
        _uiState.update {
            it.copy(
                originalBitmap = cropped,
                previewBitmap = cropped,
                selectedCropPreset = preset,
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
                temperature = 0f,
                tint = 0f,
                vignette = 0f,
                activeFilter = FilterType.ORIGINAL,
                rotationAngle = 0f,
                flipH = false,
                flipV = false
            )
        }
    }

    fun addDrawPath(drawPath: DrawPath) {
        if (drawPath.points.size < 2) return
        saveStateForUndo()
        _uiState.update { it.copy(paths = it.paths + drawPath) }
    }

    fun clearDrawPaths() {
        if (_uiState.value.paths.isNotEmpty()) {
            saveStateForUndo()
            _uiState.update { it.copy(paths = emptyList()) }
        }
    }

    fun setDrawTool(tool: DrawTool) {
        _uiState.update { it.copy(currentDrawTool = tool) }
    }

    fun setDrawColor(color: Int) {
        _uiState.update { it.copy(drawColor = color) }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size) }
    }

    fun addTextSticker(text: String, color: Int = Color.WHITE, bgColor: Int = Color.BLACK, fontSize: Float = 24f) {
        if (text.isBlank()) return
        saveStateForUndo()
        val sticker = TextSticker(
            text = text,
            color = color,
            backgroundColor = bgColor,
            fontSize = fontSize,
            normalizedX = 0.5f,
            normalizedY = 0.5f
        )
        _uiState.update { it.copy(textStickers = it.textStickers + sticker) }
    }

    fun updateTextStickerPosition(id: Long, normX: Float, normY: Float) {
        _uiState.update { state ->
            val updated = state.textStickers.map {
                if (it.id == id) it.copy(
                    normalizedX = normX.coerceIn(0.05f, 0.95f),
                    normalizedY = normY.coerceIn(0.05f, 0.95f)
                ) else it
            }
            state.copy(textStickers = updated)
        }
    }

    fun removeTextSticker(id: Long) {
        saveStateForUndo()
        _uiState.update { state ->
            state.copy(textStickers = state.textStickers.filter { it.id != id })
        }
    }

    fun createCollageInEditor(
        context: Context,
        uris: List<Uri>,
        backgroundColor: Int = Color.BLACK,
        padding: Int = 16,
        cornerRadius: Float = 12f
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collageBmp = SultanPhotoCollage.createCollage(
                    context = context,
                    uris = uris,
                    backgroundColor = backgroundColor,
                    padding = padding,
                    cornerRadius = cornerRadius
                )
                saveStateForUndo()
                _uiState.update {
                    it.copy(
                        originalBitmap = collageBmp,
                        previewBitmap = collageBmp,
                        paths = emptyList(),
                        textStickers = emptyList(),
                        brightness = 0f,
                        contrast = 1f,
                        saturation = 1f,
                        temperature = 0f,
                        tint = 0f,
                        vignette = 0f,
                        activeFilter = FilterType.ORIGINAL,
                        rotationAngle = 0f,
                        flipH = false,
                        flipV = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveStateForUndo() {
        val current = _uiState.value
        undoStack.add(current)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value
        redoStack.add(current)
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _uiState.value = previous.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.value = next.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
    }

    private fun recomputePreview() {
        val orig = _uiState.value.originalBitmap ?: return
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.Default) {
            val adjusted = ImageAdjustmentEngine.applyAdjustmentsAndFilter(
                source = orig,
                brightness = state.brightness,
                contrast = state.contrast,
                saturation = state.saturation,
                temperature = state.temperature,
                tint = state.tint,
                vignette = state.vignette,
                filter = state.activeFilter,
                rotationDegrees = state.rotationAngle,
                flipHorizontal = state.flipH,
                flipVertical = state.flipV
            )
            _uiState.update { it.copy(previewBitmap = adjusted) }
        }
    }

    suspend fun renderFinalBitmap(): Bitmap? = withContext(Dispatchers.Default) {
        val base = _uiState.value.previewBitmap ?: return@withContext null
        val state = _uiState.value

        if (state.paths.isEmpty() && state.textStickers.isEmpty()) {
            return@withContext base
        }

        val result = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(base, 0f, 0f, null)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        val imgW = base.width.toFloat()
        val imgH = base.height.toFloat()

        // Render drawing paths
        for (dp in state.paths) {
            if (dp.points.size < 2) continue
            strokePaint.color = dp.color
            // Scale stroke relative to image dimension
            val scaleFactor = (imgW.coerceAtLeast(imgH)) / 1000f
            strokePaint.strokeWidth = dp.strokeWidth * scaleFactor.coerceAtLeast(1.0f)
            strokePaint.alpha = if (dp.isHighlighter) 120 else 255

            val path = Path()
            path.moveTo(dp.points[0].x * imgW, dp.points[0].y * imgH)
            for (i in 1 until dp.points.size) {
                path.lineTo(dp.points[i].x * imgW, dp.points[i].y * imgH)
            }
            canvas.drawPath(path, strokePaint)
        }

        // Render Text stickers
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFakeBoldText = true
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (sticker in state.textStickers) {
            val textSize = sticker.fontSize * (imgW / 400f).coerceIn(1.5f, 5.0f)
            textPaint.textSize = textSize
            textPaint.color = sticker.color

            val bounds = Rect()
            textPaint.getTextBounds(sticker.text, 0, sticker.text.length, bounds)

            val posX = sticker.normalizedX * imgW
            val posY = sticker.normalizedY * imgH

            val padX = textSize * 0.4f
            val padY = textSize * 0.3f
            val bgRect = RectF(
                posX - bounds.width() / 2f - padX,
                posY - bounds.height() / 2f - padY,
                posX + bounds.width() / 2f + padX,
                posY + bounds.height() / 2f + padY
            )

            bgPaint.color = sticker.backgroundColor
            bgPaint.alpha = 210
            canvas.drawRoundRect(bgRect, 18f, 18f, bgPaint)

            // Draw text centered
            val textX = posX - bounds.width() / 2f - bounds.left
            val textY = posY + bounds.height() / 2f - bounds.bottom
            canvas.drawText(sticker.text, textX, textY, textPaint)
        }

        result
    }
}
