package com.gabs.cubo3x3.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull {
            it.storageValue == value
        } ?: SYSTEM
    }
}

val F2LLight = Color(0xFF008FA8)
val F2LDark = Color(0xFF59D7EA)
val OLLLight = Color(0xFFC47A00)
val OLLDark = Color(0xFFFFC857)
val PLLLight = Color(0xFF7447C8)
val PLLDark = Color(0xFFC4A1FF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4D5DFF),
    onPrimary = Color.White,
    secondary = Color(0xFF008FA8),
    tertiary = Color(0xFF7447C8),
    background = Color(0xFFF7F8FC),
    surface = Color.White,
    onBackground = Color(0xFF171923),
    onSurface = Color(0xFF171923),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AA4FF),
    onPrimary = Color(0xFF10143A),
    secondary = Color(0xFF59D7EA),
    tertiary = Color(0xFFC4A1FF),
    background = Color(0xFF0E1015),
    surface = Color(0xFF191C24),
    onBackground = Color(0xFFF3F4F7),
    onSurface = Color(0xFFF3F4F7),
)

private val AppTypography = Typography(
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
    ),
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun Cubo3x3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
