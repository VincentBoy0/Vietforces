/**
 * Reusable UX state composables for VietForces.
 *  - EmptyStateComposable: use when a list/screen has no data (UX-01)
 *  - ShimmerBox: use as a loading placeholder with matching size (UX-02, UX-03)
 *  - ErrorStateComposable: use on network failure with retry capability (UX-04)
 *
 * Security note (T-02-14): callers must NOT pass raw exception messages to ErrorStateComposable.
 * Use generic user-facing messages only.
 */
package com.example.vietforces.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays an empty state with an illustration emoji, message, and optional CTA button.
 *
 * Usage:
 * ```
 * EmptyStateComposable(
 *     illustration = "📚",
 *     message = "Chưa có dữ liệu.\nVui lòng thử lại sau.",
 *     ctaText = "Thử lại",
 *     onCtaClick = { viewModel.reload() }
 * )
 * ```
 *
 * UX-01
 */
@Composable
fun EmptyStateComposable(
    illustration: String,
    message: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = illustration,
            fontSize = 64.sp
        )
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (ctaText != null && onCtaClick != null) {
            Button(
                onClick = onCtaClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = ctaText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Animated shimmer placeholder for loading states.
 * Uses only [rememberInfiniteTransition] + [animateFloat] — no external shimmer library.
 *
 * Usage:
 * ```
 * ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
 * ```
 * Use as a placeholder with the same size as the real content while data loads,
 * then replace with real content once loaded.
 *
 * Alpha animates 0.3 → 0.9, tween(800ms), RepeatMode.Reverse (UX-02, UX-03).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/**
 * Displays an error state with a warning emoji, message, and a retry button.
 *
 * Security note (T-02-14): pass only generic user-facing messages — do NOT expose raw
 * exception or API error details that could leak implementation information.
 *
 * Usage:
 * ```
 * ErrorStateComposable(
 *     message = "Không thể tải dữ liệu.\nKiểm tra kết nối mạng và thử lại.",
 *     onRetry = { viewModel.reload() }
 * )
 * ```
 *
 * UX-04
 */
@Composable
fun ErrorStateComposable(
    message: String = "Không thể tải dữ liệu.\nKiểm tra kết nối mạng và thử lại.",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Text(
            text = message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        OutlinedButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Thử lại",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Thử lại")
        }
    }
}
