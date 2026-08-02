package com.biglexj.elytesia

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.biglexj.elytesia.features.library.*
import com.biglexj.elytesia.midi.DesktopMidiParser
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.storage.DesktopLocalStorage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// ─── Persistencia de estado de ventana ───────────────────────────────────────
private data class PersistentWindowState(
    val widthDp: Int = 1280,
    val heightDp: Int = 820,
    val isMaximized: Boolean = false
) {
    companion object {
        fun load(file: File): PersistentWindowState {
            return runCatching {
                if (!file.isFile) return PersistentWindowState()
                val lines = file.readLines().associate {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                }
                val w = (lines["width"]?.toIntOrNull() ?: 1280).coerceIn(600, 3840)
                val h = (lines["height"]?.toIntOrNull() ?: 820).coerceIn(400, 2160)
                val max = lines["isMaximized"]?.toBooleanStrictOrNull() ?: false
                PersistentWindowState(w, h, max)
            }.getOrDefault(PersistentWindowState())
        }

        fun save(file: File, state: WindowState) {
            runCatching {
                file.parentFile?.mkdirs()
                val isMax = state.placement == WindowPlacement.Maximized
                val newWidth: Int
                val newHeight: Int
                if (isMax) {
                    val prev = load(file)
                    newWidth = prev.widthDp
                    newHeight = prev.heightDp
                } else {
                    newWidth = state.size.width.value.toInt().coerceIn(600, 3840)
                    newHeight = state.size.height.value.toInt().coerceIn(400, 2160)
                }
                file.writeText("width=$newWidth\nheight=$newHeight\nisMaximized=$isMax\n")
            }
        }
    }
}

fun main() {
    val demoFile = File("demo_escala.mid")
    if (!demoFile.exists()) {
        try {
            DesktopMidiParser.generateSampleMidiFile(demoFile)
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
                DesktopMidiParser.writeMidiFile(generateDemoSong(), File(midiDemosDir, "bach_prelude.mid"))
                DesktopMidiParser.writeMidiFile(generateScaleSong(), File(midiDemosDir, "escala_do.mid"))
                DesktopMidiParser.writeMidiFile(generateBellaCiaoSong(), File(midiDemosDir, "bella_ciao.mid"))
                DesktopMidiParser.writeMidiFile(generateGymnopedieSong(), File(midiDemosDir, "gymnopedie.mid"))
            } catch (e: Exception) {
                println("Error al auto-poblar demos: ${e.message}")
            }
        }
    }

    // Forzar que AWT use el hilo correcto desde el inicio en macOS/Windows
    System.setProperty("apple.awt.UIElement", "false")

    // Forzar renderizado por software si el driver GPU/DirectX de Skiko falla en Windows
    System.setProperty("skiko.renderApi", "SOFTWARE_COMPAT")

    application {
        val windowStateFile = File(
            System.getenv("APPDATA") ?: System.getProperty("user.home"),
            "Ely-Tesia/window_state.txt"
        )
        val savedWindowState = remember { PersistentWindowState.load(windowStateFile) }

        val windowState = remember {
            WindowState(
                size = DpSize(savedWindowState.widthDp.dp, savedWindowState.heightDp.dp),
                position = WindowPosition.PlatformDefault,
                placement = WindowPlacement.Floating
            )
        }

        Window(
            onCloseRequest = {
                PersistentWindowState.save(windowStateFile, windowState)
                exitApplication()
            },
            title = "Ely-Tesia - Visualizador MIDI",
            state = windowState
        ) {
            LaunchedEffect(Unit) {
                if (savedWindowState.isMaximized) {
                    windowState.placement = WindowPlacement.Maximized
                }
                window.toFront()
                window.requestFocusInWindow()
            }

            val midiManager = remember { getPlatformMidiDeviceManager() }
            val localStorage = remember { DesktopLocalStorage() }
            var importedThemeJson by remember { mutableStateOf<String?>(null) }

            App(
                midiDeviceManager = midiManager,
                localStorage = localStorage,
                showProgressWhenIdle = false,
                simplifyPlaybackChrome = true,
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
                },
                onRequestThemeFile = {
                    val dialog = FileDialog(null as Frame?, "Importar tema Ely-Tesia", FileDialog.LOAD)
                    dialog.file = "*.elytheme.json;*.json"
                    dialog.isVisible = true
                    if (dialog.directory != null && dialog.file != null) {
                        runCatching { File(dialog.directory, dialog.file).readText() }
                            .onSuccess { importedThemeJson = it }
                            .onFailure { println("Error al importar tema: ${it.message}") }
                    }
                },
                importedThemeJson = importedThemeJson,
                onImportedThemeConsumed = { importedThemeJson = null },
                onRequestExportTheme = { fileName, json ->
                    val dialog = FileDialog(null as Frame?, "Exportar tema Ely-Tesia", FileDialog.SAVE)
                    dialog.file = fileName
                    dialog.isVisible = true
                    if (dialog.directory != null && dialog.file != null) {
                        val chosenName = if (dialog.file.endsWith(".json", true)) dialog.file else "${dialog.file}.elytheme.json"
                        runCatching { File(dialog.directory, chosenName).writeText(json) }
                            .onFailure { println("Error al exportar tema: ${it.message}") }
                    }
                }
            )
        }
    }
}
