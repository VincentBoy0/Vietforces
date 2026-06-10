package com.example.vietforces.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.model.AiMistake
import com.example.vietforces.data.model.WritingFeedback
import com.example.vietforces.data.model.WritingTopic
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.launch

/** Default writing topics (require_app.md §2). */
private val writingTopics = listOf(
    WritingTopic("intro", "Giới thiệu bản thân", "🙋", "Tên, tuổi, đến từ đâu, đang làm gì..."),
    WritingTopic("family", "Gia đình của tôi", "👨‍👩‍👧", "Gia đình có mấy người, làm nghề gì..."),
    WritingTopic("hobby", "Sở thích của tôi", "🎨", "Bạn thích làm gì lúc rảnh?"),
    WritingTopic("day", "Một ngày của tôi", "🌤️", "Buổi sáng, trưa, chiều bạn làm gì?"),
    WritingTopic("food", "Món ăn yêu thích", "🍜", "Món gì, vị thế nào, ăn ở đâu?"),
    WritingTopic("school", "Trường học", "🏫", "Trường của bạn, môn học yêu thích..."),
    WritingTopic("work", "Công việc", "💼", "Bạn làm nghề gì, thích điều gì ở công việc?"),
    WritingTopic("city", "Thành phố tôi đang sống", "🏙️", "Thành phố nào, có gì thú vị?"),
    WritingTopic("weekend", "Kế hoạch cuối tuần", "📅", "Cuối tuần này bạn định làm gì?")
)

private val GoodGreen = Color(0xFF2E7D32)
private val WarnAmber = Color(0xFFEF6C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingPracticeScreen(
    onBackClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedTopic by remember { mutableStateOf(writingTopics.first()) }
    var text by remember { mutableStateOf("") }
    var isGrading by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<WritingFeedback?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val wordCount = remember(text) { text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = { Text("Luyện viết", fontWeight = FontWeight.Bold, color = VietRed) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VietRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Topic picker ----
            Text("Chọn chủ đề", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                writingTopics.forEach { topic ->
                    TopicChip(
                        topic = topic,
                        selected = topic.id == selectedTopic.id,
                        onClick = {
                            selectedTopic = topic
                            feedback = null
                            errorMessage = null
                        }
                    )
                }
            }

            // ---- Prompt hint ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VietYellow.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedTopic.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(selectedTopic.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(selectedTopic.hint, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // ---- Input ----
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = { Text("Viết vài câu bằng tiếng Việt...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VietRed,
                    cursorColor = VietRed
                )
            )
            Text(
                text = "$wordCount từ • gợi ý tối thiểu ${selectedTopic.suggestedMinWords} từ",
                fontSize = 12.sp,
                color = if (wordCount >= selectedTopic.suggestedMinWords) GoodGreen else TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )

            // ---- Submit ----
            Button(
                onClick = {
                    errorMessage = null
                    feedback = null
                    isGrading = true
                    scope.launch {
                        when (val r = AiManager.gradeWriting(selectedTopic.title, text.trim())) {
                            is AiCallResult.Success -> feedback = r.data
                            is AiCallResult.Error -> errorMessage = r.message
                        }
                        isGrading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGrading && text.trim().length >= 5,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VietRed)
            ) {
                if (isGrading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("AI đang chấm...", fontWeight = FontWeight.Medium)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gửi bài cho AI chấm", fontWeight = FontWeight.Medium)
                }
            }

            // ---- Error / fallback ----
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = WarnAmber)
                        Spacer(Modifier.width(10.dp))
                        Text(msg, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            // ---- AI feedback ----
            AnimatedVisibility(visible = feedback != null) {
                feedback?.let { FeedbackCard(it) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopicChip(topic: WritingTopic, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) VietRed else Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (selected) VietRed else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "${topic.emoji} ${topic.title}",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun FeedbackCard(fb: WritingFeedback) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Score header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(scoreColor(fb.score).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${fb.score}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(fb.score)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Điểm: ${fb.score}/10", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("AI nhận xét", fontSize = 12.sp, color = TextSecondary)
                }
            }

            if (fb.overallFeedback.isNotBlank()) {
                Text(fb.overallFeedback, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            }

            if (fb.mistakes.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Text("🔍 Các điểm cần sửa", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                fb.mistakes.forEach { MistakeRow(it) }
            }

            if (fb.correctedVersion.isNotBlank()) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Text("✅ Bản viết lại gợi ý", fontWeight = FontWeight.SemiBold, color = GoodGreen)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoodGreen.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        fb.correctedVersion,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            if (fb.weaknessTags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fb.weaknessTags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(8.dp), color = VietRed.copy(alpha = 0.1f)) {
                            Text(
                                "#$tag",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = VietRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeRow(m: AiMistake) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Text("• ", color = WarnAmber, fontWeight = FontWeight.Bold)
            Column {
                if (m.text.isNotBlank()) {
                    Row {
                        Text(m.text, fontSize = 13.sp, color = WarnAmber)
                        Text("  →  ", fontSize = 13.sp, color = TextSecondary)
                        Text(m.suggestion, fontSize = 13.sp, color = GoodGreen, fontWeight = FontWeight.Medium)
                    }
                }
                if (m.explanation.isNotBlank()) {
                    Text(m.explanation, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 8 -> GoodGreen
    score >= 5 -> WarnAmber
    else -> Color(0xFFC62828)
}
