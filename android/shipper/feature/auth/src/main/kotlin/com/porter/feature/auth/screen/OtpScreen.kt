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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextButton
import com.porter.core.ui.components.PorterTextField
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.auth.viewmodel.OtpViewModel

@Composable
fun OtpScreen(
    phone: String,
    onVerified: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = hiltViewModel(),
) {
    var otpCode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onVerified()
        }
    }

    val errorMessage = when (val state = uiState) {
        is UiState.Error -> state.error.userMessage
        else -> null
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Verification",
                onNavigateBack = onNavigateBack
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Verify Mobile",
                    style = HeroDisplay.copy(fontSize = 30.sp, color = InkNearBlack)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the 6-digit OTP code sent to +91 $phone",
                    style = BodyDefault.copy(color = InkMuted48)
                )

                Spacer(modifier = Modifier.height(36.dp))

                PorterTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it.filter { c -> c.isDigit() } },
                    label = "6-Digit OTP Code",
                    placeholder = "123456",
                    errorMessage = errorMessage,
                    helperText = "For mock testing, enter any 6 digits (e.g. 123456)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(24.dp))

                PorterPrimaryButton(
                    text = "Verify & Sign In",
                    onClick = { viewModel.verifyOtp(phone, otpCode) },
                    loading = uiState is UiState.Loading,
                    enabled = otpCode.length == 6
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive code?",
                    style = BodyDefault.copy(color = InkMuted48)
                )
                PorterTextButton(
                    text = "Resend OTP",
                    onClick = { viewModel.resendOtp(phone) }
                )
            }
        }
    }
}
