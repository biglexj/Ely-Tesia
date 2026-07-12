package com.biglexj.elytesia.midi

import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.Song
import java.io.File
import javax.sound.midi.*

object DesktopMidiParser {
    fun writeMidiFile(song: Song, file: File) {
        val resolution = 480
        val sequence = Sequence(Sequence.PPQ, resolution)
        val track = sequence.createTrack()
        val mpq = (60000000.0 / song.bpm.coerceAtLeast(1.0)).toInt()

        val tempo = MetaMessage()
        tempo.setMessage(
            0x51,
            byteArrayOf((mpq shr 16).toByte(), (mpq shr 8).toByte(), mpq.toByte()),
            3
        )
        track.add(MidiEvent(tempo, 0L))

        fun msToTick(ms: Long): Long =
            ((ms.toDouble() * resolution * 1000.0) / mpq.toDouble()).toLong()

        song.notes.forEach { note ->
            val on = ShortMessage().apply {
                setMessage(ShortMessage.NOTE_ON, note.track.coerceIn(0, 15), note.pitch, note.velocity)
            }
            val off = ShortMessage().apply {
                setMessage(ShortMessage.NOTE_OFF, note.track.coerceIn(0, 15), note.pitch, 0)
            }
            track.add(MidiEvent(on, msToTick(note.startTimeMs)))
            track.add(MidiEvent(off, msToTick(note.startTimeMs + note.durationMs)))
        }
        song.controls.forEach { control ->
            val message = ShortMessage().apply {
                setMessage(
                    ShortMessage.CONTROL_CHANGE,
                    control.channel.coerceIn(0, 15),
                    control.controller,
                    control.value
                )
            }
            track.add(MidiEvent(message, msToTick(control.timeMs)))
        }

        file.parentFile?.mkdirs()
        MidiSystem.write(sequence, 1, file)
    }

    fun parseMidiFile(file: File): Song {
        val sequence = MidiSystem.getSequence(file)
        val notes = mutableListOf<NoteEvent>()
        val controls = mutableListOf<ControlEvent>()
        
        val resolution = sequence.resolution
        
        // Mapear cambios de tempo para convertir ticks en milisegundos exactos
        class TempoEvent(val tick: Long, val mpq: Long)
        val tempoEvents = mutableListOf<TempoEvent>()
        
        for (track in sequence.tracks) {
            for (i in 0 until track.size()) {
                val event = track.get(i)
                val msg = event.message
                if (msg is MetaMessage && msg.type == 0x51) {
                    val data = msg.data
                    val mpq = ((data[0].toInt() and 0xFF) shl 16) or
                              ((data[1].toInt() and 0xFF) shl 8) or
                              (data[2].toInt() and 0xFF)
                    tempoEvents.add(TempoEvent(event.tick, mpq.toLong()))
                }
            }
        }
        
        // Tempo por defecto: 120 BPM (500,000 microsegundos por negra)
        if (tempoEvents.isEmpty()) {
            tempoEvents.add(TempoEvent(0L, 500000L))
        }
        tempoEvents.sortBy { it.tick }
        
        // Función para convertir ticks del archivo MIDI a milisegundos
        fun tickToMs(tick: Long): Long {
            var ms = 0.0
            var currentTick = 0L
            var currentMpq = 500000L
            
            for (tempo in tempoEvents) {
                if (tick <= tempo.tick) {
                    break
                }
                val ticksPassed = tempo.tick - currentTick
                ms += (ticksPassed.toDouble() / resolution.toDouble()) * (currentMpq / 1000.0)
                currentTick = tempo.tick
                currentMpq = tempo.mpq
            }
            
            val remainingTicks = tick - currentTick
            ms += (remainingTicks.toDouble() / resolution.toDouble()) * (currentMpq / 1000.0)
            return ms.toLong()
        }

        // Emparejar eventos Note ON y Note OFF para calcular duraciones
        for (trackIdx in 0 until sequence.tracks.size) {
            val track = sequence.tracks[trackIdx]
            val activeNotes = mutableMapOf<Int, ShortMessage>()
            val activeNoteTicks = mutableMapOf<Int, Long>()
            
            for (i in 0 until track.size()) {
                val event = track.get(i)
                val msg = event.message
                if (msg is ShortMessage) {
                    val pitch = msg.data1
                    val velocity = msg.data2
                    
                    if (msg.command == ShortMessage.CONTROL_CHANGE) {
                        controls.add(ControlEvent(msg.data1, msg.data2, tickToMs(event.tick), msg.channel))
                    } else if (msg.command == ShortMessage.NOTE_ON && velocity > 0) {
                        if (activeNotes.containsKey(pitch)) {
                            val startTick = activeNoteTicks[pitch]!!
                            val prevOn = activeNotes[pitch]!!
                            val durationMs = tickToMs(event.tick) - tickToMs(startTick)
                            notes.add(
                                NoteEvent(
                                    pitch = pitch,
                                    startTimeMs = tickToMs(startTick),
                                    durationMs = durationMs.coerceAtLeast(10),
                                    velocity = prevOn.data2,
                                    track = trackIdx
                                )
                            )
                        }
                        activeNotes[pitch] = msg
                        activeNoteTicks[pitch] = event.tick
                    } else if (msg.command == ShortMessage.NOTE_OFF || (msg.command == ShortMessage.NOTE_ON && velocity == 0)) {
                        if (activeNotes.containsKey(pitch)) {
                            val startTick = activeNoteTicks[pitch]!!
                            val prevOn = activeNotes[pitch]!!
                            val durationMs = tickToMs(event.tick) - tickToMs(startTick)
                            notes.add(
                                NoteEvent(
                                    pitch = pitch,
                                    startTimeMs = tickToMs(startTick),
                                    durationMs = durationMs.coerceAtLeast(10),
                                    velocity = prevOn.data2,
                                    track = trackIdx
                                )
                            )
                            activeNotes.remove(pitch)
                            activeNoteTicks.remove(pitch)
                        }
                    }
                }
            }
        }
        
        notes.sortBy { it.startTimeMs }
        val durationMs = if (notes.isNotEmpty()) notes.last().startTimeMs + notes.last().durationMs else 0L
        
        val initialMpq = tempoEvents.firstOrNull()?.mpq ?: 500000L
        val initialBpm = 60000000.0 / initialMpq

        return Song(
            name = file.nameWithoutExtension.replace("_", " ").replace("-", " "),
            durationMs = durationMs,
            notes = notes,
            bpm = initialBpm
            ,controls = controls.sortedBy { it.timeMs }
        )
    }

    fun generateSampleMidiFile(file: File) {
        val sequence = Sequence(Sequence.PPQ, 24)
        val track = sequence.createTrack()
        
        // Tempo: 120 BPM (500000 mpq)
        val tempoMsg = MetaMessage()
        tempoMsg.setMessage(0x51, byteArrayOf(0x07, 0xA1.toByte(), 0x20), 3)
        track.add(MidiEvent(tempoMsg, 0L))
        
        // Escala de Do Mayor: C4 a C5
        val scaleNotes = listOf(60, 62, 64, 65, 67, 69, 71, 72)
        var tick = 10L
        for (note in scaleNotes) {
            val on = ShortMessage()
            on.setMessage(0x90, 0, note, 90)
            track.add(MidiEvent(on, tick))
            
            val off = ShortMessage()
            off.setMessage(0x80, 0, note, 0)
            track.add(MidiEvent(off, tick + 18))
            
            tick += 24
        }
        
        MidiSystem.write(sequence, 1, file)
    }
}
