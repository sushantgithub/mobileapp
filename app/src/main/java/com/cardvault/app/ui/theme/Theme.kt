package com.cardvault.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

val Navy = Color(0xFF0B1020)
val NavyElevated = Color(0xFF151C33)
val Gold = Color(0xFFD4AF37)
val GoldMuted = Color(0xFFB8962E)
val Ivory = Color(0xFFF4EFE4)
val Danger = Color(0xFFE26D5A)

private val Scheme: ColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    secondary = GoldMuted,
    background = Navy,
    surface = NavyElevated,
    onBackground = Ivory,
    onSurface = Ivory,
    error = Danger,
    onError = Ivory,
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        color = Ivory,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun CardVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = AppTypography,
        content = content,
    )
}
