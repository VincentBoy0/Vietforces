package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.ProfileManager
import com.example.vietforces.data.storage.PreferencesManager
import com.example.vietforces.ui.theme.TextPrimary
import com.example.vietforces.ui.theme.TextSecondary
import com.example.vietforces.ui.theme.VietRed
import kotlinx.coroutines.launch

/**
 * 4-step onboarding flow shown on first launch.
 * Steps: Welcome → Choose Level → Choose Daily Goal → Name + Avatar
 * On finish: writes prefs flags and calls onFinish().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    var selectedLevel by remember { mutableStateOf("beginner") }
    var selectedGoal by remember { mutableIntStateOf(10) }
    var userName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🦁") }
    val coroutineScope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> ChooseLevelPage(selectedLevel) { selectedLevel = it }
                2 -> ChooseGoalPage(selectedGoal) { selectedGoal = it }
                3 -> NameAvatarPage(
                    name = userName,
                    avatar = selectedAvatar,
                    onNameChange = { userName = it },
                    onAvatarChange = { selectedAvatar = it }
                )
            }
        }

        // Dot indicator
        PagerDotIndicator(pagerState)

        // Bottom action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pagerState.currentPage < 3) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VietRed)
                ) {
                    Text("Tiếp theo →", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Skip: mark onboarding done as guest with defaults
                        PreferencesManager.setOnboardingCompleted(true)
                        PreferencesManager.setIsGuest(true)
                        if (userName.isBlank()) {
                            ProfileManager.updateName("Chiến binh")
                        }
                        onFinish()
                    }
                ) {
                    Text("Bỏ qua", color = TextSecondary)
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userName.isNotBlank(),
                    onClick = {
                        PreferencesManager.setOnboardingCompleted(true)
                        PreferencesManager.setIsGuest(true)
                        PreferencesManager.setSelectedLevel(selectedLevel)
                        PreferencesManager.setDailyGoal(selectedGoal)
                        ProfileManager.updateName(userName.ifBlank { "Chiến binh" })
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VietRed)
                ) {
                    Text("Bắt đầu học! 🚀", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🐓", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "VietForces",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = VietRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Học từ vựng tiếng Việt\nqua trò chơi thú vị",
            fontSize = 18.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("🎮 7 chế độ chơi đặc sắc", fontSize = 15.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("🏆 Bảng xếp hạng ELO toàn cầu", fontSize = 15.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("🔥 Chuỗi ngày học liên tiếp", fontSize = 15.sp, color = TextPrimary)
    }
}

@Composable
private fun ChooseLevelPage(selectedLevel: String, onLevelSelect: (String) -> Unit) {
    val levels = listOf(
        Triple("beginner", "🌱 Người mới bắt đầu", "Chưa biết gì về tiếng Việt"),
        Triple("intermediate", "🌿 Trung cấp", "Biết một số từ cơ bản"),
        Triple("advanced", "🌳 Nâng cao", "Đã học tiếng Việt một thời gian")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Trình độ của bạn?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Chúng tôi sẽ điều chỉnh độ khó phù hợp", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        levels.forEach { (id, label, subtitle) ->
            LevelCard(
                id = id,
                label = label,
                subtitle = subtitle,
                isSelected = selectedLevel == id,
                onClick = { onLevelSelect(id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LevelCard(id: String, label: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) VietRed else Color.Transparent
    val containerColor = if (isSelected) VietRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) VietRed else TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
            }
            if (isSelected) {
                Text("✓", fontSize = 20.sp, color = VietRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChooseGoalPage(selectedGoal: Int, onGoalSelect: (Int) -> Unit) {
    val goals = listOf(
        Triple(5, "🐢 5 từ/ngày", "Nhẹ nhàng"),
        Triple(10, "🚶 10 từ/ngày", "Cân bằng (khuyến nghị)"),
        Triple(15, "🏃 15 từ/ngày", "Thử thách")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Mục tiêu mỗi ngày?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Bạn muốn học bao nhiêu từ mỗi ngày?", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        goals.forEach { (goal, label, subtitle) ->
            GoalCard(
                goal = goal,
                label = label,
                subtitle = subtitle,
                isSelected = selectedGoal == goal,
                onClick = { onGoalSelect(goal) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GoalCard(goal: Int, label: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) VietRed else Color.Transparent
    val containerColor = if (isSelected) VietRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) VietRed else TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
            }
            if (isSelected) {
                Text("✓", fontSize = 20.sp, color = VietRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NameAvatarPage(
    name: String,
    avatar: String,
    onNameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit
) {
    val avatars = listOf("🦁", "🐯", "🦊", "🐺", "🦅", "🐉", "⚔️", "🛡️")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Bạn là ai?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Đặt tên và chọn avatar chiến binh của bạn",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Selected avatar preview
        Text(avatar, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            avatars.forEach { a ->
                FilterChip(
                    selected = avatar == a,
                    onClick = { onAvatarChange(a) },
                    label = { Text(a, fontSize = 24.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VietRed.copy(alpha = 0.15f),
                        selectedLabelColor = VietRed
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Name input
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Tên của bạn") },
            placeholder = { Text("Chiến binh Việt", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VietRed,
                focusedLabelColor = VietRed,
                cursorColor = VietRed
            )
        )
    }
}

@Composable
private fun PagerDotIndicator(pagerState: PagerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val isSelected = pagerState.currentPage == index
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VietRed else Color.LightGray)
            )
        }
    }
}
