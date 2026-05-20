package com.jworks.kanjisage.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(16.dp),
    fillColor: Color = KanjiSageColors.GlassSurface,
    borderColor: Color = KanjiSageColors.GlassBorder,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fillColor, shape)
    .border(borderWidth, borderColor, shape)

fun Modifier.glassOverlay(
    shape: Shape = RoundedCornerShape(16.dp),
    fillColor: Color = KanjiSageColors.GlassSurfaceMedium,
    borderColor: Color = KanjiSageColors.GlassBorder,
    blurRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val base = this
        .clip(shape)
        .background(fillColor, shape)
        .border(borderWidth, borderColor, shape)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.blur(blurRadius)
    } else {
        base
    }
}
