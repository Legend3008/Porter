package com.porter.domain.repository

import com.porter.domain.model.DocumentType
import com.porter.domain.model.ShippingDocument
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getDocumentsForBooking(bookingId: String): Flow<List<ShippingDocument>>
    suspend fun uploadDocument(
        bookingId: String,
        type: DocumentType,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
    ): Result<ShippingDocument>
    suspend fun deleteDocument(documentId: String): Result<Unit>
}
