package com.example.generatedapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

data class ModelEngine(val id: String, val displayName: String, val badge: String)

data class ChatMessage(
    val sender: String,
    val text: String,
    val isBoss: Boolean = false,
    val modelTag: String? = null,
    val errorReason: String? = null
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            GeminiMasterScreen(
                onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) },
                startListening = { onResult -> startVoiceInput(onResult) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ta", "IN")
        }
    }

    private fun startVoiceInput(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "தமிழில் கட்டளையிடவும் பாஸ்...")
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "குரல் சரியாகக் கேட்கவில்லை", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) onResult(matches[0])
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiMasterScreen(
    onSpeak: (String) -> Unit,
    startListening: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs: SharedPreferences = remember { context.getSharedPreferences("council_vault", Context.MODE_PRIVATE) }

    val availableModels = listOf(
        ModelEngine("council-groq", "Stark Council", "Uncensored Cloud"),
        ModelEngine("puyal-local", "Puyal Core", "Local Edge"),
        ModelEngine("custom-host", "Host Slot 3", "Dedicated")
    )

    var currentModel by remember { mutableStateOf(availableModels[0]) }
    var thinkingMode by remember { mutableStateOf("Normal") }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    var showModelMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDumeDialog by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf("Checking...") }
    var attachedFileUri by remember { mutableStateOf<String?>(null) }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    // Load Chat History
    LaunchedEffect(Unit) {
        val saved = prefs.getString("chat_history", null)
        if (!saved.isNullOrEmpty()) {
            try {
                val arr = JSONArray(saved)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    messages.add(
                        ChatMessage(
                            sender = obj.getString("sender"),
                            text = obj.getString("text"),
                            isBoss = obj.getBoolean("isBoss"),
                            modelTag = obj.optString("modelTag", null)
                        )
                    )
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("JARVIS", "வணக்கம் பாஸ்! கவுன்சில் மாஸ்டர் சிஸ்டம் தயார்."))
            }
        } else {
            messages.add(ChatMessage("JARVIS", "வணக்கம் பாஸ்! கவுன்சில் மாஸ்டர் சிஸ்டம் தயார்."))
        }
    }

    // Save Chat History Helper
    fun saveHistory() {
        val arr = JSONArray()
        messages.takeLast(50).forEach { msg ->
            val obj = JSONObject().apply {
                put("sender", msg.sender)
                put("text", msg.text)
                put("isBoss", msg.isBoss)
                put("modelTag", msg.modelTag ?: "")
            }
            arr.put(obj)
        }
        prefs.edit().putString("chat_history", arr.toString()).apply()
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachedFileUri = uri.lastPathSegment ?: "file"
            showBottomSheet = false
        }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startListening { text -> inputText = text }
        else Toast.makeText(context, "மைக் அனுமதி தேவை", Toast.LENGTH_SHORT).show()
    }

    fun checkDumeStatus() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", 8080), 2000)
                s.close()
                serverStatus = "Online [✓] (போர்ட் 8080 தயார் நிலையில் உள்ளது)"
            } catch (e: Exception) {
                serverStatus = "Offline [x] (காரணம்: Termux சர்வர் இயங்கவில்லை)"
            }
        }
    }

    fun sendMessage(textToSend: String) {
        if (textToSend.isBlank() && attachedFileUri == null) return
        val dispatchText = if (attachedFileUri != null) "[$attachedFileUri] $textToSend" else textToSend

        messages.add(ChatMessage("BOSS", dispatchText, isBoss = true))
        saveHistory()
        inputText = ""
        val tempAttached = attachedFileUri
        attachedFileUri = null
        isThinking = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 8080), 8000)
                val out = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val payload = JSONObject().apply {
                    put("command", dispatchText)
                    put("engine", currentModel.id)
                    put("thinking", thinkingMode)
                    put("attachment", tempAttached ?: "")
                }
                out.println(payload.toString())

                val resp = reader.readLine()
                socket.close()

                withContext(Dispatchers.Main) {
                    isThinking = false
                    if (resp != null) {
                        val json = JSONObject(resp)
                        val reply = json.optString("reply", "")
                        val agent = json.optString("agent", "COUNCIL")
                        messages.add(ChatMessage(agent, reply, modelTag = currentModel.badge))
                    } else {
                        messages.add(ChatMessage("DUM-E", "பதில் வரவில்லை.", errorReason = "சாக்கெட் சிக்னல் மூடிவிட்டது."))
                    }
                    saveHistory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isThinking = false
                    messages.add(
                        ChatMessage(
                            "DUM-E",
                            "கட்டளையை இயக்க முடியவில்லை பாஸ்!",
                            errorReason = "நெட்வொர்க் பிழை: ${e.message}"
                        )
                    )
                    saveHistory()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showModelMenu = true }) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(currentModel.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                Text("${currentModel.badge} • Thinking: $thinkingMode", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }
                        }
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            Text("செயல்திறன் எஞ்சின்கள்", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName, color = if (model.id == currentModel.id) Color(0xFF38BDF8) else Color.White) },
                                    onClick = { currentModel = model; showModelMenu = false }
                                )
                            }
                            Divider(color = Color(0xFF334155))
                            Text("சிந்தனை வேகம்", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            listOf("Off (Direct)", "Normal", "Deep Council").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode, color = if (thinkingMode == mode) Color(0xFFA855F7) else Color.White) },
                                    onClick = { thinkingMode = mode; showModelMenu = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A)),
                actions = {
                    IconButton(onClick = { showTopMenu = !showTopMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("🛠️ DUM-E மெக்கானிக்", color = Color.White) },
                            onClick = { showTopMenu = false; checkDumeStatus(); showDumeDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("🧹 ஹிஸ்டரியை அழிக்க (Clear)", color = Color.White) },
                            onClick = {
                                showTopMenu = false
                                messages.clear()
                                prefs.edit().remove("chat_history").apply()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF0F172A))) {
                AnimatedVisibility(visible = attachedFileUri != null) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF334155)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(attachedFileUri ?: "", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(16.dp).clickable { attachedFileUri = null })
                    }
                }

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
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color(0xFF94A3B8))
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("கட்டளையிடுங்கள் பாஸ்...", color = Color(0xFF64748B), fontSize = 14.sp) },
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

                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startListening { text -> inputText = text }
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color(0xFF38BDF8))
                        }

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2563EB))
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { msg ->
                GeminiMessageItem(msg, onSpeak = { onSpeak(msg.text) }, onRegenerate = { sendMessage(msg.text) })
            }
            if (isThinking) {
                item {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${currentModel.displayName} பகுப்பாய்வு செய்கிறது...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, containerColor = Color(0xFF1E293B)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("இணைப்பு மூலங்கள்", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { fileLauncher.launch("image/*") }) {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF0284C7)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                        }
                        Text("கேலரி", color = Color(0xFFE2E8F0), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { fileLauncher.launch("*/*") }) {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF10B981)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Description, contentDescription = "Files", tint = Color.White)
                        }
                        Text("கோப்புகள்", color = Color(0xFFE2E8F0), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDumeDialog) {
        AlertDialog(
            onDismissRequest = { showDumeDialog = false },
            title = { Text("🛠️ DUM-E சிஸ்டம் கண்டறிதல்", color = Color.White) },
            text = { Text(serverStatus, color = Color(0xFFE2E8F0), fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { checkDumeStatus() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) {
                    Text("மீண்டும் சரிபார்")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDumeDialog = false }) { Text("மூடு", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun GeminiMessageItem(msg: ChatMessage, onSpeak: () -> Unit, onRegenerate: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isBoss) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = msg.sender,
                color = if (msg.isBoss) Color(0xFF60A5FA) else Color(0xFF34D399),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            )
            if (msg.modelTag != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(msg.modelTag, color = Color(0xFF94A3B8), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
        }

        Surface(
            color = if (msg.isBoss) Color(0xFF1E3A8A) else Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Parse and render Code blocks if present
                RenderMessageContent(msg.text, context)

                if (msg.errorReason != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "காரணம்: ${msg.errorReason}",
                            color = Color(0xFFFECACA),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        if (!msg.isBoss) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp).clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Council Text", msg.text))
                        Toast.makeText(context, "நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                    }
                )
                Icon(
                    Icons.Default.VolumeUp, contentDescription = "Listen", tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp).clickable { onSpeak() }
                )
                Icon(
                    Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp).clickable { onRegenerate() }
                )
            }
        }
    }
}

@Composable
fun RenderMessageContent(rawText: String, context: Context) {
    // Splits by markdown code blocks ```
    val parts = rawText.split("```")
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 == 1) {
            // This is a Code block
            val lines = part.trim().lines()
            val codeContent = if (lines.size > 1) lines.drop(1).joinToString("\n") else part.trim()
            
            Surface(
                color = Color(0xFF090D16),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CODE", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Copy Code",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Extracted Code", codeContent))
                                Toast.makeText(context, "கோட் நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = codeContent,
                        color = Color(0xFF34D399),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Normal text
            if (part.isNotBlank()) {
                Text(text = part.trim(), color = Color(0xFFF1F5F9), fontSize = 14.sp)
            }
        }
    }
}
