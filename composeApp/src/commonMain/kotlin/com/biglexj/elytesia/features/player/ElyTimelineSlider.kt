package com.biglexj.elytesia.features.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Deslizador de línea de tiempo continuo de alto rendimiento.
 *
 * Se dibuja íntegramente en Canvas y lee [currentTimeMsProvider] dentro del DrawScope,
 * desacoplando la fase de dibujo de la fase de composición de Compose. Esto elimina por completo
 * los saltitos ("judder") a 60Hz/144Hz y asegura cero recomposiciones durante la reproducción continua.
 */
@Composable
fun ElyTimelineSlider(
    currentTimeMsProvider: () -> Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    loopStartMs: Long? = null,
    loopEndMs: Long? = null,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val totalHeight = if (isCompact) 18.dp else 22.dp
    val trackHeightDp = if (isCompact) 5.dp else 7.dp
    val thumbWidthDp = if (isCompact) 8.dp else 10.dp
    val thumbHeightDp = if (isCompact) 12.dp else 16.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek((fraction * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onSeek((fraction * durationMs).toLong())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onSeek((fraction * durationMs).toLong())
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val trackHeight = trackHeightDp.toPx()
        val thumbWidth = thumbWidthDp.toPx()
        val thumbHeight = thumbHeightDp.toPx()
        val trackY = (height - trackHeight) / 2f
        val trackRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

        // 1. Pista inactiva de fondo (carril sutil)
        drawRoundRect(
            color = colors.surfaceVariant.copy(alpha = 0.45f),
            topLeft = Offset(0f, trackY),
            size = Size(width, trackHeight),
            cornerRadius = trackRadius
        )

        if (durationMs <= 0L) return@Canvas

        // 2. Región de Bucle A-B (si está activa)
        if (loopStartMs != null || loopEndMs != null) {
            val startFrac = ((loopStartMs ?: 0L).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val endFrac = ((loopEndMs ?: durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val startX = startFrac * width
            val endX = endFrac * width
            val regionWidth = (endX - startX).coerceAtLeast(0f)

            if (regionWidth > 0f) {
                drawRoundRect(
                    color = colors.tertiary.copy(alpha = 0.28f),
                    topLeft = Offset(startX, trackY - 1f),
                    size = Size(regionWidth, trackHeight + 2f),
                    cornerRadius = trackRadius
                )

                // Marcador visual punto A
                if (loopStartMs != null) {
                    drawRoundRect(
                        color = colors.tertiary,
                        topLeft = Offset(startX - 1.5f, trackY - 3f),
                        size = Size(3f, trackHeight + 6f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                }

                // Marcador visual punto B
                if (loopEndMs != null) {
                    drawRoundRect(
                        color = colors.tertiary,
                        topLeft = Offset(endX - 1.5f, trackY - 3f),
                        size = Size(3f, trackHeight + 6f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                }
            }
        }

        // 3. Progreso activo continuo (Lectura pura dentro de DrawScope = 60/144Hz fluido)
        val currentMs = if (isDragging) {
            dragFraction * durationMs.toFloat()
        } else {
            currentTimeMsProvider()
        }
        val progressFraction = (currentMs / durationMs.toFloat()).coerceIn(0f, 1f)
        val progressWidth = (progressFraction * width).coerceIn(0f, width)

        if (progressWidth > 0f) {
            // Progreso activo con color primario uniforme (sin variaciones de color)
            drawRoundRect(
                color = colors.primary,
                topLeft = Offset(0f, trackY),
                size = Size(progressWidth, trackHeight),
                cornerRadius = trackRadius
            )
        }

        // 4. Cabezal Scrubber (Píldora indicadora de posición en color primario uniforme)
        val thumbCenterX = progressWidth.coerceIn(thumbWidth / 2f, width - thumbWidth / 2f)
        val thumbLeft = thumbCenterX - (thumbWidth / 2f)
        val thumbTop = (height - thumbHeight) / 2f
        val thumbRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)

        // Resplandor exterior suave del cabezal
        drawRoundRect(
            color = colors.primary.copy(alpha = 0.35f),
            topLeft = Offset(thumbLeft - 2f, thumbTop - 2f),
            size = Size(thumbWidth + 4f, thumbHeight + 4f),
            cornerRadius = CornerRadius((thumbWidth + 4f) / 2f, (thumbWidth + 4f) / 2f)
        )

        // Núcleo del cabezal en color primario uniforme
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(thumbLeft, thumbTop),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = thumbRadius
        )

        // Borde fino nítido para contraste y alta definición
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(thumbLeft, thumbTop),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = thumbRadius,
            style = Stroke(width = 1.2f)
        )
    }
}
