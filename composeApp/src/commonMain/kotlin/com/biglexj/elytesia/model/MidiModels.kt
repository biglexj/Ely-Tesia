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

data class Song(
    val name: String,
    val durationMs: Long,
    val notes: List<NoteEvent>,
    val bpm: Double = 120.0,    // Pulsos por minuto (BPM) iniciales
    val controls: List<ControlEvent> = emptyList()
)

class KeyboardState {
    val activeKeys = mutableStateMapOf<Int, Int>()
}
