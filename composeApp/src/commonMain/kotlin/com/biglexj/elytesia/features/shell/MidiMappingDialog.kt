package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.TextContrast

@Composable
fun MidiMappingDialog(
    mappingStep: Int,
    onCancel: () -> Unit,
    onResetDefaults: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mapeo de Teclado MIDI",
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (mappingStep == 1) {
                        "Por favor, presiona la TECLA MÁS BAJA (izquierda) de tu teclado físico..."
                    } else {
                        "¡Excelente! Ahora presiona la TECLA MÁS ALTA (derecha) de tu teclado..."
                    },
                    color = AuroraViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElyButton(
                        text = "Cancelar",
                        onClick = onCancel,
                        containerColor = colors.background,
                        contentColor = TextContrast,
                        modifier = Modifier.weight(1f)
                    )

                    ElyButton(
                        text = "Restablecer Standard (88)",
                        onClick = onResetDefaults,
                        containerColor = ElyGreen,
                        contentColor = colors.surface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
