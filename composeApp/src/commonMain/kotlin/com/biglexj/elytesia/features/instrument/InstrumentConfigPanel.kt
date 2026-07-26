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
import com.biglexj.elytesia.midi.InstrumentType
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.TextContrast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentConfigPanel(
    availableMidiDevices: List<String>,
    selectedMidiDevice: String,
    onSelectMidiDevice: (String) -> Unit,
    onRefreshMidiDevices: () -> Unit,
    availableAudioOutputs: List<String>,
    selectedAudioOutput: String,
    onSelectAudioOutput: (String) -> Unit,
    selectedInstrument: InstrumentType,
    onSelectInstrument: (InstrumentType) -> Unit,
    internalSoundEnabled: Boolean,
    onToggleInternalSound: (Boolean) -> Unit,
    onStartMappingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de Instrumento & Audio",
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
                    containerColor = AuroraViolet,
                    contentColor = colors.surface
                )
            }

            ElyButton(
                text = "Mapear Teclado MIDI Guía",
                onClick = onStartMappingMode,
                containerColor = colors.surface,
                contentColor = AuroraViolet,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        // Instrument Preset Selector
        ConfigCard(title = "Timbre de Instrumento") {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InstrumentType.entries.forEach { inst ->
                    val isSelected = inst == selectedInstrument
                    val displayName = when (inst) {
                        InstrumentType.PIANO_ACUSTICO -> "Piano Acústico"
                        InstrumentType.PIANO_ELECTRICO -> "Piano Eléctrico"
                        InstrumentType.XILOFONO -> "Xilófono"
                        InstrumentType.SAXOFON -> "Saxofón"
                        InstrumentType.MELODICA -> "Melódica"
                        InstrumentType.ORGANO -> "Órgano"
                        InstrumentType.SINTETIZADOR_PAD -> "Sintetizador Pad"
                        InstrumentType.CLAVECIN -> "Clavecín"
                        InstrumentType.FLAUTA -> "Flauta"
                        InstrumentType.BAJO_SINTETIZADO -> "Bajo Sintetizado"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectInstrument(inst) },
                        label = {
                            Text(
                                text = displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                            containerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = colors.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Internal Sound Toggle Switch
        ConfigCard(title = "Salida de Sonido Interno") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reproducir sintetizador interno",
                    color = TextContrast,
                    fontSize = 12.sp
                )
                Switch(
                    checked = internalSoundEnabled,
                    onCheckedChange = onToggleInternalSound
                )
            }
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
            .background(colors.surface)
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
