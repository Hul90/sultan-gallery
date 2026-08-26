package com.example.ui.navigation

import android.net.Uri

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val GALLERY_HOME = "gallery_home"
    const val PHOTO_VIEWER = "photo_viewer/{mediaId}"
    const val VIDEO_PLAYER = "video_player/{mediaId}"
    const val AUDIO_PLAYER = "audio_player/{mediaId}"
    const val PHOTO_EDITOR = "photo_editor?uri={uri}"
    const val SULTAN_TOOLS = "sultan_tools"
    const val SECRET_VAULT = "secret_vault"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun photoViewer(mediaId: Long): String = "photo_viewer/$mediaId"
    fun videoPlayer(mediaId: Long): String = "video_player/$mediaId"
    fun audioPlayer(mediaId: Long): String = "audio_player/$mediaId"
    fun photoEditor(uri: Uri): String = "photo_editor?uri=${Uri.encode(uri.toString())}"
}
