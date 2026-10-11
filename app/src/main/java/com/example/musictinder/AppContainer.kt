package com.example.musictinder

import android.content.Context
import com.example.musictinder.audio.PlaybackController
import com.example.musictinder.data.local.AppDatabase
import com.example.musictinder.data.local.SongDao
import com.example.musictinder.data.preferences.FolderPreferences
import com.example.musictinder.media.MusicScanner

/**
 * DI manual ligero: no usa Hilt/Koin, así el proyecto compila sin pasos extra
 * y 100% gratis con dependencias de código abierto.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.getDatabase(appContext)
    val songDao: SongDao = database.songDao()
    val preferences: FolderPreferences = FolderPreferences(appContext)
    val scanner: MusicScanner = MusicScanner()
    val playback: PlaybackController = PlaybackController(appContext)
}