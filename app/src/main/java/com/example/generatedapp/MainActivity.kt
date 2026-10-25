package com.example.generatedapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isVoiceActive = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            StarkMasterUI(
                onSpeak = { txt -> speak(txt) },
                onStopSpeak = { stopSpeech() },
                isSpeaking = isVoiceActive.value
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ta", "IN")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { isVoiceActive.value = true }
                override fun onDone(id: String?) { isVoiceActive.value = false }
                override fun onError(id: String?) { isVoiceActive.value = false }
            })
        }
    }

    private fun speak(txt: String) {
        tts?.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "STARK_VOICE")
        isVoiceActive.value = true
    }

    private fun stopSpeech() {
        tts?.stop()
        isVoiceActive.value = false
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarkMasterUI(onSpeak: (String) -> Unit, onStopSpeak: () -> Unit, isSpeaking: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val sessions = remember { mutableStateListOf<ChatSession>() }
    var currentSessionId by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ConsoleMessage>() }

    var inputText by remember { mutableStateOf("") }
    var isPlanning by remember { mutableStateOf(false) }
    var showPeek by remember { mutableStateOf(false) }
    var livePtyLogs by remember { mutableStateOf("") }
    var activeTaskId by remember { mutableStateOf<String?>(null) }

    fun sync() {
        val i = sessions.indexOfFirst { it.id == currentSessionId }
        if (i != -1) {
            sessions[i] = sessions[i].copy(messages = messages.toList(), lastUpdated = System.currentTimeMillis())
        }
        StarkSessionManager.save(context, sessions.toList())
    }

    LaunchedEffect(Unit) {
        val saved = StarkSessionManager.load(context)
        if (saved.isNotEmpty()) {
            sessions.clear()
            sessions.addAll(saved)
            currentSessionId = saved.first().id
            messages.clear()
            messages.addAll(saved.first().messages)
        } else {
            val defId = UUID.randomUUID().toString()
            val init = ConsoleMessage(sender = "JARVIS", text = "வணக்கம் பாஸ்! ஸ்டார்க் கவுன்சில் தயார்.")
            val def = ChatSession(defId, "Project 1", listOf(init))
            sessions.add(def)
            currentSessionId = defId
            messages.add(init)
            sync()
        }
    }

    fun switchSession(id: String) {
        sync()
        val s = sessions.firstOrNull { it.id == id } ?: return
        currentSessionId = s.id
        messages.clear()
        messages.addAll(s.messages)
        scope.launch { drawerState.close() }
    }

    fun newSession() {
        sync()
        val nId = UUID.randomUUID().toString()
        val init = ConsoleMessage(sender = "JARVIS", text = "புதிய பிராஜெக்ட் தயார் பாஸ்!")
        val s = ChatSession(nId, "Project ${sessions.size + 1}", listOf(init))
        sessions.add(0, s)
        currentSessionId = nId
        messages.clear()
        messages.addAll(listOf(init))
        sync()
        scope.launch { drawerState.close() }
    }

    fun send(cmd: String) {
        if (cmd.isBlank()) return
        messages.add(ConsoleMessage(sender = "BOSS", text = cmd, isBoss = true))
        inputText = ""
        isPlanning = true
        sync()

        scope.launch {
            val res = StarkClient.requestPlan(currentSessionId, cmd)
            isPlanning = false
            if (res != null) {
                val a = res.optJSONObject("ultron_audit") ?: JSONObject()
                val p = res.optJSONObject("patches") ?: JSONObject()
                val tac = TacticalPayload(
                    res.optString("task_id", "t_${System.currentTimeMillis()}"),
                    res.optString("jarvis_brief", "திட்டம் தயார்."),
                    a.optString("severity", "SAFE"), a.optString("flaw", "-"),
                    a.optString("impact", "-"), p.optString("safe_patch", cmd),
                    p.optString("force_bypass", cmd), res.optString("karen_voice", "")
                )
                messages.add(ConsoleMessage(sender = "COUNCIL", tacticalData = tac))
                sync()
                if (tac.karenVoice.isNotBlank()) onSpeak(tac.karenVoice)
            } else {
                messages.add(ConsoleMessage(sender = "FRIDAY", text = "சர்வர் இணைப்பு கிடைக்கவில்லை."))
                sync()
            }
        }
    }

    fun exec(idx: Int, script: String, taskId: String) {
        activeTaskId = taskId
        livePtyLogs = "> Initializing PTY Shell...\n$ $script\n"
        messages[idx] = messages[idx].copy(isExecuting = true, terminalLogs = livePtyLogs)
        sync()

        scope.launch {
            StarkClient.executePtyStream(taskId, script) { chunk ->
                livePtyLogs += chunk
                val lines = livePtyLogs.lines()
                val tr = if (lines.size > 150) lines.takeLast(150).joinToString("\n") else livePtyLogs
                messages[idx] = messages[idx].copy(terminalLogs = tr)
            }
            messages[idx] = messages[idx].copy(isExecuting = false)
            activeTaskId = null
            sync()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0B132B), modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { newSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("புதிய பிராஜெக்ட் (New Chat)", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn {
                    itemsIndexed(sessions) { _, s ->
                        NavigationDrawerItem(
                            label = { Text(s.title, color = Color.White) },
                            selected = s.id == currentSessionId,
                            onClick = { switchSession(s.id) },
                            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0xFF1C2541)),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("STARK MISSION CONTROL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Drawer", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B)),
                    actions = {
                        if (activeTaskId != null || livePtyLogs.isNotBlank()) {
                            IconButton(onClick = { showPeek = true }) {
                                Icon(Icons.Default.Dvr, contentDescription = "Peek", tint = Color(0xFF10B981))
                            }
                        }
                        if (isSpeaking) {
                            IconButton(onClick = onStopSpeak) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Stop Voice", tint = Color(0xFFEF4444))
                            }
                        }
                        IconButton(onClick = { messages.clear(); sync() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                )
            },
            bottomBar = {
                Surface(color = Color(0xFF0B132B), modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF1C2541)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("கட்டளையிடுங்கள் பாஸ்...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { send(inputText) }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF0077B6))
                        }
                    }
                }
            },
            containerColor = Color(0xFF060A17)
        ) { pad ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(messages) { i, m ->
                    if (m.tacticalData != null) {
                        TacticalItem(
                            m = m,
                            onExec = { s -> exec(i, s, m.tacticalData.taskId) },
                            onKill = {
                                scope.launch {
                                    StarkClient.killRunningProcess(m.tacticalData.taskId)
                                    messages[i] = messages[i].copy(isExecuting = false)
                                    sync()
                                }
                            },
                            onLike = { lk -> messages[i] = messages[i].copy(isLiked = lk); sync() },
                            onBranch = {
                                val nS = ChatSession(title = "Branch: ${m.tacticalData.taskId}", messages = listOf(m))
                                sessions.add(0, nS)
                                switchSession(nS.id)
                            },
                            onAudit = { logs -> send("இந்த டெர்மினல் அவுட்புட் சரியா என ஆடிட் செய்:\n$logs") },
                            onSpeak = onSpeak
                        )
                    } else {
                        StandardItem(
                            m = m,
                            onLike = { lk -> messages[i] = messages[i].copy(isLiked = lk); sync() },
                            onBranch = {
                                val nS = ChatSession(title = "Branch: ${m.text.take(12)}", messages = listOf(m))
                                sessions.add(0, nS)
                                switchSession(nS.id)
                            },
                            onSpeak = onSpeak
                        )
                    }
                }
                if (isPlanning) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF48CAE4), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("JARVIS & ULTRON திட்டமிடுகின்றனர்...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showPeek) {
        ModalBottomSheet(onDismissRequest = { showPeek = false }, containerColor = Color(0xFF0B132B)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("🖥️ நேரலை டெர்மினல் (Live Peek)", color = Color.White, fontWeight = FontWeight.Bold)
                Surface(color = Color.Black, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp)) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item { Text(livePtyLogs.ifBlank { "> Waiting for output..." }, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    }
                }
                Button(
                    onClick = { showPeek = false; send("இந்த டெர்மினல் அவுட்புட் சரியா என ஆடிட் செய்:\n$livePtyLogs") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔍 இந்த அவுட்புட்டை AI கொண்டு சரிபார் (Audit)") }
            }
        }
    }
}

@Composable
fun TacticalItem(
    m: ConsoleMessage,
    onExec: (String) -> Unit,
    onKill: () -> Unit,
    onLike: (Boolean) -> Unit,
    onBranch: () -> Unit,
    onAudit: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val tac = m.tacticalData ?: return
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var showTerm by remember { mutableStateOf(false) }

    Surface(color = Color(0xFF111D4A), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🏛️ STARK MISSION COUNCIL", color = Color(0xFF48CAE4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF48CAE4), modifier = Modifier.size(18.dp).clickable { onSpeak(tac.karenVoice) })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(tac.jarvisBrief, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            val cmd = if (tab == 0) tac.safePatch else tac.forceBypass
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { tab = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tab == 0) Color(0xFF0077B6) else Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(6.dp)
                ) { Text("🛡️ Safe Patch", fontSize = 11.sp) }
                Button(
                    onClick = { tab = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tab == 1) Color(0xFFDC2626) else Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(6.dp)
                ) { Text("⚠️ Raw Bypass", fontSize = 11.sp) }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Surface(color = Color(0xFF060A17), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text(cmd, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showTerm = true; onExec(cmd) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (tab == 0) Color(0xFF10B981) else Color(0xFFEF4444)),
                    modifier = Modifier.weight(1f),
                    enabled = !m.isExecuting
                ) { Text("⚡ இயக்கு", fontSize = 12.sp) }
                if (m.isExecuting) {
                    Button(onClick = onKill, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    }
                }
            }

            AnimatedVisibility(visible = showTerm) {
                Surface(color = Color.Black, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(m.terminalLogs.ifBlank { "> Running..." }, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        if (!m.isExecuting && m.terminalLogs.isNotBlank()) {
                            Button(
                                onClick = { onAudit(m.terminalLogs) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().height(30.dp)
                            ) { Text("🔄 அவுட்புட்டை சரிபார் (Audit)", fontSize = 10.sp, color = Color(0xFF48CAE4)) }
                        }
                    }
                }
            }

            Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp).clickable {
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Cmd", cmd))
                        Toast.makeText(ctx, "நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                    })
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp).clickable {
                        val s = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, cmd) }
                        ctx.startActivity(Intent.createChooser(s, "Share"))
                    })
                    Icon(Icons.Default.ForkRight, contentDescription = null, tint = Color(0xFF48CAE4), modifier = Modifier.size(16.dp).clickable { onBranch() })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = if (m.isLiked == true) Color(0xFF10B981) else Color(0xFF64748B), modifier = Modifier.size(16.dp).clickable { onLike(true) })
                    Icon(Icons.Default.ThumbDown, contentDescription = null, tint = if (m.isLiked == false) Color(0xFFEF4444) else Color(0xFF64748B), modifier = Modifier.size(16.dp).clickable { onLike(false) })
                }
            }
        }
    }
}

@Composable
fun StandardItem(m: ConsoleMessage, onLike: (Boolean) -> Unit, onBranch: () -> Unit, onSpeak: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (m.isBoss) Alignment.End else Alignment.Start) {
        Surface(
            color = if (m.isBoss) Color(0xFF0077B6) else Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(m.text, color = Color.White, fontSize = 13.sp)
                if (!m.isBoss) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp).clickable { onSpeak(m.text) })
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp).clickable {
                                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Text", m.text))
                            })
                            Icon(Icons.Default.ForkRight, contentDescription = null, tint = Color(0xFF48CAE4), modifier = Modifier.size(14.dp).clickable { onBranch() })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = if (m.isLiked == true) Color(0xFF10B981) else Color(0xFF64748B), modifier = Modifier.size(14.dp).clickable { onLike(true) })
                            Icon(Icons.Default.ThumbDown, contentDescription = null, tint = if (m.isLiked == false) Color(0xFFEF4444) else Color(0xFF64748B), modifier = Modifier.size(14.dp).clickable { onLike(false) })
                        }
                    }
                }
            }
        }
    }
}
