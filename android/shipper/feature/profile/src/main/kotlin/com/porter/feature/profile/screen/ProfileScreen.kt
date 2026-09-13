package com.porter.feature.profile.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterSecondaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.User
import com.porter.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onSettings: () -> Unit,
    onSupport: () -> Unit,
    onInvoices: () -> Unit,
    onPaymentHistory: () -> Unit,
    onAccountDeletion: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipper Account",
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
                onRetry = { viewModel.loadProfile() },
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                ProfileContent(
                    user = state.data,
                    onSettings = onSettings,
                    onSupport = onSupport,
                    onInvoices = onInvoices,
                    onPaymentHistory = onPaymentHistory,
                    onAccountDeletion = onAccountDeletion,
                    onLogout = { viewModel.logout(onLogout) },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    onSettings: () -> Unit,
    onSupport: () -> Unit,
    onInvoices: () -> Unit,
    onPaymentHistory: () -> Unit,
    onAccountDeletion: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // User identity badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Parchment, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(
                    text = user.companyName ?: user.name,
                    style = HeroDisplay.copy(fontSize = 22.sp, color = InkNearBlack)
                )
                Text(
                    text = "${user.name} • ${user.phone}",
                    style = Caption.copy(color = InkMuted48)
                )
                if (!user.gstin.isNullOrBlank()) {
                    Text(
                        text = "GSTIN: ${user.gstin}",
                        style = FinePrint.copy(color = ActionBlue)
                    )
                }
            }
        }

        // Action links
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileOption(
                    title = "Tax Invoices & Receipts",
                    subtitle = "View and download GST-compliant tax invoices",
                    icon = Icons.Default.Receipt,
                    onClick = onInvoices
                )
                Divider(color = Hairline)
                ProfileOption(
                    title = "Payment History",
                    subtitle = "Transaction logs and payment gateway receipts",
                    icon = Icons.Default.Payment,
                    onClick = onPaymentHistory
                )
                Divider(color = Hairline)
                ProfileOption(
                    title = "App Settings & Preferences",
                    subtitle = "Push notifications, biometric lock, display",
                    icon = Icons.Default.Settings,
                    onClick = onSettings
                )
                Divider(color = Hairline)
                ProfileOption(
                    title = "Help & Dedicated Support",
                    subtitle = "24/7 logistics desk and dispatch escalations",
                    icon = Icons.Default.HelpOutline,
                    onClick = onSupport
                )
            }
        }

        // Account management / Deletion
        PorterCard(modifier = Modifier.fillMaxWidth()) {
            ProfileOption(
                title = "Account Data & Deletion",
                subtitle = "Permanent account closure per privacy laws",
                icon = Icons.Default.DeleteForever,
                iconTint = StatusError,
                onClick = onAccountDeletion
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout CTA
        PorterSecondaryButton(
            text = "Sign Out",
            onClick = onLogout
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = ActionBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
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
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = title, style = BodyStrong.copy(color = InkNearBlack))
                Text(text = subtitle, style = FinePrint.copy(color = InkMuted48))
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = InkMuted48,
            modifier = Modifier.size(18.dp)
        )
    }
}
