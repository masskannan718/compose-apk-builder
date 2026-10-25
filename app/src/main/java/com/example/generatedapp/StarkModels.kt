package com.example.generatedapp

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
    val sender: String,
    val text: String = "",
    val isBoss: Boolean = false,
    val tacticalData: TacticalPayload? = null,
    val terminalLogs: String = "",
    val isExecuting: Boolean = false
)
