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
    /**
     * Índice temporal reutilizable para no recorrer toda la canción en cada fotograma.
     * Las notas se ordenan por inicio y el prefijo de finales permite descartar de forma
     * segura las notas que ya terminaron, incluso cuando existen notas muy largas.
     */
    internal class TimelineIndex(notes: List<NoteEvent>) {
        private val sortedNotes = notes.sortedWith(
            compareBy<NoteEvent> { it.startTimeMs }
                .thenBy { it.track }
                .thenBy { it.pitch }
        )
        private val prefixMaxEndMs = LongArray(sortedNotes.size)

        init {
            var maxEndMs = Long.MIN_VALUE
            sortedNotes.forEachIndexed { index, note ->
                maxEndMs = maxOf(maxEndMs, note.startTimeMs + note.durationMs)
                prefixMaxEndMs[index] = maxEndMs
            }
        }

        fun activeNotesAt(timeMs: Long): List<NoteEvent> {
            val endExclusive = upperBoundStart(timeMs)
            return intersectingRange(timeMs, endExclusive).filter { note ->
                timeMs >= note.startTimeMs && timeMs < note.startTimeMs + note.durationMs
            }
        }

        fun visibleNotesAt(timeMs: Long, timeWindowMs: Long): List<NoteEvent> {
            val windowEndMs = timeMs + timeWindowMs
            val endExclusive = upperBoundStart(windowEndMs)
            return intersectingRange(timeMs, endExclusive).filter { note ->
                val noteEndTime = note.startTimeMs + note.durationMs
                noteEndTime >= timeMs && note.startTimeMs <= windowEndMs
            }
        }

        inline fun forEachVisibleNote(timeMs: Long, timeWindowMs: Long, action: (NoteEvent) -> Unit) {
            val windowEndMs = timeMs + timeWindowMs
            val endExclusive = upperBoundStart(windowEndMs)
            val start = firstPrefixEndAtLeast(timeMs)
            if (start >= endExclusive) return
            for (i in start until endExclusive) {
                val note = sortedNotes[i]
                val noteEndTime = note.startTimeMs + note.durationMs
                if (noteEndTime >= timeMs && note.startTimeMs <= windowEndMs) {
                    action(note)
                }
            }
        }

        private fun intersectingRange(timeMs: Long, endExclusive: Int): List<NoteEvent> {
            val start = firstPrefixEndAtLeast(timeMs)
            if (start >= endExclusive) return emptyList()
            return sortedNotes.subList(start, endExclusive)
        }

        private fun firstPrefixEndAtLeast(timeMs: Long): Int {
            var low = 0
            var high = prefixMaxEndMs.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (prefixMaxEndMs[middle] >= timeMs) high = middle else low = middle + 1
            }
            return low
        }

        private fun upperBoundStart(timeMs: Long): Int {
            var low = 0
            var high = sortedNotes.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (sortedNotes[middle].startTimeMs <= timeMs) low = middle + 1 else high = middle
            }
            return low
        }
    }

    fun activeNotesAt(song: Song, timeMs: Long): List<NoteEvent> =
        TimelineIndex(song.notes).activeNotesAt(timeMs)

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
