package com.biglexj.elytesia.features.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.shared.components.ElyBadge
import com.biglexj.elytesia.shared.components.ElyCard
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.ElyPink
import com.biglexj.elytesia.theme.LocalElyMusicTheme
import com.biglexj.elytesia.theme.TextContrast

@Composable
fun PracticeStatsCard(
    correctNotesCount: Int,
    wrongNotesCount: Int,
    currentStreak: Int,
    lastEvaluatedPitch: Int?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val musicTheme = LocalElyMusicTheme.current
    val totalAttempts = correctNotesCount + wrongNotesCount
    val accuracyPercentage = if (totalAttempts > 0) {
        (correctNotesCount.toFloat() / totalAttempts * 100).toInt()
    } else 100

    ElyCard(
        containerColor = colors.surface,
        borderColor = AuroraViolet.copy(alpha = 0.3f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estadísticas de Práctica",
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                ElyBadge(
                    text = "$accuracyPercentage% Precisión",
                    containerColor = musicTheme.correctNote.copy(alpha = 0.2f),
                    contentColor = musicTheme.correctNote
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Aciertos", value = correctNotesCount.toString(), color = musicTheme.correctNote)
                StatItem(label = "Errores", value = wrongNotesCount.toString(), color = musicTheme.wrongNote)
                StatItem(label = "Racha", value = "$currentStreak 🔥", color = AuroraViolet)
                StatItem(
                    label = "Última Nota",
                    value = lastEvaluatedPitch?.let { "MIDI $it" } ?: "-",
                    color = TextContrast
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = TextContrast, fontSize = 10.sp)
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
