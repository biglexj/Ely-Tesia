package com.biglexj.elytesia.midi

interface MidiDeviceManager {
    fun getAvailableDevices(): List<String>
    fun openDevice(deviceName: String, onNoteOn: (pitch: Int, velocity: Int) -> Unit, onNoteOff: (pitch: Int) -> Unit, onControlChange: (controller: Int, value: Int, channel: Int) -> Unit)
    fun closeDevice()
    fun playNoteDirect(pitch: Int, velocity: Int) // Para reproducir sonido directamente
    fun playControlChange(controller: Int, value: Int)
    fun playMetronomeClick() // Metrónomo
    fun stopAllNotes()
    fun setInternalSoundEnabled(enabled: Boolean)
    
    // Dispositivos de salida de audio
    fun getAudioOutputs(): List<String>
    fun selectAudioOutput(name: String)
}

expect fun getPlatformMidiDeviceManager(): MidiDeviceManager
