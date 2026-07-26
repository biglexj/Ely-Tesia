package com.biglexj.elytesia.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Toast flotante global centrado en la parte superior de la pantalla.
 * Se muestra con animaciones de deslizamiento y desvanecimiento y se auto-descarta tras un tiempo determinado.
 */
@Composable
fun ElyToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = 4000L
) {
    val isVisible = message != null

    LaunchedEffect(message) {
        if (message != null) {
            delay(durationMs)
            onDismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            if (message != null) {
                val colors = MaterialTheme.colorScheme
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .shadow(8.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message,
                        color = colors.onPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
