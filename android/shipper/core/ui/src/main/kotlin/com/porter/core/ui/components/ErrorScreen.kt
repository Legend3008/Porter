package com.porter.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porter.core.common.AppError
import com.porter.core.common.RecoveryAction
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.StatusError

/**
 * Full-screen error state.
 * Shows [AppError.userMessage] and a recovery button based on [AppError.recovery].
 */
@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(StatusError.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = BodyStrong.copy(fontSize = 32.sp, color = StatusError)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Something didn't go as expected",
            style = com.porter.core.designsystem.theme.DisplayMd.copy(fontSize = 20.sp),
            color = InkNearBlack,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.userMessage,
            style = BodyDefault.copy(fontSize = 14.sp),
            color = InkMuted48,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        when (error.recovery) {
            RecoveryAction.RETRY -> {
                PorterPrimaryButton(
                    text = "Try Again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
                if (onBack != null) {
                    Spacer(Modifier.height(12.dp))
                    PorterSecondaryButton(
                        text = "Go Back",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            RecoveryAction.GO_BACK -> {
                onBack?.let {
                    PorterPrimaryButton(
                        text = "Go Back",
                        onClick = it,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            RecoveryAction.RELOGIN -> {
                PorterPrimaryButton(
                    text = "Log In Again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            RecoveryAction.DISMISS -> {
                PorterSecondaryButton(
                    text = "Dismiss",
                    onClick = { onBack?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Full-screen offline state.
 * Must be distinct from ErrorScreen — different recovery action (check connection).
 */
@Composable
fun OfflineScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = "No connection",
            tint = InkMuted48,
            modifier = Modifier.height(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "You're offline",
            style = com.porter.core.designsystem.theme.DisplayMd,
            color = InkNearBlack
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Please check your internet connection and try again.",
            style = Caption,
            color = InkMuted48,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        PorterPrimaryButton(
            text = "Retry",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
