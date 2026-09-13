package com.porter.data.repository

import com.porter.core.auth.TokenStorage
import com.porter.domain.model.AuthTokens
import com.porter.domain.model.User
import com.porter.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock [AuthRepository] implementation.
 *
 * Provides realistic fake data. When the backend is ready, replace this class
 * with [AuthRepositoryNetworkImpl] — the domain and feature layers are unchanged.
 */
@Singleton
class AuthRepositoryMockImpl @Inject constructor(
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    override suspend fun sendOtp(phone: String): Result<Unit> {
        delay(800) // Simulate network
        return Result.success(Unit)
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<AuthTokens> {
        delay(1000)
        return if (otp == "123456" || otp.length == 6) {
            val tokens = AuthTokens(
                accessToken = "mock_access_token_${System.currentTimeMillis()}",
                refreshToken = "mock_refresh_token",
                expiresIn = 3600L
            )
            tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
            Result.success(tokens)
        } else {
            Result.failure(Exception("Invalid OTP"))
        }
    }

    override suspend fun login(email: String, password: String): Result<AuthTokens> {
        delay(1000)
        val tokens = AuthTokens(
            accessToken = "mock_access_token_${System.currentTimeMillis()}",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600L
        )
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
        return Result.success(tokens)
    }

    override suspend fun register(
        name: String, email: String, phone: String,
        password: String, companyName: String?, gstin: String?
    ): Result<Unit> {
        delay(1200)
        return Result.success(Unit)
    }

    override suspend fun getProfile(): Result<User> {
        delay(600)
        return Result.success(mockUser)
    }

    override suspend fun refreshToken(): Result<AuthTokens> {
        delay(500)
        val tokens = AuthTokens(
            accessToken = "mock_access_token_refreshed_${System.currentTimeMillis()}",
            refreshToken = "mock_refresh_token",
            expiresIn = 3600L
        )
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
        return Result.success(tokens)
    }

    override suspend fun logout(): Result<Unit> {
        tokenStorage.clearTokens()
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        delay(800)
        tokenStorage.clearTokens()
        return Result.success(Unit)
    }

    private val mockUser = User(
        id = "usr_001",
        name = "Rajesh Kumar",
        email = "rajesh@acmecorp.com",
        phone = "+919876543210",
        companyName = "Acme Logistics Pvt Ltd",
        gstin = "27AABCU9603R1ZX",
        profileImageUrl = null,
        isVerified = true,
        createdAt = Clock.System.now(),
    )
}
