package com.dueday.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF10241C)
private val ForestLift = Color(0xFF173328)
private val Mint = Color(0xFF3DDC97)
private val Cream = Color(0xFFE8F6EE)

@Composable
fun DueDayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint,
            onPrimary = Forest,
            background = Forest,
            surface = ForestLift,
            onBackground = Cream,
            onSurface = Cream,
        ),
        content = content,
    )
}
