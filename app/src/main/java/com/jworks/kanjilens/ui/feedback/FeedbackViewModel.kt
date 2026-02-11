package com.jworks.kanjilens.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.AuthState
import com.jworks.kanjilens.domain.models.FeedbackCategory
import com.jworks.kanjilens.domain.models.FeedbackWithHistory
import com.jworks.kanjilens.domain.models.SubmitFeedbackResult
import com.jworks.kanjilens.domain.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackList: List<FeedbackWithHistory> = emptyList(),
    val isDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedCategory: FeedbackCategory = FeedbackCategory.OTHER,
    val feedbackText: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val isLoadingHistory: Boolean = false
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val APP_ID = "kanjilens"
        private const val POLL_INTERVAL_MS = 15_000L
    }

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var lastFeedbackId: Long? = null

    private fun getCachedEmail(): String? {
        val state = authRepository.authState.value
        return if (state is AuthState.SignedIn) state.user.email else null
    }

    fun openDialog() {
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            feedbackText = "",
            selectedCategory = FeedbackCategory.OTHER,
            error = null,
            successMessage = null
        )
        loadFeedbackHistory()
        startPolling()
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(isDialogOpen = false)
        stopPolling()
    }

    fun selectCategory(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateFeedbackText(text: String) {
        _uiState.value = _uiState.value.copy(feedbackText = text)
    }

    fun submitFeedback() {
        val userEmail = getCachedEmail()
        if (userEmail == null) {
            _uiState.value = _uiState.value.copy(error = "Sign in to send feedback")
            return
        }

        val text = _uiState.value.feedbackText.trim()
        if (text.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please provide at least 10 characters")
            return
        }
        if (text.length > 1000) {
            _uiState.value = _uiState.value.copy(error = "Maximum 1000 characters allowed")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val deviceInfo = mapOf(
                "os" to "Android",
                "osVersion" to android.os.Build.VERSION.RELEASE,
                "device" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER
            )

            when (val result = feedbackRepository.submitFeedback(
                email = userEmail,
                appId = APP_ID,
                category = _uiState.value.selectedCategory,
                feedbackText = text,
                deviceInfo = deviceInfo
            )) {
                is SubmitFeedbackResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Thank you for your feedback!",
                        feedbackText = "",
                        selectedCategory = FeedbackCategory.OTHER
                    )
                    loadFeedbackHistory()
                }
                is SubmitFeedbackResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                is SubmitFeedbackResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun loadFeedbackHistory() {
        val userEmail = getCachedEmail() ?: return

        _uiState.value = _uiState.value.copy(isLoadingHistory = true)

        viewModelScope.launch {
            try {
                val feedback = feedbackRepository.getFeedbackUpdates(
                    email = userEmail,
                    appId = APP_ID,
                    sinceId = null
                )

                lastFeedbackId = feedback.maxOfOrNull { it.feedback.id }

                _uiState.value = _uiState.value.copy(
                    feedbackList = feedback.sortedByDescending { it.feedback.createdAt },
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Failed to load feedback history: ${e.message}"
                )
            }
        }
    }

    private fun startPolling() {
        stopPolling()

        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val userEmail = getCachedEmail() ?: continue

                try {
                    val newFeedback = feedbackRepository.getFeedbackUpdates(
                        email = userEmail,
                        appId = APP_ID,
                        sinceId = lastFeedbackId
                    )

                    if (newFeedback.isNotEmpty()) {
                        val updatedList = (_uiState.value.feedbackList + newFeedback)
                            .distinctBy { it.feedback.id }
                            .sortedByDescending { it.feedback.createdAt }

                        lastFeedbackId = updatedList.maxOfOrNull { it.feedback.id }

                        _uiState.value = _uiState.value.copy(feedbackList = updatedList)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
