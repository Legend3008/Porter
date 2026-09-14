package com.porter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.ShapeXl

/**
 * Psychological Empty State View.
 *
 * Implements Principle 31 (Empty States):
 * - Clearly explains what is empty.
 * - Explains why it is empty without blaming the user.
 * - Provides immediate, low-friction recovery/action pathways.
 */
@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.LocalShipping,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Parchment.copy(alpha = 0.5f), ShapeXl)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CanvasWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = BodyStrong.copy(fontSize = 18.sp, color = InkNearBlack),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = BodyDefault.copy(fontSize = 14.sp, color = InkMuted48),
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                PorterPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                PorterTextButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction
                )
            }
        }
    }
}
