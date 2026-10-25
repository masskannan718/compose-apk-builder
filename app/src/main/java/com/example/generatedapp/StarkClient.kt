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

    suspend fun requestPlan(command: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 10000)
            val out = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val payload = JSONObject().apply {
                put("action", "PLAN")
                put("command", command)
            }
            out.println(payload.toString())
            val response = reader.readLine()
            socket.close()

            if (response != null) JSONObject(response) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun executePtyStream(
        taskId: String,
        script: String,
        onChunk: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 8000)
            val out = PrintWriter(socket.getOutputStream(), true)
            val inStream = socket.getInputStream()

            val payload = JSONObject().apply {
                put("action", "EXECUTE")
                put("task_id", taskId)
                put("script", script)
            }
            out.println(payload.toString())

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                onChunk(chunk)
            }
            socket.close()
            onChunk("\n[✓ Process Completed]")
        } catch (e: Exception) {
            onChunk("\n[x Socket Closed: ${e.message}]")
        }
    }

    suspend fun sendStdin(taskId: String, input: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 3000)
            val out = PrintWriter(socket.getOutputStream(), true)
            out.println(JSONObject().apply {
                put("action", "STDIN")
                put("task_id", taskId)
                put("input", input)
            }.toString())
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun killRunningProcess(taskId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), 3000)
            val out = PrintWriter(socket.getOutputStream(), true)
            out.println(JSONObject().apply {
                put("action", "KILL")
                put("task_id", taskId)
            }.toString())
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
