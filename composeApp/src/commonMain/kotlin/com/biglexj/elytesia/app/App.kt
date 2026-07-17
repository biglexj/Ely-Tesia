package com.biglexj.elytesia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.biglexj.elytesia.midi.MidiDeviceManager
import com.biglexj.elytesia.midi.getPlatformMidiDeviceManager
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.storage.LocalStorage
import com.biglexj.elytesia.storage.NoOpLocalStorage

/** Punto de composición compartido por Android y Desktop. */
@Composable
fun App(
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
) = ElyTesiaAppContent(
    midiDeviceManager = midiDeviceManager,
    onLoadMidiFile = onLoadMidiFile,
    onRequestMidiFile = onRequestMidiFile,
    importedSong = importedSong,
    onImportedSongConsumed = onImportedSongConsumed,
    onExportMidiFile = onExportMidiFile,
    onRequestExportMidiFile = onRequestExportMidiFile,
    localStorage = localStorage,
    onParseMidiBytes = onParseMidiBytes,
    onPracticeActivityChanged = onPracticeActivityChanged,
    onRequestThemeFile = onRequestThemeFile,
    importedThemeJson = importedThemeJson,
    onImportedThemeConsumed = onImportedThemeConsumed,
    onRequestExportTheme = onRequestExportTheme,
    showProgressWhenIdle = showProgressWhenIdle,
    simplifyPlaybackChrome = simplifyPlaybackChrome,
    centerPlaybackControls = centerPlaybackControls
)
