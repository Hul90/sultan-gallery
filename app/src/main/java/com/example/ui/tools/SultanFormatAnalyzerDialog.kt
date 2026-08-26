package com.example.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MediaItem
import com.example.tools.FormatCategory
import com.example.tools.SultanFormatDetector
import com.example.tools.SultanFormatInfo
import com.example.ui.theme.SultanGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SultanFormatAnalyzerDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onNavigateToEditor: (() -> Unit)? = null,
    onNavigateToConverter: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var formatInfo by remember { mutableStateOf<SultanFormatInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item) {
        isLoading = true
        formatInfo = SultanFormatDetector.analyzeMedia(context, item)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SultanGold.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = null,
                                tint = SultanGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sultan Format Analyzer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Deep Media Engine Inspection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading || formatInfo == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SultanGold)
                    }
                } else {
                    val info = formatInfo!!

                    // Main Format Hero Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = info.formatName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SultanGold,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(SultanGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = info.shortBadge,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = info.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Specs Grid
                    Text(
                        text = "TECHNICAL SPECIFICATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SultanGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AnalyzerInfoRow("MIME Type", info.mimeType)
                    AnalyzerInfoRow("Container", info.container)
                    AnalyzerInfoRow("Codec / Stream", info.codec)
                    AnalyzerInfoRow("Compression", info.compression)

                    if (info.width > 0 && info.height > 0) {
                        val megapixels = String.format("%.1f MP", (info.width.toLong() * info.height) / 1_000_000.0)
                        AnalyzerInfoRow("Resolution", "${info.width} × ${info.height} ($megapixels)")
                    }

                    AnalyzerInfoRow("Color Profile", info.colorSpace)
                    AnalyzerInfoRow("Bit Depth", info.bitDepth)
                    AnalyzerInfoRow("File Size", item.formattedSize)

                    if (info.estimatedUncompressedRam > 0) {
                        val ramMb = String.format("%.2f MB", info.estimatedUncompressedRam / (1024.0 * 1024.0))
                        AnalyzerInfoRow("Est. RAM Footprint", ramMb)
                    }

                    if (info.magicSignatureHex.isNotEmpty()) {
                        AnalyzerInfoRow("Magic Bytes", info.magicSignatureHex, isCode = true)
                    }

                    // Camera RAW Optics section if available
                    if (info.rawMetadata != null && (info.rawMetadata.make.isNotEmpty() || info.rawMetadata.model.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CAMERA & OPTICS METADATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SultanGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val raw = info.rawMetadata
                        if (raw.make.isNotEmpty() || raw.model.isNotEmpty()) {
                            AnalyzerInfoRow("Camera", "${raw.make} ${raw.model}".trim())
                        }
                        if (raw.lensModel.isNotEmpty()) AnalyzerInfoRow("Lens", raw.lensModel)
                        if (raw.fNumber.isNotEmpty()) AnalyzerInfoRow("Aperture", raw.fNumber)
                        if (raw.exposureTime.isNotEmpty()) AnalyzerInfoRow("Shutter Speed", raw.exposureTime)
                        if (raw.iso.isNotEmpty()) AnalyzerInfoRow("ISO Sensitivity", raw.iso)
                        if (raw.focalLength.isNotEmpty()) AnalyzerInfoRow("Focal Length", raw.focalLength)
                        if (raw.whiteBalance.isNotEmpty()) AnalyzerInfoRow("White Balance", raw.whiteBalance)
                        if (raw.gpsCoordinates.isNotEmpty()) AnalyzerInfoRow("GPS Location", raw.gpsCoordinates)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Capability Pills
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (info.isDirectlyEditable) {
                            FormatCapabilityPill("Direct Photo Editing", Color(0xFF4CAF50))
                        } else {
                            FormatCapabilityPill("Edit as Copy (Convert)", Color(0xFFFFA000))
                        }
                        if (info.isHDR) FormatCapabilityPill("High Dynamic Range (HDR)", SultanGold)
                        if (info.hasAlpha) FormatCapabilityPill("Alpha Transparency", Color(0xFF29B6F6))
                        if (info.isAnimated) FormatCapabilityPill("Frame Animation", Color(0xFFAB47BC))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onNavigateToConverter != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onNavigateToConverter()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Convert")
                            }
                        }

                        if (onNavigateToEditor != null && info.isDirectlyEditable) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onNavigateToEditor()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SultanGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Photo", fontWeight = FontWeight.Bold)
                            }
                        } else if (onShare != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onShare()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SultanGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Media", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzerInfoRow(label: String, value: String, isCode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isCode) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
}

@Composable
private fun FormatCapabilityPill(title: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
