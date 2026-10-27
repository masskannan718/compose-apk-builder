package com.example.generatedapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val codeSnippet: String = "",
    val voiceSummary: String = "",
    var executionOutput: String = ""
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    var selectedVoicePitch by mutableStateOf(1.0f)
    var selectedVoiceSpeed by mutableStateOf(1.0f)

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "மைக் அனுமதி தேவை பாஸ்!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        // ஆடியோ பர்மிஷன் சரிபார்த்தல்
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF9F9FB),
                    surface = Color.White,
                    onBackground = Color(0xFF1F1F1F),
                    onSurface = Color(0xFF1F1F1F)
                )
            ) {
                GeminiMasterWithAllThreeFeatures(
                    onSpeak = { text -> speakOut(text) },
                    onStartListening = { onResult -> startSpeechRecognition(onResult) }
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
        tts?.setPitch(selectedVoicePitch)
        tts?.setSpeechRate(selectedVoiceSpeed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startSpeechRecognition(onResult: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        runOnUiThread {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Toast.makeText(this@MainActivity, "பேசுங்க பாஸ் (Listening...)", Toast.LENGTH_SHORT).show() }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                }
                override fun onError(error: Int) { Toast.makeText(this@MainActivity, "குரல் சரியாகக் கேட்கவில்லை", Toast.LENGTH_SHORT).show() }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer?.startListening(intent)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}

// லோக்கல் சாட் ஹிஸ்டரி சேமிப்பு மேலாளர்
object ChatStorage {
    private const val PREF_NAME = "gemini_chat_cache"
    private const val KEY_HISTORY = "history_json"

    fun saveMessages(context: Context, messages: List<ChatMessage>) {
        val array = JSONArray()
        for (m in messages) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("sender", m.sender)
                put("text", m.text)
                put("codeSnippet", m.codeSnippet)
                put("voiceSummary", m.voiceSummary)
                put("executionOutput", m.executionOutput)
            }
            array.put(obj)
        }
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun loadMessages(context: Context): List<ChatMessage> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val str = sp.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<ChatMessage>()
        try {
            val array = JSONArray(str)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        sender = obj.optString("sender"),
                        text = obj.optString("text"),
                        codeSnippet = obj.optString("codeSnippet"),
                        voiceSummary = obj.optString("voiceSummary"),
                        executionOutput = obj.optString("executionOutput")
                    )
                )
            }
        } catch (e: Exception) {}
        return list
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiMasterWithAllThreeFeatures(
    onSpeak: (String) -> Unit,
    onStartListening: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // தொடங்கும் போது லோக்கல் ஹிஸ்டரியை மீட்டெடுத்தல்
    LaunchedEffect(Unit) {
        val cached = ChatStorage.loadMessages(context)
        messages.addAll(cached)
    }

    // மெசேஜ் மாறும்போது தானாகவே லோக்கலில் சேமித்தல்
    fun persist() {
        ChatStorage.saveMessages(context, messages)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Live", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF1F1F1F)) },
                navigationIcon = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1F1F1F)) }
                },
                actions = {
                    IconButton(onClick = {
                        messages.clear()
                        ChatStorage.clear(context)
                    }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear History", tint = Color(0xFF1F1F1F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        },
        bottomBar = {
            // Floating Capsule Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color(0xFF444746)) }
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

                        // 2. ரியல் வாய்ஸ் இன்புட் (Mic Integration)
                        IconButton(onClick = {
                            onStartListening { spokenText ->
                                inputText = spokenText
                            }
                        }) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Speech to Text", tint = Color(0xFF0B57D0))
                        }

                        // Send Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3E3FD))
                                .clickable {
                                    if (inputText.isNotBlank()) {
                                        val userCmd = inputText
                                        messages.add(ChatMessage(sender = "user", text = userCmd))
                                        persist()
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

                                                val res = input.readLine() ?: "{}"
                                                s.close()
                                                val json = JSONObject(res)

                                                val brief = json.optString("jarvis_brief", "கட்டளை முடிக்கப்பட்டது பாஸ்.")
                                                val script = json.optJSONObject("patches")?.optString("safe_patch", "") ?: ""
                                                val voice = json.optString("karen_voice", brief)

                                                withContext(Dispatchers.Main) {
                                                    isThinking = false
                                                    messages.add(ChatMessage(sender = "gemini", text = brief, codeSnippet = script, voiceSummary = voice))
                                                    persist()
                                                    onSpeak(voice)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isThinking = false
                                                    messages.add(ChatMessage(sender = "gemini", text = "பாஸ், இணைப்பு கிடைக்கவில்லை: ${e.localizedMessage}"))
                                                    persist()
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF041E49), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF9F9FB)).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }
            items(messages) { msg ->
                if (msg.sender == "user") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE8F0FE), modifier = Modifier.widthIn(max = 290.dp)) {
                            Text(text = msg.text, color = Color(0xFF1F1F1F), fontSize = 15.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = msg.text, color = Color(0xFF1F1F1F), fontSize = 15.sp, lineHeight = 22.sp)

                        // 1. கமாண்ட் பிளாக் மற்றும் நேரடி ரன் பட்டன் (Live PTY Execution)
                        if (msg.codeSnippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E1E), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("bash", color = Color(0xFF9E9E9E), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.codeSnippet)) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = msg.codeSnippet, color = Color(0xFFD4D4D4), fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    // டெர்மினலில் இயக்கும் பட்டன்
                                    Button(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val s = Socket("127.0.0.1", 8080)
                                                    val out = OutputStreamWriter(s.getOutputStream(), "UTF-8")
                                                    val input = BufferedReader(InputStreamReader(s.getInputStream(), "UTF-8"))
                                                    val execReq = JSONObject().apply {
                                                        put("action", "EXECUTE")
                                                        put("script", msg.codeSnippet)
                                                    }
                                                    out.write(execReq.toString() + "\n")
                                                    out.flush()

                                                    val sb = java.lang.StringBuilder()
                                                    var line: String?
                                                    while (input.readLine().also { line = it } != null) {
                                                        sb.append(line).append("\n")
                                                    }
                                                    s.close()
                                                    withContext(Dispatchers.Main) {
                                                        msg.executionOutput = sb.toString().trim()
                                                        persist()
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        msg.executionOutput = "இயக்குவதில் பிழை: ${e.localizedMessage}"
                                                        persist()
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D26A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run in Terminal", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // இயக்கப்பட்ட டெர்மினல் அவுட்புட்
                                    if (msg.executionOutput.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Terminal Output:", color = Color(0xFF48CAE4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = msg.executionOutput, color = Color(0xFF00F076), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // ஆக்ஷன் பார்
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", tint = Color(0xFF747775), modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", tint = Color(0xFF747775), modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.text)) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF747775), modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { if (msg.voiceSummary.isNotBlank()) onSpeak(msg.voiceSummary) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.VolumeUp, contentDescription = "Listen", tint = Color(0xFF1F1F1F), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item { Text("Gemini சிந்திக்கிறது...", color = Color(0xFF0B57D0), fontSize = 14.sp) }
            }
        }
    }
}
