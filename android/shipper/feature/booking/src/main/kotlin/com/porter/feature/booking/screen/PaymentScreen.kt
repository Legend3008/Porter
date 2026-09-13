package com.porter.feature.booking.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.porter.core.designsystem.theme.ShapeLg
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.PaymentMethod
import com.porter.feature.booking.viewmodel.BookingFlowViewModel

@Composable
fun PaymentScreen(
    bookingId: String,
    onPaymentInitiated: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val totalInr = (draft.quote?.totalFarePaise ?: 4543000L) / 100

    val methods = listOf(
        Triple(PaymentMethod.UPI, "UPI (Google Pay, PhonePe, Paytm, BHIM)", Icons.Default.QrCode),
        Triple(PaymentMethod.NET_BANKING, "Corporate Net Banking (SBI, HDFC, ICICI, Axis)", Icons.Default.AccountBalance),
        Triple(PaymentMethod.CARD, "Commercial Credit / Debit Card", Icons.Default.CreditCard),
        Triple(PaymentMethod.NEFT_RTGS, "NEFT / RTGS / Virtual Account Transfer", Icons.Default.AccountBalance)
    )

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Payment",
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
                    text = "Select Payment Method",
                    style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
                )

                Text(
                    text = "Shipment #$bookingId • Total Amount: ₹$totalInr (inclusive of 18% GST)",
                    style = BodyStrong.copy(color = ActionBlue)
                )

                Spacer(modifier = Modifier.height(8.dp))

                methods.forEach { (method, label, icon) ->
                    val isSelected = draft.selectedPaymentMethod == method
                    val borderColor = if (isSelected) ActionBlue else Hairline
                    val bgColor = if (isSelected) Parchment else CanvasWhite

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, ShapeLg)
                            .background(bgColor, ShapeLg)
                            .clickable { viewModel.selectPaymentMethod(method) }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) ActionBlue else InkMuted48,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = label,
                                    style = BodyDefault.copy(
                                        color = if (isSelected) InkNearBlack else InkMuted48
                                    )
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectPaymentMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = ActionBlue)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                PorterPrimaryButton(
                    text = "Pay ₹$totalInr Securely",
                    onClick = onPaymentInitiated
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Payments are 256-bit encrypted and processed via RBI-authorized payment gateways.",
                    style = FinePrint.copy(color = InkMuted48),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
