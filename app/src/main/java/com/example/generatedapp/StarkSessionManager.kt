package com.example.generatedapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object StarkSessionManager {
    private const val SESSIONS_FILE = "stark_sessions.json"

    fun saveSessions(context: Context, sessions: List<ChatSession>) {
        try {
            val file = File(context.filesDir, SESSIONS_FILE)
            val jsonArray = JSONArray()

            sessions.forEach { s ->
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("lastUpdated", s.lastUpdated)

                    val msgArr = JSONArray()
                    s.messages.forEach { m ->
                        val mObj = JSONObject().apply {
                            put("id", m.id)
                            put("sender", m.sender)
                            put("text", m.text)
                            put("isBoss", m.isBoss)
                            put("terminalLogs", m.terminalLogs)
                            put("timestamp", m.timestamp)
                            m.isLiked?.let { put("isLiked", it) }

                            m.tacticalData?.let { t ->
                                val tObj = JSONObject().apply {
                                    put("taskId", t.taskId)
                                    put("jarvisBrief", t.jarvisBrief)
                                    put("ultronSeverity", t.ultronSeverity)
                                    put("ultronFlaw", t.ultronFlaw)
                                    put("ultronImpact", t.ultronImpact)
                                    put("safePatch", t.safePatch)
                                    put("forceBypass", t.forceBypass)
                                    put("karenVoice", t.karenVoice)
                                }
                                put("tacticalData", tObj)
                            }
                        }
                        msgArr.put(mObj)
                    }
                    put("messages", msgArr)
                }
                jsonArray.put(sObj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSessions(context: Context): MutableList<ChatSession> {
        val list = mutableListOf<ChatSession>()
        try {
            val file = File(context.filesDir, SESSIONS_FILE)
            if (!file.exists()) return list

            val raw = file.readText()
            val jsonArray = JSONArray(raw)

            for (i in 0 until jsonArray.length()) {
                val sObj = jsonArray.getJSONObject(i)
                val msgList = mutableListOf<ConsoleMessage>()
                val msgArr = sObj.optJSONArray("messages") ?: JSONArray()

                for (j in 0 until msgArr.length()) {
                    val mObj = msgArr.getJSONObject(j)
                    var tactical: TacticalPayload? = null

                    mObj.optJSONObject("tacticalData")?.let { t ->
                        tactical = TacticalPayload(
                            taskId = t.optString("taskId"),
                            jarvisBrief = t.optString("jarvisBrief"),
                            ultronSeverity = t.optString("ultronSeverity"),
                            ultronFlaw = t.optString("ultronFlaw"),
                            ultronImpact = t.optString("ultronImpact"),
                            safePatch = t.optString("safePatch"),
                            forceBypass = t.optString("forceBypass"),
                            karenVoice = t.optString("karenVoice")
                        )
                    }

                    val liked = if (mObj.has("isLiked")) mObj.getBoolean("isLiked") else null

                    msgList.add(
                        ConsoleMessage(
                            id = mObj.optString("id"),
                            sender = mObj.optString("sender"),
                            text = mObj.optString("text"),
                            isBoss = mObj.optBoolean("isBoss"),
                            tacticalData = tactical,
                            terminalLogs = mObj.optString("terminalLogs"),
                            isLiked = liked,
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }

                list.add(
                    ChatSession(
                        id = sObj.optString("id"),
                        title = sObj.optString("title", "Project Session"),
                        messages = msgList,
                        lastUpdated = sObj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
