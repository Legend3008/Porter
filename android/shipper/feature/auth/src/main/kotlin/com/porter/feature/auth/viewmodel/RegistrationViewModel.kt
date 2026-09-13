package com.porter.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Initial)
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()

    fun register(
        name: String,
        email: String,
        phone: String,
        companyName: String,
        gstin: String?
    ) {
        if (name.isBlank()) {
            _uiState.value = UiState.Error(AppError.Validation("name", "Contact name is required"))
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.value = UiState.Error(AppError.Validation("email", "Valid email is required"))
            return
        }
        if (phone.length != 10) {
            _uiState.value = UiState.Error(AppError.Validation("phone", "10-digit phone number is required"))
            return
        }
        if (companyName.isBlank()) {
            _uiState.value = UiState.Error(AppError.Validation("companyName", "Company name is required"))
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val regResult = authRepository.register(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                password = "temporary_otp_auth",
                companyName = companyName.trim(),
                gstin = gstin?.trim()
            )
            regResult.fold(
                onSuccess = {
                    val otpResult = authRepository.sendOtp(phone.trim())
                    otpResult.fold(
                        onSuccess = { _uiState.value = UiState.Success(phone.trim()) },
                        onFailure = { err ->
                            _uiState.value = UiState.Error(
                                AppError.Server(500, err.message ?: "Failed to send OTP", "Try again")
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.value = UiState.Error(
                        AppError.Server(400, err.message ?: "Registration failed", "Review details")
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = UiState.Initial
    }
}
