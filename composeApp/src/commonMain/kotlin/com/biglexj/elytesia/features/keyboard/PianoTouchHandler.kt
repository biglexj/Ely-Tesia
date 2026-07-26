package com.biglexj.elytesia.features.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Gestor táctil modular para el teclado de piano en Compose Multiplatform.
 * Procesa detección de teclas activas por coordenadas X/Y y gestos de pinch-to-zoom.
 */
object PianoTouchHandler {

    fun detectPitchAt(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        minPitch: Int,
        maxPitch: Int
    ): Int? {
        if (x !in 0f..width || y !in 0f..height) return null

        val whiteKeyCount = PianoKeyDrawing.getWhiteKeyCount(minPitch, maxPitch)
        val whiteKeyWidth = width / whiteKeyCount
        val blackKeyWidth = whiteKeyWidth * 0.58f
        val blackKeyHeight = height * 0.62f

        // 1. Revisar hit-test en teclas negras (superpuestas arriba)
        if (y < blackKeyHeight) {
            for (pitch in minPitch..maxPitch) {
                if (!PianoKeyDrawing.isBlackKey(pitch)) continue
                var leftWhitePitch = pitch - 1
                while (PianoKeyDrawing.isBlackKey(leftWhitePitch)) leftWhitePitch--
                val leftWhiteIndex = PianoKeyDrawing.getWhiteKeyIndex(leftWhitePitch, minPitch)
                val boundaryX = (leftWhiteIndex + 1) * whiteKeyWidth
                val left = boundaryX - (blackKeyWidth / 2f)
                val right = left + blackKeyWidth
                if (x in left..right) {
                    return pitch
                }
            }
        }

        // 2. hit-test en teclas blancas
        val whiteIndex = (x / whiteKeyWidth).toInt().coerceIn(0, whiteKeyCount - 1)
        var currentWhiteIndex = 0
        for (pitch in minPitch..maxPitch) {
            if (!PianoKeyDrawing.isBlackKey(pitch)) {
                if (currentWhiteIndex == whiteIndex) {
                    return pitch
                }
                currentWhiteIndex++
            }
        }
        return null
    }
}
