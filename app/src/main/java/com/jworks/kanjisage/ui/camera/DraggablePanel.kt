package com.jworks.kanjisage.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

@Composable
fun DraggablePanel(
    hasContent: Boolean,
    modifier: Modifier = Modifier,
    defaultHeightFraction: Float = 0.45f,
    maxHeightFraction: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val handleHeightPx = with(density) { 28.dp.toPx() }
    val minHeightPx = handleHeightPx
    val maxHeightPx = screenHeightPx * maxHeightFraction
    val defaultHeightPx = if (hasContent) screenHeightPx * defaultHeightFraction else handleHeightPx

    var panelHeightPx by remember { mutableFloatStateOf(defaultHeightPx) }

    // Expand when content arrives
    LaunchedEffect(hasContent) {
        if (hasContent && panelHeightPx < with(density) { 200.dp.toPx() }) {
            panelHeightPx = screenHeightPx * defaultHeightFraction
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { panelHeightPx.toDp() })
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(KanjiSageColors.PanelBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panelHeightPx = (panelHeightPx - dragAmount.y)
                                    .coerceIn(minHeightPx, maxHeightPx)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(KanjiSageShapes.XSmall)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                }

                // Content
                if (panelHeightPx > with(density) { 60.dp.toPx() }) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        content()
                    }
                }
            }
        }
    }
}
