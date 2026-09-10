package com.biglexj.elytesia.features.player

import com.biglexj.elytesia.features.player.PlaybackLogic
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackLogicTest {
    private val earlier = NoteEvent(60, 100L, 500L, 80, 2)
    private val later = NoteEvent(60, 200L, 500L, 90, 1)
    private val song = Song("Prueba", 1_000L, listOf(earlier, later))

    @Test
    fun activeNotesRespectEndBoundary() {
        assertEquals(listOf(earlier, later), PlaybackLogic.activeNotesAt(song, 300L))
        assertEquals(listOf(later), PlaybackLogic.activeNotesAt(song, 600L))
    }

    @Test
    fun expectedNoteUsesToleranceAndMostRecentNote() {
        assertEquals(later, PlaybackLogic.expectedNote(song, 60, 180L, 30L))
        assertNull(PlaybackLogic.expectedNote(song, 61, 180L, 30L))
    }

    @Test
    fun visibleNoteUsesMostRecentTrackAssignment() {
        assertEquals(later, PlaybackLogic.visibleNotesByPitch(listOf(earlier, later))[60])
    }

    @Test
    fun timelineIndexKeepsLongNotesVisibleAfterTheirStart() {
        val longNote = NoteEvent(48, 0L, 5_000L, 80, 0)
        val upcomingNote = NoteEvent(72, 3_000L, 200L, 80, 1)
        val index = PlaybackLogic.TimelineIndex(listOf(upcomingNote, longNote))

        assertEquals(listOf(longNote), index.activeNotesAt(2_500L))
        assertEquals(listOf(longNote, upcomingNote), index.visibleNotesAt(2_500L, 1_000L))
    }

    @Test
    fun timelineIndexDoesNotReturnNotesOutsideVisibleWindow() {
        val beforeWindow = NoteEvent(48, 0L, 500L, 80, 0)
        val afterWindow = NoteEvent(72, 4_000L, 200L, 80, 1)
        val index = PlaybackLogic.TimelineIndex(listOf(beforeWindow, afterWindow))

        assertEquals(emptyList(), index.visibleNotesAt(1_000L, 1_000L))
    }

    @Test
    fun repeatedConsecutiveNotesProduceDistinctActiveEvents() {
        val note1 = NoteEvent(64, 1_000L, 200L, 80, 0)
        val note2 = NoteEvent(64, 1_200L, 200L, 85, 0)
        val note3 = NoteEvent(64, 1_400L, 200L, 90, 0)
        val testSong = Song("RepeatedNotes", 2_000L, listOf(note1, note2, note3))
        val index = PlaybackLogic.TimelineIndex(testSong.notes)

        assertEquals(listOf(note1), index.activeNotesAt(1_100L))
        assertEquals(listOf(note2), index.activeNotesAt(1_300L))
        assertEquals(listOf(note3), index.activeNotesAt(1_500L))
    }
}
