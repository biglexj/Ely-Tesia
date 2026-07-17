package com.biglexj.elytesia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.biglexj.elytesia.model.NoteEvent
import com.biglexj.elytesia.theme.*
import kotlin.random.Random

// Estructura para las partículas de brillo que salen al pulsar teclas
data class NoteParticle(
    val x: Float,
    var y: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    var alpha: Float,
    var life: Float // 1.0f a 0.0f
)

@Composable
fun PianoRollCanvas(
    notes: List<NoteEvent>,
    currentTimeMs: Long,
    activeKeys: Map<Int, Int>,
    activeTracks: Map<Int, Int> = emptyMap(),
    minPitch: Int = 21,
    maxPitch: Int = 108,
    modifier: Modifier = Modifier,
    timeWindowMs: Long = 2000L // Cuánto tiempo de notas cabe en la pantalla verticalmente
) {
    val particles = remember { mutableStateListOf<NoteParticle>() }
    val musicTheme = LocalElyMusicTheme.current
    val themeEffects = LocalElyThemeEffects.current

    // Generador de partículas para las notas activas
    LaunchedEffect(activeKeys.toMap(), activeTracks.toMap(), currentTimeMs) {
        if (activeKeys.isNotEmpty()) {
            activeKeys.forEach { (pitch, velocity) ->
                // Generar 1-2 partículas por cada tecla activa en cada tick
                val isBlack = when (pitch % 12) {
                    1, 3, 6, 8, 10 -> true
                    else -> false
                }
                val particleColor = if (isBlack) {
                    musicTheme.blackKeyPressed
                } else {
                    if (HandColorResolver.isLeftHand(pitch, activeTracks[pitch])) musicTheme.particleLeft else musicTheme.particleRight
                }
                
                // Necesitamos calcular el X aproximado de la nota
                // Para esto pasaremos un disparador de partículas temporal
                particles.add(
                    NoteParticle(
                        x = pitch.toFloat(), // Guardamos el pitch y lo mapeamos en el dibujo
                        y = 0f, // Comienza en el fondo (teclado)
                        speedY = Random.nextFloat() * 4f + 2f,
                        color = particleColor,
                        size = (Random.nextFloat() * 6f + 4f) * themeEffects.particleIntensity.coerceAtLeast(0.1f),
                        alpha = 1.0f,
                        life = 1.0f
                    )
                )
            }
        }
    }

    // Loop de animación para las partículas
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.y += p.speedY // Mover hacia arriba (se resta de la base en el dibujo)
                    p.life -= 0.04f
                    p.alpha = p.life
                    if (p.life <= 0f) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        fun isBlackKey(pitch: Int): Boolean {
            return when (pitch % 12) {
                1, 3, 6, 8, 10 -> true
                else -> false
            }
        }

        // Cantidad de teclas blancas
        var whiteKeyCount = 0
        for (p in minPitch..maxPitch) {
            if (!isBlackKey(p)) whiteKeyCount++
        }

        val whiteKeyWidth = width / whiteKeyCount
        val blackKeyWidth = whiteKeyWidth * 0.58f

        fun getWhiteKeyIndex(pitch: Int): Int {
            var index = 0
            for (p in minPitch until pitch) {
                if (!isBlackKey(p)) index++
            }
            return index
        }

        fun getNoteXAndWidth(pitch: Int): Pair<Float, Float> {
            return if (isBlackKey(pitch)) {
                var leftWhitePitch = pitch - 1
                while (isBlackKey(leftWhitePitch)) leftWhitePitch--
                val leftWhiteIndex = getWhiteKeyIndex(leftWhitePitch)
                val boundaryX = (leftWhiteIndex + 1) * whiteKeyWidth
                val left = boundaryX - (blackKeyWidth / 2f)
                Pair(left, blackKeyWidth)
            } else {
                val index = getWhiteKeyIndex(pitch)
                val left = index * whiteKeyWidth
                Pair(left, whiteKeyWidth)
            }
        }

        // 1. Dibujar líneas de cuadrícula verticales sutiles
        for (pitch in minPitch..maxPitch) {
            if (isBlackKey(pitch)) continue
            val index = getWhiteKeyIndex(pitch)
            val left = index * whiteKeyWidth
            drawLine(
                color = BorderGray.copy(alpha = 0.15f),
                start = Offset(left, 0f),
                end = Offset(left, height),
                strokeWidth = 1f
            )
        }

        // 2. Dibujar Notas que Caen
        val visibleNotes = notes.filter { note ->
            val noteEndTime = note.startTimeMs + note.durationMs
            noteEndTime >= currentTimeMs && note.startTimeMs <= currentTimeMs + timeWindowMs
        }

        visibleNotes.forEach { note ->
            val (x, noteWidth) = getNoteXAndWidth(note.pitch)

            // Calcular coordenadas Y
            // hits the bottom (y = height) when currentTimeMs == note.startTimeMs
            val yBottom = height * (1f - (note.startTimeMs - currentTimeMs).toFloat() / timeWindowMs)
            val yTop = height * (1f - ((note.startTimeMs + note.durationMs) - currentTimeMs).toFloat() / timeWindowMs)
            
            // Intersección real con el viewport. Antes se movía yTop a cero pero
            // se conservaba toda la altura, haciendo aparecer notas largas antes
            // de que su borde de llegada hubiera entrado en pantalla.
            val drawY = yTop.coerceIn(0f, height)
            val drawBottom = yBottom.coerceIn(0f, height)
            val drawHeight = drawBottom - drawY

            if (drawHeight > 0f) {
                // Color según el pitch o canal (Violeta, Verde, Rosa)
                val baseColor = if (isBlackKey(note.pitch)) musicTheme.blackKeyPressed
                    else HandColorResolver.color(musicTheme, note.pitch, note.track)

                val brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.9f),
                        baseColor.copy(alpha = themeEffects.noteTrail.coerceIn(0.15f, 1f))
                    )
                )

                // Rectángulo redondeado para la nota
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x + 2f, drawY),
                    size = Size(noteWidth - 4f, drawHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Borde brillante
                drawRoundRect(
                    color = baseColor.copy(alpha = 0.7f),
                    topLeft = Offset(x + 2f, drawY),
                    size = Size(noteWidth - 4f, drawHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1f)
                )
            }
        }

        // 3. Dibujar partículas de brillo activas
        particles.forEach { p ->
            val (xOffset, noteWidth) = getNoteXAndWidth(p.x.toInt())
            val centerX = xOffset + (noteWidth / 2f)
            // p.y es la distancia que ha subido desde el fondo
            val drawY = height - p.y
            
            if (drawY > 0f) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(centerX + Random.nextInt(-8, 8), drawY)
                )
            }
        }
    }
}
