package com.biglexj.elytesia.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ThemeValidator {
    const val MAX_FILE_BYTES = 65_536
    private val idPattern = Regex("^[a-z0-9]+(?:[._-][a-z0-9]+)+$")
    private val colorPattern = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")
    private val versionPattern = Regex("^\\d+\\.\\d+\\.\\d+$")

    fun validate(theme: ElyThemeDefinition): List<String> = buildList {
        if (theme.schemaVersion != 1) add("schemaVersion no compatible: ${theme.schemaVersion}.")
        if (!idPattern.matches(theme.id)) add("El id debe usar minúsculas y separadores, por ejemplo com.autor.tema.")
        if (theme.name.length !in 1..60) add("El nombre debe tener entre 1 y 60 caracteres.")
        if (theme.author.length !in 1..60) add("El autor debe tener entre 1 y 60 caracteres.")
        if (!versionPattern.matches(theme.version)) add("La versión debe usar el formato 1.0.0.")
        if (theme.license.isBlank()) add("La licencia es obligatoria.")
        theme.allColors().forEach { (name, value) -> if (!colorPattern.matches(value)) add("Color inválido en $name: $value.") }
        if (theme.effects.pressedGlow !in 0f..1f) add("pressedGlow debe estar entre 0 y 1.")
        if (theme.effects.noteTrail !in 0f..1f) add("noteTrail debe estar entre 0 y 1.")
        if (theme.effects.particleIntensity !in 0f..1f) add("particleIntensity debe estar entre 0 y 1.")
        contrastPairs(theme.material).forEach { (label, pair) ->
            if (colorPattern.matches(pair.first) && colorPattern.matches(pair.second) &&
                contrastRatio(pair.first, pair.second) < 4.5
            ) {
                add("Contraste insuficiente en $label (mínimo 4.5:1).")
            }
        }
    }

    fun contrastRatio(foreground: String, background: String): Double {
        val first = luminance(foreground)
        val second = luminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun luminance(color: String): Double {
        val hex = color.removePrefix("#")
        fun channel(offset: Int): Double {
            val value = hex.substring(offset, offset + 2).toInt(16) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(0) + 0.7152 * channel(2) + 0.0722 * channel(4)
    }

    private fun contrastPairs(tokens: MaterialThemeTokens) = listOf(
        "primary/onPrimary" to (tokens.primary to tokens.onPrimary),
        "primaryContainer/onPrimaryContainer" to (tokens.primaryContainer to tokens.onPrimaryContainer),
        "secondary/onSecondary" to (tokens.secondary to tokens.onSecondary),
        "secondaryContainer/onSecondaryContainer" to (tokens.secondaryContainer to tokens.onSecondaryContainer),
        "tertiary/onTertiary" to (tokens.tertiary to tokens.onTertiary),
        "background/onBackground" to (tokens.background to tokens.onBackground),
        "surface/onSurface" to (tokens.surface to tokens.onSurface),
        "surfaceVariant/onSurfaceVariant" to (tokens.surfaceVariant to tokens.onSurfaceVariant),
        "error/onError" to (tokens.error to tokens.onError)
    )

    private fun ElyThemeDefinition.allColors(): Map<String, String> = buildMap {
        with(material) {
            put("material.primary", primary); put("material.onPrimary", onPrimary)
            put("material.primaryContainer", primaryContainer); put("material.onPrimaryContainer", onPrimaryContainer)
            put("material.secondary", secondary); put("material.onSecondary", onSecondary)
            put("material.secondaryContainer", secondaryContainer); put("material.onSecondaryContainer", onSecondaryContainer)
            put("material.tertiary", tertiary); put("material.onTertiary", onTertiary)
            put("material.background", background); put("material.onBackground", onBackground)
            put("material.surface", surface); put("material.onSurface", onSurface)
            put("material.surfaceVariant", surfaceVariant); put("material.onSurfaceVariant", onSurfaceVariant)
            put("material.error", error); put("material.onError", onError); put("material.outline", outline)
        }
        with(music) {
            put("music.leftHand", leftHand); put("music.rightHand", rightHand); put("music.neutralTrack", neutralTrack)
            put("music.whiteKey", whiteKey); put("music.whiteKeyPressed", whiteKeyPressed); put("music.blackKey", blackKey)
            put("music.blackKeyPressed", blackKeyPressed); put("music.correctNote", correctNote); put("music.wrongNote", wrongNote)
            put("music.waitingNote", waitingNote); put("music.particleLeft", particleLeft); put("music.particleRight", particleRight)
        }
    }
}
