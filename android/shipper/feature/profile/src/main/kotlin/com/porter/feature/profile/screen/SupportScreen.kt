package com.porter.feature.profile.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTopBar

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            PorterTopBar(
                title = "24/7 Shipper Support",
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Parchment, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HeadsetMic,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Dedicated Logistics Support",
                style = HeroDisplay.copy(fontSize = 26.sp, color = InkNearBlack)
            )

            Text(
                text = "Our operations command center operates 24/7 across all major Indian ports, container freight stations (CFS), and inland container depots (ICD).",
                style = BodyDefault.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PorterCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = ActionBlue,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(text = "Toll-Free Shipper Helpline", style = BodyStrong.copy(color = InkNearBlack))
                            Text(text = "1800-419-PORTER (1800 419 7678)", style = Caption.copy(color = ActionBlue))
                        }
                    }

                    Divider(color = Hairline, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = ActionBlue,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(text = "Priority Email Escalations", style = BodyStrong.copy(color = InkNearBlack))
                            Text(text = "shipper-support@porter.in", style = Caption.copy(color = ActionBlue))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PorterPrimaryButton(
                text = "Call Operations Desk Now",
                onClick = {}
            )
        }
    }
}
