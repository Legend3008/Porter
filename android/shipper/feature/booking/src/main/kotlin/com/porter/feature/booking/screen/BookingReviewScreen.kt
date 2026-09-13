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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun BookingReviewScreen(
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val confirmState by viewModel.confirmState.collectAsStateWithLifecycle()
    var agreedToTerms by remember { mutableStateOf(true) }

    LaunchedEffect(confirmState) {
        if (confirmState is UiState.Success) {
            val bookingId = (confirmState as UiState.Success<String>).data
            onConfirm(bookingId)
        }
    }

    val isConfirming = confirmState is UiState.Loading

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Review & Confirm Booking",
                onNavigateBack = onBack
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CanvasWhite)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Shipment Summary",
                    style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
                )

                Text(
                    text = "Verify your dispatch order details before finalizing carrier assignment.",
                    style = BodyDefault.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(4.dp))

                PorterCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReviewItem("Container Type", draft.containerType.label)
                        ReviewItem("Commodity", draft.cargoDescription.ifBlank { "Engineering Machinery" })
                        val weightKg = if (draft.cargoWeightKg > 0) draft.cargoWeightKg else 18500.0
                        ReviewItem("Gross Weight", "${weightKg.toInt()} kg")

                        Divider(color = Hairline)

                        ReviewItem("Pickup Terminal", "${draft.pickupAddress.line1}, ${draft.pickupAddress.city}")
                        ReviewItem("Delivery Point", "${draft.deliveryAddress.line1}, ${draft.deliveryAddress.city}")

                        Divider(color = Hairline)

                        val totalInr = (draft.quote?.totalFarePaise ?: 4543000L) / 100
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Final Total (incl. GST)", style = BodyStrong.copy(color = InkNearBlack))
                            Text(text = "₹$totalInr", style = BodyStrong.copy(color = ActionBlue, fontSize = 18.sp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = ActionBlue)
                    )
                    Text(
                        text = "I accept the Freight Carriage Agreement and confirm that the cargo contains no unauthorized or prohibited contraband.",
                        style = FinePrint.copy(color = InkNearBlack),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 28.dp)) {
                PorterPrimaryButton(
                    text = "Confirm Booking & Proceed to Pay",
                    onClick = { viewModel.confirmBooking() },
                    loading = isConfirming,
                    enabled = agreedToTerms
                )
            }
        }
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Column {
        Text(text = label, style = FinePrint.copy(color = InkMuted48))
        Text(text = value, style = BodyDefault.copy(color = InkNearBlack))
    }
}
