package com.porter.feature.documents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.DocumentType
import com.porter.domain.model.ShippingDocument
import com.porter.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ShippingDocument>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ShippingDocument>>> = _uiState.asStateFlow()

    fun loadDocuments(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            documentRepository.getDocumentsForBooking(bookingId)
                .catch { exception ->
                    _uiState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Failed to load documents: ${exception.message}",
                            recovery = "Try again"
                        )
                    )
                }
                .collect { docs ->
                    _uiState.value = if (docs.isEmpty()) UiState.Empty else UiState.Success(docs)
                }
        }
    }

    fun uploadMockDocument(bookingId: String, type: DocumentType, fileName: String) {
        viewModelScope.launch {
            val dummyBytes = ByteArray(1024 * 150)
            documentRepository.uploadDocument(
                bookingId = bookingId,
                type = type,
                fileName = fileName,
                fileBytes = dummyBytes,
                mimeType = "application/pdf"
            )
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId)
        }
    }
}
