package com.porter.data.repository

import com.porter.domain.model.DocumentType
import com.porter.domain.model.ShippingDocument
import com.porter.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryMockImpl @Inject constructor() : DocumentRepository {

    private val documents = MutableStateFlow<List<ShippingDocument>>(
        listOf(
            ShippingDocument(
                id = "doc-001",
                bookingId = "BK-2024-001",
                type = DocumentType.BILL_OF_LADING,
                fileName = "BOL_BK2024001.pdf",
                fileSizeBytes = 1024 * 450, // 450 KB
                mimeType = "application/pdf",
                downloadUrl = "https://mock.porter.in/docs/bol-001.pdf",
                uploadedAt = Instant.parse("2024-06-01T08:30:00Z"),
                isVerified = true
            ),
            ShippingDocument(
                id = "doc-002",
                bookingId = "BK-2024-001",
                type = DocumentType.GATE_PASS,
                fileName = "GatePass_JNPT.pdf",
                fileSizeBytes = 1024 * 210, // 210 KB
                mimeType = "application/pdf",
                downloadUrl = "https://mock.porter.in/docs/gp-002.pdf",
                uploadedAt = Instant.parse("2024-06-01T09:15:00Z"),
                isVerified = true
            )
        )
    )

    override fun getDocumentsForBooking(bookingId: String): Flow<List<ShippingDocument>> {
        return documents.asStateFlow()
    }

    override suspend fun uploadDocument(
        bookingId: String,
        type: DocumentType,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Result<ShippingDocument> {
        val newDoc = ShippingDocument(
            id = "doc-${System.currentTimeMillis()}",
            bookingId = bookingId,
            type = type,
            fileName = fileName,
            fileSizeBytes = fileBytes.size.toLong(),
            mimeType = mimeType,
            downloadUrl = "https://mock.porter.in/docs/$fileName",
            uploadedAt = Clock.System.now(),
            isVerified = false
        )
        documents.value = documents.value + newDoc
        return Result.success(newDoc)
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        documents.value = documents.value.filter { it.id != documentId }
        return Result.success(Unit)
    }
}
