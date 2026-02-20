package com.jworks.kanjilens.domain.models

/**
 * Structured error types for KanjiLens.
 * ViewModels expose these via StateFlow for UI to show appropriate messages.
 */
sealed class AppError(val userMessage: String) {
    data class Network(val cause: Throwable? = null) :
        AppError("Network error. Please check your connection.")

    data class Auth(val reason: String) :
        AppError("Authentication error: $reason")

    data class DictionaryLookup(val word: String, val cause: Throwable? = null) :
        AppError("Could not look up \"$word\"")

    data class KanjiInfoLookup(val kanji: String, val cause: Throwable? = null) :
        AppError("Could not load info for \"$kanji\"")

    data class BookmarkFailed(val item: String, val cause: Throwable? = null) :
        AppError("Could not update bookmark for \"$item\"")

    data class CoinEarnFailed(val action: String, val cause: Throwable? = null) :
        AppError("Could not award coins for $action")

    data class FeedbackSubmit(val cause: Throwable? = null) :
        AppError("Could not submit feedback. Please try again.")

    data class CameraInit(val cause: Throwable? = null) :
        AppError("Camera initialization failed.")

    data class Unknown(val cause: Throwable? = null) :
        AppError("Something went wrong. Please try again.")
}
