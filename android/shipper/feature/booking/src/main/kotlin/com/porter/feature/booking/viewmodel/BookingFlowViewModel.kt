package com.porter.feature.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.Address
import com.porter.domain.model.ContainerType
import com.porter.domain.model.Payment
import com.porter.domain.model.PaymentMethod
import com.porter.domain.model.PaymentStatus
import com.porter.domain.model.Quote
import com.porter.domain.model.ShipmentDetails
import com.porter.domain.repository.BookingRepository
import com.porter.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

data class BookingDraftState(
    val cargoDescription: String = "",
    val cargoWeightKg: Double = 0.0,
    val isHazardous: Boolean = false,
    val containerType: ContainerType = ContainerType.DRY_20FT,
    val temperatureRequirementC: Double? = null,
    val customsDocumentRequired: Boolean = true,
    val pickupAddress: Address = Address(
        line1 = "",
        line2 = null,
        city = "Mumbai",
        state = "Maharashtra",
        pincode = "400707",
        contactName = "",
        contactPhone = ""
    ),
    val deliveryAddress: Address = Address(
        line1 = "",
        line2 = null,
        city = "Pune",
        state = "Maharashtra",
        pincode = "411019",
        contactName = "",
        contactPhone = ""
    ),
    val draftId: String? = null,
    val quote: Quote? = null,
    val bookingId: String? = null,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.UPI,
)

@HiltViewModel
class BookingFlowViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _draft = MutableStateFlow(BookingDraftState())
    val draft: StateFlow<BookingDraftState> = _draft.asStateFlow()

    private val _quoteState = MutableStateFlow<UiState<Quote>>(UiState.Initial)
    val quoteState: StateFlow<UiState<Quote>> = _quoteState.asStateFlow()

    private val _confirmState = MutableStateFlow<UiState<String>>(UiState.Initial)
    val confirmState: StateFlow<UiState<String>> = _confirmState.asStateFlow()

    private val _paymentState = MutableStateFlow<UiState<PaymentStatus>>(UiState.Initial)
    val paymentState: StateFlow<UiState<PaymentStatus>> = _paymentState.asStateFlow()

    fun updateCargo(description: String, weightKg: Double, isHazardous: Boolean) {
        _draft.value = _draft.value.copy(
            cargoDescription = description,
            cargoWeightKg = weightKg,
            isHazardous = isHazardous
        )
    }

    fun updateContainer(containerType: ContainerType, tempC: Double?) {
        _draft.value = _draft.value.copy(
            containerType = containerType,
            temperatureRequirementC = tempC
        )
    }

    fun updateAddresses(pickup: Address, delivery: Address) {
        _draft.value = _draft.value.copy(
            pickupAddress = pickup,
            deliveryAddress = delivery
        )
    }

    fun requestQuote() {
        viewModelScope.launch {
            _quoteState.value = UiState.Loading
            val current = _draft.value

            val shipmentDetails = ShipmentDetails(
                originPort = current.pickupAddress.city,
                destinationPort = current.deliveryAddress.city,
                pickupAddress = current.pickupAddress,
                deliveryAddress = current.deliveryAddress,
                containerType = current.containerType,
                containerCount = 1,
                totalWeightKg = if (current.cargoWeightKg > 0) current.cargoWeightKg else 18500.0,
                commodity = current.cargoDescription.ifBlank { "Industrial Machinery Parts" },
                isHazardous = current.isHazardous,
                scheduledDate = Clock.System.now()
            )

            val draftResult = bookingRepository.createDraft(shipmentDetails)
            draftResult.fold(
                onSuccess = { draftId ->
                    _draft.value = _draft.value.copy(draftId = draftId)
                    val quoteResult = bookingRepository.getQuote(draftId)
                    quoteResult.fold(
                        onSuccess = { quote ->
                            _draft.value = _draft.value.copy(quote = quote)
                            _quoteState.value = UiState.Success(quote)
                        },
                        onFailure = { err ->
                            _quoteState.value = UiState.Error(
                                AppError.Server(500, err.message ?: "Failed to generate quote", "Try again")
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _quoteState.value = UiState.Error(
                        AppError.Server(500, err.message ?: "Failed to create shipment draft", "Try again")
                    )
                }
            )
        }
    }

    fun confirmBooking() {
        val current = _draft.value
        val draftId = current.draftId ?: "draft-001"
        val quoteId = current.quote?.id ?: "quote-001"

        viewModelScope.launch {
            _confirmState.value = UiState.Loading
            val result = bookingRepository.confirmBooking(draftId, quoteId)
            result.fold(
                onSuccess = { bookingId ->
                    _draft.value = _draft.value.copy(bookingId = bookingId)
                    _confirmState.value = UiState.Success(bookingId)
                },
                onFailure = { err ->
                    _confirmState.value = UiState.Error(
                        AppError.Server(500, err.message ?: "Failed to confirm booking", "Try again")
                    )
                }
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _draft.value = _draft.value.copy(selectedPaymentMethod = method)
    }

    fun processPayment(bookingId: String) {
        viewModelScope.launch {
            _paymentState.value = UiState.Loading
            paymentRepository.createPaymentOrder(bookingId)
            paymentRepository.observePaymentStatus(bookingId).collect { status ->
                when (status) {
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING -> {
                        _paymentState.value = UiState.Loading
                    }
                    PaymentStatus.SUCCESS -> {
                        _paymentState.value = UiState.Success(status)
                    }
                    PaymentStatus.FAILED -> {
                        _paymentState.value = UiState.Error(
                            AppError.Server(402, "Payment authorization failed", "Retry with another method")
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
