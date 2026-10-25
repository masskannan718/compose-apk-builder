package com.example.generatedapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

data class TacticalPayload(
    val taskId: String,
    val jarvisBrief: String,
    val ultronSeverity: String,
    val ultronFlaw: String,
    val ultronImpact: String,
    val safePatch: String,
    val forceBypass: String,
    val karenVoice: String,
    val isDestructive: Boolean
)

class ConsoleMessage(
    val sender: String,
    val text: String = "",
    val isBoss: Boolean = false,
    val tacticalData: TacticalPayload? = null
) {
    var terminalLogs by mutableStateOf("")
    var isExecuting by mutableStateOf(false)
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    var isSpeakingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            StarkMasterConsoleUI(
                onSpeakToggle = { text -> toggleSpeech(text) },
                isSpeaking = isSpeakingState.value,
                startListening = { onResult -> startVoiceInput(onResult) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ta", "IN")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { isSpeakingState.value = true }
                override fun onDone(utteranceId: String?) { isSpeakingState.value = false }
                override fun onError(utteranceId: String?) { isSpeakingState.value = false }
            })
        }
    }

    private fun toggleSpeech(text: String) {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            isSpeakingState.value = false
        } else {
            val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "TTS_MSG") }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_MSG")
            isSpeakingState.value = true
        }
    }

    private fun startVoiceInput(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "கட்டளையிடுங்கள் பாஸ்...")
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { Toast.makeText(this@MainActivity, "குரல் கேட்கவில்லை", Toast.LENGTH_SHORT).show() }
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
fun StarkMasterConsoleUI(
    onSpeakToggle: (String) -> Unit,
    isSpeaking: Boolean,
    startListening: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var inputText by remember { mutableStateOf("") }
    var isPlanning by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showDumeStatus by remember { mutableStateOf(false) }
    var dumeInfo by remember { mutableStateOf("DUM-E சிஸ்டம் சோதிக்கப்படுகிறது...") }

    val messages = remember { mutableStateListOf<ConsoleMessage>() }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                ConsoleMessage(
                    sender = "JARVIS",
                    text = "வணக்கம் பாஸ்! ஸ்டார்க் மிஷன் கண்ட்ரோல் நேரலை PTY டெர்மினலுடன் தயார் நிலையில் உள்ளது."
                )
            )
        }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startListening { text -> inputText = text }
        else Toast.makeText(context, "மைக் அனுமதி தேவை", Toast.LENGTH_SHORT).show()
    }

    fun inspectDume() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", 8080), 2000)
                s.close()
                dumeInfo = "🤖 DUM-E பின்னணி மெக்கானிக்:\n\n• போர்ட் 8080: இணைப்பில் உள்ளது [✓]\n• PTY Bridge: ஆன்லைன் [✓]\n• சாக்கெட் ஸ்ட்ரீமிங்: தயார் [✓]"
            } catch (e: Exception) {
                dumeInfo = "🤖 DUM-E எச்சரிக்கை:\n\n• போர்ட் 8080 ஆஃப்லைன்!\n• Termux-ல் stark_core_server.py இயங்கவில்லை."
            }
        }
    }

    fun submitCommand(cmd: String) {
        if (cmd.isBlank()) return
        messages.add(ConsoleMessage(sender = "BOSS", text = cmd, isBoss = true))
        inputText = ""
        isPlanning = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 8080), 12000)
                val out = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val payload = JSONObject().apply {
                    put("action", "PLAN")
                    put("command", cmd)
                }
                out.println(payload.toString())

                val resp = reader.readLine()
                socket.close()

                withContext(Dispatchers.Main) {
                    isPlanning = false
                    if (resp != null) {
                        try {
                            val json = JSONObject(resp)
                            val auditObj = json.optJSONObject("ultron_audit") ?: JSONObject()
                            val patchObj = json.optJSONObject("patches") ?: JSONObject()

                            val tactical = TacticalPayload(
                                taskId = json.optString("task_id", "task_${System.currentTimeMillis()}"),
                                jarvisBrief = json.optString("jarvis_brief", "திட்டம் தயார்."),
                                ultronSeverity = auditObj.optString("severity", "SAFE"),
                                ultronFlaw = auditObj.optString("flaw", "சிக்கல்கள் இல்லை."),
                                ultronImpact = auditObj.optString("impact", "பாதிப்பில்லை."),
                                safePatch = patchObj.optString("safe_patch", cmd),
                                forceBypass = patchObj.optString("force_bypass", cmd),
                                karenVoice = json.optString("karen_voice", "கட்டளை தயார் பாஸ்."),
                                isDestructive = json.optBoolean("destructive", false)
                            )
                            messages.add(ConsoleMessage(sender = "COUNCIL", tacticalData = tactical))
                            if (tactical.karenVoice.isNotBlank()) {
                                onSpeakToggle(tactical.karenVoice)
                            }
                        } catch (e: Exception) {
                            messages.add(ConsoleMessage(sender = "FRIDAY", text = resp))
                        }
                    } else {
                        messages.add(ConsoleMessage(sender = "FRIDAY", text = "சர்வர் இணைப்பு முறிந்தது பாஸ்."))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPlanning = false
                    messages.add(ConsoleMessage(sender = "FRIDAY", text = "கனெக்ஷன் பிழை: போர்ட் 8080 கிடைக்கவில்லை."))
                }
            }
        }
    }

    fun executeTerminalTask(msg: ConsoleMessage, script: String, taskId: String) {
        msg.isExecuting = true
        msg.terminalLogs = "> Initializing PTY Shell...\n$ $script\n"

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 8080), 8000)
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
                    withContext(Dispatchers.Main) {
                        val updated = msg.terminalLogs + chunk
                        val lines = updated.lines()
                        if (lines.size > 150) {
                            msg.terminalLogs = lines.takeLast(150).joinToString("\n")
                        } else {
                            msg.terminalLogs = updated
                        }
                    }
                }
                socket.close()
                withContext(Dispatchers.Main) {
                    msg.isExecuting = false
                    msg.terminalLogs += "\n[✓ Process Completed]"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    msg.isExecuting = false
                    msg.terminalLogs += "\n[x Execution Error: ${e.message}]"
                }
            }
        }
    }

    fun killTask(msg: ConsoleMessage, taskId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", 8080), 3000)
                val out = PrintWriter(s.getOutputStream(), true)
                out.println(JSONObject().apply {
                    put("action", "KILL")
                    put("task_id", taskId)
                }.toString())
                s.close()
                withContext(Dispatchers.Main) {
                    msg.isExecuting = false
                    msg.terminalLogs += "\n[⏹ Process Terminated by Boss - SIGKILL]"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "கில் செய்வதில் பிழை", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0B132B), modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        messages.clear()
                        messages.add(ConsoleMessage(sender = "JARVIS", text = "புதிய அமர்வு தொடங்கப்பட்டது பாஸ்!"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2541)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF48CAE4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("புதிய அரட்டை (New Chat)", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFF59E0B)) },
                    label = { Text("DUM-E பின்னணி மெக்கானிக்", color = Color.White) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        inspectDume()
                        showDumeStatus = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("STARK MISSION CONTROL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("6 AI Agents & PTY Core Active", color = Color(0xFF48CAE4), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B)),
                    actions = {
                        IconButton(onClick = { showTopMenu = !showTopMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            modifier = Modifier.background(Color(0xFF1C2541))
                        ) {
                            DropdownMenuItem(
                                text = { Text("🛠️ DUM-E சிஸ்டம் நிலை", color = Color.White) },
                                onClick = {
                                    showTopMenu = false
                                    inspectDume()
                                    showDumeStatus = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🧹 திரையை சுத்தம் செய்ய", color = Color.White) },
                                onClick = {
                                    showTopMenu = false
                                    messages.clear()
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = Color(0xFF0B132B),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF1C2541))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
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
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color(0xFF48CAE4))
                        }

                        IconButton(
                            onClick = { submitCommand(inputText) },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0077B6))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            },
            containerColor = Color(0xFF060A17)
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    if (msg.tacticalData != null) {
                        TacticalCardView(
                            msg = msg,
                            onExecute = { script -> executeTerminalTask(msg, script, msg.tacticalData.taskId) },
                            onKill = { killTask(msg, msg.tacticalData.taskId) },
                            onSpeak = { text -> onSpeakToggle(text) },
                            isSpeaking = isSpeaking
                        )
                    } else {
                        StandardMessageCard(msg = msg, onSpeak = { onSpeakToggle(msg.text) }, isSpeaking = isSpeaking)
                    }
                }

                if (isPlanning) {
                    item {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF48CAE4), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("JARVIS & ULTRON திட்டமிட்டு தணிக்கை செய்கின்றனர்...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDumeStatus) {
        AlertDialog(
            onDismissRequest = { showDumeStatus = false },
            title = { Text("🛠️ DUM-E பின்னணி மெக்கானிக்", color = Color.White) },
            text = { Text(dumeInfo, color = Color(0xFFE2E8F0), fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { inspectDume() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))) {
                    Text("மீண்டும் சரிபார்")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDumeStatus = false }) { Text("மூடு", color = Color(0xFF94A3B8)) }
            },
            containerColor = Color(0xFF1C2541)
        )
    }
}

@Composable
fun TacticalCardView(
    msg: ConsoleMessage,
    onExecute: (String) -> Unit,
    onKill: () -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: Boolean
) {
    val tactical = msg.tacticalData ?: return
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showTerminal by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF111D4A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏛️ STARK MISSION COUNCIL", color = Color(0xFF48CAE4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                    contentDescription = "Voice",
                    tint = if (isSpeaking) Color(0xFFEF4444) else Color(0xFF48CAE4),
                    modifier = Modifier.size(20.dp).clickable { onSpeak(tactical.karenVoice) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("JARVIS: ${tactical.jarvisBrief}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            val severityColor = when (tactical.ultronSeverity) {
                "CRITICAL" -> Color(0xFFEF4444)
                "WARNING" -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }

            Surface(
                color = severityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = severityColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ULTRON AUDIT [${tactical.ultronSeverity}]", color = severityColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• பிழை: ${tactical.ultronFlaw}", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    Text("• விளைவு: ${tactical.ultronImpact}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 0) Color(0xFF0077B6) else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text("🛡️ Safe Patch", color = Color.White, fontSize = 11.sp)
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 1) Color(0xFFDC2626) else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text("⚠️ Raw Bypass", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val currentScript = if (selectedTab == 0) tactical.safePatch else tactical.forceBypass

            Surface(
                color = Color(0xFF060A17),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FRIDAY SCRIPT", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Copy",
                            color = Color(0xFF48CAE4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Script", currentScript))
                                Toast.makeText(context, "நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = currentScript, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        showTerminal = true
                        onExecute(currentScript)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 0) Color(0xFF10B981) else Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    enabled = !msg.isExecuting
                ) {
                    Text(if (selectedTab == 0) "⚡ திருத்தி இயக்கு" else "⚠️ ரிஸ்க்கோடு இயக்கு", fontSize = 12.sp, color = Color.White)
                }

                if (msg.isExecuting) {
                    Button(
                        onClick = onKill,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Kill", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showTerminal = !showTerminal },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showTerminal) "▲ மறை (Hide Terminal)" else "▼ நேரலை டெர்மினல் (Show Terminal)",
                    color = Color(0xFF48CAE4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (msg.isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF10B981), strokeWidth = 2.dp)
                }
            }

            AnimatedVisibility(visible = showTerminal) {
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = msg.terminalLogs.ifBlank { "> Waiting for output..." },
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandardMessageCard(msg: ConsoleMessage, onSpeak: () -> Unit, isSpeaking: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isBoss) Alignment.End else Alignment.Start
    ) {
        Text(
            text = msg.sender,
            color = if (msg.isBoss) Color(0xFF60A5FA) else Color(0xFF48CAE4),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )

        Surface(
            color = if (msg.isBoss) Color(0xFF0077B6) else Color(0xFF1C2541),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = msg.text, color = Color.White, fontSize = 14.sp)
            }
        }

        if (!msg.isBoss) {
            Icon(
                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                contentDescription = "Voice",
                tint = if (isSpeaking) Color(0xFFEF4444) else Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp).size(18.dp).clickable { onSpeak() }
            )
        }
    }
}
