package com.example.vietforces.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.ui.theme.TextPrimary
import com.example.vietforces.ui.theme.TextSecondary
import com.example.vietforces.ui.theme.VietRed
import java.text.SimpleDateFormat
import java.util.*

/**
 * 28-day streak heatmap composable.
 *
 * STREAK-04: Shows last 28 days in a 7-column grid (Mon–Sun header), oldest→newest.
 * Practiced days are highlighted in VietRed; absent days shown in surfaceVariant.
 *
 * PRE-01: All date math uses Locale.ROOT + UTC timezone.
 */
@Composable
fun StreakHeatmapComposable(
    practicedDates: Set<String>,
    modifier: Modifier = Modifier
) {
    // PRE-01: Locale.ROOT + UTC for all date operations.
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Build the 28 UTC date strings, oldest first.
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT)
    val dates: List<String> = (27 downTo 0).map { daysAgo ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        sdf.format(cal.time)
    }

    // Determine Monday-alignment offset for the first date.
    // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, … 7=Sat → convert to 0=Mon…6=Sun.
    val firstDateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
        time = sdf.parse(dates.first())!!
    }
    val rawDow = firstDateCal.get(Calendar.DAY_OF_WEEK) // 1=Sun…7=Sat
    // Map to 0=Mon…6=Sun
    val dayOfWeekOffset = (rawDow - Calendar.MONDAY + 7) % 7

    // Pad the list with nulls so the first real date falls on the correct column.
    val paddedDates: List<String?> = List(dayOfWeekOffset) { null } + dates

    // Chunk into rows of 7.
    val rows = paddedDates.chunked(7)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Text(
                text = "Lịch học",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            // Day-of-week labels: Mon–Sun (Vietnamese abbreviated)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val dayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Grid rows
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Ensure each row has exactly 7 cells (last row may be short)
                    val paddedRow = row + List(7 - row.size) { null }
                    paddedRow.forEach { date ->
                        val isPracticed = date != null && date in practicedDates
                        val cellColor: Color = when {
                            date == null -> Color.Transparent
                            isPracticed -> VietRed
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPracticed) {
                                Text(
                                    text = "🔥",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(VietRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Đã học", fontSize = 10.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Chưa học", fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}
