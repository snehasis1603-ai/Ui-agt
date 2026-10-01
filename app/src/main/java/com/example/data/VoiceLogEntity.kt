package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_logs")
data class VoiceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domainTitle: String,
    val transcription: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = true
)
