package com.porter.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.HeroDisplay
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTextField
import com.porter.core.ui.components.PorterTopBar
import com.porter.feature.auth.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onNavigateToOtp: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val validPhone = (uiState as UiState.Success<String>).data
            viewModel.resetState()
            onNavigateToOtp(validPhone)
        }
    }

    val errorMessage = when (val state = uiState) {
        is UiState.Error -> state.error.userMessage
        else -> null
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipper Registration",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Register Company",
                style = HeroDisplay.copy(fontSize = 28.sp, color = InkNearBlack)
            )

            Text(
                text = "Create a freight shipper account for container booking, dispatch management, and instant e-invoicing.",
                style = com.porter.core.designsystem.theme.BodyDefault.copy(color = InkMuted48)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PorterTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = "Company / Business Name *",
                placeholder = "e.g. Acme Logistics Pvt Ltd"
            )

            PorterTextField(
                value = gstin,
                onValueChange = { if (it.length <= 15) gstin = it.uppercase() },
                label = "GSTIN (Optional)",
                placeholder = "27AAAAA0000A1Z5",
                helperText = "Required for GST input tax credit (ITC) on invoices",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )

            PorterTextField(
                value = name,
                onValueChange = { name = it },
                label = "Authorized Contact Person *",
                placeholder = "Full Name"
            )

            PorterTextField(
                value = email,
                onValueChange = { email = it },
                label = "Official Business Email *",
                placeholder = "logistics@company.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            PorterTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it.filter { c -> c.isDigit() } },
                label = "Authorized Mobile Number *",
                placeholder = "9876543210",
                helperText = "OTP verification will be sent to this number",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                errorMessage = errorMessage
            )

            Spacer(modifier = Modifier.height(16.dp))

            PorterPrimaryButton(
                text = "Create Account & Verify OTP",
                onClick = {
                    viewModel.register(
                        name = name,
                        email = email,
                        phone = phone,
                        companyName = companyName,
                        gstin = gstin.ifBlank { null }
                    )
                },
                loading = uiState is UiState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
