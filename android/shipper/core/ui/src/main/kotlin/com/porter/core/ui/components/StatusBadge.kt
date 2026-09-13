package com.porter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.designsystem.theme.StatusNeutral
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.designsystem.theme.StatusWarning
import com.porter.domain.model.BookingStatus

@Composable
fun StatusBadge(status: BookingStatus, modifier: Modifier = Modifier) {
    val (label, bg, textColor) = when (status) {
        BookingStatus.CONFIRMED -> Triple("Confirmed", StatusSuccess, CanvasWhite)
        BookingStatus.ASSIGNED -> Triple("Driver Assigned", Color(0xFF0066CC), CanvasWhite)
        BookingStatus.AT_PORT -> Triple("At Port / Yard", Color(0xFF0071E3), CanvasWhite)
        BookingStatus.LOADED -> Triple("Container Loaded", Color(0xFF0071E3), CanvasWhite)
        BookingStatus.IN_TRANSIT -> Triple("In Transit", Color(0xFF0066CC), CanvasWhite)
        BookingStatus.DELIVERED, BookingStatus.COMPLETED -> Triple("Delivered", StatusSuccess.copy(alpha = 0.15f), StatusSuccess)
        BookingStatus.CANCELLED -> Triple("Cancelled", StatusError.copy(alpha = 0.12f), StatusError)
        BookingStatus.PENDING_PAYMENT -> Triple("Pending Payment", StatusWarning.copy(alpha = 0.15f), StatusWarning)
        BookingStatus.PAYMENT_PROCESSING -> Triple("Payment Processing", StatusWarning.copy(alpha = 0.15f), StatusWarning)
        BookingStatus.PAYMENT_FAILED -> Triple("Payment Failed", StatusError, CanvasWhite)
        BookingStatus.DRAFT -> Triple("Draft", StatusNeutral.copy(alpha = 0.15f), StatusNeutral)
    }

    Box(
        modifier = modifier
            .background(color = bg, shape = ShapePill)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = Caption, color = textColor)
    }
}

/** Freshness indicator used on the Live Tracking screen. */
enum class FreshnessLevel { LIVE, STALE, DISCONNECTED, OFFLINE, UNKNOWN }

@Composable
fun FreshnessBadge(level: FreshnessLevel, lastUpdatedLabel: String = "", modifier: Modifier = Modifier) {
    val (label, bg) = when (level) {
        FreshnessLevel.LIVE -> Pair("● LIVE", StatusSuccess)
        FreshnessLevel.STALE -> Pair("○ $lastUpdatedLabel", StatusWarning)
        FreshnessLevel.DISCONNECTED -> Pair("⚠ Reconnecting", StatusWarning)
        FreshnessLevel.OFFLINE -> Pair("✕ Offline", StatusError)
        FreshnessLevel.UNKNOWN -> Pair("— Unknown", StatusNeutral)
    }

    Box(
        modifier = modifier
            .background(color = bg.copy(alpha = 0.15f), shape = ShapePill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = Caption, color = bg)
    }
}
