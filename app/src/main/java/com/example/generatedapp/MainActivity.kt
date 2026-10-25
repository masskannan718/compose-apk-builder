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
    var isSpeakingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            StarkMasterWorkspace(
                onSpeak = { text -> speak(text) },
                onPauseSpeech = { stopSpeech() },
                isSpeaking = isSpeakingState.value
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

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "STARK_VOICE")
        isSpeakingState.value = true
    }

    private fun stopSpeech() {
        tts?.stop()
        isSpeakingState.value = false
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarkMasterWorkspace(
    onSpeak: (String) -> Unit,
    onPauseSpeech: () -> Unit,
    isSpeaking: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessions = remember { mutableStateListOf<ChatSession>() }
    var currentSessionId by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ConsoleMessage>() }

    var inputText by remember { mutableStateOf("") }
    var isPlanning by remember { mutableStateOf(false) }

    // Live Peek State
    var showPeekSheet by remember { mutableStateOf(false) }
    var activeExecutingTaskId by remember { mutableStateOf<String?>(null) }
    var globalLiveLogs by remember { mutableStateOf("") }

    // Persistence Helper
    fun persist() {
        val idx = sessions.indexOfFirst { it.id == currentSessionId }
        if (idx != -1) {
            sessions[idx] = sessions[idx].copy(messages = messages.toList(), lastUpdated = System.currentTimeMillis())
        }
        StarkSessionManager.saveSessions(context, sessions.toList())
    }

    // Initialize Sessions
    LaunchedEffect(Unit) {
        val loaded = StarkSessionManager.loadSessions(context)
        if (loaded.isNotEmpty()) {
            sessions.clear()
            sessions.addAll(loaded)
            val first = loaded.first()
            currentSessionId = first.id
            messages.clear()
            messages.addAll(first.messages)
        } else {
            val defId = UUID.randomUUID().toString()
            val initMsg = ConsoleMessage(
                sender = "JARVIS",
                text = "வணக்கம் பாஸ்! ஸ்டார்க் ப்ரோ ஒர்க்ஸ்பேஸ் தயாராக உள்ளது. எதை ஆரம்பிக்கலாம்?"
            )
            val defSession = ChatSession(id = defId, title = "Project 01", messages = listOf(initMsg))
            sessions.add(defSession)
            currentSessionId = defId
            messages.add(initMsg)
            persist()
        }
    }

    fun switchSession(targetId: String) {
        persist()
        val s = sessions.firstOrNull { it.id == targetId } ?: return
        currentSessionId = s.id
        messages.clear()
        messages.addAll(s.messages)
        coroutineScope.launch { drawerState.close() }
    }

    fun createNewSession() {
        persist()
        val newId = UUID.randomUUID().toString()
        val newTitle = "Project ${sessions.size + 1}"
        val initMsg = ConsoleMessage(sender = "JARVIS", text = "புதிய திட்டம் தயார் பாஸ். கட்டளையிடுங்கள்!")
        val newSession = ChatSession(id = newId, title = newTitle, messages = listOf(initMsg))
        sessions.add(0, newSession)
        currentSessionId = newId
        messages.clear()
        messages.addAll(listOf(initMsg))
        persist()
        coroutineScope.launch { drawerState.close() }
    }

    fun submitCommand(cmd: String) {
        if (cmd.isBlank()) return
        val userMsg = ConsoleMessage(sender = "BOSS", text = cmd, isBoss = true)
        messages.add(userMsg)
        inputText = ""
        isPlanning = true
        persist()

        coroutineScope.launch {
            val json = StarkClient.requestPlan(cmd)
            isPlanning = false

            if (json != null) {
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
                    karenVoice = json.optString("karen_voice", "கட்டளை தயார் பாஸ்.")
                )
                messages.add(ConsoleMessage(sender = "COUNCIL", tacticalData = tactical))
                persist()
                if (tactical.karenVoice.isNotBlank()) {
                    onSpeak(tactical.karenVoice)
                }
            } else {
                messages.add(ConsoleMessage(sender = "FRIDAY", text = "போர்ட் 8080 ஆஃப்லைன். stark_core_server.py இயங்குகிறதா என சரிபார்க்கவும்."))
                persist()
            }
        }
    }

    fun executeTerminalTask(index: Int, script: String, taskId: String) {
        activeExecutingTaskId = taskId
        globalLiveLogs = "> Initializing PTY Shell...\n$ $script\n"
        messages[index] = messages[index].copy(isExecuting = true, terminalLogs = globalLiveLogs)
        persist()

        coroutineScope.launch {
            StarkClient.executePtyStream(taskId, script) { chunk ->
                globalLiveLogs += chunk
                val lines = globalLiveLogs.lines()
                val trimmed = if (lines.size > 200) lines.takeLast(200).joinToString("\n") else globalLiveLogs
                messages[index] = messages[index].copy(terminalLogs = trimmed)
            }
            messages[index] = messages[index].copy(isExecuting = false)
            activeExecutingTaskId = null
            persist()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0B132B), modifier = Modifier.width(310.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { createNewSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("புதிய திட்டம் (New Project)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("செயலில் உள்ள பிராஜெக்ட்கள்", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    itemsIndexed(sessions) { _, s ->
                        val isSelected = s.id == currentSessionId
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = if (isSelected) Color(0xFF48CAE4) else Color(0xFF64748B)) },
                            label = { Text(s.title, color = if (isSelected) Color.White else Color(0xFFCBD5E1), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            selected = isSelected,
                            onClick = { switchSession(s.id) },
                            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0xFF1C2541)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("STARK MISSION CONTROL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                if (activeExecutingTaskId != null) "⚡ Background Task Running..." else "6 AI Council Online",
                                color = if (activeExecutingTaskId != null) Color(0xFF10B981) else Color(0xFF48CAE4),
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B)),
                    actions = {
                        // Quick Peek Live Output Button
                        if (activeExecutingTaskId != null || globalLiveLogs.isNotBlank()) {
                            IconButton(onClick = { showPeekSheet = true }) {
                                Icon(Icons.Default.Dvr, contentDescription = "Peek", tint = Color(0xFF10B981))
                            }
                        }

                        // Audio Deck Controls
                        if (isSpeaking) {
                            IconButton(onClick = onPauseSpeech) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Stop Voice", tint = Color(0xFFEF4444))
                            }
                        }

                        IconButton(onClick = {
                            messages.clear()
                            persist()
                        }) {
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
                        TacticalCardView(
                            msg = msg,
                            onExecute = { script -> executeTerminalTask(index, script, msg.tacticalData.taskId) },
                            onKill = {
                                coroutineScope.launch {
                                    StarkClient.killRunningProcess(msg.tacticalData.taskId)
                                    messages[index] = messages[index].copy(isExecuting = false, terminalLogs = messages[index].terminalLogs + "\n[⏹ Terminated by Boss]")
                                    persist()
                                }
                            },
                            onAuditRoutine = { logs ->
                                submitCommand("இந்த டெர்மினல் அவுட்புட் சரியாக உள்ளதா என 100% ஆடிட் செய்:\n$logs")
                            },
                            onLike = { isLike ->
                                messages[index] = messages[index].copy(isLiked = isLike)
                                persist()
                            },
                            onBranch = {
                                val branchTitle = "Branch from ${msg.tacticalData.taskId}"
                                val newSession = ChatSession(title = branchTitle, messages = listOf(msg))
                                sessions.add(0, newSession)
                                switchSession(newSession.id)
                            },
                            onSpeak = { text -> onSpeak(text) }
                        )
                    } else {
                        StandardMessageView(
                            msg = msg,
                            onLike = { isLike ->
                                messages[index] = messages[index].copy(isLiked = isLike)
                                persist()
                            },
                            onBranch = {
                                val branchTitle = "Branch - ${msg.text.take(15)}"
                                val newSession = ChatSession(title = branchTitle, messages = listOf(msg))
                                sessions.add(0, newSession)
                                switchSession(newSession.id)
                            },
                            onSpeak = { onSpeak(msg.text) }
                        )
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

    // Live Peek Bottom Sheet Modal
    if (showPeekSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPeekSheet = false },
            containerColor = Color(0xFF0B132B)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🖥️ நேரலை டெர்மினல் பார்வை (Live Peek)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = { showPeekSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp).padding(vertical = 8.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = globalLiveLogs.ifBlank { "> Waiting for live background stream..." },
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        showPeekSheet = false
                        submitCommand("இந்த நேரலை அவுட்புட் சரியாக உள்ளதா என 100% ஆடிட் செய்:\n$globalLiveLogs")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔍 இந்த அவுட்புட்டை AI கொண்டு ஆடிட் செய் (Verify Routine)")
                }
            }
        }
    }
}

@Composable
fun TacticalCardView(
    msg: ConsoleMessage,
    onExecute: (String) -> Unit,
    onKill: () -> Unit,
    onAuditRoutine: (String) -> Unit,
    onLike: (Boolean) -> Unit,
    onBranch: () -> Unit,
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
                IconButton(onClick = { onSpeak(tactical.karenVoice) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color(0xFF48CAE4), modifier = Modifier.size(18.dp))
                }
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

                        if (!msg.isExecuting && msg.terminalLogs.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onAuditRoutine(msg.terminalLogs) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Text("🔄 இந்த அவுட்புட்டை சரிபார் (Audit Routine)", fontSize = 11.sp, color = Color(0xFF48CAE4))
                            }
                        }
                    }
                }
            }

            // Message Action Bar (Copy, Share, Like, Dislike, Branch)
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Copy
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp).clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Card", "${tactical.jarvisBrief}\n${tactical.safePatch}"))
                            Toast.makeText(context, "கார்டு நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    // Share
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp).clickable {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "STARK TASK:\n${tactical.jarvisBrief}\n\nSafe Patch:\n${tactical.safePatch}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "பகிரவும்"))
                        }
                    )
                    // Branch
                    Icon(
                        Icons.Default.ForkRight,
                        contentDescription = "Branch",
                        tint = Color(0xFF48CAE4),
                        modifier = Modifier.size(18.dp).clickable { onBranch() }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Like
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (msg.isLiked == true) Color(0xFF10B981) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp).clickable { onLike(true) }
                    )
                    // Dislike
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (msg.isLiked == false) Color(0xFFEF4444) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp).clickable { onLike(false) }
                    )
                }
            }
        }
    }
}

@Composable
fun StandardMessageView(
    msg: ConsoleMessage,
    onLike: (Boolean) -> Unit,
    onBranch: () -> Unit,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
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

                if (!msg.isBoss) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Voice",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp).clickable { onSpeak() }
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp).clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Text", msg.text))
                                    Toast.makeText(context, "நகலெடுக்கப்பட்டது!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Icon(
                                Icons.Default.ForkRight,
                                contentDescription = "Branch",
                                tint = Color(0xFF48CAE4),
                                modifier = Modifier.size(16.dp).clickable { onBranch() }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = if (msg.isLiked == true) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp).clickable { onLike(true) }
                            )
                            Icon(
                                Icons.Default.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (msg.isLiked == false) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp).clickable { onLike(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
