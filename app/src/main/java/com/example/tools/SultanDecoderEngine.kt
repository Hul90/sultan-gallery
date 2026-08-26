package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object SultanDecoderEngine {

    data class PdfPageInfo(
        val pageCount: Int,
        val currentPageBitmap: Bitmap?
    )

    /**
     * Renders a specific page from a PDF document safely.
     */
    suspend fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int = 0,
        targetWidth: Int = 1440
    ): PdfPageInfo = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext PdfPageInfo(0, null)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) return@withContext PdfPageInfo(0, null)

            val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
            val page = renderer.openPage(safeIndex)

            val scale = (targetWidth.toFloat() / page.width.toFloat()).coerceAtLeast(1.0f)
            val renderWidth = (page.width * scale).toInt().coerceIn(400, 2560)
            val renderHeight = (page.height * scale).toInt().coerceIn(400, 3840)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            PdfPageInfo(pageCount = pageCount, currentPageBitmap = bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            PdfPageInfo(0, null)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Safe memory-bounded bitmap decoder for raster/RAW images.
     */
    suspend fun decodeSafeBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First check if it is a RAW image with an embedded thumbnail
            var rawExifThumbnail: Bitmap? = null
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    if (exif.hasThumbnail()) {
                        val thumbBytes = exif.thumbnailBytes
                        if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                            rawExifThumbnail = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size)
                        }
                    }
                }
            } catch (_: Exception) {}

            // Measure dimensions
            var options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            decoded ?: rawExifThumbnail
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates an editable PNG/JPEG copy from any visual media (RAW, PDF, SVG, etc.).
     */
    suspend fun createEditableCopy(
        context: Context,
        item: MediaItem,
        targetFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): File? = withContext(Dispatchers.IO) {
        try {
            val extension = if (targetFormat == Bitmap.CompressFormat.PNG) "png" else "jpg"
            val outputFile = File(
                context.cacheDir,
                "SULTAN_EDIT_${System.currentTimeMillis()}_${item.displayName.substringBeforeLast('.')}.$extension"
            )

            val formatInfo = SultanFormatDetector.analyzeMedia(context, item)
            var sourceBitmap: Bitmap? = null

            if (formatInfo.category == FormatCategory.DOCUMENT) {
                val pdfInfo = renderPdfPage(context, item.uri, pageIndex = 0, targetWidth = 1920)
                sourceBitmap = pdfInfo.currentPageBitmap
            } else {
                sourceBitmap = decodeSafeBitmap(context, item.uri, maxDimension = 2560)
            }

            if (sourceBitmap == null) return@withContext null

            FileOutputStream(outputFile).use { out ->
                sourceBitmap.compress(targetFormat, 95, out)
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a list of image items into a single PDF document.
     */
    suspend fun convertImagesToPdf(
        context: Context,
        mediaItems: List<MediaItem>,
        outputPdfFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            var pageNum = 1
            for (item in mediaItems) {
                val bitmap = decodeSafeBitmap(context, item.uri, maxDimension = 1600) ?: continue
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNum).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                document.finishPage(page)
                pageNum++
            }

            FileOutputStream(outputPdfFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }
}
