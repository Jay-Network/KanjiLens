package com.jworks.kanjisage.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    fillColor: Color = KanjiSageColors.GlassSurface,
    borderColor: Color = KanjiSageColors.GlassBorder,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassSurface(
                shape = shape,
                fillColor = fillColor,
                borderColor = borderColor,
                borderWidth = borderWidth
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    fillColor: Color = KanjiSageColors.GlassSurfaceMedium,
    borderColor: Color = KanjiSageColors.GlassBorder,
    contentPadding: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        fillColor = fillColor,
        borderColor = borderColor,
        contentPadding = contentPadding,
        content = content
    )
}
