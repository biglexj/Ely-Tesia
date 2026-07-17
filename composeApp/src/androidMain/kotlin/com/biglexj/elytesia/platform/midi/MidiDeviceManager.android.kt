package com.biglexj.elytesia.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiOutputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

private class AndroidSoftwareSynth {
    private val sampleRate = 48_000
    private val notes = mutableMapOf<Int, Voice>()
    private val sustained = mutableSetOf<Int>()
    private var sustain = false
    @Volatile private var running = true

    private data class Voice(
        val pitch: Int,
        val frequency: Double,
        val velocity: Int,
        var phase: Double = 0.0,
        var age: Long = 0,
        var released: Boolean = false,
        var releaseAge: Long = 0,
        var releaseLevel: Double = 0.0,
        var amplitude: Double = 0.0
    )

    private val audioTrack: AudioTrack? = runCatching {
        val minimum = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minimum, 2048))
            .setTransferMode(AudioTrack.MODE_STREAM)
        if (android.os.Build.VERSION.SDK_INT >= 26) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        builder.build().also { it.play() }
    }.getOrNull()

    init {
        audioTrack?.let { track ->
            thread(isDaemon = true, name = "ElyTesiaAndroidSynth") {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(256)
                while (running) {
                    render(buffer)
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            }
        }
    }

    fun noteOn(pitch: Int, velocity: Int) = synchronized(notes) {
        sustained.remove(pitch)
        notes[pitch] = Voice(pitch, 440.0 * 2.0.pow((pitch - 69.0) / 12.0), velocity)
    }

    fun noteOff(pitch: Int) = synchronized(notes) {
        if (sustain) sustained += pitch else release(pitch)
    }

    fun setSustain(enabled: Boolean) = synchronized(notes) {
        sustain = enabled
        if (!enabled) { sustained.toList().forEach(::release); sustained.clear() }
    }

    fun click() = synchronized(notes) { notes[-1] = Voice(-1, 1000.0, 90) }
    fun allOff() = synchronized(notes) { notes.clear(); sustained.clear(); sustain = false }

    private fun release(pitch: Int) {
        notes[pitch]?.takeIf { !it.released }?.let { it.released = true; it.releaseAge = 0; it.releaseLevel = it.amplitude }
    }

    private fun render(buffer: ShortArray) = synchronized(notes) {
        val gain = 1.0 / sqrt(notes.size.coerceAtLeast(1).toDouble())
        for (index in buffer.indices) {
            var mixed = 0.0
            notes.values.forEach { voice ->
                voice.phase += 2.0 * PI * voice.frequency / sampleRate
                if (voice.phase > 2.0 * PI) voice.phase -= 2.0 * PI
                var tone = sin(voice.phase)
                if (voice.frequency * 2 < sampleRate / 2) tone += sin(voice.phase * 2) * 0.22
                if (voice.frequency * 3 < sampleRate / 2) tone += sin(voice.phase * 3) * 0.08
                mixed += tone * voice.amplitude
                voice.age++
                val seconds = voice.age.toDouble() / sampleRate
                voice.amplitude = if (voice.pitch == -1) {
                    (1.0 - seconds / 0.05).coerceAtLeast(0.0) * 0.10
                } else if (voice.released) {
                    voice.releaseAge++
                    voice.releaseLevel * (1.0 - voice.releaseAge.toDouble() / (sampleRate * 0.10)).coerceAtLeast(0.0)
                } else {
                    (seconds / 0.012).coerceIn(0.0, 1.0) * (1.0 - seconds / 3.0).coerceAtLeast(0.0) * (voice.velocity / 127.0) * 0.18
                }
            }
            buffer[index] = (tanh(mixed * gain * 1.2) * Short.MAX_VALUE * 0.9).toInt().toShort()
        }
        notes.entries.removeAll { it.value.amplitude < 0.0005 && (it.value.released || it.value.age > sampleRate / 20) }
    }

    private fun Double.pow(value: Double) = Math.pow(this, value)
}

class AndroidMidiDeviceManager(context: Context) : MidiDeviceManager {
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private var openedDevice: MidiDevice? = null
    private var openedPort: MidiOutputPort? = null
    private val synth = AndroidSoftwareSynth()
    @Volatile private var internalSound = true
    private var currentInstrument = InstrumentType.PIANO_ACUSTICO

    private data class DevicePort(val label: String, val info: MidiDeviceInfo, val portNumber: Int)

    private fun devicePorts(): List<DevicePort> {
        val devices = if (android.os.Build.VERSION.SDK_INT >= 33) {
            midiManager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            @Suppress("DEPRECATION") midiManager.devices.toSet()
        }
        return devices.flatMap { info ->
            val baseName = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "Dispositivo MIDI ${info.id}"
            info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }.map { port ->
                val portName = port.name?.takeIf { it.isNotBlank() }
                DevicePort(if (portName == null) baseName else "$baseName · $portName", info, port.portNumber)
            }
        }
    }

    override fun getAvailableDevices(): List<String> = devicePorts().map { it.label }.distinct()

    override fun openDevice(
        deviceName: String,
        onNoteOn: (pitch: Int, velocity: Int) -> Unit,
        onNoteOff: (pitch: Int) -> Unit,
        onControlChange: (controller: Int, value: Int, channel: Int) -> Unit
    ) {
        closeDevice()
        val target = devicePorts().firstOrNull { it.label == deviceName } ?: return
        midiManager.openDevice(target.info, { device ->
            if (device == null) return@openDevice
            openedDevice = device
            val port = device.openOutputPort(target.portNumber) ?: return@openDevice
            val receiver = AndroidNoteReceiver(
                onNoteOn = { pitch, velocity -> onNoteOn(pitch, velocity); playNoteDirect(pitch, velocity) },
                onNoteOff = { pitch -> onNoteOff(pitch); playNoteDirect(pitch, 0) },
                onControlChange = { controller, value, channel ->
                    onControlChange(controller, value, channel)
                    playControlChange(controller, value)
                }
            )
            port.connect(receiver)
            openedPort = port
        }, Handler(Looper.getMainLooper()))
    }

    override fun closeDevice() {
        runCatching { openedPort?.close() }
        runCatching { openedDevice?.close() }
        openedPort = null
        openedDevice = null
    }

    override fun playNoteDirect(pitch: Int, velocity: Int) {
        if (!internalSound) return
        if (velocity > 0) synth.noteOn(pitch, velocity) else synth.noteOff(pitch)
    }
    override fun playControlChange(controller: Int, value: Int) { if (controller == 64) synth.setSustain(value >= 64) }
    override fun playMetronomeClick() { if (internalSound) synth.click() }
    override fun stopAllNotes() = synth.allOff()
    override fun setInternalSoundEnabled(enabled: Boolean) { internalSound = enabled; if (!enabled) synth.allOff() }
    override fun getAudioOutputs(): List<String> = listOf("Sistema (Android)")
    override fun selectAudioOutput(name: String) = Unit

    override fun selectInstrument(instrument: InstrumentType) {
        currentInstrument = instrument
    }

    override fun getInstrument(): InstrumentType = currentInstrument

    private class AndroidNoteReceiver(
        private val onNoteOn: (Int, Int) -> Unit,
        private val onNoteOff: (Int) -> Unit,
        private val onControlChange: (Int, Int, Int) -> Unit
    ) : MidiReceiver() {
        private var runningStatus = 0
        private var expectedData = 0
        private val data = ArrayList<Int>(2)

        override fun onSend(message: ByteArray, offset: Int, count: Int, timestamp: Long) {
            for (index in offset until offset + count) feed(message[index].toInt() and 0xFF)
        }

        private fun feed(value: Int) {
            if (value >= 0xF8) return // Mensajes realtime intercalables
            if (value >= 0x80) {
                data.clear()
                if (value < 0xF0) {
                    runningStatus = value
                    val command = value and 0xF0
                    expectedData = if (command == 0xC0 || command == 0xD0) 1 else 2
                } else {
                    runningStatus = 0
                    expectedData = 0
                }
                return
            }
            if (runningStatus == 0 || expectedData == 0) return
            data += value
            if (data.size < expectedData) return

            val command = runningStatus and 0xF0
            if (expectedData == 2) {
                val pitch = data[0]
                val velocity = data[1]
                when {
                    command == 0x90 && velocity > 0 -> onNoteOn(pitch, velocity)
                    command == 0x80 || (command == 0x90 && velocity == 0) -> onNoteOff(pitch)
                    command == 0xB0 -> onControlChange(pitch, velocity, runningStatus and 0x0F)
                }
            }
            data.clear()
        }
    }
}

private class UnavailableAndroidMidiDeviceManager : MidiDeviceManager {
    override fun getAvailableDevices() = emptyList<String>()
    override fun openDevice(deviceName: String, onNoteOn: (Int, Int) -> Unit, onNoteOff: (Int) -> Unit, onControlChange: (Int, Int, Int) -> Unit) = Unit
    override fun closeDevice() = Unit
    override fun playNoteDirect(pitch: Int, velocity: Int) = Unit
    override fun playControlChange(controller: Int, value: Int) = Unit
    override fun playMetronomeClick() = Unit
    override fun stopAllNotes() = Unit
    override fun setInternalSoundEnabled(enabled: Boolean) = Unit
    override fun getAudioOutputs() = emptyList<String>()
    override fun selectAudioOutput(name: String) = Unit
    override fun selectInstrument(instrument: InstrumentType) = Unit
    override fun getInstrument(): InstrumentType = InstrumentType.PIANO_ACUSTICO
}

actual fun getPlatformMidiDeviceManager(): MidiDeviceManager = UnavailableAndroidMidiDeviceManager()
