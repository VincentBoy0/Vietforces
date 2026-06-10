package com.example.vietforces.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.model.AiMistake
import com.example.vietforces.data.model.ChatMessage
import com.example.vietforces.data.model.RoleplayScenario
import com.example.vietforces.data.model.RoleplayScenarios
import com.example.vietforces.data.storage.PreferencesManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayScreen(
    onBackClick: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<RoleplayScenario?>(null) }

    val current = selected
    if (current == null) {
        ScenarioPicker(onBackClick = onBackClick, onPick = { selected = it })
    } else {
        ChatView(
            scenario = current,
            onBackClick = { selected = null }
        )
    }
}

// =====================================================================
//  Scenario picker
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioPicker(
    onBackClick: () -> Unit,
    onPick: (RoleplayScenario) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = { Text("Nhập vai trò chuyện", fontWeight = FontWeight.Bold, color = VietRed) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VietRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Chọn một tình huống để luyện nói tiếng Việt với AI 🤖",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            RoleplayScenarios.categories.forEach { category ->
                item {
                    Text(
                        category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(RoleplayScenarios.all.filter { it.category == category }) { sc ->
                    ScenarioCard(sc, onClick = { onPick(sc) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ScenarioCard(sc: RoleplayScenario, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VietYellow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(sc.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sc.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("🎯 ${sc.goal}", fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

// A chat message plus, for the learner's turns, the tutor's corrections.
private data class UiMsg(
    val role: String,
    val content: String,
    val corrections: List<AiMistake> = emptyList()
)

// ---- Persistence (resume a saved conversation) ----

private fun serializeSession(messages: List<UiMsg>, metGoals: Set<String>): String {
    val msgArr = JSONArray()
    messages.forEach { m ->
        val o = JSONObject()
            .put("role", m.role)
            .put("content", m.content)
        if (m.corrections.isNotEmpty()) {
            val ca = JSONArray()
            m.corrections.forEach { c ->
                ca.put(
                    JSONObject()
                        .put("type", c.type)
                        .put("text", c.text)
                        .put("suggestion", c.suggestion)
                        .put("explanation", c.explanation)
                )
            }
            o.put("corrections", ca)
        }
        msgArr.put(o)
    }
    val goalArr = JSONArray()
    metGoals.forEach { goalArr.put(it) }
    return JSONObject().put("messages", msgArr).put("metGoals", goalArr).toString()
}

private fun parseSession(json: String?): Pair<List<UiMsg>, Set<String>>? {
    if (json.isNullOrBlank()) return null
    return try {
        val root = JSONObject(json)
        val arr = root.optJSONArray("messages") ?: return null
        val msgs = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val corr = o.optJSONArray("corrections")?.let { ca ->
                (0 until ca.length()).map { j ->
                    val c = ca.getJSONObject(j)
                    AiMistake(
                        type = c.optString("type"),
                        text = c.optString("text"),
                        suggestion = c.optString("suggestion"),
                        explanation = c.optString("explanation")
                    )
                }
            } ?: emptyList()
            UiMsg(o.optString("role"), o.optString("content"), corr)
        }
        val goals = root.optJSONArray("metGoals")?.let { g ->
            (0 until g.length()).map { g.optString(it) }.toSet()
        } ?: emptySet()
        if (msgs.isEmpty()) null else msgs to goals
    } catch (e: Exception) {
        null
    }
}

// =====================================================================
//  Chat view
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatView(
    scenario: RoleplayScenario,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Restore a saved conversation for this scenario (resume where we left off).
    val restored = remember(scenario.id) {
        parseSession(PreferencesManager.getRoleplaySession(scenario.id))
    }

    // Conversation shown to the user (assistant/user turns; no system message).
    var messages by remember(scenario.id) {
        mutableStateOf(restored?.first ?: listOf(UiMsg("assistant", scenario.opening)))
    }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var metGoals by remember(scenario.id) { mutableStateOf(restored?.second ?: emptySet()) }
    var isHinting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Persist after every change so closing the app / leaving keeps the chat.
    // A scenario with only the opening line counts as "fresh" → clear instead.
    LaunchedEffect(messages, metGoals) {
        if (messages.size > 1) {
            PreferencesManager.saveRoleplaySession(scenario.id, serializeSession(messages, metGoals))
        } else {
            PreferencesManager.clearRoleplaySession(scenario.id)
        }
    }

    // Keep the latest message in view.
    LaunchedEffect(messages.size, isSending) {
        val target = messages.size + if (isSending) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    fun resetConversation() {
        messages = listOf(UiMsg("assistant", scenario.opening))
        metGoals = emptySet()
        suggestions = emptyList()
        input = ""
        error = null
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || isSending) return
        input = ""
        suggestions = emptyList()
        error = null
        messages = messages + UiMsg("user", text)
        isSending = true
        scope.launch {
            val history = messages.map { ChatMessage(it.role, it.content) }
            when (val r = AiManager.roleplayReply(scenario, history)) {
                is AiCallResult.Success -> {
                    val turn = r.data
                    val updated = messages.toMutableList()
                    // Attach corrections to the learner's last message.
                    val idx = updated.indexOfLast { it.role == "user" }
                    if (idx >= 0) updated[idx] = updated[idx].copy(corrections = turn.corrections)
                    updated.add(UiMsg("assistant", turn.reply))
                    messages = updated
                    suggestions = turn.suggestions
                    if (turn.goalsMet.isNotEmpty()) metGoals = metGoals + turn.goalsMet
                }
                is AiCallResult.Error -> error = r.message
            }
            isSending = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scenario.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(scenario.title, fontWeight = FontWeight.Bold, color = VietRed)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VietRed)
                }
            },
            actions = {
                IconButton(onClick = { resetConversation() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Bắt đầu lại", tint = VietRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Goal header (checklist when sub-steps exist, else a single line).
        GoalHeader(scenario, metGoals)

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(messages) { _, m -> MessageBubble(m, scenario.emoji) }
            if (isSending) {
                item { TypingBubble(scenario.emoji) }
            }
        }

        // Suggestion chips (from the tutor or the 💡 button) — tap to fill input.
        if (suggestions.isNotEmpty()) {
            SuggestionChips(
                suggestions = suggestions,
                onPick = { input = it.substringBefore('(').trim(); suggestions = emptyList() }
            )
        }

        error?.let { e ->
            Text(
                e,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDECEA))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp,
                color = Color(0xFFC62828)
            )
        }

        // Input row
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hint button
                IconButton(
                    onClick = {
                        if (isHinting) return@IconButton
                        isHinting = true
                        error = null
                        scope.launch {
                            val history = messages.map { ChatMessage(it.role, it.content) }
                            when (val r = AiManager.roleplayHint(scenario, history)) {
                                is AiCallResult.Success -> suggestions = listOf(r.data)
                                is AiCallResult.Error -> error = r.message
                            }
                            isHinting = false
                        }
                    }
                ) {
                    if (isHinting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VietYellow, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Gợi ý", tint = VietYellow)
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tiếng Việt...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VietRed,
                        cursorColor = VietRed
                    )
                )

                IconButton(
                    onClick = { send() },
                    enabled = input.isNotBlank() && !isSending
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = if (input.isNotBlank() && !isSending) VietRed else TextSecondary
                    )
                }
            }
        }
    }
}

// Tutor-correction palette.
private val CorrectionBg = Color(0xFFFFF3E0)   // soft orange
private val CorrectionFg = Color(0xFFE65100)   // deep orange
private val WrongRed = Color(0xFFC62828)
private val RightGreen = Color(0xFF2E7D32)

@Composable
private fun GoalHeader(scenario: RoleplayScenario, metGoals: Set<String>) {
    Surface(color = VietYellow.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (scenario.goalSteps.isEmpty()) {
                Text(
                    "🎯 Mục tiêu: ${scenario.goal}",
                    fontSize = 12.sp,
                    color = Color(0xFF8D6E00)
                )
            } else {
                Text(
                    "🎯 Mục tiêu",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8D6E00)
                )
                Spacer(Modifier.height(4.dp))
                scenario.goalSteps.forEach { step ->
                    val done = metGoals.contains(step)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Icon(
                            if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (done) RightGreen else Color(0xFFBFA23A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            step,
                            fontSize = 12.sp,
                            color = if (done) RightGreen else Color(0xFF8D6E00),
                            textDecoration = if (done) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChips(suggestions: List<String>, onPick: (String) -> Unit) {
    Surface(color = VietRed.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = VietRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gợi ý — chạm để dùng", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            suggestions.forEach { s ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, VietRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clickable { onPick(s) }
                ) {
                    Text(
                        s,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: UiMsg, npcEmoji: String) {
    val isUser = m.role == "user"
    var expanded by remember(m.content, m.corrections.size) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(VietYellow.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(npcEmoji, fontSize = 18.sp) }
                Spacer(Modifier.width(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) VietRed else Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    m.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else TextPrimary,
                    lineHeight = 19.sp
                )
            }
        }

        // Correction affordance — hidden by default, tap to reveal (learner turns only).
        if (isUser && m.corrections.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CorrectionBg,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = CorrectionFg, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (expanded) "Góp ý sửa lỗi"
                            else "${m.corrections.size} góp ý sửa — chạm để xem",
                            fontSize = 12.sp,
                            color = CorrectionFg,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CorrectionFg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (expanded) {
                        m.corrections.forEach { c ->
                            Spacer(Modifier.height(6.dp))
                            CorrectionRow(c)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionRow(c: AiMistake) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(typeLabel(c.type), fontSize = 10.sp, color = CorrectionFg, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (c.text.isNotBlank()) {
                Text(c.text, fontSize = 12.sp, color = WrongRed, textDecoration = TextDecoration.LineThrough)
                Text("  →  ", fontSize = 12.sp, color = TextSecondary)
            }
            Text(c.suggestion, fontSize = 12.sp, color = RightGreen, fontWeight = FontWeight.Medium)
        }
        if (c.explanation.isNotBlank()) {
            Text(c.explanation, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "missing_tone" -> "DẤU THANH"
    "spelling" -> "CHÍNH TẢ"
    "word_order" -> "TRẬT TỰ TỪ"
    "word_choice" -> "DÙNG TỪ"
    "grammar" -> "NGỮ PHÁP"
    "punctuation" -> "DẤU CÂU"
    else -> "GÓP Ý"
}

@Composable
private fun TypingBubble(npcEmoji: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(VietYellow.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) { Text(npcEmoji, fontSize = 18.sp) }
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = VietRed, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("đang soạn...", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}
