package com.biglexj.elytesia.features.instrument

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.midi.InstrumentType

/**
 * Panel dedicado exclusivamente a la Selección de Timbres e Instrumentos Musicales.
 * Muestra una grilla expresiva de tarjetas con íconos, títulos legibles y descripciones.
 */
@Composable
fun InstrumentSelectionPanel(
    selectedInstrument: InstrumentType,
    onSelectInstrument: (InstrumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Instrumentos Musicales",
            color = colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(InstrumentType.entries, key = InstrumentType::name) { inst ->
                val isSelected = inst == selectedInstrument
                val (icon, title, desc) = when (inst) {
                    InstrumentType.PIANO_ACUSTICO -> Triple("🎹", "Piano Acústico", "Timbre clásico de gran piano")
                    InstrumentType.PIANO_ELECTRICO -> Triple("⚡", "Piano Eléctrico", "Tono cálido vintage FM")
                    InstrumentType.XILOFONO -> Triple("🪵", "Xilófono", "Percusión melódica brillante")
                    InstrumentType.SAXOFON -> Triple("🎷", "Saxofón", "Viento madera expresivo")
                    InstrumentType.MELODICA -> Triple("🎹", "Melódica", "Sonido melódico de fuelle")
                    InstrumentType.ORGANO -> Triple("🪗", "Órgano", "Sonido armónico de tubos")
                    InstrumentType.SINTETIZADOR_PAD -> Triple("🎛️", "Sintetizador Pad", "Atmósfera suave y envolvente")
                    InstrumentType.CLAVECIN -> Triple("🎼", "Clavecín", "Pulsación barroca metálica")
                    InstrumentType.FLAUTA -> Triple("🪈", "Flauta", "Tono dulce y fluido de viento")
                    InstrumentType.BAJO_SINTETIZADO -> Triple("🎸", "Bajo Sintetizado", "Grave potente con punch")
                }

                val targetContainer = if (isSelected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.45f)
                val targetBorder = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f)
                val targetContent = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant

                val containerColor by animateColorAsState(targetContainer, spring(stiffness = 400f))
                val borderColor by animateColorAsState(targetBorder, spring(stiffness = 400f))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onSelectInstrument(inst) },
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = targetContent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = icon, fontSize = 20.sp)
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = targetContent
                            )
                        }

                        Text(
                            text = desc,
                            fontSize = 10.sp,
                            color = targetContent.copy(alpha = 0.8f),
                            maxLines = 2,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}
