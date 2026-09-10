package com.biglexj.elytesia.features.keyboard


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
import com.biglexj.elytesia.features.player.PlaybackLogic
import com.biglexj.elytesia.theme.*
import kotlin.random.Random

// Estructura para las partículas de brillo que salen al pulsar teclas
class NoteParticle(
    val x: Float,
    var y: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    var alpha: Float,
    var life: Float, // 1.0f a 0.0f
    val xJitter: Int
)

@Composable
fun PianoRollCanvas(
    notes: List<NoteEvent>,
    currentTimeMsProvider: () -> Float,
    activeKeys: Map<Int, Int>,
    activeTracks: Map<Int, Int> = emptyMap(),
    minPitch: Int = 21,
    maxPitch: Int = 108,
    modifier: Modifier = Modifier,
    timeWindowMs: Long = 2000L // Cuánto tiempo de notas cabe en la pantalla verticalmente
) {
    val particles = remember { ArrayList<NoteParticle>() }
    val timeline = remember(notes) { PlaybackLogic.TimelineIndex(notes) }
    val musicTheme = LocalElyMusicTheme.current
    val themeEffects = LocalElyThemeEffects.current

    // Generador de partículas: solo se ejecuta cuando cambia el conjunto de notas activas.
    LaunchedEffect(activeKeys.toMap()) {
        if (activeKeys.isNotEmpty()) {
            activeKeys.forEach { (pitch, track) ->
                if (particles.size >= MAX_PARTICLES) {
                    particles.removeAt(0)
                }

                val particleColor = HandColorResolver.color(musicTheme, pitch, track)
                
                particles.add(
                    NoteParticle(
                        x = pitch.toFloat(),
                        y = 0f,
                        speedY = Random.nextFloat() * 4f + 2f,
                        color = particleColor,
                        size = (Random.nextFloat() * 6f + 4f) * themeEffects.particleIntensity.coerceAtLeast(0.1f),
                        alpha = 1.0f,
                        life = 1.0f,
                        xJitter = Random.nextInt(-8, 8)
                    )
                )
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

        // 2. Dibujar Notas que Caen (Sub-pixel smooth, cero asignaciones de memoria)
        val currentTime = currentTimeMsProvider()
        val currentLong = currentTime.toLong()

        timeline.forEachVisibleNote(currentLong, timeWindowMs) { note ->
            val (x, noteWidth) = getNoteXAndWidth(note.pitch)

            val yBottom = height * (1f - (note.startTimeMs - currentTime) / timeWindowMs.toFloat())
            val yTop = height * (1f - ((note.startTimeMs + note.durationMs) - currentTime) / timeWindowMs.toFloat())
            
            val drawY = yTop.coerceIn(0f, height)
            val drawBottom = yBottom.coerceIn(0f, height)
            val drawHeight = drawBottom - drawY

            if (drawHeight > 0f) {
                val baseColor = HandColorResolver.color(musicTheme, note.pitch, note.track)
                val trailAlpha = (themeEffects.noteTrail * 0.40f).coerceIn(0.12f, 0.70f)

                // 1. Estela y desenfoque de movimiento vertical (Motion Blur / Trail)
                // Difuminado progresivo desde la cola superior (drawY) hasta la cabeza de impacto (drawBottom)
                val noteBrush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = trailAlpha),
                        baseColor.copy(alpha = 0.95f)
                    ),
                    startY = drawY,
                    endY = drawBottom
                )

                drawRoundRect(
                    brush = noteBrush,
                    topLeft = Offset(x + 2f, drawY),
                    size = Size(noteWidth - 4f, drawHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // 2. Cabeza luminosa de impacto en el extremo inferior (Head Glow)
                if (drawHeight >= 6f) {
                    val headHeight = (noteWidth * 0.35f).coerceIn(2f, 8f).coerceAtMost(drawHeight * 0.45f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.40f),
                        topLeft = Offset(x + 3f, drawBottom - headHeight),
                        size = Size(noteWidth - 6f, headHeight),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                // 3. Contorno armónico suavizado (elimina el judder/estroboscópico de bordes blancos duros)
                drawRoundRect(
                    color = baseColor.copy(alpha = 0.55f),
                    topLeft = Offset(x + 2f, drawY),
                    size = Size(noteWidth - 4f, drawHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1f)
                )
            }
        }

        // 3. Actualizar física y dibujar partículas directamente en DrawScope (Index loop, sin iterador)
        if (particles.isNotEmpty()) {
            for (i in particles.lastIndex downTo 0) {
                val p = particles[i]
                p.y += p.speedY
                p.life -= 0.04f
                p.alpha = p.life.coerceIn(0f, 1f)

                if (p.life <= 0f) {
                    particles.removeAt(i)
                } else {
                    val (xOffset, noteWidth) = getNoteXAndWidth(p.x.toInt())
                    val centerX = xOffset + (noteWidth / 2f)
                    val drawY = height - p.y
                    
                    if (drawY > 0f) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = p.size,
                            center = Offset(centerX + p.xJitter, drawY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PianoRollCanvas(
    notes: List<NoteEvent>,
    currentTimeMs: Long,
    activeKeys: Map<Int, Int>,
    activeTracks: Map<Int, Int> = emptyMap(),
    minPitch: Int = 21,
    maxPitch: Int = 108,
    modifier: Modifier = Modifier,
    timeWindowMs: Long = 2000L
) = PianoRollCanvas(
    notes = notes,
    currentTimeMsProvider = { currentTimeMs.toFloat() },
    activeKeys = activeKeys,
    activeTracks = activeTracks,
    minPitch = minPitch,
    maxPitch = maxPitch,
    modifier = modifier,
    timeWindowMs = timeWindowMs
)

private const val MAX_PARTICLES = 160
