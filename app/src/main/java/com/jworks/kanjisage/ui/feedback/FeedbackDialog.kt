package com.jworks.kanjisage.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jworks.kanjisage.domain.models.FeedbackCategory
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.domain.models.FeedbackWithHistory
import com.jworks.kanjisage.ui.theme.KanjiSageShapes

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    viewModel: FeedbackViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = {
            viewModel.closeDialog()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = KanjiSageShapes.Large
                )
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.feedback_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    viewModel.closeDialog()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.feedback_close))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category selection
            Text(
                text = stringResource(R.string.feedback_category),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback text
            OutlinedTextField(
                value = uiState.feedbackText,
                onValueChange = { viewModel.updateFeedbackText(it) },
                label = { Text("Your feedback") },
                placeholder = { Text("Tell us what you think...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 8,
                supportingText = {
                    Text("${uiState.feedbackText.length}/1000")
                },
                isError = uiState.error != null
            )

            // Error/Success message
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (uiState.successMessage != null) {
                Text(
                    text = uiState.successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit button
            Button(
                onClick = { viewModel.submitFeedback() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting && uiState.feedbackText.length >= 10
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.feedback_send))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback history header
            Text(
                text = stringResource(R.string.feedback_previous),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Feedback list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(200.dp)
            ) {
                if (uiState.isLoadingHistory) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                } else if (uiState.feedbackList.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.feedback_none_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(uiState.feedbackList) { item ->
                        FeedbackHistoryItem(item)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackHistoryItem(item: FeedbackWithHistory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = KanjiSageShapes.Medium
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.feedback.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.feedback.status.emoji,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.feedback.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (item.feedback.status.value) {
                        "deployed" -> MaterialTheme.colorScheme.primary
                        "rejected", "cancelled" -> MaterialTheme.colorScheme.error
                        "in_progress", "testing" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.feedback.feedbackText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )

        if (item.feedback.completionNote != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.feedback_note, item.feedback.completionNote!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
