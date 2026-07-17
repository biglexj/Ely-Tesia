package com.biglexj.elytesia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biglexj.elytesia.theme.AuroraViolet
import com.biglexj.elytesia.theme.BorderGray
import com.biglexj.elytesia.theme.DarkGrayBg
import com.biglexj.elytesia.theme.ElyGreen
import com.biglexj.elytesia.theme.ElyPink
import com.biglexj.elytesia.theme.TextMain
import com.biglexj.elytesia.theme.LocalElyMusicTheme

@Composable
internal fun PlaybackProgressBar(
    currentTimeMs: Long,
    durationMs: Long,
    onPreviewSeek: (Long) -> Unit,
    onSeekCommitted: () -> Unit,
    showBottomAccent: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val music = LocalElyMusicTheme.current
    val actualProgress = if (durationMs > 0L) {
        currentTimeMs.toFloat().div(durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(actualProgress) }

    LaunchedEffect(actualProgress, isDragging) {
        if (!isDragging) dragProgress = actualProgress
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .background(colors.background.copy(alpha = 0.9f))
    ) {
        if (showBottomAccent) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(dragProgress)
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(music.leftHand, colors.primary, music.rightHand)))
            )
        }
        Slider(
            value = dragProgress,
            onValueChange = { value ->
                isDragging = true
                dragProgress = value
                onPreviewSeek((durationMs * value).toLong())
            },
            onValueChangeFinished = {
                isDragging = false
                onSeekCommitted()
            },
            enabled = durationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = music.rightHand,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatPlaybackTime((durationMs * dragProgress).toLong())} / ${formatPlaybackTime(durationMs)}",
                color = colors.onBackground,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(colors.background.copy(alpha = 0.82f)).padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

internal fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1000L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "${totalSeconds / 60L}:$seconds"
}
