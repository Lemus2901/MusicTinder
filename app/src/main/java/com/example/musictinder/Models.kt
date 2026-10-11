package com.example.musictinder

import com.example.musictinder.data.local.SongEntity

enum class Category { REJECT, LIKE, SUPERLIKE }

data class Track(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long
)

fun SongEntity.toTrack(): Track = Track(
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs
)

fun String.toCategoryOrNull(): Category? = runCatching { Category.valueOf(this) }.getOrNull()

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}