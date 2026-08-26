package com.example.data.repository

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.FavoriteEntity
import com.example.data.local.SultanDao
import com.example.data.local.TrashEntity
import com.example.data.local.VaultEntity
import com.example.data.model.MediaAlbum
import com.example.data.model.MediaItem
import com.example.data.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MediaStoreRepository(
    private val context: Context,
    private val dao: SultanDao
) {
    val favoriteUris: Flow<List<String>> = dao.getAllFavoriteUris()
    val trashEntities: Flow<List<TrashEntity>> = dao.getAllTrash()
    val vaultEntities: Flow<List<VaultEntity>> = dao.getAllVaultItems()

    suspend fun loadAllMedia(includeAudio: Boolean = true): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val favList = dao.getAllFavoriteUris().firstOrNull() ?: emptyList()
        val favSet = favList.toSet()
        val trashList = dao.getAllTrash().firstOrNull() ?: emptyList()
        val trashSet = trashList.map { it.uriString }.toSet()

        // 1. Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        try {
            context.contentResolver.query(
                imageUri,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(imageUri, id)
                val uriStr = contentUri.toString()
                if (trashSet.contains(uriStr)) continue

                val name = cursor.getString(nameCol) ?: "Image_$id"
                val path = cursor.getString(pathCol) ?: ""
                val dateAdded = cursor.getLong(dateAddCol) * 1000L
                val dateModified = cursor.getLong(dateModCol) * 1000L
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Pictures" else "Pictures"
                val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        mimeType = mimeType,
                        isVideo = false,
                        isAudio = false,
                        width = width,
                        height = height,
                        bucketName = bucketName,
                        bucketId = bucketId,
                        isFavorite = favSet.contains(uriStr)
                    )
                )
            }
            }
        } catch (_: SecurityException) {
            // Image permission is not currently granted; continue with other media types.
        }

        // 2. Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )

        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        try {
            context.contentResolver.query(
                videoUri,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(videoUri, id)
                val uriStr = contentUri.toString()
                if (trashSet.contains(uriStr)) continue

                val name = cursor.getString(nameCol) ?: "Video_$id"
                val path = cursor.getString(pathCol) ?: ""
                val dateAdded = cursor.getLong(dateAddCol) * 1000L
                val dateModified = cursor.getLong(dateModCol) * 1000L
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Videos" else "Videos"
                val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        path = path,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        mimeType = mimeType,
                        isVideo = true,
                        isAudio = false,
                        durationMs = duration,
                        width = width,
                        height = height,
                        bucketName = bucketName,
                        bucketId = bucketId,
                        isFavorite = favSet.contains(uriStr)
                    )
                )
            }
            }
        } catch (_: SecurityException) {
            // Video permission is not currently granted; continue with audio/documents.
        }

        // 2b. Robust MediaStore.Files visual fallback.
        // Some OEM MediaStore implementations expose visual files more reliably
        // through the generic files collection than through Images/Video. This
        // fallback also covers unusual MIME registrations without duplicating
        // already-loaded media.
        if (mediaList.none { !it.isAudio }) {
            try {
                val filesUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE
                )
                val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
                val args = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )
                context.contentResolver.query(
                    filesUri, projection, selection, args,
                    "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val existingUris = mediaList.mapTo(mutableSetOf()) { it.uri.toString() }
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(filesUri, id)
                        val uriStr = uri.toString()
                        if (uriStr in existingUris || trashSet.contains(uriStr)) continue
                        val name = cursor.getString(nameCol) ?: "Media_$id"
                        val mime = cursor.getString(mimeCol).orEmpty().lowercase()
                        val isVideo = mime.startsWith("video/")
                        val isImage = mime.startsWith("image/")
                        if (!isImage && !isVideo) continue
                        val path = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                        val bucket = path.substringAfterLast('/', "Media").ifBlank { if (isVideo) "Videos" else "Pictures" }
                        mediaList.add(
                            MediaItem(
                                id = id,
                                uri = uri,
                                displayName = name,
                                path = path,
                                dateAdded = cursor.getLong(dateAddCol) * 1000L,
                                dateModified = cursor.getLong(dateModCol) * 1000L,
                                size = cursor.getLong(sizeCol),
                                mimeType = mime,
                                isVideo = isVideo,
                                isAudio = false,
                                bucketName = bucket,
                                bucketId = bucket.lowercase(),
                                isFavorite = favSet.contains(uriStr)
                            )
                        )
                        existingUris.add(uriStr)
                    }
                }
            } catch (_: SecurityException) {
                // The current Android permission scope may intentionally limit access.
            } catch (_: Exception) {
                // OEM-specific MediaStore differences must never crash the gallery.
            }
        }

        // 3. Audio (if enabled)
        if (includeAudio) {
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DURATION
            )
            val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            try {
                context.contentResolver.query(
                    audioUri,
                    audioProjection,
                    null,
                    null,
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(audioUri, id)
                    val uriStr = contentUri.toString()
                    if (trashSet.contains(uriStr)) continue

                    val name = cursor.getString(nameCol) ?: "Audio_$id"
                    val path = cursor.getString(pathCol) ?: ""
                    val dateAdded = cursor.getLong(dateAddCol) * 1000L
                    val dateModified = cursor.getLong(dateModCol) * 1000L
                    val size = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            path = path,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            size = size,
                            mimeType = mimeType,
                            isVideo = false,
                            isAudio = true,
                            durationMs = duration,
                            bucketName = "Audio",
                            bucketId = "audio",
                            isFavorite = favSet.contains(uriStr)
                        )
                    )
                }
                }
            } catch (_: SecurityException) {
                // Audio permission is optional; continue without audio.
            }
        }

        // 4. Visual Documents & Specialized Formats (PDF, SVG, PSD, RAW)
        try {
            val visualExtensions = listOf(
                "pdf", "svg", "psd", "psb", "ai", "eps",
                "dng", "cr2", "cr3", "crw", "nef", "nrw", "arw", "srf", "sr2",
                "raf", "rw2", "rwl", "orf", "pef", "ptx", "mrw", "kdc", "dcr", "erf",
                "x3f", "iiq", "3fr", "mef", "mos",
                "tif", "tiff", "tga", "ico", "cur", "avif", "heic", "heif",
                "jp2", "j2k", "jpf", "jpx", "jpm", "jxl", "apng", "bmp", "dib",
                "dds", "hdr", "exr", "qoi", "pcx", "ppm", "pgm", "pbm", "pam",
                "xbm", "xpm", "sgi", "rgb", "rgba", "icns"
            )
            val existingPaths = mediaList.map { it.path }.toSet()
            val existingIds = mediaList.map { it.id }.toSet()

            val fileProjection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            val fileUri = MediaStore.Files.getContentUri("external")
            val extensionSelection = visualExtensions.joinToString(" OR ") {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            }
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR $extensionSelection"
            val selectionArgs = arrayOf("application/pdf") + visualExtensions.map { "%.$it" }.toTypedArray()

            context.contentResolver.query(
                fileUri,
                fileProjection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(fileUri, id)
                    val uriStr = contentUri.toString()
                    if (trashSet.contains(uriStr)) continue

                    val name = cursor.getString(nameCol) ?: "Document_$id"
                    val path = cursor.getString(pathCol) ?: ""
                    if (existingPaths.contains(path) || existingIds.contains(id)) continue

                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in visualExtensions && !name.endsWith(".pdf", ignoreCase = true)) continue

                    val dateAdded = cursor.getLong(dateAddCol) * 1000L
                    val dateModified = cursor.getLong(dateModCol) * 1000L
                    val size = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeCol) ?: if (ext == "pdf") "application/pdf" else "image/$ext"

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            path = path,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            size = size,
                            mimeType = mimeType,
                            isVideo = false,
                            isAudio = false,
                            width = 0,
                            height = 0,
                            bucketName = "Documents",
                            bucketId = "documents",
                            isFavorite = favSet.contains(uriStr)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Optional files scan fallback
        }

        mediaList.sortByDescending { it.dateAdded }
        mediaList
    }

    suspend fun toggleFavorite(mediaItem: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val uriStr = mediaItem.uri.toString()
        val isFav = dao.isFavorite(uriStr)
        if (isFav) {
            dao.removeFavorite(uriStr)
            false
        } else {
            dao.addFavorite(FavoriteEntity(uriStr))
            true
        }
    }

    suspend fun moveToTrash(item: MediaItem) = withContext(Dispatchers.IO) {
        dao.insertTrash(
            TrashEntity(
                uriString = item.uri.toString(),
                originalPath = item.path,
                displayName = item.displayName,
                mimeType = item.mimeType,
                size = item.size,
                deletedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFromTrash(trashEntity: TrashEntity) = withContext(Dispatchers.IO) {
        dao.deleteTrashItem(trashEntity.uriString)
    }

    suspend fun permanentlyDeleteTrash(trashEntity: TrashEntity) = withContext(Dispatchers.IO) {
        dao.deleteTrashItem(trashEntity.uriString)
        try {
            val uri = Uri.parse(trashEntity.uriString)
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
            // Scoped storage security exception fallback
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        dao.clearTrash()
    }

    suspend fun moveToVault(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "sultan_vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val targetFile = File(vaultDir, "vault_${System.currentTimeMillis()}_${item.id}.enc")
            val encrypted = context.contentResolver.openInputStream(item.uri)?.use { input ->
                com.example.data.vault.SultanVaultCryptoEngine.encryptStreamToFile(input, targetFile)
            } ?: false

            if (!encrypted) return@withContext false

            dao.insertVaultItem(
                VaultEntity(
                    displayName = item.displayName,
                    originalUri = item.uri.toString(),
                    encryptedPath = targetFile.absolutePath,
                    mimeType = item.mimeType,
                    size = item.size,
                    isVideo = item.isVideo
                )
            )
            // Mark as trashed / hide from public gallery list
            moveToTrash(item)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromVault(vaultEntity: VaultEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(vaultEntity.encryptedPath)
            if (file.exists()) {
                if (vaultEntity.isVideo) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "Restored_${vaultEntity.displayName}")
                        put(MediaStore.Video.Media.MIME_TYPE, vaultEntity.mimeType.ifBlank { "video/mp4" })
                        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SultanGallery")
                        }
                    }
                    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            com.example.data.vault.SultanVaultCryptoEngine.decryptFileToStream(file, output)
                        }
                    }
                } else {
                    val bitmap = com.example.data.vault.SultanVaultCryptoEngine.decryptFileToBitmap(file)
                    if (bitmap != null) {
                        saveEditedBitmap(bitmap, "Restored_${vaultEntity.displayName}", Bitmap.CompressFormat.JPEG, 95)
                    }
                }
                file.delete()
            }
            dao.deleteVaultItem(vaultEntity.id)
            dao.deleteTrashItem(vaultEntity.originalUri)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveEditedBitmap(
        bitmap: Bitmap,
        baseName: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Uri? = withContext(Dispatchers.IO) {
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
            else -> "jpg"
        }
        val mimeType = when (format) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
            else -> "image/jpeg"
        }
        val cleanName = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = if (cleanName.endsWith(".$extension", ignoreCase = true)) cleanName else "${cleanName}_EDITED.$extension"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SultanGallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(format, quality, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                return@withContext uri
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    suspend fun saveDocumentBytes(
        bytes: ByteArray,
        displayName: String,
        mimeType: String
    ): Uri? = withContext(Dispatchers.IO) {
        val cleanName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, cleanName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
            put(MediaStore.Files.FileColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/SultanGallery")
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                return@withContext uri
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    suspend fun setAsWallpaper(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.setBitmap(bitmap)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    fun shareMedia(uri: Uri, mimeType: String, title: String = "Share Media") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareMultipleMedia(uris: List<Uri>, title: String = "Share Files") {
        if (uris.isEmpty()) return
        val arrayList = ArrayList<Uri>(uris)
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun extractAlbums(mediaList: List<MediaItem>): List<MediaAlbum> {
        val albumMap = mutableMapOf<String, MutableList<MediaItem>>()
        for (item in mediaList) {
            val bucket = if (item.isAudio) "Audio" else if (item.bucketName.isNotBlank()) item.bucketName else "Other"
            albumMap.getOrPut(bucket) { mutableListOf() }.add(item)
        }

        return albumMap.map { (name, items) ->
            val photos = items.count { !it.isVideo && !it.isAudio }
            val videos = items.count { it.isVideo }
            val latest = items.maxOfOrNull { it.dateAdded } ?: 0L
            val coverUri = items.firstOrNull { !it.isAudio }?.uri ?: items.firstOrNull()?.uri
            MediaAlbum(
                id = name,
                name = name,
                coverUri = coverUri,
                itemCount = items.size,
                photoCount = photos,
                videoCount = videos,
                lastModified = latest,
                relativePath = items.firstOrNull()?.path ?: ""
            )
        }.sortedByDescending { it.itemCount }
    }
}
