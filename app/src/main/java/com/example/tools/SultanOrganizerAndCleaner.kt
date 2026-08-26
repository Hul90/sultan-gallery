package com.example.tools

import com.example.data.model.MediaItem

object SultanScreenshotCleaner {

    fun filterScreenshots(mediaList: List<MediaItem>): List<MediaItem> {
        return mediaList.filter { item ->
            val name = item.displayName.lowercase()
            val path = item.path.lowercase()
            val bucket = item.bucketName.lowercase()
            name.contains("screenshot") ||
            name.contains("screen_") ||
            path.contains("screenshots") ||
            bucket.contains("screenshot")
        }.sortedByDescending { it.dateAdded }
    }
}

object SultanMediaOrganizer {

    data class CategoryStat(
        val title: String,
        val count: Int,
        val totalSizeBytes: Long,
        val items: List<MediaItem>
    ) {
        val formattedSize: String
            get() {
                val mb = totalSizeBytes / (1024.0 * 1024.0)
                val gb = mb / 1024.0
                return when {
                    gb >= 1.0 -> String.format("%.2f GB", gb)
                    mb >= 1.0 -> String.format("%.1f MB", mb)
                    else -> String.format("%.0f KB", totalSizeBytes / 1024.0)
                }
            }
    }

    fun organizeMedia(mediaList: List<MediaItem>): List<CategoryStat> {
        val cameraItems = mutableListOf<MediaItem>()
        val screenshotItems = mutableListOf<MediaItem>()
        val downloadItems = mutableListOf<MediaItem>()
        val whatsappItems = mutableListOf<MediaItem>()
        val telegramItems = mutableListOf<MediaItem>()
        val videoItems = mutableListOf<MediaItem>()
        val audioItems = mutableListOf<MediaItem>()
        val otherItems = mutableListOf<MediaItem>()

        for (item in mediaList) {
            val name = item.displayName.lowercase()
            val path = item.path.lowercase()
            val bucket = item.bucketName.lowercase()

            when {
                item.isAudio -> audioItems.add(item)
                item.isVideo -> videoItems.add(item)
                name.contains("screenshot") || bucket.contains("screenshot") || path.contains("screenshots") -> screenshotItems.add(item)
                bucket.contains("whatsapp") || path.contains("whatsapp") -> whatsappItems.add(item)
                bucket.contains("telegram") || path.contains("telegram") -> telegramItems.add(item)
                bucket.contains("download") || path.contains("download") -> downloadItems.add(item)
                bucket.contains("camera") || bucket.contains("dcim") || path.contains("dcim") -> cameraItems.add(item)
                else -> otherItems.add(item)
            }
        }

        return listOf(
            CategoryStat("Camera & DCIM", cameraItems.size, cameraItems.sumOf { it.size }, cameraItems),
            CategoryStat("Videos", videoItems.size, videoItems.sumOf { it.size }, videoItems),
            CategoryStat("Screenshots", screenshotItems.size, screenshotItems.sumOf { it.size }, screenshotItems),
            CategoryStat("Downloads", downloadItems.size, downloadItems.sumOf { it.size }, downloadItems),
            CategoryStat("WhatsApp Media", whatsappItems.size, whatsappItems.sumOf { it.size }, whatsappItems),
            CategoryStat("Telegram Media", telegramItems.size, telegramItems.sumOf { it.size }, telegramItems),
            CategoryStat("Audio & Music", audioItems.size, audioItems.sumOf { it.size }, audioItems),
            CategoryStat("Other Media", otherItems.size, otherItems.sumOf { it.size }, otherItems)
        ).filter { it.count > 0 }
    }
}
