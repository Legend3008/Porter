package com.porter.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PorterApiService {
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("bookings")
    suspend fun getBookings(@Query("status") status: String?): Response<List<Map<String, Any>>>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): Response<Map<String, Any>>

    @POST("bookings/drafts")
    suspend fun createDraft(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body shipmentDetails: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("bookings/drafts/{draftId}/quote")
    suspend fun getQuote(@Path("draftId") draftId: String): Response<Map<String, Any>>

    @POST("bookings/confirm")
    suspend fun confirmBooking(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("trips/{tripId}")
    suspend fun getTrip(@Path("tripId") tripId: String): Response<Map<String, Any>>

    @POST("payments/orders")
    suspend fun createPaymentOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>
}
