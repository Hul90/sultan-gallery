package com.example.ui.viewer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.components.MediaDetailsDialog
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    initialMediaId: Long,
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val photos = remember(state.allMedia) {
        state.allMedia.filter { !it.isVideo && !it.isAudio }
    }

    val initialIndex = remember(initialMediaId, photos) {
        val index = photos.indexOfFirst { it.id == initialMediaId }
        if (index >= 0) index else 0
    }

    if (photos.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No photos to view", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
    var showControls by remember { mutableStateOf(true) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ZoomablePhotoItem(
                photo = photo,
                onTap = { showControls = !showControls }
            )
        }

        // Top App Bar Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentPhoto.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${photos.size} • ${currentPhoto.formattedSize}",
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
                    val ext = currentPhoto.displayName.substringAfterLast('.', "").uppercase(java.util.Locale.ROOT)
                    if (ext.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(SultanGold.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ext,
                                color = SultanGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = { showDetailsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.repository.setAsWallpaper(currentPhoto.uri)
                            viewModel.showMessage("Wallpaper updated")
                        }
                    }) {
                        Icon(Icons.Default.Wallpaper, contentDescription = "Set Wallpaper", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                )
            )
        }

        // Bottom Action Bar Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                IconButton(onClick = {
                    viewModel.repository.shareMedia(currentPhoto.uri, currentPhoto.mimeType)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }

                // Edit
                IconButton(onClick = {
                    onNavigateToEditor(currentPhoto.uri)
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SultanGold)
                }

                // Favorite
                IconButton(onClick = {
                    viewModel.toggleFavorite(currentPhoto)
                }) {
                    Icon(
                        imageVector = if (currentPhoto.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentPhoto.isFavorite) SultanGold else Color.White
                    )
                }

                // Move to Vault
                IconButton(onClick = {
                    viewModel.toggleSelection(currentPhoto.id)
                    viewModel.batchVaultSelected()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.Lock, contentDescription = "Move to Vault", tint = Color.White)
                }

                // Delete to Trash
                IconButton(onClick = {
                    scope.launch {
                        viewModel.repository.moveToTrash(currentPhoto)
                        viewModel.showMessage("Moved to Trash")
                        onNavigateBack()
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (showDetailsDialog) {
            MediaDetailsDialog(
                item = currentPhoto,
                onDismiss = { showDetailsDialog = false },
                onSetWallpaper = {
                    scope.launch {
                        viewModel.repository.setAsWallpaper(currentPhoto.uri)
                        viewModel.showMessage("Wallpaper updated")
                    }
                }
            )
        }
    }
}

@Composable
private fun ZoomablePhotoItem(
    photo: MediaItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val isPdf = photo.mimeType == "application/pdf" || photo.displayName.endsWith(".pdf", ignoreCase = true)
    var pdfPageIndex by remember { mutableStateOf(0) }
    var pdfPageCount by remember { mutableStateOf(0) }
    var pdfBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isPdfLoading by remember { mutableStateOf(isPdf) }

    LaunchedEffect(photo.uri, pdfPageIndex) {
        if (isPdf) {
            isPdfLoading = true
            val pageInfo = com.example.tools.SultanDecoderEngine.renderPdfPage(context, photo.uri, pdfPageIndex, targetWidth = 1920)
            pdfPageCount = pageInfo.pageCount
            pdfBitmap = pageInfo.currentPageBitmap
            isPdfLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(scale) {
                if (scale > 1f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offset = Offset(offset.x + pan.x, offset.y + pan.y)
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isPdf) {
            if (isPdfLoading && pdfBitmap == null) {
                CircularProgressIndicator(color = SultanGold)
            } else if (pdfBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                // PDF Page Navigation Bar
                if (pdfPageCount > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                            .background(Color.Black.copy(alpha = 0.75f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (pdfPageIndex > 0) pdfPageIndex-- },
                            enabled = pdfPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (pdfPageIndex > 0) SultanGold else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Page ${pdfPageIndex + 1} of $pdfPageCount",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        IconButton(
                            onClick = { if (pdfPageIndex < pdfPageCount - 1) pdfPageIndex++ },
                            enabled = pdfPageIndex < pdfPageCount - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Next Page",
                                tint = if (pdfPageIndex < pdfPageCount - 1) SultanGold else Color.Gray,
                                modifier = Modifier.size(18.dp).graphicsLayer(rotationZ = 180f)
                            )
                        }
                    }
                }
            } else {
                Text("Unable to render PDF page", color = Color.White)
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}
