package com.porter.core.common

/**
 * Sealed class for all application-level errors.
 *
 * Per frontend.md §8: every HTTP/network/WS error maps to a user-safe message
 * with a [recovery] action. Never expose raw exception messages to the user.
 */
sealed class AppError {
    abstract val userMessage: String
    abstract val recovery: RecoveryAction

    // ── Network ───────────────────────────────────────────────────────────────

    /** Network error with message and action label. */
    data class Network(
        override val userMessage: String = "Network error. Please check your connection.",
        val recoveryAction: RecoveryAction = RecoveryAction.RETRY,
    ) : AppError() {
        constructor(userMessage: String, recovery: String) : this(
            userMessage = userMessage,
            recoveryAction = RecoveryAction.fromString(recovery),
        )
        override val recovery: RecoveryAction get() = recoveryAction
        val message: String get() = userMessage
        val action: String get() = recoveryAction.name

        companion object {
            operator fun invoke(userMessage: String, recovery: String = "Retry"): Network =
                Network(userMessage, RecoveryAction.fromString(recovery))
        }
    }

    /** Server error with status code, message, and action label. */
    data class Server(
        val code: Int = 500,
        override val userMessage: String = "Something went wrong on our end. Please try again.",
        val recoveryAction: RecoveryAction = if (code == 404) RecoveryAction.GO_BACK else RecoveryAction.RETRY,
    ) : AppError() {
        constructor(code: Int, userMessage: String, recovery: String) : this(
            code = code,
            userMessage = userMessage,
            recoveryAction = RecoveryAction.fromString(recovery),
        )
        override val recovery: RecoveryAction get() = recoveryAction
        val message: String get() = userMessage
        val action: String get() = recoveryAction.name

        companion object {
            operator fun invoke(code: Int = 500, userMessage: String = "Something went wrong on our end. Please try again.", recovery: String = "Retry"): Server =
                Server(code, userMessage, RecoveryAction.fromString(recovery))
        }
    }

    /** Device has no connectivity. */
    data object NetworkUnavailable : AppError() {
        override val userMessage = "You're offline. Please check your connection."
        override val recovery = RecoveryAction.RETRY
    }

    /** Server returned 5xx. */
    data class ServerError(val code: Int, val detail: String? = null) : AppError() {
        override val userMessage = "Something went wrong on our end. Please try again."
        override val recovery = RecoveryAction.RETRY
    }

    /** Server returned 4xx (generic). */
    data class ClientError(val code: Int, val detail: String) : AppError() {
        override val userMessage: String get() = detail
        override val recovery = RecoveryAction.DISMISS
    }

    /** 401 — access token expired/invalid. */
    data object Unauthorized : AppError() {
        override val userMessage = "Your session has expired. Please log in again."
        override val recovery = RecoveryAction.RELOGIN
    }

    /** 403 — user does not have permission. */
    data object Forbidden : AppError() {
        override val userMessage = "You don't have permission to do this."
        override val recovery = RecoveryAction.DISMISS
    }

    /** 404 — resource not found. */
    data object NotFound : AppError() {
        override val userMessage = "We couldn't find what you're looking for."
        override val recovery = RecoveryAction.GO_BACK
    }

    /** 409 — conflict (e.g., quote expired, booking duplicate). */
    data class Conflict(val detail: String) : AppError() {
        override val userMessage: String get() = detail
        override val recovery = RecoveryAction.DISMISS
    }

    /** 422 — validation failed. */
    data class Validation(
        val field: String? = null,
        val message: String = "",
        val rule: String? = null,
        val fields: Map<String, String> = if (field != null) mapOf(field to (if (message.isNotBlank()) message else (rule ?: ""))) else emptyMap(),
    ) : AppError() {
        constructor(fields: Map<String, String>) : this(
            field = null,
            message = fields.values.firstOrNull() ?: "",
            rule = null,
            fields = fields
        )
        constructor(field: String, rule: String, message: String) : this(
            field = field,
            message = message,
            rule = rule,
            fields = mapOf(field to message)
        )

        override val userMessage: String
            get() = when {
                message.isNotBlank() -> message
                !rule.isNullOrBlank() -> rule
                fields.isNotEmpty() -> fields.values.first()
                else -> "Please check the highlighted fields."
            }
        override val recovery = RecoveryAction.DISMISS
    }

    /** Request timed out. */
    data object Timeout : AppError() {
        override val userMessage = "The request timed out. Please try again."
        override val recovery = RecoveryAction.RETRY
    }

    /** Payment-specific failure (non-network). */
    data class PaymentFailed(val reason: String) : AppError() {
        override val userMessage: String get() = "Payment failed: $reason"
        override val recovery = RecoveryAction.RETRY
    }

    /** Unknown / unhandled error. */
    data class Unknown(val throwable: Throwable? = null) : AppError() {
        override val userMessage = "An unexpected error occurred. Please try again."
        override val recovery = RecoveryAction.RETRY
    }
}

enum class RecoveryAction {
    /** Show a retry button — re-execute the last action. */
    RETRY,
    /** Dismiss the error — no action needed. */
    DISMISS,
    /** Navigate back — e.g., 404. */
    GO_BACK,
    /** Clear session and navigate to login. */
    RELOGIN;

    companion object {
        fun fromString(str: String?): RecoveryAction {
            return when (str?.lowercase()?.trim()) {
                "retry", "try again", "try again later", "check sms and try again" -> RETRY
                "go back", "goback", "back" -> GO_BACK
                "relogin", "login", "log in" -> RELOGIN
                else -> RETRY
            }
        }
    }
}
