package com.example.musictinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val folder: String,
    val addedAt: Long = System.currentTimeMillis()
)