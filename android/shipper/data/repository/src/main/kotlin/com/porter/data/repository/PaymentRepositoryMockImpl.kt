package com.porter.data.repository

import com.porter.domain.model.*
import com.porter.domain.repository.PaymentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryMockImpl @Inject constructor() : PaymentRepository {

    override suspend fun createPaymentOrder(bookingId: String): Result<Payment> {
        delay(1000)
        return Result.success(
            Payment(
                id = "pay_${System.currentTimeMillis()}",
                bookingId = bookingId,
                orderId = "order_mock_${System.currentTimeMillis()}",
                status = PaymentStatus.PENDING,
                amountPaise = 6195000L,
                method = null,
                gatewayReference = null,
                createdAt = Clock.System.now(),
                confirmedAt = null, // NOT confirmed yet — awaiting backend webhook
                failureReason = null
            )
        )
    }

    /**
     * Polls payment status.
     * In production, this subscribes to a WebSocket/SSE that the backend pushes to
     * after receiving the payment gateway webhook.
     * Here we simulate: PENDING → PROCESSING → SUCCESS after 8 seconds.
     */
    override fun observePaymentStatus(bookingId: String): Flow<PaymentStatus> = flow {
        emit(PaymentStatus.PENDING)
        delay(3000)
        emit(PaymentStatus.PROCESSING)
        delay(5000)
        emit(PaymentStatus.SUCCESS)
    }

    override suspend fun getPayments(): Result<List<Payment>> {
        delay(600)
        return Result.success(mockPayments)
    }

    override suspend fun getInvoices(): Result<List<Invoice>> {
        delay(600)
        return Result.success(mockInvoices)
    }

    override suspend fun getInvoice(id: String): Result<Invoice> {
        delay(400)
        return mockInvoices.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(Exception("Invoice not found"))
    }

    private val now = Clock.System.now()

    private val mockPayments = listOf(
        Payment(
            id = "pay_001", bookingId = "bkg_001",
            orderId = "order_001", status = PaymentStatus.SUCCESS,
            amountPaise = 6195000L, method = PaymentMethod.UPI,
            gatewayReference = "GW_REF_001", createdAt = now,
            confirmedAt = now, failureReason = null
        ),
        Payment(
            id = "pay_002", bookingId = "bkg_002",
            orderId = "order_002", status = PaymentStatus.SUCCESS,
            amountPaise = 3200000L, method = PaymentMethod.CARD,
            gatewayReference = "GW_REF_002", createdAt = now,
            confirmedAt = now, failureReason = null
        )
    )

    private val mockInvoices = listOf(
        Invoice(
            id = "inv_001", bookingId = "bkg_001",
            invoiceNumber = "INV-2024-001", status = InvoiceStatus.PAID,
            amountPaise = 5250000L, gstPaise = 945000L, totalPaise = 6195000L,
            issuedAt = now, dueAt = now.plus(7, DateTimeUnit.DAY, TimeZone.UTC), paidAt = now,
            lineItems = listOf(
                InvoiceLineItem("40ft Container Transport x2", 2, 2250000L, 4500000L),
                InvoiceLineItem("Fuel Surcharge", 1, 450000L, 450000L),
                InvoiceLineItem("Port Handling", 1, 200000L, 200000L),
                InvoiceLineItem("Other Charges", 1, 100000L, 100000L),
            ),
            downloadUrl = null
        )
    )
}
