package com.biglexj.elytesia.features.instrument

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.biglexj.elytesia.shared.components.ElyButton

/**
 * Panel de Configuración de Teclado MIDI y Audio Virtual.
 * Agrupa la selección de dispositivos MIDI, mapeo guía y salida de sonido interno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardConfigPanel(
    availableMidiDevices: List<String>,
    selectedMidiDevice: String,
    onSelectMidiDevice: (String) -> Unit,
    onRefreshMidiDevices: () -> Unit,
    internalSoundEnabled: Boolean,
    onToggleInternalSound: (Boolean) -> Unit,
    onStartMappingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de Teclado & MIDI",
            color = colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // MIDI Device Selector
        ConfigCard(title = "Teclado / Dispositivo MIDI Entrada") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                var midiExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = midiExpanded,
                    onExpandedChange = { midiExpanded = !midiExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (selectedMidiDevice.isBlank()) "Sin teclado conectado" else selectedMidiDevice,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = midiExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = midiExpanded,
                        onDismissRequest = { midiExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin teclado conectado") },
                            onClick = {
                                onSelectMidiDevice("")
                                midiExpanded = false
                            }
                        )
                        availableMidiDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text(dev) },
                                onClick = {
                                    onSelectMidiDevice(dev)
                                    midiExpanded = false
                                }
                            )
                        }
                    }
                }

                ElyButton(
                    text = "Refrescar",
                    onClick = onRefreshMidiDevices,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            }
        }

        // Mapeo MIDI Guía Card (Contenedor sólido coherente)
        ConfigCard(title = "Mapeo de Teclado Físico") {
            Text(
                text = "Asigna rangos y notas de tu teclado MIDI físico para guiar tu práctica en pantalla.",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )

            ElyButton(
                text = "🎯 Mapear Teclado MIDI Guía",
                onClick = onStartMappingMode,
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                height = 42.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        // Internal Sound Toggle Switch
        ConfigCard(title = "Salida de Sonido Virtual") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sintetizador de sonido interno",
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Desactiva si prefieres usar el altavoz propio de tu teclado MIDI",
                        color = colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = internalSoundEnabled,
                    onCheckedChange = onToggleInternalSound
                )
            }
        }

        // Centro de Feedback & Reporte de Errores Card
        ConfigCard(title = "Centro de Feedback & Reportes") {
            Text(
                text = "¿Encontraste un error o tienes alguna sugerencia para mejorar Ely-Tesia?",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )

            ElyButton(
                text = "💬 Enviar Feedback / Reportar Error",
                onClick = { runCatching { uriHandler.openUri("https://github.com/biglexj/Ely-Tesia/issues") } },
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
                height = 38.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ConfigCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
