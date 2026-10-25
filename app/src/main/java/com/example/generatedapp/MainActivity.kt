package com.example.generatedapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

data class ChatMessage(val sender: String, val text: String, val isBoss: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeminiConsoleScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiConsoleScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDumeDialog by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf("Checking...") }
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("JARVIS", "வணக்கம் பாஸ்! ஜெமினி ஸ்டைல் ஸ்டார்க் கவுன்சில் முழுமையாக ஒருங்கிணைக்கப்பட்டுள்ளது. எதையும் கட்டளையிடலாம்!"),
            ChatMessage("FRIDAY", "லைவ் வாய்ஸ், ஆக்‌ஷன் பார் மற்றும் பாதுகாப்பு அம்சங்கள் இணைக்கப்பட்டுள்ளன பாஸ்.")
        )
    }

    fun checkDumeStatus() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", 8080), 2000)
                s.close()
                serverStatus = "Online [✓]"
            } catch (e: Exception) {
                serverStatus = "Offline [x] (Failed to connect)"
            }
        }
    }

    fun sendMessage(msgToSend: String) {
        if (msgToSend.isBlank()) return
        messages.add(ChatMessage("BOSS", msgToSend, isBoss = true))
        inputText = ""

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 8080), 3000)
                val out = PrintWriter(socket.getOutputStream(), true)
                val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))

                val payload = JSONObject().apply { put("command", msgToSend) }
                out.println(payload.toString())

                val resp = `in`.readLine()
                socket.close()

                withContext(Dispatchers.Main) {
                    if (resp != null) {
                        val json = JSONObject(resp)
                        val reply = json.optString("reply", "பதில் பெறப்படவில்லை")
                        val agent = json.optString("agent", "COUNCIL")
                        messages.add(ChatMessage(agent, reply))
                    } else {
                        messages.add(ChatMessage("DUM-E", "சர்வரிலிருந்து பதில் வரவில்லை பாஸ்!"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage("DUM-E", "இணைப்புப் பிழை: ${e.message}\nசெட்டிங்ஸில் சரிபார்க்கவும்."))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SUPER CONSOLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Gemini Engine Active", color = Color(0xFF64B5F6), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A)),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = BoxModifierTheme.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("🛠️ DUM-E மெக்கானிக்", color = Color.White) },
                            onClick = {
                                showMenu = false
                                checkDumeStatus()
                                showDumeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🧹 அரட்டையை அழிக்க", color = Color.White) },
                            onClick = {
                                showMenu = false
                                messages.clear()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Gemini-Style Bottom Bar
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // + Attachment Button
                    IconButton(onClick = {
                        Toast.makeText(context, "கோப்பு இணைப்பு வசதி விரைவில்...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color(0xFF94A3B8))
                    }

                    // Text Input
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("கேளுங்கள் அல்லது கட்டளையிடுங்கள்...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Mic Button
                    IconButton(onClick = {
                        Toast.makeText(context, "மைக் மூலமாக பேசலாம்...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color(0xFF94A3B8))
                    }

                    // Live Voice Waveform Button / Send Button
                    if (inputText.isBlank()) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "நேரடி ஆடியோ மோட் தொடங்குகிறது...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Live Voice", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = { sendMessage(inputText) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF090D16)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { msg ->
                GeminiMessageItem(msg)
            }
        }
    }

    if (showDumeDialog) {
        AlertDialog(
            onDismissRequest = { showDumeDialog = false },
            title = { Text("🛠️ DUM-E மெக்கானிக்", color = Color.White) },
            text = {
                Column {
                    Text("1. Termux சர்வர் (127.0.0.1:8080):\n$serverStatus", color = Color(0xFFE2E8F0))
                }
            },
            confirmButton = {
                Button(
                    onClick = { checkDumeStatus() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("சரிபார் (Recheck)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDumeDialog = false }) {
                    Text("மூடு", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun GeminiMessageItem(msg: ChatMessage) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isBoss) Alignment.End else Alignment.Start
    ) {
        // Sender Label
        Text(
            text = msg.sender,
            color = if (msg.isBoss) Color(0xFF60A5FA) else Color(0xFF34D399),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )

        // Bubble
        Surface(
            color = if (msg.isBoss) Color(0xFF1E3A8A) else Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = msg.text,
                color = Color(0xFFF1F5F9),
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        // Action Bar for AI replies (Copy, Listen, Regenerate)
        if (!msg.isBoss) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Action
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Council Text", msg.text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                        }
                )
                // Listen Action
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Listen",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            Toast.makeText(context, "வாசிக்கிறது...", Toast.LENGTH_SHORT).show()
                        }
                )
                // Reload Action
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Regenerate",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            Toast.makeText(context, "மீண்டும் உருவாக்குகிறது...", Toast.LENGTH_SHORT).show()
                        }
                )
            }
        }
    }
}

object BoxModifierTheme {
    fun background(c: Color) = Modifier.background(c)
}
