package com.example.musictinder.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "music_tinder_settings")

class FolderPreferences(private val context: Context) {

    private val folderKey = stringPreferencesKey("selected_folder_uri")

    val folderUri: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[folderKey] }
        .distinctUntilChanged()

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[folderKey] = uri
        }
    }
}