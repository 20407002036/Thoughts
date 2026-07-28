package com.example.thoughts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    val isExpired = _isExpired.asStateFlow()

    private var progressJob: Job? = null
    private var currentPath: String? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startUpdatingProgress()
                } else {
                    stopUpdatingProgress()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isReady.value = playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING
                _isLoading.value = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE && currentPath != null
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                    _error.value = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _isReady.value = false
                _isPlaying.value = false
                _isLoading.value = false

                val cause = error.cause
                val message = when {
                    cause is HttpDataSource.InvalidResponseCodeException -> {
                        if (cause.responseCode == 400 || cause.responseCode == 403) {
                            _isExpired.value = true
                            "Session expired (400)"
                        } else {
                            "Server error (${cause.responseCode})"
                        }
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                        "Local file not found"
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                        "Network connection failed"
                    }
                    else -> "Playback error: ${error.errorCodeName}"
                }

                Log.e("AudioPlayer", "Playback error: $message", error)
                _error.value = message
                stopUpdatingProgress()
            }
        })
    }

    fun loadAudio(pathOrUrl: String) {
        if (pathOrUrl.isBlank()) return
        if (currentPath == pathOrUrl && player.playbackState != Player.STATE_IDLE && _error.value == null) return
        
        currentPath = pathOrUrl
        _error.value = null
        _isExpired.value = false
        _isLoading.value = true

        val uri = if (pathOrUrl.startsWith("/") && !pathOrUrl.startsWith("file://")) {
            "file://$pathOrUrl"
        } else {
            pathOrUrl
        }
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun retry() {
        currentPath?.let { loadAudio(it) }
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    private fun startUpdatingProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition
                delay(500)
            }
        }
    }

    private fun stopUpdatingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        stopUpdatingProgress()
    }
}
