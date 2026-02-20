package com.jworks.kanjilens.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.R
import com.jworks.kanjilens.ui.anim.CoinPulseGlow
import com.jworks.kanjilens.ui.theme.KanjiLensColors
import kotlin.math.roundToInt

/**
 * Data class holding all floating button offsets for the 3x3 grid.
 */
data class ButtonOffsets(
    val settings: Offset = Offset.Zero,
    val flash: Offset = Offset.Zero,
    val mode: Offset = Offset.Zero,
    val vertical: Offset = Offset.Zero,
    val pause: Offset = Offset.Zero,
    val profile: Offset = Offset.Zero,
    val feedback: Offset = Offset.Zero,
    val jcoin: Offset = Offset.Zero,
    val bookmark: Offset = Offset.Zero
)

/**
 * Calculate the initial 3x3 button grid positions.
 */
fun calculateButtonGrid(
    maxWidthPx: Float,
    maxHeightPx: Float,
    btnSizePx: Float,
    rightMargin: Float,
    bottomPadding: Float,
    topMargin: Float
): ButtonOffsets {
    val btnGap = CameraDimens.BUTTON_GAP_PX

    val col3 = maxWidthPx - btnSizePx - rightMargin
    val col2 = col3 - btnSizePx - btnGap
    val col1 = col2 - btnSizePx - btnGap

    val row2 = (maxHeightPx - bottomPadding - btnSizePx).coerceAtLeast(topMargin)
    val row1 = (row2 - btnSizePx - btnGap).coerceAtLeast(topMargin)
    val row0 = (row1 - btnSizePx - btnGap).coerceAtLeast(topMargin)

    return ButtonOffsets(
        // Top row: mode, vertical, pause
        mode = Offset(col1, row0),
        vertical = Offset(col2, row0),
        pause = Offset(col3, row0),
        // Middle row: flash, settings, profile
        flash = Offset(col1, row1),
        settings = Offset(col2, row1),
        profile = Offset(col3, row1),
        // Bottom row: bookmark, jcoin, feedback
        bookmark = Offset(col1, row2),
        jcoin = Offset(col2, row2),
        feedback = Offset(col3, row2)
    )
}

/**
 * A floating button that can be dragged to reposition.
 * Short tap triggers onClick; drag moves the button.
 */
@Composable
fun DraggableFloatingButton(
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onClick: () -> Unit,
    maxWidth: Float,
    maxHeight: Float,
    btnSize: Float,
    bgColor: Color = Color.Black.copy(alpha = 0.4f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(CameraDimens.BUTTON_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val firstDown = down.changes.firstOrNull() ?: continue
                        if (!firstDown.pressed) continue
                        firstDown.consume()

                        var totalDrag = Offset.Zero
                        var wasDragged = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                if (!wasDragged) onClick()
                                break
                            }
                            val delta = change.positionChange()
                            totalDrag += delta
                            if (totalDrag.getDistance() > viewConfiguration.touchSlop) {
                                wasDragged = true
                            }
                            if (wasDragged) {
                                onOffsetChange(
                                    Offset(
                                        (offset.x + delta.x).coerceIn(0f, maxWidth - btnSize),
                                        (offset.y + delta.y).coerceIn(0f, maxHeight - btnSize)
                                    )
                                )
                            }
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// --- Individual button content composables ---

@Composable
fun SettingsButtonContent() {
    Icon(
        painter = painterResource(id = R.drawable.ic_settings),
        contentDescription = "Settings",
        tint = Color.White,
        modifier = Modifier.size(CameraDimens.ICON_SIZE)
    )
}

@Composable
fun FlashButtonContent(isFlashOn: Boolean) {
    Icon(
        painter = painterResource(
            id = if (isFlashOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
        ),
        contentDescription = if (isFlashOn) "Flash On" else "Flash Off",
        tint = if (isFlashOn) Color.Yellow else Color.White,
        modifier = Modifier.size(CameraDimens.ICON_SIZE)
    )
}

@Composable
fun ModeButtonContent(isFullMode: Boolean) {
    Text(
        if (isFullMode) "FULL" else "25%",
        color = Color.White,
        fontSize = 10.sp
    )
}

@Composable
fun VerticalModeButtonContent(isVerticalMode: Boolean) {
    Text(
        if (isVerticalMode) "\u2B07" else "\u27A1",  // ⬇ or ➡
        color = if (isVerticalMode) Color.Yellow else Color.White,
        fontSize = 16.sp
    )
}

@Composable
fun PauseButtonContent(isPaused: Boolean) {
    Text(
        if (isPaused) "▶" else "⏸",
        color = if (isPaused) Color.Yellow else Color.White,
        fontSize = 14.sp
    )
}

@Composable
fun ProfileButtonContent() {
    Icon(
        painter = painterResource(id = R.drawable.ic_person),
        contentDescription = "Profile",
        tint = Color.White,
        modifier = Modifier.size(CameraDimens.ICON_SIZE)
    )
}

@Composable
fun JCoinButtonContent() {
    Box(contentAlignment = Alignment.Center) {
        // Subtle pulsing glow behind the coin
        CoinPulseGlow(modifier = Modifier.size(CameraDimens.BUTTON_SIZE))
        // Golden coin circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KanjiLensColors.CoinShine,
                            KanjiLensColors.CoinGold,
                            KanjiLensColors.CoinAccent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "J",
                color = Color(0xFF5D4037),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun FeedbackButtonContent() {
    Icon(
        painter = painterResource(id = R.drawable.ic_email),
        contentDescription = "Send Feedback",
        tint = Color.White,
        modifier = Modifier.size(CameraDimens.ICON_SIZE)
    )
}

@Composable
fun BookmarkButtonContent() {
    Icon(
        painter = painterResource(id = R.drawable.ic_bookmark_filled),
        contentDescription = "Bookmarks",
        tint = Color.White,
        modifier = Modifier.size(CameraDimens.ICON_SIZE)
    )
}
