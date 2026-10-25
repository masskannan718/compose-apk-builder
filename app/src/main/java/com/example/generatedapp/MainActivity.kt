package com.example.generatedapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    val karenVoice: String
)

data class ConsoleMessage(
    val sender: String,
    val text: String = "",
    val isBoss: Boolean = false,
    val tacticalData: TacticalPayload? = null,
    val terminalLogs: String = "",
    val isExecuting: Boolean = false
)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            StarkConsoleRoot(onSpeak = { text -> speakOut(text) })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ta", "IN")
        }
    }

    private fun speakOut(text: String) {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "STARK_VOICE")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarkConsoleRoot(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isPlanning by remember { mutableStateOf(false) }
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
                                ultronFlaw = auditObj.optString("flaw", "குறைபாடுகள் இல்லை."),
                                ultronImpact = auditObj.optString("impact", "பாதிப்பில்லை."),
                                safePatch = patchObj.optString("safe_patch", cmd),
                                forceBypass = patchObj.optString("force_bypass", cmd),
                                karenVoice = json.optString("karen_voice", "கட்டளை தயார் பாஸ்.")
                            )
                            messages.add(ConsoleMessage(sender = "COUNCIL", tacticalData = tactical))
                            if (tactical.karenVoice.isNotBlank()) {
                                onSpeak(tactical.karenVoice)
                            }
                        } catch (e: Exception) {
                            messages.add(ConsoleMessage(sender = "FRIDAY", text = resp))
                        }
                    } else {
                        messages.add(ConsoleMessage(sender = "FRIDAY", text = "சர்வர் இணைப்பு கிடைக்கவில்லை பாஸ்."))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPlanning = false
                    messages.add(ConsoleMessage(sender = "FRIDAY", text = "போர்ட் 8080 ஆஃப்லைன். stark_core_server.py இயங்குகிறதா என சரிபார்க்கவும்."))
                }
            }
        }
    }

    fun executeTerminalTask(index: Int, script: String, taskId: String) {
        messages[index] = messages[index].copy(
            isExecuting = true,
            terminalLogs = "> Initializing PTY Shell...\n$ $script\n"
        )

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
                        val currentLogs = messages[index].terminalLogs + chunk
                        val lines = currentLogs.lines()
                        val trimmed = if (lines.size > 150) lines.takeLast(150).joinToString("\n") else currentLogs
                        messages[index] = messages[index].copy(terminalLogs = trimmed)
                    }
                }
                socket.close()
                withContext(Dispatchers.Main) {
                    val finalLogs = messages[index].terminalLogs + "\n[✓ Process Completed]"
                    messages[index] = messages[index].copy(isExecuting = false, terminalLogs = finalLogs)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errLogs = messages[index].terminalLogs + "\n[x Error: ${e.message}]"
                    messages[index] = messages[index].copy(isExecuting = false, terminalLogs = errLogs)
                }
            }
        }
    }

    fun killTask(index: Int, taskId: String) {
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
                    val killedLogs = messages[index].terminalLogs + "\n[⏹ Terminated by Boss - SIGKILL]"
                    messages[index] = messages[index].copy(isExecuting = false, terminalLogs = killedLogs)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "கில் தோல்வி", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STARK MISSION CONTROL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("6 Agents & PTY Core Active", color = Color(0xFF48CAE4), fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B)),
                actions = {
                    IconButton(onClick = { messages.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.White)
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
            itemsIndexed(messages) { index, msg ->
                if (msg.tacticalData != null) {
                    TacticalCardItem(
                        msg = msg,
                        onExecute = { script -> executeTerminalTask(index, script, msg.tacticalData.taskId) },
                        onKill = { killTask(index, msg.tacticalData.taskId) },
                        onSpeak = { text -> onSpeak(text) }
                    )
                } else {
                    StandardMessageItem(msg = msg, onSpeak = { onSpeak(msg.text) })
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

@Composable
fun TacticalCardItem(
    msg: ConsoleMessage,
    onExecute: (String) -> Unit,
    onKill: () -> Unit,
    onSpeak: (String) -> Unit
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
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Voice",
                    tint = Color(0xFF48CAE4),
                    modifier = Modifier.size(20.dp).clickable { onSpeak(tactical.karenVoice) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("JARVIS: ${tactical.jarvisBrief}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            val severityColor = if (tactical.ultronSeverity == "CRITICAL") Color(0xFFEF4444) else Color(0xFF10B981)

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
fun StandardMessageItem(msg: ConsoleMessage, onSpeak: () -> Unit) {
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
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Voice",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp).size(18.dp).clickable { onSpeak() }
            )
        }
    }
}
