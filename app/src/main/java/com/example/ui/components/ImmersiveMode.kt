package com.example.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides the status bar and navigation bar while this composable is present on screen
 * (swipe from the edge to reveal them temporarily), and restores the normal system bars
 * when the screen is left. Use this in full-screen viewers like the photo/video player so
 * player controls are never obscured by, or drawn underneath, the system bars.
 */
@Composable
fun ImmersiveMode(enabled: Boolean = true) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity

    DisposableEffect(enabled) {
        val window = activity?.window
        if (window == null) {
            return@DisposableEffect onDispose { }
        }

        val insetsController = WindowCompat.getInsetsController(window, view)
        val originalDecorFitsSystemWindows = true

        if (enabled) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, originalDecorFitsSystemWindows)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
