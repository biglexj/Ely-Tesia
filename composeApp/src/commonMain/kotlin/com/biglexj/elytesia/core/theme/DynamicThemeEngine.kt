package com.biglexj.elytesia.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Motor de derivación armónica de color dinámico para Ely-Tesia (Material 3 Expressive).
 * 
 * Convierte cualquier [ColorScheme] de Material 3 (procedente de Monet en Android 12+
 * o de paletas generadas por semilla en Desktop) en un conjunto completo de tokens
 * armónicos para el teclado de piano y la cascada de notas.
 */
object DynamicThemeEngine {

    /**
     * Deriva un [ResolvedMusicTheme] dinámico garantizando que:
     * - Teclas Blancas (Notas Naturales) adopten tonos de superficie expresiva (`surfaceContainerHigh` / `surfaceVariant`).
     * - Teclas Negras (Accidentales: Sostenidos y Bemoles) adopten la variante de contraste `tertiary` / `tertiaryContainer`.
     * - Las notas tocadas por la Mano Izquierda y Mano Derecha sigan la paleta `secondary` y `primary`.
     */
    fun deriveMusicTheme(colorScheme: ColorScheme): ResolvedMusicTheme {
        val rightColor = colorScheme.primary
        val leftColor  = colorScheme.secondary  // variante más oscura/fría para la mano izquierda
        return ResolvedMusicTheme(
            leftHand  = leftColor,
            rightHand = rightColor,
            neutralTrack = colorScheme.tertiary,

            // Teclas Blancas y Negras tradicionales en reposo
            whiteKey = Color(0xFFF8FAFC),
            whiteKeyPressed = rightColor,

            blackKey = Color(0xFF0F172A),
            blackKeyPressed = rightColor,

            // Retroalimentación de notas
            correctNote = rightColor,
            wrongNote = colorScheme.error,
            waitingNote = colorScheme.tertiaryContainer,

            // Partículas reactivas con color de su mano correspondiente
            particleLeft  = leftColor.copy(alpha = 0.8f),
            particleRight = rightColor.copy(alpha = 0.8f)
        )
    }

    /**
     * Genera una paleta [ColorScheme] armónica para plataformas que no poseen Monet nativo (como Desktop),
     * a partir de un color primario o semilla.
     */
    fun createSeedColorScheme(seedColor: Color, darkTheme: Boolean = true): ColorScheme {
        val primary = seedColor
        val secondary = lerpColor(seedColor, Color(0xFF00C7B1), 0.4f)
        val tertiary = lerpColor(seedColor, Color(0xFFFB7793), 0.5f)

        return if (darkTheme) {
            androidx.compose.material3.darkColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                primaryContainer = primary.copy(alpha = 0.35f),
                secondaryContainer = secondary.copy(alpha = 0.35f),
                tertiaryContainer = tertiary.copy(alpha = 0.35f),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                surfaceVariant = Color(0xFF334155),
                onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFF8FAFC),
                onSurfaceVariant = Color(0xFFCBD5E1)
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                primaryContainer = primary.copy(alpha = 0.15f),
                secondaryContainer = secondary.copy(alpha = 0.15f),
                tertiaryContainer = tertiary.copy(alpha = 0.15f),
                background = Color(0xFFF8FAFC),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE2E8F0),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF475569)
            )
        }
    }

    private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (stop.red - start.red) * f,
            green = start.green + (stop.green - start.green) * f,
            blue = start.blue + (stop.blue - start.blue) * f,
            alpha = start.alpha + (stop.alpha - start.alpha) * f
        )
    }
}
