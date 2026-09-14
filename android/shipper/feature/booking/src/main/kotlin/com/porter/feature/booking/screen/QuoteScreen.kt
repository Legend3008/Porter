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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusWarning
import com.porter.core.ui.components.BookingStepIndicator
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.core.ui.components.StagedLaborLoader
import com.porter.domain.model.Quote
import com.porter.feature.booking.viewmodel.BookingFlowViewModel
import kotlinx.coroutines.delay

@Composable
fun QuoteScreen(
    onProceed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingFlowViewModel = hiltViewModel(),
) {
    val quoteState by viewModel.quoteState.collectAsState()

    LaunchedEffect(Unit) {
        if (quoteState !is UiState.Success) {
            viewModel.requestQuote()
        }
    }

    Scaffold(
        topBar = {
            Column {
                PorterTopBar(
                    title = "New Container Booking",
                    onNavigateBack = onBack
                )
                BookingStepIndicator(
                    currentStep = 4,
                    totalSteps = 5,
                    stepTitle = "Guaranteed Quote"
                )
            }
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        when (val state = quoteState) {
            is UiState.Loading -> StagedLaborLoader(
                title = "Calculating Guaranteed Rate",
                stages = listOf(
                    "Verifying port gate & inland ICD route",
                    "Calculating container freight, toll & fuel surcharge",
                    "Securing 15-minute price lock guarantee"
                ),
                footnote = "Zero hidden fees. Your quote is locked for 15 minutes once loaded.",
                modifier = Modifier.padding(padding)
            )
            is UiState.Error -> ErrorScreen(
                error = state.error,
                onRetry = { viewModel.requestQuote() },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                QuoteContent(
                    quote = state.data,
                    onProceed = onProceed,
                    onRefresh = { viewModel.requestQuote() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun QuoteContent(
    quote: Quote,
    onProceed: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsRemaining by remember { mutableIntStateOf(900) } // 15 minutes quote lock

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerStr = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Expiry countdown pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Parchment, ShapePill)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(StatusWarning, ShapePill)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LOCKED RATE",
                            style = FinePrint.copy(color = CanvasWhite, fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "  Expires in:",
                        style = Caption.copy(color = InkNearBlack)
                    )
                }
                Text(
                    text = timerStr,
                    style = BodyStrong.copy(color = if (secondsRemaining < 120) StatusWarning else ActionBlue)
                )
            }

            Text(
                text = "Fare Breakdown",
                style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
            )

            Text(
                text = "All inclusive commercial rate with verified GST input tax credit.",
                style = BodyDefault.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FareRow("Base Freight Haulage", quote.baseFarePaise)
                    FareRow("Fuel Surcharge (LSD)", quote.fuelSurchargePaise)
                    FareRow("National Highway Toll Charges", quote.tollChargesPaise)
                    FareRow("Port & Terminal Handling (THC)", quote.terminalHandlingChargesPaise)

                    Divider(color = Hairline, modifier = Modifier.padding(vertical = 4.dp))

                    FareRow("Subtotal", quote.subtotalPaise, isSubtotal = true)
                    FareRow("Integrated GST (18% IGST)", quote.gstPaise)

                    Divider(color = Hairline, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Payable",
                            style = BodyStrong.copy(fontSize = 18.sp, color = InkNearBlack)
                        )
                        val totalInr = quote.totalFarePaise / 100
                        Text(
                            text = "₹$totalInr",
                            style = BodyStrong.copy(fontSize = 22.sp, color = ActionBlue)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 32.dp)) {
            PorterPrimaryButton(
                text = "Accept Quote & Review",
                onClick = onProceed,
                enabled = secondsRemaining > 0
            )
        }
    }
}

@Composable
private fun FareRow(title: String, amountPaise: Long, isSubtotal: Boolean = false) {
    val inr = amountPaise / 100
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = if (isSubtotal) BodyStrong.copy(color = InkNearBlack) else Caption.copy(color = InkMuted48)
        )
        Text(
            text = "₹$inr",
            style = if (isSubtotal) BodyStrong.copy(color = InkNearBlack) else Caption.copy(color = InkNearBlack)
        )
    }
}
