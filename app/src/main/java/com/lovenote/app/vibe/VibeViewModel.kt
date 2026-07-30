package com.lovenote.app.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovenote.app.rituals.RitualTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VibeViewModel(
    val repository: VibeRepository,
) : ViewModel() {

    private val _showShareSheet = MutableStateFlow(false)
    val showShareSheet: StateFlow<Boolean> = _showShareSheet.asStateFlow()

    private val _showCreateRitual = MutableStateFlow(false)
    val showCreateRitual: StateFlow<Boolean> = _showCreateRitual.asStateFlow()

    private val _showTemplates = MutableStateFlow(false)
    val showTemplates: StateFlow<Boolean> = _showTemplates.asStateFlow()

    private val _playingSong = MutableStateFlow<SharedSong?>(null)
    val playingSong: StateFlow<SharedSong?> = _playingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val myUid: String get() = repository.myUid

    fun setShowShareSheet(v: Boolean) { _showShareSheet.value = v }
    fun setShowCreateRitual(v: Boolean) { _showCreateRitual.value = v }
    fun setShowTemplates(v: Boolean) { _showTemplates.value = v }
    fun setPlayingSong(v: SharedSong?) { _playingSong.value = v }
    fun setIsPlaying(v: Boolean) { _isPlaying.value = v }

    fun shareSong(
        uri: String, source: String, title: String, artist: String,
        albumArtUrl: String?, audioUrl: String? = null,
    ) = viewModelScope.launch {
        repository.shareSong(uri, source, title, artist, albumArtUrl, audioUrl)
    }

    fun deleteSong(songId: String) = viewModelScope.launch {
        repository.deleteSong(songId)
    }

    fun reactToSong(songId: String, emoji: String?) = viewModelScope.launch {
        repository.reactToSong(songId, emoji)
    }

    fun completeRitual(ritualId: String, note: String?) = viewModelScope.launch {
        repository.completeRitual(ritualId, note)
    }

    fun toggleRitualActive(id: String, active: Boolean) = viewModelScope.launch {
        repository.toggleRitualActive(id, active)
    }

    fun deleteRitual(id: String) = viewModelScope.launch {
        repository.deleteRitual(id)
    }
}
