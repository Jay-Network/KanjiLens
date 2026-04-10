package com.jworks.kanjisage.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a visible focus border to any interactive element for WCAG compliance.
 * Shows a 2dp primary-color border when the element receives focus
 * (keyboard, d-pad, TalkBack navigation).
 *
 * Place BEFORE .clickable {} in the modifier chain so the border wraps the element.
 */
fun Modifier.focusBorder(
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 2.dp
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) Modifier.border(borderWidth, color, shape)
            else Modifier
        )
        .focusable()
}
