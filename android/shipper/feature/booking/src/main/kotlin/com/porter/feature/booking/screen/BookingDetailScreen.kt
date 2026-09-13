package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterSecondaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.core.ui.components.StatusBadge
import com.porter.domain.model.Booking
import com.porter.feature.booking.viewmodel.BookingDetailViewModel

@Composable
fun BookingDetailScreen(
    bookingId: String,
    onTrack: (String) -> Unit,
    onTimeline: () -> Unit,
    onDocuments: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookingId) {
        viewModel.loadBooking(bookingId)
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipment #$bookingId",
                onNavigateBack = onBack
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorScreen(
                error = state.error,
                onRetry = { viewModel.loadBooking(bookingId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                BookingDetailContent(
                    booking = state.data,
                    onTrack = onTrack,
                    onTimeline = onTimeline,
                    onDocuments = onDocuments,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun BookingDetailContent(
    booking: Booking,
    onTrack: (String) -> Unit,
    onTimeline: () -> Unit,
    onDocuments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val simulatedTripId = "trip-001"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Status header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = booking.id,
                    style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
                )
                Text(
                    text = "Container Dispatch Order",
                    style = Caption.copy(color = InkMuted48)
                )
            }
            StatusBadge(status = booking.status)
        }

        // Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PorterPrimaryButton(
                text = "Live GPS Tracking",
                onClick = { onTrack(simulatedTripId) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PorterSecondaryButton(
                    text = "Trip Timeline",
                    onClick = onTimeline,
                    modifier = Modifier.weight(1f)
                )
                PorterSecondaryButton(
                    text = "Documents",
                    onClick = onDocuments,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Route Card
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "ROUTE & DESTINATION", style = FinePrint.copy(color = ActionBlue))

                Column {
                    Text(text = "Origin Port / Yard", style = FinePrint.copy(color = InkMuted48))
                    Text(
                        text = "${booking.shipmentDetails.pickupAddress.line1}, ${booking.shipmentDetails.pickupAddress.city}",
                        style = BodyStrong.copy(color = InkNearBlack)
                    )
                    Text(
                        text = "Contact: ${booking.shipmentDetails.pickupAddress.contactName} (${booking.shipmentDetails.pickupAddress.contactPhone})",
                        style = Caption.copy(color = InkMuted48)
                    )
                }

                Divider(color = Hairline)

                Column {
                    Text(text = "Delivery Destination", style = FinePrint.copy(color = InkMuted48))
                    Text(
                        text = "${booking.shipmentDetails.deliveryAddress.line1}, ${booking.shipmentDetails.deliveryAddress.city}",
                        style = BodyStrong.copy(color = InkNearBlack)
                    )
                    Text(
                        text = "Contact: ${booking.shipmentDetails.deliveryAddress.contactName} (${booking.shipmentDetails.deliveryAddress.contactPhone})",
                        style = Caption.copy(color = InkMuted48)
                    )
                }
            }
        }

        // Container & Cargo Card
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "CONTAINER & CARGO SPECIFICATION", style = FinePrint.copy(color = ActionBlue))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Container Size", style = Caption.copy(color = InkMuted48))
                    Text(text = booking.shipmentDetails.containerType.displayName, style = BodyStrong.copy(color = InkNearBlack))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Commodity", style = Caption.copy(color = InkMuted48))
                    Text(text = booking.shipmentDetails.commodity, style = BodyStrong.copy(color = InkNearBlack))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Cargo Weight", style = Caption.copy(color = InkMuted48))
                    Text(text = "${booking.shipmentDetails.totalWeightKg.toInt()} kg", style = BodyStrong.copy(color = InkNearBlack))
                }
            }
        }

        // Financial Summary
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "FINANCIAL SUMMARY", style = FinePrint.copy(color = ActionBlue))

                val totalInr = booking.totalAmountPaise / 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Paid Amount", style = BodyStrong.copy(color = InkNearBlack))
                    Text(text = "₹$totalInr", style = BodyStrong.copy(color = ActionBlue, fontSize = 18.sp))
                }

                Text(
                    text = "Includes 18% Integrated Goods and Services Tax (IGST). Tax invoice generated.",
                    style = FinePrint.copy(color = InkMuted48)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
