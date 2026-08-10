package com.biglexj.elytesia.update

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.biglexj.elytesia.shared.components.ElyBadge
import com.biglexj.elytesia.shared.components.ElyButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo Central Interactivo de Actualizaciones (`UpdateModalDialog`).
 * Incluye simulador de descarga progresiva (0-100%) y estados de instalación
 * conforme a las directivas de auto_updater.md y github_auto_updater_guide.md.
 */
@Composable
fun UpdateModalDialog(
    updateResult: UpdateResult,
    onDismissRequest: () -> Unit,
    onDownloadRequested: ((String) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloaded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val isLandscapeMobile = maxHeight < 500.dp
            val isDesktop = maxWidth >= 700.dp && !isLandscapeMobile
            val widthFraction = when {
                isDesktop -> 0.32f
                isLandscapeMobile -> 0.60f
                else -> 0.80f
            }
            val maxCustomWidth = if (isDesktop) 380.dp else 400.dp

            val maxCustomHeight = if (!isDesktop && !isLandscapeMobile) maxHeight * 0.70f else maxHeight - 24.dp
            val minCustomHeight = if (isDesktop) 500.dp.coerceAtMost(maxCustomHeight) else androidx.compose.ui.unit.Dp.Unspecified

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .widthIn(max = maxCustomWidth)
                    .heightIn(min = minCustomHeight, max = maxCustomHeight)
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(if (isLandscapeMobile) 14.dp else 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isLandscapeMobile) 10.dp else 14.dp)
                ) {
                    // Badge de Encabezado
                    ElyBadge(
                        text = if (isDownloading) "⏳ DESCARGANDO ACTUALIZACIÓN" else if (isDownloaded) "✅ LISTO PARA INSTALAR" else "🚀 NUEVA VERSIÓN DISPONIBLE",
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )

                    Text(
                        text = "Ely-Tesia v${updateResult.latestVersion}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primary
                    )

                    Text(
                        text = "Versión instalada: v${UpdateChecker.currentVersion}  •  Última versión: v${updateResult.latestVersion}",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    // Notas de Lanzamiento Sanitizadas (Contenedor con desplazamiento)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.45f))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📋 Novedades de la versión:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = if (updateResult.releaseNotes.isNotBlank()) updateResult.releaseNotes else "• Mejoras generales de estabilidad y rendimiento.",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    // Acciones principales y Simulador de Progreso
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Descargando paquete...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = colors.primary,
                                    trackColor = colors.primaryContainer.copy(alpha = 0.4f)
                                )

                                Text(
                                    text = "Simulando flujo de descarga interactiva (0-100%)...",
                                    fontSize = 9.sp,
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (isDownloaded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.primaryContainer)
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎉 ¡Paquete de actualización verificado!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimaryContainer
                                    )
                                }

                                ElyButton(
                                    text = "🚀 Instalar y Reiniciar Ely-Tesia",
                                    onClick = {
                                        onDismissRequest()
                                        onDownloadRequested?.invoke(updateResult.releaseUrl)
                                    },
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary,
                                    fontSize = 12.sp,
                                    height = 42.dp,
                                    cornerRadius = 21.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            ElyButton(
                                text = "📥 Descargar e Instalar v${updateResult.latestVersion}",
                                onClick = {
                                    isDownloading = true
                                    downloadProgress = 0f
                                    scope.launch {
                                        for (step in 1..100) {
                                            delay(30L) // 3 segundos de animación progresiva fluida (0-100%)
                                            downloadProgress = step / 100f
                                        }
                                        isDownloading = false
                                        isDownloaded = true
                                    }
                                },
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                                fontSize = 12.sp,
                                height = 42.dp,
                                cornerRadius = 21.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Recordarme más tarde", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
