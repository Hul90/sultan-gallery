package com.example.editor

import android.graphics.Color
import android.graphics.Path
import androidx.compose.ui.geometry.Offset

enum class DrawTool {
    BRUSH,
    HIGHLIGHTER,
    ERASER,
    RECTANGLE,
    CIRCLE,
    ARROW,
    TEXT,
    MOSAIC
}

data class DrawPath(
    val path: Path,
    val color: Int = Color.RED,
    val strokeWidth: Float = 10f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val isMosaic: Boolean = false
)

data class ShapeElement(
    val tool: DrawTool,
    val start: Offset,
    val end: Offset,
    val color: Int,
    val strokeWidth: Float
)

data class TextSticker(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val color: Int = Color.WHITE,
    val backgroundColor: Int = Color.BLACK,
    val fontSize: Float = 28f,
    val position: Offset = Offset(100f, 100f),
    val rotation: Float = 0f,
    val scale: Float = 1.0f
)
