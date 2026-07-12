package com.biglexj.elytesia.midi

import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardMidiCodecTest {
    @Test
    fun roundTripPreservesNotesVelocityAndSustain() {
        val original = Song(
            name = "Prueba Android",
            durationMs = 1500,
            bpm = 120.0,
            notes = listOf(
                NoteEvent(60, 0, 500, 96, 0),
                NoteEvent(64, 500, 1000, 72, 1)
            ),
            controls = listOf(
                ControlEvent(64, 127, 450, 0),
                ControlEvent(64, 0, 1400, 0)
            )
        )

        val decoded = StandardMidiCodec.decode(StandardMidiCodec.encode(original), "prueba.mid")

        assertEquals(2, decoded.notes.size)
        assertEquals(listOf(60, 64), decoded.notes.map { it.pitch })
        assertEquals(listOf(96, 72), decoded.notes.map { it.velocity })
        assertEquals(listOf(127, 0), decoded.controls.filter { it.controller == 64 }.map { it.value })
        assertTrue(decoded.notes.zip(original.notes).all { (actual, expected) ->
            kotlin.math.abs(actual.startTimeMs - expected.startTimeMs) <= 3 &&
                kotlin.math.abs(actual.durationMs - expected.durationMs) <= 3
        })
    }
}
