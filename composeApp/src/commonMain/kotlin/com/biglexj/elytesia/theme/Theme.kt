package com.biglexj.elytesia.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkGrayBg = Color(0xFF0F172A)
val SurfaceGray = Color(0xFF1E293B)
val BorderGray = Color(0xFF334155)
val TextMain = Color(0xFFF8FAFC)
val TextContrast = Color(0xFF94A3B8)

// Colores Aurora/Ely
val AuroraViolet = Color(0xFF5B4CFF)
val ElyGreen = Color(0xFF00C7B1)
val ElyPink = Color(0xFFFB7793)
val ElyCream = Color(0xFFFFE6CA)

private val DarkColorScheme = darkColorScheme(
    primary = AuroraViolet,
    secondary = ElyGreen,
    tertiary = ElyPink,
    background = DarkGrayBg,
    surface = SurfaceGray,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun ElyTesiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
