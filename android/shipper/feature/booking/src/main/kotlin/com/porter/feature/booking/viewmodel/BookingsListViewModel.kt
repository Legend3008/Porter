package com.porter.feature.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.BookingListItem
import com.porter.domain.model.BookingStatus
import com.porter.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BookingFilterTab {
    ALL,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

@HiltViewModel
class BookingsListViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<BookingListItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<BookingListItem>>> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(BookingFilterTab.ALL)
    val selectedTab: StateFlow<BookingFilterTab> = _selectedTab.asStateFlow()

    private var allBookings: List<BookingListItem> = emptyList()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            bookingRepository.getBookings()
                .catch { exception ->
                    _uiState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Could not load bookings: ${exception.message}",
                            recovery = "Try again"
                        )
                    )
                }
                .collect { list ->
                    allBookings = list
                    applyFilter(_selectedTab.value)
                }
        }
    }

    fun selectTab(tab: BookingFilterTab) {
        _selectedTab.value = tab
        applyFilter(tab)
    }

    private fun applyFilter(tab: BookingFilterTab) {
        val filtered = when (tab) {
            BookingFilterTab.ALL -> allBookings
            BookingFilterTab.ACTIVE -> allBookings.filter {
                it.status != BookingStatus.DELIVERED && it.status != BookingStatus.CANCELLED
            }
            BookingFilterTab.COMPLETED -> allBookings.filter { it.status == BookingStatus.DELIVERED }
            BookingFilterTab.CANCELLED -> allBookings.filter { it.status == BookingStatus.CANCELLED }
        }

        _uiState.value = if (filtered.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(filtered)
        }
    }
}
