package com.kinozal.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Brand = Color(0xFFE5484D)
val Gold = Color(0xFFF5C451)

private val colors = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = Gold,
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF22252C),
    onSurfaceVariant = Color(0xFFA4A8B3),
    surfaceContainer = Color(0xFF16181D),
    surfaceContainerHigh = Color(0xFF1D2026),
    surfaceContainerHighest = Color(0xFF262A31),
    outline = Color(0xFF3A3E47),
)

private val typography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun KinozalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
