package com.biglexj.elytesia.midi

import javax.sound.midi.*
import javax.sound.sampled.*
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.concurrent.thread

// Sintetizador de Software Polyfónico de baja latencia para OpenJDK / JBR
class SimpleSoftwareSynth {
    private val SAMPLE_RATE = 44100f
    private var line: SourceDataLine? = null
    private val activeNotes = mutableMapOf<Int, ActiveNote>()
    private val sustainedNotes = mutableSetOf<Int>()
    private var sustainEnabled = false
    private var running = true
    private var synthThread: Thread? = null

    class ActiveNote(val pitch: Int, val frequency: Double, val velocity: Int) {
        var phase = 0.0
        var amplitude = 0.0
        var ageSamples = 0L
        var isReleased = false
        var releaseAmplitude = 0.0
        var releaseSamples = 0L

        fun updateEnvelope(samples: Int) {
            ageSamples += samples
            if (pitch == -1) {
                val sec = ageSamples.toDouble() / 44100.0
                amplitude = (1.0 - (sec / 0.05)).coerceAtLeast(0.0) * 0.08
            } else if (isReleased) {
                releaseSamples += samples
                val releaseSec = releaseSamples.toDouble() / 44100.0
                // Desvanecimiento rápido para evitar clicks/pops (0.12 segundos)
                val releaseFactor = (1.0 - (releaseSec / 0.12)).coerceAtLeast(0.0)
                amplitude = releaseAmplitude * releaseFactor
            } else {
                val sec = ageSamples.toDouble() / 44100.0
                // Decaimiento natural de piano (2.5 segundos)
                val attack = (sec / 0.012).coerceIn(0.0, 1.0)
                val decay = (1.0 - (sec / 2.5)).coerceAtLeast(0.0)
                amplitude = attack * decay * (velocity / 127.0) * 0.18
            }
        }
    }

    init {
        try {
            val format = AudioFormat(SAMPLE_RATE, 16, 1, true, true)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val l = AudioSystem.getLine(info) as SourceDataLine
            // Abrir con un buffer muy pequeño (512 bytes = ~2.9ms latencia) para tiempo real
            l.open(format, 1024)
            l.start()
            line = l

            synthThread = thread(start = true, isDaemon = true, name = "SoftwareSynthThread") {
                val buffer = ByteArray(512)
                while (running) {
                    generateSamples(buffer)
                    line?.write(buffer, 0, buffer.size)
                }
            }
        } catch (e: Exception) {
            println("Error al iniciar sintetizador de software: ${e.message}")
        }
    }

    fun noteOn(pitch: Int, velocity: Int) {
        val freq = 440.0 * Math.pow(2.0, (pitch - 69.0) / 12.0)
        synchronized(activeNotes) {
            sustainedNotes.remove(pitch)
            activeNotes[pitch] = ActiveNote(pitch, freq, velocity)
        }
    }

    fun noteOff(pitch: Int) {
        synchronized(activeNotes) {
            if (sustainEnabled) {
                sustainedNotes.add(pitch)
                return
            }
            releaseNote(pitch)
        }
    }

    private fun releaseNote(pitch: Int) {
            val note = activeNotes[pitch]
            if (note != null && !note.isReleased) {
                note.isReleased = true
                note.releaseAmplitude = note.amplitude
                note.releaseSamples = 0L
            }
    }

    fun setSustain(enabled: Boolean) {
        synchronized(activeNotes) {
            sustainEnabled = enabled
            if (!enabled) {
                sustainedNotes.toList().forEach(::releaseNote)
                sustainedNotes.clear()
            }
        }
    }

    fun playClick() {
        synchronized(activeNotes) {
            activeNotes[-1] = ActiveNote(-1, 1000.0, 90).apply {
                amplitude = 0.08
            }
        }
    }

    fun allNotesOff() {
        synchronized(activeNotes) {
            activeNotes.clear()
            sustainedNotes.clear()
        }
    }

    private fun generateSamples(buffer: ByteArray) {
        val numSamples = buffer.size / 2
        val mixedSamples = DoubleArray(numSamples)

        synchronized(activeNotes) {
            // Eliminar notas ya apagadas/silenciadas
            activeNotes.entries.removeIf { it.value.amplitude <= 0.001 }

            val polyphonyGain = 1.0 / sqrt(activeNotes.size.coerceAtLeast(1).toDouble())
            val nyquist = SAMPLE_RATE / 2.0

            for (i in 0 until numSamples) {
                var mixed = 0.0
                for (note in activeNotes.values) {
                    val angle = 2.0 * PI * note.frequency / SAMPLE_RATE
                    note.phase += angle
                    if (note.phase > 2.0 * PI) note.phase -= 2.0 * PI

                    // Solo generar armónicos por debajo de Nyquist. Los armónicos
                    // fuera de rango se repliegan como chirridos (aliasing).
                    var tone = sin(note.phase)
                    if (note.frequency * 2.0 < nyquist) tone += sin(note.phase * 2.0) * 0.25
                    if (note.frequency * 3.0 < nyquist) tone += sin(note.phase * 3.0) * 0.10
                    if (note.frequency * 4.0 < nyquist) tone += sin(note.phase * 4.0) * 0.04

                    mixed += tone * note.amplitude
                }
                // Normalizar la polifonía y limitar suavemente; el recorte duro era
                // otra fuente de distorsión al tocar acordes.
                mixedSamples[i] = tanh(mixed * polyphonyGain * 1.2) * 0.9
            }

            // Actualizar decaimiento de las envolventes
            for (note in activeNotes.values) {
                note.updateEnvelope(numSamples)
            }
        }

        // Conversión a PCM de 16 bits en Big Endian
        for (i in 0 until numSamples) {
            val sampleVal = (mixedSamples[i] * 32767.0).toInt()
            buffer[i * 2] = (sampleVal shr 8).toByte()
            buffer[i * 2 + 1] = (sampleVal and 0xFF).toByte()
        }
    }

    fun close() {
        running = false
        try {
            line?.drain()
            line?.close()
        } catch (e: Exception) {}
    }

    fun selectMixer(mixerName: String) {
        try {
            val format = AudioFormat(SAMPLE_RATE, 16, 1, true, true)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            
            val newLine = if (mixerName == "Sistema (Predeterminado)") {
                AudioSystem.getLine(info) as SourceDataLine
            } else {
                val mixerInfo = AudioSystem.getMixerInfo().firstOrNull { it.name == mixerName }
                if (mixerInfo != null) {
                    AudioSystem.getMixer(mixerInfo).getLine(info) as SourceDataLine
                } else {
                    AudioSystem.getLine(info) as SourceDataLine
                }
            }
            
            newLine.open(format, 1024)
            newLine.start()
            
            val oldLine = line
            line = newLine
            
            oldLine?.stop()
            oldLine?.close()
            println("Salida de audio cambiada a: $mixerName")
        } catch (e: Exception) {
            println("Error al cambiar salida de audio: ${e.message}")
        }
    }
}

class DesktopMidiDeviceManager : MidiDeviceManager {
    private var activeDevice: MidiDevice? = null
    private val softwareSynth = SimpleSoftwareSynth()
    @Volatile private var internalSoundEnabled = true

    override fun getAvailableDevices(): List<String> {
        val infos = MidiSystem.getMidiDeviceInfo()
        val devices = mutableListOf<String>()
        for (info in infos) {
            try {
                val dev = MidiSystem.getMidiDevice(info)
                val name = info.name
                // Filtrar secuenciadores y mapeadores internos de Java/Windows
                if (name.contains("Sequencer", ignoreCase = true) || 
                    name.contains("Mapper", ignoreCase = true) || 
                    name.contains("Synth", ignoreCase = true)) {
                    continue
                }
                if (dev.maxTransmitters != 0) {
                    devices.add(name)
                }
            } catch (e: Exception) {
                // Ignorar dispositivos no accesibles
            }
        }
        return devices
    }

    override fun openDevice(
        deviceName: String,
        onNoteOn: (pitch: Int, velocity: Int) -> Unit,
        onNoteOff: (pitch: Int) -> Unit,
        onControlChange: (controller: Int, value: Int, channel: Int) -> Unit
    ) {
        closeDevice()
        
        val infos = MidiSystem.getMidiDeviceInfo()
        val info = infos.firstOrNull { 
            it.name == deviceName && 
            try { MidiSystem.getMidiDevice(it).maxTransmitters != 0 } catch(e: Exception) { false }
        } ?: return
        
        try {
            val dev = MidiSystem.getMidiDevice(info)
            dev.open()
            activeDevice = dev
            
            val transmitter = dev.transmitter
            transmitter.receiver = object : Receiver {
                override fun send(message: MidiMessage, timeStamp: Long) {
                    if (message is ShortMessage) {
                        val pitch = message.data1
                        val velocity = message.data2
                        when (message.command) {
                            ShortMessage.NOTE_ON -> {
                                if (velocity > 0) {
                                    onNoteOn(pitch, velocity)
                                    playNoteDirect(pitch, velocity)
                                } else {
                                    onNoteOff(pitch)
                                    playNoteDirect(pitch, 0)
                                }
                            }
                            ShortMessage.NOTE_OFF -> {
                                onNoteOff(pitch)
                                playNoteDirect(pitch, 0)
                            }
                            ShortMessage.CONTROL_CHANGE -> {
                                onControlChange(message.data1, message.data2, message.channel)
                                playControlChange(message.data1, message.data2)
                            }
                        }
                    }
                }
                override fun close() {}
            }
            println("Teclado MIDI '$deviceName' conectado con éxito.")
        } catch (e: Exception) {
            println("Error al conectar teclado MIDI: ${e.message}")
        }
    }

    override fun closeDevice() {
        activeDevice?.close()
        activeDevice = null
    }

    override fun playNoteDirect(pitch: Int, velocity: Int) {
        if (!internalSoundEnabled) return
        if (velocity > 0) {
            softwareSynth.noteOn(pitch, velocity)
        } else {
            softwareSynth.noteOff(pitch)
        }
    }

    override fun playControlChange(controller: Int, value: Int) {
        if (controller == 64) softwareSynth.setSustain(value >= 64)
    }

    override fun playMetronomeClick() {
        if (internalSoundEnabled) softwareSynth.playClick()
    }

    override fun stopAllNotes() {
        softwareSynth.allNotesOff()
    }

    override fun setInternalSoundEnabled(enabled: Boolean) {
        internalSoundEnabled = enabled
        if (!enabled) softwareSynth.allNotesOff()
    }

    override fun getAudioOutputs(): List<String> {
        val mixers = AudioSystem.getMixerInfo()
        val outputs = mutableListOf<String>()
        outputs.add("Sistema (Predeterminado)")
        for (info in mixers) {
            try {
                val mixer = AudioSystem.getMixer(info)
                val lineInfo = Line.Info(SourceDataLine::class.java)
                if (mixer.isLineSupported(lineInfo) || mixer.sourceLineInfo.isNotEmpty()) {
                    val name = info.name
                    if (!name.contains("Port", ignoreCase = true) && name.isNotBlank()) {
                        outputs.add(name)
                    }
                }
            } catch (e: Exception) {}
        }
        return outputs.distinct()
    }

    override fun selectAudioOutput(name: String) {
        softwareSynth.selectMixer(name)
    }
}

actual fun getPlatformMidiDeviceManager(): MidiDeviceManager {
    return DesktopMidiDeviceManager()
}
