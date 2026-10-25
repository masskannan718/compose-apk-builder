package com.example.generatedapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

object StarkClient {
    private const val HOST = "127.0.0.1"
    private const val PORT = 8080

    suspend fun requestPlan(sessionId: String, command: String, cwd: String = "/data/data/com.termux/files/home"): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 10000)
            val out = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val payload = JSONObject().apply {
                put("action", "PLAN")
                put("session_id", sessionId)
                put("command", command)
                put("cwd", cwd)
            }
            out.println(payload.toString())
            val res = reader.readLine()
            socket.close()
            if (res != null) JSONObject(res) else null
        } catch (e: Exception) { null }
    }

    suspend fun executePtyStream(taskId: String, script: String, cwd: String = "/data/data/com.termux/files/home", onChunk: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 8000)
            val out = PrintWriter(socket.getOutputStream(), true)
            val inStream = socket.getInputStream()

            val payload = JSONObject().apply {
                put("action", "EXECUTE")
                put("task_id", taskId)
                put("script", script)
                put("cwd", cwd)
            }
            out.println(payload.toString())

            val buffer = ByteArray(1024)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                onChunk(String(buffer, 0, read, Charsets.UTF_8))
            }
            socket.close()
            onChunk("\n[✓ Process Completed]")
        } catch (e: Exception) {
            onChunk("\n[x Socket Error: ${e.message}]")
        }
    }

    suspend fun sendStdin(taskId: String, input: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 3000)
            PrintWriter(socket.getOutputStream(), true).println(JSONObject().apply {
                put("action", "STDIN")
                put("task_id", taskId)
                put("input", input)
            }.toString())
            socket.close()
            true
        } catch (e: Exception) { false }
    }

    suspend fun killRunningProcess(taskId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 3000)
            PrintWriter(socket.getOutputStream(), true).println(JSONObject().apply {
                put("action", "KILL")
                put("task_id", taskId)
            }.toString())
            socket.close()
            true
        } catch (e: Exception) { false }
    }
}
