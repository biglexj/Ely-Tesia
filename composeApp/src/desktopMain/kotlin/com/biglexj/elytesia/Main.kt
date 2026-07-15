package com.biglexj.elytesia

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.biglexj.elytesia.midi.DesktopMidiParser
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.storage.DesktopLocalStorage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() {
    val demoFile = File("demo_escala.mid")
    if (!demoFile.exists()) {
        try {
            DesktopMidiParser.generateSampleMidiFile(demoFile)
            println("Archivo de prueba generado en: ${demoFile.absolutePath}")
        } catch (e: Exception) {
            println("Error al generar MIDI de prueba: ${e.message}")
        }
    }

    val midiDemosDir = File("midi_demos")
    if (midiDemosDir.exists() && midiDemosDir.isDirectory) {
        val filesToCheck = listOf("bach_prelude.mid", "escala_do.mid", "bella_ciao.mid", "gymnopedie.mid")
        val needsRecreation = filesToCheck.any {
            val f = File(midiDemosDir, it)
            !f.exists() || f.length() < 100
        }
        if (needsRecreation) {
            try {
                val bach = generateDemoSong()
                val scale = generateScaleSong()
                val bella = generateBellaCiaoSong()
                val gymnopedie = generateGymnopedieSong()

                DesktopMidiParser.writeMidiFile(bach, File(midiDemosDir, "bach_prelude.mid"))
                DesktopMidiParser.writeMidiFile(scale, File(midiDemosDir, "escala_do.mid"))
                DesktopMidiParser.writeMidiFile(bella, File(midiDemosDir, "bella_ciao.mid"))
                DesktopMidiParser.writeMidiFile(gymnopedie, File(midiDemosDir, "gymnopedie.mid"))
                println("Canciones demo iniciales (escala, gymnopedie, bella ciao, bach) generadas físicamente en midi_demos/")
            } catch (e: Exception) {
                println("Error al auto-poblar demos iniciales: ${e.message}")
            }
        }
    }

    application {
        Window(
        onCloseRequest = ::exitApplication,
        title = "Ely-Tesia - Visualizador MIDI",
        icon = painterResource("elytesia-icon.png"),
        state = WindowState(size = DpSize(1280.dp, 820.dp))
    ) {
        val midiManager = remember { getPlatformMidiDeviceManager() }
        val localStorage = remember { DesktopLocalStorage() }
        
        App(
            midiDeviceManager = midiManager,
            localStorage = localStorage,
            onParseMidiBytes = { bytes, name -> DesktopMidiParser.parseMidiBytes(bytes, name) },
            onLoadMidiFile = {
                val fileDialog = FileDialog(null as Frame?, "Seleccionar Archivo MIDI", FileDialog.LOAD)
                fileDialog.file = "*.mid;*.midi"
                fileDialog.isVisible = true
                if (fileDialog.directory != null && fileDialog.file != null) {
                    val file = File(fileDialog.directory, fileDialog.file)
                    try {
                        DesktopMidiParser.parseMidiFile(file)
                    } catch (e: Exception) {
                        println("Error al cargar archivo MIDI: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            },
            onExportMidiFile = { song ->
                val fileDialog = FileDialog(null as Frame?, "Exportar grabación MIDI", FileDialog.SAVE)
                fileDialog.file = "${song.name.replace(" ", "_")}.mid"
                fileDialog.isVisible = true
                if (fileDialog.directory != null && fileDialog.file != null) {
                    runCatching {
                        val chosenName = fileDialog.file.let {
                            if (it.endsWith(".mid", ignoreCase = true)) it else "$it.mid"
                        }
                        DesktopMidiParser.writeMidiFile(song, File(fileDialog.directory, chosenName))
                    }.onFailure { println("Error al exportar MIDI: ${it.message}") }.isSuccess
                } else false
            }
        )
    }
}
}
