package com.biglexj.elytesia.features.keyboard

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.theme.HandColorResolver
import com.biglexj.elytesia.theme.ResolvedMusicTheme
import com.biglexj.elytesia.theme.ThemeEffects

/**
 * Utilitarios de renderizado modular para teclas de piano y etiquetas (Material 3 Expressive).
 * Separa la lógica de dibujo de Canvas del composable de control táctil.
 */
object PianoKeyDrawing {

    fun isBlackKey(pitch: Int): Boolean = when ((pitch % 12 + 12) % 12) {
        1, 3, 6, 8, 10 -> true
        else -> false
    }

    fun getWhiteKeyCount(minPitch: Int, maxPitch: Int): Int {
        var count = 0
        for (p in minPitch..maxPitch) {
            if (!isBlackKey(p)) count++
        }
        return count.coerceAtLeast(1)
    }

    fun getWhiteKeyIndex(pitch: Int, minPitch: Int): Int {
        var index = 0
        for (p in minPitch until pitch) {
            if (!isBlackKey(p)) index++
        }
        return index
    }

    /**
     * Renderiza una Tecla Blanca (Nota Natural) en el canvas.
     */
    fun DrawScope.drawWhiteKey(
        left: Float,
        whiteKeyWidth: Float,
        height: Float,
        isUserActive: Boolean,
        isSongActive: Boolean,
        isWrong: Boolean,
        userTrack: Int?,
        songTrack: Int?,
        pitch: Int,
        musicTheme: ResolvedMusicTheme,
        themeEffects: ThemeEffects,
        materialColors: ColorScheme
    ) {
        val rectSize = Size(whiteKeyWidth, height)
        val fillBrush = when {
            isWrong -> Brush.verticalGradient(
                listOf(musicTheme.wrongNote.copy(alpha = 0.45f), musicTheme.wrongNote)
            )
            isSongActive -> {
                val trackColor = HandColorResolver.color(musicTheme, pitch, songTrack)
                Brush.verticalGradient(
                    listOf(trackColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.15f, 0.85f)), trackColor)
                )
            }
            isUserActive -> {
                val latchedColor = userTrack?.let { HandColorResolver.color(musicTheme, pitch, it) }
                    ?: musicTheme.whiteKeyPressed
                Brush.verticalGradient(
                    listOf(latchedColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.15f, 0.85f)), latchedColor)
                )
            }
            else -> Brush.verticalGradient(
                listOf(musicTheme.whiteKey, musicTheme.whiteKey)
            )
        }

        drawRect(brush = fillBrush, topLeft = Offset(left, 0f), size = rectSize)
        drawRect(
            color = materialColors.outline.copy(alpha = 0.35f),
            topLeft = Offset(left, 0f),
            size = rectSize,
            style = Stroke(width = 1.2f)
        )
    }

    /**
     * Renderiza una Teclas Negras (Sostenidos y Bemoles) en el canvas.
     */
    fun DrawScope.drawBlackKey(
        left: Float,
        blackKeyWidth: Float,
        blackKeyHeight: Float,
        isUserActive: Boolean,
        isSongActive: Boolean,
        isWrong: Boolean,
        musicTheme: ResolvedMusicTheme,
        themeEffects: ThemeEffects,
        materialColors: ColorScheme
    ) {
        val size = Size(blackKeyWidth, blackKeyHeight)
        val fillBrush = when {
            isWrong -> Brush.verticalGradient(
                listOf(musicTheme.wrongNote, musicTheme.wrongNote.copy(alpha = 0.55f))
            )
            isSongActive || isUserActive -> {
                val latchedColor = musicTheme.blackKeyPressed
                Brush.verticalGradient(
                    listOf(latchedColor, latchedColor.copy(alpha = themeEffects.pressedGlow.coerceIn(0.2f, 0.85f)))
                )
            }
            else -> Brush.verticalGradient(
                listOf(musicTheme.blackKey, musicTheme.blackKey)
            )
        }

        drawRect(brush = fillBrush, topLeft = Offset(left, 0f), size = size)
        drawRect(
            color = materialColors.outline,
            topLeft = Offset(left, 0f),
            size = size,
            style = Stroke(width = 1f)
        )
    }

    /**
     * Renderiza las etiquetas solfeo/letras sobre las teclas blancas.
     */
    fun DrawScope.drawNoteLabels(
        minPitch: Int,
        maxPitch: Int,
        whiteKeyWidth: Float,
        height: Float,
        noteLabelMode: NoteLabelMode,
        userActiveKeys: Set<Int>,
        songActiveKeys: Map<Int, Int>,
        wrongUserKeys: Set<Int>,
        textMeasurer: TextMeasurer,
        materialColors: ColorScheme
    ) {
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
            if (label.isEmpty()) continue

            val isActive = pitch in userActiveKeys || pitch in songActiveKeys || pitch in wrongUserKeys
            val layout = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    color = if (isActive) Color.White else Color(0xFF0F172A),
                    fontSize = if (whiteKeyWidth < 18f) 7.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            val left = getWhiteKeyIndex(pitch, minPitch) * whiteKeyWidth
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    left + (whiteKeyWidth - layout.size.width) / 2f,
                    height - layout.size.height - 6f
                )
            )
        }
    }
}
