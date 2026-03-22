package com.sosring.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SosRed   = Color(0xFFFF3B30)
val WarnAmber= Color(0xFFFF9F0A)
val SafeGreen= Color(0xFF34C759)
val Subtle   = Color(0xFF8E8E93)

private val DarkColors = darkColorScheme(
    primary   = SosRed,
    background= Color(0xFF0F0F12),
    surface   = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF2F2F7),
    onSurface    = Color(0xFFF2F2F7),
)

@Composable
fun SosRingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
