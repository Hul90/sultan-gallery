package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.SkipPrevious
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.calculateZoom
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.components.ImmersiveMode
import com.example.ui.components.MediaDetailsDialog
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoPlayerScreen(
    mediaId: Long,
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Only videos, never mixed with photos/audio: MediaStore ids are not unique across
    // content types, so looking this id up in the combined list could open the wrong file.
    val videoList = remember(state.allMedia) { state.allMedia.filter { it.isVideo } }

    var currentIndex by remember(mediaId, videoList) {
        val idx = videoList.indexOfFirst { it.id == mediaId }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    val videoItem = videoList.getOrNull(currentIndex)

    ImmersiveMode(enabled = true)

    // Restore the app's normal orientation lock when leaving this screen.
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        onDispose {
            if (original != null) activity.requestedOrientation = original
        }
    }

    if (videoItem == null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Video not found", color = Color.White)
        }
        return
    }

    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(videoItem.durationMs) }
    var showControls by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }

    // Pinch-to-zoom of the video surface
    var videoScale by remember { mutableFloatStateOf(1f) }

    // Volume / brightness gesture overlays
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(showVolumeIndicator) {
        if (showVolumeIndicator) {
            delay(900)
            showVolumeIndicator = false
        }
    }
    LaunchedEffect(showBrightnessIndicator) {
        if (showBrightnessIndicator) {
            delay(900)
            showBrightnessIndicator = false
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Load whichever video is current (handles both the initial load and Next/Previous)
    LaunchedEffect(videoItem.id) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoItem.uri))
        exoPlayer.prepare()
        exoPlayer.play()
        currentPosition = 0L
        totalDuration = videoItem.durationMs
        videoScale = 1f
    }

    fun goToNext() {
        if (currentIndex < videoList.lastIndex) currentIndex++
    }

    fun goToPrevious() {
        if (currentIndex > 0) currentIndex--
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(1L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Position updater ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(1L)
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isControlsLocked, videoList.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val isRightHalf = down.position.x > size.width / 2f
                    var isVerticalDrag = false
                    var isHorizontalDrag = false
                    var isMultiTouch = false
                    var totalDx = 0f
                    var totalDy = 0f
                    var moved = false
                    var event = awaitPointerEvent()

                    do {
                        val pointerCount = event.changes.size

                        if (pointerCount >= 2) {
                            isMultiTouch = true
                            if (!isControlsLocked) {
                                val zoomChange = event.calculateZoom()
                                videoScale = (videoScale * zoomChange).coerceIn(1f, 3f)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (!isMultiTouch) {
                            val change = event.changes[0]
                            val delta = change.positionChange()
                            totalDx += delta.x
                            totalDy += delta.y
                            if (abs(delta.x) > 0.5f || abs(delta.y) > 0.5f) moved = true

                            if (!isVerticalDrag && !isHorizontalDrag && !isControlsLocked) {
                                if (abs(totalDy) > 18f && abs(totalDy) > abs(totalDx)) {
                                    isVerticalDrag = true
                                } else if (abs(totalDx) > 24f && abs(totalDx) > abs(totalDy)) {
                                    isHorizontalDrag = true
                                }
                            }

                            if (isVerticalDrag) {
                                change.consume()
                                if (isRightHalf) {
                                    val sensitivity = maxVolume / 350f
                                    volumeLevel = (volumeLevel - delta.y * sensitivity).coerceIn(0f, maxVolume.toFloat())
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeLevel.toInt(), 0)
                                    showVolumeIndicator = true
                                } else {
                                    val window = activity?.window
                                    if (window != null) {
                                        brightnessLevel = (brightnessLevel - delta.y / 500f).coerceIn(0.02f, 1f)
                                        val params = window.attributes
                                        params.screenBrightness = brightnessLevel
                                        window.attributes = params
                                        showBrightnessIndicator = true
                                    }
                                }
                            } else if (isHorizontalDrag) {
                                change.consume()
                            }
                        }
                        event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })

                    if (!isMultiTouch && !moved) {
                        // Simple tap: toggle controls
                        if (!isControlsLocked) {
                            showControls = !showControls
                        } else {
                            showControls = true
                        }
                    } else if (isHorizontalDrag && !isControlsLocked) {
                        if (totalDx < -120f) {
                            goToNext()
                        } else if (totalDx > 120f) {
                            goToPrevious()
                        }
                    }
                }
            }
    ) {
        // Player Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = videoScale, scaleY = videoScale)
        )

        // Volume indicator overlay
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureIndicator(
                icon = Icons.Default.VolumeUp,
                progress = if (maxVolume > 0) volumeLevel / maxVolume else 0f,
                label = "Volume"
            )
        }

        // Brightness indicator overlay
        AnimatedVisibility(
            visible = showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureIndicator(
                icon = Icons.Default.Brightness6,
                progress = brightnessLevel,
                label = "Brightness"
            )
        }

        // Top App Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = videoItem.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = videoItem.formattedSize,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Lock Toggle
                    IconButton(onClick = { isControlsLocked = !isControlsLocked }) {
                        Icon(
                            if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = if (isControlsLocked) SultanGold else Color.White
                        )
                    }

                    if (!isControlsLocked) {
                        // Rotate / orientation toggle
                        IconButton(onClick = {
                            activity?.let {
                                isLandscape = !isLandscape
                                it.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        }) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                        }

                        // Extract Frame Button
                        IconButton(onClick = {
                            scope.launch {
                                val frameBmp = extractFrameAt(context, videoItem.uri, exoPlayer.currentPosition)
                                if (frameBmp != null) {
                                    viewModel.repository.saveEditedBitmap(frameBmp, "${videoItem.displayName}_FRAME")
                                    viewModel.showMessage("Frame captured to Gallery")
                                } else {
                                    viewModel.showMessage("Failed to extract frame")
                                }
                            }
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Extract Frame", tint = SultanGold)
                        }

                        // Speed Menu
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x", fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            playbackSpeed = speed
                                            exoPlayer.playbackParameters = PlaybackParameters(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showDetailsDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                )
            )
        }

        // Center Playback Skip / Play / Pause / Previous / Next Controls
        AnimatedVisibility(
            visible = showControls && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Previous video
                IconButton(
                    onClick = { goToPrevious() },
                    enabled = currentIndex > 0,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.SkipPrevious,
                        contentDescription = "Previous Video",
                        tint = if (currentIndex > 0) Color.White else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) },
                    modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Play / Pause
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.size(68.dp).background(SultanGold, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Forward 10s
                IconButton(
                    onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)) },
                    modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Next video
                IconButton(
                    onClick = { goToNext() },
                    enabled = currentIndex < videoList.lastIndex,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.SkipNext,
                        contentDescription = "Next Video",
                        tint = if (currentIndex < videoList.lastIndex) Color.White else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Bottom Controls Bar (Slider, time, actions)
        AnimatedVisibility(
            visible = showControls && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Progress Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            currentPosition = it.toLong()
                            exoPlayer.seekTo(it.toLong())
                        },
                        valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = SultanGold,
                            activeTrackColor = SultanGold,
                            inactiveTrackColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(totalDuration),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.repository.shareMedia(videoItem.uri, videoItem.mimeType) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.toggleFavorite(videoItem) }) {
                        Icon(
                            if (videoItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (videoItem.isFavorite) SultanGold else Color.White
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val wasLast = videoList.size <= 1
                            viewModel.repository.moveToTrash(videoItem)
                            viewModel.showMessage("Video moved to Trash")
                            if (wasLast) {
                                onNavigateBack()
                            } else if (currentIndex >= videoList.lastIndex) {
                                goToPrevious()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showDetailsDialog) {
            MediaDetailsDialog(
                item = videoItem,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}

@Composable
private fun GestureIndicator(
    icon: ImageVector,
    progress: Float,
    label: String
) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(SultanGold, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

private suspend fun extractFrameAt(context: android.content.Context, uri: android.net.Uri, positionMs: Long): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val timeUs = positionMs * 1000L
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            retriever.release()
            frame
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
