package com.biglexj.elytesia.storage

import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.Song

interface LocalStorage {
    fun read(): String?
    fun write(contents: String)
}

object NoOpLocalStorage : LocalStorage {
    override fun read(): String? = null
    override fun write(contents: String) = Unit
}

data class SavedAppState(
    val minPitch: Int = 21,
    val maxPitch: Int = 108,
    val internalSoundEnabled: Boolean = true,
    val noteLabelMode: String = "NONE",
    val selectedAudioDevice: String = "Sistema (Predeterminado)",
    val selectedSongName: String? = null,
    val selectedInstrument: String = "PIANO_ACUSTICO",
    val songs: List<Song> = emptyList()
)

object AppStateCodec {
    fun encode(state: SavedAppState): String = buildString {
        appendLine("ELYTESIA_STATE_1")
        appendLine("range=${state.minPitch},${state.maxPitch}")
        appendLine("sound=${state.internalSoundEnabled}")
        appendLine("labels=${state.noteLabelMode}")
        appendLine("audio=${escape(state.selectedAudioDevice)}")
        appendLine("selected=${escape(state.selectedSongName.orEmpty())}")
        appendLine("instrument=${state.selectedInstrument}")
        state.songs.forEach { song ->
            val notes = song.notes.joinToString(";") {
                "${it.pitch},${it.startTimeMs},${it.durationMs},${it.velocity},${it.track}"
            }
            val controls = song.controls.joinToString(";") {
                "${it.controller},${it.value},${it.timeMs},${it.channel}"
            }
            appendLine("song=${escape(song.name)}|${song.durationMs}|${song.bpm}|$notes|$controls")
        }
    }

    fun decode(text: String?): SavedAppState? {
        if (text == null) return null
        val lines = text.lineSequence().toList()
        if (lines.firstOrNull() != "ELYTESIA_STATE_1") return null
        return runCatching {
            val range = lines.firstOrNull { it.startsWith("range=") }
                ?.removePrefix("range=")?.split(',')
            val songs = lines.filter { it.startsWith("song=") }.map { line ->
                val fields = line.removePrefix("song=").split('|', limit = 5)
                val notes = if (fields[3].isBlank()) emptyList() else fields[3].split(';').map { encoded ->
                    val n = encoded.split(',')
                    NoteEvent(n[0].toInt(), n[1].toLong(), n[2].toLong(), n[3].toInt(), n[4].toInt())
                }
                val controls = fields.getOrNull(4)?.takeIf { it.isNotBlank() }?.split(';')?.map { encoded ->
                    val c = encoded.split(',')
                    ControlEvent(c[0].toInt(), c[1].toInt(), c[2].toLong(), c[3].toInt())
                }.orEmpty()
                Song(unescape(fields[0]), fields[1].toLong(), notes, fields[2].toDouble(), controls)
            }
            SavedAppState(
                minPitch = range?.getOrNull(0)?.toIntOrNull() ?: 21,
                maxPitch = range?.getOrNull(1)?.toIntOrNull() ?: 108,
                internalSoundEnabled = lines.firstOrNull { it.startsWith("sound=") }
                    ?.removePrefix("sound=")?.toBooleanStrictOrNull() ?: true,
                noteLabelMode = lines.firstOrNull { it.startsWith("labels=") }
                    ?.removePrefix("labels=") ?: "NONE",
                selectedAudioDevice = lines.firstOrNull { it.startsWith("audio=") }
                    ?.removePrefix("audio=")?.let(::unescape) ?: "Sistema (Predeterminado)",
                selectedSongName = lines.firstOrNull { it.startsWith("selected=") }
                    ?.removePrefix("selected=")?.let(::unescape)?.ifBlank { null },
                selectedInstrument = lines.firstOrNull { it.startsWith("instrument=") }
                    ?.removePrefix("instrument=") ?: "PIANO_ACUSTICO",
                songs = songs
            )
        }.getOrNull()
    }

    private fun escape(value: String): String = value
        .replace("%", "%25").replace("|", "%7C").replace("\n", "%0A").replace("\r", "%0D")

    private fun unescape(value: String): String = value
        .replace("%0D", "\r").replace("%0A", "\n").replace("%7C", "|").replace("%25", "%")
}
