package com.porter.domain.model

import kotlinx.datetime.Instant

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
}

enum class PaymentMethod {
    UPI,
    CARD,
    NETBANKING,
    NET_BANKING,
    NEFT_RTGS,
    WALLET,
    EMI,
    CASH,
}

data class Payment(
    val id: String,
    val bookingId: String,
    val orderId: String,             // Razorpay/PhonePe order ID
    val status: PaymentStatus,
    val amountPaise: Long,
    val method: PaymentMethod?,
    val gatewayReference: String?,  // Gateway transaction ID
    val createdAt: Instant,
    val confirmedAt: Instant?,      // Set ONLY after backend webhook — never from SDK callback
    val failureReason: String?,
)

data class Invoice(
    val id: String,
    val bookingId: String,
    val invoiceNumber: String,
    val status: InvoiceStatus,
    val amountPaise: Long,
    val gstPaise: Long,
    val totalPaise: Long,
    val issuedAt: Instant,
    val dueAt: Instant?,
    val paidAt: Instant?,
    val lineItems: List<InvoiceLineItem>,
    val downloadUrl: String?,
)

enum class InvoiceStatus { DRAFT, ISSUED, PAID, OVERDUE, CANCELLED }

data class InvoiceLineItem(
    val description: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val totalPaise: Long,
)
