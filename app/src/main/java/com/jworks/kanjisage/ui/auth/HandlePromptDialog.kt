package com.jworks.kanjisage.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.KanjiSageTypography
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

@Composable
fun HandlePromptDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var handle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        val trimmed = handle.trim()
        return when {
            trimmed.length < 3 -> {
                errorMessage = "At least 3 characters"
                false
            }
            trimmed.length > 20 -> {
                errorMessage = "20 characters max"
                false
            }
            !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                errorMessage = "Letters, numbers, and underscores only"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    KanjiSageShapes.Large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.handle_prompt_title),
                fontSize = KanjiSageTypography.TitleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.handle_prompt_subtitle),
                fontSize = KanjiSageTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = {
                    handle = it.take(20)
                    errorMessage = null
                },
                label = { Text("Display name") },
                placeholder = { Text("e.g. kanji_master_42") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (validate()) onSave(handle.trim())
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = KanjiSageShapes.Card
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Maybe later",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (validate()) onSave(handle.trim()) },
                    enabled = handle.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KanjiSageColors.LinkBlue
                    ),
                    shape = KanjiSageShapes.Card
                ) {
                    Text("Save")
                }
            }
        }
    }
}
