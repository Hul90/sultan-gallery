package com.example.ui.editor

import android.graphics.Bitmap
import android.graphics.Path
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CropPreset
import com.example.data.model.FilterType
import com.example.editor.DrawPath
import com.example.editor.DrawTool
import com.example.editor.EditorTab
import com.example.editor.PhotoEditorViewModel
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    galleryViewModel: GalleryViewModel,
    editorViewModel: PhotoEditorViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()

    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        editorViewModel.loadBitmapFromUri(context, imageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sultan Photo Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editorViewModel.undo() }, enabled = state.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) SultanGold else Color.Gray
                        )
                    }
                    IconButton(onClick = { editorViewModel.redo() }, enabled = state.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) SultanGold else Color.Gray
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val finalBmp = editorViewModel.renderFinalBitmap()
                                if (finalBmp != null) {
                                    val savedUri = galleryViewModel.repository.saveEditedBitmap(
                                        bitmap = finalBmp,
                                        baseName = "SULTAN_${System.currentTimeMillis()}"
                                    )
                                    if (savedUri != null) {
                                        galleryViewModel.showMessage("Photo saved to SultanGallery album!")
                                        galleryViewModel.refreshMedia()
                                        onNavigateBack()
                                    } else {
                                        galleryViewModel.showMessage("Failed to save image")
                                    }
                                }
                                isSaving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
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
        ) {
            // Main Canvas / Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07090E))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.previewBitmap != null) {
                    val bmp = state.previewBitmap!!
                    var currentPath by remember { mutableStateOf<Path?>(null) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.activeTab, state.currentDrawTool) {
                                if (state.activeTab == EditorTab.DRAW) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val p = Path().apply { moveTo(offset.x, offset.y) }
                                            currentPath = p
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                        },
                                        onDragEnd = {
                                            currentPath?.let { p ->
                                                editorViewModel.addDrawPath(
                                                    DrawPath(
                                                        path = p,
                                                        color = state.drawColor,
                                                        strokeWidth = state.brushSize,
                                                        isHighlighter = state.currentDrawTool == DrawTool.HIGHLIGHTER,
                                                        isEraser = state.currentDrawTool == DrawTool.ERASER
                                                    )
                                                )
                                            }
                                            currentPath = null
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Editor Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    CircularProgressIndicator(color = SultanGold)
                }
            }

            // Bottom Tool Controls Drawer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Tool panel content based on selected tab
                    when (state.activeTab) {
                        EditorTab.ADJUST -> AdjustmentsPanel(state, editorViewModel)
                        EditorTab.FILTERS -> FiltersPanel(state, editorViewModel)
                        EditorTab.CROP -> SmartCropPanel(state, editorViewModel)
                        EditorTab.TRANSFORM -> TransformPanel(state, editorViewModel)
                        EditorTab.DRAW -> DrawPanel(state, editorViewModel)
                        EditorTab.TEXT -> TextPanel(state, onAddTextClick = { showTextDialog = true })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorTab.values().forEach { tab ->
                            val isSelected = state.activeTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { editorViewModel.setActiveTab(tab) },
                                label = { Text(tab.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    val icon = when (tab) {
                                        EditorTab.ADJUST -> Icons.Default.Tune
                                        EditorTab.FILTERS -> Icons.Default.Filter
                                        EditorTab.CROP -> Icons.Default.Crop
                                        EditorTab.TRANSFORM -> Icons.Default.RotateRight
                                        EditorTab.DRAW -> Icons.Default.Brush
                                        EditorTab.TEXT -> Icons.Default.TextFields
                                    }
                                    Icon(icon, contentDescription = null, tint = if (isSelected) SultanGold else Color.Gray)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SultanGold.copy(alpha = 0.2f),
                                    selectedLabelColor = SultanGold
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                title = { Text("Add Text Sticker") },
                text = {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Enter text...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            editorViewModel.addTextSticker(textInput)
                            textInput = ""
                            showTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
                    ) {
                        Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun AdjustmentsPanel(state: com.example.editor.EditorSessionState, vm: PhotoEditorViewModel) {
    var activeAdjust by remember { mutableStateOf("Brightness") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Brightness", "Contrast", "Saturation", "Warmth", "Tint", "Vignette").forEach { adj ->
                FilterChip(
                    selected = activeAdjust == adj,
                    onClick = { activeAdjust = adj },
                    label = { Text(adj, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (activeAdjust) {
            "Brightness" -> SliderRow(label = "Brightness", value = state.brightness, range = -60f..60f) { vm.updateAdjustment(brightness = it) }
            "Contrast" -> SliderRow(label = "Contrast", value = state.contrast, range = 0.5f..1.8f) { vm.updateAdjustment(contrast = it) }
            "Saturation" -> SliderRow(label = "Saturation", value = state.saturation, range = 0f..2f) { vm.updateAdjustment(saturation = it) }
            "Warmth" -> SliderRow(label = "Warmth", value = state.temperature, range = -40f..40f) { vm.updateAdjustment(temperature = it) }
            "Tint" -> SliderRow(label = "Tint", value = state.tint, range = -40f..40f) { vm.updateAdjustment(tint = it) }
            "Vignette" -> SliderRow(label = "Vignette", value = state.vignette, range = 0f..1f) { vm.updateAdjustment(vignette = it) }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = SultanGold, modifier = Modifier.width(75.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = SultanGold, activeTrackColor = SultanGold),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FiltersPanel(state: com.example.editor.EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.values().forEach { filter ->
            val isSelected = state.activeFilter == filter
            Surface(
                onClick = { vm.setFilter(filter) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) SultanGold else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else SultanGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filter.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartCropPanel(state: com.example.editor.EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CropPreset.values().forEach { preset ->
            FilterChip(
                selected = state.selectedCropPreset == preset,
                onClick = { vm.applyCropPreset(preset) },
                label = { Text(preset.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
            )
        }
    }
}

@Composable
private fun TransformPanel(state: com.example.editor.EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { vm.rotate90() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.Default.RotateRight, contentDescription = null, tint = SultanGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rotate 90°", color = MaterialTheme.colorScheme.onSurface)
        }

        Button(
            onClick = { vm.toggleFlipHorizontal() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.Default.Flip, contentDescription = null, tint = SultanGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Flip H", color = MaterialTheme.colorScheme.onSurface)
        }

        Button(
            onClick = { vm.toggleFlipVertical() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.Default.Flip, contentDescription = null, tint = SultanGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Flip V", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DrawPanel(state: com.example.editor.EditorSessionState, vm: PhotoEditorViewModel) {
    val colors = listOf(
        android.graphics.Color.RED,
        android.graphics.Color.YELLOW,
        android.graphics.Color.GREEN,
        android.graphics.Color.CYAN,
        android.graphics.Color.BLUE,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Pair(DrawTool.BRUSH, "Brush"),
                Pair(DrawTool.HIGHLIGHTER, "Highlighter"),
                Pair(DrawTool.ERASER, "Eraser")
            ).forEach { (tool, label) ->
                FilterChip(
                    selected = state.currentDrawTool == tool,
                    onClick = { vm.setDrawTool(tool) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SultanGold.copy(alpha = 0.25f), selectedLabelColor = SultanGold)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Color palette
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (state.drawColor == c) 3.dp else 1.dp,
                            color = if (state.drawColor == c) SultanGold else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { vm.setDrawColor(c) }
                )
            }
        }
    }
}

@Composable
private fun TextPanel(state: com.example.editor.EditorSessionState, onAddTextClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${state.textStickers.size} text stickers added",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onAddTextClick,
            colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
        ) {
            Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Text", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
