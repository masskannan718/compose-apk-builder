package com.example.generatedapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object StarkSessionManager {
    private const val FILE_NAME = "stark_projects_cache.json"

    fun save(context: Context, sessions: List<ChatSession>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val root = JSONArray()
            sessions.forEach { s ->
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("lastUpdated", s.lastUpdated)
                    val mArr = JSONArray()
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
                                put("tacticalData", JSONObject().apply {
                                    put("taskId", t.taskId)
                                    put("jarvisBrief", t.jarvisBrief)
                                    put("ultronSeverity", t.ultronSeverity)
                                    put("ultronFlaw", t.ultronFlaw)
                                    put("ultronImpact", t.ultronImpact)
                                    put("safePatch", t.safePatch)
                                    put("forceBypass", t.forceBypass)
                                    put("karenVoice", t.karenVoice)
                                })
                            }
                        }
                        mArr.put(mObj)
                    }
                    put("messages", mArr)
                }
                root.put(sObj)
            }
            file.writeText(root.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun load(context: Context): MutableList<ChatSession> {
        val list = mutableListOf<ChatSession>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val root = JSONArray(file.readText())
            for (i in 0 until root.length()) {
                val sObj = root.getJSONObject(i)
                val mList = mutableListOf<ConsoleMessage>()
                val mArr = sObj.optJSONArray("messages") ?: JSONArray()
                for (j in 0 until mArr.length()) {
                    val m = mArr.getJSONObject(j)
                    var tac: TacticalPayload? = null
                    m.optJSONObject("tacticalData")?.let { t ->
                        tac = TacticalPayload(
                            t.optString("taskId"), t.optString("jarvisBrief"),
                            t.optString("ultronSeverity"), t.optString("flaw"),
                            t.optString("impact"), t.optString("safePatch"),
                            t.optString("forceBypass"), t.optString("karenVoice")
                        )
                    }
                    val lk = if (m.has("isLiked")) m.getBoolean("isLiked") else null
                    mList.add(ConsoleMessage(
                        id = m.optString("id"), sender = m.optString("sender"),
                        text = m.optString("text"), isBoss = m.optBoolean("isBoss"),
                        tacticalData = tac, terminalLogs = m.optString("terminalLogs"),
                        isLiked = lk, timestamp = m.optLong("timestamp")
                    ))
                }
                list.add(ChatSession(sObj.optString("id"), sObj.optString("title"), mList, sObj.optLong("lastUpdated")))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }
}
