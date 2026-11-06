package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaMetadataDao {
    @Query("SELECT * FROM media_metadata")
    fun getAllMetadata(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media_metadata WHERE uriString = :uriString LIMIT 1")
    suspend fun getMetadata(uriString: String): MediaMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: MediaMetadataEntity)

    @Query("UPDATE media_metadata SET isFavorite = :isFavorite WHERE uriString = :uriString")
    suspend fun setFavorite(uriString: String, isFavorite: Boolean)

    @Query("UPDATE media_metadata SET isTrashed = :isTrashed, trashedTimestampMs = :timestamp WHERE uriString = :uriString")
    suspend fun setTrashed(uriString: String, isTrashed: Boolean, timestamp: Long)

    @Query("DELETE FROM media_metadata WHERE uriString = :uriString")
    suspend fun deleteMetadata(uriString: String)
}
