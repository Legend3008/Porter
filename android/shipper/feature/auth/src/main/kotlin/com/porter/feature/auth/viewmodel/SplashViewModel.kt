package com.porter.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import com.porter.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
