package com.example.ui.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.firstOrNull
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.VaultEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SultanGold
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretVaultScreen(
    galleryViewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by galleryViewModel.uiState.collectAsStateWithLifecycle()
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val storedPinHash by galleryViewModel.preferences.vaultPin.collectAsStateWithLifecycle(initialValue = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SultanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SECRET VAULT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isVaultUnlocked) {
                        IconButton(onClick = { galleryViewModel.lockVault() }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = SultanGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (!state.isVaultUnlocked && storedPinHash.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SultanGold, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(18.dp))
                Text("Set Up Secret Vault", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Create a 4-digit PIN in Settings before using the encrypted vault.", color = Color.LightGray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = SultanGold, contentColor = Color.Black)
                ) { Text("Set Vault PIN", fontWeight = FontWeight.Bold) }
            }
        } else if (!state.isVaultUnlocked) {
            // PIN Entry Keypad
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SultanGold,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter Vault PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (pinError) "Incorrect PIN" else "Enter your 4-digit Vault PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pinError) MaterialTheme.colorScheme.error else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = enteredPin.length > i
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) SultanGold else Color.DarkGray)
                                .border(1.dp, if (isFilled) SultanGold else Color.Gray, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Number Pad (1-9, 0, Backspace)
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    keypad.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.4f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (digit.isNotEmpty()) DarkSurface else Color.Transparent)
                                        .clickable(enabled = digit.isNotEmpty()) {
                                            if (digit == "DEL") {
                                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                pinError = false
                                            } else {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + digit
                                                    enteredPin = newPin
                                                    if (newPin.length == 4) {
                                                        galleryViewModel.unlockVault(newPin) { success ->
                                                            if (!success) {
                                                                pinError = true
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit == "DEL") {
                                        Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = SultanGold)
                                    } else if (digit.isNotEmpty()) {
                                        Text(text = digit, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Unlocked Vault Media Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                if (state.vaultList.isEmpty()) {
                    EmptyStateView(
                        title = "Secret Vault is Empty",
                        subtitle = "Select any photo or video in the gallery and tap 'Move to Vault' to hide it here with encryption.",
                        icon = Icons.Default.LockOpen
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.vaultList, key = { it.id }) { vaultItem ->
                            VaultMediaThumbnail(
                                vaultEntity = vaultItem,
                                onRestore = { galleryViewModel.restoreVaultItem(vaultItem) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultMediaThumbnail(
    vaultEntity: VaultEntity,
    onRestore: () -> Unit
) {
    val file = remember(vaultEntity.encryptedPath) { File(vaultEntity.encryptedPath) }
    val decryptedBitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(initialValue = null, key1 = vaultEntity.encryptedPath) {
        value = com.example.data.vault.SultanVaultCryptoEngine.decryptFileToBitmap(file)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface),
        contentAlignment = Alignment.Center
    ) {
        if (decryptedBitmap != null) {
            Image(
                bitmap = decryptedBitmap!!.asImageBitmap(),
                contentDescription = vaultEntity.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (vaultEntity.isVideo) Icons.Default.Security else Icons.Default.Lock,
                contentDescription = null,
                tint = SultanGold.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vaultEntity.displayName,
                maxLines = 1,
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restore to Gallery", tint = SultanGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}
