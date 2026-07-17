package com.biglexj.elytesia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.theme.ElyThemeDefinition
import com.biglexj.elytesia.theme.toComposeColor
import com.biglexj.elytesia.theme.LocalElyThemeEffects
import com.biglexj.elytesia.theme.isPlatformDynamicColorAvailable

@Composable
internal fun ThemeManagerPanel(
    themes: List<ElyThemeDefinition>,
    selectedThemeId: String,
    useDynamicColor: Boolean,
    canImport: Boolean,
    canExport: Boolean,
    onSelect: (String) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onImport: () -> Unit,
    onExport: (ElyThemeDefinition) -> Unit,
    onDelete: (ElyThemeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val expressiveMotion = LocalElyThemeEffects.current.expressiveMotion
    val dynamicColorAvailable = isPlatformDynamicColorAvailable()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (dynamicColorAvailable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Color dinámico Android",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Usa el wallpaper para la interfaz",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = useDynamicColor, onCheckedChange = onToggleDynamicColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onImport,
                enabled = canImport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Importar", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { themes.firstOrNull { it.id == selectedThemeId }?.let(onExport) },
                enabled = canExport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Exportar", fontSize = 11.sp) }
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(themes, key = ElyThemeDefinition::id) { theme ->
                val selected = theme.id == selectedThemeId
                val targetContainer = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                val targetBorder = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                val targetContent = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                val containerColor = if (expressiveMotion) animateColorAsState(targetContainer, spring()).value else targetContainer
                val borderColor = if (expressiveMotion) animateColorAsState(targetBorder, spring()).value else targetBorder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            borderColor,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(theme.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = targetContent
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(theme.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = targetContent)
                                Text("${theme.author} · ${theme.license}", fontSize = 9.sp, color = targetContent.copy(alpha = 0.78f))
                            }
                            if (!theme.builtIn) {
                                TextButton(
                                    onClick = { onDelete(theme) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = targetContent)
                                ) { Text("Eliminar", fontSize = 9.sp) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(
                                theme.material.primary, theme.material.secondary, theme.music.leftHand,
                                theme.music.rightHand, theme.music.blackKeyPressed, theme.music.wrongNote
                            ).forEach { hex ->
                                Box(
                                    Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                                        .background(hex.toComposeColor())
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
                                )
                            }
                        }
                        if (theme.description.isNotBlank()) {
                            Text(theme.description, fontSize = 10.sp, maxLines = 2, color = targetContent)
                        }
                    }
                }
            }
        }
    }
}
