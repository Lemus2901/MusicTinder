package com.example.musictinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val songUri: String,
    val category: String,
    val decidedAt: Long = System.currentTimeMillis()
)