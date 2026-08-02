package com.biglexj.elytesia.features.player


import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.Song

/** Manos que el usuario quiere practicar en el Modo Práctica por Mano. */
enum class HandMode(val label: String, val icon: String) {
    AMBAS("Ambas Manos", "🎹"),
    MANO_IZQUIERDA("Mano Izq.", "🫲"),
    MANO_DERECHA("Mano Der.", "🫱");

    fun next(): HandMode = entries[(ordinal + 1) % entries.size]
}


/** Cálculos deterministas del reproductor, separados de Compose y del reloj. */
internal object PlaybackLogic {
    fun activeNotesAt(song: Song, timeMs: Long): List<NoteEvent> =
        song.notes.filter { timeMs >= it.startTimeMs && timeMs < it.startTimeMs + it.durationMs }

    fun expectedNote(song: Song, pitch: Int, timeMs: Long, toleranceMs: Long): NoteEvent? =
        song.notes
            .asSequence()
            .filter { note ->
                note.pitch == pitch &&
                    timeMs >= note.startTimeMs - toleranceMs &&
                    timeMs <= note.startTimeMs + note.durationMs + toleranceMs
            }
            .maxWithOrNull(notePriority)

    fun visibleNotesByPitch(activeNotes: List<NoteEvent>): Map<Int, NoteEvent> =
        activeNotes.groupBy(NoteEvent::pitch).mapValues { (_, notesAtPitch) ->
            notesAtPitch.maxWithOrNull(notePriority)!!
        }

    private val notePriority = compareBy<NoteEvent> { it.startTimeMs }.thenBy { -it.track }
}
