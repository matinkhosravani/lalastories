package com.kidstories.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.kidstories.app.R

private val BKoodak = FontFamily(
    Font(R.font.b_koodak, FontWeight.Normal),
    Font(R.font.b_koodak, FontWeight.Bold)
)

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
    bodyLarge = TextStyle(fontFamily = BKoodak, fontSize = 20.sp),
    bodyMedium = TextStyle(fontFamily = BKoodak, fontSize = 16.sp),
    titleLarge = TextStyle(fontFamily = BKoodak, fontSize = 28.sp),
    titleMedium = TextStyle(fontFamily = BKoodak, fontSize = 22.sp),
    labelLarge = TextStyle(fontFamily = BKoodak, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = BKoodak, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = BKoodak, fontSize = 12.sp),
    headlineLarge = TextStyle(fontFamily = BKoodak, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = BKoodak, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = BKoodak, fontSize = 22.sp),
)

@Composable
fun KidStoriesTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = KidColorScheme,
            typography = KidTypography,
            content = content
        )
    }
}
