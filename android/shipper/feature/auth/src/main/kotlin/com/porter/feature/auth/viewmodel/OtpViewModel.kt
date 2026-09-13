package com.porter.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.AuthTokens
import com.porter.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AuthTokens>>(UiState.Initial)
    val uiState: StateFlow<UiState<AuthTokens>> = _uiState.asStateFlow()

    private val _resendSuccess = MutableStateFlow(false)
    val resendSuccess: StateFlow<Boolean> = _resendSuccess.asStateFlow()

    fun verifyOtp(phone: String, otp: String) {
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 6) {
            _uiState.value = UiState.Error(
                AppError.Validation("otp", "Please enter the complete 6-digit verification code")
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.verifyOtp(phone, cleanOtp)
            result.fold(
                onSuccess = { tokens ->
                    _uiState.value = UiState.Success(tokens)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        AppError.Server(
                            code = 401,
                            userMessage = error.message ?: "Invalid OTP code",
                            recovery = "Check SMS and try again"
                        )
                    )
                }
            )
        }
    }

    fun resendOtp(phone: String) {
        viewModelScope.launch {
            val result = authRepository.sendOtp(phone)
            result.onSuccess {
                _resendSuccess.value = true
            }
        }
    }
}
