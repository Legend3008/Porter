package com.porter.feature.home.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.porter.core.designsystem.theme.DarkTile1
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextButton
import com.porter.core.ui.components.StatusBadge
import com.porter.domain.model.BookingListItem
import com.porter.feature.home.viewmodel.HomeDashboardData
import com.porter.feature.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onCreateShipment: () -> Unit,
    onViewBookings: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = onOpenProfile,
                unreadNotifications = 2
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorScreen(
                error = state.error,
                onRetry = { viewModel.loadDashboard() },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                HomeContent(
                    data = state.data,
                    onCreateShipment = onCreateShipment,
                    onViewBookings = onViewBookings,
                    onOpenBooking = onOpenBooking,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun HomeTopBar(
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    unreadNotifications: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasWhite)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PORTER",
                style = BodyDefault.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = ActionBlue
                )
            )
            Text(
                text = "Freight Logistics",
                style = FinePrint.copy(color = InkMuted48)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = StatusError) {
                                Text(unreadNotifications.toString(), color = CanvasWhite)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = InkNearBlack
                    )
                }
            }

            IconButton(onClick = onOpenProfile) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Parchment, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = ActionBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeDashboardData,
    onCreateShipment: () -> Unit,
    onViewBookings: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Banner / Greeting
        item {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                val company = data.user?.companyName ?: "Shipper Portal"
                Text(
                    text = company,
                    style = Caption.copy(color = InkMuted48)
                )
                Text(
                    text = "Shipment Overview",
                    style = HeroDisplay.copy(fontSize = 30.sp, color = InkNearBlack)
                )
            }
        }

        // Action Hero Card: Book Container
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkTile1, com.porter.core.designsystem.theme.ShapeXl)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "NEW CONTAINER DISPATCH",
                        style = FinePrint.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = ActionBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Book 20ft & 40ft Trailers",
                        style = BodyStrong.copy(fontSize = 22.sp, color = CanvasWhite)
                    )
                    Text(
                        text = "Instant guaranteed rates between ports, CFS, and inland warehouses across India.",
                        style = Caption.copy(color = com.porter.core.designsystem.theme.BodyMuted)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PorterPrimaryButton(
                        text = "Create Shipment Booking",
                        onClick = onCreateShipment
                    )
                }
            }
        }

        // Active Shipments Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Shipments (${data.activeBookings.size})",
                    style = BodyStrong.copy(fontSize = 18.sp, color = InkNearBlack)
                )
                PorterTextButton(
                    text = "View All",
                    onClick = onViewBookings
                )
            }
        }

        if (data.activeBookings.isEmpty()) {
            item {
                PorterCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = InkMuted48,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active container shipments",
                            style = BodyStrong.copy(color = InkNearBlack)
                        )
                        Text(
                            text = "Create your first shipment to get instant quote and live tracking.",
                            style = Caption.copy(color = InkMuted48)
                        )
                    }
                }
            }
        } else {
            items(data.activeBookings, key = { it.id }) { booking ->
                ActiveBookingCard(
                    booking = booking,
                    onClick = { onOpenBooking(booking.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActiveBookingCard(
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.containerType.displayName} • ${booking.containerCount} Units",
                    style = Caption.copy(color = InkMuted48)
                )
                val totalInr = booking.totalAmountPaise / 100
                Text(
                    text = "₹$totalInr",
                    style = BodyStrong.copy(color = ActionBlue, fontSize = 16.sp)
                )
            }
        }
    }
}
