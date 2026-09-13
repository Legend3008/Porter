package com.porter.feature.booking.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterSecondaryButton
import com.porter.domain.model.PaymentStatus
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun PaymentProcessingScreen(
    bookingId: String,
    onPaymentConfirmed: () -> Unit,
    onPaymentFailed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    val paymentState by viewModel.paymentState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.processPayment(bookingId)
    }

    LaunchedEffect(paymentState) {
        if (paymentState is UiState.Success) {
            onPaymentConfirmed()
        }
    }

    val isFailed = paymentState is UiState.Error

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isFailed) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = ActionBlue,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Authorizing Payment...",
                style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Awaiting authoritative confirmation from the banking network. Please do not press back or close the application.",
                style = BodyDefault.copy(color = InkMuted48),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Booking Reference: $bookingId",
                style = Caption.copy(color = ActionBlue)
            )
        } else {
            Text(
                text = "Payment Authorization Failed",
                style = HeroDisplay.copy(fontSize = 24.sp, color = com.porter.core.designsystem.theme.StatusError)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The bank was unable to complete the transaction. Your booking has been saved as a draft.",
                style = BodyDefault.copy(color = InkMuted48),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PorterPrimaryButton(
                text = "Retry Payment",
                onClick = { viewModel.processPayment(bookingId) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PorterSecondaryButton(
                text = "Choose Another Method",
                onClick = onPaymentFailed
            )
        }
    }
}
