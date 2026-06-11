package com.kidstories.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val KidColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B6B),
    secondary = Color(0xFF4ECDC4),
    tertiary = Color(0xFFFFE66D),
    background = Color(0xFFFFF9F0),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF2D2D2D)
)

private val KidTypography = Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 28.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
)

@Composable
fun KidStoriesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = KidTypography,
        content = content
    )
}
