package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterSecondaryButton

@Composable
fun BookingConfirmationScreen(
    bookingId: String,
    onTrack: (String) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val simulatedTripId = "TRIP-8842-JNPT"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            // 1. Did it work? (Peak-End Rule Triumphant Affirmation)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(StatusSuccess.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(StatusSuccess, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CanvasWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Shipment Confirmed & Locked",
                style = HeroDisplay.copy(fontSize = 26.sp, color = InkNearBlack),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your container transport dispatch order is confirmed. Carrier chassis trailer has been assigned.",
                style = BodyDefault.copy(color = InkMuted48),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. What happened? (Order Reference & Details)
            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Booking Reference", style = FinePrint.copy(color = InkMuted48))
                        Text(text = bookingId, style = BodyStrong.copy(color = InkNearBlack))
                    }
                    HorizontalDivider(color = Hairline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Assigned Trip ID", style = FinePrint.copy(color = InkMuted48))
                        Text(text = simulatedTripId, style = BodyStrong.copy(color = ActionBlue))
                    }
                    HorizontalDivider(color = Hairline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Carrier Status", style = FinePrint.copy(color = InkMuted48))
                        Text(text = "DRIVER DISPATCHED", style = BodyStrong.copy(color = StatusSuccess))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. What happens next? (Chronological Next-Steps Timeline)
            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "WHAT HAPPENS NEXT",
                        style = FinePrint.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = ActionBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NextStepItem(
                        stepNumber = "1",
                        title = "Dispatch Order Confirmed",
                        detail = "Driver assigned & vehicle pre-trip verification done",
                        isCompleted = true,
                        isCurrent = false
                    )
                    NextStepItem(
                        stepNumber = "2",
                        title = "Container Pickup at Port Terminal",
                        detail = "Trailer en route to JNPT Gate 4A (Est: 25 mins)",
                        isCompleted = false,
                        isCurrent = true
                    )
                    NextStepItem(
                        stepNumber = "3",
                        title = "Highway Transit & Live Telemetry",
                        detail = "Continuous GPS tracking & tamper-seal sensor active",
                        isCompleted = false,
                        isCurrent = false
                    )
                    NextStepItem(
                        stepNumber = "4",
                        title = "Delivery & Digital Proof of Delivery",
                        detail = "Consignee OTP verification & instant e-POD generation",
                        isCompleted = false,
                        isCurrent = false,
                        isLast = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Is there anything I need to do right now? (Explicit Reassurance)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Parchment, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "No immediate action required",
                        style = BodyStrong.copy(fontSize = 14.sp, color = InkNearBlack)
                    )
                    Text(
                        text = "We will send real-time SMS & push alerts when the container is loaded and clears the terminal outbound gate.",
                        style = FinePrint.copy(color = InkMuted48)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PorterPrimaryButton(
                text = "Track Container Live",
                onClick = { onTrack(simulatedTripId) }
            )

            PorterSecondaryButton(
                text = "Return to Dashboard",
                onClick = onGoHome
            )
        }
    }
}

@Composable
private fun NextStepItem(
    stepNumber: String,
    title: String,
    detail: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(ActionBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CanvasWhite, CircleShape)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = InkMuted48.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (isCompleted) StatusSuccess.copy(alpha = 0.5f) else Hairline)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(
                text = title,
                style = BodyStrong.copy(
                    fontSize = 14.sp,
                    color = if (isCurrent) ActionBlue else InkNearBlack
                )
            )
            Text(
                text = detail,
                style = FinePrint.copy(color = InkMuted48)
            )
        }
    }
}
