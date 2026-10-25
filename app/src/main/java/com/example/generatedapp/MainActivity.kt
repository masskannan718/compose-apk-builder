package com.example.generatedapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
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
    var selectedVoicePitch by mutableStateOf(1.0f)
    var selectedVoiceSpeed by mutableStateOf(1.0f)

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
                GeminiCompleteApp(
                    onSpeak = { text -> speakOut(text) },
                    onVoiceChange = { pitch, speed ->
                        selectedVoicePitch = pitch
                        selectedVoiceSpeed = speed
                        tts?.setPitch(pitch)
                        tts?.setSpeechRate(speed)
                    }
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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiCompleteApp(onSpeak: (String) -> Unit, onVoiceChange: (Float, Float) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("chat") } // chat, library, scheduled, settings
    var showTopMenu by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("Gemini Flash") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF9F9FB),
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gemini", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F1F1F))
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1F1F1F))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer Items
                    DrawerNavRow(Icons.Outlined.Edit, "New chat") {
                        currentScreen = "chat"
                        scope.launch { drawerState.close() }
                    }
                    DrawerNavRow(Icons.Outlined.Folder, "Library") {
                        currentScreen = "library"
                        scope.launch { drawerState.close() }
                    }
                    DrawerNavRow(Icons.Outlined.Image, "Images") { scope.launch { drawerState.close() } }
                    DrawerNavRow(Icons.Outlined.Schedule, "Scheduled") {
                        currentScreen = "scheduled"
                        scope.launch { drawerState.close() }
                    }
                    DrawerNavRow(Icons.Outlined.School, "Students") { scope.launch { drawerState.close() } }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E2E2))

                    Text("Notebooks", fontSize = 13.sp, color = Color(0xFF747775), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    DrawerNavRow(Icons.Outlined.Add, "New notebook") {}

                    Spacer(modifier = Modifier.weight(1f))

                    // User Profile & Settings Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentScreen = "settings"
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mass Kannan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1F1F1F))
                            Text("PRO", fontSize = 11.sp, color = Color(0xFF0B57D0), fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color(0xFF747775))
                    }
                }
            }
        }
    ) {
        when (currentScreen) {
            "chat" -> GeminiChatScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } },
                selectedModel = selectedModel,
                onModelSelect = { selectedModel = it },
                onSpeak = onSpeak
            )
            "library" -> GeminiLibraryScreen(onBack = { currentScreen = "chat" })
            "scheduled" -> GeminiScheduledScreen(onBack = { currentScreen = "chat" })
            "settings" -> GeminiSettingsScreen(
                onBack = { currentScreen = "chat" },
                onVoiceChange = onVoiceChange
            )
        }
    }
}

@Composable
fun DrawerNavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF444746), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 14.sp, color = Color(0xFF1F1F1F), fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    onOpenDrawer: () -> Unit,
    selectedModel: String,
    onModelSelect: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showModelMenu = true }
                        ) {
                            Text(selectedModel, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Color(0xFF1F1F1F))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                            DropdownMenuItem(text = { Text("Gemini Flash") }, onClick = { onModelSelect("Gemini Flash"); showModelMenu = false })
                            DropdownMenuItem(text = { Text("Gemini Pro 1.5") }, onClick = { onModelSelect("Gemini Pro 1.5"); showModelMenu = false })
                            DropdownMenuItem(text = { Text("STARK Council v5") }, onClick = { onModelSelect("STARK Council v5"); showModelMenu = false })
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1F1F1F))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color(0xFF1F1F1F))
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF1F1F1F))
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            DropdownMenuItem(text = { Text("Share conversation") }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { showTopMenu = false })
                            DropdownMenuItem(text = { Text("Pin") }, leadingIcon = { Icon(Icons.Outlined.PushPin, null) }, onClick = { showTopMenu = false })
                            DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { showTopMenu = false })
                            DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { messages.clear(); showTopMenu = false })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        },
        bottomBar = {
            // Floating Pill Input Bar
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
                    shadowElevation = 3.dp,
                    border = null,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color(0xFF444746))
                        }
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Gemini", color = Color(0xFF747775), fontSize = 15.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Voice", tint = Color(0xFF444746))
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3E3FD))
                                .clickable {
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

                                                val res = input.readLine() ?: "{}"
                                                s.close()
                                                val json = JSONObject(res)

                                                val brief = json.optString("jarvis_brief", "கட்டளை முடிக்கப்பட்டது பாஸ்.")
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
                                                    messages.add(ChatMessage(sender = "gemini", text = "பாஸ், சாக்கெட் சர்வர் தொடர்பு கிடைக்கவில்லை: ${e.localizedMessage}"))
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("||", fontWeight = FontWeight.Bold, color = Color(0xFF041E49), fontSize = 14.sp)
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
            item { Spacer(modifier = Modifier.height(6.dp)) }
            items(messages) { msg ->
                if (msg.sender == "user") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE8F0FE),
                            modifier = Modifier.widthIn(max = 290.dp)
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

                        // Clean Black Terminal Code Box
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Retry", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.text)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF747775), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (msg.voiceSummary.isNotBlank()) onSpeak(msg.voiceSummary) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.VolumeUp, contentDescription = "Listen", tint = Color(0xFF1F1F1F), modifier = Modifier.size(20.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLibraryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            Text("Documents", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F1F1F))
            Spacer(modifier = Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF0F4F9), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF1F1F1F))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Master Server Blueprint V5", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Oct 25, 2026, 6:40 PM", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiScheduledScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            Text("Suggested Routines", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            listOf("Organize my subscriptions", "Recommend local tech events", "Draft daily git commit log").forEach { title ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF0F4F9), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(title, modifier = Modifier.padding(16.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(onBack: () -> Unit, onVoiceChange: (Float, Float) -> Unit) {
    var showVoiceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9FB))
            )
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            Text("Preferences", fontSize = 13.sp, color = Color(0xFF747775), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem("Voice", "Change voice tone and persona") { showVoiceDialog = true }
            SettingsItem("Theme", "System default") {}
            SettingsItem("Skills", "Manage automated AI capabilities") {}
            SettingsItem("Personal Intelligence", "On") {}

            if (showVoiceDialog) {
                AlertDialog(
                    onDismissRequest = { showVoiceDialog = false },
                    title = { Text("குரலைத் தேர்ந்தெடுக்கவும் (Voice Persona)") },
                    text = {
                        Column {
                            Text("1. KAREN (மென்மையான பெண் குரல்)", modifier = Modifier.clickable {
                                onVoiceChange(1.1f, 1.0f); showVoiceDialog = false
                            }.padding(vertical = 8.dp))
                            Text("2. JARVIS (கம்பீரமான ஆண் குரல்)", modifier = Modifier.clickable {
                                onVoiceChange(0.7f, 0.95f); showVoiceDialog = false
                            }.padding(vertical = 8.dp))
                            Text("3. FRIDAY (இயற்கையான வழிகாட்டி குரல்)", modifier = Modifier.clickable {
                                onVoiceChange(1.0f, 1.05f); showVoiceDialog = false
                            }.padding(vertical = 8.dp))
                            Text("4. STARK (வேகமான பாஸ் வாய்ஸ்)", modifier = Modifier.clickable {
                                onVoiceChange(0.85f, 1.15f); showVoiceDialog = false
                            }.padding(vertical = 8.dp))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showVoiceDialog = false }) { Text("சரி") }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F1F1F))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF747775))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF747775))
    }
}
