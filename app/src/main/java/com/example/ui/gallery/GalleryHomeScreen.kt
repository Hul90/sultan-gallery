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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.request.ImageRequest
import com.example.data.model.GridMode
import com.example.data.model.MediaAlbum
import com.example.data.model.MediaItem
import com.example.data.model.MediaTab
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.SultanTopBar
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.SultanGold


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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshMedia()
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
                onNavigateToTools = onNavigateToTools,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

                // Compact folder/album strip so the gallery opens with real device folders visible.
                if (state.albums.isNotEmpty()) {
                    AlbumsQuickRow(
                        albums = state.albums,
                        onAlbumClick = { viewModel.selectAlbum(it) }
                    )
                }

                // Format Filter Bar (Images, RAW, SVG, PSD, PDF, etc.)
                FormatFilterRow(
                    selectedFilter = state.formatFilter,
                    onFilterSelected = { filter -> viewModel.selectFormatFilter(filter) }
                )

                // Quick Tools Header Bar
                QuickToolsBar(onNavigateToTools = onNavigateToTools)
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
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.filteredMedia,
                        key = { it.id }
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
}


@Composable
private fun AlbumsQuickRow(
    albums: List<MediaAlbum>,
    onAlbumClick: (MediaAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Folders & Albums",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${albums.size} folders",
                style = MaterialTheme.typography.labelSmall,
                color = SultanGold
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            albums.take(10).forEach { album ->
                Surface(
                    onClick = { onAlbumClick(album) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.width(104.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (album.coverUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(album.coverUri).crossfade(true).build(),
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = SultanGold, modifier = Modifier.size(34.dp))
                            }
                        }
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text(
                                album.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${album.itemCount} items",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
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
private fun FormatFilterRow(
    selectedFilter: com.example.data.model.FormatFilter,
    onFilterSelected: (com.example.data.model.FormatFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        com.example.data.model.FormatFilter.values().forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SultanGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SultanGold) else null
            ) {
                Text(
                    text = filter.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SultanGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickToolsBar(
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickToolPill("Sultan Tools", Icons.Default.AutoAwesome, SultanGold) { onNavigateToTools() }
        QuickToolPill("Collage", Icons.Default.ViewQuilt, Color(0xFF06B6D4)) { onNavigateToTools() }
        QuickToolPill("Compress", Icons.Default.Compress, Color(0xFF10B981)) { onNavigateToTools() }
        QuickToolPill("Smart Crop", Icons.Default.Crop, Color(0xFFF43F5E)) { onNavigateToTools() }
        QuickToolPill("Cleaner", Icons.Default.CleaningServices, Color(0xFF8B5CF6)) { onNavigateToTools() }
    }
}

@Composable
private fun QuickToolPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
                                        .crossfade(true)
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
