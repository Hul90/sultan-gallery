package com.example.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val path: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bucketName: String = "",
    val bucketId: String = "",
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val isVaulted: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }
}

data class MediaAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val photoCount: Int,
    val videoCount: Int,
    val lastModified: Long,
    val relativePath: String = ""
)
