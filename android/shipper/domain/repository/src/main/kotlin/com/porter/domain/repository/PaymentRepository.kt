package com.porter.domain.repository

import com.porter.domain.model.Invoice
import com.porter.domain.model.Payment
import com.porter.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    suspend fun createPaymentOrder(bookingId: String): Result<Payment>
    /** Poll payment status — CONFIRMED only when backend confirms via webhook */
    fun observePaymentStatus(bookingId: String): Flow<PaymentStatus>
    suspend fun getPayments(): Result<List<Payment>>
    suspend fun getInvoices(): Result<List<Invoice>>
    suspend fun getInvoice(id: String): Result<Invoice>
}
