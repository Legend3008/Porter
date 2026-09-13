package com.porter.domain.repository

import com.porter.domain.model.AuthTokens
import com.porter.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isLoggedIn(): Boolean
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, otp: String): Result<AuthTokens>
    suspend fun login(email: String, password: String): Result<AuthTokens>
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        companyName: String?,
        gstin: String?,
    ): Result<Unit>
    suspend fun getProfile(): Result<User>
    suspend fun refreshToken(): Result<AuthTokens>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
