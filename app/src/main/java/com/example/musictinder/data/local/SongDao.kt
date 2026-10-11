package com.example.musictinder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs")
    fun observeSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE folder = :folder")
    suspend fun deleteSongsByFolder(folder: String)

    @Query("DELETE FROM songs WHERE uri = :uri")
    suspend fun deleteSong(uri: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT * FROM decisions")
    fun observeDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecision(decision: DecisionEntity)

    @Query("DELETE FROM decisions WHERE songUri = :uri")
    suspend fun deleteDecision(uri: String)

    @Query("DELETE FROM decisions WHERE songUri NOT IN (SELECT uri FROM songs)")
    suspend fun pruneOrphanDecisions()

    @Query("DELETE FROM decisions WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("DELETE FROM decisions")
    suspend fun clearAllDecisions()
}