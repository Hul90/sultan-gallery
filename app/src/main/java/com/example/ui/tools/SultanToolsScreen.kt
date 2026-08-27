package com.example.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MediaThumbnail
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CropPreset
import com.example.data.model.MediaItem
import com.example.tools.SultanContactSheet
import com.example.tools.SultanDuplicateFinder
import com.example.tools.SultanFormatConverter
import com.example.tools.SultanImageCompressor
import com.example.tools.SultanImageResizer
import com.example.tools.SultanLargeFileFinder
import com.example.tools.SultanMediaOrganizer
import com.example.tools.SultanPhotoCollage
import com.example.tools.SultanScreenshotCleaner
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SultanToolsScreen(
    galleryViewModel: GalleryViewModel,
    initialMediaId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by galleryViewModel.uiState.collectAsStateWithLifecycle()

    var activeToolDialog by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(initialMediaId, state.allMedia, state.toolSelectionIds) {
        if (state.allMedia.isNotEmpty() && selectedIds.isEmpty()) {
            val requestedIds = if (initialMediaId != null && initialMediaId > 0L) {
                setOf(initialMediaId)
            } else {
                state.toolSelectionIds
            }
            if (requestedIds.isNotEmpty()) {
                val requested = state.allMedia.filter { requestedIds.contains(it.id) && !it.isAudio }
                if (requested.isNotEmpty()) {
                    selectedIds = requested.map { it.id }.toSet()
                    selectedFolderId = requested.first().bucketId.ifBlank { requested.first().bucketName }
                }
                galleryViewModel.clearToolSelection()
            }
        }
    }

    val folderMedia = remember(state.allMedia, selectedFolderId, selectedIds) {
        if (selectedFolderId == null) {
            state.allMedia.filter { !it.isAudio }
        } else {
            state.allMedia.filter { item ->
                !item.isAudio && (
                    item.bucketId == selectedFolderId ||
                    item.bucketName.equals(selectedFolderId, ignoreCase = true) ||
                    selectedIds.contains(item.id)
                )
            }
        }
    }
    val selectedMedia = remember(state.allMedia, selectedIds) {
        state.allMedia.filter { selectedIds.contains(it.id) }
    }
    // Keep the selected files first, but retain the entire working folder so the user
    // can add more photos without leaving Sultan Tools.
    val workingMedia = remember(folderMedia, selectedMedia) {
        if (selectedMedia.isEmpty()) folderMedia
        else selectedMedia + folderMedia.filterNot { selectedIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SultanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SULTAN TOOLS", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = SultanGold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Working-folder and file selector. This keeps the home gallery clean while
            // still letting every tool operate on exactly the files the user chooses.
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SultanGold.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = SultanGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Working Folder", fontWeight = FontWeight.Bold)
                                Text(
                                    if (selectedFolderId == null) "All folders" else folderMedia.firstOrNull()?.bucketName ?: "Selected folder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                "${selectedMedia.size} selected",
                                color = if (selectedMedia.isNotEmpty()) SultanGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (selectedMedia.isNotEmpty()) {
                                TextButton(onClick = { selectedIds = emptySet() }) {
                                    Text("Clear", color = SultanGold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedFolderId == null,
                                onClick = {
                                    selectedFolderId = null
                                    selectedIds = emptySet()
                                },
                                label = { Text("All Folders", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.2f), selectedLabelColor = SultanGold)
                            )
                            state.albums.forEach { album ->
                                FilterChip(
                                    selected = selectedFolderId == album.id || selectedFolderId.equals(album.name, ignoreCase = true),
                                    onClick = {
                                        selectedFolderId = album.id
                                        selectedIds = emptySet()
                                    },
                                    label = { Text(album.name, maxLines = 1, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.2f), selectedLabelColor = SultanGold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedMedia.isNotEmpty()) "Selected files are used by the tools below." else "Select files below. If none are selected, the selected folder is used.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            userScrollEnabled = true
                        ) {
                            gridItems(folderMedia, key = { item -> "tool_${item.id}_${item.uri}" }) { item ->
                                val selected = selectedIds.contains(item.id)
                                MediaThumbnail(
                                    item = item,
                                    isSelected = selected,
                                    isSelectionMode = selectedIds.isNotEmpty(),
                                    onClick = {
                                        selectedIds = if (selected) selectedIds - item.id else selectedIds + item.id
                                    },
                                    onLongClick = {
                                        selectedIds = if (selected) selectedIds - item.id else selectedIds + item.id
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SultanGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Professional Media Toolkit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SultanGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Advanced photo utilities, batch duplicate scanner, image compression, printable contact sheets, and media organizer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tool Items
            item {
                ToolItemCard(
                    title = "Sultan Photo Collage",
                    description = "Combine 2 to 9 photos into stylish grid collages with custom frames.",
                    icon = Icons.Default.ViewQuilt,
                    badgeColor = Color(0xFF06B6D4),
                    onClick = { activeToolDialog = "COLLAGE" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Image Compressor",
                    description = "Reduce image file size with custom quality levels without losing clarity.",
                    icon = Icons.Default.Compress,
                    badgeColor = Color(0xFF10B981),
                    onClick = { activeToolDialog = "COMPRESS" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Contact Sheet Generator",
                    description = "Generate high-resolution printable media index sheets with metadata.",
                    icon = Icons.Default.GridOn,
                    badgeColor = Color(0xFF8B5CF6),
                    onClick = { activeToolDialog = "CONTACT_SHEET" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Duplicate Finder",
                    description = "Scan gallery to find duplicate and identical photos wasting storage.",
                    icon = Icons.Default.ContentCopy,
                    badgeColor = Color(0xFFF43F5E),
                    onClick = { activeToolDialog = "DUPLICATES" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Large File Cleaner",
                    description = "Find media larger than 10MB, 50MB, or 100MB to reclaim storage.",
                    icon = Icons.Default.CleaningServices,
                    badgeColor = Color(0xFFFFB300),
                    onClick = { activeToolDialog = "LARGE_FILES" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Media Organizer & Stats",
                    description = "Storage breakdown by Camera, Screenshots, Downloads, WhatsApp & Videos.",
                    icon = Icons.Default.PieChart,
                    badgeColor = Color(0xFF38BDF8),
                    onClick = { activeToolDialog = "ORGANIZER" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Format Converter",
                    description = "Convert photos, RAW, SVG, or PDFs instantly to JPG, PNG, WEBP, or PDF.",
                    icon = Icons.Default.Transform,
                    badgeColor = Color(0xFFA855F7),
                    onClick = { activeToolDialog = "CONVERTER" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Format Analyzer",
                    description = "Inspect deep container codec, magic bytes, color gamut, bit depth & EXIF.",
                    icon = Icons.Default.DataObject,
                    badgeColor = SultanGold,
                    onClick = { activeToolDialog = "FORMAT_ANALYZER_SELECT" }
                )
            }

            item {
                ToolItemCard(
                    title = "Sultan Images to PDF Document",
                    description = "Combine multiple gallery photos into a high quality multi-page PDF document.",
                    icon = Icons.Default.PictureAsPdf,
                    badgeColor = Color(0xFFEF4444),
                    onClick = { activeToolDialog = "IMAGES_TO_PDF" }
                )
            }
        }

        var analyzerSelectedItem by remember { mutableStateOf<MediaItem?>(null) }

        // Active Tool Dialogs
        when (activeToolDialog) {
            "FORMAT_ANALYZER_SELECT" -> {
                AlertDialog(
                    onDismissRequest = { activeToolDialog = null },
                    title = { Text("Select Media to Inspect") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Choose any media item from your gallery:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.height(260.dp)) {
                                items(workingMedia.take(60)) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                analyzerSelectedItem = item
                                                activeToolDialog = null
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔍 ", fontSize = 14.sp)
                                        Column {
                                            Text(item.displayName, maxLines = 1, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text("${item.mimeType} • ${item.formattedSize}", style = MaterialTheme.typography.labelSmall, color = SultanGold)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { activeToolDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) {
                            Text("Cancel", color = Color.Black)
                        }
                    }
                )
            }
            "IMAGES_TO_PDF" -> {
                ImagesToPdfDialog(
                    mediaList = workingMedia.filter { !it.isVideo && !it.isAudio },
                    onDismiss = { activeToolDialog = null },
                    onGeneratePdf = { selectedItems ->
                        scope.launch {
                            val pdfBytes = SultanFormatConverter.imagesToPdf(context, selectedItems.map { it.uri })
                            if (pdfBytes != null) {
                                val uri = galleryViewModel.repository.saveDocumentBytes(pdfBytes, "Sultan_Doc_${System.currentTimeMillis()}.pdf", "application/pdf")
                                if (uri != null) {
                                    galleryViewModel.showMessage("Multi-page PDF created and saved to Documents!")
                                } else {
                                    galleryViewModel.showMessage("PDF generated successfully")
                                }
                                galleryViewModel.refreshMedia()
                            } else {
                                galleryViewModel.showMessage("Failed to generate PDF")
                            }
                            activeToolDialog = null
                        }
                    }
                )
            }
            "COLLAGE" -> CollageDialog(
                mediaList = workingMedia.filter { !it.isVideo && !it.isAudio },
                onDismiss = { activeToolDialog = null },
                onCollageCreated = { bmp ->
                    scope.launch {
                        galleryViewModel.repository.saveEditedBitmap(bmp, "SULTAN_COLLAGE")
                        galleryViewModel.showMessage("Collage saved to SultanGallery!")
                        galleryViewModel.refreshMedia()
                        activeToolDialog = null
                    }
                }
            )
            "COMPRESS" -> CompressorDialog(
                mediaList = workingMedia.filter { !it.isVideo && !it.isAudio },
                onDismiss = { activeToolDialog = null },
                onCompress = { item, level ->
                    scope.launch {
                        val result = SultanImageCompressor.compress(context, item.uri, level)
                        galleryViewModel.repository.saveEditedBitmap(result.compressedBitmap, "${item.displayName}_COMPRESSED")
                        val savedMb = (result.originalSize - result.estimatedSize) / (1024.0 * 1024.0)
                        galleryViewModel.showMessage(String.format("Saved %.2f MB! Compressed photo saved.", savedMb.coerceAtLeast(0.0)))
                        galleryViewModel.refreshMedia()
                        activeToolDialog = null
                    }
                }
            )
            "CONTACT_SHEET" -> ContactSheetDialog(
                mediaList = workingMedia.filter { !it.isVideo && !it.isAudio },
                onDismiss = { activeToolDialog = null },
                onGenerate = { items, cols ->
                    scope.launch {
                        val sheet = SultanContactSheet.generateContactSheet(context, items, cols)
                        galleryViewModel.repository.saveEditedBitmap(sheet, "SULTAN_CONTACT_SHEET")
                        galleryViewModel.showMessage("Contact Sheet saved to gallery!")
                        galleryViewModel.refreshMedia()
                        activeToolDialog = null
                    }
                }
            )
            "DUPLICATES" -> DuplicatesDialog(
                mediaList = workingMedia,
                onDismiss = { activeToolDialog = null },
                onDelete = { item ->
                    galleryViewModel.trashMediaItem(item)
                }
            )
            "LARGE_FILES" -> LargeFilesDialog(
                mediaList = workingMedia,
                onDismiss = { activeToolDialog = null },
                onDelete = { item ->
                    galleryViewModel.trashMediaItem(item)
                }
            )
            "ORGANIZER" -> OrganizerDialog(
                mediaList = workingMedia,
                onDismiss = { activeToolDialog = null }
            )
            "CONVERTER" -> ConverterDialog(
                mediaList = workingMedia.filter { !it.isVideo && !it.isAudio },
                onDismiss = { activeToolDialog = null },
                onConvert = { item, fmt ->
                    scope.launch {
                        val (bmp, res) = SultanFormatConverter.convert(context, item.uri, fmt)
                        if (fmt == SultanFormatConverter.TargetFormat.PDF) {
                            galleryViewModel.repository.saveDocumentBytes(res.outputBytes, "${item.displayName}_CONVERTED.pdf", "application/pdf")
                        } else {
                            galleryViewModel.repository.saveEditedBitmap(bmp, "${item.displayName}_CONVERTED", fmt.format ?: Bitmap.CompressFormat.JPEG)
                        }
                        galleryViewModel.showMessage("Converted to ${fmt.extension.uppercase()} and saved!")
                        galleryViewModel.refreshMedia()
                        activeToolDialog = null
                    }
                }
            )
        }

        if (analyzerSelectedItem != null) {
            SultanFormatAnalyzerDialog(
                item = analyzerSelectedItem!!,
                onDismiss = { analyzerSelectedItem = null }
            )
        }
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CollageDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onCollageCreated: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isBuilding by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sultan Photo Collage") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select up to 4 photos from your gallery to create a collage:")
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(mediaList.take(24)) { item ->
                        val isSel = selectedUris.contains(item.uri)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSel) selectedUris = selectedUris - item.uri
                                    else if (selectedUris.size < 4) selectedUris = selectedUris + item.uri
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSel) "☑ " else "☐ ", fontSize = 18.sp, color = SultanGold)
                            Text(item.displayName, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUris.size >= 2) {
                        isBuilding = true
                        scope.launch {
                            val bmp = SultanPhotoCollage.createCollage(context, selectedUris)
                            onCollageCreated(bmp)
                        }
                    }
                },
                enabled = selectedUris.size >= 2 && !isBuilding,
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                if (isBuilding) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Create Collage (${selectedUris.size})", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CompressorDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onCompress: (MediaItem, SultanImageCompressor.CompressionLevel) -> Unit
) {
    var selectedItem by remember { mutableStateOf(mediaList.firstOrNull()) }
    var level by remember { mutableStateOf(SultanImageCompressor.CompressionLevel.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sultan Image Compressor") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select image to compress:")
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(mediaList.take(15)) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItem = item }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (selectedItem?.id == item.id) "● " else "○ ", color = SultanGold)
                            Text("${item.displayName} (${item.formattedSize})", maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Compression preset:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SultanImageCompressor.CompressionLevel.values().take(4).forEach { l ->
                        FilterChip(
                            selected = level == l,
                            onClick = { level = l },
                            label = { Text(l.title.split(" ").first(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedItem?.let { onCompress(it, level) } },
                enabled = selectedItem != null,
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                Text("Compress & Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ContactSheetDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onGenerate: (List<MediaItem>, Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Sheet Generator") },
        text = {
            Text("Generate a master index contact sheet for the first ${mediaList.take(20).size} photos with file names, sizes, and timestamps formatted for printing.")
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(mediaList.take(20), 4) },
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                Text("Generate Sheet", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DuplicatesDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    val duplicates = remember(mediaList) { SultanDuplicateFinder.findDuplicates(mediaList) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sultan Duplicate Finder") },
        text = {
            if (duplicates.isEmpty()) {
                Text("No duplicate media files detected. Your gallery is clean!")
            } else {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(duplicates) { group ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("${group.items.size} matching files (${group.items.first().formattedSize})", fontWeight = FontWeight.Bold, color = SultanGold)
                                group.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.displayName, maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { onDelete(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) {
                Text("Done", color = Color.Black)
            }
        }
    )
}

@Composable
private fun LargeFilesDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    var threshold by remember { mutableStateOf(SultanLargeFileFinder.SizeThreshold.MB_10) }
    val largeFiles = remember(mediaList, threshold) {
        SultanLargeFileFinder.findLargeFiles(mediaList, threshold)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Large File Cleaner") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(
                        SultanLargeFileFinder.SizeThreshold.MB_10,
                        SultanLargeFileFinder.SizeThreshold.MB_50,
                        SultanLargeFileFinder.SizeThreshold.MB_100
                    ).forEach { th ->
                        FilterChip(
                            selected = threshold == th,
                            onClick = { threshold = th },
                            label = { Text(th.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Found ${largeFiles.size} files:", fontWeight = FontWeight.Bold, color = SultanGold)
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(largeFiles) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayName, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                Text(item.formattedSize, color = SultanGold, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) {
                Text("Close", color = Color.Black)
            }
        }
    )
}

@Composable
private fun OrganizerDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit
) {
    val stats = remember(mediaList) { SultanMediaOrganizer.organizeMedia(mediaList) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media Storage Organizer") },
        text = {
            LazyColumn(modifier = Modifier.height(260.dp)) {
                items(stats) { stat ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stat.title, fontWeight = FontWeight.Bold)
                                Text("${stat.count} items", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(stat.formattedSize, fontWeight = FontWeight.Bold, color = SultanGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SultanGold)) {
                Text("Close", color = Color.Black)
            }
        }
    )
}

@Composable
private fun ConverterDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onConvert: (MediaItem, SultanFormatConverter.TargetFormat) -> Unit
) {
    var selectedItem by remember { mutableStateOf(mediaList.firstOrNull()) }
    var targetFormat by remember { mutableStateOf(SultanFormatConverter.TargetFormat.WEBP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sultan Format Converter") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select image to convert:")
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(130.dp)) {
                    items(mediaList.take(15)) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedItem = item }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (selectedItem?.id == item.id) "● " else "○ ", color = SultanGold)
                            Text(item.displayName, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Convert to format:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SultanFormatConverter.TargetFormat.values().forEach { fmt ->
                        FilterChip(
                            selected = targetFormat == fmt,
                            onClick = { targetFormat = fmt },
                            label = { Text(fmt.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedItem?.let { onConvert(it, targetFormat) } },
                enabled = selectedItem != null,
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                Text("Convert", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImagesToPdfDialog(
    mediaList: List<MediaItem>,
    onDismiss: () -> Unit,
    onGeneratePdf: (List<MediaItem>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Images to PDF Document") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select photos to compile into a PDF:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(mediaList.take(30)) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSelected) "☑ " else "☐ ", color = if (isSelected) SultanGold else Color.Gray, fontSize = 16.sp)
                            Text(item.displayName, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Selected: ${selectedIds.size} images", fontWeight = FontWeight.Bold, color = SultanGold, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = mediaList.filter { selectedIds.contains(it.id) }
                    onGeneratePdf(selected)
                },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                Text("Create PDF", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
