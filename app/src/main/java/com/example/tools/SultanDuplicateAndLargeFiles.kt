package com.example.tools

import android.content.Context
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object SultanDuplicateFinder {

    data class DuplicateGroup(
        val key: String,
        val totalSize: Long,
        val items: List<MediaItem>
    )

    /**
     * Fast initial grouping by exact byte length and dimensions.
     */
    fun findDuplicates(mediaList: List<MediaItem>): List<DuplicateGroup> {
        val groups = mutableMapOf<String, MutableList<MediaItem>>()

        for (item in mediaList) {
            if (item.size <= 0) continue
            // Primary key: exact byte size and aspect/dimensions
            val key = "${item.size}_${item.width}x${item.height}"
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.filter { it.value.size > 1 }
            .map { (key, items) ->
                DuplicateGroup(
                    key = key,
                    totalSize = items.sumOf { it.size },
                    items = items
                )
            }.sortedByDescending { it.totalSize }
    }

    /**
     * Exact content validation via SHA-256 byte stream hashing.
     */
    suspend fun computeMediaChecksum(context: Context, item: MediaItem): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            } ?: return@withContext ""
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

object SultanLargeFileFinder {

    enum class SizeThreshold(val label: String, val minBytes: Long) {
        MB_10("> 10 MB", 10L * 1024L * 1024L),
        MB_50("> 50 MB", 50L * 1024L * 1024L),
        MB_100("> 100 MB", 100L * 1024L * 1024L),
        MB_500("> 500 MB", 500L * 1024L * 1024L),
        ALL("All Large Files", 5L * 1024L * 1024L)
    }

    fun findLargeFiles(
        mediaList: List<MediaItem>,
        threshold: SizeThreshold,
        onlyVideos: Boolean = false
    ): List<MediaItem> {
        return mediaList.filter { item ->
            val matchesSize = item.size >= threshold.minBytes
            val matchesType = if (onlyVideos) item.isVideo else true
            matchesSize && matchesType
        }.sortedByDescending { it.size }
    }
}
