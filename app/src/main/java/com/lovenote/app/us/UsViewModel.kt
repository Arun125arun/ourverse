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

    val myUid: String get() = repository.myUid

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
