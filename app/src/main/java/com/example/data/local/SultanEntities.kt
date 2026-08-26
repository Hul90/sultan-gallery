package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val uriString: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey val uriString: String,
    val originalPath: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val trashFilePath: String = "",
    val deletedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val originalUri: String,
    val encryptedPath: String,
    val mimeType: String,
    val size: Long,
    val isVideo: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_album_media", primaryKeys = ["albumId", "mediaUri"])
data class CustomAlbumMediaEntity(
    val albumId: Long,
    val mediaUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
