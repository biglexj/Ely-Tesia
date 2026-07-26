package com.biglexj.elytesia.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ElyBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    fontSize: TextUnit = 10.sp,
    paddingHorizontal: Dp = 8.dp,
    paddingVertical: Dp = 3.dp
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .background(containerColor, shape)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}
