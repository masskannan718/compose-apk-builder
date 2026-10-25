package com.example.generatedapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val codeSnippet: String = "",
    val voiceSummary: String = ""
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF9F9FB),
                    surface = Color.White,
                    onBackground = Color(0xFF1F1F1F),
                    onSurface = Color(0xFF1F1F1F)
                )
            ) {
                GeminiStyleScreen(
                    onSpeak = { text -> speakOut(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ta", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiStyleScreen(onSpeak: (String) -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gemini", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF1F1F1F))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1F1F1F))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Edit, contentDescription = "New Chat", tint = Color(0xFF1F1F1F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        },
        bottomBar = {
            // Floating Gemini Capsule Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF444746))
                        }
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Gemini...", color = Color(0xFF747775), fontSize = 15.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (inputText.isNotBlank()) {
                                val userCmd = inputText
                                messages.add(ChatMessage(sender = "user", text = userCmd))
                                inputText = ""
                                isThinking = true

                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val s = Socket("127.0.0.1", 8080)
                                        s.soTimeout = 15000
                                        val out = OutputStreamWriter(s.getOutputStream(), "UTF-8")
                                        val input = BufferedReader(InputStreamReader(s.getInputStream(), "UTF-8"))

                                        val req = JSONObject().apply {
                                            put("action", "PLAN")
                                            put("command", userCmd)
                                            put("cwd", "/data/data/com.termux/files/home")
                                        }
                                        out.write(req.toString() + "\n")
                                        out.flush()

                                        val responseStr = input.readLine() ?: "{}"
                                        s.close()
                                        val json = JSONObject(responseStr)

                                        val brief = json.optString("jarvis_brief", "கட்டளை முடிக்கப்பட்டது.")
                                        val script = json.optJSONObject("patches")?.optString("safe_patch", "") ?: ""
                                        val voice = json.optString("karen_voice", brief)

                                        withContext(Dispatchers.Main) {
                                            isThinking = false
                                            messages.add(ChatMessage(sender = "gemini", text = brief, codeSnippet = script, voiceSummary = voice))
                                            onSpeak(voice)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isThinking = false
                                            messages.add(ChatMessage(sender = "gemini", text = "பாஸ், சர்வர் இணைப்பு கிடைக்கவில்லை: ${e.localizedMessage}"))
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF0B57D0))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9FB))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(messages) { msg ->
                if (msg.sender == "user") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE8F0FE),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = Color(0xFF1F1F1F),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = msg.text,
                            color = Color(0xFF1F1F1F),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        // Clean Black Terminal Code Snippet Box
                        if (msg.codeSnippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("bash", color = Color(0xFF9E9E9E), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        IconButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(msg.codeSnippet)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = msg.codeSnippet,
                                        color = Color(0xFFD4D4D4),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Gemini Minimal Action Bar
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.text)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { if (msg.voiceSummary.isNotBlank()) onSpeak(msg.voiceSummary) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = "Listen", tint = Color(0xFF0B57D0), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Text("Gemini சிந்திக்கிறது...", color = Color(0xFF0B57D0), fontSize = 14.sp)
                }
            }
        }
    }
}
