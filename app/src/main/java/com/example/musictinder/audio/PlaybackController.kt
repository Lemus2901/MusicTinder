package com.example.musictinder.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(context: Context) {

    private val player = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentUri = MutableStateFlow<String?>(null)
    val currentUri: StateFlow<String?> = _currentUri.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = player.duration
                        _isBuffering.value = false
                    }
                    Player.STATE_BUFFERING -> _isBuffering.value = true
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _positionMs.value = player.duration
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                _isPlaying.value =
                    playWhenReady && player.playbackState == Player.STATE_READY
            }
        })

        scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition
                delay(250)
            }
        }
    }

    /** Carga y reproduce la pista si aún no está puesta como fuente actual. */
    fun load(uri: String, autoPlay: Boolean) {
        if (_currentUri.value != uri) {
            _currentUri.value = uri
            player.stop()
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            player.prepare()
        }
        player.playWhenReady = autoPlay
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekForward30() = seekRelative(30_000)

    fun seekBackward30() = seekRelative(-30_000)

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, if (player.duration > 0) player.duration else Long.MAX_VALUE)
        player.seekTo(target)
    }

    private fun seekRelative(offsetMs: Long) {
        val max = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (player.currentPosition + offsetMs).coerceIn(0L, max)
        player.seekTo(target)
    }

    fun release() {
        player.release()
    }
}