package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biglexj.elytesia.shared.components.ElyBadge
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.update.UpdateChecker
import com.biglexj.elytesia.update.UpdateResult
import kotlinx.coroutines.launch

/**
 * Diálogo de Información "Acerca de la Aplicación" y Apoyo al Autor (Material 3 Expressive).
 * Incluye verificador de actualizaciones multiplataforma (Windows & Android) vía GitHub Releases.
 */
@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(380.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Badge
                ElyBadge(
                    text = "ACERCA DE LA APLICACIÓN",
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )

                Text(
                    text = "Ely-Tesia",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )

                Text(
                    text = "Piano & MIDI Suite Multiplataforma\nVersión ${UpdateChecker.currentVersion} · Licencia MIT (2026)",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                // Mensaje de Apoyo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "¡Gracias por usar Ely-Tesia! ❤️\n\nEste software es desarrollado de forma independiente por biglexj. Si te resulta útil para practicar piano o visualizar MIDI, puedes apoyar el desarrollo continuo mediante transferencias locales (Yape / Plin / Perú) o plataformas internacionales:",
                        fontSize = 11.sp,
                        color = colors.onSurface,
                        lineHeight = 16.sp
                    )
                }

                // Sección de Actualizaciones
                UpdateCheckerSection(
                    updateState = updateState,
                    onCheckUpdates = {
                        updateState = UpdateCheckState.Checking
                        scope.launch {
                            val result = UpdateChecker.checkForUpdates()
                            updateState = UpdateCheckState.Done(result)
                        }
                    },
                    onOpenRelease = { url ->
                        runCatching { uriHandler.openUri(url) }
                    }
                )

                // Donation Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElyButton(
                        text = "💳 Donación Directa (Yape / Plin / Web)",
                        onClick = { runCatching { uriHandler.openUri("https://www.biglexj.com/donaciones") } },
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        fontSize = 11.sp,
                        height = 38.dp,
                        cornerRadius = 19.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElyButton(
                            text = "☕ Buy Coffee",
                            onClick = { runCatching { uriHandler.openUri("https://buymeacoffee.com/biglexj") } },
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer,
                            fontSize = 10.sp,
                            height = 34.dp,
                            cornerRadius = 17.dp,
                            modifier = Modifier.weight(1f)
                        )

                        ElyButton(
                            text = "🐙 GitHub Autor",
                            onClick = { runCatching { uriHandler.openUri("https://github.com/biglexj") } },
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            height = 34.dp,
                            cornerRadius = 17.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class Done(val result: UpdateResult) : UpdateCheckState()
}

@Composable
private fun UpdateCheckerSection(
    updateState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🔄 Actualizaciones",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        when (updateState) {
            is UpdateCheckState.Idle -> {
                ElyButton(
                    text = "Buscar actualizaciones",
                    onClick = onCheckUpdates,
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    fontSize = 11.sp,
                    height = 34.dp,
                    cornerRadius = 17.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UpdateCheckState.Checking -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary
                    )
                    Text(
                        text = "Consultando GitHub Releases...",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            is UpdateCheckState.Done -> {
                val result = updateState.result
                if (result.error != null) {
                    Text(
                        text = "⚠️ Sin conexión o error: ${result.error}",
                        fontSize = 10.sp,
                        color = colors.error
                    )
                    ElyButton(
                        text = "Reintentar",
                        onClick = onCheckUpdates,
                        containerColor = colors.errorContainer,
                        contentColor = colors.onErrorContainer,
                        fontSize = 10.sp,
                        height = 30.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (result.isUpdateAvailable) {
                    Text(
                        text = "🎉 Nueva versión disponible: v${result.latestVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    if (result.releaseNotes.isNotBlank()) {
                        Text(
                            text = result.releaseNotes,
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                    ElyButton(
                        text = "📥 Descargar v${result.latestVersion}",
                        onClick = { onOpenRelease(result.releaseUrl) },
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        fontSize = 11.sp,
                        height = 34.dp,
                        cornerRadius = 17.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "✅ Tienes la versión más reciente (v${UpdateChecker.currentVersion})",
                        fontSize = 11.sp,
                        color = colors.onSurface
                    )
                }
            }
        }
    }
}

