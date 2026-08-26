package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.example.data.model.CropPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object SultanSmartCrop {

    fun calculateCropRect(imageWidth: Int, imageHeight: Int, preset: CropPreset): Rect {
        if (preset == CropPreset.FREE || preset.ratioX <= 0f || preset.ratioY <= 0f) {
            return Rect(0, 0, imageWidth, imageHeight)
        }

        val targetRatio = preset.ratioX / preset.ratioY
        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int

        if (imageRatio > targetRatio) {
            // Image is wider than target ratio
            cropHeight = imageHeight
            cropWidth = (imageHeight * targetRatio).toInt()
        } else {
            // Image is taller than target ratio
            cropWidth = imageWidth
            cropHeight = (imageWidth / targetRatio).toInt()
        }

        val left = (imageWidth - cropWidth) / 2
        val top = (imageHeight - cropHeight) / 2
        return Rect(left, top, left + cropWidth, top + cropHeight)
    }

    fun cropBitmap(source: Bitmap, cropRect: Rect): Bitmap {
        val safeLeft = cropRect.left.coerceIn(0, source.width - 1)
        val safeTop = cropRect.top.coerceIn(0, source.height - 1)
        val safeWidth = cropRect.width().coerceIn(1, source.width - safeLeft)
        val safeHeight = cropRect.height().coerceIn(1, source.height - safeTop)
        return Bitmap.createBitmap(source, safeLeft, safeTop, safeWidth, safeHeight)
    }
}

object SultanImageCompressor {

    enum class CompressionLevel(val title: String, val quality: Int, val scaleFactor: Float) {
        MAX("Maximum Quality", 95, 1.0f),
        HIGH("High Quality", 80, 0.85f),
        MEDIUM("Medium Quality", 60, 0.70f),
        LOW("Compact File", 40, 0.50f),
        CUSTOM("Custom Quality", 75, 1.0f)
    }

    data class CompressionResult(
        val compressedBitmap: Bitmap,
        val originalSize: Long,
        val estimatedSize: Long,
        val width: Int,
        val height: Int,
        val quality: Int
    )

    suspend fun compress(
        context: Context,
        uri: Uri,
        level: CompressionLevel,
        customQuality: Int = 75
    ): CompressionResult = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
        val originalBytes = input?.readBytes() ?: ByteArray(0)
        val originalSize = originalBytes.size.toLong()

        val originalBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            ?: throw IllegalStateException("Could not decode image")

        val quality = if (level == CompressionLevel.CUSTOM) customQuality.coerceIn(10, 100) else level.quality
        val scale = level.scaleFactor

        val targetWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(100)
        val targetHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(100)

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val compressedBytes = outputStream.toByteArray()

        CompressionResult(
            compressedBitmap = scaledBitmap,
            originalSize = originalSize,
            estimatedSize = compressedBytes.size.toLong(),
            width = scaledBitmap.width,
            height = scaledBitmap.height,
            quality = quality
        )
    }
}
