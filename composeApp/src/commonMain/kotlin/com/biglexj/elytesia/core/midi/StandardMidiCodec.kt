package com.biglexj.elytesia.midi

import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.Song

/** Minimal Standard MIDI File codec shared by Android and desktop export paths. */
object StandardMidiCodec {
    private data class RawNote(val pitch: Int, val channel: Int, val velocity: Int, val start: Long, val end: Long)
    private data class Tempo(val tick: Long, val mpq: Int)
    private data class RawControl(val controller: Int, val value: Int, val tick: Long, val channel: Int)

    fun decode(bytes: ByteArray, name: String): Song {
        val reader = Reader(bytes)
        require(reader.ascii(4) == "MThd") { "El archivo no contiene una cabecera MIDI válida" }
        val headerLength = reader.u32().toInt()
        reader.u16() // format
        val trackCount = reader.u16()
        val division = reader.u16()
        require((division and 0x8000) == 0) { "Los archivos MIDI SMPTE todavía no son compatibles" }
        val resolution = division.coerceAtLeast(1)
        if (headerLength > 6) reader.skip(headerLength - 6)

        val notes = mutableListOf<RawNote>()
        val tempos = mutableListOf(Tempo(0, 500_000))
        val controls = mutableListOf<RawControl>()

        repeat(trackCount) { trackIndex ->
            val sig = reader.ascii(4)
            require(sig == "MTrk") { "Pista MIDI no válida: se esperaba MTrk pero se leyó '$sig' en posición ${reader.position - 4}" }
            // The track size starts after its four-byte length field. Keeping the
            // read separate is important: using `position + u32()` captures the
            // position before u32() advances it and truncates every track by four
            // bytes. On format-1 files that also makes the next MTrk unreadable.
            val trackLength = reader.u32().toInt()
            val trackEnd = reader.position + trackLength
            var tick = 0L
            var runningStatus = 0
            val active = mutableMapOf<Int, Pair<Long, Int>>()

            while (reader.position < trackEnd) {
                tick += reader.vlq()
                var status = reader.u8()
                if (status < 0x80) {
                    require(runningStatus != 0) { "Running status MIDI inválido" }
                    reader.position--
                    status = runningStatus
                } else if (status < 0xF0) {
                    runningStatus = status
                }

                when {
                    status == 0xFF -> {
                        val type = reader.u8()
                        val length = reader.vlq().toInt()
                        if (type == 0x51 && length == 3) {
                            tempos += Tempo(tick, (reader.u8() shl 16) or (reader.u8() shl 8) or reader.u8())
                        } else reader.skip(length)
                    }
                    status == 0xF0 || status == 0xF7 -> reader.skip(reader.vlq().toInt())
                    else -> {
                        val command = status and 0xF0
                        val channel = status and 0x0F
                        val data1 = reader.u8()
                        val data2 = if (command == 0xC0 || command == 0xD0) 0 else reader.u8()
                        if (command == 0x90 && data2 > 0) {
                            val key = channel * 128 + data1
                            active.remove(key)?.let { previous ->
                                notes += RawNote(data1, channel, previous.second, previous.first, tick)
                            }
                            active[key] = tick to data2
                        } else if (command == 0x80 || (command == 0x90 && data2 == 0)) {
                            val key = channel * 128 + data1
                            active.remove(key)?.let { started ->
                                notes += RawNote(data1, channel, started.second, started.first, tick)
                            }
                        } else if (command == 0xB0) {
                            controls += RawControl(data1, data2, tick, channel)
                        }
                    }
                }
            }
            reader.position = trackEnd
        }

        val tempoMap = tempos.groupBy { it.tick }.map { (_, values) -> values.last() }.sortedBy { it.tick }
        fun tickToMs(target: Long): Long {
            var elapsedMicros = 0.0
            var cursor = 0L
            var mpq = 500_000
            for (tempo in tempoMap) {
                if (tempo.tick > target) break
                elapsedMicros += (tempo.tick - cursor).toDouble() * mpq / resolution
                cursor = tempo.tick
                mpq = tempo.mpq
            }
            elapsedMicros += (target - cursor).toDouble() * mpq / resolution
            return (elapsedMicros / 1000.0).toLong()
        }

        val decoded = notes.map {
            val start = tickToMs(it.start)
            val end = tickToMs(it.end)
            NoteEvent(it.pitch, start, (end - start).coerceAtLeast(10), it.velocity, it.channel)
        }.sortedBy { it.startTimeMs }
        val duration = decoded.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
        val bpm = 60_000_000.0 / (tempoMap.firstOrNull()?.mpq ?: 500_000)
        val decodedControls = controls.map { ControlEvent(it.controller, it.value, tickToMs(it.tick), it.channel) }
        val parsedSong = Song(name.substringBeforeLast('.').replace('_', ' ').replace('-', ' '), duration, decoded, bpm, decodedControls)
        return parsedSong.copy(difficulty = parsedSong.calculateAutoDifficulty())
    }

    fun encode(song: Song): ByteArray {
        val resolution = 480
        val mpq = (60_000_000.0 / song.bpm.coerceAtLeast(1.0)).toInt()
        data class Event(val tick: Long, val priority: Int, val data: ByteArray)
        fun msToTick(ms: Long) = (ms.toDouble() * resolution * 1000.0 / mpq).toLong()
        val events = mutableListOf(Event(0, 0, byteArrayOf(0xFF.toByte(), 0x51, 0x03, (mpq shr 16).toByte(), (mpq shr 8).toByte(), mpq.toByte())))
        song.notes.forEach { note ->
            val channel = note.track.coerceIn(0, 15)
            events += Event(msToTick(note.startTimeMs), 2, byteArrayOf((0x90 or channel).toByte(), note.pitch.toByte(), note.velocity.toByte()))
            events += Event(msToTick(note.startTimeMs + note.durationMs), 1, byteArrayOf((0x80 or channel).toByte(), note.pitch.toByte(), 0))
        }
        song.controls.forEach { control ->
            events += Event(
                msToTick(control.timeMs),
                0,
                byteArrayOf((0xB0 or control.channel.coerceIn(0, 15)).toByte(), control.controller.toByte(), control.value.toByte())
            )
        }
        val track = mutableListOf<Byte>()
        var previousTick = 0L
        events.sortedWith(compareBy<Event> { it.tick }.thenBy { it.priority }).forEach { event ->
            track += encodeVlq(event.tick - previousTick).toList()
            track += event.data.toList()
            previousTick = event.tick
        }
        track += byteArrayOf(0, 0xFF.toByte(), 0x2F, 0).toList()

        return buildList<Byte> {
            addAll("MThd".encodeToByteArray().toList()); addAll(u32(6).toList())
            addAll(u16(0).toList()); addAll(u16(1).toList()); addAll(u16(resolution).toList())
            addAll("MTrk".encodeToByteArray().toList()); addAll(u32(track.size).toList()); addAll(track)
        }.toByteArray()
    }

    private fun encodeVlq(value: Long): ByteArray {
        var current = value.coerceAtLeast(0)
        val reversed = mutableListOf((current and 0x7F).toByte())
        current = current shr 7
        while (current > 0) { reversed += ((current and 0x7F) or 0x80).toByte(); current = current shr 7 }
        return reversed.reversed().toByteArray()
    }
    private fun u16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())
    private fun u32(value: Int) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    private class Reader(private val bytes: ByteArray) {
        var position = 0
        fun u8(): Int = bytes[position++].toInt() and 0xFF
        fun u16(): Int = (u8() shl 8) or u8()
        fun u32(): Long = (u8().toLong() shl 24) or (u8().toLong() shl 16) or (u8().toLong() shl 8) or u8().toLong()
        fun ascii(length: Int): String = bytes.copyOfRange(position, position + length).decodeToString().also { position += length }
        fun skip(length: Int) { position += length }
        fun vlq(): Long { var result = 0L; do { val b = u8(); result = (result shl 7) or (b and 0x7F).toLong() } while ((b and 0x80) != 0); return result }
    }
}
