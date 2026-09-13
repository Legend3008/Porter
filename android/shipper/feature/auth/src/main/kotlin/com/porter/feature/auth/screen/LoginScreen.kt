package com.porter.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextButton
import com.porter.core.ui.components.PorterTextField
import com.porter.feature.auth.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    var phone by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val validatedPhone = (uiState as UiState.Success<String>).data
            viewModel.resetState()
            onNavigateToOtp(validatedPhone)
        }
    }

    val errorMessage = when (val state = uiState) {
        is UiState.Error -> state.error.userMessage
        else -> null
    }

    val isLoading = uiState is UiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(48.dp))

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
                text = "Welcome back.",
                style = HeroDisplay.copy(fontSize = 34.sp, lineHeight = 40.sp, color = InkNearBlack)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to manage and track your container shipments.",
                style = BodyDefault.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(36.dp))

            PorterTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it.filter { char -> char.isDigit() } },
                label = "Mobile Number",
                placeholder = "9876543210",
                errorMessage = errorMessage,
                helperText = "We will send an SMS with a 6-digit OTP verification code",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PorterPrimaryButton(
                text = "Continue with OTP",
                onClick = { viewModel.sendOtp(phone) },
                loading = isLoading,
                enabled = phone.length == 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demo Mode:",
                    style = com.porter.core.designsystem.theme.Caption.copy(color = InkMuted48)
                )
                PorterTextButton(
                    text = "Skip to Home",
                    onClick = onNavigateToHome
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New freight shipper?",
                style = BodyDefault.copy(color = InkMuted48)
            )
            PorterTextButton(
                text = "Create an account",
                onClick = onNavigateToRegister
            )
        }
    }
}
