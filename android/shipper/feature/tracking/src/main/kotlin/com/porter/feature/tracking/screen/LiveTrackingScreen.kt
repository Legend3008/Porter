package com.porter.feature.tracking.screen

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.porter.core.designsystem.theme.DarkTile2
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapeLg
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.designsystem.theme.StatusWarning
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.FreshnessBadge
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterSecondaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.tracking.viewmodel.LiveTrackingViewModel
import com.porter.feature.tracking.viewmodel.TrackingUiModel

@Composable
fun LiveTrackingScreen(
    tripId: String,
    onTimeline: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTrackingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tripId) {
        viewModel.startTracking(tripId)
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Live Container Tracking",
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
                onRetry = { viewModel.startTracking(tripId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                TrackingContent(
                    data = state.data,
                    onTimeline = onTimeline,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun TrackingContent(
    data: TrackingUiModel,
    onTimeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trip = data.trip
    val loc = data.currentLocation

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Freshness & Trip ID bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRIP #${trip.id}",
                    style = FinePrint.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ActionBlue
                    )
                )
                Text(
                    text = "Booking Ref: ${trip.bookingId}",
                    style = Caption.copy(color = InkMuted48)
                )
            }
            FreshnessBadge(
                level = data.freshness,
                lastUpdatedLabel = data.lastUpdatedText
            )
        }

        // Radar / Telemetry Map Card (Dark Tile Design per design.md)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(DarkTile1, ShapeLg)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "GPS TELEMETRY",
                            style = FinePrint.copy(
                                color = ActionBlue,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = String.format("%.4f° N, %.4f° E", loc.latitude, loc.longitude),
                            style = BodyStrong.copy(color = CanvasWhite)
                        )
                    }

                    val heading = loc.heading ?: 0f
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkTile2, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Heading",
                            tint = ActionBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(heading)
                        )
                    }
                }

                // Center animated pulse and trailer icon
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ActionBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ActionBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = CanvasWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Speed & ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${(loc.speedKmh ?: 52.0).toInt()} km/h",
                            style = BodyStrong.copy(color = CanvasWhite)
                        )
                    }

                    Text(
                        text = "ETA: ~2 hrs 45 mins",
                        style = BodyStrong.copy(color = StatusWarning)
                    )
                }
            }
        }

        // Driver & Vehicle Card
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Parchment, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ActionBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = trip.driver.name,
                                style = BodyStrong.copy(fontSize = 17.sp, color = InkNearBlack)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = StatusWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${trip.driver.rating} • Verified Carrier Driver",
                                    style = FinePrint.copy(color = InkMuted48)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ActionBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = ActionBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = Hairline, modifier = Modifier.padding(vertical = 14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TRAILER REGISTRATION", style = FinePrint.copy(color = InkMuted48))
                        Text(text = trip.vehicle.number, style = BodyStrong.copy(color = InkNearBlack))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "CHASSIS / VEHICLE", style = FinePrint.copy(color = InkMuted48))
                        Text(text = "${trip.vehicle.make} ${trip.vehicle.model}", style = BodyStrong.copy(color = InkNearBlack))
                    }
                }
            }
        }

        // Timeline CTA Button
        PorterSecondaryButton(
            text = "View Status Timeline & Checkpoints",
            onClick = onTimeline
        )
    }
}
