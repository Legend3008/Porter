package com.porter.feature.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.Booking
import com.porter.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Booking>>(UiState.Loading)
    val uiState: StateFlow<UiState<Booking>> = _uiState.asStateFlow()

    fun loadBooking(id: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = bookingRepository.getBooking(id)
            result.fold(
                onSuccess = { booking -> _uiState.value = UiState.Success(booking) },
                onFailure = { err ->
                    _uiState.value = UiState.Error(
                        AppError.Server(404, err.message ?: "Booking not found", "Go back")
                    )
                }
            )
        }
    }

    fun cancelBooking(id: String, reason: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(id, reason)
            loadBooking(id)
        }
    }
}
