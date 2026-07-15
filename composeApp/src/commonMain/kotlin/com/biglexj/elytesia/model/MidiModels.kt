package com.biglexj.elytesia.model

import androidx.compose.runtime.mutableStateMapOf

data class NoteEvent(
    val pitch: Int,             // Número de nota MIDI (21 a 108)
    val startTimeMs: Long,      // Tiempo de inicio en milisegundos
    val durationMs: Long,       // Duración en milisegundos
    val velocity: Int,          // Velocidad/fuerza (0-127)
    val track: Int              // Canal/Pista
)

data class ControlEvent(
    val controller: Int,
    val value: Int,
    val timeMs: Long,
    val channel: Int = 0
)

enum class Difficulty(val displayName: String, val colorHex: Long) {
    FACIL("Fácil", 0xFF2DD4BF),
    INTERMEDIO("Intermedio", 0xFFFBBF24),
    AVANZADO("Avanzado", 0xFF8B5CF6)
}

data class Song(
    val name: String,
    val durationMs: Long,
    val notes: List<NoteEvent>,
    val bpm: Double = 120.0,    // Pulsos por minuto (BPM) iniciales
    val controls: List<ControlEvent> = emptyList(),
    val isDemo: Boolean = false,
    val difficulty: Difficulty = Difficulty.FACIL
) {
    fun calculateAutoDifficulty(): Difficulty {
        if (notes.isEmpty()) return Difficulty.FACIL
        
        val durationSec = durationMs / 1000.0
        if (durationSec <= 0) return Difficulty.FACIL
        
        // 1. Densidad de notas por segundo (Velocidad de ejecución)
        val density = notes.size / durationSec
        
        // 2. Factor de acordes (Polifonía y complejidad armónica)
        val startTimes = notes.map { it.startTimeMs }.toSet()
        val chordFactor = if (startTimes.isNotEmpty()) notes.size.toDouble() / startTimes.size else 1.0
        
        // 3. Rango tonal/teclado (Span)
        val pitches = notes.map { it.pitch }
        val minPitch = pitches.minOrNull() ?: 60
        val maxPitch = pitches.maxOrNull() ?: 60
        val span = maxPitch - minPitch
        
        // 4. Puntuación de dificultad combinada
        val score = (density * 0.8) + ((chordFactor - 1.0) * 3.0) + (span / 24.0)
        
        return when {
            score < 3.5 -> Difficulty.FACIL
            score < 6.0 -> Difficulty.INTERMEDIO
            else -> Difficulty.AVANZADO
        }
    }
}

class KeyboardState {
    val activeKeys = mutableStateMapOf<Int, Int>()
}
