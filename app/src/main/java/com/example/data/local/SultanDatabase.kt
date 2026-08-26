package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        TrashEntity::class,
        VaultEntity::class,
        CustomAlbumEntity::class,
        CustomAlbumMediaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SultanDatabase : RoomDatabase() {
    abstract fun dao(): SultanDao

    companion object {
        @Volatile
        private var INSTANCE: SultanDatabase? = null

        fun getDatabase(context: Context): SultanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SultanDatabase::class.java,
                    "sultan_gallery_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
