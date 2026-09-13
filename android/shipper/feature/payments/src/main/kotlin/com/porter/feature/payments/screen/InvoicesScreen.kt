package com.porter.feature.payments.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.designsystem.theme.StatusWarning
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.Invoice
import com.porter.domain.model.InvoiceStatus
import com.porter.feature.payments.viewmodel.PaymentsViewModel

@Composable
fun InvoicesScreen(
    onOpenInvoice: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.invoicesState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInvoices()
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Tax Invoices",
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
                onRetry = { viewModel.loadInvoices() },
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
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = InkMuted48,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No invoices issued yet", style = BodyStrong.copy(color = InkNearBlack))
                        Text(text = "Invoices will be generated upon shipment booking.", style = Caption.copy(color = InkMuted48))
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
                            text = "GST Tax Invoices",
                            style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
                        )
                        Text(
                            text = "Download official GST tax invoices for claiming Input Tax Credit (ITC).",
                            style = BodyDefault.copy(color = InkMuted48)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.data, key = { it.id }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            onClick = { onOpenInvoice(invoice.id) }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun InvoiceItemCard(
    invoice: Invoice,
    onClick: () -> Unit
) {
    PorterCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = BodyStrong.copy(color = InkNearBlack)
                )
                val (statusLabel, statusColor) = when (invoice.status) {
                    InvoiceStatus.PAID -> Pair("PAID", StatusSuccess)
                    InvoiceStatus.ISSUED -> Pair("ISSUED", ActionBlue)
                    InvoiceStatus.OVERDUE -> Pair("OVERDUE", StatusWarning)
                    else -> Pair(invoice.status.name, InkMuted48)
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), ShapePill)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = statusLabel, style = FinePrint.copy(color = statusColor))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Booking Ref: ${invoice.bookingId}",
                style = Caption.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val gstInr = invoice.gstPaise / 100
                Text(
                    text = "GST (18%): ₹$gstInr",
                    style = FinePrint.copy(color = InkMuted48)
                )
                val totalInr = invoice.totalPaise / 100
                Text(
                    text = "₹$totalInr",
                    style = BodyStrong.copy(fontSize = 18.sp, color = ActionBlue)
                )
            }
        }
    }
}
