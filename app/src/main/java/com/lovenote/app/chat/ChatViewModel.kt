package com.lovenote.app.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    val repository: ChatRepository,
) : AndroidViewModel(application) {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _replying = MutableStateFlow<Message?>(null)
    val replying: StateFlow<Message?> = _replying.asStateFlow()

    private val _editing = MutableStateFlow<Message?>(null)
    val editing: StateFlow<Message?> = _editing.asStateFlow()

    private val _reactingTo = MutableStateFlow<String?>(null)
    val reactingTo: StateFlow<String?> = _reactingTo.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _recordSeconds = MutableStateFlow(0)
    val recordSeconds: StateFlow<Int> = _recordSeconds.asStateFlow()

    private val _playingVoiceId = MutableStateFlow<String?>(null)
    val playingVoiceId: StateFlow<String?> = _playingVoiceId.asStateFlow()

    private val _pendingPhoto = MutableStateFlow<String?>(null)
    val pendingPhoto: StateFlow<String?> = _pendingPhoto.asStateFlow()

    private val _pendingOnce = MutableStateFlow(false)
    val pendingOnce: StateFlow<Boolean> = _pendingOnce.asStateFlow()

    private val _viewingPhoto = MutableStateFlow<Message?>(null)
    val viewingPhoto: StateFlow<Message?> = _viewingPhoto.asStateFlow()

    private val _cameraTarget = MutableStateFlow<android.net.Uri?>(null)
    val cameraTarget: StateFlow<android.net.Uri?> = _cameraTarget.asStateFlow()

    private val _olderMessages = MutableStateFlow<List<Message>>(emptyList())
    val olderMessages: StateFlow<List<Message>> = _olderMessages.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _hiddenIds = MutableStateFlow(HiddenMessages.load(application))
    val hiddenIds: StateFlow<Set<String>> = _hiddenIds.asStateFlow()

    val myUid: String get() = repository.myUid

    fun setInput(v: String) { _input.value = v }
    fun setReplying(v: Message?) { _replying.value = v }
    fun setEditing(v: Message?) { _editing.value = v }
    fun setReactingTo(v: String?) { _reactingTo.value = v }
    fun setRecording(v: Boolean) { _recording.value = v }
    fun setRecordSeconds(v: Int) { _recordSeconds.value = v }
    fun setPlayingVoiceId(v: String?) { _playingVoiceId.value = v }
    fun setPendingPhoto(v: String?) { _pendingPhoto.value = v }
    fun setPendingOnce(v: Boolean) { _pendingOnce.value = v }
    fun setViewingPhoto(v: Message?) { _viewingPhoto.value = v }
    fun setCameraTarget(v: android.net.Uri?) { _cameraTarget.value = v }
    fun setOlderMessages(v: List<Message>) { _olderMessages.value = v }
    fun setLoadingMore(v: Boolean) { _loadingMore.value = v }
    fun setHasMore(v: Boolean) { _hasMore.value = v }

    fun send(text: String, replyTo: Message? = null) = viewModelScope.launch {
        repository.send(text, replyTo)
    }

    fun sendPhoto(base64Jpeg: String, once: Boolean = false) = viewModelScope.launch {
        repository.sendPhoto(base64Jpeg, once)
    }

    fun sendVoice(audioBase64: String, durationSec: Long) = viewModelScope.launch {
        repository.sendVoice(audioBase64, durationSec)
    }

    fun deleteMessage(messageId: String) = viewModelScope.launch {
        repository.delete(messageId)
    }

    fun editMessage(messageId: String, newText: String) = viewModelScope.launch {
        repository.edit(messageId, newText)
    }

    fun react(messageId: String, emoji: String?) = viewModelScope.launch {
        repository.react(messageId, emoji)
    }

    fun markSeen(messages: List<Message>) = viewModelScope.launch {
        repository.markPartnerMessagesSeen(messages)
    }

    fun consumeOncePhoto(messageId: String) = viewModelScope.launch {
        repository.consumeOncePhoto(messageId)
    }

    fun hideMessage(id: String) {
        _hiddenIds.value = _hiddenIds.value + id
        HiddenMessages.hide(getApplication(), id)
    }

    fun heartbeatPresence() = viewModelScope.launch {
        repository.heartbeatPresence()
    }

    fun setTyping() = viewModelScope.launch {
        repository.setTyping()
    }

    fun loadOlderMessages() = viewModelScope.launch {
        if (_loadingMore.value || !_hasMore.value) return@launch
        _loadingMore.value = true
        val loaded = repository.loadOlderMessages()
        _olderMessages.value = _olderMessages.value + loaded
        _hasMore.value = repository.canLoadMore()
        _loadingMore.value = false
    }
}
