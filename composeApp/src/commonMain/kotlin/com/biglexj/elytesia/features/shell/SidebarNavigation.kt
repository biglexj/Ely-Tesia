package com.biglexj.elytesia.features.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.update.UpdateResult

enum class SidebarMode { BIBLIOTECA, INSTRUMENTOS, CONFIGURACION, TEMAS }

@Composable
fun SidebarNavigation(
    selectedMode: SidebarMode?,
    onModeSelected: (SidebarMode) -> Unit,
    onShowToast: ((String) -> Unit)? = null,
    onShowUpdateModal: ((UpdateResult) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .width(210.dp)
            .fillMaxHeight()
            .background(colors.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App Title Branding M3 Expressive
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "ELY-TESIA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )
                Text(
                    text = "Piano & MIDI Suite",
                    fontSize = 10.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Navegación Principal
        SidebarNavItem(
            label = "Biblioteca MIDI",
            isSelected = selectedMode == SidebarMode.BIBLIOTECA,
            onClick = { onModeSelected(SidebarMode.BIBLIOTECA) }
        )

        SidebarNavItem(
            label = "Instrumentos",
            isSelected = selectedMode == SidebarMode.INSTRUMENTOS,
            onClick = { onModeSelected(SidebarMode.INSTRUMENTOS) }
        )

        SidebarNavItem(
            label = "Configuración",
            isSelected = selectedMode == SidebarMode.CONFIGURACION,
            onClick = { onModeSelected(SidebarMode.CONFIGURACION) }
        )

        SidebarNavItem(
            label = "Temas Visuales",
            isSelected = selectedMode == SidebarMode.TEMAS,
            onClick = { onModeSelected(SidebarMode.TEMAS) }
        )

        Spacer(modifier = Modifier.weight(1f))

        var showAboutDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        if (showAboutDialog) {
            AboutDialog(
                onDismissRequest = { showAboutDialog = false },
                onShowToast = onShowToast,
                onShowUpdateModal = onShowUpdateModal
            )
        }

        // Pie de página de Apoyo y Autor con Badge "Acerca de"
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceVariant.copy(alpha = 0.45f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hecho con ❤️ por biglexj",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Ely-Tesia v1.0.6 (2026)",
                            fontSize = 9.sp,
                            color = colors.onSurfaceVariant
                        )
                    }

                    com.biglexj.elytesia.shared.components.ElyBadge(
                        text = "ℹ️ Acerca de",
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                }

                // Botón principal de Donaciones Directas (Perú / Yape / Plin / Transferencias)
                com.biglexj.elytesia.shared.components.ElyButton(
                    text = "💳 Donación Directa",
                    onClick = { runCatching { uriHandler.openUri("https://www.biglexj.com/donaciones") } },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    fontSize = 10.sp,
                    height = 32.dp,
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.biglexj.elytesia.shared.components.ElyButton(
                        text = "☕ Coffee",
                        onClick = { runCatching { uriHandler.openUri("https://buymeacoffee.com/biglexj") } },
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        fontSize = 9.sp,
                        height = 28.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    com.biglexj.elytesia.shared.components.ElyButton(
                        text = "GitHub",
                        onClick = { runCatching { uriHandler.openUri("https://github.com/biglexj") } },
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.onSurfaceVariant,
                        fontSize = 9.sp,
                        height = 28.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val targetContainer = if (isSelected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.4f)
    val targetContent = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant

    val containerColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = spring(stiffness = 400f)
    )
    val contentColor by animateColorAsState(
        targetValue = targetContent,
        animationSpec = spring(stiffness = 400f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
