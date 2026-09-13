package com.porter.core.common

/**
 * Sealed class representing every possible UI state for a screen.
 *
 * Per frontend.md §7: all screens must handle all 7 states explicitly.
 * Never collapse Offline into Error — the user needs different recovery actions.
 */
sealed class UiState<out T> {
    /** Screen has not yet started loading. Initial render frame. */
    data object Initial : UiState<Nothing>()

    /** Data is being fetched. Show skeleton or spinner. */
    data object Loading : UiState<Nothing>()

    /** Data loaded successfully. */
    data class Success<T>(val data: T) : UiState<T>()

    /** Fetch succeeded but returned zero items. Show empty-state illustration. */
    data object Empty : UiState<Nothing>()

    /** An error occurred. Show message + retry action. */
    data class Error(val error: AppError) : UiState<Nothing>()

    /**
     * Device has no network connectivity.
     * Must never be conflated with [Error] — recovery action is different (check connection).
     */
    data object Offline : UiState<Nothing>()

    /**
     * The auth session is expired or invalid.
     * ViewModel must emit this to trigger auth re-flow — never redirect silently.
     */
    data object Unauthorized : UiState<Nothing>()
}

/** Convenience: is this state terminal (no more loading)? */
val <T> UiState<T>.isTerminal: Boolean
    get() = this !is UiState.Loading && this !is UiState.Initial

/** Convenience: extract data or null */
val <T> UiState<T>.dataOrNull: T?
    get() = if (this is UiState.Success) data else null
