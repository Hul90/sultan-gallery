package com.example.tools

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

enum class FormatCategory(val displayName: String) {
    RASTER_STANDARD("Standard Raster"),
    HIGH_EFFICIENCY("High-Efficiency Photo"),
    CAMERA_RAW("Camera RAW Photo"),
    VECTOR("Vector Graphic"),
    DOCUMENT("Visual Document / PDF"),
    DESIGN_PSD("Adobe Photoshop Document"),
    ANIMATED("Animated Media"),
    NEXT_GEN("Next-Gen Format"),
    SPECIALIZED("Specialized / Legacy Format"),
    VIDEO("Video"),
    AUDIO("Audio"),
    UNKNOWN("Other Media")
}

data class RawCameraMetadata(
    val make: String = "",
    val model: String = "",
    val lensModel: String = "",
    val iso: String = "",
    val exposureTime: String = "",
    val fNumber: String = "",
    val focalLength: String = "",
    val dateTime: String = "",
    val gpsCoordinates: String = "",
    val whiteBalance: String = "",
    val flash: String = "",
    val colorSpace: String = ""
)

data class SultanFormatInfo(
    val formatName: String,
    val shortBadge: String,
    val category: FormatCategory,
    val extension: String,
    val mimeType: String,
    val container: String,
    val codec: String,
    val compression: String,
    val width: Int,
    val height: Int,
    val colorSpace: String,
    val bitDepth: String,
    val isHDR: Boolean,
    val isAnimated: Boolean,
    val hasAlpha: Boolean,
    val fileSize: Long,
    val estimatedUncompressedRam: Long,
    val isDirectlyEditable: Boolean,
    val isViewable: Boolean,
    val rawMetadata: RawCameraMetadata? = null,
    val exifAvailable: Boolean = false,
    val iccProfile: String = "sRGB / Standard",
    val magicSignatureHex: String = "",
    val description: String = ""
)

object SultanFormatDetector {

    /**
     * Inspects magic bytes and metadata to determine the real format.
     */
    suspend fun analyzeMedia(context: Context, item: MediaItem): SultanFormatInfo = withContext(Dispatchers.IO) {
        val extension = getExtension(item.displayName, item.path).lowercase(Locale.ROOT)
        val headerBytes = ByteArray(64)
        var bytesRead = 0

        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                bytesRead = stream.read(headerBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val magicHex = if (bytesRead > 0) {
            headerBytes.take(minOf(bytesRead, 16)).joinToString(" ") { "%02X".format(it) }
        } else ""

        // Extract EXIF data
        var rawMeta: RawCameraMetadata? = null
        var exifAvailable = false
        var detectedColorSpace = "sRGB"
        var isHDR = false
        var exifWidth = item.width
        var exifHeight = item.height

        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL) ?: ""
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: ""
                val exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: ""
                val fNum = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: ""
                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: ""
                val dt = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: ""
                val latLong = exif.latLong
                val gps = if (latLong != null) "${String.format("%.4f", latLong[0])}, ${String.format("%.4f", latLong[1])}" else ""
                val wb = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE) ?: ""
                val flash = exif.getAttribute(ExifInterface.TAG_FLASH) ?: ""
                val cs = exif.getAttribute(ExifInterface.TAG_COLOR_SPACE)

                if (cs == "1") detectedColorSpace = "sRGB"
                else if (cs == "2" || cs == "65535") detectedColorSpace = "Adobe RGB / Wide Gamut"

                val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (w > 0) exifWidth = w
                if (h > 0) exifHeight = h

                if (make.isNotEmpty() || model.isNotEmpty() || iso.isNotEmpty()) {
                    exifAvailable = true
                    rawMeta = RawCameraMetadata(
                        make = make,
                        model = model,
                        lensModel = lens,
                        iso = if (iso.isNotEmpty()) "ISO $iso" else "",
                        exposureTime = if (exp.isNotEmpty()) "${exp}s" else "",
                        fNumber = if (fNum.isNotEmpty()) "f/$fNum" else "",
                        focalLength = if (focal.isNotEmpty()) "${focal}mm" else "",
                        dateTime = dt,
                        gpsCoordinates = gps,
                        whiteBalance = if (wb == "1") "Manual" else if (wb == "0") "Auto" else wb,
                        flash = flash,
                        colorSpace = detectedColorSpace
                    )
                }
            }
        } catch (_: Exception) {
        }

        val effectiveWidth = if (exifWidth > 0) exifWidth else item.width
        val effectiveHeight = if (exifHeight > 0) exifHeight else item.height
        val uncompressedRam = (effectiveWidth.toLong() * effectiveHeight.toLong() * 4).coerceAtLeast(0L)

        // 1. Check Magic Bytes signatures
        // PDF: %PDF-
        if (bytesRead >= 5 && headerBytes[0] == 0x25.toByte() && headerBytes[1] == 0x50.toByte() &&
            headerBytes[2] == 0x44.toByte() && headerBytes[3] == 0x46.toByte() && headerBytes[4] == 0x2D.toByte() ||
            extension == "pdf"
        ) {
            return@withContext SultanFormatInfo(
                formatName = "Portable Document Format (PDF)",
                shortBadge = "PDF",
                category = FormatCategory.DOCUMENT,
                extension = "pdf",
                mimeType = "application/pdf",
                container = "Adobe PDF Document",
                codec = "Vector / Raster Mixed",
                compression = "Deflate / JBIG2 / JPEG",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "DeviceRGB / CMYK",
                bitDepth = "8-bit per channel",
                isHDR = false,
                isAnimated = false,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Visual PDF document supported with high-fidelity native page rendering, page navigation, and export to image."
            )
        }

        // SVG: <svg or <?xml ... <svg
        val headerString = if (bytesRead > 0) String(headerBytes, 0, minOf(bytesRead, 64), Charsets.UTF_8).lowercase(Locale.ROOT) else ""
        if (headerString.contains("<svg") || headerString.contains("<?xml") && extension == "svg" || extension == "svg") {
            return@withContext SultanFormatInfo(
                formatName = "Scalable Vector Graphics (SVG)",
                shortBadge = "SVG",
                category = FormatCategory.VECTOR,
                extension = "svg",
                mimeType = "image/svg+xml",
                container = "XML Vector Specification",
                codec = "Scalable Vector Paths",
                compression = "None / Lossless",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "sRGB / Vector Curves",
                bitDepth = "Infinite Resolution (Vector)",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Vector graphic rendered with infinite scaling fidelity without pixelation. Can be exported to high-res PNG/JPG."
            )
        }

        // Photoshop PSD / PSB: 8BPS
        if (bytesRead >= 4 && headerBytes[0] == 0x38.toByte() && headerBytes[1] == 0x42.toByte() &&
            headerBytes[2] == 0x50.toByte() && headerBytes[3] == 0x53.toByte() ||
            extension == "psd" || extension == "psb"
        ) {
            return@withContext SultanFormatInfo(
                formatName = if (extension == "psb") "Photoshop Large Document (PSB)" else "Adobe Photoshop Document (PSD)",
                shortBadge = if (extension == "psb") "PSB" else "PSD",
                category = FormatCategory.DESIGN_PSD,
                extension = extension.ifEmpty { "psd" },
                mimeType = "image/vnd.adobe.photoshop",
                container = "Adobe Photoshop PSD Layered",
                codec = "RLE / Raw Composite",
                compression = "PackBits RLE / Raw",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit / 16-bit / 32-bit",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Adobe Photoshop project file. Preview and composite rendering supported. Convert to PNG/JPG to edit."
            )
        }

        // GIF: GIF87a / GIF89a
        if (bytesRead >= 4 && headerBytes[0] == 0x47.toByte() && headerBytes[1] == 0x49.toByte() &&
            headerBytes[2] == 0x46.toByte() && headerBytes[3] == 0x38.toByte() ||
            extension == "gif"
        ) {
            return@withContext SultanFormatInfo(
                formatName = "Graphics Interchange Format (GIF)",
                shortBadge = "GIF",
                category = FormatCategory.ANIMATED,
                extension = "gif",
                mimeType = "image/gif",
                container = "GIF Animation Container",
                codec = "LZW Indexed Color",
                compression = "Lossless LZW",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "sRGB (256 Colors)",
                bitDepth = "8-bit Indexed",
                isHDR = false,
                isAnimated = true,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Animated GIF with full frame playback and animated loop controls. Edit frame as copy."
            )
        }

        // PNG: 89 50 4E 47
        if (bytesRead >= 4 && (headerBytes[0].toInt() and 0xFF == 0x89) && headerBytes[1] == 0x50.toByte() &&
            headerBytes[2] == 0x4E.toByte() && headerBytes[3] == 0x47.toByte() ||
            extension == "png" || extension == "apng"
        ) {
            val isApng = extension == "apng"
            return@withContext SultanFormatInfo(
                formatName = if (isApng) "Animated Portable Network Graphics (APNG)" else "Portable Network Graphics (PNG)",
                shortBadge = if (isApng) "APNG" else "PNG",
                category = if (isApng) FormatCategory.ANIMATED else FormatCategory.RASTER_STANDARD,
                extension = if (isApng) "apng" else "png",
                mimeType = if (isApng) "image/apng" else "image/png",
                container = "PNG Chunky Chunked Stream",
                codec = "Deflate / Inflate Filtered",
                compression = "Lossless Deflate (zlib)",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit / 16-bit Truecolor + Alpha",
                isHDR = false,
                isAnimated = isApng,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = !isApng,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Full lossless PNG with 32-bit RGBA transparency support and direct editing."
            )
        }

        // WebP: RIFF .... WEBP
        if (bytesRead >= 12 && headerBytes[0] == 0x52.toByte() && headerBytes[1] == 0x49.toByte() &&
            headerBytes[2] == 0x46.toByte() && headerBytes[3] == 0x46.toByte() &&
            headerBytes[8] == 0x57.toByte() && headerBytes[9] == 0x45.toByte() &&
            headerBytes[10] == 0x42.toByte() && headerBytes[11] == 0x50.toByte() ||
            extension == "webp"
        ) {
            return@withContext SultanFormatInfo(
                formatName = "Google WebP Image",
                shortBadge = "WEBP",
                category = FormatCategory.RASTER_STANDARD,
                extension = "webp",
                mimeType = "image/webp",
                container = "RIFF WebP Container",
                codec = "VP8 / VP8L / VP8X",
                compression = "Lossy VP8 & Lossless VP8L",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit + Alpha Transparency",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = true,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "High-efficiency WebP raster image with alpha transparency and direct photo editing support."
            )
        }

        // HEIF / HEIC / AVIF: ftyp box
        if (bytesRead >= 12 && headerBytes[4] == 0x66.toByte() && headerBytes[5] == 0x74.toByte() &&
            headerBytes[6] == 0x79.toByte() && headerBytes[7] == 0x70.toByte() ||
            extension in listOf("heic", "heif", "avif", "hif")
        ) {
            val brand = if (bytesRead >= 12) String(headerBytes, 8, 4, Charsets.US_ASCII).lowercase(Locale.ROOT) else ""
            val isAvif = brand.contains("avif") || brand.contains("avis") || extension == "avif"
            isHDR = isAvif || brand.contains("heix")

            return@withContext SultanFormatInfo(
                formatName = if (isAvif) "AV1 Image File Format (AVIF)" else "High Efficiency Image (HEIC/HEIF)",
                shortBadge = if (isAvif) "AVIF" else "HEIC",
                category = FormatCategory.HIGH_EFFICIENCY,
                extension = if (isAvif) "avif" else "heic",
                mimeType = if (isAvif) "image/avif" else "image/heic",
                container = "ISO Base Media File Format (ISOBMFF)",
                codec = if (isAvif) "AV1 Still Image Codec" else "HEVC (H.265) Still Picture",
                compression = "Ultra-High Efficiency Transform",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = if (isHDR) "Display P3 / BT.2020 (HDR)" else detectedColorSpace,
                bitDepth = if (isHDR) "10-bit / 12-bit HDR" else "8-bit / 10-bit",
                isHDR = isHDR,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = true,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = if (isAvif) "Next-generation AV1 image with modern compression and wide gamut HDR." else "High-efficiency mobile photo format with preserved EXIF and hardware-accelerated decoding."
            )
        }

        // Camera RAW formats
        val rawFormatMap = mapOf(
            "dng" to Pair("Adobe Digital Negative (DNG)", "DNG"),
            "cr2" to Pair("Canon Raw v2 (CR2)", "CR2"),
            "cr3" to Pair("Canon Raw v3 (CR3)", "CR3"),
            "crw" to Pair("Canon Raw (CRW)", "CRW"),
            "nef" to Pair("Nikon Electronic Format (NEF)", "NEF"),
            "nrw" to Pair("Nikon Raw (NRW)", "NRW"),
            "arw" to Pair("Sony Alpha RAW (ARW)", "ARW"),
            "srf" to Pair("Sony Raw File (SRF)", "SRF"),
            "sr2" to Pair("Sony Raw v2 (SR2)", "SR2"),
            "raf" to Pair("Fujifilm Raw (RAF)", "RAF"),
            "rw2" to Pair("Panasonic / Leica RAW (RW2)", "RW2"),
            "rwl" to Pair("Leica RAW (RWL)", "RWL"),
            "orf" to Pair("Olympus / OM RAW (ORF)", "ORF"),
            "pef" to Pair("Pentax Electronic File (PEF)", "PEF"),
            "ptx" to Pair("Pentax Raw (PTX)", "PTX"),
            "mrw" to Pair("Minolta Raw (MRW)", "MRW"),
            "kdc" to Pair("Kodak Digital Camera RAW (KDC)", "KDC"),
            "dcr" to Pair("Kodak RAW (DCR)", "DCR"),
            "erf" to Pair("Epson Raw Format (ERF)", "ERF"),
            "x3f" to Pair("Sigma Foveon RAW (X3F)", "X3F"),
            "iiq" to Pair("Phase One RAW (IIQ)", "IIQ"),
            "3fr" to Pair("Hasselblad 3F RAW (3FR)", "3FR"),
            "mef" to Pair("Mamiya Electronic File (MEF)", "MEF"),
            "mos" to Pair("Leaf RAW (MOS)", "MOS")
        )

        if (rawFormatMap.containsKey(extension)) {
            val (fullName, badge) = rawFormatMap[extension]!!
            return@withContext SultanFormatInfo(
                formatName = fullName,
                shortBadge = badge,
                category = FormatCategory.CAMERA_RAW,
                extension = extension,
                mimeType = "image/x-adobe-dng",
                container = "TIFF / ISOBMFF RAW Container",
                codec = "Unprocessed Sensor Bayer Matrix",
                compression = "Uncompressed / Lossless JPEG",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "ProPhoto RGB / Native Sensor Gamut",
                bitDepth = "12-bit / 14-bit / 16-bit Sensor RAW",
                isHDR = true,
                isAnimated = false,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Professional camera sensor RAW photo. Full EXIF, camera optics, and sensor data detected. Export to high-res JPG/PNG to edit."
            )
        }

        // TIFF: 49 49 2A 00 (II) or 4D 4D 00 2A (MM)
        if (bytesRead >= 4 && (
                    (headerBytes[0] == 0x49.toByte() && headerBytes[1] == 0x49.toByte() && headerBytes[2] == 0x2A.toByte() && headerBytes[3] == 0x00.toByte()) ||
                            (headerBytes[0] == 0x4D.toByte() && headerBytes[1] == 0x4D.toByte() && headerBytes[2] == 0x00.toByte() && headerBytes[3] == 0x2A.toByte())
                    ) || extension in listOf("tif", "tiff")
        ) {
            return@withContext SultanFormatInfo(
                formatName = "Tagged Image File Format (TIFF)",
                shortBadge = "TIFF",
                category = FormatCategory.RASTER_STANDARD,
                extension = "tiff",
                mimeType = "image/tiff",
                container = "TIFF Directory Structure",
                codec = "RGB / Grayscale / LZW / Deflate",
                compression = "Uncompressed / LZW / ZIP",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit / 16-bit / 32-bit Deep Color",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "High-depth professional TIFF raster graphic. Supports lossless deep color depth and multi-channel data."
            )
        }

        // BMP / DIB: 42 4D (BM)
        if (bytesRead >= 2 && headerBytes[0] == 0x42.toByte() && headerBytes[1] == 0x4D.toByte() ||
            extension in listOf("bmp", "dib")
        ) {
            return@withContext SultanFormatInfo(
                formatName = "Windows Bitmap (BMP/DIB)",
                shortBadge = "BMP",
                category = FormatCategory.RASTER_STANDARD,
                extension = "bmp",
                mimeType = "image/bmp",
                container = "Windows Device Independent Bitmap",
                codec = "Uncompressed RGB Matrix",
                compression = "None / BI_RGB / BI_RLE",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "sRGB",
                bitDepth = "24-bit / 32-bit RGB",
                isHDR = false,
                isAnimated = false,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = true,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Standard Windows Device Independent Bitmap with direct photo editor support."
            )
        }

        // JPEG 2000: JP2 / J2K
        if (extension in listOf("jp2", "j2k", "jpf", "jpx", "jpm") ||
            (bytesRead >= 4 && (headerBytes[0].toInt() and 0xFF == 0xFF) && (headerBytes[1].toInt() and 0xFF == 0x4F))
        ) {
            return@withContext SultanFormatInfo(
                formatName = "JPEG 2000 Wavelet Image (JP2)",
                shortBadge = "JP2",
                category = FormatCategory.NEXT_GEN,
                extension = extension.ifEmpty { "jp2" },
                mimeType = "image/jp2",
                container = "JPEG 2000 Wavelet Container",
                codec = "Discrete Wavelet Transform (DWT)",
                compression = "Lossless & Lossy Wavelet",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit to 16-bit Wavelet",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "JPEG 2000 wavelet-compressed visual format. High quality compression with seamless conversion."
            )
        }

        // JPEG XL: JXL
        if (extension == "jxl" || (bytesRead >= 2 && headerBytes[0].toInt() and 0xFF == 0xFF && headerBytes[1].toInt() and 0xFF == 0x0A)) {
            return@withContext SultanFormatInfo(
                formatName = "JPEG XL (JXL)",
                shortBadge = "JXL",
                category = FormatCategory.NEXT_GEN,
                extension = "jxl",
                mimeType = "image/jxl",
                container = "JPEG XL Container Box",
                codec = "VarDCT / Modular",
                compression = "Next-Gen Ultra Lossy & Lossless",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "Wide Gamut / Display P3 / Rec.2020",
                bitDepth = "Up to 32-bit Float HDR",
                isHDR = true,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = false,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Next-generation JPEG XL image format. Safely detected by SULTAN GALLERY media engine."
            )
        }

        // ICO / CUR
        if (extension in listOf("ico", "cur") || (bytesRead >= 4 && headerBytes[0] == 0x00.toByte() && headerBytes[1] == 0x00.toByte() &&
                    (headerBytes[2] == 0x01.toByte() || headerBytes[2] == 0x02.toByte()) && headerBytes[3] == 0x00.toByte())) {
            val isCur = extension == "cur" || (bytesRead >= 3 && headerBytes[2] == 0x02.toByte())
            return@withContext SultanFormatInfo(
                formatName = if (isCur) "Windows Cursor (CUR)" else "Windows Icon Resource (ICO)",
                shortBadge = if (isCur) "CUR" else "ICO",
                category = FormatCategory.SPECIALIZED,
                extension = if (isCur) "cur" else "ico",
                mimeType = "image/x-icon",
                container = "Icon Directory Directory Header",
                codec = "Multi-Resolution BMP / PNG",
                compression = "None / Lossless PNG",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "sRGB",
                bitDepth = "32-bit RGBA Icon",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Windows multi-resolution icon/cursor file with alpha transparency."
            )
        }

        // TGA / Truevision
        if (extension == "tga") {
            return@withContext SultanFormatInfo(
                formatName = "Truevision Targa Graphic (TGA)",
                shortBadge = "TGA",
                category = FormatCategory.SPECIALIZED,
                extension = "tga",
                mimeType = "image/x-tga",
                container = "Truevision TGA File Structure",
                codec = "Raw / RLE Uncompressed",
                compression = "Uncompressed / RLE",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "sRGB",
                bitDepth = "24-bit / 32-bit RGBA",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Truevision TARGA raster graphics commonly used in game textures and 3D rendering."
            )
        }

        // Design vector / CAD / EPS / AI
        if (extension in listOf("eps", "ai")) {
            return@withContext SultanFormatInfo(
                formatName = if (extension == "ai") "Adobe Illustrator Artwork (AI)" else "Encapsulated PostScript (EPS)",
                shortBadge = if (extension == "ai") "AI" else "EPS",
                category = FormatCategory.VECTOR,
                extension = extension,
                mimeType = if (extension == "ai") "application/illustrator" else "application/postscript",
                container = "PostScript / PDF Container",
                codec = "PostScript Vector Operators",
                compression = "Deflate / Vector Streams",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "CMYK / RGB Vector",
                bitDepth = "Vector Paths",
                isHDR = false,
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Professional vector design document. Convert to high-resolution PNG or JPG for full photo editing."
            )
        }

        // Specialized: QOI, DDS, HDR, EXR, ICNS, PCX, PPM, PGM, PBM, PAM
        val specMap = mapOf(
            "qoi" to Pair("Quite OK Image Format (QOI)", "QOI"),
            "dds" to Pair("DirectDraw Surface (DDS)", "DDS"),
            "hdr" to Pair("Radiance High Dynamic Range (HDR)", "HDR"),
            "exr" to Pair("Industrial Light & Magic OpenEXR (EXR)", "EXR"),
            "icns" to Pair("Apple Icon Image (ICNS)", "ICNS"),
            "pcx" to Pair("Picture eXchange (PCX)", "PCX"),
            "ppm" to Pair("Portable Pixmap (PPM)", "PPM"),
            "pgm" to Pair("Portable Graymap (PGM)", "PGM"),
            "pbm" to Pair("Portable Bitmap (PBM)", "PBM"),
            "pam" to Pair("Portable Arbitrary Map (PAM)", "PAM")
        )
        if (specMap.containsKey(extension)) {
            val (specName, specBadge) = specMap[extension]!!
            return@withContext SultanFormatInfo(
                formatName = specName,
                shortBadge = specBadge,
                category = FormatCategory.SPECIALIZED,
                extension = extension,
                mimeType = "image/$extension",
                container = "$specBadge Container Format",
                codec = "$specBadge Codec",
                compression = "Specialized Compression",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = if (extension in listOf("hdr", "exr")) "Linear Rec.709 / ACES (HDR)" else "sRGB",
                bitDepth = if (extension in listOf("hdr", "exr")) "32-bit Float HDR" else "8-bit to 16-bit",
                isHDR = extension in listOf("hdr", "exr"),
                isAnimated = false,
                hasAlpha = true,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = false,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Specialized visual format detected by SULTAN GALLERY media engine."
            )
        }

        // Standard JPEG: FF D8 FF
        val isJpeg = (bytesRead >= 3 && (headerBytes[0].toInt() and 0xFF == 0xFF) &&
                (headerBytes[1].toInt() and 0xFF == 0xD8) &&
                (headerBytes[2].toInt() and 0xFF == 0xFF)) ||
                extension in listOf("jpg", "jpeg", "jpe", "jfif", "jif", "jfi")

        if (isJpeg) {
            return@withContext SultanFormatInfo(
                formatName = "Joint Photographic Experts Group (JPEG)",
                shortBadge = "JPEG",
                category = FormatCategory.RASTER_STANDARD,
                extension = "jpg",
                mimeType = "image/jpeg",
                container = "JFIF / EXIF File Interchange Format",
                codec = "Discrete Cosine Transform (DCT)",
                compression = "Lossy JPEG Compression",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = detectedColorSpace,
                bitDepth = "8-bit Truecolor (24-bit RGB)",
                isHDR = false,
                isAnimated = false,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = true,
                isViewable = true,
                rawMetadata = rawMeta,
                exifAvailable = exifAvailable,
                magicSignatureHex = magicHex,
                description = "Standard photographic JPEG with full EXIF metadata support and non-destructive photo editing."
            )
        }

        // Video / Audio or generic fallback
        if (item.isVideo) {
            return@withContext SultanFormatInfo(
                formatName = "Digital Video (${extension.uppercase(Locale.ROOT)})",
                shortBadge = extension.uppercase(Locale.ROOT).take(4),
                category = FormatCategory.VIDEO,
                extension = extension.ifEmpty { "mp4" },
                mimeType = item.mimeType,
                container = "MPEG-4 / Matroska Video Container",
                codec = "H.264 / HEVC / VP9 / AV1",
                compression = "Temporal Video Interframe Compression",
                width = effectiveWidth,
                height = effectiveHeight,
                colorSpace = "BT.709 / BT.2020",
                bitDepth = "8-bit / 10-bit Video",
                isHDR = false,
                isAnimated = true,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = uncompressedRam,
                isDirectlyEditable = false,
                isViewable = true,
                magicSignatureHex = magicHex,
                description = "Digital video playback powered by Media3 ExoPlayer with hardware acceleration."
            )
        }

        if (item.isAudio) {
            return@withContext SultanFormatInfo(
                formatName = "Audio Track (${extension.uppercase(Locale.ROOT)})",
                shortBadge = extension.uppercase(Locale.ROOT).take(4),
                category = FormatCategory.AUDIO,
                extension = extension.ifEmpty { "mp3" },
                mimeType = item.mimeType,
                container = "Audio Stream Container",
                codec = "MPEG Layer-3 / AAC / FLAC / Opus",
                compression = "Perceptual Audio Compression",
                width = 0,
                height = 0,
                colorSpace = "N/A",
                bitDepth = "16-bit / 24-bit Hi-Res Audio",
                isHDR = false,
                isAnimated = false,
                hasAlpha = false,
                fileSize = item.size,
                estimatedUncompressedRam = 0L,
                isDirectlyEditable = false,
                isViewable = true,
                magicSignatureHex = magicHex,
                description = "Audio recording with lossless / lossy high-fidelity playback and vinyl visualizer."
            )
        }

        // Unknown / Fallback
        SultanFormatInfo(
            formatName = if (extension.isNotEmpty()) "${extension.uppercase(Locale.ROOT)} Media File" else "Standard Media",
            shortBadge = if (extension.isNotEmpty()) extension.uppercase(Locale.ROOT).take(4) else "FILE",
            category = FormatCategory.UNKNOWN,
            extension = extension.ifEmpty { "dat" },
            mimeType = item.mimeType.ifEmpty { "application/octet-stream" },
            container = "Media File Container",
            codec = "Generic Codec",
            compression = "Standard",
            width = effectiveWidth,
            height = effectiveHeight,
            colorSpace = "Standard",
            bitDepth = "8-bit",
            isHDR = false,
            isAnimated = false,
            hasAlpha = false,
            fileSize = item.size,
            estimatedUncompressedRam = uncompressedRam,
            isDirectlyEditable = false,
            isViewable = true,
            rawMetadata = rawMeta,
            exifAvailable = exifAvailable,
            magicSignatureHex = magicHex,
            description = "Media file safely indexed by SULTAN GALLERY media engine."
        )
    }

    private fun getExtension(displayName: String, path: String): String {
        val name = displayName.ifEmpty { path }
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1)
        } else ""
    }
}
