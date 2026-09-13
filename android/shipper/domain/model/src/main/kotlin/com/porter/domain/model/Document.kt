package com.porter.domain.model

import kotlinx.datetime.Instant

enum class DocumentType {
    BILL_OF_LADING,
    DELIVERY_ORDER,
    GATE_PASS,
    COMMERCIAL_INVOICE,
    PACKING_LIST,
    CUSTOMS_DECLARATION,
    PROOF_OF_DELIVERY,
    OTHER,
}

data class ShippingDocument(
    val id: String,
    val bookingId: String,
    val type: DocumentType,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val downloadUrl: String,
    val uploadedAt: Instant,
    val isVerified: Boolean = false,
)
