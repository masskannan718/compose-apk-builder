package com.example.generatedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class CouncilMessage(
    val agent: String,
    val message: String,
    val color: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                SuperConsoleScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperConsoleScreen() {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var currentStage by remember { mutableStateOf("Stage 0: Ready") }
    var isMuted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showApprovalGate by remember { mutableStateOf(false) }
    var pendingActionId by remember { mutableStateOf("") }
    var summaryAction by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            CouncilMessage("JARVIS", "வணக்கம் பாஸ்! ஸ்டார்க் கவுன்சில் தயார் நிலையில் உள்ளது. உங்கள் கட்டளையைத் தெரிவிக்கலாம்.", Color(0xFF38BDF8)),
            CouncilMessage("FRIDAY", "பாஸ், ஆடியோ மற்றும் பாதுகாப்பு கட்டுப்பாடுகள் அனைத்தும் இணைக்கப்பட்டுள்ளன.", Color(0xFF4ADE80))
        )
    }

    fun sendDebateRequest(userQuery: String) {
        if (userQuery.isBlank()) return
        isLoading = true
        currentStage = "Stage 2: Council Debating..."
        messages.add(CouncilMessage("BOSS", userQuery, Color(0xFFFBBF24)))

        coroutineScope.launch {
            try {
                val resultJson = withContext(Dispatchers.IO) {
                    val url = URL("http://127.0.0.1:8080/api/council/debate")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        doOutput = true
                        connectTimeout = 60000
                        readTimeout = 60000
                    }

                    val jsonBody = JSONObject().apply {
                        put("query", userQuery)
                    }

                    OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(responseText)
                    } else {
                        null
                    }
                }

                if (resultJson != null) {
                    val discussions = resultJson.getJSONArray("discussions")
                    for (i in 0 until discussions.length()) {
                        val item = discussions.getJSONObject(i)
                        val agent = item.getString("agent")
                        val msg = item.getString("message")
                        val color = when (agent) {
                            "JARVIS" -> Color(0xFF38BDF8)
                            "VERONICA" -> Color(0xFFE879F9)
                            "HOMER" -> Color(0xFFFACC15)
                            "ULTRON" -> Color(0xFFF87171)
                            "FRIDAY" -> Color(0xFF4ADE80)
                            else -> Color.White
                        }
                        messages.add(CouncilMessage(agent, msg, color))
                    }

                    pendingActionId = resultJson.optString("action_id", "")
                    summaryAction = resultJson.optString("summary_action", "பணிகள் தயார்")
                    currentStage = "Stage 4: Awaiting Approval"
                    showApprovalGate = true
                } else {
                    messages.add(CouncilMessage("SYSTEM", "சர்வர் பதில் அளிக்கவில்லை (Check Server)", Color.Red))
                }
            } catch (e: Exception) {
                messages.add(CouncilMessage("SYSTEM", "இணைப்புப் பிழை: ${e.localizedMessage}", Color.Red))
            } finally {
                isLoading = false
            }
        }
    }

    fun sendApproval(approved: Boolean) {
        coroutineScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val url = URL("http://127.0.0.1:8080/api/council/approve")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        doOutput = true
                    }

                    val jsonBody = JSONObject().apply {
                        put("action_id", pendingActionId)
                        put("approved", approved)
                    }

                    OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(responseText)
                    } else {
                        null
                    }
                }

                if (res != null) {
                    val statusMsg = res.getString("message")
                    currentStage = res.getString("stage")
                    messages.add(CouncilMessage("FRIDAY", statusMsg, Color(0xFF4ADE80)))
                    showApprovalGate = false
                }
            } catch (e: Exception) {
                messages.add(CouncilMessage("SYSTEM", "அனுமதிப் பிழை: ${e.localizedMessage}", Color.Red))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SUPER CONSOLE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(currentStage, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                },
                actions = {
                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = if (isMuted) Color.Red else Color.Green
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF0F172A)).padding(8.dp)) {
                if (showApprovalGate) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("இறுதி உறுதிப்படுத்தல்:", fontWeight = FontWeight.Bold, color = Color.Yellow)
                            Text(summaryAction, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(
                                    onClick = { sendApproval(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("Approve & Execute")
                                }
                                Button(
                                    onClick = { sendApproval(false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* File Attachment Picker */ }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Attach File", tint = Color(0xFF38BDF8))
                    }
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("தமிழில் கட்டளையிடவும்...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val text = inputText
                            inputText = ""
                            sendDebateRequest(text)
                        },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF38BDF8))
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120))
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.agent == "BOSS") Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.agent == "BOSS") Color(0xFF1E3A8A) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = msg.agent,
                                color = msg.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.message,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Text(
                        "AI கவுன்சில் விவாதிக்கிறது...",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
