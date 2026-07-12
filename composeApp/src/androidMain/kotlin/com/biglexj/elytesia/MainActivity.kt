package com.biglexj.elytesia

import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.biglexj.elytesia.midi.StandardMidiCodec
import com.biglexj.elytesia.midi.AndroidMidiDeviceManager
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.storage.AndroidLocalStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val localStorage = remember { AndroidLocalStorage(this@MainActivity) }
            val midiDeviceManager = remember { AndroidMidiDeviceManager(this@MainActivity) }
            var importedSong by remember { mutableStateOf<Song?>(null) }
            var pendingExport by remember { mutableStateOf<Song?>(null) }

            val openMidi = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    runCatching {
                        val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                            ?: "Canción MIDI"
                        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("No se pudo leer el archivo")
                        StandardMidiCodec.decode(bytes, displayName)
                    }.onSuccess { importedSong = it }
                        .onFailure { Toast.makeText(this@MainActivity, "MIDI no válido: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }

            val saveMidi = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/midi")) { uri ->
                val song = pendingExport
                if (uri != null && song != null) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { it.write(StandardMidiCodec.encode(song)) }
                            ?: error("No se pudo crear el archivo")
                    }.onSuccess {
                        Toast.makeText(this@MainActivity, "MIDI exportado", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this@MainActivity, "Error al exportar: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
                pendingExport = null
            }

            App(
                localStorage = localStorage,
                midiDeviceManager = midiDeviceManager,
                onRequestMidiFile = {
                    openMidi.launch(arrayOf("audio/midi", "audio/x-midi", "application/octet-stream"))
                },
                importedSong = importedSong,
                onImportedSongConsumed = { importedSong = null },
                onRequestExportMidiFile = { song ->
                    pendingExport = song
                    saveMidi.launch("${song.name.replace(' ', '_')}.mid")
                }
            )
        }
    }
}
