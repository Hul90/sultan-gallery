package com.example.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.model.FilterType

object ImageAdjustmentEngine {

    fun applyAdjustmentsAndFilter(
        source: Bitmap,
        brightness: Float = 0f, // -100 to 100
        contrast: Float = 1f,   // 0.5 to 2.0
        saturation: Float = 1f, // 0.0 to 2.0
        temperature: Float = 0f,// -50 to 50 (Warmth)
        tint: Float = 0f,       // -50 to 50 (Green/Magenta)
        vignette: Float = 0f,   // 0.0 to 1.0
        filter: FilterType = FilterType.ORIGINAL,
        rotationDegrees: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Build Color Matrix
        val cm = ColorMatrix()

        // Saturation
        cm.setSaturation(saturation.coerceIn(0f, 3f))

        // Contrast & Brightness
        // Formula: scale = contrast, translate = brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        // Temperature (Warmth -> Red/Yellow, Cool -> Blue)
        if (temperature != 0f) {
            val tempVal = temperature * 1.5f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, tempVal,
                    0f, 1f, 0f, 0f, tempVal * 0.4f,
                    0f, 0f, 1f, 0f, -tempVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tempMatrix)
        }

        // Tint (Green <-> Magenta)
        if (tint != 0f) {
            val tintVal = tint * 1.2f
            val tintMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, tintVal,
                    0f, 1f, 0f, 0f, -tintVal,
                    0f, 0f, 1f, 0f, tintVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tintMatrix)
        }

        // Filter Matrix
        val filterMatrix = getFilterMatrix(filter)
        if (filterMatrix != null) {
            cm.postConcat(filterMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Vignette effect
        if (vignette > 0.05f) {
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = Math.hypot(source.width.toDouble(), source.height.toDouble()).toFloat() / 2f
            val gradient = RadialGradient(
                source.width / 2f,
                source.height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((vignette * 220).toInt(), 0, 0, 0)),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            vignettePaint.shader = gradient
            canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), vignettePaint)
        }

        // Transformations: Rotation & Flips
        var finalBitmap = result
        if (rotationDegrees != 0f || flipHorizontal || flipVertical) {
            val matrix = Matrix()
            if (flipHorizontal) matrix.postScale(-1f, 1f, finalBitmap.width / 2f, finalBitmap.height / 2f)
            if (flipVertical) matrix.postScale(1f, -1f, finalBitmap.width / 2f, finalBitmap.height / 2f)
            if (rotationDegrees != 0f) matrix.postRotate(rotationDegrees)

            finalBitmap = Bitmap.createBitmap(
                finalBitmap,
                0,
                0,
                finalBitmap.width,
                finalBitmap.height,
                matrix,
                true
            )
        }

        return finalBitmap
    }

    private fun getFilterMatrix(filter: FilterType): ColorMatrix? {
        return when (filter) {
            FilterType.ORIGINAL -> null
            FilterType.VIVID -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 10f,
                    0f, 1.2f, 0f, 0f, 10f,
                    0f, 0f, 1.2f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.WARM -> ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 20f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.9f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.COOL -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0f, 0f, 5f,
                    0f, 0f, 1.2f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    1.1f, 0f, 0f, 0f, 10f,
                    0f, 1.0f, 0f, 0f, -5f,
                    0f, 0f, 0.95f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.VINTAGE -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, 30f,
                    0f, 0.8f, 0f, 0f, 20f,
                    0f, 0f, 0.6f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.BW -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                matrix
            }
            FilterType.PORTRAIT -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 15f,
                    0f, 1.04f, 0f, 0f, 8f,
                    0f, 0f, 1.0f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.DRAMATIC -> ColorMatrix(
                floatArrayOf(
                    1.3f, 0f, 0f, 0f, -20f,
                    0f, 1.3f, 0f, 0f, -20f,
                    0f, 0f, 1.3f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.SOFT -> ColorMatrix(
                floatArrayOf(
                    0.95f, 0f, 0f, 0f, 20f,
                    0f, 0.95f, 0f, 0f, 20f,
                    0f, 0f, 0.95f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.HDR -> ColorMatrix(
                floatArrayOf(
                    1.4f, -0.1f, -0.1f, 0f, 5f,
                    -0.1f, 1.4f, -0.1f, 0f, 5f,
                    -0.1f, -0.1f, 1.4f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }
}
