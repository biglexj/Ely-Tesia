package com.biglexj.elytesia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.biglexj.elytesia.midi.MidiDeviceManager
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.midi.InstrumentType
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.model.Difficulty
import com.biglexj.elytesia.storage.AppStateCodec
import com.biglexj.elytesia.storage.LocalStorage
import com.biglexj.elytesia.storage.NoOpLocalStorage
import com.biglexj.elytesia.storage.SavedAppState
import com.biglexj.elytesia.theme.*
import com.biglexj.elytesia.ui.PianoKeyboard
import com.biglexj.elytesia.ui.NoteLabelMode
import com.biglexj.elytesia.ui.PianoRollCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ely_tesia.composeapp.generated.resources.*

enum class SidebarMode { BIBLIOTECA, INSTRUMENTOS, TEMAS }

@OptIn(ExperimentalMaterial3Api::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
internal fun ElyTesiaAppContent(
    midiDeviceManager: MidiDeviceManager = remember { getPlatformMidiDeviceManager() },
    onLoadMidiFile: (() -> Song?)? = null,
    onRequestMidiFile: (() -> Unit)? = null,
    importedSong: Song? = null,
    onImportedSongConsumed: (() -> Unit)? = null,
    onExportMidiFile: ((Song) -> Boolean)? = null,
    onRequestExportMidiFile: ((Song) -> Unit)? = null,
    localStorage: LocalStorage = NoOpLocalStorage,
    onParseMidiBytes: ((ByteArray, String) -> Song)? = null,
    onPracticeActivityChanged: (Boolean) -> Unit = {},
    onRequestThemeFile: (() -> Unit)? = null,
    importedThemeJson: String? = null,
    onImportedThemeConsumed: (() -> Unit)? = null,
    onRequestExportTheme: ((String, String) -> Unit)? = null,
    showProgressWhenIdle: Boolean = true,
    simplifyPlaybackChrome: Boolean = false,
    centerPlaybackControls: Boolean = false
) {
    val restoredState = remember(localStorage) { AppStateCodec.decode(localStorage.read()) }
    val restoredThemes = remember(restoredState) {
        restoredState?.importedThemes.orEmpty().mapNotNull { ThemeJsonCodec.decode(it).getOrNull() }
    }
    val installedThemes = remember {
        mutableStateListOf<ElyThemeDefinition>().apply {
            addAll(ThemeDefaults.builtIns)
            restoredThemes.forEach { restored -> if (none { it.id == restored.id }) add(restored) }
        }
    }
    var selectedThemeId by remember { mutableStateOf(restoredState?.selectedThemeId ?: ThemeDefaults.Aurora.id) }
    var useDynamicColor by remember { mutableStateOf(restoredState?.useDynamicColor ?: false) }
    var stateInitialized by remember { mutableStateOf(false) }
    var loadedSong by remember { mutableStateOf<Song?>(null) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isWaitingForMidiTrigger by remember { mutableStateOf(false) }
    var loopEnabled by remember { mutableStateOf(false) }
    var playbackRestartToken by remember { mutableStateOf(0) }
    var waitMode by remember { mutableStateOf(false) } // Modo Espera
    
    // Controles de Tempo y Metrónomo
    var speedMultiplier by remember { mutableStateOf(1.0f) }
    var metronomeEnabled by remember { mutableStateOf(false) }
    var internalSoundEnabled by remember { mutableStateOf(restoredState?.internalSoundEnabled ?: true) }
    var noteLabelMode by remember {
        mutableStateOf(
            restoredState?.noteLabelMode?.let { saved ->
                NoteLabelMode.entries.firstOrNull { it.name == saved }
            } ?: NoteLabelMode.NONE
        )
    }
    
    // Edición de BPM por teclado
    var isEditingBpm by remember { mutableStateOf(false) }
    var bpmInputText by remember { mutableStateOf("") }
    
    // Límites de teclado dinámicos (Mapeo Interactivo)
    var minPitch by remember { mutableStateOf(restoredState?.minPitch ?: 21) } // La0 por defecto
    var maxPitch by remember { mutableStateOf(restoredState?.maxPitch ?: 108) } // Do8 por defecto
    var visibleMinPitch by remember { mutableStateOf(restoredState?.minPitch ?: 21) }
    var visibleMaxPitch by remember { mutableStateOf(restoredState?.maxPitch ?: 108) }
    var keyboardZoomAccumulator by remember { mutableStateOf(1f) }
    
    var mappingMode by remember { mutableStateOf(false) }
    var mappingStep by remember { mutableStateOf(0) } // 0 = tecla inicio, 1 = tecla fin
    var tempMinPitch by remember { mutableStateOf(21) }

    LaunchedEffect(minPitch, maxPitch) {
        visibleMinPitch = minPitch
        visibleMaxPitch = maxPitch
        keyboardZoomAccumulator = 1f
    }

    // Biblioteca / Sidebar
    var activeSidebar by remember { mutableStateOf<SidebarMode?>(null) }
    var selectedInstrument by remember {
        mutableStateOf(
            restoredState?.selectedInstrument?.let { saved ->
                InstrumentType.entries.firstOrNull { it.name == saved }
            } ?: InstrumentType.PIANO_ACUSTICO
        )
    }
    val songList = remember { mutableStateListOf<Song>() }

    val activeKeys = remember { mutableStateMapOf<Int, Int>() } // Notas activas de la reproducción
    val activeKeyTracks = remember { mutableStateMapOf<Int, Int>() }
    val userActiveKeys = remember { mutableStateListOf<Int>() }   // Notas físicas o por mouse
    val wrongUserKeys = remember { mutableStateListOf<Int>() }
    val userKeyTracks = remember { mutableStateMapOf<Int, Int>() }

    // Grabación MIDI en tiempo real
    var isRecording by remember { mutableStateOf(false) }
    var isCountInActive by remember { mutableStateOf(false) }
    var countInBars by remember { mutableStateOf(1) }
    var countInBeat by remember { mutableStateOf<Int?>(null) }
    var recordingStartMs by remember { mutableStateOf(0L) }
    val recordedNotes = remember { mutableStateListOf<NoteEvent>() }
    val recordedControls = remember { mutableStateListOf<ControlEvent>() }
    val recordingActiveNotes = remember { mutableMapOf<Int, Pair<Long, Int>>() }
    var lastRecording by remember { mutableStateOf<Song?>(null) }
    var recordingName by remember { mutableStateOf("") }
    var exportSucceeded by remember { mutableStateOf<Boolean?>(null) }
    var userMessage by remember { mutableStateOf<String?>(null) }
    
    // Dispositivos MIDI físicos
    var availableDevices by remember { mutableStateOf(emptyList<String>()) }
    var selectedDevice by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Salidas de Audio
    var audioDevices by remember { mutableStateOf(emptyList<String>()) }
    var selectedAudioDevice by remember {
        mutableStateOf(restoredState?.selectedAudioDevice ?: "Sistema (Predeterminado)")
    }
    var audioDropdownExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun refreshMidiDevices() {
        runCatching { midiDeviceManager.getAvailableDevices() }
            .onSuccess { devices ->
                availableDevices = devices
                if (selectedDevice !in devices) selectedDevice = devices.firstOrNull().orEmpty()
            }
            .onFailure { error ->
                availableDevices = emptyList()
                selectedDevice = ""
                userMessage = "No se pudieron consultar los dispositivos MIDI: ${error.message ?: "error desconocido"}"
            }
    }

    fun refreshAudioDevices() {
        runCatching { midiDeviceManager.getAudioOutputs() }
            .onSuccess { audioDevices = it }
            .onFailure { error ->
                audioDevices = emptyList()
                userMessage = "No se pudieron consultar las salidas de audio: ${error.message ?: "error desconocido"}"
            }
    }

    LaunchedEffect(isPlaying, isRecording) {
        onPracticeActivityChanged(isPlaying || isRecording)
    }
    DisposableEffect(Unit) {
        onDispose { onPracticeActivityChanged(false) }
    }

    fun recordNoteOn(pitch: Int, velocity: Int) {
        if (!isRecording || pitch in recordingActiveNotes) return
        recordingActiveNotes[pitch] =
            (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(0L) to velocity
    }

    fun evaluatePlayedPitch(pitch: Int) {
        userKeyTracks.remove(pitch)
        if (!isPlaying && !waitMode) return
        val song = loadedSong ?: return
        val toleranceMs = 180L
        val expectedNote = PlaybackLogic.expectedNote(song, pitch, currentTimeMs, toleranceMs)
        if (expectedNote == null) {
            if (pitch !in wrongUserKeys) wrongUserKeys.add(pitch)
        } else {
            wrongUserKeys.remove(pitch)
            // El color queda fijado durante toda esta pulsación física.
            userKeyTracks[pitch] = expectedNote.track
        }
    }

    fun recordNoteOff(pitch: Int) {
        if (!isRecording) return
        val started = recordingActiveNotes.remove(pitch) ?: return
        val endMs = (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(started.first + 10L)
        recordedNotes.add(
            NoteEvent(pitch, started.first, endMs - started.first, started.second, 0)
        )
    }

    fun recordControlChange(controller: Int, value: Int, channel: Int) {
        if (!isRecording) return
        recordedControls.add(
            ControlEvent(controller, value, (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(0L), channel)
        )
    }

    fun finishRecording() {
        if (!isRecording) return
        val now = (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(0L)
        recordingActiveNotes.toMap().forEach { (pitch, started) ->
            recordedNotes.add(
                NoteEvent(pitch, started.first, (now - started.first).coerceAtLeast(10L), started.second, 0)
            )
        }
        recordingActiveNotes.clear()
        isRecording = false

        if (recordedNotes.isNotEmpty()) {
            val defaultName = "Grabación ${songList.count { it.name.startsWith("Grabación") } + 1}"
            val duration = recordedNotes.maxOf { it.startTimeMs + it.durationMs }
            val song = Song(defaultName, duration, recordedNotes.sortedBy { it.startTimeMs }, loadedSong?.bpm ?: 120.0, recordedControls.toList())
            songList.add(song)
            loadedSong = song
            lastRecording = song
            recordingName = song.name
            currentTimeMs = 0L
            exportSucceeded = null
        }
    }

    fun startRecording() {
        if (isRecording || isCountInActive) return
        scope.launch {
            isPlaying = false
            midiDeviceManager.stopAllNotes()
            recordedNotes.clear()
            recordedControls.clear()
            recordingActiveNotes.clear()
            isCountInActive = countInBars > 0

            val bpm = loadedSong?.bpm ?: 120.0
            val beatDurationMs = (60000.0 / bpm).toLong().coerceAtLeast(100L)
            val totalBeats = countInBars * 4
            for (remaining in totalBeats downTo 1) {
                countInBeat = remaining
                midiDeviceManager.playMetronomeClick()
                delay(beatDurationMs)
            }

            countInBeat = null
            isCountInActive = false
            recordingStartMs = System.currentTimeMillis()
            isRecording = true
        }
    }

    fun addImportedSong(song: Song) {
        songList.add(song)
        loadedSong = song
        isPlaying = false
        currentTimeMs = 0L
        activeKeys.clear()
        speedMultiplier = 1.0f
    }

    fun requestMidiFile() {
        val synchronousSong = onLoadMidiFile?.invoke()
        if (synchronousSong != null) addImportedSong(synchronousSong)
        else onRequestMidiFile?.invoke()
    }

    fun requestExport(song: Song) {
        exportSucceeded = if (onExportMidiFile != null) onExportMidiFile.invoke(song)
        else {
            onRequestExportMidiFile?.invoke(song)
            null
        }
    }

    fun installTheme(source: String) {
        ThemeJsonCodec.decode(source)
            .onSuccess { decoded ->
                val theme = decoded.copy(builtIn = false)
                val index = installedThemes.indexOfFirst { it.id == theme.id }
                when {
                    index < 0 -> installedThemes.add(theme)
                    !installedThemes[index].builtIn -> installedThemes[index] = theme
                    else -> {
                        userMessage = "No se puede reemplazar un tema integrado. Usa otro id."
                        return@onSuccess
                    }
                }
                selectedThemeId = theme.id
                userMessage = "Tema '${theme.name}' instalado."
            }
            .onFailure { userMessage = "Tema no válido: ${it.message ?: "error desconocido"}" }
    }

    LaunchedEffect(importedSong) {
        importedSong?.let {
            addImportedSong(it)
            onImportedSongConsumed?.invoke()
        }
    }

    LaunchedEffect(importedThemeJson) {
        importedThemeJson?.let(::installTheme)
        if (importedThemeJson != null) onImportedThemeConsumed?.invoke()
    }

    // Inicializar dispositivos, precargar canciones y listar salidas de audio
    LaunchedEffect(Unit) {
        refreshMidiDevices()
        refreshAudioDevices()
        
        val restoredSongs = restoredState?.songs.orEmpty()
        val systemDemos = mutableListOf<Song>()
        
        runCatching {
            val catalogBytes = Res.readBytes("files/catalog.json")
            val catalogText = catalogBytes.decodeToString()
            
            // Dividir por bloques de objetos de canciones (delimitados por la llave de apertura)
            val songBlocks = catalogText.split("{")
            for (block in songBlocks) {
                if (block.contains("\"title\"") && block.contains("\"file\"")) {
                    val title = block.substringAfter("\"title\"").substringAfter("\"").substringBefore("\"").trim()
                    val file = block.substringAfter("\"file\"").substringAfter("\"").substringBefore("\"").trim()
                    val difficultyStr = if (block.contains("\"difficulty\"")) {
                        block.substringAfter("\"difficulty\"").substringAfter("\"").substringBefore("\"").trim()
                    } else "Fácil"
                    
                    val diff = when {
                        difficultyStr.contains("fácil", ignoreCase = true) && difficultyStr.contains("intermedia", ignoreCase = true) -> Difficulty.INTERMEDIO
                        difficultyStr.contains("fácil", ignoreCase = true) -> Difficulty.FACIL
                        difficultyStr.contains("intermedia", ignoreCase = true) -> Difficulty.INTERMEDIO
                        difficultyStr.contains("avanzad", ignoreCase = true) -> Difficulty.AVANZADO
                        else -> Difficulty.FACIL
                    }
                    
                    runCatching {
                        val midiBytes = Res.readBytes("files/$file")
                        val decodedSong = if (onParseMidiBytes != null) {
                            onParseMidiBytes(midiBytes, title)
                        } else {
                            com.biglexj.elytesia.midi.StandardMidiCodec.decode(midiBytes, title)
                        }
                        // Some legacy demo files were shipped as valid but empty
                        // 26-byte MIDI containers. Keep their catalog entries, but
                        // replace them with the equivalent generated song so every
                        // visible library item can actually be played.
                        val playableSong = if (decodedSong.notes.isNotEmpty() && decodedSong.durationMs > 0L) {
                            decodedSong
                        } else {
                            when (file) {
                                "escala_do.mid" -> generateScaleSong()
                                "gymnopedie.mid" -> generateGymnopedieSong()
                                "bella_ciao.mid" -> generateBellaCiaoSong()
                                "bach_prelude.mid" -> generateDemoSong()
                                else -> error("El MIDI '$file' no contiene notas reproducibles")
                            }
                        }
                        systemDemos.add(
                            playableSong.copy(
                                name = title,
                                isDemo = true,
                                difficulty = diff
                            )
                        )
                    }.onFailure { err ->
                        val size = runCatching { Res.readBytes("files/$file").size }.getOrElse { -1 }
                        println("Error al cargar canción individual ($file), tamaño: $size bytes. Detalle: ${err.message}")
                        err.printStackTrace()
                    }
                }
            }
        }.onFailure {
            println("Catalog.json no cargado en absoluto: ${it.message}")
        }

        // 2. Si no hay demos cargadas desde recursos, usar las 4 demos integradas en código
        if (systemDemos.isEmpty()) {
            systemDemos.add(generateDemoSong())
            systemDemos.add(generateScaleSong())
            systemDemos.add(generateBellaCiaoSong())
            systemDemos.add(generateGymnopedieSong())
        }

        if (restoredSongs.isNotEmpty()) {
            val customSongs = restoredSongs.filter { restored ->
                fun normalizedSongName(value: String) = value
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
                val restoredName = normalizedSongName(restored.name)
                val matchesSystemDemo = systemDemos.any { system ->
                    val systemName = normalizedSongName(system.name)
                    restoredName == systemName ||
                        (restoredName.length >= 8 && systemName.startsWith(restoredName)) ||
                        (systemName.length >= 8 && restoredName.startsWith(systemName))
                }
                !restored.isDemo &&
                    restored.notes.isNotEmpty() &&
                    restored.durationMs > 0L &&
                    !matchesSystemDemo
            }
            songList.addAll(systemDemos)
            songList.addAll(customSongs)
            loadedSong = songList.firstOrNull { it.name == restoredState?.selectedSongName }
                ?: songList.first()
        } else {
            songList.addAll(systemDemos)
            loadedSong = systemDemos.firstOrNull() ?: generateDemoSong()
        }
        stateInitialized = true
    }

    LaunchedEffect(
        stateInitialized,
        minPitch,
        maxPitch,
        internalSoundEnabled,
        noteLabelMode,
        selectedAudioDevice,
        loadedSong?.name,
        selectedInstrument,
        selectedThemeId,
        useDynamicColor,
        installedThemes.toList(),
        songList.toList()
    ) {
        if (stateInitialized) {
            localStorage.write(
                AppStateCodec.encode(
                    SavedAppState(
                        minPitch = minPitch,
                        maxPitch = maxPitch,
                        internalSoundEnabled = internalSoundEnabled,
                        noteLabelMode = noteLabelMode.name,
                        selectedAudioDevice = selectedAudioDevice,
                        selectedSongName = loadedSong?.name,
                        selectedInstrument = selectedInstrument.name,
                        selectedThemeId = selectedThemeId,
                        useDynamicColor = useDynamicColor,
                        importedThemes = installedThemes.filterNot { it.builtIn }.map(ThemeJsonCodec::encode),
                        // System demos are bundled resources and must not be
                        // serialized as user imports. Older versions did so and
                        // recreated stale duplicates after an update.
                        songs = songList.filterNot { it.isDemo }
                    )
                )
            )
        }
    }

    // Vincular al teclado MIDI físico (Soporta mapeo interactivo de teclas al tocar)
    LaunchedEffect(selectedDevice, mappingMode, mappingStep) {
        if (selectedDevice.isNotEmpty()) {
            runCatching {
                midiDeviceManager.openDevice(
                selectedDevice,
                onNoteOn = { pitch, velocity ->
                    scope.launch {
                        if (mappingMode) {
                            if (mappingStep == 0) {
                                tempMinPitch = pitch
                                mappingStep = 1
                            } else {
                                val p1 = tempMinPitch
                                val p2 = pitch
                                minPitch = kotlin.math.min(p1, p2)
                                maxPitch = kotlin.math.max(p1, p2)
                                mappingMode = false
                                mappingStep = 0
                            }
                        } else {
                            if (isWaitingForMidiTrigger) {
                                isWaitingForMidiTrigger = false
                            }
                            evaluatePlayedPitch(pitch)
                            recordNoteOn(pitch, velocity)
                            if (pitch !in userActiveKeys) {
                                userActiveKeys.add(pitch)
                            }
                        }
                    }
                },
                onNoteOff = { pitch ->
                    scope.launch {
                        wrongUserKeys.remove(pitch)
                        userKeyTracks.remove(pitch)
                        recordNoteOff(pitch)
                        if (!mappingMode) {
                            userActiveKeys.remove(pitch)
                        }
                    }
                },
                onControlChange = { controller, value, channel ->
                    scope.launch {
                        recordControlChange(controller, value, channel)
                    }
                }
                )
            }.onFailure { error ->
                userMessage = "No se pudo abrir '$selectedDevice': ${error.message ?: "error MIDI desconocido"}"
                selectedDevice = ""
            }
        } else {
            midiDeviceManager.closeDevice()
        }
    }

    // Vincular la salida de audio seleccionada
    LaunchedEffect(selectedAudioDevice) {
        runCatching { midiDeviceManager.selectAudioOutput(selectedAudioDevice) }
            .onFailure { userMessage = "No se pudo seleccionar la salida de audio: ${it.message ?: "error desconocido"}" }
    }

    LaunchedEffect(selectedInstrument) {
        runCatching { midiDeviceManager.selectInstrument(selectedInstrument) }
            .onFailure { userMessage = "No se pudo cambiar el instrumento: ${it.message ?: "error desconocido"}" }
    }

    LaunchedEffect(internalSoundEnabled) {
        runCatching { midiDeviceManager.setInternalSoundEnabled(internalSoundEnabled) }
            .onFailure { userMessage = "No se pudo cambiar el sonido virtual: ${it.message ?: "error desconocido"}" }
    }

    // Reloj/Bucle de reproducción, metrónomo y avance
    LaunchedEffect(isPlaying, loadedSong, playbackRestartToken) {
        if (isPlaying && loadedSong != null) {
            val song = loadedSong!!
            isWaitingForMidiTrigger = selectedDevice.isNotEmpty()
            var lastTimeSystem = System.currentTimeMillis()
            val previousActiveNotes = mutableSetOf<NoteEvent>()
            val playedControls = mutableSetOf<ControlEvent>()
            
            val msPerBeat = 60000.0 / song.bpm
            var lastBeatPlayed = -1

            while (isPlaying) {
                val currentTimeSystem = System.currentTimeMillis()
                val delta = currentTimeSystem - lastTimeSystem
                lastTimeSystem = currentTimeSystem
                
                val activeNow = PlaybackLogic.activeNotesAt(song, currentTimeMs)
                
                val startingNotes = activeNow.filter { currentTimeMs >= it.startTimeMs && currentTimeMs < it.startTimeMs + 80 }
                val missingPitches = startingNotes.map { it.pitch }.filter { it !in userActiveKeys }
                
                val needsToWait = (waitMode && missingPitches.isNotEmpty()) || isWaitingForMidiTrigger
                
                if (!needsToWait) {
                    currentTimeMs += (delta * speedMultiplier).toLong()

                    song.controls.filter { it.timeMs <= currentTimeMs && it !in playedControls }.forEach { control ->
                        midiDeviceManager.playControlChange(control.controller, control.value)
                        playedControls.add(control)
                    }
                    
                    val currentBeatIndex = (currentTimeMs / msPerBeat).toInt()
                    if (currentBeatIndex > lastBeatPlayed) {
                        if (metronomeEnabled) {
                            midiDeviceManager.playMetronomeClick()
                        }
                        lastBeatPlayed = currentBeatIndex
                    }
                    
                    if (currentTimeMs >= song.durationMs) {
                        activeKeys.clear()
                        previousActiveNotes.forEach { midiDeviceManager.playNoteDirect(it.pitch, 0) }
                        previousActiveNotes.clear()
                        midiDeviceManager.stopAllNotes()

                        if (loopEnabled) {
                            currentTimeMs = song.durationMs
                            delay(5000L)
                            if (!isPlaying) break
                            currentTimeMs = 0L
                            playedControls.clear()
                            midiDeviceManager.playControlChange(64, 0)
                            lastBeatPlayed = -1
                            lastTimeSystem = System.currentTimeMillis()
                            continue
                        } else {
                            isPlaying = false
                            currentTimeMs = 0L
                            break
                        }
                    }
                    
                    val notesToStart = activeNow.filter { it !in previousActiveNotes }
                    notesToStart.forEach { note ->
                        midiDeviceManager.playNoteDirect(note.pitch, note.velocity)
                    }
                    
                    val notesToStop = previousActiveNotes.filter { it !in activeNow }
                    notesToStop.forEach { note ->
                        midiDeviceManager.playNoteDirect(note.pitch, 0)
                    }
                    
                    // Una misma tecla puede seguir sostenida por varias pistas.
                    // Para representarla elegimos la nota de inicio más reciente;
                    // en un unísono exacto conservamos la pista de menor índice.
                    val visibleActiveNotes = PlaybackLogic.visibleNotesByPitch(activeNow)

                    activeKeyTracks.clear()
                    visibleActiveNotes.forEach { (pitch, note) -> activeKeyTracks[pitch] = note.track }
                    // Publicar primero la pista evita que la UI pinte fugazmente
                    // la nota con el color predeterminado al activarse.
                    activeKeys.clear()
                    visibleActiveNotes.forEach { (pitch, note) -> activeKeys[pitch] = note.velocity }
                    
                    previousActiveNotes.clear()
                    previousActiveNotes.addAll(activeNow)
                } else {
                    lastTimeSystem = System.currentTimeMillis()
                }
                
                delay(12)
            }
        } else {
            isWaitingForMidiTrigger = false
            activeKeys.clear()
            activeKeyTracks.clear()
            midiDeviceManager.stopAllNotes()
        }
    }

    // Offset visual para compensar latencia de renderizado
    val visualTimeMs = currentTimeMs

    val activeTheme = installedThemes.firstOrNull { it.id == selectedThemeId } ?: ThemeDefaults.Aurora
    val platformColorScheme = rememberPlatformColorScheme(
        enabled = useDynamicColor,
        darkTheme = activeTheme.mode != ThemeMode.LIGHT
    )

    ElyTesiaTheme(theme = activeTheme, platformColorScheme = platformColorScheme) {
        // Alias locales: el shell histórico conserva nombres legibles, pero ya
        // obtiene todos los colores de los roles semánticos del tema activo.
        val DarkGrayBg = MaterialTheme.colorScheme.background
        val SurfaceGray = MaterialTheme.colorScheme.surface
        val SurfaceLight = MaterialTheme.colorScheme.surfaceVariant
        val BorderGray = MaterialTheme.colorScheme.outline
        val TextMain = MaterialTheme.colorScheme.onSurface
        val TextContrast = MaterialTheme.colorScheme.onSurfaceVariant
        val AuroraViolet = MaterialTheme.colorScheme.primary
        val OnAuroraViolet = MaterialTheme.colorScheme.onPrimary
        val ElyGreen = MaterialTheme.colorScheme.secondary
        val OnElyGreen = MaterialTheme.colorScheme.onSecondary
        val ElyPink = MaterialTheme.colorScheme.tertiary
        val OnElyPink = MaterialTheme.colorScheme.onTertiary
        val ElyCream = MaterialTheme.colorScheme.secondaryContainer
        val OnElyCream = MaterialTheme.colorScheme.onSecondaryContainer
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGrayBg)
        ) {
            val isCompactLayout = maxWidth < 700.dp || maxHeight < 600.dp
            if (isPlaying || showProgressWhenIdle) {
                PlaybackProgressBar(
                    currentTimeMs = currentTimeMs,
                    durationMs = loadedSong?.durationMs ?: 0L,
                    onPreviewSeek = { targetMs ->
                        currentTimeMs = targetMs
                        activeKeys.clear()
                        activeKeyTracks.clear()
                    },
                    onSeekCommitted = {
                        midiDeviceManager.stopAllNotes()
                        playbackRestartToken++
                    },
                    showBottomAccent = !simplifyPlaybackChrome,
                    modifier = Modifier
                        .zIndex(10f)
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }

            // 1. Piano Roll Canvas (Fondo de notas cayendo - Dinámico según rango de teclas)
            PianoRollCanvas(
                notes = loadedSong?.notes ?: emptyList(),
                currentTimeMs = visualTimeMs,
                activeKeys = activeKeys,
                activeTracks = activeKeyTracks,
                minPitch = visibleMinPitch,
                maxPitch = visibleMaxPitch,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 174.dp)
            )

            // 2. Panel superior flotante (Controles)
            if (!isPlaying) {
                if (isCompactLayout) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 182.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGray.copy(alpha = 0.94f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ely-Tesia", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AuroraViolet)
                                    Text(
                                        loadedSong?.let { "${it.name} · ${it.bpm.toInt()} BPM" } ?: "Ninguna canción",
                                        fontSize = 12.sp,
                                        color = TextContrast,
                                        maxLines = 2
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { activeSidebar = SidebarMode.BIBLIOTECA },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                        modifier = Modifier.height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) { Text("📁 Biblioteca", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { activeSidebar = SidebarMode.INSTRUMENTOS },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                        modifier = Modifier.height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) { Text("🎹 Instrumentos", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { activeSidebar = SidebarMode.TEMAS },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElyPink, contentColor = OnElyPink),
                                        modifier = Modifier.height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) { Text("🎨 Temas", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                }
                            }

                            HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (availableDevices.isEmpty()) {
                                            refreshMidiDevices()
                                        } else {
                                            val current = availableDevices.indexOf(selectedDevice)
                                            selectedDevice = availableDevices[(current + 1).mod(availableDevices.size)]
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkGrayBg),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(
                                        if (selectedDevice.isBlank()) "Sin teclado MIDI" else selectedDevice,
                                        color = if (selectedDevice.isBlank()) TextContrast else ElyGreen,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Button(
                                    onClick = { refreshMidiDevices() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Buscar MIDI", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { isPlaying = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) { Text("Reproducir", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = {
                                        isPlaying = false
                                        currentTimeMs = 0L
                                        activeKeys.clear()
                                        midiDeviceManager.stopAllNotes()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElyPink, contentColor = OnElyPink),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) { Text("Detener", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = {
                                        requestMidiFile()
                                    },
                                    enabled = onLoadMidiFile != null || onRequestMidiFile != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = OnElyGreen),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) { Text("Cargar MIDI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { waitMode = !waitMode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (waitMode) ElyGreen else SurfaceLight,
                                        contentColor = if (waitMode) OnElyGreen else TextContrast
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Espera: ${if (waitMode) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { internalSoundEnabled = !internalSoundEnabled },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (internalSoundEnabled) ElyGreen else SurfaceLight,
                                        contentColor = if (internalSoundEnabled) OnElyGreen else TextContrast
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Sonido: ${if (internalSoundEnabled) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { metronomeEnabled = !metronomeEnabled },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (metronomeEnabled) ElyGreen else SurfaceLight,
                                        contentColor = if (metronomeEnabled) OnElyGreen else TextContrast
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Metro: ${if (metronomeEnabled) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        currentTimeMs = 0L
                                        midiDeviceManager.stopAllNotes()
                                        playbackRestartToken++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) { Text("Reiniciar", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { loopEnabled = !loopEnabled },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (loopEnabled) ElyGreen else SurfaceLight,
                                        contentColor = if (loopEnabled) OnElyGreen else TextContrast
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) { Text("Bucle: ${if (loopEnabled) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { mappingMode = true; mappingStep = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Mapear", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Rango: ${maxPitch - minPitch + 1} teclas (${getNoteName(minPitch)} a ${getNoteName(maxPitch)})",
                                    color = TextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { noteLabelMode = noteLabelMode.next() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Notas: ${noteLabelMode.displayName}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { if (isRecording) finishRecording() else startRecording() },
                                    enabled = !isCountInActive,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) ElyPink else Color(0xFFDC2626)),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text(if (isRecording) "Detener toma" else "● Grabar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { countInBars = (countInBars + 1) % 3 },
                                    enabled = !isRecording && !isCountInActive,
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Cuenta: $countInBars", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }

                            if (isCountInActive || isRecording) {
                                Text(
                                    if (isCountInActive) "Empieza en ${countInBeat ?: "..."}" else "GRABANDO",
                                    color = ElyPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            lastRecording?.let { take ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { loadedSong = take; currentTimeMs = 0L; isPlaying = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { Text("Escuchar toma", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { requestExport(take) },
                                        enabled = onExportMidiFile != null || onRequestExportMidiFile != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = OnElyGreen),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { Text("Exportar MIDI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                } else {
                // PANEL DE CONTROL EXPANDIDO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 940.dp)
                            .border(1.dp, BorderGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceGray.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // FILA 1: Información y selectores alineados horizontalmente
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Ely-Tesia",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuroraViolet
                                    )
                                    Text(
                                        text = loadedSong?.let { "Canción: ${it.name} (${it.bpm.toInt()} BPM)" } ?: "Ninguna cargada",
                                        fontSize = 12.sp,
                                        color = TextContrast,
                                        maxLines = 1
                                    )
                                }
                                
                                // Entrada MIDI + Salida de Audio
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Selector MIDI
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Entrada MIDI", fontSize = 11.sp, color = TextContrast, fontWeight = FontWeight.Bold)
                                        Box(modifier = Modifier.width(160.dp)) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(38.dp)
                                                    .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkGrayBg)
                                                    .clickable { dropdownExpanded = true },
                                                color = Color.Transparent
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = selectedDevice.ifEmpty { "Sin Teclado MIDI" },
                                                        fontSize = 12.sp,
                                                        color = if (selectedDevice.isNotEmpty()) ElyGreen else TextContrast,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false },
                                                modifier = Modifier.width(160.dp).heightIn(max = 320.dp).background(SurfaceGray)
                                            ) {
                                                if (availableDevices.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("No hay teclados", color = TextContrast, fontSize = 12.sp) },
                                                        onClick = { dropdownExpanded = false },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                } else {
                                                    availableDevices.forEach { device ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                val isActive = device == selectedDevice
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    if (isActive) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(6.dp)
                                                                                .background(ElyGreen, shape = RoundedCornerShape(50))
                                                                        )
                                                                    }
                                                                    Text(
                                                                        text = device,
                                                                        modifier = Modifier.weight(1f),
                                                                        color = if (isActive) ElyGreen else TextMain,
                                                                        fontSize = 12.sp,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedDevice = device
                                                                dropdownExpanded = false
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = BorderGray)
                                                DropdownMenuItem(
                                                    text = { Text("Buscar de nuevo ↻", color = ElyCream, fontSize = 12.sp) },
                                                    onClick = { refreshMidiDevices() },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Selector Audio
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Salida Audio", fontSize = 11.sp, color = TextContrast, fontWeight = FontWeight.Bold)
                                        Box(modifier = Modifier.width(160.dp)) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(38.dp)
                                                    .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkGrayBg)
                                                    .clickable { audioDropdownExpanded = true },
                                                color = Color.Transparent
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = selectedAudioDevice.ifEmpty { "Sistema" },
                                                        fontSize = 12.sp,
                                                        color = ElyCream,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = audioDropdownExpanded,
                                                onDismissRequest = { audioDropdownExpanded = false },
                                                modifier = Modifier.width(160.dp).heightIn(max = 320.dp).background(SurfaceGray)
                                            ) {
                                                audioDevices.forEach { devName ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            val isActive = devName == selectedAudioDevice
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                if (isActive) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(6.dp)
                                                                            .background(ElyCream, shape = RoundedCornerShape(50))
                                                                    )
                                                                }
                                                                Text(
                                                                    text = devName,
                                                                    modifier = Modifier.weight(1f),
                                                                    color = if (isActive) ElyCream else TextMain,
                                                                    fontSize = 12.sp,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedAudioDevice = devName
                                                            audioDropdownExpanded = false
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                                HorizontalDivider(color = BorderGray)
                                                DropdownMenuItem(
                                                    text = { Text("Buscar de nuevo ↻", color = ElyCream, fontSize = 12.sp) },
                                                    onClick = { refreshAudioDevices() },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = BorderGray.copy(alpha = 0.3f))
                            
                            // FILA 2: Controles de Reproducción, Espera, Metrónomo y Tempo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Reproducir
                                    Button(
                                        onClick = { isPlaying = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    
                                    // Detener
                                    Button(
                                        onClick = {
                                            isPlaying = false
                                            currentTimeMs = 0L
                                            activeKeys.clear()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElyPink,
                                            contentColor = OnElyPink
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Detener", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Cargar MIDI
                                    Button(
                                        onClick = {
                                            requestMidiFile()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = OnElyGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Cargar MIDI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    
                                    // Espera
                                    Button(
                                        onClick = { waitMode = !waitMode },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (waitMode) ElyGreen else SurfaceLight,
                                            contentColor = if (waitMode) OnElyGreen else TextContrast
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = if (waitMode) "Espera: ON" else "Espera: OFF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                // Metrónomo y Tempo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = { internalSoundEnabled = !internalSoundEnabled },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (internalSoundEnabled) ElyGreen else SurfaceLight,
                                            contentColor = if (internalSoundEnabled) OnElyGreen else TextContrast
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            if (internalSoundEnabled) "🎛️ Sonido Virtual" else "🎹 Sonido Teclado",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Metrónomo
                                    Button(
                                        onClick = { metronomeEnabled = !metronomeEnabled },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (metronomeEnabled) ElyGreen else SurfaceLight,
                                            contentColor = if (metronomeEnabled) OnElyGreen else TextContrast
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = if (metronomeEnabled) "Metrónomo: ON" else "Metrónomo: OFF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Controles del multiplicador de Tempo
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(38.dp)
                                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkGrayBg)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { speedMultiplier = (speedMultiplier - 0.05f).coerceAtLeast(0.25f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        // Texto del BPM: Editable por Teclado
                                        val bpmValue = ((loadedSong?.bpm ?: 120.0) * speedMultiplier).toInt()
                                        if (isEditingBpm) {
                                            BasicTextField(
                                                value = bpmInputText,
                                                onValueChange = { newValue ->
                                                    if (newValue.length <= 3 && newValue.all { it.isDigit() }) {
                                                        bpmInputText = newValue
                                                    }
                                                },
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                keyboardActions = KeyboardActions(onDone = {
                                                    val typedBpm = bpmInputText.toDoubleOrNull()
                                                    if (typedBpm != null) {
                                                        speedMultiplier = (typedBpm / (loadedSong?.bpm ?: 120.0)).toFloat().coerceIn(0.25f, 2.0f)
                                                    }
                                                    isEditingBpm = false
                                                }),
                                                singleLine = true,
                                                cursorBrush = SolidColor(Color.White),
                                                modifier = Modifier
                                                    .width(55.dp)
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                                                            val typedBpm = bpmInputText.toDoubleOrNull()
                                                            if (typedBpm != null) {
                                                                speedMultiplier = (typedBpm / (loadedSong?.bpm ?: 120.0)).toFloat().coerceIn(0.25f, 2.0f)
                                                            }
                                                            isEditingBpm = false
                                                            true
                                                        } else if (keyEvent.key == Key.Escape) {
                                                            isEditingBpm = false
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }
                                            )
                                        } else {
                                            Text(
                                                text = "$bpmValue BPM",
                                                fontSize = 12.sp,
                                                color = TextMain,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        bpmInputText = bpmValue.toString()
                                                        isEditingBpm = true
                                                    }
                                                    .padding(horizontal = 8.dp)
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { speedMultiplier = (speedMultiplier + 0.05f).coerceAtMost(2.0f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    // Restablecer tempo
                                    if (kotlin.math.abs(speedMultiplier - 1.0f) > 0.001f) {
                                        Button(
                                            onClick = {
                                                speedMultiplier = 1.0f
                                                isEditingBpm = false
                                                bpmInputText = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElyCream, contentColor = OnElyCream),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Text("Original", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderGray.copy(alpha = 0.3f))

                            // FILA 3: Mapeo Interactivo de Teclas (Rango de Teclado Físico)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Rango Teclas:", fontSize = 12.sp, color = TextContrast, fontWeight = FontWeight.Bold)
                                    
                                    if (mappingMode) {
                                        val instr = if (mappingStep == 0) {
                                            "Toca la PRIMERA tecla (más a la izquierda) en tu teclado físico..."
                                        } else {
                                            "Toca la ÚLTIMA tecla (más a la derecha) en tu teclado físico..."
                                        }
                                        Text(
                                            text = instr,
                                            fontSize = 12.sp,
                                            color = ElyPink,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        val keysCount = maxPitch - minPitch + 1
                                        Text(
                                            text = "$keysCount Teclas (${getNoteName(minPitch)} a ${getNoteName(maxPitch)})",
                                            fontSize = 12.sp,
                                            color = TextMain,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (mappingMode) {
                                        Button(
                                            onClick = {
                                                mappingMode = false
                                                mappingStep = 0
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElyPink, contentColor = OnElyPink),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                currentTimeMs = 0L
                                                activeKeys.clear()
                                                midiDeviceManager.stopAllNotes()
                                                playbackRestartToken++
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp)
                                        ) {
                                            Text("Reiniciar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { loopEnabled = !loopEnabled },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (loopEnabled) ElyGreen else SurfaceLight,
                                                contentColor = if (loopEnabled) OnElyGreen else TextContrast
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp)
                                        ) {
                                            Text(
                                                if (loopEnabled) "Bucle: ON" else "Bucle: OFF",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Button(
                                            onClick = { noteLabelMode = noteLabelMode.next() },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                "Notas: ${noteLabelMode.displayName}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                mappingMode = true
                                                mappingStep = 0
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text("Mapear Rango 🎹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                    }
                                }
                            }

                            HorizontalDivider(color = BorderGray.copy(alpha = 0.3f))

                            // FILA 4: Grabación MIDI y gestión de la última toma
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (isRecording) finishRecording() else startRecording()
                                        },
                                        enabled = !isCountInActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) ElyPink else Color(0xFFDC2626),
                                            contentColor = OnElyPink
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            if (isRecording) "Detener toma" else "● Grabar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Button(
                                        onClick = { countInBars = (countInBars + 1) % 3 },
                                        enabled = !isRecording && !isCountInActive,
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("Cuenta: ${countInBars} comp.", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    when {
                                        isCountInActive -> Text(
                                            "Empieza en ${countInBeat ?: "..."}",
                                            color = ElyCream,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        isRecording -> Text(
                                            "GRABANDO",
                                            color = ElyPink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                lastRecording?.let { take ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BasicTextField(
                                            value = recordingName,
                                            onValueChange = { recordingName = it.take(60) },
                                            singleLine = true,
                                            textStyle = TextStyle(color = TextMain, fontSize = 12.sp),
                                            cursorBrush = SolidColor(ElyGreen),
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(DarkGrayBg)
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val newName = recordingName.trim()
                                                if (newName.isNotEmpty()) {
                                                    val renamed = take.copy(name = newName)
                                                    val index = songList.indexOf(take)
                                                    if (index >= 0) songList[index] = renamed
                                                    if (loadedSong == take) loadedSong = renamed
                                                    lastRecording = renamed
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextContrast),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("Renombrar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                loadedSong = lastRecording
                                                currentTimeMs = 0L
                                                isPlaying = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("Escuchar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { requestExport(take) },
                                            enabled = onExportMidiFile != null || onRequestExportMidiFile != null,
                                            colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = OnElyGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                if (exportSucceeded == true) "Exportado" else "Exportar MIDI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            } else {
                // PANEL DE CONTROL COLAPSADO (Al reproducir)
                Card(
                    modifier = Modifier
                        .padding(
                            top = when {
                                centerPlaybackControls -> 54.dp
                                simplifyPlaybackChrome -> 46.dp
                                else -> 24.dp
                            },
                            end = if (centerPlaybackControls) 0.dp else 24.dp
                        )
                        .align(if (centerPlaybackControls) Alignment.TopCenter else Alignment.TopEnd)
                        .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val songName = loadedSong?.name ?: "Demo"
                        Text(
                            text = if (isWaitingForMidiTrigger) "Esperando MIDI..." else if (songName.length > 18) songName.take(15) + "..." else songName,
                            fontSize = 13.sp,
                            color = if (isWaitingForMidiTrigger) ElyPink else TextContrast,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Pausa
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AuroraViolet)
                                .clickable { isPlaying = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⏸",
                                color = OnAuroraViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceLight)
                                .clickable {
                                    currentTimeMs = 0L
                                    activeKeys.clear()
                                    midiDeviceManager.stopAllNotes()
                                    playbackRestartToken++
                                }
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Reiniciar", color = TextContrast, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (loopEnabled) ElyGreen else SurfaceLight)
                                .clickable { loopEnabled = !loopEnabled }
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (loopEnabled) "Bucle ON" else "Bucle OFF",
                                color = if (loopEnabled) OnElyGreen else TextContrast,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Detener
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BorderGray)
                                .clickable {
                                    isPlaying = false
                                    currentTimeMs = 0L
                                    activeKeys.clear()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⏹",
                                color = TextMain,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Botones Flotantes de Biblioteca e Instrumentos (En Columna)
            if (!isPlaying && activeSidebar == null && !isCompactLayout) {
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp, start = 24.dp)
                        .wrapContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGray.copy(alpha = 0.9f))
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            .clickable { activeSidebar = SidebarMode.BIBLIOTECA }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📁", fontSize = 14.sp)
                            Text("Biblioteca", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGray.copy(alpha = 0.9f))
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            .clickable { activeSidebar = SidebarMode.INSTRUMENTOS }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎹", fontSize = 14.sp)
                            Text("Instrumentos", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { activeSidebar = SidebarMode.TEMAS }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("🎨  Temas", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Sidebar (Menú lateral izquierdo dinámico)
            if (activeSidebar != null && !isPlaying) {
                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .padding(bottom = 160.dp)
                        .border(
                            width = 1.dp, 
                            color = BorderGray, 
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        )
                        .align(Alignment.CenterStart),
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray.copy(alpha = 0.96f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (activeSidebar) {
                                    SidebarMode.BIBLIOTECA -> "Biblioteca MIDI (${songList.size})"
                                    SidebarMode.INSTRUMENTOS -> "Instrumentos"
                                    SidebarMode.TEMAS -> "Temas"
                                    null -> ""
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            IconButton(
                                onClick = { activeSidebar = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = TextContrast, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (activeSidebar == SidebarMode.BIBLIOTECA) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(songList.size) { index ->
                                    val song = songList[index]
                                    val isSelected = song == loadedSong
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) AuroraViolet.copy(alpha = 0.2f) else Color.Transparent)
                                            .border(
                                                width = 1.dp, 
                                                color = if (isSelected) AuroraViolet else BorderGray.copy(alpha = 0.5f), 
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    loadedSong = song
                                                    isPlaying = false
                                                    currentTimeMs = 0L
                                                    activeKeys.clear()
                                                    speedMultiplier = 1.0f
                                                    activeSidebar = null
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = song.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) TextMain else TextContrast,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "${song.bpm.toInt()} BPM",
                                                        fontSize = 10.sp,
                                                        color = TextContrast
                                                    )
                                                    Text(
                                                        text = "${song.durationMs / 1000}s",
                                                        fontSize = 10.sp,
                                                        color = TextContrast
                                                    )
                                                }
                                                // Banderita de Dificultad
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(song.difficulty.colorHex))
                                                    )
                                                    Text(
                                                        text = song.difficulty.displayName,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(song.difficulty.colorHex)
                                                    )
                                                }
                                            }
                                        }

                                        if (!song.isDemo) {
                                            IconButton(
                                                onClick = {
                                                    songList.remove(song)
                                                    if (loadedSong == song) {
                                                        loadedSong = songList.firstOrNull()
                                                        isPlaying = false
                                                        currentTimeMs = 0L
                                                        activeKeys.clear()
                                                    }
                                                },
                                                modifier = Modifier.padding(end = 8.dp).size(32.dp)
                                            ) {
                                                Text("🗑️", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (activeSidebar == SidebarMode.TEMAS) {
                            ThemeManagerPanel(
                                themes = installedThemes,
                                selectedThemeId = selectedThemeId,
                                useDynamicColor = useDynamicColor,
                                canImport = onRequestThemeFile != null,
                                canExport = onRequestExportTheme != null,
                                onSelect = { selectedThemeId = it },
                                onToggleDynamicColor = { useDynamicColor = it },
                                onImport = { onRequestThemeFile?.invoke() },
                                onExport = { theme ->
                                    onRequestExportTheme?.invoke("${theme.id.substringAfterLast('.')}.elytheme.json", ThemeJsonCodec.encode(theme))
                                },
                                onDelete = { theme ->
                                    installedThemes.remove(theme)
                                    if (selectedThemeId == theme.id) selectedThemeId = ThemeDefaults.Aurora.id
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            val instrumentsList = listOf(
                                Pair(InstrumentType.PIANO_ACUSTICO, "🎹 Piano Acústico"),
                                Pair(InstrumentType.PIANO_ELECTRICO, "🔔 Piano Eléctrico"),
                                Pair(InstrumentType.XILOFONO, "🪵 Xilófono"),
                                Pair(InstrumentType.SAXOFON, "🎷 Saxofón"),
                                Pair(InstrumentType.MELODICA, "🌬️ Melódica"),
                                Pair(InstrumentType.ORGANO, "⛪ Órgano de Iglesia"),
                                Pair(InstrumentType.SINTETIZADOR_PAD, "🌌 Colchón Pad"),
                                Pair(InstrumentType.CLAVECIN, "🎸 Clavecín"),
                                Pair(InstrumentType.FLAUTA, "🎵 Flauta Dulce"),
                                Pair(InstrumentType.BAJO_SINTETIZADO, "🔊 Bajo Sintetizado")
                            )

                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(instrumentsList.size) { index ->
                                    val (type, name) = instrumentsList[index]
                                    val isSelected = type == selectedInstrument
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) AuroraViolet.copy(alpha = 0.2f) else Color.Transparent)
                                            .border(
                                                width = 1.dp, 
                                                color = if (isSelected) AuroraViolet else BorderGray.copy(alpha = 0.5f), 
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                selectedInstrument = type
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) TextMain else TextContrast
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Teclado Virtual / Interactivo
            PianoKeyboard(
                songActiveKeys = activeKeys,
                songActiveTracks = activeKeyTracks,
                userActiveKeys = userActiveKeys.toSet(),
                userActiveTracks = userKeyTracks,
                wrongUserKeys = wrongUserKeys.toSet(),
                noteLabelMode = noteLabelMode,
                onKeyAction = { pitch, isPressed ->
                    if (isPressed) {
                        if (isWaitingForMidiTrigger) {
                            isWaitingForMidiTrigger = false
                        }
                        if (pitch !in userActiveKeys) {
                            userActiveKeys.add(pitch)
                        }
                        evaluatePlayedPitch(pitch)
                        recordNoteOn(pitch, 90)
                        midiDeviceManager.playNoteDirect(pitch, 90)
                    } else {
                        userActiveKeys.remove(pitch)
                        wrongUserKeys.remove(pitch)
                        userKeyTracks.remove(pitch)
                        recordNoteOff(pitch)
                        midiDeviceManager.playNoteDirect(pitch, 0)
                    }
                },
                minPitch = visibleMinPitch,
                maxPitch = visibleMaxPitch,
                onZoom = { scaleChange ->
                    keyboardZoomAccumulator *= scaleChange
                    val zoomDirection = when {
                        keyboardZoomAccumulator >= 1.12f -> -1
                        keyboardZoomAccumulator <= 0.89f -> 1
                        else -> 0
                    }
                    if (zoomDirection != 0) {
                        val mappedCount = maxPitch - minPitch + 1
                        val currentCount = visibleMaxPitch - visibleMinPitch + 1
                        val minimumCount = minOf(12, mappedCount)
                        val targetCount = (currentCount + zoomDirection * 12)
                            .coerceIn(minimumCount, mappedCount)
                        val centerPitch = (visibleMinPitch + visibleMaxPitch) / 2
                        var targetMin = centerPitch - (targetCount - 1) / 2
                        var targetMax = targetMin + targetCount - 1
                        if (targetMin < minPitch) {
                            targetMin = minPitch
                            targetMax = targetMin + targetCount - 1
                        }
                        if (targetMax > maxPitch) {
                            targetMax = maxPitch
                            targetMin = targetMax - targetCount + 1
                        }
                        visibleMinPitch = targetMin
                        visibleMaxPitch = targetMax
                        keyboardZoomAccumulator = 1f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .background(SurfaceGray)
                    .border(1.dp, BorderGray)
            )

            userMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 184.dp),
                    action = {
                        TextButton(onClick = { userMessage = null }) {
                            Text("Cerrar", color = ElyGreen)
                        }
                    },
                    containerColor = SurfaceGray,
                    contentColor = TextMain
                ) {
                    Text(message, fontSize = 12.sp)
                }
            }

            if (isWaitingForMidiTrigger) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 420.dp)
                            .border(1.dp, AuroraViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceGray.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎹",
                                fontSize = 36.sp
                            )
                            Text(
                                text = "Esperando entrada MIDI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Text(
                                text = "Presiona cualquier tecla de tu teclado MIDI para iniciar la reproducción de '${loadedSong?.name ?: "la canción"}'.",
                                fontSize = 13.sp,
                                color = TextContrast,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { isWaitingForMidiTrigger = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet, contentColor = OnAuroraViolet)
                                ) {
                                    Text("Iniciar ya", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { 
                                        isPlaying = false
                                        isWaitingForMidiTrigger = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextContrast),
                                    border = BorderStroke(1.dp, BorderGray)
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

