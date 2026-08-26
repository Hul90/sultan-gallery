package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CropPreset
import com.example.data.model.FilterType
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
    val isSaving: Boolean = false
)

enum class EditorTab(val label: String) {
    ADJUST("Adjust"),
    FILTERS("Filters"),
    CROP("Crop"),
    TRANSFORM("Rotate/Flip"),
    DRAW("Draw & Paint"),
    TEXT("Text & Stickers")
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
                    val options = BitmapFactory.Options().apply {
                        // Max dimension 2560 for performance and memory safety
                        inJustDecodeBounds = true
                    }
                    val bytes = stream.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 2560 || options.outHeight / sampleSize > 2560) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    if (bitmap != null) {
                        _uiState.update {
                            it.copy(
                                originalBitmap = bitmap,
                                previewBitmap = bitmap
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

    fun setFilter(filter: FilterType) {
        saveStateForUndo()
        _uiState.update { it.copy(activeFilter = filter) }
        recomputePreview()
    }

    fun rotate90() {
        saveStateForUndo()
        _uiState.update { it.copy(rotationAngle = (it.rotationAngle + 90f) % 360f) }
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
        saveStateForUndo()
        val rect = SultanSmartCrop.calculateCropRect(currentBitmap.width, currentBitmap.height, preset)
        val cropped = SultanSmartCrop.cropBitmap(currentBitmap, rect)
        _uiState.update {
            it.copy(
                originalBitmap = cropped,
                previewBitmap = cropped,
                selectedCropPreset = preset
            )
        }
    }

    fun addDrawPath(drawPath: DrawPath) {
        saveStateForUndo()
        _uiState.update { it.copy(paths = it.paths + drawPath) }
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

    fun addTextSticker(text: String, color: Int = Color.WHITE, bgColor: Int = Color.BLACK) {
        if (text.isBlank()) return
        saveStateForUndo()
        val sticker = TextSticker(
            text = text,
            color = color,
            backgroundColor = bgColor,
            position = androidx.compose.ui.geometry.Offset(200f, 200f)
        )
        _uiState.update { it.copy(textStickers = it.textStickers + sticker) }
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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        // Draw overlay paths
        for (dp in state.paths) {
            paint.color = dp.color
            paint.strokeWidth = dp.strokeWidth
            if (dp.isHighlighter) {
                paint.alpha = 110
            } else {
                paint.alpha = 255
            }
            canvas.drawPath(dp.path, paint)
        }

        // Draw Text stickers
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            isFakeBoldText = true
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (sticker in state.textStickers) {
            textPaint.color = sticker.color
            bgPaint.color = sticker.backgroundColor
            bgPaint.alpha = 180

            val bounds = Rect()
            textPaint.getTextBounds(sticker.text, 0, sticker.text.length, bounds)
            val padding = 16f
            val rect = androidx.compose.ui.geometry.Rect(
                sticker.position.x - padding,
                sticker.position.y - bounds.height() - padding,
                sticker.position.x + bounds.width() + padding,
                sticker.position.y + padding
            )
            canvas.drawRoundRect(
                rect.left, rect.top, rect.right, rect.bottom,
                16f, 16f, bgPaint
            )
            canvas.drawText(sticker.text, sticker.position.x, sticker.position.y, textPaint)
        }

        result
    }
}
