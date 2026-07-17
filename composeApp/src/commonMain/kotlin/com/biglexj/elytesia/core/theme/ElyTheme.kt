package com.biglexj.elytesia.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { DARK, LIGHT, AUTO }

@Immutable
data class ElyThemeDefinition(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val author: String,
    val description: String = "",
    val version: String = "1.0.0",
    val license: String = "CC0-1.0",
    val mode: ThemeMode = ThemeMode.DARK,
    val material: MaterialThemeTokens,
    val music: MusicThemeTokens,
    val effects: ThemeEffects = ThemeEffects(),
    val builtIn: Boolean = false
)

@Immutable
data class MaterialThemeTokens(
    val primary: String,
    val onPrimary: String,
    val primaryContainer: String,
    val onPrimaryContainer: String,
    val secondary: String,
    val onSecondary: String,
    val secondaryContainer: String,
    val onSecondaryContainer: String,
    val tertiary: String,
    val onTertiary: String,
    val background: String,
    val onBackground: String,
    val surface: String,
    val onSurface: String,
    val surfaceVariant: String,
    val onSurfaceVariant: String,
    val error: String,
    val onError: String,
    val outline: String
)

@Immutable
data class MusicThemeTokens(
    val leftHand: String,
    val rightHand: String,
    val neutralTrack: String,
    val whiteKey: String,
    val whiteKeyPressed: String,
    val blackKey: String,
    val blackKeyPressed: String,
    val correctNote: String,
    val wrongNote: String,
    val waitingNote: String,
    val particleLeft: String,
    val particleRight: String
)

@Immutable
data class ThemeEffects(
    val pressedGlow: Float = 0.75f,
    val noteTrail: Float = 0.55f,
    val particleIntensity: Float = 0.8f,
    val expressiveMotion: Boolean = true
)

@Immutable
data class ResolvedMusicTheme(
    val leftHand: Color,
    val rightHand: Color,
    val neutralTrack: Color,
    val whiteKey: Color,
    val whiteKeyPressed: Color,
    val blackKey: Color,
    val blackKeyPressed: Color,
    val correctNote: Color,
    val wrongNote: Color,
    val waitingNote: Color,
    val particleLeft: Color,
    val particleRight: Color
)

internal fun MusicThemeTokens.resolve(): ResolvedMusicTheme = ResolvedMusicTheme(
    leftHand = leftHand.toComposeColor(),
    rightHand = rightHand.toComposeColor(),
    neutralTrack = neutralTrack.toComposeColor(),
    whiteKey = whiteKey.toComposeColor(),
    whiteKeyPressed = whiteKeyPressed.toComposeColor(),
    blackKey = blackKey.toComposeColor(),
    blackKeyPressed = blackKeyPressed.toComposeColor(),
    correctNote = correctNote.toComposeColor(),
    wrongNote = wrongNote.toComposeColor(),
    waitingNote = waitingNote.toComposeColor(),
    particleLeft = particleLeft.toComposeColor(),
    particleRight = particleRight.toComposeColor()
)

fun String.toComposeColor(): Color {
    val normalized = removePrefix("#")
    val argb = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized.takeLast(2) + normalized.take(6)
        else -> error("Color inválido: $this")
    }
    return Color(argb.toLong(16))
}

fun Color.toThemeHex(includeAlpha: Boolean = false): String {
    fun component(value: Float) = (value.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0').uppercase()
    val rgb = component(red) + component(green) + component(blue)
    return if (includeAlpha || alpha < 1f) "#$rgb${component(alpha)}" else "#$rgb"
}

object HandColorResolver {
    fun isLeftHand(pitch: Int, track: Int?): Boolean = when (track) {
        1 -> true
        0 -> false
        else -> pitch < 60
    }

    fun color(theme: ResolvedMusicTheme, pitch: Int, track: Int?): Color =
        if (isLeftHand(pitch, track)) theme.leftHand else theme.rightHand
}
