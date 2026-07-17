package com.biglexj.elytesia

import com.biglexj.elytesia.model.Difficulty
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.Song

fun getNoteName(pitch: Int): String {
    val notes = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    return "${notes[pitch % 12]}${(pitch / 12) - 1}"
}

fun generateDemoSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    val progressions = listOf(
        listOf(60, 64, 67, 72),
        listOf(57, 60, 64, 69),
        listOf(53, 57, 60, 65),
        listOf(55, 59, 62, 67)
    )
    var timeMs = 1200L
    repeat(2) {
        progressions.forEach { chord ->
            chord.forEachIndexed { index, pitch ->
                notes += NoteEvent(pitch, timeMs + index * 200L, 400L, 85, 1)
            }
            timeMs += 1000L
            chord.forEach { pitch -> notes += NoteEvent(pitch, timeMs, 800L, 95, 2) }
            timeMs += 1200L
        }
    }
    return Song("Bach Prelude C-Major (Demo)", timeMs + 1000L, notes, 120.0, isDemo = true, difficulty = Difficulty.AVANZADO)
}

fun generateScaleSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 800L
    listOf(60, 62, 64, 65, 67, 69, 71, 72, 72, 71, 69, 67, 65, 64, 62, 60).forEach { pitch ->
        notes += NoteEvent(pitch, timeMs, 250L, 90, 1)
        timeMs += 300L
    }
    return Song("Escala Do Mayor (Prueba)", timeMs + 400L, notes, 120.0, isDemo = true, difficulty = Difficulty.FACIL)
}

fun generateBellaCiaoSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 800L
    val quarter = 300L
    val melody = listOf(
        57 to quarter, 60 to quarter, 62 to quarter, 64 to quarter * 2,
        57 to quarter, 60 to quarter, 62 to quarter, 64 to quarter * 2,
        57 to quarter, 60 to quarter, 62 to quarter, 64 to quarter,
        67 to quarter, 69 to quarter, 67 to quarter, 64 to quarter,
        69 to quarter * 2, 69 to quarter, 69 to quarter, 69 to quarter,
        67 to quarter, 64 to quarter, 62 to quarter * 2, 60 to quarter,
        62 to quarter, 64 to quarter * 2, 62 to quarter, 60 to quarter,
        57 to quarter * 2
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 30L, 95, 1)
        if (timeMs % 1200L == 0L) notes += NoteEvent(pitch - 12, timeMs, duration * 2, 70, 2)
        timeMs += duration
    }
    return Song("Bella Ciao (Demo)", timeMs + 1000L, notes, 125.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generateGymnopedieSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 1000L
    val beat = 800L
    repeat(4) {
        notes += NoteEvent(43, timeMs, beat * 2, 80, 2)
        listOf(59, 62, 67).forEach { notes += NoteEvent(it, timeMs + beat, beat * 2, 70, 2) }
        timeMs += beat * 3
        notes += NoteEvent(38, timeMs, beat * 2, 80, 2)
        listOf(57, 61, 66).forEach { notes += NoteEvent(it, timeMs + beat, beat * 2, 70, 2) }
        timeMs += beat * 3
    }
    var melodyTimeMs = 1000L + beat * 3
    listOf(
        69 to beat * 3, 71 to beat * 3, 74 to beat * 3, 76 to beat * 3,
        71 to beat * 3, 67 to beat * 3, 64 to beat * 6
    ).forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, melodyTimeMs, duration - 50L, 85, 1)
        melodyTimeMs += duration
    }
    return Song("Gymnopédie No. 1 (Demo)", melodyTimeMs + 1000L, notes, 72.0, isDemo = true, difficulty = Difficulty.FACIL)
}
