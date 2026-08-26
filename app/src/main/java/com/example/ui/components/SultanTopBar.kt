package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GridMode
import com.example.data.model.SortOrder
import com.example.ui.theme.SultanGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SultanTopBar(
    title: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    selectedSizeStr: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShareSelected: () -> Unit,
    onTrashSelected: () -> Unit,
    onVaultSelected: () -> Unit,
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    currentGridMode: GridMode,
    onGridModeChange: (GridMode) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            // Multi-Select Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$selectedCount selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SultanGold
                        )
                        if (selectedSizeStr.isNotBlank()) {
                            Text(
                                text = selectedSizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }
                    IconButton(onClick = onShareSelected) {
                        Icon(Icons.Default.Share, contentDescription = "Share Selected")
                    }
                    IconButton(onClick = onVaultSelected) {
                        Icon(Icons.Default.Lock, contentDescription = "Move to Vault")
                    }
                    IconButton(onClick = onTrashSelected) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        } else {
            // Normal Top Bar
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search photos, videos, albums...") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SULTAN GALLERY",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp,
                                color = SultanGold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = { onToggleSearch(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { onToggleSearch(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Media")
                        }

                        IconButton(onClick = onNavigateToTools) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Sultan Tools",
                                tint = SultanGold
                            )
                        }

                        // Sort Menu
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                fontWeight = if (order == currentSortOrder) FontWeight.Bold else FontWeight.Normal,
                                                color = if (order == currentSortOrder) SultanGold else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onSortOrderChange(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Grid Menu
                        Box {
                            IconButton(onClick = { showGridMenu = true }) {
                                Icon(
                                    if (currentGridMode == GridMode.LIST) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Grid layout"
                                )
                            }
                            DropdownMenu(
                                expanded = showGridMenu,
                                onDismissRequest = { showGridMenu = false }
                            ) {
                                GridMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.label,
                                                fontWeight = if (mode == currentGridMode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (mode == currentGridMode) SultanGold else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onGridModeChange(mode)
                                            showGridMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}
