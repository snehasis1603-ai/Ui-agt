package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VoiceLogEntity
import com.example.data.VoiceLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpacesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = VoiceLogRepository(database.voiceLogDao())

    // Represents the active space under direct Level 1 console focus
    private val _selectedDomain = MutableStateFlow("Trading & SIP")
    val selectedDomain: StateFlow<String> = _selectedDomain

    fun selectDomain(domainTitle: String) {
        _selectedDomain.value = domainTitle
    }

    val allLogs: StateFlow<List<VoiceLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getLogsForDomain(domainTitle: String): Flow<List<VoiceLogEntity>> {
        return repository.getLogsForDomain(domainTitle)
    }

    fun saveVoiceLog(domainTitle: String, transcription: String, isVoice: Boolean = true) {
        viewModelScope.launch {
            repository.saveLog(domainTitle, transcription, isVoice)
        }
    }

    fun deleteVoiceLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    fun updateVoiceLog(id: Int, transcription: String) {
        viewModelScope.launch {
            repository.updateLog(id, transcription)
        }
    }

    fun clearLogs(domainTitle: String) {
        viewModelScope.launch {
            repository.clearDomainLogs(domainTitle)
        }
    }

    fun clearAllVoiceLogs() {
        viewModelScope.launch {
            repository.deleteAllLogs()
        }
    }
}
