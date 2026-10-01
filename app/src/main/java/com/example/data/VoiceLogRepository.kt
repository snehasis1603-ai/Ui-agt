package com.example.data

import kotlinx.coroutines.flow.Flow

class VoiceLogRepository(private val voiceLogDao: VoiceLogDao) {
    val allLogs: Flow<List<VoiceLogEntity>> = voiceLogDao.getAllLogs()

    fun getLogsForDomain(domainTitle: String): Flow<List<VoiceLogEntity>> {
        return voiceLogDao.getLogsByDomain(domainTitle)
    }

    suspend fun saveLog(domainTitle: String, transcription: String, isVoice: Boolean = true) {
        val entity = VoiceLogEntity(
            domainTitle = domainTitle,
            transcription = transcription,
            isVoice = isVoice
        )
        voiceLogDao.insertLog(entity)
    }

    suspend fun deleteLog(id: Int) {
        voiceLogDao.deleteLogById(id)
    }

    suspend fun updateLog(id: Int, transcription: String) {
        voiceLogDao.updateLogTranscription(id, transcription)
    }

    suspend fun clearDomainLogs(domainTitle: String) {
        voiceLogDao.clearLogsForDomain(domainTitle)
    }

    suspend fun deleteAllLogs() {
        voiceLogDao.deleteAllLogs()
    }
}
