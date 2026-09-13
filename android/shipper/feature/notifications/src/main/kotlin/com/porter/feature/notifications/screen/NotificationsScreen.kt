package com.porter.feature.notifications.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterTextButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.PorterNotification
import com.porter.feature.notifications.viewmodel.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onOpenBooking: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipment Notifications",
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
                onRetry = { viewModel.loadNotifications() },
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
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = InkMuted48,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No notifications yet", style = BodyStrong.copy(color = InkNearBlack))
                        Text(text = "Real-time dispatch and trip milestones will appear here.", style = Caption.copy(color = InkMuted48))
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Alerts",
                                style = HeroDisplay.copy(fontSize = 24.sp, color = InkNearBlack)
                            )
                            PorterTextButton(
                                text = "Mark all read",
                                onClick = { viewModel.markAllAsRead() }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.data, key = { it.id }) { notif ->
                        NotificationItemCard(
                            notification = notif,
                            onClick = {
                                viewModel.markAsRead(notif.id)
                                notif.bookingId?.let { onOpenBooking(it) }
                            }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: PorterNotification,
    onClick: () -> Unit
) {
    val bgColor = if (!notification.isRead) Parchment else CanvasWhite

    PorterCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 12.dp)
                        .size(10.dp)
                        .background(ActionBlue, CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = BodyStrong.copy(fontSize = 16.sp, color = InkNearBlack)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = BodyDefault.copy(fontSize = 14.sp, color = InkMuted48)
                )
                if (notification.bookingId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "View Booking #${notification.bookingId} →",
                        style = FinePrint.copy(color = ActionBlue)
                    )
                }
            }
        }
    }
}
