package com.example.musictinder.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.musictinder.MusicTinderApplication
import com.example.musictinder.TinderViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicTinderApplication
            TinderViewModel(
                application = app,
                dao = app.container.songDao,
                preferences = app.container.preferences,
                scanner = app.container.scanner
            )
        }
    }
}