package com.example.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.data.model.GridMode
import com.example.data.model.MediaAlbum
import com.example.data.model.MediaItem
import com.example.data.model.MediaTab
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.SultanTopBar
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.launch


private fun hasMediaReadAccess(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val image = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        val selected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        image || video || audio || selected
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun hasFullVisualAccess(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return hasMediaReadAccess(context)
    val image = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    return image || video
}

private fun hasLimitedVisualAccess(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !hasFullVisualAccess(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
}

private fun missingMediaPermissions(context: android.content.Context): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_MEDIA_VIDEO)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_MEDIA_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fullImage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val fullVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                val selected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                if (!fullImage && !fullVideo && !selected) add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }.toTypedArray()
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel,
    onNavigateToViewer: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToAudio: (Long) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var moveCopyMode by remember { mutableStateOf<String?>(null) }
    var pendingMoveCopyRetry by remember { mutableStateOf<(() -> Unit)?>(null) }
    val moveCopyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val retry = pendingMoveCopyRetry
        pendingMoveCopyRetry = null
        if (result.resultCode == android.app.Activity.RESULT_OK && retry != null) {
            retry.invoke()
        } else if (retry != null) {
            viewModel.showMessage("Permission needed to move this media")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshMedia(forceFullScan = true)
    }
    val hasAccess = hasMediaReadAccess(context)

    LaunchedEffect(Unit) {
        val missing = missingMediaPermissions(context)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (!hasAccess) {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EmptyStateView(
                title = "Media Access Required",
                subtitle = "Allow photo, video and audio access so SULTAN GALLERY can display your device media.",
                actionButtonText = "Allow Media Access",
                onActionClick = {
                    val missing = missingMediaPermissions(context)
                    if (missing.isNotEmpty()) permissionLauncher.launch(missing)
                }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SultanTopBar(
                title = "SULTAN GALLERY",
                isSelectionMode = state.isSelectionMode,
                selectedCount = state.selectedItemIds.size,
                selectedSizeStr = if (state.selectedTotalSize > 0) {
                    val mb = state.selectedTotalSize / (1024.0 * 1024.0)
                    String.format("%.1f MB", mb)
                } else "",
                isSearchActive = state.isSearchActive,
                searchQuery = state.searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onToggleSearch = { viewModel.toggleSearch(it) },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onShareSelected = { viewModel.batchShareSelected() },
                onTrashSelected = { viewModel.batchTrashSelected() },
                onVaultSelected = { viewModel.batchVaultSelected() },
                currentSortOrder = state.sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
                currentGridMode = state.gridMode,
                onGridModeChange = { viewModel.setGridMode(it) },
                currentFormatFilter = state.formatFilter,
                onFormatFilterChange = { viewModel.selectFormatFilter(it) },
                onNavigateToTools = {
                    viewModel.prepareToolSelection()
                    onNavigateToTools()
                },
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Selected Album Header (if drilling into an album)
            if (state.selectedAlbum != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectAlbum(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Albums")
                    }
                    Column {
                        Text(
                            text = state.selectedAlbum!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.filteredMedia.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Category Tabs Bar
                MediaTabsRow(
                    selectedTab = state.currentTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            MediaTab.VAULT -> onNavigateToVault()
                            MediaTab.TRASH -> onNavigateToTrash()
                            else -> viewModel.selectTab(tab)
                        }
                    }
                )

            }

            if (hasLimitedVisualAccess(context)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Limited photo/video access is enabled. Some media may be hidden.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Manage",
                            color = SultanGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Main Content Area
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SultanGold)
                }
            } else if (state.currentTab == MediaTab.ALBUMS && state.selectedAlbum == null) {
                // Albums Grid View
                AlbumsGridView(
                    albums = state.albums,
                    onAlbumClick = { album -> viewModel.selectAlbum(album) }
                )
            } else if (state.filteredMedia.isEmpty()) {
                // Empty State
                EmptyStateView(
                    title = if (state.searchQuery.isNotEmpty()) "No Matching Media" else "No Media Found",
                    subtitle = if (state.searchQuery.isNotEmpty()) "Try searching for a different file name, date, or folder." else "Tap the refresh button or adjust permission access.",
                    actionButtonText = "Refresh Gallery",
                    onActionClick = { viewModel.refreshMedia() }
                )
            } else {
                // Media Grid View
                val columns = when (state.gridMode) {
                    GridMode.COMPACT -> 4
                    GridMode.NORMAL -> 3
                    GridMode.LARGE -> 2
                    GridMode.LIST -> 1
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = if (state.isSelectionMode) 96.dp else 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.filteredMedia,
                        key = { item ->
                            val typePrefix = when {
                                item.isVideo -> "v"
                                item.isAudio -> "a"
                                else -> "p"
                            }
                            "$typePrefix${item.id}"
                        }
                    ) { item ->
                        val isSelected = state.selectedItemIds.contains(item.id)
                        MediaThumbnail(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = state.isSelectionMode,
                            onClick = {
                                if (state.isSelectionMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    when {
                                        item.isAudio -> onNavigateToAudio(item.id)
                                        item.isVideo -> onNavigateToPlayer(item.id)
                                        else -> onNavigateToViewer(item.id)
                                    }
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(item.id)
                            }
                        )
                    }
                }
            }
        }

        if (state.isSelectionMode) {
            SultanQuickPanel(
                selectedCount = state.selectedItemIds.size,
                modifier = Modifier.align(Alignment.BottomCenter),
                onTools = {
                    viewModel.prepareToolSelection()
                    onNavigateToTools()
                },
                onShare = { viewModel.batchShareSelected() },
                onMove = { moveCopyMode = "MOVE" },
                onCopy = { moveCopyMode = "COPY" },
                onDelete = { viewModel.batchTrashSelected() }
            )
        }

        val currentMoveCopyMode = moveCopyMode
        if (currentMoveCopyMode != null) {
            MoveCopyAlbumDialog(
                mode = currentMoveCopyMode,
                albums = state.albums,
                onDismiss = { moveCopyMode = null },
                onAlbumSelected = { album ->
                    val selected = state.selectedItems.toList()
                    val mode = currentMoveCopyMode
                    moveCopyMode = null
                    if (selected.isNotEmpty()) {
                        suspend fun runBatch(startIndex: Int) {
                            var index = startIndex
                            var success = 0
                            while (index < selected.size) {
                                val item = selected[index]
                                if (mode == "MOVE") {
                                    val result = viewModel.repository.moveToAlbum(item, album.name)
                                    if (result.intentSender != null) {
                                        val retryIndex = index
                                        pendingMoveCopyRetry = {
                                            scope.launch { runBatch(retryIndex) }
                                        }
                                        moveCopyPermissionLauncher.launch(
                                            IntentSenderRequest.Builder(result.intentSender).build()
                                        )
                                        return
                                    }
                                    if (result.success) success++
                                } else if (viewModel.repository.copyToAlbum(item, album.name)) {
                                    success++
                                }
                                index++
                            }
                            viewModel.clearSelection()
                            viewModel.refreshMedia()
                            viewModel.showMessage(
                                if (mode == "MOVE") "$success item(s) moved to ${album.name}"
                                else "$success item(s) copied to ${album.name}"
                            )
                        }
                        scope.launch { runBatch(0) }
                    }
                }
            )
        }
        }
    }
}


@Composable
private fun SultanQuickPanel(
    selectedCount: Int,
    onTools: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SultanGold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SultanQuickPanelAction(icon = Icons.Default.AutoAwesome, label = "Tools", onClick = onTools)
                SultanQuickPanelAction(icon = Icons.Default.Share, label = "Share", onClick = onShare)
                SultanQuickPanelAction(icon = Icons.Default.DriveFileMove, label = "Move", onClick = onMove)
                SultanQuickPanelAction(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
                SultanQuickPanelAction(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun SultanQuickPanelAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SultanGold,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoveCopyAlbumDialog(
    mode: String,
    albums: List<MediaAlbum>,
    onDismiss: () -> Unit,
    onAlbumSelected: (MediaAlbum) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == "MOVE") "Move to folder" else "Copy to folder") },
        text = {
            if (albums.isEmpty()) {
                Text("No device folders are available.")
            } else {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(300.dp)) {
                    lazyItems(albums, key = { it.id }) { album ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onAlbumSelected(album) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = SultanGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(album.name, fontWeight = FontWeight.Bold)
                                Text("${album.itemCount} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) { Text("Cancel", color = Color.Black) }
        }
    )
}

@Composable
private fun MediaTabsRow(
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MediaTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    val icon = when (tab) {
                        MediaTab.ALL -> Icons.Default.PhotoLibrary
                        MediaTab.PHOTOS -> Icons.Default.PhotoAlbum
                        MediaTab.VIDEOS -> Icons.Default.VideoLibrary
                        MediaTab.ALBUMS -> Icons.Default.Folder
                        MediaTab.VAULT -> Icons.Default.Lock
                        MediaTab.TRASH -> Icons.Default.Delete
                        else -> null
                    }
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) SultanGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SultanGold.copy(alpha = 0.2f),
                    selectedLabelColor = SultanGold
                )
            )
        }
    }
}

@Composable
private fun AlbumsGridView(
    albums: List<MediaAlbum>,
    onAlbumClick: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (albums.isEmpty()) {
        EmptyStateView(
            title = "No Albums Found",
            subtitle = "Albums will appear here once photos or videos are indexed."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Surface(
                    onClick = { onAlbumClick(album) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f)
                                .background(Color.DarkGray)
                        ) {
                            if (album.coverUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(album.coverUri)
                                        .crossfade(false)
                                        .decoderFactory(VideoFrameDecoder.Factory())
                                        .build(),
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = SultanGold,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.Center)
                                )
                            }

                            // Item count badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${album.itemCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${album.photoCount} photos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (album.videoCount > 0) {
                                    Text(
                                        text = "${album.videoCount} videos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SultanGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

