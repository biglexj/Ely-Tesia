package com.biglexj.elytesia

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.midi.MidiDeviceManager
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.model.ControlEvent
import com.biglexj.elytesia.model.Song
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    midiDeviceManager: MidiDeviceManager = remember { getPlatformMidiDeviceManager() },
    onLoadMidiFile: (() -> Song?)? = null,
    onRequestMidiFile: (() -> Unit)? = null,
    importedSong: Song? = null,
    onImportedSongConsumed: (() -> Unit)? = null,
    onExportMidiFile: ((Song) -> Boolean)? = null,
    onRequestExportMidiFile: ((Song) -> Unit)? = null,
    localStorage: LocalStorage = NoOpLocalStorage
) {
    val restoredState = remember(localStorage) { AppStateCodec.decode(localStorage.read()) }
    var stateInitialized by remember { mutableStateOf(false) }
    var loadedSong by remember { mutableStateOf<Song?>(null) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
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
    
    var mappingMode by remember { mutableStateOf(false) }
    var mappingStep by remember { mutableStateOf(0) } // 0 = tecla inicio, 1 = tecla fin
    var tempMinPitch by remember { mutableStateOf(21) }

    // Biblioteca / Sidebar
    var sidebarOpen by remember { mutableStateOf(false) }
    val songList = remember { mutableStateListOf<Song>() }

    val activeKeys = remember { mutableStateMapOf<Int, Int>() } // Notas activas de la reproducción
    val userActiveKeys = remember { mutableStateListOf<Int>() }   // Notas físicas o por mouse

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

    fun recordNoteOn(pitch: Int, velocity: Int) {
        if (!isRecording || pitch in recordingActiveNotes) return
        recordingActiveNotes[pitch] =
            (System.currentTimeMillis() - recordingStartMs).coerceAtLeast(0L) to velocity
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

    LaunchedEffect(importedSong) {
        importedSong?.let {
            addImportedSong(it)
            onImportedSongConsumed?.invoke()
        }
    }

    // Inicializar dispositivos, precargar canciones y listar salidas de audio
    LaunchedEffect(Unit) {
        availableDevices = midiDeviceManager.getAvailableDevices()
        if (availableDevices.isNotEmpty()) {
            selectedDevice = availableDevices.first()
        }
        
        audioDevices = midiDeviceManager.getAudioOutputs()
        
        val restoredSongs = restoredState?.songs.orEmpty()
        if (restoredSongs.isNotEmpty()) {
            songList.addAll(restoredSongs)
            loadedSong = restoredSongs.firstOrNull { it.name == restoredState?.selectedSongName }
                ?: restoredSongs.first()
        } else {
            val bachDemo = generateDemoSong()
            val scaleDemo = generateScaleSong()
            songList.add(bachDemo)
            songList.add(scaleDemo)
            loadedSong = bachDemo
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
                        songs = songList.toList()
                    )
                )
            )
        }
    }

    // Vincular al teclado MIDI físico (Soporta mapeo interactivo de teclas al tocar)
    LaunchedEffect(selectedDevice, mappingMode, mappingStep) {
        if (selectedDevice.isNotEmpty()) {
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
                            recordNoteOn(pitch, velocity)
                            if (pitch !in userActiveKeys) {
                                userActiveKeys.add(pitch)
                            }
                        }
                    }
                },
                onNoteOff = { pitch ->
                    scope.launch {
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
        } else {
            midiDeviceManager.closeDevice()
        }
    }

    // Vincular la salida de audio seleccionada
    LaunchedEffect(selectedAudioDevice) {
        midiDeviceManager.selectAudioOutput(selectedAudioDevice)
    }

    LaunchedEffect(internalSoundEnabled) {
        midiDeviceManager.setInternalSoundEnabled(internalSoundEnabled)
    }

    // Reloj/Bucle de reproducción, metrónomo y avance
    LaunchedEffect(isPlaying, loadedSong, playbackRestartToken) {
        if (isPlaying && loadedSong != null) {
            val song = loadedSong!!
            var lastTimeSystem = System.currentTimeMillis()
            val previousActiveNotes = mutableSetOf<NoteEvent>()
            val playedControls = mutableSetOf<ControlEvent>()
            
            val msPerBeat = 60000.0 / song.bpm
            var lastBeatPlayed = -1

            while (isPlaying) {
                val currentTimeSystem = System.currentTimeMillis()
                val delta = currentTimeSystem - lastTimeSystem
                lastTimeSystem = currentTimeSystem
                
                val activeNow = song.notes.filter { 
                    currentTimeMs >= it.startTimeMs && currentTimeMs < it.startTimeMs + it.durationMs 
                }
                
                val startingNotes = activeNow.filter { currentTimeMs >= it.startTimeMs && currentTimeMs < it.startTimeMs + 80 }
                val missingPitches = startingNotes.map { it.pitch }.filter { it !in userActiveKeys }
                
                val needsToWait = waitMode && missingPitches.isNotEmpty()
                
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
                    
                    activeKeys.clear()
                    activeNow.forEach { activeKeys[it.pitch] = it.velocity }
                    
                    previousActiveNotes.clear()
                    previousActiveNotes.addAll(activeNow)
                } else {
                    lastTimeSystem = System.currentTimeMillis()
                }
                
                delay(12)
            }
        } else {
            activeKeys.clear()
            midiDeviceManager.stopAllNotes()
        }
    }

    // Offset visual para compensar latencia de renderizado
    val visualTimeMs = if (isPlaying) currentTimeMs + 120L else currentTimeMs

    ElyTesiaTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGrayBg)
        ) {
            val isCompactLayout = maxWidth < 700.dp
            val playbackProgress = if ((loadedSong?.durationMs ?: 0L) > 0L) {
                (currentTimeMs.toFloat() / loadedSong!!.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .background(BorderGray.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(playbackProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElyGreen, AuroraViolet, ElyPink)
                            )
                        )
                )
            }

            // 1. Piano Roll Canvas (Fondo de notas cayendo - Dinámico según rango de teclas)
            PianoRollCanvas(
                notes = loadedSong?.notes ?: emptyList(),
                currentTimeMs = visualTimeMs,
                activeKeys = activeKeys,
                minPitch = minPitch,
                maxPitch = maxPitch,
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
                                Button(
                                    onClick = { sidebarOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("📁 Biblioteca", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                                            availableDevices = midiDeviceManager.getAvailableDevices()
                                            selectedDevice = availableDevices.firstOrNull().orEmpty()
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
                                    onClick = {
                                        availableDevices = midiDeviceManager.getAvailableDevices()
                                        if (selectedDevice !in availableDevices) selectedDevice = availableDevices.firstOrNull().orEmpty()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Buscar MIDI", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { isPlaying = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = ElyPink),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) { Text("Detener", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = {
                                        requestMidiFile()
                                    },
                                    enabled = onLoadMidiFile != null || onRequestMidiFile != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) { Text("Cargar MIDI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { waitMode = !waitMode },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (waitMode) Color(0xFF38BDF8) else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Espera: ${if (waitMode) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { internalSoundEnabled = !internalSoundEnabled },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (internalSoundEnabled) ElyGreen else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text("Sonido: ${if (internalSoundEnabled) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { metronomeEnabled = !metronomeEnabled },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (metronomeEnabled) ElyGreen else Color(0xFF334155)),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) { Text("Reiniciar", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { loopEnabled = !loopEnabled },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (loopEnabled) ElyGreen else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) { Text("Bucle: ${if (loopEnabled) "ON" else "OFF"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { mappingMode = true; mappingStep = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
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
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { Text("Escuchar toma", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { requestExport(take) },
                                        enabled = onExportMidiFile != null || onRequestExportMidiFile != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = Color.Black),
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
                                                modifier = Modifier.width(160.dp).background(SurfaceGray)
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
                                                            text = { Text(device, color = TextMain, fontSize = 12.sp) },
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
                                                    onClick = {
                                                        availableDevices = midiDeviceManager.getAvailableDevices()
                                                    },
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
                                                modifier = Modifier.width(160.dp).background(SurfaceGray)
                                            ) {
                                                audioDevices.forEach { devName ->
                                                    DropdownMenuItem(
                                                        text = { Text(devName, color = TextMain, fontSize = 12.sp) },
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
                                                    onClick = {
                                                        audioDevices = midiDeviceManager.getAudioOutputs()
                                                    },
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
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Reproducir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                            contentColor = Color.White
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
                                        colors = ButtonDefaults.buttonColors(containerColor = ElyGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text("Cargar MIDI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    
                                    // Espera
                                    Button(
                                        onClick = { waitMode = !waitMode },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (waitMode) Color(0xFF38BDF8) else Color(0xFF334155),
                                            contentColor = if (waitMode) Color.Black else Color.White
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
                                            containerColor = if (internalSoundEnabled) ElyGreen else Color(0xFF334155),
                                            contentColor = if (internalSoundEnabled) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            if (internalSoundEnabled) "Sonido Ely: ON" else "Sonido teclado",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Metrónomo
                                    Button(
                                        onClick = { metronomeEnabled = !metronomeEnabled },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (metronomeEnabled) ElyGreen else Color(0xFF334155),
                                            contentColor = if (metronomeEnabled) Color.Black else Color.White
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = ElyCream),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = ElyPink, contentColor = Color.White),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp)
                                        ) {
                                            Text("Reiniciar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { loopEnabled = !loopEnabled },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (loopEnabled) ElyGreen else Color(0xFF334155),
                                                contentColor = if (loopEnabled) Color.Black else Color.White
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text("Mapear Rango 🎹", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                            contentColor = Color.White
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
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("Escuchar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { requestExport(take) },
                                            enabled = onExportMidiFile != null || onRequestExportMidiFile != null,
                                            colors = ButtonDefaults.buttonColors(containerColor = ElyGreen, contentColor = Color.Black),
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
                        .padding(top = 24.dp, end = 24.dp)
                        .align(Alignment.TopEnd)
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
                            text = if (songName.length > 18) songName.take(15) + "..." else songName,
                            fontSize = 13.sp,
                            color = TextContrast,
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
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF334155))
                                .clickable {
                                    currentTimeMs = 0L
                                    activeKeys.clear()
                                    midiDeviceManager.stopAllNotes()
                                    playbackRestartToken++
                                }
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Reiniciar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (loopEnabled) ElyGreen else Color(0xFF334155))
                                .clickable { loopEnabled = !loopEnabled }
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (loopEnabled) "Bucle ON" else "Bucle OFF",
                                color = if (loopEnabled) Color.Black else Color.White,
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

            // 3. Botón Flotante de Biblioteca (Sidebar)
            if (!isPlaying && !sidebarOpen && !isCompactLayout) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp, start = 24.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceGray.copy(alpha = 0.9f))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .clickable { sidebarOpen = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📁", fontSize = 14.sp)
                        Text("Biblioteca", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Sidebar (Menú lateral de canciones)
            if (sidebarOpen && !isPlaying) {
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
                                text = "Biblioteca MIDI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuroraViolet
                            )
                            IconButton(
                                onClick = { sidebarOpen = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = TextContrast, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(songList.size) { index ->
                                val song = songList[index]
                                val isSelected = song == loadedSong
                                
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
                                            loadedSong = song
                                            isPlaying = false
                                            currentTimeMs = 0L
                                            activeKeys.clear()
                                            speedMultiplier = 1.0f
                                            sidebarOpen = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
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
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
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
                userActiveKeys = userActiveKeys.toSet(),
                noteLabelMode = noteLabelMode,
                onKeyAction = { pitch, isPressed ->
                    if (isPressed) {
                        if (pitch !in userActiveKeys) {
                            userActiveKeys.add(pitch)
                        }
                        recordNoteOn(pitch, 90)
                        midiDeviceManager.playNoteDirect(pitch, 90)
                    } else {
                        userActiveKeys.remove(pitch)
                        recordNoteOff(pitch)
                        midiDeviceManager.playNoteDirect(pitch, 0)
                    }
                },
                minPitch = minPitch,
                maxPitch = maxPitch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .background(SurfaceGray)
                    .border(1.dp, BorderGray)
            )
        }
    }
}

fun getNoteName(pitch: Int): String {
    val notes = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    val octave = (pitch / 12) - 1
    val noteName = notes[pitch % 12]
    return "$noteName$octave"
}

fun generateDemoSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    val progressions = listOf(
        listOf(60, 64, 67, 72), // C Major
        listOf(57, 60, 64, 69), // A minor
        listOf(53, 57, 60, 65), // F Major
        listOf(55, 59, 62, 67)  // G Major
    )
    
    var timeMs = 1200L
    val duration = 400L
    val step = 200L

    repeat(2) {
        for (chord in progressions) {
            for ((idx, pitch) in chord.withIndex()) {
                notes.add(NoteEvent(pitch, timeMs + (idx * step), duration, 85, 1))
            }
            timeMs += 1000L
            
            for (pitch in chord) {
                notes.add(NoteEvent(pitch, timeMs, 800L, 95, 2))
            }
            timeMs += 1200L
        }
    }

    return Song("Bach Prelude C-Major (Demo)", timeMs + 1000L, notes, 120.0)
}

fun generateScaleSong(): Song {
    val notes = mutableListOf<NoteEvent>()
    val scaleNotes = listOf(60, 62, 64, 65, 67, 69, 71, 72, 72, 71, 69, 67, 65, 64, 62, 60)
    var timeMs = 800L
    val duration = 250L
    val step = 300L
    for (note in scaleNotes) {
        notes.add(NoteEvent(note, timeMs, duration, 90, 1))
        timeMs += step
    }
    return Song("Escala Do Mayor (Prueba)", timeMs + 400L, notes, 120.0)
}
