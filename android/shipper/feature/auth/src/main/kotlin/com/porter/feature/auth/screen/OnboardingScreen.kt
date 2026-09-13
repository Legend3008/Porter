package com.porter.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.ui.components.PorterPrimaryButton

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = "PORTER",
                style = BodyDefault.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = ActionBlue
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Container\nlogistics,\nsimplified.",
                style = HeroDisplay.copy(
                    lineHeight = 58.sp,
                    color = InkNearBlack
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "End-to-end container transport for Indian freight shippers. Instant quotes, live GPS tracking, and transparent billing.",
                style = BodyDefault.copy(color = InkMuted48)
            )
        }

        // Value props
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FeatureHighlight(
                icon = Icons.Default.LocalShipping,
                title = "20ft & 40ft Containers",
                description = "Standard, High Cube, and Reefer containers on demand"
            )
            FeatureHighlight(
                icon = Icons.Default.LocationOn,
                title = "Live Satellite Tracking",
                description = "Real-time updates directly from port to destination warehouse"
            )
            FeatureHighlight(
                icon = Icons.Default.VerifiedUser,
                title = "Guaranteed Pricing",
                description = "Zero hidden surcharges. Complete GST-compliant invoicing"
            )
        }

        // CTA Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PorterPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "By continuing, you agree to Porter's Terms of Service & Privacy Policy",
                style = com.porter.core.designsystem.theme.FinePrint.copy(color = InkMuted48),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Parchment, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ActionBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = BodyDefault.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = InkNearBlack
                )
            )
            Text(
                text = description,
                style = com.porter.core.designsystem.theme.Caption.copy(color = InkMuted48)
            )
        }
    }
}
