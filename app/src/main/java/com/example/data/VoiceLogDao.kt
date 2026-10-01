package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceLogDao {
    @Query("SELECT * FROM voice_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<VoiceLogEntity>>

    @Query("SELECT * FROM voice_logs WHERE domainTitle = :domainTitle ORDER BY timestamp DESC")
    fun getLogsByDomain(domainTitle: String): Flow<List<VoiceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VoiceLogEntity)

    @Query("DELETE FROM voice_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("UPDATE voice_logs SET transcription = :transcription WHERE id = :id")
    suspend fun updateLogTranscription(id: Int, transcription: String)

    @Query("DELETE FROM voice_logs WHERE domainTitle = :domainTitle")
    suspend fun clearLogsForDomain(domainTitle: String)

    @Query("DELETE FROM voice_logs")
    suspend fun deleteAllLogs()
}
