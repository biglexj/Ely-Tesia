package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.features.instrument.InstrumentSelectionPanel
import com.biglexj.elytesia.features.instrument.KeyboardConfigPanel
import com.biglexj.elytesia.features.keyboard.NoteLabelMode
import com.biglexj.elytesia.features.keyboard.PianoKeyboard
import com.biglexj.elytesia.features.keyboard.PianoRollCanvas
import com.biglexj.elytesia.features.library.DemoSongs
import com.biglexj.elytesia.features.library.LibraryPanel
import com.biglexj.elytesia.features.player.PlaybackControlBar
import com.biglexj.elytesia.features.player.PlaybackLogic
import com.biglexj.elytesia.features.theme.ThemeManagerPanel
import com.biglexj.elytesia.midi.InstrumentType
import com.biglexj.elytesia.midi.MidiDeviceManager
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.shared.components.ElyToast
import com.biglexj.elytesia.storage.AppStateCodec
import com.biglexj.elytesia.storage.LocalStorage
import com.biglexj.elytesia.storage.NoOpLocalStorage
import com.biglexj.elytesia.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val activeTheme = installedThemes.firstOrNull { it.id == selectedThemeId } ?: ThemeDefaults.Aurora
    val platformColorScheme = rememberPlatformColorScheme(enabled = useDynamicColor, darkTheme = activeTheme.mode == ThemeMode.DARK)

    var activeSidebar by remember { mutableStateOf<SidebarMode?>(SidebarMode.BIBLIOTECA) }
    val demoSongs = remember { DemoSongs.all }
    val customSongs = remember { mutableStateListOf<Song>() }
    val allSongs = remember(customSongs.size) { demoSongs + customSongs }

    var loadedSongName by rememberSaveable { mutableStateOf<String?>(demoSongs.firstOrNull()?.name) }
    var loadedSong by remember { mutableStateOf<Song?>(demoSongs.firstOrNull()) }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    val effectiveRequestMidi = onRequestMidiFile ?: if (onLoadMidiFile != null) {
        {
            val song = onLoadMidiFile()
            if (song != null) {
                if (customSongs.none { it.name == song.name }) {
                    customSongs.add(song)
                }
                loadedSong = song
                loadedSongName = song.name
                currentTimeMs = 0L
                isPlaying = false
                activeSidebar = null
            }
        }
    } else null

    LaunchedEffect(importedSong) {
        val s = importedSong
        if (s != null) {
            if (customSongs.none { it.name == s.name }) {
                customSongs.add(s)
            }
            loadedSong = s
            loadedSongName = s.name
            currentTimeMs = 0L
            isPlaying = false
            onImportedSongConsumed?.invoke()
        }
    }
    var waitMode by rememberSaveable { mutableStateOf(false) }
    var loopEnabled by rememberSaveable { mutableStateOf(false) }
    var speedMultiplier by rememberSaveable { mutableStateOf(1.0f) }
    var transposeSemitones by rememberSaveable { mutableStateOf(0) }
    var metronomeEnabled by rememberSaveable { mutableStateOf(false) }
    var internalSoundEnabled by rememberSaveable { mutableStateOf(restoredState?.internalSoundEnabled ?: true) }
    var noteLabelMode by rememberSaveable { mutableStateOf(NoteLabelMode.NONE) }

    var availableDevices by remember { mutableStateOf(emptyList<String>()) }
    var selectedDevice by remember { mutableStateOf("") }
    var availableAudioOutputs by remember { mutableStateOf(listOf("Sistema (Predeterminado)")) }
    var selectedAudioOutput by remember { mutableStateOf("Sistema (Predeterminado)") }
    var selectedInstrument by remember { mutableStateOf(InstrumentType.PIANO_ACUSTICO) }

    var minPitch by remember { mutableStateOf(21) }
    var maxPitch by remember { mutableStateOf(108) }
    var mappingMode by remember { mutableStateOf(false) }
    var mappingStep by remember { mutableStateOf(0) }

    var loopCountdown by remember { mutableStateOf<Int?>(null) }
    var isLoopWaitingForMidi by remember { mutableStateOf(false) }

    var correctNotesCount by remember { mutableStateOf(0) }
    var wrongNotesCount by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var lastEvaluatedPitch by remember { mutableStateOf<Int?>(null) }

    val activeKeys = remember { mutableStateMapOf<Int, Int>() }
    val userActiveKeys = remember { mutableStateListOf<Int>() }
    val wrongUserKeys = remember { mutableStateListOf<Int>() }

    // Sincronización automática de canción al rotar o cambiar orientación
    LaunchedEffect(loadedSongName) {
        if (loadedSongName != null && loadedSong?.name != loadedSongName) {
            val found = demoSongs.firstOrNull { it.name == loadedSongName }
            if (found != null) {
                loadedSong = found
            }
        }
    }

    // Canción efectiva adaptada con la transposición (desplazamiento de semitonos)
    val effectiveSong = remember(loadedSong, transposeSemitones) {
        if (loadedSong == null || transposeSemitones == 0) loadedSong
        else {
            loadedSong!!.copy(
                notes = loadedSong!!.notes.map { note ->
                    note.copy(pitch = (note.pitch + transposeSemitones).coerceIn(21, 108))
                }
            )
        }
    }

    // Inicialización del motor de audio y MIDI
    LaunchedEffect(Unit) {
        runCatching {
            availableDevices = midiDeviceManager.getAvailableDevices()
        }
        // Aplicar instrumento inicial al sintetizador interno
        midiDeviceManager.selectInstrument(selectedInstrument)
    }

    // Propagar cambio de instrumento al sintetizador en tiempo real
    LaunchedEffect(selectedInstrument) {
        midiDeviceManager.selectInstrument(selectedInstrument)
    }

    // Abrir y conectar el dispositivo MIDI seleccionado
    LaunchedEffect(selectedDevice) {
        if (selectedDevice.isNotBlank()) {
            midiDeviceManager.openDevice(
                deviceName = selectedDevice,
                onNoteOn = { _, velocity ->
                    if (velocity > 0) {
                        isLoopWaitingForMidi = false
                    }
                },
                onNoteOff = { _ -> },
                onControlChange = { _, _, _ -> }
            )
        }
    }


    // Playback Loop Clock
    LaunchedEffect(isPlaying, effectiveSong, speedMultiplier) {
        if (!isPlaying || effectiveSong == null) return@LaunchedEffect
        val song = effectiveSong

        var lastTime = withFrameNanos { it }
        var playingPitches = setOf<Int>()

        try {
            while (isPlaying) {
                val now = withFrameNanos { it }
                val deltaMs = ((now - lastTime) / 1_000_000f * speedMultiplier).toLong()
                lastTime = now

                if (!waitMode && !isLoopWaitingForMidi && loopCountdown == null) {
                    currentTimeMs += deltaMs
                    if (currentTimeMs >= song.durationMs) {
                        if (loopEnabled) {
                            currentTimeMs = 0L
                            playingPitches.forEach { midiDeviceManager.playNoteDirect(it, 0) }
                            playingPitches = emptySet()
                            activeKeys.clear()

                            // Conteo regresivo (3, 2, 1) antes de reiniciar bucle
                            loopCountdown = 3
                            delay(1000)
                            loopCountdown = 2
                            delay(1000)
                            loopCountdown = 1
                            delay(1000)
                            loopCountdown = null

                            // Si hay teclado MIDI conectado, entrar en modo espera hasta tocar primera tecla
                            if (selectedDevice.isNotBlank()) {
                                isLoopWaitingForMidi = true
                                while (isLoopWaitingForMidi && isPlaying && loopEnabled) {
                                    delay(50)
                                }
                            }

                            // Sincronizar el reloj de fotogramas justo cuando arranca la canción en el segundo 0
                            lastTime = withFrameNanos { it }
                            currentTimeMs = 0L
                        } else {
                            isPlaying = false
                            currentTimeMs = 0L
                            activeKeys.clear()
                            break
                        }
                    }
                }

                val currentActiveNotes = PlaybackLogic.activeNotesAt(song, currentTimeMs)
                val currentPitches = currentActiveNotes.map { it.pitch }.toSet()

                activeKeys.clear()
                currentActiveNotes.forEach { note ->
                    activeKeys[note.pitch] = note.track
                }

                if (internalSoundEnabled) {
                    val newPitches = currentPitches - playingPitches
                    for (pitch in newPitches) {
                        val note = currentActiveNotes.firstOrNull { it.pitch == pitch }
                        midiDeviceManager.playNoteDirect(pitch, note?.velocity ?: 90)
                    }

                    val stoppedPitches = playingPitches - currentPitches
                    for (pitch in stoppedPitches) {
                        midiDeviceManager.playNoteDirect(pitch, 0)
                    }
                }

                playingPitches = currentPitches
                delay(16)
            }
        } catch (e: Exception) {
            println("Aviso en bucle de reproducción: ${e.message}")
        } finally {
            midiDeviceManager.stopAllNotes()
        }
    }

    ElyTesiaTheme(
        theme = activeTheme,
        platformColorScheme = platformColorScheme,
        useDynamicColor = useDynamicColor
    ) {
        val colors = MaterialTheme.colorScheme

        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 600.dp
                val isCompactHeight = maxHeight < 480.dp

                Row(modifier = Modifier.fillMaxSize().background(colors.background)) {
                    // Navigation Sidebar + Panel: En Desktop (side-by-side)
                    if (!isCompact && activeSidebar != null) {
                        SidebarNavigation(
                            selectedMode = activeSidebar,
                            onModeSelected = { activeSidebar = it }
                        )
                        // Panel de contenido del sidebar seleccionado
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                                .background(colors.surface)
                        ) {
                            SidebarContentPanel(
                                activeSidebar = activeSidebar,
                                demoSongs = allSongs,
                                loadedSong = loadedSong,
                                selectedInstrument = selectedInstrument,
                                availableDevices = availableDevices,
                                selectedDevice = selectedDevice,
                                internalSoundEnabled = internalSoundEnabled,
                                installedThemes = installedThemes,
                                selectedThemeId = selectedThemeId,
                                useDynamicColor = useDynamicColor,
                                onRequestMidiFile = effectiveRequestMidi,
                                onRequestThemeFile = onRequestThemeFile,
                                onRequestExportTheme = onRequestExportTheme,
                                onSelectSong = { song ->
                                    loadedSong = song
                                    loadedSongName = song.name
                                    currentTimeMs = 0L
                                    isPlaying = false
                                    activeSidebar = null
                                },
                                onSelectInstrument = { selectedInstrument = it },
                                onSelectMidiDevice = { selectedDevice = it },
                                onRefreshMidiDevices = { availableDevices = midiDeviceManager.getAvailableDevices() },
                                onToggleInternalSound = { internalSoundEnabled = it },
                                onStartMappingMode = { mappingMode = true; mappingStep = 0 },
                                onSelectTheme = { selectedThemeId = it },
                                onToggleDynamicColor = { useDynamicColor = it }
                            )
                        }
                    }

                    // Center Content Workspace & Piano Canvas (Ocupa el 100% de la pantalla)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Header Control Bar
                        TopHeaderControlBar(
                            loadedSong = loadedSong,
                            currentTimeMs = currentTimeMs,
                            activeTheme = activeTheme,
                            isSidebarOpen = activeSidebar != null,
                            onToggleSidebar = {
                                activeSidebar = if (activeSidebar != null) null else SidebarMode.BIBLIOTECA
                            }
                        )

                        // Main Content Workspace
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Piano View Workspace
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                // Piano Roll Cascade Canvas con Overlay de Bucle
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    PianoRollCanvas(
                                        notes = effectiveSong?.notes.orEmpty(),
                                        currentTimeMs = currentTimeMs,
                                        activeKeys = activeKeys,
                                        minPitch = minPitch,
                                        maxPitch = maxPitch,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Overlay de Conteo e Indicación de Modo Bucle
                                    if (loopCountdown != null || isLoopWaitingForMidi) {
                                        val bannerText = when {
                                            loopCountdown != null -> "🔄 Reiniciando bucle en $loopCountdown..."
                                            isLoopWaitingForMidi -> "🎹 Modo Bucle: Toca la primera tecla MIDI para iniciar"
                                            else -> ""
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 12.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                                .background(colors.primaryContainer.copy(alpha = 0.92f))
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = bannerText,
                                                color = colors.onPrimaryContainer,
                                                fontSize = 12.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Practice Statistics Summary Bar (oculta en modo horizontal ultra compacto para dar espacio a la cascada)
                                if (!isCompactHeight) {
                                    PracticeStatsCard(
                                        correctNotesCount = correctNotesCount,
                                        wrongNotesCount = wrongNotesCount,
                                        currentStreak = currentStreak,
                                        lastEvaluatedPitch = lastEvaluatedPitch,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }

                                // Interactive Piano Keyboard
                                PianoKeyboard(
                                    songActiveKeys = activeKeys,
                                    userActiveKeys = userActiveKeys.toSet(),
                                    wrongUserKeys = wrongUserKeys.toSet(),
                                    onKeyAction = { pitch, isPressed ->
                                        if (isPressed) {
                                            isLoopWaitingForMidi = false
                                            userActiveKeys.add(pitch)
                                            lastEvaluatedPitch = pitch
                                            midiDeviceManager.playNoteDirect(pitch, 90)
                                            if (activeKeys.containsKey(pitch)) {
                                                correctNotesCount++
                                                currentStreak++
                                            } else {
                                                wrongNotesCount++
                                                currentStreak = 0
                                                wrongUserKeys.add(pitch)
                                            }
                                        } else {
                                            userActiveKeys.remove(pitch)
                                            wrongUserKeys.remove(pitch)
                                            midiDeviceManager.playNoteDirect(pitch, 0)
                                        }
                                    },
                                    minPitch = minPitch,
                                    maxPitch = maxPitch,
                                    noteLabelMode = noteLabelMode,
                                    modifier = Modifier.fillMaxWidth().height(if (isCompactHeight) 95.dp else 130.dp)
                                )
                            }
                        }

                        // Bottom Playback Control Bar
                        PlaybackControlBar(
                            isPlaying = isPlaying,
                            onPlayToggle = {
                                isPlaying = !isPlaying
                                if (isPlaying) {
                                    activeSidebar = null // Cierra todo el sidebar al reproducir
                                }
                            },
                            onStop = {
                                isPlaying = false
                                currentTimeMs = 0L
                                loopCountdown = null
                                isLoopWaitingForMidi = false
                                activeKeys.clear()
                                midiDeviceManager.stopAllNotes()
                            },
                            currentTimeMs = currentTimeMs,
                            durationMs = loadedSong?.durationMs ?: 0L,
                            onSeek = { currentTimeMs = it },
                            speedMultiplier = speedMultiplier,
                            onSpeedChange = { speedMultiplier = it },
                            baseBpm = loadedSong?.bpm ?: 120.0,
                            transposeSemitones = transposeSemitones,
                            onTransposeChange = { transposeSemitones = it },
                            metronomeEnabled = metronomeEnabled,
                            onMetronomeToggle = { metronomeEnabled = !metronomeEnabled },
                            waitMode = waitMode,
                            onWaitModeToggle = { waitMode = !waitMode },
                            loopEnabled = loopEnabled,
                            onLoopToggle = { loopEnabled = !loopEnabled },
                            noteLabelMode = noteLabelMode,
                            onCycleNoteLabelMode = { noteLabelMode = noteLabelMode.next() },
                            isCompactHeight = isCompactHeight
                        )
                    }
                }

                // Overlay Drawer flotante para pantallas móviles (isCompact == true)
                if (isCompact && activeSidebar != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { activeSidebar = null }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.92f)
                                .background(colors.surface)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Evitar cerrar al hacer clic dentro del panel */ }
                        ) {
                            SidebarNavigation(
                                selectedMode = activeSidebar,
                                onModeSelected = { activeSidebar = it }
                            )
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                SidebarContentPanel(
                                    activeSidebar = activeSidebar,
                                    demoSongs = allSongs,
                                    loadedSong = loadedSong,
                                    selectedInstrument = selectedInstrument,
                                    availableDevices = availableDevices,
                                    selectedDevice = selectedDevice,
                                    internalSoundEnabled = internalSoundEnabled,
                                    installedThemes = installedThemes,
                                    selectedThemeId = selectedThemeId,
                                    useDynamicColor = useDynamicColor,
                                    onRequestMidiFile = effectiveRequestMidi,
                                    onRequestThemeFile = onRequestThemeFile,
                                    onRequestExportTheme = onRequestExportTheme,
                                    onSelectSong = { song ->
                                        loadedSong = song
                                        loadedSongName = song.name
                                        currentTimeMs = 0L
                                        isPlaying = false
                                        activeSidebar = null
                                    },
                                    onSelectInstrument = { selectedInstrument = it },
                                    onSelectMidiDevice = { selectedDevice = it },
                                    onRefreshMidiDevices = { availableDevices = midiDeviceManager.getAvailableDevices() },
                                    onToggleInternalSound = { internalSoundEnabled = it },
                                    onStartMappingMode = { mappingMode = true; mappingStep = 0 },
                                    onSelectTheme = { selectedThemeId = it },
                                    onToggleDynamicColor = { useDynamicColor = it }
                                )
                            }
                        }
                    }
                }
            }

            // Modal Mapping Dialog Overlay
            if (mappingMode) {
                MidiMappingDialog(
                    mappingStep = mappingStep,
                    onCancel = { mappingMode = false },
                    onResetDefaults = {
                        minPitch = 21
                        maxPitch = 108
                        mappingMode = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun SidebarContentPanel(
    activeSidebar: SidebarMode?,
    demoSongs: List<Song>,
    loadedSong: Song?,
    selectedInstrument: InstrumentType,
    availableDevices: List<String>,
    selectedDevice: String,
    internalSoundEnabled: Boolean,
    installedThemes: List<ElyThemeDefinition>,
    selectedThemeId: String,
    useDynamicColor: Boolean,
    onRequestMidiFile: (() -> Unit)?,
    onRequestThemeFile: (() -> Unit)?,
    onRequestExportTheme: ((String, String) -> Unit)?,
    onSelectSong: (Song) -> Unit,
    onSelectInstrument: (InstrumentType) -> Unit,
    onSelectMidiDevice: (String) -> Unit,
    onRefreshMidiDevices: () -> Unit,
    onToggleInternalSound: (Boolean) -> Unit,
    onStartMappingMode: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    when (activeSidebar) {
        SidebarMode.BIBLIOTECA -> LibraryPanel(
            songsList = demoSongs,
            selectedSong = loadedSong,
            onSelectSong = onSelectSong,
            onRequestMidiFile = onRequestMidiFile
        )
        SidebarMode.INSTRUMENTOS -> InstrumentSelectionPanel(
            selectedInstrument = selectedInstrument,
            onSelectInstrument = onSelectInstrument
        )
        SidebarMode.CONFIGURACION -> KeyboardConfigPanel(
            availableMidiDevices = availableDevices,
            selectedMidiDevice = selectedDevice,
            onSelectMidiDevice = onSelectMidiDevice,
            onRefreshMidiDevices = onRefreshMidiDevices,
            internalSoundEnabled = internalSoundEnabled,
            onToggleInternalSound = onToggleInternalSound,
            onStartMappingMode = onStartMappingMode
        )
        SidebarMode.TEMAS -> ThemeManagerPanel(
            themes = installedThemes,
            selectedThemeId = selectedThemeId,
            useDynamicColor = useDynamicColor,
            canImport = onRequestThemeFile != null,
            canExport = onRequestExportTheme != null,
            onSelect = onSelectTheme,
            onToggleDynamicColor = onToggleDynamicColor,
            onImport = { onRequestThemeFile?.invoke() },
            onExport = { themeDef -> onRequestExportTheme?.invoke(themeDef.id, "") },
            onDelete = { themeDef -> (installedThemes as? MutableList<ElyThemeDefinition>)?.remove(themeDef) }
        )
        null -> {}
    }
}
