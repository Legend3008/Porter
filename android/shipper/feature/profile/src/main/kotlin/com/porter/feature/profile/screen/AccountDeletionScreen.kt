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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.ShapePill
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.ui.components.PorterSecondaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.profile.viewmodel.ProfileViewModel

@Composable
fun AccountDeletionScreen(
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    var confirmed by remember { mutableStateOf(false) }
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    LaunchedEffect(deleteState) {
        if (deleteState is UiState.Success) {
            onDeleted()
        }
    }

    val isDeleting = deleteState is UiState.Loading

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Account Closure",
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Permanent Account Deletion",
                    style = HeroDisplay.copy(fontSize = 24.sp, color = StatusError)
                )

                Text(
                    text = "Deleting your shipper account will permanently erase your business profile, linked billing contacts, saved delivery addresses, and API credentials.",
                    style = BodyDefault.copy(color = InkNearBlack)
                )

                Text(
                    text = "Note: Per Indian GST regulations, past tax invoices and completed delivery manifests must be retained by Porter for 7 years and cannot be purged from audit logs.",
                    style = FinePrint.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = { confirmed = it },
                        colors = CheckboxDefaults.colors(checkedColor = StatusError)
                    )
                    Text(
                        text = "I understand this action is permanent and cannot be undone.",
                        style = FinePrint.copy(color = InkNearBlack),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.deleteAccount() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = confirmed && !isDeleting,
                    shape = ShapePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = CanvasWhite,
                        disabledContainerColor = StatusError.copy(alpha = 0.3f),
                        disabledContentColor = CanvasWhite.copy(alpha = 0.5f)
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = CanvasWhite,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Permanently Delete My Account", style = BodyDefault)
                    }
                }

                PorterSecondaryButton(
                    text = "Cancel & Keep Account",
                    onClick = onBack
                )
            }
        }
    }
}
