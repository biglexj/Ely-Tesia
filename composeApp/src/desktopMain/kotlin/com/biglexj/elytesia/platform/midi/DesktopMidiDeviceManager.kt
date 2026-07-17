package com.biglexj.elytesia.midi

import javax.sound.midi.*
import javax.sound.sampled.*
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.math.exp
import kotlin.concurrent.thread

// Sintetizador de Software Polifónico de baja latencia para OpenJDK / JBR con 10 Instrumentos
class SimpleSoftwareSynth {
    private val SAMPLE_RATE = 44100f
    @Volatile private var line: SourceDataLine? = null
    private val activeNotes = mutableMapOf<Int, ActiveNote>()
    private val sustainedNotes = mutableSetOf<Int>()
    private var sustainEnabled = false
    private var running = true
    private var synthThread: Thread? = null
    private var selectedMixerName = "Sistema (Predeterminado)"

    class ActiveNote(val pitch: Int, val frequency: Double, val velocity: Int, val instrument: InstrumentType) {
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
                amplitude = (1.0 - (sec / 0.05)).coerceAtLeast(0.0) * 0.18
                return
            }
            
            val sec = ageSamples.toDouble() / 44100.0
            
            if (isReleased) {
                releaseSamples += samples
                val releaseSec = releaseSamples.toDouble() / 44100.0
                
                val releaseTime = when (instrument) {
                    InstrumentType.SINTETIZADOR_PAD -> 0.45 // Colchón de liberación lenta
                    InstrumentType.SAXOFON, InstrumentType.MELODICA, InstrumentType.ORGANO, InstrumentType.FLAUTA -> 0.07 // Parada rápida de aire
                    else -> 0.12 // Decaimiento estándar de cuerda/barra
                }
                val releaseFactor = (1.0 - (releaseSec / releaseTime)).coerceAtLeast(0.0)
                amplitude = releaseAmplitude * releaseFactor
            } else {
                when (instrument) {
                    InstrumentType.PIANO_ACUSTICO -> {
                        val attack = (sec / 0.008).coerceIn(0.0, 1.0)
                        val decay = exp(-0.8 * sec)
                        amplitude = attack * decay * (velocity / 127.0) * 0.38
                    }
                    InstrumentType.PIANO_ELECTRICO -> {
                        val attack = (sec / 0.005).coerceIn(0.0, 1.0)
                        val decay = exp(-0.9 * sec)
                        amplitude = attack * decay * (velocity / 127.0) * 0.42
                    }
                    InstrumentType.XILOFONO -> {
                        val attack = (sec / 0.001).coerceIn(0.0, 1.0) // Ataque percusivo instantáneo
                        val decay = exp(-4.5 * sec) // Decaimiento súper rápido de bloque de madera
                        amplitude = attack * decay * (velocity / 127.0) * 0.45
                    }
                    InstrumentType.SAXOFON -> {
                        val attack = (sec / 0.045).coerceIn(0.0, 1.0) // Ataque de aire lento
                        val sustain = 1.0
                        amplitude = attack * sustain * (velocity / 127.0) * 0.26
                    }
                    InstrumentType.MELODICA -> {
                        val attack = (sec / 0.015).coerceIn(0.0, 1.0)
                        val sustain = 1.0
                        amplitude = attack * sustain * (velocity / 127.0) * 0.28
                    }
                    InstrumentType.ORGANO -> {
                        val attack = (sec / 0.015).coerceIn(0.0, 1.0)
                        val sustain = 1.0
                        amplitude = attack * sustain * (velocity / 127.0) * 0.22
                    }
                    InstrumentType.SINTETIZADOR_PAD -> {
                        val attack = (sec / 0.25).coerceIn(0.0, 1.0) // Ataque lento
                        val sustain = 1.0
                        amplitude = attack * sustain * (velocity / 127.0) * 0.35
                    }
                    InstrumentType.CLAVECIN -> {
                        val attack = (sec / 0.002).coerceIn(0.0, 1.0)
                        val decay = exp(-2.2 * sec) // Cuerda pulsada rápida
                        amplitude = attack * decay * (velocity / 127.0) * 0.36
                    }
                    InstrumentType.FLAUTA -> {
                        val attack = (sec / 0.04).coerceIn(0.0, 1.0)
                        val sustain = 1.0
                        amplitude = attack * sustain * (velocity / 127.0) * 0.35
                    }
                    InstrumentType.BAJO_SINTETIZADO -> {
                        val attack = (sec / 0.01).coerceIn(0.0, 1.0)
                        val decay = exp(-0.6 * sec)
                        amplitude = attack * decay * (velocity / 127.0) * 0.45
                    }
                }
            }
        }
    }

    init {
        try {
            val format = AudioFormat(SAMPLE_RATE, 16, 2, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val l = AudioSystem.getLine(info) as SourceDataLine
            l.open(format, 4096)
            l.start()
            line = l

            synthThread = thread(start = true, isDaemon = true, name = "SoftwareSynthThread") {
                val buffer = ByteArray(1024)
                while (running) {
                    try {
                        generateSamples(buffer)
                        val currentLine = line
                        if (currentLine != null && currentLine.isOpen) {
                            currentLine.write(buffer, 0, buffer.size)
                        } else {
                            Thread.sleep(10)
                        }
                    } catch (e: Exception) {
                        try { Thread.sleep(20) } catch (te: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            println("Error al iniciar sintetizador de software: ${e.message}")
        }
    }

    fun noteOn(pitch: Int, velocity: Int, instrument: InstrumentType) {
        val freq = 440.0 * Math.pow(2.0, (pitch - 69.0) / 12.0)
        synchronized(activeNotes) {
            sustainedNotes.remove(pitch)
            activeNotes[pitch] = ActiveNote(pitch, freq, velocity, instrument)
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
            activeNotes[-1] = ActiveNote(-1, 1000.0, 90, InstrumentType.PIANO_ACUSTICO).apply {
                amplitude = 0.18
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
        val numSamples = buffer.size / 4
        val mixedSamples = DoubleArray(numSamples)

        synchronized(activeNotes) {
            // Eliminar notas apagadas/silenciadas que ya hayan sido procesadas al menos una vez
            activeNotes.entries.removeIf { it.value.ageSamples > 0 && it.value.amplitude <= 0.001 }

            val polyphonyGain = 1.0 / sqrt(activeNotes.size.coerceAtLeast(1).toDouble())
            val nyquist = SAMPLE_RATE / 2.0

            for (i in 0 until numSamples) {
                var mixed = 0.0
                for (note in activeNotes.values) {
                    val angle = 2.0 * PI * note.frequency / SAMPLE_RATE
                    note.phase += angle
                    if (note.phase > 2.0 * PI) note.phase -= 2.0 * PI

                    val t = note.ageSamples.toDouble() / SAMPLE_RATE + (i.toDouble() / SAMPLE_RATE)
                    var tone = 0.0

                    when (note.instrument) {
                        InstrumentType.PIANO_ACUSTICO -> {
                            var s = sin(note.phase)
                            val h2Decay = exp(-4.0 * t)
                            val h3Decay = exp(-8.0 * t)
                            val h4Decay = exp(-12.0 * t)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0) * 0.35 * h2Decay
                            if (note.frequency * 3.0 < nyquist) s += sin(note.phase * 3.0) * 0.15 * h3Decay
                            if (note.frequency * 4.0 < nyquist) s += sin(note.phase * 4.0) * 0.08 * h4Decay
                            tone = s
                        }
                        InstrumentType.PIANO_ELECTRICO -> {
                            var s = sin(note.phase)
                            val h2Decay = exp(-1.5 * t)
                            val h3Decay = exp(-3.0 * t)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0) * 0.6 * h2Decay
                            if (note.frequency * 3.0 < nyquist) s += sin(note.phase * 3.0) * 0.35 * h3Decay
                            val tremolo = 1.0 + 0.18 * sin(2.0 * PI * 5.0 * t)
                            tone = s * tremolo
                        }
                        InstrumentType.XILOFONO -> {
                            var s = sin(note.phase)
                            val f2 = note.frequency * 3.0
                            val f3 = note.frequency * 6.0
                            val h2Decay = exp(-25.0 * t)
                            val h3Decay = exp(-45.0 * t)
                            if (f2 < nyquist) s += sin(note.phase * 3.0) * 0.5 * h2Decay
                            if (f3 < nyquist) s += sin(note.phase * 6.0) * 0.25 * h3Decay
                            tone = s
                        }
                        InstrumentType.SAXOFON -> {
                            val vibrato = 1.0 + 0.007 * sin(2.0 * PI * 6.0 * t)
                            val modulatedPhase = note.phase * vibrato
                            var s = sin(modulatedPhase)
                            if (note.frequency * 2.0 < nyquist) s += sin(modulatedPhase * 2.0) * 0.4
                            if (note.frequency * 3.0 < nyquist) s += sin(modulatedPhase * 3.0) * 0.5
                            if (note.frequency * 4.0 < nyquist) s += sin(modulatedPhase * 4.0) * 0.2
                            if (note.frequency * 5.0 < nyquist) s += sin(modulatedPhase * 5.0) * 0.3
                            val breathNoise = (kotlin.random.Random.nextFloat() * 2.0 - 1.0) * 0.08 * exp(-10.0 * t)
                            tone = s + breathNoise
                        }
                        InstrumentType.MELODICA -> {
                            var s = sin(note.phase)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0) * 0.3
                            if (note.frequency * 3.0 < nyquist) s += sin(note.phase * 3.0) * 0.6
                            if (note.frequency * 5.0 < nyquist) s += sin(note.phase * 5.0) * 0.4
                            if (note.frequency * 7.0 < nyquist) s += sin(note.phase * 7.0) * 0.2
                            tone = s * 0.8
                        }
                        InstrumentType.ORGANO -> {
                            var s = sin(note.phase)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0) * 0.5
                            if (note.frequency * 3.0 < nyquist) s += sin(note.phase * 3.0) * 0.3
                            if (note.frequency * 4.0 < nyquist) s += sin(note.phase * 4.0) * 0.2
                            if (note.frequency * 5.0 < nyquist) s += sin(note.phase * 5.0) * 0.1
                            tone = s
                        }
                        InstrumentType.SINTETIZADOR_PAD -> {
                            val s1 = sin(note.phase * 0.9985)
                            val s2 = sin(note.phase * 1.0015)
                            tone = (s1 + s2) * 0.5
                        }
                        InstrumentType.CLAVECIN -> {
                            var s = sin(note.phase)
                            val h2Decay = exp(-3.0 * t)
                            val h3Decay = exp(-5.0 * t)
                            val h4Decay = exp(-7.0 * t)
                            val h5Decay = exp(-9.0 * t)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0) * 0.7 * h2Decay
                            if (note.frequency * 3.0 < nyquist) s += sin(note.phase * 3.0) * 0.5 * h3Decay
                            if (note.frequency * 4.0 < nyquist) s += sin(note.phase * 4.0) * 0.4 * h4Decay
                            if (note.frequency * 5.0 < nyquist) s += sin(note.phase * 5.0) * 0.3 * h5Decay
                            tone = s
                        }
                        InstrumentType.FLAUTA -> {
                            val vibrato = 1.0 + 0.005 * sin(2.0 * PI * 4.5 * t)
                            var s = sin(note.phase * vibrato)
                            if (note.frequency * 2.0 < nyquist) s += sin(note.phase * 2.0 * vibrato) * 0.1
                            val breath = (kotlin.random.Random.nextFloat() * 2.0 - 1.0) * 0.04
                            tone = s + breath
                        }
                        InstrumentType.BAJO_SINTETIZADO -> {
                            var s = sin(note.phase)
                            for (harm in 2..6) {
                                val fH = note.frequency * harm
                                if (fH < nyquist) {
                                    s += sin(note.phase * harm) * (1.0 / harm)
                                }
                            }
                            tone = s * 0.9
                        }
                    }

                    mixed += tone * note.amplitude
                }
                mixedSamples[i] = tanh(mixed * polyphonyGain * 1.2) * 0.9
            }

            // Actualizar decaimiento de las envolventes
            for (note in activeNotes.values) {
                note.updateEnvelope(numSamples)
            }
        }

        // Conversión a PCM de 16 bits en Little Endian (Estéreo)
        for (i in 0 until numSamples) {
            val sampleVal = (mixedSamples[i] * 32767.0).toInt().coerceIn(-32768, 32767)
            val low = (sampleVal and 0xFF).toByte()
            val high = ((sampleVal shr 8) and 0xFF).toByte()
            
            buffer[i * 4] = low
            buffer[i * 4 + 1] = high
            buffer[i * 4 + 2] = low
            buffer[i * 4 + 3] = high
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
        if (mixerName == selectedMixerName && line?.isOpen == true) return
        try {
            val format = AudioFormat(SAMPLE_RATE, 16, 2, true, false)
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
            
            newLine.open(format, 4096)
            newLine.start()
            
            val oldLine = line
            line = newLine
            
            oldLine?.stop()
            oldLine?.close()
            selectedMixerName = mixerName
            println("Salida de audio cambiada a: $mixerName")
        } catch (e: Exception) {
            println("Error al cambiar salida de audio: ${e.message}")
        }
    }
}

class DesktopMidiDeviceManager : MidiDeviceManager {
    private var activeDevice: MidiDevice? = null
    // Dispositivo de salida del teclado físico, usado para alternar Local Control
    private var activeOutputDevice: MidiDevice? = null
    private val softwareSynth = SimpleSoftwareSynth()
    @Volatile private var internalSoundEnabled = true
    private var currentInstrument = InstrumentType.PIANO_ACUSTICO

    override fun getAvailableDevices(): List<String> {
        val infos = MidiSystem.getMidiDeviceInfo()
        val devices = mutableListOf<String>()
        for (info in infos) {
            try {
                val dev = MidiSystem.getMidiDevice(info)
                val name = info.name
                if (name.contains("Sequencer", ignoreCase = true) || 
                    name.contains("Mapper", ignoreCase = true) || 
                    name.contains("Synth", ignoreCase = true)) {
                    continue
                }
                if (dev.maxTransmitters != 0) {
                    devices.add(name)
                }
            } catch (e: Exception) {}
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

            // Buscar el puerto de salida del teclado para gestionar Local Control.
            // Local Control OFF (CC 122, valor 0): silencia el teclado físico → solo suena el sintetizador interno.
            // Local Control ON  (CC 122, valor 127): restaura el audio del teclado → úsalo cuando el sintetizador esté apagado.
            try {
                val outInfo = infos.firstOrNull { 
                    it.name == deviceName && 
                    try { MidiSystem.getMidiDevice(it).maxReceivers != 0 } catch(e: Exception) { false }
                }
                if (outInfo != null) {
                    val outDev = MidiSystem.getMidiDevice(outInfo)
                    outDev.open()
                    activeOutputDevice = outDev
                    // Si el sonido interno está activo, silenciamos el hardware; si no, lo dejamos sonar.
                    val localControlValue = if (internalSoundEnabled) 0 else 127
                    val msg = ShortMessage()
                    msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 122, localControlValue)
                    outDev.receiver.send(msg, -1)
                    println("Local Control del teclado físico configurado a: ${if (localControlValue == 0) "OFF (solo sintetizador)" else "ON (hardware activo)"}")
                }
            } catch (e: Exception) {
                println("Aviso: No se pudo configurar el Local Control del teclado físico (puede no soportarlo).")
            }

        } catch (e: Exception) {
            println("Error al conectar teclado MIDI: ${e.message}")
        }
    }

    override fun closeDevice() {
        // Restaurar Local Control ON al desconectar para no dejar el teclado mudo
        try {
            activeOutputDevice?.let { outDev ->
                if (outDev.isOpen) {
                    val msg = ShortMessage()
                    msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 122, 127)
                    outDev.receiver.send(msg, -1)
                    outDev.close()
                    println("Local Control restaurado a ON al desconectar el teclado.")
                }
            }
        } catch (e: Exception) {}
        activeOutputDevice = null
        activeDevice?.close()
        activeDevice = null
    }

    override fun playNoteDirect(pitch: Int, velocity: Int) {
        if (!internalSoundEnabled) return
        if (velocity > 0) {
            softwareSynth.noteOn(pitch, velocity, currentInstrument)
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
        // Sincronizar Local Control del teclado físico:
        // - Sintetizador ON  → Local Control OFF (el hardware no suena, solo el synth)
        // - Sintetizador OFF → Local Control ON  (el hardware suena por sus propios altavoces/salida de audio)
        try {
            activeOutputDevice?.let { outDev ->
                if (outDev.isOpen) {
                    val localControlValue = if (enabled) 0 else 127
                    val msg = ShortMessage()
                    msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 122, localControlValue)
                    outDev.receiver.send(msg, -1)
                    println("Local Control: ${if (localControlValue == 0) "OFF" else "ON"} (internalSound=$enabled)")
                }
            }
        } catch (e: Exception) {}
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

    override fun selectInstrument(instrument: InstrumentType) {
        currentInstrument = instrument
    }

    override fun getInstrument(): InstrumentType = currentInstrument
}

actual fun getPlatformMidiDeviceManager(): MidiDeviceManager {
    return DesktopMidiDeviceManager()
}
