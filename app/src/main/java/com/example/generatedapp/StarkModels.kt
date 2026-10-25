package com.example.generatedapp

import java.util.UUID

data class TacticalPayload(
    val taskId: String,
    val jarvisBrief: String,
    val ultronSeverity: String,
    val ultronFlaw: String,
    val ultronImpact: String,
    val safePatch: String,
    val forceBypass: String,
    val karenVoice: String
)

data class ConsoleMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String = "",
    val isBoss: Boolean = false,
    val tacticalData: TacticalPayload? = null,
    val terminalLogs: String = "",
    val isExecuting: Boolean = false,
    val isLiked: Boolean? = null, // true = like, false = dislike, null = none
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ConsoleMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)
