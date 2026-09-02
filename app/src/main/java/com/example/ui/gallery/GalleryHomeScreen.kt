package com.example.ui.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
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

enum class HomeNavTab(val title: String, val icon: ImageVector) {
    PHOTOS("Photos", Icons.Default.PhotoLibrary),
    ALBUMS("Albums", Icons.Default.Folder),
    STUDIO("Studio & Tools", Icons.Default.AutoAwesome),
    VAULT("Vault & Trash", Icons.Default.Lock)
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

    var activeNavTab by remember { mutableStateOf(HomeNavTab.PHOTOS) }
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
                subtitle = "Allow photo, video and audio access so SULTAN GALLERY can display and organize your device media.",
                actionButtonText = "Allow Media Access",
                onActionClick = {
                    val missing = missingMediaPermissions(context)
                    if (missing.isNotEmpty()) permissionLauncher.launch(missing)
                }
            )
        }
        return
    }

    val themeConfig = com.example.ui.theme.LocalSultanThemeConfig.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeConfig.backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
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
            },
            bottomBar = {
                if (!state.isSelectionMode) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        HomeNavTab.values().forEach { tab ->
                            val isSelected = activeNavTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    activeNavTab = tab
                                    if (tab == HomeNavTab.PHOTOS && state.selectedAlbum != null) {
                                        viewModel.selectAlbum(null)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!state.isSelectionMode && activeNavTab == HomeNavTab.PHOTOS) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.prepareToolSelection()
                            onNavigateToTools()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Collage & Tools", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            when (activeNavTab) {
                HomeNavTab.PHOTOS -> {
                    PhotosFeedView(
                        state = state,
                        viewModel = viewModel,
                        context = context,
                        onNavigateToViewer = onNavigateToViewer,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToAudio = onNavigateToAudio
                    )
                }
                HomeNavTab.ALBUMS -> {
                    AlbumsHubView(
                        state = state,
                        viewModel = viewModel,
                        onAlbumSelected = { album ->
                            viewModel.selectAlbum(album)
                            activeNavTab = HomeNavTab.PHOTOS
                        },
                        onNavigateToVault = onNavigateToVault,
                        onNavigateToTrash = onNavigateToTrash,
                        onNavigateToAudio = {
                            val firstAudio = state.allMedia.firstOrNull { it.isAudio }
                            if (firstAudio != null) onNavigateToAudio(firstAudio.id)
                            else viewModel.showMessage("No audio files found")
                        }
                    )
                }
                HomeNavTab.STUDIO -> {
                    StudioHubView(
                        onNavigateToTools = {
                            viewModel.prepareToolSelection()
                            onNavigateToTools()
                        },
                        onNavigateToViewer = {
                            val first = state.allMedia.firstOrNull { !it.isVideo && !it.isAudio }
                            if (first != null) onNavigateToViewer(first.id)
                            else viewModel.showMessage("No photos available to edit")
                        }
                    )
                }
                HomeNavTab.VAULT -> {
                    VaultAndTrashHubView(
                        vaultCount = state.vaultList.size,
                        trashCount = state.trashList.size,
                        onNavigateToVault = onNavigateToVault,
                        onNavigateToTrash = onNavigateToTrash
                    )
                }
            }

            // Floating Multi-Selection Action Panel
            AnimatedVisibility(
                visible = state.isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SultanQuickPanel(
                    selectedCount = state.selectedItemIds.size,
                    hasCollageEligible = state.selectedItems.count { !it.isVideo && !it.isAudio } >= 2,
                    onCollage = {
                        viewModel.prepareToolSelection()
                        onNavigateToTools()
                    },
                    onTools = {
                        viewModel.prepareToolSelection()
                        onNavigateToTools()
                    },
                    onShare = { viewModel.batchShareSelected() },
                    onMove = { moveCopyMode = "MOVE" },
                    onCopy = { moveCopyMode = "COPY" },
                    onVault = { viewModel.batchVaultSelected() },
                    onDelete = { viewModel.batchTrashSelected() }
                )
            }

            // Move / Copy Album Dialog
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
}

@Composable
private fun PhotosFeedView(
    state: GalleryUiState,
    viewModel: GalleryViewModel,
    context: android.content.Context,
    onNavigateToViewer: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToAudio: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Selected Album Header (if drilling into an album)
        if (state.selectedAlbum != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectAlbum(null) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to All Albums",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.selectedAlbum!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.filteredMedia.size} items in folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Category Filter Chips Bar
            MediaTabsRow(
                selectedTab = state.currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        }

        if (hasLimitedVisualAccess(context)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Limited photo/video access is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Manage Access",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Main Media Grid Area
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.currentTab == MediaTab.ALBUMS && state.selectedAlbum == null) {
            AllAlbumsGridView(
                albums = state.albums,
                context = context,
                onAlbumSelected = { album -> viewModel.selectAlbum(album) },
                modifier = Modifier.fillMaxSize()
            )
        } else if (state.filteredMedia.isEmpty()) {
            EmptyStateView(
                title = if (state.searchQuery.isNotEmpty()) "No Matching Media" else "No Photos or Videos Found",
                subtitle = if (state.searchQuery.isNotEmpty()) "Try searching for a different file name or extension." else "Photos and videos on your device will appear here.",
                actionButtonText = "Refresh Gallery",
                onActionClick = { viewModel.refreshMedia(forceFullScan = true) }
            )
        } else {
            val columns = when (state.gridMode) {
                GridMode.COMPACT -> 4
                GridMode.NORMAL -> 3
                GridMode.LARGE -> 2
                GridMode.LIST -> 1
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = if (state.isSelectionMode) 100.dp else 80.dp),
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
}

@Composable
private fun AllAlbumsGridView(
    albums: List<MediaAlbum>,
    context: android.content.Context,
    onAlbumSelected: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        EmptyStateView(
            title = "No Albums Found",
            subtitle = "Folders containing pictures and videos on your device will appear here."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(albums, key = { it.id }) { album ->
                Surface(
                    onClick = { onAlbumSelected(album) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
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
                                        .crossfade(true)
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.Center)
                                )
                            }

                            // Bottom subtle gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )

                            // Item count pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${album.itemCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
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
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
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

@Composable
private fun AlbumsHubView(
    state: GalleryUiState,
    viewModel: GalleryViewModel,
    onAlbumSelected: (MediaAlbum) -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToAudio: () -> Unit
) {
    val context = LocalContext.current
    val albums = state.albums

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Category Cards Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAlbumCard(
                    title = "Videos",
                    count = "${state.allMedia.count { it.isVideo }} items",
                    icon = Icons.Default.VideoLibrary,
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.selectTab(MediaTab.VIDEOS)
                        viewModel.selectAlbum(null)
                    }
                )
                QuickAlbumCard(
                    title = "Audio",
                    count = "${state.allMedia.count { it.isAudio }} items",
                    icon = Icons.Default.Mic,
                    color = Color(0xFF8E24AA),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAudio
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAlbumCard(
                    title = "Secret Vault",
                    count = "${state.vaultList.size} protected",
                    icon = Icons.Default.Lock,
                    color = SultanGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToVault
                )
                QuickAlbumCard(
                    title = "Recycle Bin",
                    count = "${state.trashList.size} in trash",
                    icon = Icons.Default.Delete,
                    color = Color(0xFFE53935),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTrash
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device Folders & Albums (${albums.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (albums.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Albums Found",
                    subtitle = "Folders containing pictures and videos will appear here."
                )
            }
        } else {
            val chunked = albums.chunked(2)
            lazyItems(chunked) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { album ->
                        Surface(
                            onClick = { onAlbumSelected(album) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
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
                                                .crossfade(true)
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
                                            modifier = Modifier.size(48.dp).align(Alignment.Center)
                                        )
                                    }

                                    // Item count pill
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
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

                                Column(modifier = Modifier.padding(10.dp)) {
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
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAlbumCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StudioHubView(
    onNavigateToTools: () -> Unit,
    onNavigateToViewer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Hero Collage Studio Card
            Card(
                onClick = onNavigateToTools,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(SultanGold.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SultanGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ViewQuilt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Photo Collage Studio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SultanGold
                                    )
                                    Text(
                                        text = "Combine 2 to 9 photos into stylish grids",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToTools,
                            colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Launch Collage Maker", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Creative & Utility Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            StudioToolTile(
                title = "Photo Editor & Effects",
                desc = "Brush painting, text stickers, smart crop, filters & color grading",
                icon = Icons.Default.AutoAwesome,
                iconColor = SultanGold,
                onClick = onNavigateToViewer
            )
        }

        item {
            StudioToolTile(
                title = "Contact Sheet Builder",
                desc = "Generate indexed thumbnail sheets with timestamps & file info",
                icon = Icons.Default.GridOn,
                iconColor = Color(0xFF00ACC1),
                onClick = onNavigateToTools
            )
        }

        item {
            StudioToolTile(
                title = "Compress & Resize Studio",
                desc = "Reduce large photos up to 85% without noticeable quality loss",
                icon = Icons.Default.Compress,
                iconColor = Color(0xFF43A047),
                onClick = onNavigateToTools
            )
        }

        item {
            StudioToolTile(
                title = "Format Converter & PDF Maker",
                desc = "Convert images between JPG, PNG, WebP or generate multi-page PDFs",
                icon = Icons.Default.PictureAsPdf,
                iconColor = Color(0xFFFB8C00),
                onClick = onNavigateToTools
            )
        }

        item {
            StudioToolTile(
                title = "Duplicate & Screenshot Cleaner",
                desc = "Scan and reclaim device storage by removing duplicate and clutter files",
                icon = Icons.Default.CleaningServices,
                iconColor = Color(0xFFE53935),
                onClick = onNavigateToTools
            )
        }
    }
}

@Composable
private fun StudioToolTile(
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VaultAndTrashHubView(
    vaultCount: Int,
    trashCount: Int,
    onNavigateToVault: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            onClick = onNavigateToVault,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SultanGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SultanGold, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Secret Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SultanGold)
                    Text("$vaultCount encrypted media items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Protected with AES-256 GCM encryption and private biometric/PIN lock.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Card(
            onClick = onNavigateToTrash,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recycle Bin (Trash)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE53935))
                    Text("$trashCount items in trash", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deleted photos are held safely for 30 days before permanent deletion.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun SultanQuickPanel(
    selectedCount: Int,
    hasCollageEligible: Boolean,
    onCollage: () -> Unit,
    onTools: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onVault: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SultanGold.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SultanGold
                )
                if (hasCollageEligible) {
                    Button(
                        onClick = onCollage,
                        colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ViewQuilt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Collage", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SultanQuickPanelAction(icon = Icons.Default.Share, label = "Share", onClick = onShare)
                SultanQuickPanelAction(icon = Icons.Default.AutoAwesome, label = "Tools", onClick = onTools)
                SultanQuickPanelAction(icon = Icons.Default.DriveFileMove, label = "Move", onClick = onMove)
                SultanQuickPanelAction(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
                SultanQuickPanelAction(icon = Icons.Default.Lock, label = "Vault", onClick = onVault)
                SultanQuickPanelAction(icon = Icons.Default.Delete, label = "Trash", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun SultanQuickPanelAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = SultanGold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
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
        title = { Text(if (mode == "MOVE") "Move to folder" else "Copy to folder", fontWeight = FontWeight.Bold) },
        text = {
            if (albums.isEmpty()) {
                Text("No device folders are available.")
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    lazyItems(albums, key = { it.id }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAlbumSelected(album) }
                                .padding(10.dp),
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
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) {
                Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MediaTabsRow(
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickTabs = listOf(
        MediaTab.ALBUMS,
        MediaTab.ALL,
        MediaTab.PHOTOS,
        MediaTab.VIDEOS,
        MediaTab.FAVORITES
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickTabs.forEach { tab ->
            val isSelected = selectedTab == tab
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    val icon = when (tab) {
                        MediaTab.ALBUMS -> Icons.Default.Folder
                        MediaTab.ALL -> Icons.Default.PhotoLibrary
                        MediaTab.PHOTOS -> Icons.Default.PhotoAlbum
                        MediaTab.VIDEOS -> Icons.Default.VideoLibrary
                        MediaTab.FAVORITES -> Icons.Default.Favorite
                        MediaTab.AUDIO -> Icons.Default.Mic
                        else -> null
                    }
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
