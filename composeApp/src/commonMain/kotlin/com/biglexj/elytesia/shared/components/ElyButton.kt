package com.biglexj.elytesia.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón Expresivo ElyButton (Material 3 Expressive).
 * Ofrece transiciones fluidas de color con físicas de muelle y un hover discreto, sutil y redondeado.
 */
@Composable
fun ElyButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    fontSize: TextUnit = 11.sp,
    cornerRadius: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Hover discreto y sutil que armoniza suavemente sin contrastes agresivos
    val effectiveContainer = if (isHovered && enabled) {
        containerColor.copy(alpha = (containerColor.alpha * 1.15f).coerceAtMost(1f))
    } else {
        containerColor
    }

    val animatedContainer by animateColorAsState(
        targetValue = if (enabled) effectiveContainer else containerColor.copy(alpha = 0.4f),
        animationSpec = spring(stiffness = 500f)
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = animatedContainer,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        interactionSource = interactionSource,
        modifier = modifier
            .clip(shape)
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
