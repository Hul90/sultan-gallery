package com.example.editor

import android.graphics.Color
import androidx.compose.ui.geometry.Offset

enum class DrawTool {
    BRUSH,
    HIGHLIGHTER,
    ERASER
}

data class DrawPoint(
    val x: Float, // Normalized 0..1 relative to image width
    val y: Float  // Normalized 0..1 relative to image height
)

data class DrawPath(
    val points: List<DrawPoint>,
    val color: Int = Color.RED,
    val strokeWidth: Float = 14f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false
)

data class TextSticker(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val color: Int = Color.WHITE,
    val backgroundColor: Int = Color.BLACK,
    val fontSize: Float = 24f,
    val normalizedX: Float = 0.5f, // Normalized 0..1 center
    val normalizedY: Float = 0.5f  // Normalized 0..1 center
)
