package com.biglexj.elytesia.features.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import com.biglexj.elytesia.theme.LocalElyMusicTheme
import com.biglexj.elytesia.theme.LocalElyThemeEffects

enum class NoteLabelMode(val displayName: String) {
    NONE("Sin nombres"),
    SOLFEGE("Do-Re-Mi"),
    LETTERS("C-D-E"),
    NUMBERS("1-7");

    fun next(): NoteLabelMode = entries[(ordinal + 1) % entries.size]
}

@Composable
fun PianoKeyboard(
    songActiveKeys: Map<Int, Int>,
    songActiveTracks: Map<Int, Int> = emptyMap(),
    userActiveKeys: Set<Int>,
    userActiveTracks: Map<Int, Int> = emptyMap(),
    wrongUserKeys: Set<Int> = emptySet(),
    onKeyAction: (pitch: Int, isPressed: Boolean) -> Unit,
    minPitch: Int = 21,
    maxPitch: Int = 108,
    noteLabelMode: NoteLabelMode = NoteLabelMode.NONE,
    onZoom: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var clickedPitch by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val musicTheme = LocalElyMusicTheme.current
    val themeEffects = LocalElyThemeEffects.current
    val materialColors = MaterialTheme.colorScheme

    Canvas(
        modifier = modifier.pointerInput(minPitch, maxPitch) {
            var previousPinchDistance: Float? = null
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.size >= 2) {
                        clickedPitch?.let { onKeyAction(it, false) }
                        clickedPitch = null
                        val distance = (pressedChanges[0].position - pressedChanges[1].position).getDistance()
                        previousPinchDistance?.takeIf { it > 0f }?.let { previous ->
                            onZoom(distance / previous)
                        }
                        previousPinchDistance = distance
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    previousPinchDistance = null
                    val position = event.changes.firstOrNull()?.position ?: Offset.Zero

                    val detectedPitch = PianoTouchHandler.detectPitchAt(
                        x = position.x,
                        y = position.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        minPitch = minPitch,
                        maxPitch = maxPitch
                    )

                    val isDown = event.changes.any { it.pressed }
                    if (isDown) {
                        if (detectedPitch != clickedPitch) {
                            clickedPitch?.let { onKeyAction(it, false) }
                            detectedPitch?.let { onKeyAction(it, true) }
                            clickedPitch = detectedPitch
                        }
                        event.changes.forEach { it.consume() }
                    } else {
                        clickedPitch?.let { onKeyAction(it, false) }
                        clickedPitch = null
                    }
                }
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val whiteKeyCount = PianoKeyDrawing.getWhiteKeyCount(minPitch, maxPitch)
        val whiteKeyWidth = width / whiteKeyCount
        val blackKeyWidth = whiteKeyWidth * 0.58f
        val blackKeyHeight = height * 0.62f

        // 1. Teclas Blancas
        for (pitch in minPitch..maxPitch) {
            if (PianoKeyDrawing.isBlackKey(pitch)) continue
            val index = PianoKeyDrawing.getWhiteKeyIndex(pitch, minPitch)
            val left = index * whiteKeyWidth
            PianoKeyDrawing.run {
                drawWhiteKey(
                    left = left,
                    whiteKeyWidth = whiteKeyWidth,
                    height = height,
                    isUserActive = pitch in userActiveKeys,
                    isSongActive = pitch in songActiveKeys,
                    isWrong = pitch in wrongUserKeys,
                    userTrack = userActiveTracks[pitch],
                    songTrack = songActiveTracks[pitch] ?: songActiveKeys[pitch],
                    pitch = pitch,
                    musicTheme = musicTheme,
                    themeEffects = themeEffects,
                    materialColors = materialColors
                )
            }
        }

        // 2. Teclas Negras / Accidentales
        for (pitch in minPitch..maxPitch) {
            if (!PianoKeyDrawing.isBlackKey(pitch)) continue
            var leftWhitePitch = pitch - 1
            while (PianoKeyDrawing.isBlackKey(leftWhitePitch)) leftWhitePitch--
            val leftWhiteIndex = PianoKeyDrawing.getWhiteKeyIndex(leftWhitePitch, minPitch)
            val boundaryX = (leftWhiteIndex + 1) * whiteKeyWidth
            val left = boundaryX - (blackKeyWidth / 2f)

            PianoKeyDrawing.run {
                drawBlackKey(
                    left = left,
                    blackKeyWidth = blackKeyWidth,
                    blackKeyHeight = blackKeyHeight,
                    isUserActive = pitch in userActiveKeys,
                    isSongActive = pitch in songActiveKeys,
                    isWrong = pitch in wrongUserKeys,
                    musicTheme = musicTheme,
                    themeEffects = themeEffects,
                    materialColors = materialColors
                )
            }
        }

        // 3. Nombres de las notas
        if (noteLabelMode != NoteLabelMode.NONE) {
            PianoKeyDrawing.run {
                drawNoteLabels(
                    minPitch = minPitch,
                    maxPitch = maxPitch,
                    whiteKeyWidth = whiteKeyWidth,
                    height = height,
                    noteLabelMode = noteLabelMode,
                    userActiveKeys = userActiveKeys,
                    songActiveKeys = songActiveKeys,
                    wrongUserKeys = wrongUserKeys,
                    textMeasurer = textMeasurer,
                    materialColors = materialColors
                )
            }
        }
    }
}
