package com.example.ui.gallery

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SultanDatabase
import com.example.data.local.TrashEntity
import com.example.data.local.VaultEntity
import com.example.data.model.GridMode
import com.example.data.model.MediaAlbum
import com.example.data.model.MediaItem
import com.example.data.model.MediaTab
import com.example.data.model.SortOrder
import com.example.data.preferences.SultanPreferences
import com.example.data.repository.MediaStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val albums: List<MediaAlbum> = emptyList(),
    val trashList: List<TrashEntity> = emptyList(),
    val vaultList: List<VaultEntity> = emptyList(),
    val currentTab: MediaTab = MediaTab.ALL,
    val formatFilter: com.example.data.model.FormatFilter = com.example.data.model.FormatFilter.ALL,
    val selectedAlbum: MediaAlbum? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val gridMode: GridMode = GridMode.NORMAL,
    val isVaultUnlocked: Boolean = false,
    val isAmoled: Boolean = true,
    val isDark: Boolean = true,
    val showAudio: Boolean = true,
    val userMessage: String? = null,
    val toolSelectionIds: Set<Long> = emptySet()
) {
    val selectedItems: List<MediaItem>
        get() = allMedia.filter { selectedItemIds.contains(it.id) }

    val selectedTotalSize: Long
        get() = selectedItems.sumOf { it.size }
}

private data class GalleryPreferenceSnapshot(
    val grid: GridMode,
    val sort: SortOrder,
    val amoled: Boolean,
    val dark: Boolean,
    val audio: Boolean
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SultanDatabase.getDatabase(application)
    val repository = MediaStoreRepository(application, db.dao())
    val preferences = SultanPreferences(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private var mediaRefreshJob: Job? = null
    private var refreshInProgress = false

    init {
        observePreferences()
        observeTrashAndVault()
        repository.startMediaObserver {
            mediaRefreshJob?.cancel()
            mediaRefreshJob = viewModelScope.launch {
                delay(650)
                refreshMedia()
            }
        }
        refreshMedia()
    }

    override fun onCleared() {
        repository.stopMediaObserver()
        mediaRefreshJob?.cancel()
        super.onCleared()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferences.gridMode,
                preferences.sortOrder,
                preferences.isAmoledMode,
                preferences.isDarkTheme,
                preferences.showAudio
            ) { grid, sort, amoled, dark, audio ->
                GalleryPreferenceSnapshot(grid, sort, amoled, dark, audio)
            }.combine(preferences.formatFilter) { snapshot, format ->
                _uiState.update {
                    it.copy(
                        gridMode = snapshot.grid,
                        sortOrder = snapshot.sort,
                        isAmoled = snapshot.amoled,
                        isDark = snapshot.dark,
                        showAudio = snapshot.audio,
                        formatFilter = format
                    )
                }
                applyFilterAndSort()
            }.collect {}
        }
    }

    private fun observeTrashAndVault() {
        viewModelScope.launch {
            repository.trashEntities.collect { trash ->
                _uiState.update { it.copy(trashList = trash) }
            }
        }
        viewModelScope.launch {
            repository.vaultEntities.collect { vault ->
                _uiState.update { it.copy(vaultList = vault) }
            }
        }
    }

    fun refreshMedia(forceFullScan: Boolean = false) {
        if (refreshInProgress && !forceFullScan) return
        refreshInProgress = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val includeAudio = _uiState.value.showAudio
                val cached = repository.loadCachedMedia(includeAudio)

                // Fast path: paint the last successful scan immediately.
                if (cached != null) {
                    val albums = repository.extractAlbums(cached)
                    _uiState.update {
                        it.copy(isLoading = false, allMedia = cached, albums = albums)
                    }
                    applyFilterAndSort()
                } else {
                    _uiState.update { it.copy(isLoading = true) }
                }

                // Never re-query thousands of rows unless MediaStore actually changed.
                val changed = forceFullScan || cached == null || repository.hasMediaStoreChanged(includeAudio)
                if (!changed) return@launch

                val media = repository.loadAllMedia(includeAudio = includeAudio)
                val albums = repository.extractAlbums(media)
                _uiState.update {
                    it.copy(isLoading = false, allMedia = media, albums = albums)
                }
                applyFilterAndSort()
            } finally {
                refreshInProgress = false
            }
        }
    }

    fun prepareToolSelection() {
        _uiState.update { it.copy(toolSelectionIds = it.selectedItemIds) }
    }

    fun clearToolSelection() {
        _uiState.update { it.copy(toolSelectionIds = emptySet()) }
    }

    fun selectTab(tab: MediaTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                selectedAlbum = null,
                isSelectionMode = false,
                selectedItemIds = emptySet()
            )
        }
        applyFilterAndSort()
    }

    fun selectFormatFilter(filter: com.example.data.model.FormatFilter) {
        _uiState.update { it.copy(formatFilter = filter) }
        viewModelScope.launch { preferences.setFormatFilter(filter) }
        applyFilterAndSort()
    }

    fun selectAlbum(album: MediaAlbum?) {
        _uiState.update { it.copy(selectedAlbum = album) }
        applyFilterAndSort()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
        applyFilterAndSort()
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferences.setSortOrder(order)
            _uiState.update { it.copy(sortOrder = order) }
            applyFilterAndSort()
        }
    }

    fun setGridMode(mode: GridMode) {
        viewModelScope.launch {
            preferences.setGridMode(mode)
            _uiState.update { it.copy(gridMode = mode) }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val newStatus = repository.toggleFavorite(item)
            val updated = _uiState.value.allMedia.map {
                if (it.id == item.id) it.copy(isFavorite = newStatus) else it
            }
            _uiState.update { it.copy(allMedia = updated) }
            applyFilterAndSort()
        }
    }

    fun toggleSelection(itemId: Long) {
        _uiState.update { state ->
            val current = state.selectedItemIds.toMutableSet()
            if (current.contains(itemId)) {
                current.remove(itemId)
            } else {
                current.add(itemId)
            }
            state.copy(
                selectedItemIds = current,
                isSelectionMode = current.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredMedia.map { it.id }.toSet()
            state.copy(
                selectedItemIds = allIds,
                isSelectionMode = allIds.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun batchTrashSelected() {
        val selected = _uiState.value.selectedItems
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { repository.moveToTrash(it) }
            clearSelection()
            refreshMedia()
            showMessage("${selected.size} items moved to Trash")
        }
    }

    fun trashMediaItem(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            refreshMedia()
            showMessage("Moved to Trash")
        }
    }

    fun batchVaultSelected() {
        val selected = _uiState.value.selectedItems
        if (selected.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            selected.forEach {
                if (repository.moveToVault(it)) count++
            }
            clearSelection()
            refreshMedia()
            showMessage("$count items moved to Secret Vault")
        }
    }

    fun batchShareSelected() {
        val selected = _uiState.value.selectedItems
        if (selected.isEmpty()) return
        repository.shareMultipleMedia(selected.map { it.uri })
    }

    fun restoreTrashItem(trashEntity: TrashEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(trashEntity)
            refreshMedia()
            showMessage("Item restored")
        }
    }

    fun permanentlyDeleteTrashItem(
        trashEntity: TrashEntity,
        onDeleteRequest: (IntentSender, List<String>) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.permanentlyDeleteTrash(trashEntity)
            if (result.intentSender != null) {
                onDeleteRequest(result.intentSender, result.pendingUriStrings)
            } else if (result.deleted) {
                showMessage("Permanently deleted")
            } else {
                showMessage("Could not permanently delete this item")
            }
        }
    }

    fun emptyTrash(
        onDeleteRequest: (IntentSender, List<String>) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.emptyTrash()
            if (result.intentSender != null) {
                onDeleteRequest(result.intentSender, result.pendingUriStrings)
            } else if (result.deleted) {
                showMessage("Trash cleared")
            } else {
                showMessage("Some items could not be permanently deleted")
            }
        }
    }

    fun finalizePendingTrashDeletion(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        viewModelScope.launch {
            repository.finalizeTrashDeletion(uriStrings)
            showMessage("Permanently deleted")
        }
    }

    fun restoreVaultItem(vaultEntity: VaultEntity) {
        viewModelScope.launch {
            repository.restoreFromVault(vaultEntity)
            refreshMedia()
            showMessage("Restored from Vault")
        }
    }

    fun unlockVault(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val storedHash = preferences.vaultPin.firstOrNull() ?: ""
            val success = com.example.data.vault.SultanVaultCryptoEngine.verifyPin(pin, storedHash)
            _uiState.update { it.copy(isVaultUnlocked = success) }
            onResult(success)
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isVaultUnlocked = false) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(userMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun applyFilterAndSort() {
        val state = _uiState.value
        var list = state.allMedia

        // Filter by Tab / Album
        if (state.selectedAlbum != null) {
            val albumName = state.selectedAlbum.name
            list = list.filter {
                if (albumName == "Audio") it.isAudio
                else it.bucketName.equals(albumName, ignoreCase = true)
            }
        } else {
            list = when (state.currentTab) {
                MediaTab.ALL -> list
                MediaTab.RECENT -> list.take(150)
                MediaTab.PHOTOS -> list.filter { !it.isVideo && !it.isAudio }
                MediaTab.VIDEOS -> list.filter { it.isVideo }
                MediaTab.AUDIO -> list.filter { it.isAudio }
                MediaTab.FAVORITES -> list.filter { it.isFavorite }
                MediaTab.SCREENSHOTS -> list.filter {
                    val name = it.displayName.lowercase()
                    val bucket = it.bucketName.lowercase()
                    name.contains("screenshot") || bucket.contains("screenshot")
                }
                MediaTab.DOWNLOADS -> list.filter {
                    val bucket = it.bucketName.lowercase()
                    val path = it.path.lowercase()
                    bucket.contains("download") || path.contains("download")
                }
                MediaTab.CAMERA -> list.filter {
                    val bucket = it.bucketName.lowercase()
                    bucket.contains("camera") || bucket.contains("dcim")
                }
                MediaTab.WHATSAPP -> list.filter {
                    it.bucketName.lowercase().contains("whatsapp") || it.path.lowercase().contains("whatsapp")
                }
                MediaTab.TELEGRAM -> list.filter {
                    it.bucketName.lowercase().contains("telegram") || it.path.lowercase().contains("telegram")
                }
                MediaTab.ALBUMS, MediaTab.TRASH, MediaTab.VAULT -> list
            }
        }

        // Filter by Format
        if (state.formatFilter != com.example.data.model.FormatFilter.ALL) {
            val allowedExtensions = state.formatFilter.extensions.map { it.lowercase() }
            list = list.filter { item ->
                val ext = item.displayName.substringAfterLast('.', "").lowercase()
                allowedExtensions.contains(ext) || allowedExtensions.any { item.mimeType.lowercase().contains(it) }
            }
        }

        // Filter by Search
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            list = list.filter { item ->
                item.displayName.lowercase().contains(q) ||
                item.bucketName.lowercase().contains(q) ||
                item.mimeType.lowercase().contains(q) ||
                (q == "video" && item.isVideo) ||
                (q == "photo" && !item.isVideo && !item.isAudio) ||
                (q == "audio" && item.isAudio) ||
                (q == "favorite" && item.isFavorite) ||
                (q == "large" && item.size > 10 * 1024 * 1024)
            }
        }

        // Apply Sorting
        list = when (state.sortOrder) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> list.sortedBy { it.dateAdded }
            SortOrder.NAME_ASC -> list.sortedBy { it.displayName.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.displayName.lowercase() }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> list.sortedBy { it.size }
        }

        _uiState.update { it.copy(filteredMedia = list) }
    }
}
