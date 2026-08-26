package com.example.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.TrashEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SultanGold
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    galleryViewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by galleryViewModel.uiState.collectAsStateWithLifecycle()
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = SultanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RECYCLE BIN / TRASH", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.trashList.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            if (state.trashList.isEmpty()) {
                EmptyStateView(
                    title = "Recycle Bin is Empty",
                    subtitle = "Items moved to trash will appear here for 30 days before permanent removal.",
                    icon = Icons.Default.DeleteSweep
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.trashList, key = { it.uriString }) { trashItem ->
                        TrashMediaThumbnail(
                            trashEntity = trashItem,
                            onRestore = { galleryViewModel.restoreTrashItem(trashItem) },
                            onDelete = { galleryViewModel.permanentlyDeleteTrashItem(trashItem) }
                        )
                    }
                }
            }
        }

        if (showEmptyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmDialog = false },
                title = { Text("Empty Recycle Bin?") },
                text = { Text("All ${state.trashList.size} items will be permanently deleted and cannot be recovered.") },
                confirmButton = {
                    Button(
                        onClick = {
                            galleryViewModel.emptyTrash()
                            showEmptyConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permanently Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirmDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun TrashMediaThumbnail(
    trashEntity: TrashEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val imageModel: Any = remember(trashEntity.uriString, trashEntity.trashFilePath) {
        if (trashEntity.trashFilePath.isNotBlank() && File(trashEntity.trashFilePath).exists()) {
            File(trashEntity.trashFilePath)
        } else {
            android.net.Uri.parse(trashEntity.uriString)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = trashEntity.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = SultanGold, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}
