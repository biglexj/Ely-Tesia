package com.biglexj.elytesia.core.analyzer

import com.biglexj.elytesia.model.Difficulty
import com.biglexj.elytesia.model.Song

/**
 * Motor Heurístico Multimétrico de Clasificación de Dificultad (`DifficultyAnalyzer`).
 * Evalúa canciones y secuencias MIDI mediante:
 * 1. BPM (Pulsos Por Minuto)
 * 2. Densidad de Notas por Segundo (NPS)
 * 3. Polifonía Máxima Simultánea (Acordes complejos)
 * 4. Rango Tonal / Teclado (Pitch Span)
 * 5. Peso por Duración Total (>3 minutos)
 */
object DifficultyAnalyzer {

    fun analyze(song: Song): Difficulty {
        if (song.notes.isEmpty()) return Difficulty.FACIL

        val durationSec = (song.durationMs / 1000.0).coerceAtLeast(1.0)

        // 1. Densidad de notas por segundo (NPS)
        val density = song.notes.size / durationSec

        // 2. Polifonía simultánea máxima y promedio de acordes
        val timeGrouped = song.notes.groupBy { it.startTimeMs }
        val maxPolyphony = timeGrouped.values.maxOfOrNull { it.size } ?: 1
        val chordFactor = if (timeGrouped.isNotEmpty()) song.notes.size.toDouble() / timeGrouped.size else 1.0

        // 3. Rango tonal (Pitch Span en semitonos)
        val pitches = song.notes.map { it.pitch }
        val minPitch = pitches.minOrNull() ?: 60
        val maxPitch = pitches.maxOrNull() ?: 60
        val pitchSpan = maxPitch - minPitch

        // 4. Factor de BPM
        val bpmFactor = when {
            song.bpm < 80.0 -> 0.85
            song.bpm in 80.0..130.0 -> 1.0
            else -> 1.25
        }

        // 5. Factor de Duración
        val durationFactor = if (durationSec > 180.0) 1.15 else 1.0

        // Puntuación Heurística Combinada
        val baseScore = (density * 0.75) + ((chordFactor - 1.0) * 2.5) + (maxPolyphony * 0.35) + (pitchSpan / 28.0)
        val finalScore = baseScore * bpmFactor * durationFactor

        return when {
            finalScore < 4.0 -> Difficulty.FACIL
            finalScore < 7.5 -> Difficulty.INTERMEDIO
            else -> Difficulty.AVANZADO
        }
    }
}
