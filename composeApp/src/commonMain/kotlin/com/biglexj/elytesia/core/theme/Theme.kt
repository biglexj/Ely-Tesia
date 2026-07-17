package com.biglexj.elytesia.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

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

val LocalElyThemeDefinition = staticCompositionLocalOf { ThemeDefaults.Aurora }
val LocalElyMusicTheme = staticCompositionLocalOf { ThemeDefaults.Aurora.music.resolve() }
val LocalElyThemeEffects = staticCompositionLocalOf { ThemeDefaults.Aurora.effects }

private val ElyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

fun ElyThemeDefinition.toColorScheme(): ColorScheme {
    val t = material
    val colors = arrayOf(
        t.primary, t.onPrimary, t.primaryContainer, t.onPrimaryContainer, t.secondary, t.onSecondary,
        t.secondaryContainer, t.onSecondaryContainer, t.tertiary, t.onTertiary, t.background,
        t.onBackground, t.surface, t.onSurface, t.surfaceVariant, t.onSurfaceVariant, t.error,
        t.onError, t.outline
    ).map(String::toComposeColor)
    return if (mode == ThemeMode.LIGHT) {
        lightColorScheme(
            primary = colors[0], onPrimary = colors[1], primaryContainer = colors[2], onPrimaryContainer = colors[3],
            secondary = colors[4], onSecondary = colors[5], secondaryContainer = colors[6], onSecondaryContainer = colors[7],
            tertiary = colors[8], onTertiary = colors[9], background = colors[10], onBackground = colors[11],
            surface = colors[12], onSurface = colors[13], surfaceVariant = colors[14], onSurfaceVariant = colors[15],
            error = colors[16], onError = colors[17], outline = colors[18]
        )
    } else {
        darkColorScheme(
            primary = colors[0], onPrimary = colors[1], primaryContainer = colors[2], onPrimaryContainer = colors[3],
            secondary = colors[4], onSecondary = colors[5], secondaryContainer = colors[6], onSecondaryContainer = colors[7],
            tertiary = colors[8], onTertiary = colors[9], background = colors[10], onBackground = colors[11],
            surface = colors[12], onSurface = colors[13], surfaceVariant = colors[14], onSurfaceVariant = colors[15],
            error = colors[16], onError = colors[17], outline = colors[18]
        )
    }
}

// Tipografía explícita con SansSerif para garantizar soporte completo de
// caracteres españoles (acentos: á é í ó ú ü, y ñ) en Desktop JVM / Android.
private val ElyTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp),
)

@Composable
fun ElyTesiaTheme(
    theme: ElyThemeDefinition = ThemeDefaults.Aurora,
    platformColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalElyThemeDefinition provides theme,
        LocalElyMusicTheme provides theme.music.resolve(),
        LocalElyThemeEffects provides theme.effects
    ) {
        MaterialTheme(
            colorScheme = platformColorScheme ?: theme.toColorScheme(),
            typography = ElyTypography,
            shapes = ElyShapes,
            content = content
        )
    }
}
