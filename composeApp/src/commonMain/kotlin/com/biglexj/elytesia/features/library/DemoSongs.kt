package com.biglexj.elytesia.features.library

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

fun generateHappyBirthdaySong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 600L
    val q = 350L
    val melody = listOf(
        60 to q, 60 to q, 62 to q * 2, 60 to q * 2, 65 to q * 2, 64 to q * 4,
        60 to q, 60 to q, 62 to q * 2, 60 to q * 2, 67 to q * 2, 65 to q * 4,
        60 to q, 60 to q, 72 to q * 2, 69 to q * 2, 65 to q * 2, 64 to q * 2, 62 to q * 2,
        70 to q, 70 to q, 69 to q * 2, 65 to q * 2, 67 to q * 2, 65 to q * 4
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 30L, 90, 1)
        timeMs += duration
    }
    return Song("Happy Birthday to You", timeMs + 600L, notes, 96.0, isDemo = true, difficulty = Difficulty.FACIL)
}

fun generateTwinkleTwinkleSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 600L
    val q = 400L
    val melody = listOf(
        60, 60, 67, 67, 69, 69, 67,
        65, 65, 64, 64, 62, 62, 60,
        67, 67, 65, 65, 64, 64, 62,
        67, 67, 65, 65, 64, 64, 62,
        60, 60, 67, 67, 69, 69, 67,
        65, 65, 64, 64, 62, 62, 60
    )
    melody.forEachIndexed { i, pitch ->
        val dur = if ((i + 1) % 7 == 0) q * 2 else q
        notes += NoteEvent(pitch, timeMs, dur - 40L, 85, 1)
        timeMs += dur
    }
    return Song("Twinkle Twinkle Little Star", timeMs + 600L, notes, 92.0, isDemo = true, difficulty = Difficulty.FACIL)
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

fun generateKorobeinikiSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 600L
    val q = 280L
    val melody = listOf(
        76 to q * 2, 71 to q, 72 to q, 74 to q * 2, 72 to q, 71 to q,
        69 to q * 2, 69 to q, 72 to q, 76 to q * 2, 74 to q, 72 to q,
        71 to q * 3, 72 to q, 74 to q * 2, 76 to q * 2, 72 to q * 2, 69 to q * 2, 69 to q * 4
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 20L, 95, 1)
        timeMs += duration
    }
    return Song("Korobeiniki (Tetris)", timeMs + 600L, notes, 138.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generateLaBambaSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 600L
    val q = 250L
    val melody = listOf(
        60 to q, 64 to q, 67 to q, 69 to q * 2, 67 to q * 2,
        65 to q, 67 to q, 65 to q, 64 to q * 2, 60 to q * 2,
        60 to q, 64 to q, 67 to q, 69 to q * 2, 67 to q * 2
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 20L, 90, 1)
        timeMs += duration
    }
    return Song("La Bamba", timeMs + 600L, notes, 150.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generateMorningMoodSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 800L
    val q = 400L
    val melody = listOf(
        64 to q, 62 to q, 60 to q, 62 to q, 64 to q, 67 to q * 2,
        64 to q, 62 to q, 60 to q, 62 to q, 64 to q * 3,
        67 to q, 69 to q, 72 to q, 69 to q, 67 to q, 64 to q * 2
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 30L, 88, 1)
        timeMs += duration
    }
    return Song("Morning Mood (Grieg)", timeMs + 800L, notes, 72.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generatePrimaveraSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 600L
    val q = 280L
    val melody = listOf(
        64 to q * 2, 68 to q, 68 to q, 68 to q, 66 to q, 64 to q, 71 to q * 2,
        71 to q, 69 to q, 68 to q, 69 to q * 2, 68 to q * 2
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 25L, 92, 1)
        timeMs += duration
    }
    return Song("La Primavera (Vivaldi)", timeMs + 600L, notes, 126.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generateElCondorPasaSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 800L
    val q = 420L
    val melody = listOf(
        69 to q, 71 to q, 72 to q * 2, 71 to q, 69 to q, 67 to q * 2,
        69 to q, 71 to q, 72 to q * 3, 74 to q, 76 to q * 2, 74 to q, 72 to q * 2
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 40L, 85, 1)
        timeMs += duration
    }
    return Song("El Cóndor Pasa", timeMs + 800L, notes, 84.0, isDemo = true, difficulty = Difficulty.INTERMEDIO)
}

fun generateCanonInDSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    var timeMs = 800L
    val q = 350L
    val melody = listOf(
        74 to q * 2, 73 to q * 2, 71 to q * 2, 69 to q * 2,
        67 to q * 2, 66 to q * 2, 67 to q * 2, 69 to q * 2,
        62 to q, 66 to q, 69 to q, 74 to q, 73 to q, 69 to q, 73 to q, 76 to q
    )
    melody.forEach { (pitch, duration) ->
        notes += NoteEvent(pitch, timeMs, duration - 30L, 95, 1)
        timeMs += duration
    }
    return Song("Canon in D (Pachelbel)", timeMs + 800L, notes, 100.0, isDemo = true, difficulty = Difficulty.AVANZADO)
}

object DemoSongs {
    val all: List<Song>
        get() = listOf(
            generateScaleSong(),
            generateHappyBirthdaySong(),
            generateTwinkleTwinkleSong(),
            generateGymnopedieSong(),
            generateBellaCiaoSong(),
            generateKorobeinikiSong(),
            generateLaBambaSong(),
            generateMorningMoodSong(),
            generatePrimaveraSong(),
            generateElCondorPasaSong(),
            generateDemoSong(),
            generateCanonInDSong()
        )
}
