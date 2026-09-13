package com.porter.feature.booking.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterTopBar
import com.porter.core.ui.components.StatusBadge
import com.porter.domain.model.BookingListItem
import com.porter.feature.booking.viewmodel.BookingFilterTab
import com.porter.feature.booking.viewmodel.BookingsListViewModel

@Composable
fun BookingsListScreen(
    onOpenBooking: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val tabs = BookingFilterTab.values()

    Scaffold(
        topBar = {
            Column {
                PorterTopBar(
                    title = "My Shipments",
                    onNavigateBack = onBack
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = CanvasWhite,
                    contentColor = ActionBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = ActionBlue
                        )
                    },
                    edgePadding = 16.dp
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = if (selectedTab == tab) BodyStrong else BodyDefault,
                                    color = if (selectedTab == tab) ActionBlue else InkMuted48
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorScreen(
                error = state.error,
                onRetry = { viewModel.loadBookings() },
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
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = InkMuted48,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "No shipments found",
                            style = BodyStrong.copy(color = InkNearBlack)
                        )
                        Text(
                            text = "There are no bookings matching the selected filter.",
                            style = Caption.copy(color = InkMuted48)
                        )
                    }
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.data, key = { it.id }) { booking ->
                        BookingItemCard(
                            booking = booking,
                            onClick = { onOpenBooking(booking.id) }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun BookingItemCard(
    booking: BookingListItem,
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
                    text = booking.id,
                    style = BodyStrong.copy(color = InkNearBlack)
                )
                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ORIGIN PORT", style = FinePrint.copy(color = InkMuted48))
                    Text(
                        text = booking.originPort,
                        style = BodyStrong.copy(fontSize = 16.sp, color = InkNearBlack)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DESTINATION", style = FinePrint.copy(color = InkMuted48))
                    Text(
                        text = booking.destinationPort,
                        style = BodyStrong.copy(fontSize = 16.sp, color = InkNearBlack)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.containerType.displayName} • ${booking.containerCount} Units",
                    style = Caption.copy(color = InkMuted48)
                )
                val inr = booking.totalAmountPaise / 100
                Text(
                    text = "₹$inr",
                    style = BodyStrong.copy(color = ActionBlue)
                )
            }
        }
    }
}
