package com.porter.domain.model

import kotlinx.datetime.Instant

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val companyName: String?,
    val gstin: String?,
    val profileImageUrl: String?,
    val isVerified: Boolean,
    val createdAt: Instant,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // seconds
)

data class OtpRequest(
    val phone: String,
    val countryCode: String = "+91",
)
