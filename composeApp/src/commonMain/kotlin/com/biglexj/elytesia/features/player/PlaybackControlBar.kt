package com.biglexj.elytesia.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.features.keyboard.NoteLabelMode
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.ElyPink

@Composable
fun PlaybackControlBar(
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onStop: () -> Unit,
    currentTimeMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    speedMultiplier: Float,
    onSpeedChange: (Float) -> Unit,
    baseBpm: Double = 120.0,
    transposeSemitones: Int = 0,
    onTransposeChange: (Int) -> Unit = {},
    metronomeEnabled: Boolean,
    onMetronomeToggle: () -> Unit,
    waitMode: Boolean,
    onWaitModeToggle: () -> Unit,
    loopEnabled: Boolean,
    onLoopToggle: () -> Unit,
    handMode: HandMode = HandMode.AMBAS,
    onCycleHandMode: () -> Unit = {},
    noteLabelMode: NoteLabelMode,
    onCycleNoteLabelMode: () -> Unit,
    loopStartMs: Long? = null,
    loopEndMs: Long? = null,
    onSetLoopStart: () -> Unit = {},
    onSetLoopEnd: () -> Unit = {},
    onClearLoopAB: () -> Unit = {},
    isCompactHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val inactiveContainer = colors.surfaceVariant.copy(alpha = 0.5f)
    val inactiveContent = colors.onSurfaceVariant

    var showBpmDialog by remember { mutableStateOf(false) }
    var showTransposeDialog by remember { mutableStateOf(false) }

    val currentBpm = (baseBpm * speedMultiplier).toInt()
    val toneLabel = when {
        transposeSemitones > 0 -> "+$transposeSemitones st"
        transposeSemitones < 0 -> "$transposeSemitones st"
        else -> "0 st"
    }

    if (isCompactHeight) {
        // Layout Ultra-Compacto para Teléfonos en Modo Horizontal (Landscape)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Slider de Línea de Tiempo compacto
            if (durationMs > 0L) {
                Slider(
                    value = currentTimeMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(14.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botones principales compactos
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ElyButton(
                        text = if (isPlaying) "Pausar" else "Reproducir",
                        onClick = onPlayToggle,
                        containerColor = if (isPlaying) ElyGreen else AuroraViolet,
                        contentColor = colors.surface,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    )

                    ElyButton(
                        text = "Detener",
                        onClick = onStop,
                        containerColor = ElyPink,
                        contentColor = colors.surface,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    )

                    ElyButton(
                        text = if (waitMode) "Espera ON" else "Espera OFF",
                        onClick = onWaitModeToggle,
                        containerColor = if (waitMode) colors.secondary else inactiveContainer,
                        contentColor = if (waitMode) colors.onSecondary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )

                    ElyButton(
                        text = if (loopEnabled) "Bucle ON" else "Bucle OFF",
                        onClick = onLoopToggle,
                        containerColor = if (loopEnabled) colors.primary else inactiveContainer,
                        contentColor = if (loopEnabled) colors.onPrimary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )

                    ElyButton(
                        text = "${handMode.icon} ${handMode.label}",
                        onClick = onCycleHandMode,
                        containerColor = if (handMode != HandMode.AMBAS) colors.tertiary else inactiveContainer,
                        contentColor = if (handMode != HandMode.AMBAS) colors.onTertiary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )
                }

                // Botones secundarios limpios con modal para BPM y Transposición
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ElyButton(
                        text = "$currentBpm BPM",
                        onClick = { showBpmDialog = true },
                        containerColor = if (speedMultiplier != 1.0f) colors.primary else inactiveContainer,
                        contentColor = if (speedMultiplier != 1.0f) colors.onPrimary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )

                    ElyButton(
                        text = "Tono: $toneLabel",
                        onClick = { showTransposeDialog = true },
                        containerColor = if (transposeSemitones != 0) colors.primary else inactiveContainer,
                        contentColor = if (transposeSemitones != 0) colors.onPrimary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )

                    ElyButton(
                        text = if (metronomeEnabled) "Metro ON" else "Metro OFF",
                        onClick = onMetronomeToggle,
                        containerColor = if (metronomeEnabled) colors.secondary else inactiveContainer,
                        contentColor = if (metronomeEnabled) colors.onSecondary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )

                    ElyButton(
                        text = "Notas: ${noteLabelMode.displayName}",
                        onClick = onCycleNoteLabelMode,
                        containerColor = if (noteLabelMode != NoteLabelMode.NONE) colors.primary else inactiveContainer,
                        contentColor = if (noteLabelMode != NoteLabelMode.NONE) colors.onPrimary else inactiveContent,
                        height = 30.dp,
                        fontSize = 10.sp,
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    )
                }
            }
        }
    } else {
        // Layout Estándar (Desktop / Modo Vertical en Teléfono)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timeline slider
            if (durationMs > 0L) {
                Slider(
                    value = currentTimeMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(18.dp)
                )
            }

            // Row 1: Primary Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElyButton(
                    text = if (isPlaying) "Pausar" else "Reproducir",
                    onClick = onPlayToggle,
                    containerColor = if (isPlaying) ElyGreen else AuroraViolet,
                    contentColor = colors.surface,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = "Detener",
                    onClick = onStop,
                    containerColor = ElyPink,
                    contentColor = colors.surface,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = if (waitMode) "Modo Espera ON" else "Modo Espera OFF",
                    onClick = onWaitModeToggle,
                    containerColor = if (waitMode) colors.secondary else inactiveContainer,
                    contentColor = if (waitMode) colors.onSecondary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = if (loopEnabled) "Bucle ON" else "Bucle OFF",
                    onClick = onLoopToggle,
                    containerColor = if (loopEnabled) colors.primary else inactiveContainer,
                    contentColor = if (loopEnabled) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                // Selector de mano (Ambas / Izquierda / Derecha)
                ElyButton(
                    text = "${handMode.icon} ${handMode.label}",
                    onClick = onCycleHandMode,
                    containerColor = if (handMode != HandMode.AMBAS) colors.tertiary else inactiveContainer,
                    contentColor = if (handMode != HandMode.AMBAS) colors.onTertiary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Secondary Uniform Buttons (BPM Modal, Transposición Modal, Metrónomo, Notas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElyButton(
                    text = "$currentBpm BPM",
                    onClick = { showBpmDialog = true },
                    containerColor = if (speedMultiplier != 1.0f) colors.primary else inactiveContainer,
                    contentColor = if (speedMultiplier != 1.0f) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = "Tono: $toneLabel",
                    onClick = { showTransposeDialog = true },
                    containerColor = if (transposeSemitones != 0) colors.primary else inactiveContainer,
                    contentColor = if (transposeSemitones != 0) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = if (metronomeEnabled) "Metrónomo ON" else "Metrónomo OFF",
                    onClick = onMetronomeToggle,
                    containerColor = if (metronomeEnabled) colors.secondary else inactiveContainer,
                    contentColor = if (metronomeEnabled) colors.onSecondary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = "Notas: ${noteLabelMode.displayName}",
                    onClick = onCycleNoteLabelMode,
                    containerColor = if (noteLabelMode != NoteLabelMode.NONE) colors.primary else inactiveContainer,
                    contentColor = if (noteLabelMode != NoteLabelMode.NONE) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Controles de Práctica por Secciones (Bucle A-B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAbActive = loopStartMs != null || loopEndMs != null
                val startStr = if (loopStartMs != null) formatMs(loopStartMs!!) else "Inicio"
                val endStr = if (loopEndMs != null) formatMs(loopEndMs!!) else "Fin"

                ElyButton(
                    text = if (loopStartMs != null) "📍 A: $startStr" else "📍 A: Inicio",
                    onClick = onSetLoopStart,
                    containerColor = if (loopStartMs != null) colors.primary else inactiveContainer,
                    contentColor = if (loopStartMs != null) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                ElyButton(
                    text = if (loopEndMs != null) "📌 B: $endStr" else "📌 B: Fin",
                    onClick = onSetLoopEnd,
                    containerColor = if (loopEndMs != null) colors.primary else inactiveContainer,
                    contentColor = if (loopEndMs != null) colors.onPrimary else inactiveContent,
                    modifier = Modifier.weight(1f)
                )

                if (isAbActive) {
                    ElyButton(
                        text = "❌ Limpiar",
                        onClick = onClearLoopAB,
                        containerColor = colors.errorContainer,
                        contentColor = colors.onErrorContainer,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }

    // Modal Ajustador de Tempo (BPM)
    if (showBpmDialog) {
        AlertDialog(
            onDismissRequest = { showBpmDialog = false },
            title = {
                Text(
                    text = "🎹 Ajustar Tempo (BPM)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "$currentBpm BPM",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ElyButton(
                            text = "- 5 BPM",
                            onClick = { onSpeedChange((speedMultiplier - 0.05f).coerceAtLeast(0.25f)) },
                            containerColor = inactiveContainer,
                            contentColor = inactiveContent,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        )

                        ElyButton(
                            text = "+ 5 BPM",
                            onClick = { onSpeedChange((speedMultiplier + 0.05f).coerceAtMost(2.0f)) },
                            containerColor = inactiveContainer,
                            contentColor = inactiveContent,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Velocidad Predeterminada:", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            val pct = (speed * 100).toInt()
                            ElyButton(
                                text = "$pct%",
                                onClick = { onSpeedChange(speed) },
                                containerColor = if (speedMultiplier == speed) colors.primary else inactiveContainer,
                                contentColor = if (speedMultiplier == speed) colors.onPrimary else inactiveContent,
                                height = 34.dp,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                ElyButton(
                    text = "Aceptar",
                    onClick = { showBpmDialog = false },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            }
        )
    }

    // Modal Ajustador de Transposición (Tono)
    if (showTransposeDialog) {
        AlertDialog(
            onDismissRequest = { showTransposeDialog = false },
            title = {
                Text(
                    text = "🎵 Transposición (Tono)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    val fullToneText = when {
                        transposeSemitones > 0 -> "+$transposeSemitones semitonos"
                        transposeSemitones < 0 -> "$transposeSemitones semitonos"
                        else -> "Tono Original (0 st)"
                    }

                    Text(
                        text = fullToneText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ElyButton(
                            text = "- 1 Semitono",
                            onClick = { onTransposeChange((transposeSemitones - 1).coerceAtLeast(-12)) },
                            containerColor = inactiveContainer,
                            contentColor = inactiveContent,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        )

                        ElyButton(
                            text = "+ 1 Semitono",
                            onClick = { onTransposeChange((transposeSemitones + 1).coerceAtMost(12)) },
                            containerColor = inactiveContainer,
                            contentColor = inactiveContent,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ElyButton(
                        text = "Restablecer Tono Original (0 st)",
                        onClick = { onTransposeChange(0) },
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.onSurfaceVariant,
                        height = 36.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                ElyButton(
                    text = "Aceptar",
                    onClick = { showTransposeDialog = false },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    val secStr = if (sec < 10) "0$sec" else "$sec"
    return "$min:$secStr"
}
