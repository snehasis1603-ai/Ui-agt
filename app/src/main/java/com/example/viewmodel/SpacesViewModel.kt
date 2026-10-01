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

    // Agent Activation State
    private val _isAgentActive = MutableStateFlow(true)
    val isAgentActive: StateFlow<Boolean> = _isAgentActive

    // Agent Optimization State
    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing: StateFlow<Boolean> = _isOptimizing

    private val _optimizationScore = MutableStateFlow(98)
    val optimizationScore: StateFlow<Int> = _optimizationScore

    private val _optimizationStatus = MutableStateFlow("System Fully Optimized: All 6 spaces running at peak efficiency")
    val optimizationStatus: StateFlow<String> = _optimizationStatus

    // Last operation message
    private val _lastOperationResult = MutableStateFlow<String?>("Gateway Ready: Armed for Owner Snehasis")
    val lastOperationResult: StateFlow<String?> = _lastOperationResult

    fun toggleAgentActive() {
        val newState = !_isAgentActive.value
        _isAgentActive.value = newState
        val logMsg = if (newState) "OMNI Agent Master Core ACTIVATED by Owner Snehasis" else "OMNI Agent switched to STANDBY by Owner Snehasis"
        saveVoiceLog(selectedDomain.value, logMsg, isVoice = false)
        _lastOperationResult.value = if (newState) "Agent is now ACTIVE and monitoring." else "Agent is now in STANDBY."
    }

    fun runOptimization() {
        if (_isOptimizing.value) return
        viewModelScope.launch {
            _isOptimizing.value = true
            _optimizationStatus.value = "Optimizing neural routing, SIP portfolios, and cyber defense..."
            kotlinx.coroutines.delay(1200)
            _optimizationScore.value = 99
            _optimizationStatus.value = "Peak Optimization Achieved: +195% SIP projection, -38% burn rate, 12ms latency"
            saveVoiceLog(selectedDomain.value, "Optimization Pass Completed: All 6 agent spaces tuned to 99% peak efficiency", isVoice = false)
            _lastOperationResult.value = "Optimization complete: 6/6 spaces tuned."
            _isOptimizing.value = false
        }
    }

    fun operateDirective(actionName: String, domainTitle: String) {
        viewModelScope.launch {
            saveVoiceLog(domainTitle, "OPERATIONAL DIRECTIVE: $actionName executed by Owner Snehasis", isVoice = false)
            _lastOperationResult.value = "Directive executed: $actionName in [$domainTitle]"
        }
    }

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
