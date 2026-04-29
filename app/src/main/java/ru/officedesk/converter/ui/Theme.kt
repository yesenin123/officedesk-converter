package ru.officedesk.converter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val paper      = Color(0xFFFBF6EC)
    val paper2     = Color(0xFFF0E8D6)
    val ink        = Color(0xFF2A2724)
    val inkSoft    = Color(0xFF55504a)
    val inkMute    = Color(0xFF8a8175)
    val rule       = Color(0xFFD9CDB4)
    val ruleSoft   = Color(0xFFE9DEC8)
    val ochre      = Color(0xFFB8884A)
    val ochreDeep  = Color(0xFF8A5F2C)
    val moss       = Color(0xFF7A8A4A)
    val stamp      = Color(0xFFB04A3A)
    val teal       = Color(0xFF3F6E6A)
}

private val LightScheme = lightColorScheme(
    primary = Palette.ochreDeep,
    onPrimary = Color.White,
    primaryContainer = Palette.paper2,
    onPrimaryContainer = Palette.ink,
    secondary = Palette.teal,
    onSecondary = Color.White,
    background = Palette.paper,
    onBackground = Palette.ink,
    surface = Color(0xFFFFFAF0),
    onSurface = Palette.ink,
    surfaceVariant = Palette.paper2,
    onSurfaceVariant = Palette.inkSoft,
    error = Palette.stamp,
    outline = Palette.rule,
    outlineVariant = Palette.ruleSoft,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.ochre,
    onPrimary = Color.Black,
    background = Color(0xFF1B1916),
    onBackground = Color(0xFFEDE3D0),
    surface = Color(0xFF24211C),
    onSurface = Color(0xFFEDE3D0),
)

@Composable
fun OfficeDeskTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val typography = Typography(
        titleLarge = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        ),
        bodyLarge = TextStyle(fontSize = 16.sp),
        bodyMedium = TextStyle(fontSize = 14.sp),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        ),
    )
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = typography,
        content = content,
    )
}
