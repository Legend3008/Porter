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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.TripStatusEvent
import com.porter.feature.tracking.viewmodel.StatusTimelineViewModel

@Composable
fun StatusTimelineScreen(
    bookingId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatusTimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookingId) {
        viewModel.loadTimeline(bookingId)
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipment Milestone Timeline",
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
                onRetry = { viewModel.loadTimeline(bookingId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(CanvasWhite)
                        .padding(24.dp)
                ) {
                    item {
                        Text(
                            text = "Milestone Checkpoints",
                            style = com.porter.core.designsystem.theme.HeroDisplay.copy(
                                fontSize = 24.sp,
                                color = InkNearBlack
                            )
                        )
                        Text(
                            text = "Audited lifecycle events for shipment #$bookingId",
                            style = Caption.copy(color = InkMuted48)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    itemsIndexed(state.data) { index, event ->
                        val isLast = index == state.data.lastIndex
                        TimelineNode(event = event, isLast = isLast)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun TimelineNode(event: TripStatusEvent, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Node dot + line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            val dotColor = when {
                event.isCompleted -> StatusSuccess
                event.isCurrent -> ActionBlue
                else -> Hairline
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(dotColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (event.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CanvasWhite,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (event.isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(CanvasWhite, CircleShape)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(if (event.isCompleted) StatusSuccess.copy(alpha = 0.5f) else Hairline)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.label,
                    style = BodyStrong.copy(
                        color = if (event.isCurrent) ActionBlue else InkNearBlack,
                        fontSize = 16.sp
                    )
                )
                if (event.isCurrent) {
                    Box(
                        modifier = Modifier
                            .background(ActionBlue.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IN PROGRESS",
                            style = FinePrint.copy(color = ActionBlue)
                        )
                    }
                }
            }

            val description = event.description
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = BodyDefault.copy(color = InkMuted48)
                )
            }

            val location = event.location
            if (!location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📍 $location",
                    style = FinePrint.copy(color = InkMuted48)
                )
            }
        }
    }
}
