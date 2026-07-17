package com.biglexj.elytesia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import com.biglexj.elytesia.theme.*

enum class NoteLabelMode(val displayName: String) {
    NONE("Sin nombres"),
    SOLFEGE("Do-Re-Mi"),
    LETTERS("C-D-E"),
    NUMBERS("1-7");

    fun next(): NoteLabelMode = entries[(ordinal + 1) % entries.size]
}

@Composable
fun PianoKeyboard(
    songActiveKeys: Map<Int, Int>, // Notas tocadas por la canción
    songActiveTracks: Map<Int, Int> = emptyMap(),
    userActiveKeys: Set<Int>,      // Notas pulsadas físicamente por el usuario (o con el ratón)
    userActiveTracks: Map<Int, Int> = emptyMap(),
    wrongUserKeys: Set<Int> = emptySet(),
    onKeyAction: (pitch: Int, isPressed: Boolean) -> Unit, // Callback para Modo Libre
    minPitch: Int = 21,            // Nota mínima dinámica
    maxPitch: Int = 108,           // Nota máxima dinámica
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
        modifier = modifier.pointerInput(minPitch, maxPitch) { // Re-enlaza si cambian los límites
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
                    val x = position.x
                    val y = position.y
                    
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    
                    fun isBlackKey(pitch: Int): Boolean {
                        return when (pitch % 12) {
                            1, 3, 6, 8, 10 -> true
                            else -> false
                        }
                    }
                    
                    var whiteKeyCount = 0
                    for (p in minPitch..maxPitch) {
                        if (!isBlackKey(p)) whiteKeyCount++
                    }
                    val whiteKeyWidth = width / whiteKeyCount
                    val blackKeyWidth = whiteKeyWidth * 0.58f
                    val blackKeyHeight = height * 0.62f
                    
                    fun getWhiteKeyIndex(pitch: Int): Int {
                        var index = 0
                        for (p in minPitch until pitch) {
                            if (!isBlackKey(p)) index++
                        }
                        return index
                    }
                    
                    // Encontrar qué tecla está bajo el ratón/puntero
                    var detectedPitch: Int? = null
                    if (x in 0f..width && y in 0f..height) {
                        // 1. Revisar teclas negras
                        if (y < blackKeyHeight) {
                            for (pitch in minPitch..maxPitch) {
                                if (!isBlackKey(pitch)) continue
                                var leftWhitePitch = pitch - 1
                                while (isBlackKey(leftWhitePitch)) leftWhitePitch--
                                val leftWhiteIndex = getWhiteKeyIndex(leftWhitePitch)
                                val boundaryX = (leftWhiteIndex + 1) * whiteKeyWidth
                                val left = boundaryX - (blackKeyWidth / 2f)
                                val right = left + blackKeyWidth
                                if (x >= left && x <= right) {
                                    detectedPitch = pitch
                                    break
                                }
                            }
                        }
                        
                        // 2. Si no es tecla negra, revisar teclas blancas
                        if (detectedPitch == null) {
                            val whiteIndex = (x / whiteKeyWidth).toInt().coerceIn(0, whiteKeyCount - 1)
                            var currentWhiteIndex = 0
                            for (pitch in minPitch..maxPitch) {
                                if (!isBlackKey(pitch)) {
                                    if (currentWhiteIndex == whiteIndex) {
                                        detectedPitch = pitch
                                        break
                                    }
                                    currentWhiteIndex++
                                }
                            }
                        }
                    }
                    
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

        fun isBlackKey(pitch: Int): Boolean {
            return when (pitch % 12) {
                1, 3, 6, 8, 10 -> true
                else -> false
            }
        }

        var whiteKeyCount = 0
        for (p in minPitch..maxPitch) {
            if (!isBlackKey(p)) whiteKeyCount++
        }

        val whiteKeyWidth = width / whiteKeyCount
        val blackKeyWidth = whiteKeyWidth * 0.58f
        val blackKeyHeight = height * 0.62f

        fun getWhiteKeyIndex(pitch: Int): Int {
            var index = 0
            for (p in minPitch until pitch) {
                if (!isBlackKey(p)) index++
            }
            return index
        }

        // 1. Dibujar Teclas Blancas
        for (pitch in minPitch..maxPitch) {
            if (isBlackKey(pitch)) continue
            
            val index = getWhiteKeyIndex(pitch)
            val left = index * whiteKeyWidth
            val rectSize = Size(whiteKeyWidth, height)
            
            val isUserActive = pitch in userActiveKeys
            val isWrong = pitch in wrongUserKeys
            val isSongActive = pitch in songActiveKeys
            
            val fillBrush = when {
                isWrong -> {
                    Brush.verticalGradient(listOf(musicTheme.wrongNote.copy(alpha = 0.45f), musicTheme.wrongNote))
                }
                isSongActive -> {
                    val trackColor = HandColorResolver.color(musicTheme, pitch, songActiveTracks[pitch])
                    Brush.verticalGradient(
                        listOf(trackColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.15f, 0.85f)), trackColor)
                    )
                }
                isUserActive -> {
                    val track = userActiveTracks[pitch]
                    val latchedColor = track?.let { HandColorResolver.color(musicTheme, pitch, it) }
                        ?: musicTheme.whiteKeyPressed
                    Brush.verticalGradient(
                        colors = listOf(
                            latchedColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.15f, 0.85f)),
                            latchedColor
                        )
                    )
                }
                else -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            musicTheme.whiteKey,
                            musicTheme.whiteKey.copy(alpha = 0.82f)
                        )
                    )
                }
            }
            
            drawRect(
                brush = fillBrush,
                topLeft = Offset(left, 0f),
                size = rectSize
            )
            
            drawRect(
                color = materialColors.outline,
                topLeft = Offset(left, 0f),
                size = rectSize,
                style = Stroke(width = 1.5f)
            )
        }

        // 2. Dibujar Teclas Negras (Superpuestas)
        for (pitch in minPitch..maxPitch) {
            if (!isBlackKey(pitch)) continue
            
            var leftWhitePitch = pitch - 1
            while (isBlackKey(leftWhitePitch)) leftWhitePitch--
            
            val leftWhiteIndex = getWhiteKeyIndex(leftWhitePitch)
            val boundaryX = (leftWhiteIndex + 1) * whiteKeyWidth
            val left = boundaryX - (blackKeyWidth / 2f)
            
            val isUserActive = pitch in userActiveKeys
            val isWrong = pitch in wrongUserKeys
            val isSongActive = pitch in songActiveKeys
            
            val fillBrush = when {
                isWrong -> {
                    Brush.verticalGradient(listOf(musicTheme.wrongNote, musicTheme.wrongNote.copy(alpha = 0.55f)))
                }
                isSongActive -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            musicTheme.blackKeyPressed,
                            musicTheme.blackKeyPressed.copy(alpha = themeEffects.pressedGlow.coerceIn(0.2f, 0.85f))
                        )
                    )
                }
                isUserActive -> {
                    val latchedColor = musicTheme.blackKeyPressed
                    Brush.verticalGradient(
                        colors = listOf(
                            latchedColor,
                            latchedColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.2f, 0.85f))
                        )
                    )
                }
                else -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            musicTheme.blackKey.copy(alpha = 0.82f),
                            musicTheme.blackKey
                        )
                    )
                }
            }
            
            drawRect(
                brush = fillBrush,
                topLeft = Offset(left, 0f),
                size = Size(blackKeyWidth, blackKeyHeight)
            )
            
            drawRect(
                color = materialColors.outline,
                topLeft = Offset(left, 0f),
                size = Size(blackKeyWidth, blackKeyHeight),
                style = Stroke(width = 1f)
            )
        }

        // 3. Nombres de las notas sobre las teclas blancas
        if (noteLabelMode != NoteLabelMode.NONE) {
            val solfege = listOf("Do", "Re", "Mi", "Fa", "Sol", "La", "Si")
            val letters = listOf("C", "D", "E", "F", "G", "A", "B")
            val naturalPitchClasses = listOf(0, 2, 4, 5, 7, 9, 11)

            for (pitch in minPitch..maxPitch) {
                if (isBlackKey(pitch)) continue
                val degree = naturalPitchClasses.indexOf((pitch % 12 + 12) % 12)
                if (degree < 0) continue
                val label = when (noteLabelMode) {
                    NoteLabelMode.SOLFEGE -> solfege[degree]
                    NoteLabelMode.LETTERS -> letters[degree]
                    NoteLabelMode.NUMBERS -> (degree + 1).toString()
                    NoteLabelMode.NONE -> ""
                }
                val layout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        color = if (pitch in userActiveKeys || pitch in songActiveKeys || pitch in wrongUserKeys) materialColors.onPrimary else materialColors.outline,
                        fontSize = if (whiteKeyWidth < 18f) 5.sp else 7.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                val left = getWhiteKeyIndex(pitch) * whiteKeyWidth
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        left + (whiteKeyWidth - layout.size.width) / 2f,
                        height - layout.size.height - 5f
                    )
                )
            }
        }
    }
}
