package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.shared.components.ElyBadge
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.ElyThemeDefinition
import com.biglexj.elytesia.theme.TextContrast

@Composable
fun TopHeaderControlBar(
    loadedSong: Song?,
    currentTimeMs: Long,
    activeTheme: ElyThemeDefinition,
    isSidebarOpen: Boolean,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Toggle Sidebar / Menu Button
        com.biglexj.elytesia.shared.components.ElyButton(
            text = if (isSidebarOpen) "✕ Cerrar Menú" else "☰ Menú",
            onClick = onToggleSidebar,
            containerColor = if (isSidebarOpen) colors.surfaceVariant.copy(alpha = 0.5f) else colors.primary,
            contentColor = if (isSidebarOpen) colors.onSurfaceVariant else colors.onPrimary,
            fontSize = 11.sp,
            height = 36.dp,
            cornerRadius = 18.dp
        )

        // Song title and details
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = loadedSong?.name ?: "Sin canción cargada",
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val durationMs = loadedSong?.durationMs ?: 0L
            val currentFormatted = formatMs(currentTimeMs)
            val totalFormatted = formatMs(durationMs)
            Text(
                text = "$currentFormatted / $totalFormatted",
                color = TextContrast,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            loadedSong?.difficulty?.let { diff ->
                ElyBadge(
                    text = diff.name,
                    containerColor = ElyGreen.copy(alpha = 0.2f),
                    contentColor = ElyGreen
                )
            }

            ElyBadge(
                text = activeTheme.name,
                containerColor = AuroraViolet.copy(alpha = 0.2f),
                contentColor = AuroraViolet
            )
        }
    }
}

private fun formatMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
