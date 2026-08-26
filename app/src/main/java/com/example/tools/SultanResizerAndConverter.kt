package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object SultanImageResizer {

    data class ResizePreset(val name: String, val width: Int, val height: Int)

    val PRESETS = listOf(
        ResizePreset("Full HD (1920x1080)", 1920, 1080),
        ResizePreset("HD (1280x720)", 1280, 720),
        ResizePreset("Square (1080x1080)", 1080, 1080),
        ResizePreset("VGA (640x480)", 640, 480),
        ResizePreset("Web Small (800x600)", 800, 600)
    )

    fun resize(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        maintainAspectRatio: Boolean
    ): Bitmap {
        val finalWidth: Int
        val finalHeight: Int

        if (maintainAspectRatio) {
            val srcRatio = source.width.toFloat() / source.height.toFloat()
            val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

            if (srcRatio > targetRatio) {
                finalWidth = targetWidth
                finalHeight = (targetWidth / srcRatio).toInt()
            } else {
                finalHeight = targetHeight
                finalWidth = (targetHeight * srcRatio).toInt()
            }
        } else {
            finalWidth = targetWidth
            finalHeight = targetHeight
        }

        return Bitmap.createScaledBitmap(
            source,
            finalWidth.coerceAtLeast(10),
            finalHeight.coerceAtLeast(10),
            true
        )
    }
}

object SultanFormatConverter {

    enum class TargetFormat(val title: String, val extension: String, val mimeType: String, val format: Bitmap.CompressFormat?) {
        JPG("JPEG Image (.jpg)", "jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
        PNG("Lossless PNG (.png)", "png", "image/png", Bitmap.CompressFormat.PNG),
        WEBP("Google WebP (.webp)", "webp", "image/webp", Bitmap.CompressFormat.WEBP),
        PDF("PDF Visual Document (.pdf)", "pdf", "application/pdf", null)
    }

    data class ConversionResult(
        val outputBytes: ByteArray,
        val format: TargetFormat,
        val outputSize: Long
    )

    suspend fun convert(
        context: Context,
        uri: Uri,
        targetFormat: TargetFormat,
        quality: Int = 90
    ): Pair<Bitmap, ConversionResult> = withContext(Dispatchers.IO) {
        // Try safe decoder first (supports standard, RAW, PDF, SVG, etc.)
        val bitmap = SultanDecoderEngine.decodeSafeBitmap(context, uri, maxDimension = 2560)
            ?: run {
                // Fallback to PDF page renderer if PDF
                val pdfInfo = SultanDecoderEngine.renderPdfPage(context, uri, pageIndex = 0, targetWidth = 1920)
                pdfInfo.currentPageBitmap
            }
            ?: throw IllegalStateException("Unable to decode source file for conversion.")

        val output = ByteArrayOutputStream()
        if (targetFormat == TargetFormat.PDF) {
            val doc = android.graphics.pdf.PdfDocument()
            try {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                doc.finishPage(page)
                doc.writeTo(output)
            } finally {
                doc.close()
            }
        } else {
            val compressFormat = targetFormat.format ?: Bitmap.CompressFormat.PNG
            bitmap.compress(compressFormat, quality.coerceIn(10, 100), output)
        }

        val bytes = output.toByteArray()
        val result = ConversionResult(
            outputBytes = bytes,
            format = targetFormat,
            outputSize = bytes.size.toLong()
        )
        Pair(bitmap, result)
    }

    suspend fun imagesToPdf(
        context: Context,
        uris: List<Uri>
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        val doc = android.graphics.pdf.PdfDocument()
        val output = ByteArrayOutputStream()
        try {
            var pageNum = 1
            for (uri in uris) {
                val bitmap = SultanDecoderEngine.decodeSafeBitmap(context, uri, maxDimension = 1920)
                if (bitmap != null) {
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNum++).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = android.graphics.Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    doc.finishPage(page)
                }
            }
            if (pageNum > 1) {
                doc.writeTo(output)
                output.toByteArray()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            doc.close()
        }
    }
}
