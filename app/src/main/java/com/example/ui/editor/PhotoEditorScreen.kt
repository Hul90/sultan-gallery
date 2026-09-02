package com.example.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CropPreset
import com.example.data.model.FilterType
import com.example.data.model.MediaItem
import com.example.editor.DrawPath
import com.example.editor.DrawPoint
import com.example.editor.DrawTool
import com.example.editor.EditorSessionState
import com.example.editor.EditorTab
import com.example.editor.PhotoEditorViewModel
import com.example.editor.TextSticker
import com.example.ui.components.ImmersiveMode
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SultanGold
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val galleryState by galleryViewModel.uiState.collectAsStateWithLifecycle()

    ImmersiveMode(enabled = true)

    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var selectedTextColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }
    var selectedBgColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }
    var selectedFontSize by remember { mutableFloatStateOf(24f) }
    var isSaving by remember { mutableStateOf(false) }
    var showCollagePicker by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        editorViewModel.loadBitmapFromUri(context, imageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sultan Photo Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Hold to compare original
                    IconButton(
                        onClick = { editorViewModel.setShowOriginal(!state.showOriginal) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Compare",
                            tint = if (state.showOriginal) SultanGold else Color.LightGray
                        )
                    }

                    // Undo
                    IconButton(onClick = { editorViewModel.undo() }, enabled = state.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) SultanGold else Color.DarkGray
                        )
                    }

                    // Redo
                    IconButton(onClick = { editorViewModel.redo() }, enabled = state.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) SultanGold else Color.DarkGray
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val finalBmp = editorViewModel.renderFinalBitmap()
                                if (finalBmp != null) {
                                    val savedUri = galleryViewModel.repository.saveEditedBitmap(
                                        bitmap = finalBmp,
                                        baseName = "SULTAN_EDIT_${System.currentTimeMillis()}"
                                    )
                                    if (savedUri != null) {
                                        galleryViewModel.showMessage("Photo successfully saved to SultanGallery!")
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            // Main Canvas & Image Display Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07090E))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (state.showOriginal) state.originalBitmap else state.previewBitmap
                if (displayBitmap != null) {
                    EditorCanvasViewport(
                        bitmap = displayBitmap,
                        state = state,
                        onAddDrawPath = { editorViewModel.addDrawPath(it) },
                        onUpdateStickerPos = { id, x, y -> editorViewModel.updateTextStickerPosition(id, x, y) },
                        onRemoveSticker = { editorViewModel.removeTextSticker(it) }
                    )
                } else {
                    CircularProgressIndicator(color = SultanGold)
                }
            }

            // Bottom Tool Drawer & Settings
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Active Tool Controls Panel
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                        when (state.activeTab) {
                            EditorTab.ADJUST -> AdjustmentsPanel(state, editorViewModel)
                            EditorTab.FILTERS -> FiltersPanel(state, editorViewModel)
                            EditorTab.CROP -> SmartCropPanel(state, editorViewModel)
                            EditorTab.TRANSFORM -> TransformPanel(state, editorViewModel)
                            EditorTab.DRAW -> DrawPanel(state, editorViewModel)
                            EditorTab.TEXT -> TextPanel(
                                state = state,
                                onAddTextClick = { showTextDialog = true }
                            )
                            EditorTab.COLLAGE -> CollagePanel(
                                onOpenCollagePicker = { showCollagePicker = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Tool Tabs Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorTab.values().forEach { tab ->
                            val isSelected = state.activeTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (tab == EditorTab.COLLAGE && state.activeTab != EditorTab.COLLAGE) {
                                        editorViewModel.setActiveTab(tab)
                                    } else {
                                        editorViewModel.setActiveTab(tab)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    val icon = when (tab) {
                                        EditorTab.ADJUST -> Icons.Default.Tune
                                        EditorTab.FILTERS -> Icons.Default.Filter
                                        EditorTab.CROP -> Icons.Default.Crop
                                        EditorTab.TRANSFORM -> Icons.Default.RotateRight
                                        EditorTab.DRAW -> Icons.Default.Brush
                                        EditorTab.TEXT -> Icons.Default.TextFields
                                        EditorTab.COLLAGE -> Icons.Default.ViewQuilt
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) SultanGold else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SultanGold.copy(alpha = 0.25f),
                                    selectedLabelColor = SultanGold
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Text Sticker Dialog
        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                title = { Text("Add Text Sticker", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Write your caption...") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Text Color", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.YELLOW,
                                android.graphics.Color.RED,
                                android.graphics.Color.CYAN,
                                android.graphics.Color.GREEN,
                                android.graphics.Color.BLACK
                            ).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(c))
                                        .border(
                                            width = if (selectedTextColor == c) 3.dp else 1.dp,
                                            color = if (selectedTextColor == c) SultanGold else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedTextColor = c }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Badge Background", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                android.graphics.Color.BLACK,
                                android.graphics.Color.DKGRAY,
                                android.graphics.Color.BLUE,
                                android.graphics.Color.RED,
                                android.graphics.Color.TRANSPARENT
                            ).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (c == android.graphics.Color.TRANSPARENT) Color.Gray.copy(alpha = 0.3f) else Color(c))
                                        .border(
                                            width = if (selectedBgColor == c) 3.dp else 1.dp,
                                            color = if (selectedBgColor == c) SultanGold else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedBgColor = c }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                editorViewModel.addTextSticker(
                                    text = textInput,
                                    color = selectedTextColor,
                                    bgColor = selectedBgColor,
                                    fontSize = selectedFontSize
                                )
                                textInput = ""
                                showTextDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
                    ) {
                        Text("Add Sticker", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Integrated Collage Photo Picker Bottom Sheet
        if (showCollagePicker) {
            CollagePhotoPickerSheet(
                currentUri = imageUri,
                galleryItems = galleryState.allMedia.filter { !it.isVideo && !it.isAudio },
                onDismiss = { showCollagePicker = false },
                onCollageCreated = { uris, bg, pad, radius ->
                    showCollagePicker = false
                    editorViewModel.createCollageInEditor(
                        context = context,
                        uris = uris,
                        backgroundColor = bg,
                        padding = pad,
                        cornerRadius = radius
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorCanvasViewport(
    bitmap: Bitmap,
    state: EditorSessionState,
    onAddDrawPath: (DrawPath) -> Unit,
    onUpdateStickerPos: (Long, Float, Float) -> Unit,
    onRemoveSticker: (Long) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        val imageRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerRatio = containerWidth / containerHeight

        val displayWidth: Float
        val displayHeight: Float

        if (imageRatio > containerRatio) {
            displayWidth = containerWidth
            displayHeight = containerWidth / imageRatio
        } else {
            displayHeight = containerHeight
            displayWidth = containerHeight * imageRatio
        }

        val density = LocalDensity.current
        val displayWidthDp = with(density) { displayWidth.toDp() }
        val displayHeightDp = with(density) { displayHeight.toDp() }

        var currentNormalizedPoints by remember { mutableStateOf<List<DrawPoint>>(emptyList()) }

        Box(
            modifier = Modifier
                .size(displayWidthDp, displayHeightDp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            // Base Image Bitmap
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Editor Target",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // Drawing Vector Overlay Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.activeTab, state.currentDrawTool, state.drawColor, state.brushSize) {
                        if (state.activeTab == EditorTab.DRAW) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                    val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                    currentNormalizedPoints = listOf(DrawPoint(nx, ny))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                    currentNormalizedPoints = currentNormalizedPoints + DrawPoint(nx, ny)
                                },
                                onDragEnd = {
                                    if (currentNormalizedPoints.size >= 2) {
                                        onAddDrawPath(
                                            DrawPath(
                                                points = currentNormalizedPoints,
                                                color = state.drawColor,
                                                strokeWidth = state.brushSize,
                                                isHighlighter = state.currentDrawTool == DrawTool.HIGHLIGHTER,
                                                isEraser = state.currentDrawTool == DrawTool.ERASER
                                            )
                                        )
                                    }
                                    currentNormalizedPoints = emptyList()
                                },
                                onDragCancel = {
                                    currentNormalizedPoints = emptyList()
                                }
                            )
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                // Draw existing committed paths
                for (dp in state.paths) {
                    if (dp.points.size < 2) continue
                    val path = Path()
                    path.moveTo(dp.points[0].x * canvasW, dp.points[0].y * canvasH)
                    for (i in 1 until dp.points.size) {
                        path.lineTo(dp.points[i].x * canvasW, dp.points[i].y * canvasH)
                    }

                    val drawColor = if (dp.isEraser) Color(0xFF07090E) else Color(dp.color)
                    val alpha = if (dp.isHighlighter) 0.45f else 1.0f

                    drawPath(
                        path = path,
                        color = drawColor.copy(alpha = alpha),
                        style = Stroke(
                            width = dp.strokeWidth * (canvasW / 400f).coerceIn(0.6f, 2.5f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw currently active stroke
                if (currentNormalizedPoints.size >= 2) {
                    val activePath = Path()
                    activePath.moveTo(currentNormalizedPoints[0].x * canvasW, currentNormalizedPoints[0].y * canvasH)
                    for (i in 1 until currentNormalizedPoints.size) {
                        activePath.lineTo(currentNormalizedPoints[i].x * canvasW, currentNormalizedPoints[i].y * canvasH)
                    }

                    val isEraser = state.currentDrawTool == DrawTool.ERASER
                    val isHighlighter = state.currentDrawTool == DrawTool.HIGHLIGHTER
                    val activeColor = if (isEraser) Color(0xFF07090E) else Color(state.drawColor)
                    val alpha = if (isHighlighter) 0.45f else 1.0f

                    drawPath(
                        path = activePath,
                        color = activeColor.copy(alpha = alpha),
                        style = Stroke(
                            width = state.brushSize * (canvasW / 400f).coerceIn(0.6f, 2.5f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Draggable Text Stickers
            state.textStickers.forEach { sticker ->
                val stickerX = (sticker.normalizedX * displayWidth).roundToInt()
                val stickerY = (sticker.normalizedY * displayHeight).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(stickerX - 50, stickerY - 20) }
                        .pointerInput(sticker.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = ((stickerX + dragAmount.x) / displayWidth).coerceIn(0.05f, 0.95f)
                                val newY = ((stickerY + dragAmount.y) / displayHeight).coerceIn(0.05f, 0.95f)
                                onUpdateStickerPos(sticker.id, newX, newY)
                            }
                        }
                        .background(
                            color = Color(sticker.backgroundColor).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, SultanGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sticker.text,
                            color = Color(sticker.color),
                            fontSize = sticker.fontSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Sticker",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveSticker(sticker.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentsPanel(state: EditorSessionState, vm: PhotoEditorViewModel) {
    var activeAdjust by remember { mutableStateOf("Brightness") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Brightness", "Contrast", "Saturation", "Warmth", "Tint", "Vignette").forEach { adj ->
                FilterChip(
                    selected = activeAdjust == adj,
                    onClick = { activeAdjust = adj },
                    label = { Text(adj, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SultanGold.copy(alpha = 0.25f),
                        selectedLabelColor = SultanGold
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            IconButton(
                onClick = { vm.resetAdjustments() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeAdjust) {
            "Brightness" -> SliderRow(label = "Brightness", value = state.brightness, range = -60f..60f) { vm.updateAdjustment(brightness = it) }
            "Contrast" -> SliderRow(label = "Contrast", value = state.contrast, range = 0.5f..1.8f) { vm.updateAdjustment(contrast = it) }
            "Saturation" -> SliderRow(label = "Saturation", value = state.saturation, range = 0f..2.2f) { vm.updateAdjustment(saturation = it) }
            "Warmth" -> SliderRow(label = "Warmth", value = state.temperature, range = -40f..40f) { vm.updateAdjustment(temperature = it) }
            "Tint" -> SliderRow(label = "Tint", value = state.tint, range = -40f..40f) { vm.updateAdjustment(tint = it) }
            "Vignette" -> SliderRow(label = "Vignette", value = state.vignette, range = 0f..1f) { vm.updateAdjustment(vignette = it) }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = SultanGold, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(75.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SultanGold,
                activeTrackColor = SultanGold,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FiltersPanel(state: EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterType.values().forEach { filter ->
            val isSelected = state.activeFilter == filter
            Surface(
                onClick = { vm.setFilter(filter) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SultanGold else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null,
                modifier = Modifier.padding(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else SultanGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filter.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartCropPanel(state: EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CropPreset.values().forEach { preset ->
            val isSelected = state.selectedCropPreset == preset
            FilterChip(
                selected = isSelected,
                onClick = { vm.applyCropPreset(preset) },
                label = { Text(preset.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = if (isSelected) SultanGold else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SultanGold.copy(alpha = 0.25f),
                    selectedLabelColor = SultanGold
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun TransformPanel(state: EditorSessionState, vm: PhotoEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { vm.rotate90(clockwise = false) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.RotateLeft, contentDescription = null, tint = SultanGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Left 90°", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }

        Button(
            onClick = { vm.rotate90(clockwise = true) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.RotateRight, contentDescription = null, tint = SultanGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Right 90°", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }

        Button(
            onClick = { vm.toggleFlipHorizontal() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Flip, contentDescription = null, tint = SultanGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Flip H", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }

        Button(
            onClick = { vm.toggleFlipVertical() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Flip, contentDescription = null, tint = SultanGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Flip V", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DrawPanel(state: EditorSessionState, vm: PhotoEditorViewModel) {
    val colors = listOf(
        android.graphics.Color.RED,
        android.graphics.Color.YELLOW,
        android.graphics.Color.GREEN,
        android.graphics.Color.CYAN,
        android.graphics.Color.BLUE,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK,
        android.graphics.Color.parseColor("#E5A93C") // Sultan Gold
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair(DrawTool.BRUSH, "Brush"),
                    Pair(DrawTool.HIGHLIGHTER, "Highlighter"),
                    Pair(DrawTool.ERASER, "Eraser")
                ).forEach { (tool, label) ->
                    FilterChip(
                        selected = state.currentDrawTool == tool,
                        onClick = { vm.setDrawTool(tool) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SultanGold.copy(alpha = 0.25f),
                            selectedLabelColor = SultanGold
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            if (state.paths.isNotEmpty()) {
                TextButton(onClick = { vm.clearDrawPaths() }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Clear", color = Color.Red, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Color palette & Brush Size
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
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

            Spacer(modifier = Modifier.width(8.dp))

            // Size toggle fine / thick
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(8f, 14f, 24f, 40f).forEach { size ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (state.brushSize == size) SultanGold else Color.DarkGray)
                            .clickable { vm.setBrushSize(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((size / 4f).dp.coerceIn(3.dp, 12.dp))
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextPanel(
    state: EditorSessionState,
    onAddTextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${state.textStickers.size} text sticker(s) placed",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Drag stickers on photo to reposition or tap × to remove",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Button(
            onClick = onAddTextClick,
            colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Text", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CollagePanel(
    onOpenCollagePicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Photo Collage Studio",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SultanGold
            )
            Text(
                text = "Combine 2 to 9 photos from your gallery into a stunning collage layout!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onOpenCollagePicker,
            colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pick Photos", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollagePhotoPickerSheet(
    currentUri: Uri,
    galleryItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onCollageCreated: (List<Uri>, Int, Int, Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedUris = remember { mutableStateListOf<Uri>(currentUri) }
    var selectedBgColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }
    var paddingVal by remember { mutableIntStateOf(16) }
    var cornerRadiusVal by remember { mutableFloatStateOf(12f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Create Photo Collage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${selectedUris.size} of 9 photos selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = SultanGold
                    )
                }
                Button(
                    onClick = {
                        if (selectedUris.size >= 2) {
                            onCollageCreated(selectedUris.toList(), selectedBgColor, paddingVal, cornerRadiusVal)
                        }
                    },
                    enabled = selectedUris.size >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = SultanGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Collage", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collage Background & Spacing Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Canvas:", fontSize = 11.sp, color = Color.LightGray)
                    listOf(
                        android.graphics.Color.BLACK,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.parseColor("#E5A93C"),
                        android.graphics.Color.DKGRAY
                    ).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selectedBgColor == c) 2.dp else 1.dp,
                                    color = if (selectedBgColor == c) SultanGold else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedBgColor = c }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gap:", fontSize = 11.sp, color = Color.LightGray)
                    listOf(8, 16, 24).forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (paddingVal == p) SultanGold else Color.DarkGray)
                                .clickable { paddingVal = p }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${p}px",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paddingVal == p) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Photo Grid Selector
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(galleryItems, key = { it.id }) { item ->
                    val isPicked = selectedUris.contains(item.uri)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isPicked) 3.dp else 1.dp,
                                color = if (isPicked) SultanGold else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (isPicked) {
                                    if (selectedUris.size > 1) selectedUris.remove(item.uri)
                                } else {
                                    if (selectedUris.size < 9) selectedUris.add(item.uri)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isPicked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(SultanGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val idx = selectedUris.indexOf(item.uri) + 1
                                Text(
                                    text = "$idx",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
