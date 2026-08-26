package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SultanPhotoCollage {

    suspend fun createCollage(
        context: Context,
        uris: List<Uri>,
        backgroundColor: Int = Color.BLACK,
        padding: Int = 16,
        cornerRadius: Float = 12f,
        outputWidth: Int = 1200,
        outputHeight: Int = 1200
    ): Bitmap = withContext(Dispatchers.IO) {
        val count = uris.size.coerceIn(2, 9)
        val bitmaps = uris.take(count).mapNotNull { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }

        val resultBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (bitmaps.size) {
            2 -> {
                // Split vertically (Left / Right)
                val halfW = (outputWidth - (padding * 3)) / 2
                val h = outputHeight - (padding * 2)

                drawScaledBitmap(canvas, bitmaps[0], RectF(padding.toFloat(), padding.toFloat(), (padding + halfW).toFloat(), (padding + h).toFloat()), paint)
                drawScaledBitmap(canvas, bitmaps[1], RectF((padding * 2 + halfW).toFloat(), padding.toFloat(), (outputWidth - padding).toFloat(), (padding + h).toFloat()), paint)
            }
            3 -> {
                // 1 Top large, 2 Bottom
                val topH = (outputHeight - (padding * 3)) / 2
                val topW = outputWidth - (padding * 2)
                drawScaledBitmap(canvas, bitmaps[0], RectF(padding.toFloat(), padding.toFloat(), (padding + topW).toFloat(), (padding + topH).toFloat()), paint)

                val bottomH = topH
                val bottomW = (outputWidth - (padding * 3)) / 2
                val topOffset = padding * 2 + topH
                drawScaledBitmap(canvas, bitmaps[1], RectF(padding.toFloat(), topOffset.toFloat(), (padding + bottomW).toFloat(), (topOffset + bottomH).toFloat()), paint)
                drawScaledBitmap(canvas, bitmaps[2], RectF((padding * 2 + bottomW).toFloat(), topOffset.toFloat(), (outputWidth - padding).toFloat(), (topOffset + bottomH).toFloat()), paint)
            }
            4 -> {
                // 2x2 Grid
                val cellW = (outputWidth - (padding * 3)) / 2
                val cellH = (outputHeight - (padding * 3)) / 2

                val rects = listOf(
                    RectF(padding.toFloat(), padding.toFloat(), (padding + cellW).toFloat(), (padding + cellH).toFloat()),
                    RectF((padding * 2 + cellW).toFloat(), padding.toFloat(), (outputWidth - padding).toFloat(), (padding + cellH).toFloat()),
                    RectF(padding.toFloat(), (padding * 2 + cellH).toFloat(), (padding + cellW).toFloat(), (outputHeight - padding).toFloat()),
                    RectF((padding * 2 + cellW).toFloat(), (padding * 2 + cellH).toFloat(), (outputWidth - padding).toFloat(), (outputHeight - padding).toFloat())
                )

                bitmaps.forEachIndexed { index, bmp ->
                    if (index < rects.size) {
                        drawScaledBitmap(canvas, bmp, rects[index], paint)
                    }
                }
            }
            else -> {
                // 3x2 or 3x3 Grid
                val cols = 3
                val rows = if (bitmaps.size <= 6) 2 else 3
                val cellW = (outputWidth - (padding * (cols + 1))) / cols
                val cellH = (outputHeight - (padding * (rows + 1))) / rows

                bitmaps.forEachIndexed { i, bmp ->
                    val r = i / cols
                    val c = i % cols
                    if (r < rows) {
                        val left = padding + c * (cellW + padding)
                        val top = padding + r * (cellH + padding)
                        val right = left + cellW
                        val bottom = top + cellH
                        drawScaledBitmap(canvas, bmp, RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()), paint)
                    }
                }
            }
        }

        resultBitmap
    }

    private fun drawScaledBitmap(canvas: Canvas, bitmap: Bitmap, destRect: RectF, paint: Paint) {
        val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val destRatio = destRect.width() / destRect.height()

        val srcCrop: Rect
        if (srcRatio > destRatio) {
            val cropW = (bitmap.height * destRatio).toInt()
            val left = (bitmap.width - cropW) / 2
            srcCrop = Rect(left, 0, left + cropW, bitmap.height)
        } else {
            val cropH = (bitmap.width / destRatio).toInt()
            val top = (bitmap.height - cropH) / 2
            srcCrop = Rect(0, top, bitmap.width, top + cropH)
        }

        canvas.drawBitmap(bitmap, srcCrop, destRect, paint)
    }
}

object SultanContactSheet {

    suspend fun generateContactSheet(
        context: Context,
        items: List<MediaItem>,
        columns: Int = 4,
        includeMetadata: Boolean = true,
        headerTitle: String = "SULTAN GALLERY CONTACT SHEET"
    ): Bitmap = withContext(Dispatchers.IO) {
        val count = items.size.coerceAtMost(36)
        val selectedItems = items.take(count)
        val rows = (selectedItems.size + columns - 1) / columns

        val cellWidth = 300
        val cellHeight = if (includeMetadata) 360 else 300
        val padding = 20
        val headerHeight = 120

        val totalWidth = columns * cellWidth + (columns + 1) * padding
        val totalHeight = headerHeight + rows * cellHeight + (rows + 1) * padding

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#090B10"))

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            isFakeBoldText = true
        }

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = 20f
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 16f
        }

        // Draw Header
        canvas.drawText(headerTitle, padding.toFloat(), 50f, textPaint)
        val dateStr = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated by SULTAN GALLERY • $count Items • $dateStr", padding.toFloat(), 85f, subTextPaint)

        // Draw Cells
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        selectedItems.forEachIndexed { index, mediaItem ->
            val col = index % columns
            val row = index / columns

            val left = padding + col * (cellWidth + padding)
            val top = headerHeight + padding + row * (cellHeight + padding)
            val imgBottom = top + (if (includeMetadata) cellHeight - 60 else cellHeight)

            val destRect = RectF(left.toFloat(), top.toFloat(), (left + cellWidth).toFloat(), imgBottom.toFloat())

            // Decode image
            try {
                context.contentResolver.openInputStream(mediaItem.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val itemBmp = BitmapFactory.decodeStream(stream, null, options)
                    if (itemBmp != null) {
                        canvas.drawBitmap(itemBmp, null, destRect, imgPaint)
                    }
                }
            } catch (_: Exception) {}

            if (includeMetadata) {
                val nameShort = if (mediaItem.displayName.length > 22) {
                    mediaItem.displayName.take(19) + "..."
                } else {
                    mediaItem.displayName
                }
                canvas.drawText(nameShort, left.toFloat(), (imgBottom + 24).toFloat(), metaPaint)
                val metaStr = "${mediaItem.formattedSize} • ${dateFormat.format(Date(mediaItem.dateAdded))}"
                canvas.drawText(metaStr, left.toFloat(), (imgBottom + 46).toFloat(), metaPaint)
            }
        }

        bitmap
    }
}
