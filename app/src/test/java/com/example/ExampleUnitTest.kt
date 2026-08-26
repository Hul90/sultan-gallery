package com.example

import android.net.Uri
import com.example.data.model.MediaItem
import com.example.tools.SultanDuplicateFinder
import com.example.tools.SultanLargeFileFinder
import com.example.tools.SultanMediaOrganizer
import com.example.tools.SultanScreenshotCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private fun createDummyItem(
        id: Long,
        name: String,
        path: String,
        bucket: String = "DCIM",
        size: Long = 1024 * 1024,
        isVideo: Boolean = false,
        isAudio: Boolean = false
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.EMPTY,
            displayName = name,
            path = path,
            size = size,
            mimeType = if (isVideo) "video/mp4" else if (isAudio) "audio/mp3" else "image/jpeg",
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            bucketName = bucket,
            isVideo = isVideo,
            isAudio = isAudio
        )
    }

    @Test
    fun testDuplicateFinder() {
        val item1 = createDummyItem(1L, "IMG_001.jpg", "/sdcard/DCIM/IMG_001.jpg", size = 5000)
        val item2 = createDummyItem(2L, "IMG_001_copy.jpg", "/sdcard/Downloads/IMG_001_copy.jpg", size = 5000)
        val item3 = createDummyItem(3L, "IMG_002.jpg", "/sdcard/DCIM/IMG_002.jpg", size = 9999)

        val duplicates = SultanDuplicateFinder.findDuplicates(listOf(item1, item2, item3))
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates[0].items.size)
    }

    @Test
    fun testLargeFileFinder() {
        val smallItem = createDummyItem(1L, "small.jpg", "/sdcard/small.jpg", size = 2 * 1024 * 1024)
        val largeItem = createDummyItem(2L, "large.mp4", "/sdcard/large.mp4", size = 60 * 1024 * 1024, isVideo = true)

        val filtered = SultanLargeFileFinder.findLargeFiles(
            listOf(smallItem, largeItem),
            threshold = SultanLargeFileFinder.SizeThreshold.MB_50
        )
        assertEquals(1, filtered.size)
        assertEquals(largeItem.id, filtered[0].id)
    }

    @Test
    fun testScreenshotCleaner() {
        val normalItem = createDummyItem(1L, "photo.jpg", "/sdcard/DCIM/Camera/photo.jpg", bucket = "Camera")
        val screenshotItem = createDummyItem(2L, "Screenshot_2026.png", "/sdcard/Pictures/Screenshots/Screenshot_2026.png", bucket = "Screenshots")

        val screenshots = SultanScreenshotCleaner.filterScreenshots(listOf(normalItem, screenshotItem))
        assertEquals(1, screenshots.size)
        assertEquals("Screenshot_2026.png", screenshots[0].displayName)
    }

    @Test
    fun testMediaOrganizer() {
        val cam = createDummyItem(1L, "cam.jpg", "/sdcard/DCIM/Camera/cam.jpg", bucket = "Camera")
        val vid = createDummyItem(2L, "vid.mp4", "/sdcard/Movies/vid.mp4", bucket = "Movies", isVideo = true)
        val aud = createDummyItem(3L, "song.mp3", "/sdcard/Music/song.mp3", bucket = "Music", isAudio = true)

        val stats = SultanMediaOrganizer.organizeMedia(listOf(cam, vid, aud))
        assertTrue(stats.any { it.title == "Camera & DCIM" })
        assertTrue(stats.any { it.title == "Videos" })
        assertTrue(stats.any { it.title == "Audio & Music" })
    }
}
