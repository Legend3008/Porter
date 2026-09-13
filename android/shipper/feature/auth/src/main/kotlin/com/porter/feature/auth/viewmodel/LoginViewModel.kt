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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Initial)
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()

    fun sendOtp(phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.length < 10) {
            _uiState.value = UiState.Error(
                AppError.Validation(
                    field = "phone",
                    rule = "Please enter a valid 10-digit mobile number"
                )
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.sendOtp(cleanPhone)
            result.fold(
                onSuccess = {
                    _uiState.value = UiState.Success(cleanPhone)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        AppError.Server(
                            400,
                            error.message ?: "Failed to send OTP",
                            "Try again"
                        )
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = UiState.Initial
    }
}
