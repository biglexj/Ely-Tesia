package com.biglexj.elytesia.midi

import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.generateBellaCiaoSong
import com.biglexj.elytesia.generateDemoSong
import com.biglexj.elytesia.generateGymnopedieSong
import com.biglexj.elytesia.generateScaleSong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardMidiCodecTest {
    @Test
    fun bundledFallbackDemosArePlayable() {
        val demos = listOf(
            generateScaleSong(),
            generateGymnopedieSong(),
            generateBellaCiaoSong(),
            generateDemoSong()
        )

        assertTrue(demos.all { it.notes.isNotEmpty() })
        assertTrue(demos.all { it.durationMs > 0L })
    }

    @Test
    fun decodesFormatOneMidiWithMultipleTracks() {
        fun ascii(value: String) = value.encodeToByteArray().toList()
        fun u16(value: Int) = listOf((value shr 8).toByte(), value.toByte())
        fun u32(value: Int) = listOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
        fun track(events: List<Byte>) = ascii("MTrk") + u32(events.size) + events

        val tempoTrack = listOf<Byte>(
            0, 0xFF.toByte(), 0x51, 3, 0x07, 0xA1.toByte(), 0x20,
            0, 0xFF.toByte(), 0x2F, 0
        )
        val noteTrack = listOf<Byte>(
            0, 0x90.toByte(), 60, 100,
            0x83.toByte(), 0x60, 0x80.toByte(), 60, 0,
            0, 0xFF.toByte(), 0x2F, 0
        )
        val midi = buildList<Byte> {
            addAll(ascii("MThd")); addAll(u32(6))
            addAll(u16(1)); addAll(u16(2)); addAll(u16(480))
            addAll(track(tempoTrack)); addAll(track(noteTrack))
        }.toByteArray()

        val decoded = StandardMidiCodec.decode(midi, "dos_pistas.mid")

        assertEquals(1, decoded.notes.size)
        assertEquals(60, decoded.notes.single().pitch)
        assertEquals(500, decoded.notes.single().durationMs)
        assertEquals(500, decoded.durationMs)
    }

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
