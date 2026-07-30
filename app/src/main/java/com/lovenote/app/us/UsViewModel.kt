package com.lovenote.app.us

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsViewModel(
    val repository: UsRepository,
) : ViewModel() {

    private val _showAddEvent = MutableStateFlow(false)
    val showAddEvent: StateFlow<Boolean> = _showAddEvent.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _recordSeconds = MutableStateFlow(0)
    val recordSeconds: StateFlow<Int> = _recordSeconds.asStateFlow()

    private val _showCaptionDialog = MutableStateFlow(false)
    val showCaptionDialog: StateFlow<Boolean> = _showCaptionDialog.asStateFlow()

    private val _pendingAudio = MutableStateFlow<Pair<String, Long>?>(null)
    val pendingAudio: StateFlow<Pair<String, Long>?> = _pendingAudio.asStateFlow()

    private val _playingVoiceId = MutableStateFlow<String?>(null)
    val playingVoiceId: StateFlow<String?> = _playingVoiceId.asStateFlow()

    private val _showRouletteAnswer = MutableStateFlow(false)
    val showRouletteAnswer: StateFlow<Boolean> = _showRouletteAnswer.asStateFlow()

    private val _rouletteQuestion = MutableStateFlow<String?>(null)
    val rouletteQuestion: StateFlow<String?> = _rouletteQuestion.asStateFlow()

    private val _rouletteAnswer = MutableStateFlow("")
    val rouletteAnswer: StateFlow<String> = _rouletteAnswer.asStateFlow()

    private val _showCountdownPicker = MutableStateFlow(false)
    val showCountdownPicker: StateFlow<Boolean> = _showCountdownPicker.asStateFlow()

    private val _countdownTitle = MutableStateFlow("")
    val countdownTitle: StateFlow<String> = _countdownTitle.asStateFlow()

    private val _memoryLane = MutableStateFlow<Memory?>(null)
    val memoryLane: StateFlow<Memory?> = _memoryLane.asStateFlow()

    private val _countdownTick = MutableStateFlow(0)
    val countdownTick: StateFlow<Int> = _countdownTick.asStateFlow()

    val myUid: String get() = repository.myUid

    fun setShowAddEvent(v: Boolean) { _showAddEvent.value = v }
    fun setRecording(v: Boolean) { _recording.value = v }
    fun setRecordSeconds(v: Int) { _recordSeconds.value = v }
    fun setShowCaptionDialog(v: Boolean) { _showCaptionDialog.value = v }
    fun setPendingAudio(p: Pair<String, Long>?) { _pendingAudio.value = p }
    fun setPlayingVoiceId(v: String?) { _playingVoiceId.value = v }
    fun setShowRouletteAnswer(v: Boolean) { _showRouletteAnswer.value = v }
    fun setRouletteQuestion(v: String?) { _rouletteQuestion.value = v }
    fun setRouletteAnswer(v: String) { _rouletteAnswer.value = v }
    fun setShowCountdownPicker(v: Boolean) { _showCountdownPicker.value = v }
    fun setCountdownTitle(v: String) { _countdownTitle.value = v }
    fun setMemoryLane(v: Memory?) { _memoryLane.value = v }
    fun setCountdownTick(v: Int) { _countdownTick.value = v }

    fun setMood(emoji: String, statusWord: String? = null) = viewModelScope.launch {
        repository.setMood(emoji, statusWord)
    }

    fun submitAnswer(dateKey: String, answer: String) = viewModelScope.launch {
        repository.submitAnswer(dateKey, answer)
    }

    fun submitQuiz(dateKey: String, myAnswer: Int, guessForPartner: Int) = viewModelScope.launch {
        repository.submitQuiz(dateKey, myAnswer, guessForPartner)
    }

    fun addEvent(title: String, dateMillis: Long) = viewModelScope.launch {
        repository.addEvent(title, dateMillis)
    }

    fun deleteEvent(id: String) = viewModelScope.launch {
        repository.deleteEvent(id)
    }

    fun sendVoiceLetter(audioBase64: String, durationSec: Long, caption: String) = viewModelScope.launch {
        repository.sendVoiceLetter(audioBase64, durationSec, caption)
    }

    fun deleteVoiceLetter(id: String) = viewModelScope.launch {
        repository.deleteVoiceLetter(id)
    }

    fun submitRouletteAnswer(questionIndex: Int, answer: String) = viewModelScope.launch {
        repository.submitRouletteAnswer(questionIndex, answer)
    }

    fun setCountdown(title: String, targetMillis: Long) = viewModelScope.launch {
        repository.setCountdown(title, targetMillis)
    }

    fun clearCountdown() = viewModelScope.launch {
        repository.clearCountdown()
    }
}
