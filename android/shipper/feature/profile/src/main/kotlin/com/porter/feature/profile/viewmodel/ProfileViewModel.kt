package com.porter.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.User
import com.porter.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Initial)
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.getProfile()
            result.fold(
                onSuccess = { user -> _uiState.value = UiState.Success(user) },
                onFailure = { err ->
                    _uiState.value = UiState.Error(
                        AppError.Server(500, err.message ?: "Failed to load profile", "Retry")
                    )
                }
            )
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            val result = authRepository.deleteAccount()
            result.fold(
                onSuccess = { _deleteState.value = UiState.Success(Unit) },
                onFailure = { err ->
                    _deleteState.value = UiState.Error(
                        AppError.Server(500, err.message ?: "Failed to delete account", "Contact support")
                    )
                }
            )
        }
    }
}
