package com.example.ui.viewer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.components.ImmersiveMode
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

    ImmersiveMode(enabled = true)

    // Guard navigation so a fast tap on the toolbar/back gesture cannot issue
    // multiple popBackStack calls while the viewer is being disposed.
    var isLeavingViewer by remember { mutableStateOf(false) }
    val leaveViewer = {
        if (!isLeavingViewer) {
            isLeavingViewer = true
            onNavigateBack()
        }
    }
    BackHandler(enabled = !isLeavingViewer) { leaveViewer() }

    DisposableEffect(Unit) {
        onDispose { isLeavingViewer = true }
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
    var showControls by remember { mutableStateOf(true) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAlbumDialog by remember { mutableStateOf(false) }
    var albumDialogCopy by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showPdfConfirm by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ZoomablePhotoItem(
                photo = photo,
                // Viewer actions stay visible just like a dedicated gallery
                // viewer: tapping the image never hides the action bars.
                onTap = { showControls = true },
                onZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) isCurrentPageZoomed = zoomed
                }
            )
        }

        // Top App Bar Controls
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
                    IconButton(onClick = leaveViewer) {
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
            modifier = Modifier.align(Alignment.BottomCenter)
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

                // Unique action: Rotate & Save
                IconButton(onClick = {
                    scope.launch {
                        val ok = viewModel.repository.rotateAndSave(currentPhoto)
                        viewModel.showMessage(if (ok) "Rotated and saved" else "Could not rotate image")
                        if (ok) viewModel.refreshMedia()
                    }
                }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Rotate and save", tint = SultanGold)
                }

                // Delete to Trash
                IconButton(onClick = {
                    if (!isLeavingViewer) {
                        scope.launch {
                            viewModel.repository.moveToTrash(currentPhoto)
                            viewModel.refreshMedia()
                            viewModel.showMessage("Moved to Trash")
                            leaveViewer()
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }

                // More actions
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Text("⋮", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move to album") },
                            leadingIcon = { Icon(Icons.Default.FolderCopy, contentDescription = null) },
                            onClick = { showMoreMenu = false; albumDialogCopy = false; showAlbumDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy to album") },
                            leadingIcon = { Icon(Icons.Default.FolderCopy, contentDescription = null) },
                            onClick = { showMoreMenu = false; albumDialogCopy = true; showAlbumDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                renameText = currentPhoto.displayName.substringBeforeLast('.')
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Convert to PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = { showMoreMenu = false; showPdfConfirm = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Details") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = { showMoreMenu = false; showDetailsDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Set as Wallpaper") },
                            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                scope.launch {
                                    val ok = viewModel.repository.setAsWallpaper(currentPhoto.uri)
                                    viewModel.showMessage(if (ok) "Wallpaper updated" else "Could not set wallpaper")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        label = { Text("File name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val base = renameText.trim()
                        if (base.isNotEmpty()) {
                            showRenameDialog = false
                            scope.launch {
                                val ok = viewModel.repository.renameMedia(currentPhoto, base)
                                viewModel.showMessage(if (ok) "Renamed successfully" else "Rename failed")
                                viewModel.refreshMedia()
                            }
                        }
                    }) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
            )
        }

        if (showAlbumDialog) {
            AlbumPickerDialog(
                title = if (albumDialogCopy) "Copy to album" else "Move to album",
                albums = state.albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                currentAlbum = currentPhoto.bucketName,
                onDismiss = { showAlbumDialog = false },
                onConfirm = { album ->
                    showAlbumDialog = false
                    scope.launch {
                        val target = album.relativePath.ifBlank { album.name }
                        val ok = if (albumDialogCopy) viewModel.repository.copyToAlbum(currentPhoto, target) else viewModel.repository.moveToAlbum(currentPhoto, target)
                        viewModel.showMessage(if (ok) (if (albumDialogCopy) "Copied to $target" else "Moved to $target") else "Album operation failed")
                        viewModel.refreshMedia()
                    }
                }
            )
        }

        if (showPdfConfirm) {
            AlertDialog(
                onDismissRequest = { showPdfConfirm = false },
                title = { Text("Convert to PDF") },
                text = { Text("Create a PDF from this picture and save it to Sultan Gallery?") },
                confirmButton = {
                    TextButton(onClick = {
                        showPdfConfirm = false
                        scope.launch {
                            val result = viewModel.repository.convertImageToPdf(currentPhoto)
                            viewModel.showMessage(if (result != null) "PDF saved" else "PDF conversion failed")
                        }
                    }) { Text("Convert") }
                },
                dismissButton = { TextButton(onClick = { showPdfConfirm = false }) { Text("Cancel") } }
            )
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
private fun AlbumPickerDialog(
    title: String,
    albums: List<com.example.data.model.MediaAlbum>,
    currentAlbum: String,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.model.MediaAlbum) -> Unit
) {
    var newAlbumMode by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (newAlbumMode) {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    singleLine = true,
                    label = { Text("New album name") },
                    placeholder = { Text("e.g. Family, Travel") }
                )
            } else {
                Column(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (albums.isEmpty()) {
                        Text("No albums found. Create a new album below.")
                    } else {
                        LazyColumn {
                            items(albums, key = { it.id }) { album ->
                                TextButton(
                                    onClick = { onConfirm(album) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (album.name.equals(currentAlbum, true)) "${album.name} (current)" else album.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (newAlbumMode) {
                TextButton(onClick = {
                    val name = newAlbumName.trim()
                    if (name.isNotEmpty()) onConfirm(com.example.data.model.MediaAlbum(name, name, null, 0, 0, 0, System.currentTimeMillis(), ""))
                }) { Text("Create & Use") }
            } else {
                TextButton(onClick = { newAlbumMode = true }) { Text("+ New album") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ZoomablePhotoItem(
    photo: MediaItem,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

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
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var event = awaitPointerEvent()
                    do {
                        val pointerCount = event.changes.size
                        if (pointerCount >= 2) {
                            // Real pinch: always handle zoom + pan, consume so the
                            // pager never sees it.
                            val zoomChange = event.calculateZoom()
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            val panChange = event.calculatePan()
                            offset = if (scale > 1f) {
                                Offset(offset.x + panChange.x, offset.y + panChange.y)
                            } else {
                                Offset.Zero
                            }
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            // Already zoomed in: a single finger pans the image
                            // instead of swiping to the next/previous photo.
                            val change = event.changes[0]
                            val delta = change.positionChange()
                            offset = Offset(offset.x + delta.x, offset.y + delta.y)
                            change.consume()
                        }
                        // else: single finger at scale=1 — don't consume, let the
                        // HorizontalPager handle the swipe to the next/previous photo.
                        event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
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
