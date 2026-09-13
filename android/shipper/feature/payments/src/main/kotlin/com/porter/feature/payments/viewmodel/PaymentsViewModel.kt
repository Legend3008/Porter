package com.porter.feature.payments.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.Invoice
import com.porter.domain.model.Payment
import com.porter.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _invoicesState = MutableStateFlow<UiState<List<Invoice>>>(UiState.Loading)
    val invoicesState: StateFlow<UiState<List<Invoice>>> = _invoicesState.asStateFlow()

    private val _invoiceDetailState = MutableStateFlow<UiState<Invoice>>(UiState.Loading)
    val invoiceDetailState: StateFlow<UiState<Invoice>> = _invoiceDetailState.asStateFlow()

    private val _paymentsState = MutableStateFlow<UiState<List<Payment>>>(UiState.Loading)
    val paymentsState: StateFlow<UiState<List<Payment>>> = _paymentsState.asStateFlow()

    fun loadInvoices() {
        viewModelScope.launch {
            _invoicesState.value = UiState.Loading
            val result = paymentRepository.getInvoices()
            result.fold(
                onSuccess = { list ->
                    _invoicesState.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                },
                onFailure = { err ->
                    _invoicesState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Could not load invoices: ${err.message}",
                            recovery = "Try again"
                        )
                    )
                }
            )
        }
    }

    fun loadInvoiceDetail(invoiceId: String) {
        viewModelScope.launch {
            _invoiceDetailState.value = UiState.Loading
            val result = paymentRepository.getInvoice(invoiceId)
            result.fold(
                onSuccess = { invoice ->
                    _invoiceDetailState.value = UiState.Success(invoice)
                },
                onFailure = { err ->
                    _invoiceDetailState.value = UiState.Error(
                        AppError.Server(404, "Invoice not found", "Go back")
                    )
                }
            )
        }
    }

    fun loadPaymentHistory() {
        viewModelScope.launch {
            _paymentsState.value = UiState.Loading
            val result = paymentRepository.getPayments()
            result.fold(
                onSuccess = { list ->
                    _paymentsState.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                },
                onFailure = { err ->
                    _paymentsState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Could not load payment history: ${err.message}",
                            recovery = "Try again"
                        )
                    )
                }
            )
        }
    }
}
