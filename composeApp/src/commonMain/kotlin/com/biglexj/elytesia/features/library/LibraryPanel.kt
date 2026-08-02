package com.biglexj.elytesia.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.biglexj.elytesia.model.Difficulty
import com.biglexj.elytesia.model.Song
import com.biglexj.elytesia.shared.components.ElyBadge
import com.biglexj.elytesia.shared.components.ElyButton
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.TextContrast
import kotlinx.coroutines.launch

@Composable
fun LibraryPanel(
    songsList: List<Song>,
    selectedSong: Song?,
    onSelectSong: (Song) -> Unit,
    onRequestMidiFile: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    val demoSongs = remember(songsList) { songsList.filter { it.isDemo } }
    val importedSongs = remember(songsList) { songsList.filter { !it.isDemo } }

    val filteredDemo = remember(demoSongs, searchQuery, selectedDifficulty) {
        demoSongs.filter { song ->
            val matchesQuery = song.name.contains(searchQuery, ignoreCase = true)
            val matchesDiff = selectedDifficulty == null || song.difficulty == selectedDifficulty
            matchesQuery && matchesDiff
        }
    }
    val filteredImported = remember(importedSongs, searchQuery, selectedDifficulty) {
        importedSongs.filter { song ->
            val matchesQuery = song.name.contains(searchQuery, ignoreCase = true)
            val matchesDiff = selectedDifficulty == null || song.difficulty == selectedDifficulty
            matchesQuery && matchesDiff
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Biblioteca de Canciones",
                color = colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (onRequestMidiFile != null) {
                ElyButton(
                    text = "📥 Importar MIDI",
                    onClick = onRequestMidiFile,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    height = 32.dp,
                    fontSize = 11.sp,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar canción...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Difficulty filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChipItem(
                label = "Todas",
                isSelected = selectedDifficulty == null,
                onClick = { selectedDifficulty = null }
            )
            Difficulty.entries.forEach { diff ->
                FilterChipItem(
                    label = diff.name,
                    isSelected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = diff }
                )
            }
        }



        // Song catalog list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredDemo, key = { "demo_${it.name}" }) { song ->
                SongCardItem(
                    song = song,
                    isSelected = selectedSong?.name == song.name,
                    onClick = { onSelectSong(song) }
                )
            }
            if (filteredImported.isNotEmpty()) {
                item {
                    Text(
                        text = "📂 Importadas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(filteredImported, key = { "import_${it.name}" }) { song ->
                    SongCardItem(
                        song = song,
                        isSelected = selectedSong?.name == song.name,
                        onClick = { onSelectSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isSelected) AuroraViolet else colors.surface
    val content = if (isSelected) colors.surface else colors.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = label, color = content, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SongCardItem(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isSelected) AuroraViolet.copy(alpha = 0.15f) else colors.surface
    val border = if (isSelected) AuroraViolet else TextContrast.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${song.notes.size} notas • ${formatMs(song.durationMs)}",
                    color = TextContrast,
                    fontSize = 11.sp
                )
            }

            val diffColor = androidx.compose.ui.graphics.Color(song.difficulty.colorHex)
            ElyBadge(
                text = song.difficulty.displayName,
                containerColor = diffColor.copy(alpha = 0.2f),
                contentColor = diffColor
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
