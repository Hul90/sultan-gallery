package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.theme.SultanGold
import com.example.ui.tools.SultanFormatAnalyzerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaDetailsDialog(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var showFormatAnalyzer by remember { mutableStateOf(false) }

    if (showFormatAnalyzer) {
        SultanFormatAnalyzerDialog(
            item = item,
            onDismiss = { showFormatAnalyzer = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SultanGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Media Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailRow("Name", item.displayName)
                DetailRow("Type", item.mimeType)
                DetailRow("Size", item.formattedSize)
                if (item.width > 0 && item.height > 0) {
                    DetailRow("Resolution", "${item.width} × ${item.height}")
                }
                if (item.durationMs > 0) {
                    DetailRow("Duration", item.formattedDuration)
                }
                DetailRow("Folder", item.bucketName.ifBlank { "Root" })
                DetailRow("Date Added", dateFormat.format(Date(item.dateAdded)))
                if (item.dateModified > 0) {
                    DetailRow("Date Modified", dateFormat.format(Date(item.dateModified)))
                }
                if (item.path.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Path", style = MaterialTheme.typography.labelSmall, color = SultanGold)
                            Text(item.path, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = {
                            val clip = ClipData.newPlainText("File Path", item.path)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(clip)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Path", tint = SultanGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showFormatAnalyzer = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DataObject, contentDescription = null, tint = SultanGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sultan Format Analyzer", fontWeight = FontWeight.SemiBold, color = SultanGold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SultanGold)
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SultanGold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
