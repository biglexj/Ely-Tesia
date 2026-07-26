package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.TextContrast

@Composable
fun ExportMidiDialog(
    songToExport: Song,
    onConfirmExport: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var exportName by remember { mutableStateOf(songToExport.name) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Exportar Archivo MIDI",
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = exportName,
                    onValueChange = { exportName = it },
                    label = { Text("Nombre de la canción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElyButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        containerColor = colors.background,
                        contentColor = TextContrast,
                        modifier = Modifier.weight(1f)
                    )

                    ElyButton(
                        text = "Exportar",
                        onClick = {
                            onConfirmExport(songToExport.copy(name = exportName))
                            onDismiss()
                        },
                        containerColor = AuroraViolet,
                        contentColor = colors.surface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ExportThemeDialog(
    themeJson: String,
    themeName: String,
    onConfirmExport: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Exportar Tema Visual JSON",
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "El tema '$themeName' será guardado en formato JSON estándar de Ely-Tesia.",
                    color = TextContrast,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElyButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        containerColor = colors.background,
                        contentColor = TextContrast,
                        modifier = Modifier.weight(1f)
                    )

                    ElyButton(
                        text = "Guardar JSON",
                        onClick = {
                            onConfirmExport(themeName, themeJson)
                            onDismiss()
                        },
                        containerColor = AuroraViolet,
                        contentColor = colors.surface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
