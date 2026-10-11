package com.example.musictinder

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musictinder.data.local.DecisionEntity
import com.example.musictinder.data.local.SongDao
import com.example.musictinder.data.local.SongEntity
import com.example.musictinder.data.preferences.FolderPreferences
import com.example.musictinder.media.MusicScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TinderUiState(
    val songs: List<Track> = emptyList(),
    val decisions: Map<String, Category> = emptyMap(),
    val deckOrder: List<String> = emptyList(),
    val deckIndex: Int = 0,
    val likedCount: Int = 0,
    val rejectedCount: Int = 0,
    val superlikedCount: Int = 0
) {
    /** Canción que se muestra/reproduce ahora. */
    val current: Track?
        get() {
            val uri = deckOrder.getOrNull(deckIndex) ?: return null
            return songs.firstOrNull { it.uri == uri }
        }

    val next: Track?
        get() {
            val uri = deckOrder.getOrNull(deckIndex + 1) ?: return null
            return songs.firstOrNull { it.uri == uri }
        }
}

class TinderViewModel(
    application: Application,
    private val dao: SongDao,
    private val preferences: FolderPreferences,
    private val scanner: MusicScanner
) : AndroidViewModel(application) {

    private val _folderUri = MutableStateFlow<String?>(null)
    val folderUri: StateFlow<String?> = _folderUri.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _deckOrder = MutableStateFlow<List<String>>(emptyList())
    private val _deckIndex = MutableStateFlow(0)

    val uiState: StateFlow<TinderUiState> = combine(
        dao.observeSongs(),
        dao.observeDecisions(),
        _deckOrder,
        _deckIndex
    ) { songs, decisions, order, index ->
        val tracks = songs.map { it.toTrack() }
        val decisionsMap = buildMap {
            decisions.forEach { d ->
                val cat = d.category.toCategoryOrNull()
                if (cat != null) put(d.songUri, cat)
            }
        }
        TinderUiState(
            songs = tracks,
            decisions = decisionsMap,
            deckOrder = order,
            deckIndex = index,
            likedCount = decisionsMap.values.count { it == Category.LIKE },
            rejectedCount = decisionsMap.values.count { it == Category.REJECT },
            superlikedCount = decisionsMap.values.count { it == Category.SUPERLIKE }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TinderUiState())

    init {
        // Al arrancar: si ya hay una carpeta guardada se re-escanea para
        // recoger archivos nuevos, conservando las decisiones.
        viewModelScope.launch {
            preferences.folderUri.collect { uri ->
                if (uri != null && _folderUri.value != uri) {
                    _folderUri.value = uri
                    selectFolderInternal(Uri.parse(uri), sameFolder = true)
                }
            }
        }

        // Reconciliador del mazo: mantiene el orden estable y vuelve a
        // introducir canciones revividas desde Superlikes.
        viewModelScope.launch {
            combine(dao.observeSongs(), dao.observeDecisions()) { songs, decisions ->
                val songUris = songs.map { it.uri }.toSet()
                val superLiked = decisions
                    .filter { it.category == Category.SUPERLIKE.name }
                    .map { it.songUri }
                    .toSet()
                songUris to superLiked
            }.collect { (songUris, superLiked) ->
                val valid = _deckOrder.value.filter { it in songUris && it !in superLiked }
                val missing = (songUris - superLiked) - valid.toSet()
                val next = if (missing.isNotEmpty()) valid + missing.shuffled() else valid
                if (next != _deckOrder.value) _deckOrder.value = next
                if (_deckIndex.value > _deckOrder.value.lastIndex && _deckOrder.value.isNotEmpty()) {
                    _deckIndex.value = 0
                }
            }
        }
    }

    /** Selecciona una carpeta con el selector del sistema y arma el mazo. */
    fun selectFolder(uri: Uri) {
        viewModelScope.launch {
            if (_isScanning.value) return@launch
            val uriString = uri.toString()
            val sameFolder = _folderUri.value == uriString
            _folderUri.value = uriString
            preferences.setFolderUri(uriString)
            selectFolderInternal(uri, sameFolder)
        }
    }

    private suspend fun selectFolderInternal(uri: Uri, sameFolder: Boolean) {
        _isScanning.value = true
        _scanMessage.value = null
        try {
            val tracks = scanner.scan(getApplication(), uri)

            if (sameFolder) {
                dao.deleteSongsByFolder(uri.toString())
            } else {
                dao.deleteAllSongs()
                dao.clearAllDecisions()
            }
            dao.insertSongs(tracks.map { it.toEntity(uri.toString()) })
            dao.pruneOrphanDecisions()

            val superLiked = dao.observeDecisions().first()
                .filter { it.category == Category.SUPERLIKE.name }
                .map { it.songUri }
                .toSet()
            _deckOrder.value = tracks.filterNot { it.uri in superLiked }.map { it.uri }.shuffled()
            _deckIndex.value = 0

            if (tracks.isEmpty()) {
                _scanMessage.value = "No se encontraron archivos de audio en esa carpeta"
            }
        } catch (e: Exception) {
            _scanMessage.value = "No se pudo leer la carpeta seleccionada"
        } finally {
            _isScanning.value = false
        }
    }

    /** Desliza la tarjeta actual hacia la categoría elegida. */
    fun swipe(category: Category) {
        val current = uiState.value.current ?: return
        viewModelScope.launch {
            dao.upsertDecision(DecisionEntity(songUri = current.uri, category = category.name))
        }
        updateDeckAfterSwipe(current.uri, category)
    }

    private fun updateDeckAfterSwipe(uri: String, category: Category) {
        val deck = _deckOrder.value.toMutableList()
        if (deck.isEmpty()) return
        val index = _deckIndex.value.coerceIn(0, deck.lastIndex)
        deck.remove(uri)
        // Like y Rechazo vuelven al final del mazo (seguirán apareciendo).
        // Superlike no vuelve a entrar al mazo de evaluación.
        if (category != Category.SUPERLIKE) deck.add(uri)

        var newIndex = index
        // Si llegamos al final (o el elemento en el índice es el recién swipeado),
        // rebarajamos para que el mazo siga siendo variado.
        if (newIndex >= deck.size || (deck.isNotEmpty() && deck[newIndex] == uri)) {
            deck.shuffle()
            newIndex = 0
        }
        _deckOrder.value = deck
        _deckIndex.value = newIndex
    }

    /** Quita una canción de la lista donde estaba (por si te equivocaste). */
    fun removeFromList(uri: String) {
        viewModelScope.launch { dao.deleteDecision(uri) }
    }

    /** Vacía una categoría entera; los superlikes revividos vuelven al mazo. */
    fun clearCategory(category: Category) {
        viewModelScope.launch { dao.clearCategory(category.name) }
    }



    /** Borra el archivo del dispositivo y limpia registros locales. */
    fun deleteFromDevice(uri: String) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val contentUri = Uri.parse(uri)
                withContext(Dispatchers.IO) {
                    try {
                        DocumentsContract.deleteDocument(app.contentResolver, contentUri)
                    } catch (_: Exception) {
                        try {
                            app.contentResolver.delete(contentUri, null, null)
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                dao.deleteDecision(uri)
                dao.deleteSong(uri)
                val deck = _deckOrder.value.toMutableList()
                deck.remove(uri)
                _deckOrder.value = deck
                if (_deckIndex.value >= _deckOrder.value.size && _deckOrder.value.isNotEmpty()) {
                    _deckIndex.value = _deckOrder.value.lastIndex
                } else if (_deckOrder.value.isEmpty()) {
                    _deckIndex.value = 0
                }
            }
        }
    }

    fun deleteAllFromDevice() {
        viewModelScope.launch {
            val toDelete = mutableListOf<String>()
            try {
                val app = getApplication<Application>()
                val decisions = dao.observeDecisions().first()
                val rejectUris = decisions.filter { it.category == Category.REJECT.name }.map { it.songUri }
                toDelete.addAll(rejectUris)
                val songs = dao.observeSongs().first()
                val rejectSongs = songs.filter { it.uri in rejectUris }
                withContext(Dispatchers.IO) {
                    rejectSongs.forEach { song ->
                        try {
                            val uri = Uri.parse(song.uri)
                            try {
                                DocumentsContract.deleteDocument(app.contentResolver, uri)
                            } catch (_: Exception) {
                                try {
                                    app.contentResolver.delete(uri, null, null)
                                } catch (_: Exception) {
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                dao.clearCategory(Category.REJECT.name)
                toDelete.forEach { uri ->
                    dao.deleteSong(uri)
                    val deck = _deckOrder.value.toMutableList()
                    deck.remove(uri)
                    _deckOrder.value = deck
                }
                if (_deckIndex.value >= _deckOrder.value.size && _deckOrder.value.isNotEmpty()) {
                    _deckIndex.value = _deckOrder.value.lastIndex
                } else if (_deckOrder.value.isEmpty()) {
                    _deckIndex.value = 0
                }
            }
        }
    }


    fun dismissMessage() {
        _scanMessage.value = null
    }

    private fun Track.toEntity(folder: String): SongEntity = SongEntity(
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        folder = folder
    )
}