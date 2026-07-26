package com.biglexj.elytesia.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ElyCard(
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .padding(contentPadding),
        content = content
    )
}
