package com.example.generatedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class Message(val sender: String, val text: String, val isUser: Boolean = false, val isError: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuperConsoleScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperConsoleScreen() {
    val messages = remember { mutableStateListOf(
        Message("JARVIS", "வணக்கம் பாஸ்! ஸ்டார்க் கவுன்சில் தயார் நிலையில் உள்ளது."),
        Message("FRIDAY", "பாஸ், ஆடியோ மற்றும் பாதுகாப்பு கட்டுப்பாடுகள் இணைக்கப்பட்டுள்ளன.")
    ) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showDumeDialog by remember { mutableStateOf(false) }

    var serverStatus by remember { mutableStateOf("Checking...") }
    var groqStatus by remember { mutableStateOf("Checking...") }
    var isChecking by remember { mutableStateOf(false) }

    fun checkDumeDiagnostics() {
        isChecking = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/dume/diagnose")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                withContext(Dispatchers.Main) {
                    serverStatus = if (json.optBoolean("server_online")) "Online [✓]" else "Offline [✗]"
                    groqStatus = if (json.optBoolean("groq_connected")) "Connected [✓]" else json.optString("groq_msg", "Error [✗]")
                    isChecking = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    serverStatus = "Offline [✗] (${e.localizedMessage})"
                    groqStatus = "Unreachable [✗]"
                    isChecking = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SUPER CONSOLE", fontSize = 18.sp, color = Color.White)
                        Text("Stage 2: Council Debating...", fontSize = 11.sp, color = Color.LightGray)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🛠️ DUM-E மெக்கானிக் (Settings)") },
                            onClick = {
                                showMenu = false
                                showDumeDialog = true
                                checkDumeDiagnostics()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("தமிழில் கட்டளையிடவும்...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = inputText
                        if (text.isNotBlank()) {
                            messages.add(Message("BOSS", text, isUser = true))
                            inputText = ""
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val url = URL("http://127.0.0.1:8080/api/chat")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.doOutput = true
                                    conn.connectTimeout = 5000
                                    conn.readTimeout = 20000

                                    val payload = JSONObject().put("task", text)
                                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                                    val json = JSONObject(response)
                                    val jarvisReply = json.optString("jarvis", "பதில் பெறப்படவில்லை.")
                                    withContext(Dispatchers.Main) {
                                        messages.add(Message("JARVIS", jarvisReply))
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        messages.add(Message("SYSTEM", "இணைப்புப் பிழை: ${e.localizedMessage}", isError = true))
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.background(Color(0xFF2563EB), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }

    if (showDumeDialog) {
        AlertDialog(
            onDismissRequest = { showDumeDialog = false },
            title = { Text("🛠️ DUM-E மெக்கானிக் கண்டறிதல்") },
            text = {
                Column {
                    Text("1. Termux சர்வர் (127.0.0.1:8080):")
                    Text(serverStatus, color = if (serverStatus.contains("[✓]")) Color(0xFF22C55E) else Color(0xFFEF4444), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. Groq AI கவுன்சில் இணைப்பு:")
                    Text(groqStatus, color = if (groqStatus.contains("[✓]")) Color(0xFF22C55E) else Color(0xFFEF4444), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isChecking = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL("http://127.0.0.1:8080/api/dume/fix")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.connectTimeout = 4000
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } catch (_: Exception) {}
                            checkDumeDiagnostics()
                        }
                    }
                ) {
                    Text("பழுதுநீக்கு (Fix System)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDumeDialog = false }) {
                    Text("மூடு")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(msg: Message) {
    val bgColor = when {
        msg.isError -> Color(0xFF3B151E)
        msg.isUser -> Color(0xFF1D4ED8)
        else -> Color(0xFF1E293B)
    }
    val align = if (msg.isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.sender,
                    fontSize = 11.sp,
                    color = if (msg.isError) Color(0xFFF87171) else Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = msg.text, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
