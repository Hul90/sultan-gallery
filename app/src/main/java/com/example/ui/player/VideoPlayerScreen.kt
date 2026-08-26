package com.example.ui.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.MediaDetailsDialog
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    mediaId: Long,
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val videos = remember(state.allMedia) { state.allMedia.filter { it.isVideo } }
    val initialIndex = remember(mediaId, videos) { videos.indexOfFirst { it.id == mediaId }.coerceAtLeast(0) }
    var videoIndex by remember(mediaId, videos) { mutableIntStateOf(initialIndex) }
    val videoItem = videos.getOrNull(videoIndex)

    if (videoItem == null) {
        Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Video not found", color = Color.White)
        }
        return
    }

    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(videoItem.durationMs.coerceAtLeast(1L)) }
    var showControls by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(readBrightness(context)) }

    val exoPlayer = remember(videoItem.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoItem.uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) totalDuration = exoPlayer.duration.coerceAtLeast(1L)
                if (playbackState == Player.STATE_ENDED && videoIndex < videos.lastIndex) {
                    videoIndex++
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }

    LaunchedEffect(videoIndex) {
        currentPosition = 0L
        zoom = 1f
        rotation = 0f
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.setMediaItem(MediaItem.fromUri(videoItem.uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    LaunchedEffect(isPlaying, videoItem.id) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(1L)
            delay(300)
        }
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        exoPlayer.volume = volume
    }
    fun setBrightness(v: Float) {
        brightness = v.coerceIn(0.05f, 1f)
        setWindowBrightness(context, brightness)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(isControlsLocked) {
                detectTapGestures(
                    onTap = { showControls = if (isControlsLocked) true else !showControls },
                    onDoubleTap = { zoom = if (zoom > 1.1f) 1f else 2f }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, scaleChange, _ ->
                    zoom = (zoom * scaleChange).coerceIn(1f, 4f)
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, rotationZ = rotation)
        )

        AnimatedVisibility(showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            TopAppBar(
                title = {
                    Column {
                        Text(videoItem.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${videoIndex + 1} / ${videos.size} • ${videoItem.formattedSize}", color = Color.LightGray, fontSize = 12.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                actions = {
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) { Icon(Icons.Default.ScreenRotation, "Rotate", tint = Color.White) }
                    IconButton(onClick = { zoom = (zoom + .25f).coerceAtMost(4f) }) { Icon(Icons.Default.ZoomIn, "Zoom in", tint = Color.White) }
                    IconButton(onClick = { zoom = (zoom - .25f).coerceAtLeast(1f) }) { Icon(Icons.Default.ZoomOut, "Zoom out", tint = Color.White) }
                    IconButton(onClick = { isControlsLocked = !isControlsLocked }) {
                        Icon(if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock", tint = if (isControlsLocked) SultanGold else Color.White)
                    }
                    if (!isControlsLocked) {
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) { Icon(Icons.Default.Speed, "Speed", tint = Color.White) }
                            DropdownMenu(showSpeedMenu, { showSpeedMenu = false }) {
                                listOf(.5f,.75f,1f,1.25f,1.5f,2f).forEach { speed ->
                                    DropdownMenuItem(text={Text("${speed}x")}, onClick={
                                        playbackSpeed=speed; exoPlayer.playbackParameters=PlaybackParameters(speed); showSpeedMenu=false
                                    })
                                }
                            }
                        }
                        IconButton(onClick = { showDetailsDialog = true }) { Icon(Icons.Default.Info, "Details", tint = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha=.7f))
            )
        }

        AnimatedVisibility(showControls && !isControlsLocked, enter=fadeIn(), exit=fadeOut(), modifier=Modifier.align(Alignment.Center)) {
            Row(horizontalArrangement=Arrangement.spacedBy(18.dp), verticalAlignment=Alignment.CenterVertically) {
                IconButton(
                    onClick={ if (videoIndex>0) videoIndex-- },
                    enabled=videoIndex>0,
                    modifier=Modifier.size(52.dp).background(Color.Black.copy(.6f),CircleShape)
                ){ Icon(Icons.Default.SkipPrevious,"Previous video",tint=if(videoIndex>0)Color.White else Color.Gray,modifier=Modifier.size(30.dp)) }
                IconButton(
                    onClick={ if(isPlaying)exoPlayer.pause() else exoPlayer.play() },
                    modifier=Modifier.size(68.dp).background(SultanGold,CircleShape)
                ){ Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,"Play/Pause",tint=Color.Black,modifier=Modifier.size(40.dp)) }
                IconButton(
                    onClick={ if(videoIndex<videos.lastIndex) videoIndex++ },
                    enabled=videoIndex<videos.lastIndex,
                    modifier=Modifier.size(52.dp).background(Color.Black.copy(.6f),CircleShape)
                ){ Icon(Icons.Default.SkipNext,"Next video",tint=if(videoIndex<videos.lastIndex)Color.White else Color.Gray,modifier=Modifier.size(30.dp)) }
            }
        }

        AnimatedVisibility(showControls && !isControlsLocked, enter=fadeIn(), exit=fadeOut(), modifier=Modifier.align(Alignment.BottomCenter)) {
            Column(
                Modifier.fillMaxWidth().background(Color.Black.copy(.8f)).padding(horizontal=12.dp,vertical=6.dp)
            ) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(formatTime(currentPosition),Color.White,fontSize=12.sp)
                    Slider(
                        value=currentPosition.toFloat().coerceIn(0f,totalDuration.toFloat()),
                        onValueChange={currentPosition=it.toLong();exoPlayer.seekTo(it.toLong())},
                        valueRange=0f..totalDuration.toFloat().coerceAtLeast(1f),
                        modifier=Modifier.weight(1f).padding(horizontal=6.dp)
                    )
                    Text(formatTime(totalDuration),Color.LightGray,fontSize=12.sp)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    IconButton(onClick={scope.launch{viewModel.repository.shareMedia(videoItem.uri,videoItem.mimeType)}}){Icon(Icons.Default.Share,"Share",tint=Color.White)}
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(100.dp)){
                        Text("Volume ${((volume*100).roundToInt())}%",color=Color.White,fontSize=10.sp)
                        Slider(volume,{setVolume(it)})
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(100.dp)){
                        Text("Brightness ${((brightness*100).roundToInt())}%",color=Color.White,fontSize=10.sp)
                        Slider(brightness,{setBrightness(it)})
                    }
                    IconButton(onClick={
                        scope.launch{viewModel.repository.moveToTrash(videoItem);viewModel.showMessage("Video moved to Trash");onNavigateBack()}
                    }){Icon(Icons.Default.Delete,"Delete",tint=MaterialTheme.colorScheme.error)}
                }
            }
        }

        if(showDetailsDialog) MediaDetailsDialog(item=videoItem,onDismiss={showDetailsDialog=false})
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun readBrightness(context: Context): Float {
    val window = (context as? android.app.Activity)?.window ?: return 1f
    return if (window.attributes.screenBrightness > 0f) window.attributes.screenBrightness else 1f
}

private fun setWindowBrightness(context: Context, value: Float) {
    (context as? android.app.Activity)?.window?.let { window ->
        val params = window.attributes
        params.screenBrightness = value.coerceIn(.05f,1f)
        window.attributes = params
    }
}

private suspend fun extractFrameAt(context: Context, uri: android.net.Uri, positionMs: Long): android.graphics.Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
            }
        }.getOrNull()
    }
