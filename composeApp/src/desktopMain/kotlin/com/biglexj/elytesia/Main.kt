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

    application {
        Window(
        onCloseRequest = ::exitApplication,
        title = "Ely-Tesia - Visualizador MIDI",
        icon = painterResource("elytesia-icon.png"),
        state = WindowState(size = DpSize(1280.dp, 820.dp))
    ) {
        val midiManager = getPlatformMidiDeviceManager()
        val localStorage = remember { DesktopLocalStorage() }
        
        App(
            midiDeviceManager = midiManager,
            localStorage = localStorage,
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
