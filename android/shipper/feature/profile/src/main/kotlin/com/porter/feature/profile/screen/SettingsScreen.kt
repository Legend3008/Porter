package com.porter.feature.profile.screen

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.Hairline
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tripAlerts by remember { mutableStateOf(true) }
    var paymentReceipts by remember { mutableStateOf(true) }
    var promotionalAlerts by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Preferences & Settings",
                onNavigateBack = onBack
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CanvasWhite)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Notifications & Alerts",
                style = HeroDisplay.copy(fontSize = 22.sp, color = InkNearBlack)
            )

            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingToggleRow(
                        title = "Real-time Trip Milestones",
                        subtitle = "Push notifications when container is loaded, in-transit, and delivered",
                        checked = tripAlerts,
                        onCheckedChange = { tripAlerts = it }
                    )
                    Divider(color = Hairline)
                    SettingToggleRow(
                        title = "Payment & Billing Receipts",
                        subtitle = "Instant SMS and push receipts upon payment confirmation",
                        checked = paymentReceipts,
                        onCheckedChange = { paymentReceipts = it }
                    )
                    Divider(color = Hairline)
                    SettingToggleRow(
                        title = "Lane & Route Discounts",
                        subtitle = "Promotional lane discounts for recurring shippers",
                        checked = promotionalAlerts,
                        onCheckedChange = { promotionalAlerts = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "System & Security",
                style = HeroDisplay.copy(fontSize = 22.sp, color = InkNearBlack)
            )

            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "App Version", style = BodyDefault.copy(color = InkNearBlack))
                        Text(text = "1.0.0-release (Production)", style = FinePrint.copy(color = InkMuted48))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Token Encryption", style = BodyDefault.copy(color = InkNearBlack))
                        Text(text = "AES256-GCM Hardware Encrypted", style = FinePrint.copy(color = ActionBlue))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = BodyStrong.copy(color = InkNearBlack))
            Text(text = subtitle, style = FinePrint.copy(color = InkMuted48))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CanvasWhite, checkedTrackColor = ActionBlue)
        )
    }
}
