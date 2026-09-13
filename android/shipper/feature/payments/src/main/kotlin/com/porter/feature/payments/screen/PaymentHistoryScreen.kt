package com.porter.feature.payments.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
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
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.designsystem.theme.StatusWarning
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.Payment
import com.porter.domain.model.PaymentStatus
import com.porter.feature.payments.viewmodel.PaymentsViewModel

@Composable
fun PaymentHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.paymentsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadPaymentHistory()
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Payment History",
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
                onRetry = { viewModel.loadPaymentHistory() },
                modifier = Modifier.padding(padding)
            )
            is UiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = InkMuted48,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No payment records found", style = BodyStrong.copy(color = InkNearBlack))
                        Text(text = "Completed transaction logs will appear here.", style = Caption.copy(color = InkMuted48))
                    }
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(CanvasWhite)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Transaction History",
                            style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
                        )
                        Text(
                            text = "Authoritative payment records verified against banking webhooks.",
                            style = BodyDefault.copy(color = InkMuted48)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.data, key = { it.id }) { payment ->
                        PaymentHistoryItemCard(payment = payment)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PaymentHistoryItemCard(payment: Payment) {
    PorterCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: ${payment.bookingId}",
                    style = BodyStrong.copy(color = InkNearBlack)
                )
                val (label, color) = when (payment.status) {
                    PaymentStatus.SUCCESS -> Pair("SUCCESS", StatusSuccess)
                    PaymentStatus.FAILED -> Pair("FAILED", StatusError)
                    PaymentStatus.PROCESSING -> Pair("PROCESSING", ActionBlue)
                    PaymentStatus.PENDING -> Pair("PENDING", StatusWarning)
                    else -> Pair(payment.status.name, InkMuted48)
                }
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.12f), ShapePill)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = label, style = FinePrint.copy(color = color))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gateway Order: ${payment.orderId}",
                style = Caption.copy(color = InkMuted48)
            )

            if (payment.gatewayReference != null) {
                Text(
                    text = "Bank Txn ID: ${payment.gatewayReference}",
                    style = FinePrint.copy(color = InkMuted48)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payment.method?.name ?: "ONLINE",
                    style = FinePrint.copy(color = InkMuted48)
                )
                val inr = payment.amountPaise / 100
                Text(
                    text = "₹$inr",
                    style = BodyStrong.copy(fontSize = 18.sp, color = ActionBlue)
                )
            }
        }
    }
}
