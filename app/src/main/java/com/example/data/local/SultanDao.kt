package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SultanDao {

    // Favorites
    @Query("SELECT uriString FROM favorites")
    fun getAllFavoriteUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uriString = :uriString")
    suspend fun removeFavorite(uriString: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uriString = :uriString)")
    suspend fun isFavorite(uriString: String): Boolean

    // Trash
    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrash(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(item: TrashEntity)

    @Query("DELETE FROM trash_items WHERE uriString = :uriString")
    suspend fun deleteTrashItem(uriString: String)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()

    // Vault
    @Query("SELECT * FROM vault_items ORDER BY addedTimestamp DESC")
    fun getAllVaultItems(): Flow<List<VaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultEntity): Long

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: Long)

    // Custom Albums
    @Query("SELECT * FROM custom_albums ORDER BY name ASC")
    fun getAllCustomAlbums(): Flow<List<CustomAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomAlbum(album: CustomAlbumEntity): Long

    @Query("DELETE FROM custom_albums WHERE id = :id")
    suspend fun deleteCustomAlbum(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaToAlbum(crossRef: CustomAlbumMediaEntity)

    @Query("SELECT mediaUri FROM custom_album_media WHERE albumId = :albumId")
    fun getMediaForAlbum(albumId: Long): Flow<List<String>>
}
