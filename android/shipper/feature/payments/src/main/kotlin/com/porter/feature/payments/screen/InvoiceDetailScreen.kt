package com.porter.feature.payments.screen

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
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.Invoice
import com.porter.feature.payments.viewmodel.PaymentsViewModel

@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.invoiceDetailState.collectAsStateWithLifecycle()

    LaunchedEffect(invoiceId) {
        viewModel.loadInvoiceDetail(invoiceId)
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Tax Invoice",
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
                onRetry = { viewModel.loadInvoiceDetail(invoiceId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                InvoiceContent(
                    invoice = state.data,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun InvoiceContent(invoice: Invoice, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = invoice.invoiceNumber,
                style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
            )
            Text(
                text = "Booking Reference: ${invoice.bookingId} • Status: ${invoice.status.name}",
                style = Caption.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "BILLING PARTICULARS", style = FinePrint.copy(color = ActionBlue))

                    invoice.lineItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.description, style = BodyDefault.copy(color = InkNearBlack))
                                Text(text = "Qty: ${item.quantity}", style = FinePrint.copy(color = InkMuted48))
                            }
                            val itemInr = item.totalPaise / 100
                            Text(text = "₹$itemInr", style = BodyDefault.copy(color = InkNearBlack))
                        }
                    }

                    Divider(color = Hairline)

                    val subtotalInr = invoice.amountPaise / 100
                    val gstInr = invoice.gstPaise / 100
                    val totalInr = invoice.totalPaise / 100

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Taxable Value", style = Caption.copy(color = InkMuted48))
                        Text(text = "₹$subtotalInr", style = Caption.copy(color = InkNearBlack))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Integrated GST (18% IGST)", style = Caption.copy(color = InkMuted48))
                        Text(text = "₹$gstInr", style = Caption.copy(color = InkNearBlack))
                    }

                    Divider(color = Hairline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Invoice Grand Total", style = BodyStrong.copy(color = InkNearBlack, fontSize = 16.sp))
                        Text(text = "₹$totalInr", style = BodyStrong.copy(color = ActionBlue, fontSize = 20.sp))
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 32.dp)) {
            PorterPrimaryButton(
                text = "Download Official PDF Invoice",
                onClick = {}
            )
        }
    }
}
