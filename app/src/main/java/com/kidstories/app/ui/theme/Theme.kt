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
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF5C1500),
    secondary = Color(0xFF7C4DFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF21005D),
    tertiary = Color(0xFF00BFA5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2EFE8),
    background = Color(0xFFFFF9E6),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF3EFF4),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFFF3B30),
    onError = Color.White,
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
